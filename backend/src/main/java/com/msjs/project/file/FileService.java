package com.msjs.project.file;

import com.msjs.project.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口（MinIO 存储）
 * 业务约束：固定分桶、SHA-256 哈希校验、时效签名 URL
 */
public interface FileService {

    /**
     * 上传文件到正式归档桶（archive-formal）
     *
     * @param file 上传文件
     * @return 文件元信息：原始文件名、对象路径、SHA-256 哈希、大小
     */
    FileUploadVO upload(MultipartFile file);

    /**
     * 获取文件时效签名访问 URL（10 分钟有效）
     *
     * @param object 对象路径
     * @return 可直接访问的签名 URL
     */
    String getPresignedUrl(String object);
}
