package com.msjs.project.service;

import com.msjs.project.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务：MinIO 上传、SHA256 哈希、时效签名URL
 * 业务约束：
 * 1. 固定分桶 upload-temp / archive-formal / archive-history（见 Constants）
 * 2. 上传强制校验后缀黑名单与文件大小
 * 3. 预览/下载仅通过时效签名URL，禁止永久直链
 */
public interface FileService {

    /** 上传文件到正式归档桶 archive-formal，返回对象信息（含 SHA256） */
    FileUploadVO upload(MultipartFile file);

    /**
     * 生成时效签名URL（有效期见 Constants.PRESIGNED_URL_EXPIRE）
     *
     * @param bucket   存储桶
     * @param fileKey  对象 key
     * @param fileName 原始文件名（下载时用于响应头 Content-Disposition）
     * @param download true=强制下载（attachment），false=浏览器内联预览
     */
    String presign(String bucket, String fileKey, String fileName, boolean download);
}
