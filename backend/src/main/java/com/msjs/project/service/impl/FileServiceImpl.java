package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.UserContext;
import com.msjs.project.entity.SysFile;
import com.msjs.project.mapper.SysFileMapper;
import com.msjs.project.service.FileService;
import com.msjs.project.vo.FileUploadVO;
import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 文件服务实现：MinIO 上传、SHA256 流式哈希、秒传去重、分片续传、时效签名URL
 * 安全要求：后缀黑名单、大小限制在上传入口强制校验；预览/下载仅使用时效签名URL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    /** 流式读取缓冲区大小（1MB），避免大文件全量入内存 */
    private static final int STREAM_BUFFER_SIZE = 1024 * 1024;
    /** 上传标识合法格式：字母、数字、中划线，长度 ≤ 64，防止路径穿越与越权前缀 */
    private static final Pattern UPLOAD_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]{1,64}$");
    /** 合并后临时对象名（位于 uploadId 前缀目录下） */
    private static final String MERGED_OBJECT_NAME = "merged";
    /** 未获取到登录用户时的默认操作人 */
    private static final String DEFAULT_OPERATOR = "system";
    /** 业务类型最大长度（与表字段 biz_type VARCHAR(32) 对齐） */
    private static final int BIZ_TYPE_MAX_LENGTH = 32;

    private final MinioClient minioClient;
    private final SysFileMapper sysFileMapper;

    @Override
    public FileUploadVO upload(MultipartFile file) {
        return upload(file, Constants.DEFAULT_BIZ_TYPE);
    }

    @Override
    public FileUploadVO upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        // 文件大小限制
        if (file.getSize() > Constants.FILE_MAX_SIZE) {
            throw new BizException("文件大小超过限制（最大 500MB）");
        }
        String originalName = resolveOriginalName(file);
        String suffix = resolveSuffix(originalName);
        checkSuffix(suffix);
        String effectiveBizType = resolveBizType(bizType);

        // 流式计算 SHA256：边读边算，避免 500MB 文件全量载入内存
        String sha256;
        try {
            sha256 = calcFileSha256(file);
        } catch (Exception e) {
            log.error("计算文件 SHA256 失败: {}", originalName, e);
            throw new BizException("文件读取失败，请稍后重试");
        }

        // 秒传：命中记录且对象仍存在时直接复用，不再写新对象
        SysFile staleRecord = null;
        SysFile existing = findExistingBySha256(sha256);
        if (existing != null) {
            if (isObjectExists(existing.getBucket(), existing.getFileKey())) {
                log.info("命中秒传，复用已有文件: fileId={}, sha256={}", existing.getId(), sha256);
                return toVO(existing, true);
            }
            // 对象已被清理：忽略该记录继续正常上传，后续复用该记录修复元数据（避免唯一键冲突）
            log.warn("秒传记录对应对象已不存在，重新上传: fileId={}, fileKey={}", existing.getId(), existing.getFileKey());
            staleRecord = existing;
        }

        try {
            ensureBucket(Constants.BUCKET_ARCHIVE_FORMAL);
            // 对象 key：日期分目录 + UUID 防重名，保留原后缀
            String fileKey = buildFileKey(suffix);
            String contentType = file.getContentType() == null
                    ? "application/octet-stream" : file.getContentType();

            // 重新打开输入流上传同一文件内容（MultipartFile 支持多次读取）
            try (InputStream in = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(Constants.BUCKET_ARCHIVE_FORMAL)
                        .object(fileKey)
                        .stream(in, file.getSize(), -1)
                        .contentType(contentType)
                        .build());
            }

            SysFile record = persistRecord(staleRecord, originalName, fileKey,
                    Constants.BUCKET_ARCHIVE_FORMAL, file.getSize(), contentType, sha256, effectiveBizType);
            return toVO(record, false);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败: {}", originalName, e);
            throw new BizException("文件上传失败，请稍后重试");
        }
    }

    @Override
    public String presign(String bucket, String fileKey, String fileName, boolean download) {
        if (bucket == null || bucket.isBlank() || fileKey == null || fileKey.isBlank()) {
            throw new BizException("缺少文件存储信息");
        }
        // 只允许固定分桶，防止越权访问其他桶
        if (!Constants.BUCKET_UPLOAD_TEMP.equals(bucket)
                && !Constants.BUCKET_ARCHIVE_FORMAL.equals(bucket)
                && !Constants.BUCKET_ARCHIVE_HISTORY.equals(bucket)) {
            throw new BizException("非法的存储桶");
        }
        try {
            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(fileKey)
                    .expiry(Constants.PRESIGNED_URL_EXPIRE, TimeUnit.SECONDS);
            if (download) {
                // 下载：强制 Content-Disposition 附件响应头，文件名按 RFC5987 编码
                String name = (fileName == null || fileName.isBlank()) ? "download" : fileName;
                String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
                builder.extraQueryParams(Map.of(
                        "response-content-disposition",
                        "attachment; filename*=UTF-8''" + encoded));
            }
            return minioClient.getPresignedObjectUrl(builder.build());
        } catch (Exception e) {
            log.error("生成签名URL失败: {}/{}", bucket, fileKey, e);
            throw new BizException("生成文件访问链接失败");
        }
    }

    @Override
    public void uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk) {
        String validUploadId = validateUploadId(uploadId);
        if (chunkIndex < 0) {
            throw new BizException("分片序号不能小于0");
        }
        if (chunk == null || chunk.isEmpty()) {
            throw new BizException("分片文件不能为空");
        }
        try {
            ensureBucket(Constants.BUCKET_UPLOAD_TEMP);
            String objectName = chunkObjectName(validUploadId, chunkIndex);
            try (InputStream in = chunk.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(Constants.BUCKET_UPLOAD_TEMP)
                        .object(objectName)
                        .stream(in, chunk.getSize(), -1)
                        .contentType(chunk.getContentType() == null
                                ? "application/octet-stream" : chunk.getContentType())
                        .build());
            }
            log.debug("分片上传成功: uploadId={}, chunkIndex={}", validUploadId, chunkIndex);
        } catch (Exception e) {
            log.error("分片上传失败: uploadId={}, chunkIndex={}", validUploadId, chunkIndex, e);
            throw new BizException("分片上传失败，请稍后重试");
        }
    }

    @Override
    public List<Integer> chunkStatus(String uploadId) {
        String validUploadId = validateUploadId(uploadId);
        try {
            ensureBucket(Constants.BUCKET_UPLOAD_TEMP);
            return listChunkIndexes(validUploadId);
        } catch (Exception e) {
            log.error("查询分片状态失败: uploadId={}", validUploadId, e);
            throw new BizException("查询分片状态失败，请稍后重试");
        }
    }

    @Override
    public FileUploadVO mergeChunks(String uploadId, int totalChunks, String fileName, String bizType) {
        String validUploadId = validateUploadId(uploadId);
        if (totalChunks < 1 || totalChunks > Constants.CHUNK_MAX_COUNT) {
            throw new BizException("分片总数必须在 1 ~ " + Constants.CHUNK_MAX_COUNT + " 之间");
        }
        String originalName = (fileName == null || fileName.isBlank()) ? "unnamed" : fileName;
        String suffix = resolveSuffix(originalName);
        checkSuffix(suffix);
        String effectiveBizType = resolveBizType(bizType);

        try {
            ensureBucket(Constants.BUCKET_UPLOAD_TEMP);
            ensureBucket(Constants.BUCKET_ARCHIVE_FORMAL);

            // 校验分片完整性，缺片时返回具体序号便于前端续传
            List<Integer> uploadedIndexes = listChunkIndexes(validUploadId);
            for (int i = 0; i < totalChunks; i++) {
                if (!uploadedIndexes.contains(i)) {
                    throw new BizException("缺少分片：" + i);
                }
            }

            // MinIO 对象合成要求：除最后一个分片外，每个分片必须不小于 5MB，提前校验给出明确提示
            for (int i = 0; i < totalChunks - 1; i++) {
                StatObjectResponse partStat = minioClient.statObject(StatObjectArgs.builder()
                        .bucket(Constants.BUCKET_UPLOAD_TEMP)
                        .object(chunkObjectName(validUploadId, i))
                        .build());
                if (partStat.size() < Constants.CHUNK_SIZE) {
                    throw new BizException("分片 " + i + " 大小不足 5MB，无法合并（仅最后一个分片可小于 5MB）");
                }
            }

            // 按序合成临时对象 {uploadId}/merged
            List<ComposeSource> sources = new ArrayList<>(totalChunks);
            for (int i = 0; i < totalChunks; i++) {
                sources.add(ComposeSource.builder()
                        .bucket(Constants.BUCKET_UPLOAD_TEMP)
                        .object(chunkObjectName(validUploadId, i))
                        .build());
            }
            String mergedObject = validUploadId + "/" + MERGED_OBJECT_NAME;
            minioClient.composeObject(ComposeObjectArgs.builder()
                    .bucket(Constants.BUCKET_UPLOAD_TEMP)
                    .object(mergedObject)
                    .sources(sources)
                    .build());

            // 合并后校验大小，防止绕过单分片大小限制
            StatObjectResponse mergedStat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(Constants.BUCKET_UPLOAD_TEMP)
                    .object(mergedObject)
                    .build());
            if (mergedStat.size() > Constants.FILE_MAX_SIZE) {
                removeTempObjects(validUploadId);
                throw new BizException("文件大小超过限制（最大 500MB）");
            }

            // 流式计算合并后文件的 SHA256
            String sha256 = calcObjectSha256(mergedObject);

            // 秒传：命中记录且对象仍存在，清理临时分片并返回原记录
            SysFile staleRecord = null;
            SysFile existing = findExistingBySha256(sha256);
            if (existing != null) {
                if (isObjectExists(existing.getBucket(), existing.getFileKey())) {
                    removeTempObjects(validUploadId);
                    log.info("分片合并命中秒传，复用已有文件: fileId={}, sha256={}", existing.getId(), sha256);
                    return toVO(existing, true);
                }
                log.warn("秒传记录对应对象已不存在，重新归档: fileId={}, fileKey={}",
                        existing.getId(), existing.getFileKey());
                staleRecord = existing;
            }

            // 复制到正式归档桶（沿用日期目录 + UUID key 规则）
            String fileKey = buildFileKey(suffix);
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(Constants.BUCKET_ARCHIVE_FORMAL)
                    .object(fileKey)
                    .source(CopySource.builder()
                            .bucket(Constants.BUCKET_UPLOAD_TEMP)
                            .object(mergedObject)
                            .build())
                    .build());

            SysFile record = persistRecord(staleRecord, originalName, fileKey,
                    Constants.BUCKET_ARCHIVE_FORMAL, mergedStat.size(), mergedStat.contentType(),
                    sha256, effectiveBizType);
            // 归档完成后清理该 uploadId 下全部临时分片与合并对象
            removeTempObjects(validUploadId);
            return toVO(record, false);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("分片合并失败: uploadId={}", validUploadId, e);
            throw new BizException("分片合并失败，请稍后重试");
        }
    }

    /** 流式计算 MultipartFile 的 SHA256（读取缓冲区不保留文件内容） */
    private String calcFileSha256(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(
                new DigestInputStream(file.getInputStream(), digest), STREAM_BUFFER_SIZE)) {
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            while (in.read(buffer) != -1) {
                // 读取即更新摘要，不保留字节内容
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** 流式计算 MinIO 对象的 SHA256（用于分片合并后的秒传判断） */
    private String calcObjectSha256(String objectName) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(
                new DigestInputStream(minioClient.getObject(GetObjectArgs.builder()
                        .bucket(Constants.BUCKET_UPLOAD_TEMP)
                        .object(objectName)
                        .build()), digest), STREAM_BUFFER_SIZE)) {
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            while (in.read(buffer) != -1) {
                // 读取即更新摘要，不保留字节内容
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** 按 SHA256 查询已归档文件记录（逻辑删除条件由 MyBatis-Plus 自动追加） */
    private SysFile findExistingBySha256(String sha256) {
        return sysFileMapper.selectOne(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getSha256, sha256));
    }

    /**
     * 落库文件元数据
     * staleRecord 不为空表示该 SHA256 已有记录但对象已丢失，复用原记录修复元数据，避免 uk_sha256 冲突
     */
    private SysFile persistRecord(SysFile staleRecord, String fileName, String fileKey, String bucket,
                                 long fileSize, String contentType, String sha256, String bizType) {
        SysFile record = staleRecord == null ? new SysFile() : staleRecord;
        record.setFileName(fileName);
        record.setFileKey(fileKey);
        record.setBucket(bucket);
        record.setFileSize(fileSize);
        record.setContentType(contentType);
        record.setSha256(sha256);
        record.setBizType(bizType);
        record.setStatus(Constants.FILE_STATUS_FORMAL);
        record.setCreateBy(currentUsername());
        record.setCreateTime(java.time.LocalDateTime.now());
        if (staleRecord == null) {
            sysFileMapper.insert(record);
        } else {
            sysFileMapper.updateById(record);
        }
        return record;
    }

    /** 组装上传结果（不包含内部字段） */
    private FileUploadVO toVO(SysFile record, boolean exist) {
        FileUploadVO vo = new FileUploadVO();
        vo.setFileId(record.getId());
        vo.setExist(exist);
        vo.setFileName(record.getFileName());
        vo.setFileKey(record.getFileKey());
        vo.setBucket(record.getBucket());
        vo.setFileSize(record.getFileSize());
        vo.setFileHash(record.getSha256());
        return vo;
    }

    /** 当前登录账号，未登录（如内部调用）时使用 system */
    private String currentUsername() {
        LoginUser user = UserContext.get();
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return DEFAULT_OPERATOR;
        }
        return user.getUsername();
    }

    /** 校验并返回合法的 uploadId，防止路径穿越与越权前缀 */
    private String validateUploadId(String uploadId) {
        if (uploadId == null || !UPLOAD_ID_PATTERN.matcher(uploadId).matches()) {
            throw new BizException("上传标识不合法（仅允许字母、数字、中划线，长度不超过64）");
        }
        return uploadId;
    }

    /** 分片对象名：{uploadId}/{chunkIndex} */
    private String chunkObjectName(String uploadId, int chunkIndex) {
        return uploadId + "/" + chunkIndex;
    }

    /** 列出已上传分片序号（升序去重），非纯数字对象（如 merged）自动忽略 */
    private List<Integer> listChunkIndexes(String uploadId) throws Exception {
        String prefix = uploadId + "/";
        List<Integer> indexes = new ArrayList<>();
        for (Result<Item> result : minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(Constants.BUCKET_UPLOAD_TEMP)
                .prefix(prefix)
                .recursive(true)
                .build())) {
            Item item = result.get();
            String rest = item.objectName().substring(prefix.length());
            if (rest.isEmpty() || rest.length() > 9 || !rest.chars().allMatch(Character::isDigit)) {
                continue;
            }
            Integer index = Integer.valueOf(rest);
            if (!indexes.contains(index)) {
                indexes.add(index);
            }
        }
        Collections.sort(indexes);
        return indexes;
    }

    /** 清空该 uploadId 下的全部临时分片与合并对象（失败仅告警，不阻断已完成的归档） */
    private void removeTempObjects(String uploadId) {
        String prefix = uploadId + "/";
        List<DeleteObject> objects = new ArrayList<>();
        try {
            for (Result<Item> result : minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(Constants.BUCKET_UPLOAD_TEMP)
                    .prefix(prefix)
                    .recursive(true)
                    .build())) {
                objects.add(new DeleteObject(result.get().objectName()));
            }
        } catch (Exception e) {
            log.warn("列出待清理临时分片失败: uploadId={}", uploadId, e);
            return;
        }
        if (objects.isEmpty()) {
            return;
        }
        try {
            for (Result<DeleteError> result : minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(Constants.BUCKET_UPLOAD_TEMP)
                    .objects(objects)
                    .build())) {
                DeleteError error = result.get();
                log.warn("清理临时分片失败: uploadId={}, object={}, message={}",
                        uploadId, error.objectName(), error.message());
            }
        } catch (Exception e) {
            log.warn("清理临时分片异常: uploadId={}", uploadId, e);
        }
    }

    /** 校验对象是否真实存在（不存在或校验异常均按不存在处理，交由调用方重新上传） */
    private boolean isObjectExists(String bucket, String fileKey) {
        if (bucket == null || bucket.isBlank() || fileKey == null || fileKey.isBlank()) {
            return false;
        }
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(fileKey).build());
            return true;
        } catch (ErrorResponseException e) {
            log.warn("文件对象不存在，按失效处理: {}/{}, code={}", bucket, fileKey,
                    e.errorResponse() == null ? null : e.errorResponse().code());
            return false;
        } catch (Exception e) {
            log.warn("校验文件对象失败，按不存在处理: {}/{}", bucket, fileKey, e);
            return false;
        }
    }

    /** 原始文件名缺省处理 */
    private String resolveOriginalName(MultipartFile file) {
        return file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
    }

    /** 提取小写后缀（不含点） */
    private String resolveSuffix(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        return dotIdx >= 0 ? fileName.substring(dotIdx + 1).toLowerCase() : "";
    }

    /** 后缀黑名单校验（可执行文件禁止上传） */
    private void checkSuffix(String suffix) {
        if (!suffix.isEmpty() && Constants.FILE_SUFFIX_BLACKLIST.contains(suffix)) {
            throw new BizException("不允许上传 ." + suffix + " 类型文件");
        }
    }

    /** 业务类型归一化：为空取默认值，超长直接拒绝（与表字段长度对齐） */
    private String resolveBizType(String bizType) {
        if (bizType == null || bizType.isBlank()) {
            return Constants.DEFAULT_BIZ_TYPE;
        }
        String trimmed = bizType.trim();
        if (trimmed.length() > BIZ_TYPE_MAX_LENGTH) {
            throw new BizException("业务类型长度不能超过" + BIZ_TYPE_MAX_LENGTH);
        }
        return trimmed;
    }

    /** 对象 key：日期分目录 + UUID 防重名，保留原后缀 */
    private String buildFileKey(String suffix) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return datePath + "/" + UUID.randomUUID().toString().replace("-", "")
                + (suffix.isEmpty() ? "" : "." + suffix);
    }

    /** 桶不存在时自动创建（固定分桶，仅在系统约定的三个桶范围内调用） */
    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
