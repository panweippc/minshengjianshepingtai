package com.msjs.project.service;

import com.msjs.project.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务：MinIO 上传、SHA256 哈希、秒传去重、分片续传、时效签名URL
 * 业务约束：
 * 1. 固定分桶 upload-temp / archive-formal / archive-history（见 Constants）
 * 2. 上传强制校验后缀黑名单与文件大小
 * 3. 预览/下载仅通过时效签名URL，禁止永久直链
 * 4. 分片上传临时文件存储于 upload-temp，合并成功后清理
 */
public interface FileService {

    /** 上传文件到正式归档桶 archive-formal，返回对象信息（含 SHA256） */
    FileUploadVO upload(MultipartFile file);

    /**
     * 上传文件并记录业务类型（秒传命中时直接复用已有文件）
     *
     * @param file    上传文件
     * @param bizType 业务类型（为空时使用 Constants.DEFAULT_BIZ_TYPE）
     */
    FileUploadVO upload(MultipartFile file, String bizType);

    /**
     * 生成时效签名URL（有效期见 Constants.PRESIGNED_URL_EXPIRE）
     *
     * @param bucket   存储桶
     * @param fileKey  对象 key
     * @param fileName 原始文件名（下载时用于响应头 Content-Disposition）
     * @param download true=强制下载（attachment），false=浏览器内联预览
     */
    String presign(String bucket, String fileKey, String fileName, boolean download);

    /**
     * 上传单个分片到临时桶 upload-temp
     *
     * @param uploadId   上传标识（仅字母、数字、中划线，长度 ≤ 64）
     * @param chunkIndex 分片序号（从 0 开始）
     * @param chunk      分片文件
     */
    void uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk);

    /**
     * 查询已上传分片序号（升序），供断点续传跳过已传分片
     *
     * @param uploadId 上传标识
     */
    List<Integer> chunkStatus(String uploadId);

    /**
     * 合并分片：按序合成后流式计算 SHA256，命中秒传则复用已有文件，否则归档到 archive-formal 并清理临时分片
     *
     * @param uploadId    上传标识
     * @param totalChunks 分片总数（1 ~ Constants.CHUNK_MAX_COUNT）
     * @param fileName    原始文件名（用于后缀校验与归档命名）
     * @param bizType     业务类型（为空时使用 Constants.DEFAULT_BIZ_TYPE）
     */
    FileUploadVO mergeChunks(String uploadId, int totalChunks, String fileName, String bizType);
}
