package com.msjs.project.service.impl;

import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.service.FileService;
import com.msjs.project.vo.FileUploadVO;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件服务实现：MinIO 上传、SHA256 哈希、时效签名URL
 * 安全要求：后缀黑名单、大小限制在上传入口强制校验；预览/下载仅使用时效签名URL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;

    @Override
    public FileUploadVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        // 文件大小限制
        if (file.getSize() > Constants.FILE_MAX_SIZE) {
            throw new BizException("文件大小超过限制（最大 500MB）");
        }
        String originalName = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
        // 后缀黑名单校验（可执行文件禁止上传）
        String suffix = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx >= 0) {
            suffix = originalName.substring(dotIdx + 1).toLowerCase();
        }
        if (Constants.FILE_SUFFIX_BLACKLIST.contains(suffix)) {
            throw new BizException("不允许上传 ." + suffix + " 类型文件");
        }

        try {
            ensureBucket(Constants.BUCKET_ARCHIVE_FORMAL);
            // 对象 key：日期分目录 + UUID 防重名，保留原后缀
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileKey = datePath + "/" + UUID.randomUUID().toString().replace("-", "")
                    + (suffix.isEmpty() ? "" : "." + suffix);

            // 读取字节流：一次入内存计算 SHA256 后上传（附件场景文件较小，500MB 上限内可接受）
            byte[] bytes;
            try (InputStream in = file.getInputStream()) {
                bytes = in.readAllBytes();
            }
            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(Constants.BUCKET_ARCHIVE_FORMAL)
                    .object(fileKey)
                    .stream(new java.io.ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build());

            FileUploadVO vo = new FileUploadVO();
            vo.setFileName(originalName);
            vo.setFileKey(fileKey);
            vo.setBucket(Constants.BUCKET_ARCHIVE_FORMAL);
            vo.setFileSize(file.getSize());
            vo.setFileHash(sha256);
            return vo;
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

    /** 桶不存在时自动创建（固定分桶，仅在系统约定的三个桶范围内调用） */
    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
