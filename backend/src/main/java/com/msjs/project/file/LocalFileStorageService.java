package com.msjs.project.file;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.vo.FileUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地磁盘文件存储（开发环境临时方案）
 * 业务约束：文件落盘到 file.upload-dir 下，访问必须鉴权；
 * 安全要求：后缀白名单、大小限制、SHA-256 哈希、路径穿越校验
 * 说明：后续可平滑切换为 MinIO 的 archive-formal 桶存储
 */
@Slf4j
@Service
public class LocalFileStorageService {

    /** 文件存储根目录（相对或绝对路径均可） */
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /** 业务子目录：设施附件 */
    private static final String BIZ_DIR = "facility";

    /**
     * 上传文件到本地磁盘
     *
     * @param file 上传文件
     * @return 文件元信息：原始文件名、相对路径、SHA-256 哈希、大小
     */
    public FileUploadVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new BizException("文件名不能为空");
        }
        // 大小限制
        if (file.getSize() > Constants.FILE_MAX_SIZE) {
            throw new BizException("文件大小超过限制：" + (Constants.FILE_MAX_SIZE / 1024 / 1024) + "MB");
        }
        // 后缀白名单校验
        String suffix = extractSuffix(original);
        if (suffix == null || !Constants.ALLOWED_UPLOAD_EXTENSIONS.contains(suffix.toLowerCase())) {
            throw new BizException("不支持的文件类型：" + suffix);
        }

        // SHA-256 哈希（用于文件校验与去重）
        String hex;
        try (InputStream in = file.getInputStream()) {
            hex = toHex(sha256(in));
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BizException("文件读取失败");
        }

        // 相对路径：facility/yyyyMMdd/雪花ID.后缀
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String relative = BIZ_DIR + "/" + dateDir + "/" + IdWorker.getId() + suffix;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("文件落盘失败 path={}", relative, e);
            throw new BizException("文件保存失败");
        }

        FileUploadVO vo = new FileUploadVO();
        vo.setFileName(original);
        vo.setFilePath(relative);
        vo.setFileHash(hex);
        vo.setFileSize(file.getSize());
        return vo;
    }

    /**
     * 将相对路径解析为磁盘绝对路径，并防止路径穿越
     *
     * @param relativePath 相对路径
     * @return 绝对路径
     */
    public Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || relativePath.contains("..")) {
            throw new BizException("文件路径不合法");
        }
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BizException("文件路径不合法");
        }
        return target;
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
