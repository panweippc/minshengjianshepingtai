package com.msjs.project.file;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.vo.FileUploadVO;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 文件服务实现（MinIO）
 * 安全要求：校验后缀白名单、大小限制、SHA-256 哈希、时效签名 URL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;

    /** 允许上传的后缀白名单（小写，含点） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp",
            ".zip", ".rar", ".7z", ".txt", ".csv"
    );

    @Override
    public FileUploadVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new BizException("文件名不能为空");
        }
        // 大小限制：500MB
        if (file.getSize() > Constants.FILE_MAX_SIZE) {
            throw new BizException("文件大小超过限制：" + (Constants.FILE_MAX_SIZE / 1024 / 1024) + "MB");
        }
        // 后缀白名单校验
        String suffix = extractSuffix(original);
        if (suffix == null || !ALLOWED_EXTENSIONS.contains(suffix.toLowerCase())) {
            throw new BizException("不支持的文件类型：" + suffix);
        }

        // 计算 SHA-256 哈希（用于去重与校验）
        byte[] digest;
        try (InputStream in = file.getInputStream()) {
            digest = sha256(in);
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BizException("文件读取失败");
        }
        String hex = toHex(digest);

        // 对象路径：facility/yyyyMMdd/雪花id_原文件名
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String object = "facility/" + dateDir + "/" + IdWorker.getId() + suffix;

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(Constants.BUCKET_ARCHIVE_FORMAL)
                    .object(object)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (MinioException e) {
            log.error("MinIO 上传失败: {}", e.getMessage(), e);
            throw new BizException("文件存储失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("文件上传异常", e);
            throw new BizException("文件上传失败");
        }

        FileUploadVO vo = new FileUploadVO();
        vo.setFileName(original);
        vo.setFilePath(object);
        vo.setFileHash(hex);
        vo.setFileSize(file.getSize());
        return vo;
    }

    @Override
    public String getPresignedUrl(String object) {
        if (object == null || object.isBlank()) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(Constants.BUCKET_ARCHIVE_FORMAL)
                            .object(object)
                            .expiry(Constants.PRESIGNED_URL_EXPIRE, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取签名URL失败 object={}", object, e);
            return null;
        }
    }

    private String extractSuffix(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0) {
            return null;
        }
        return name.substring(idx).toLowerCase();
    }

    private byte[] sha256(InputStream in) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
            return md.digest();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new BizException("文件哈希计算失败");
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
