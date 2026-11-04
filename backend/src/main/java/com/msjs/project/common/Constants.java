package com.msjs.project.common;

/**
 * 全局常量类
 * 魔法数字、固定分桶名称、编码前缀、文件大小限制统一在此定义，禁止硬编码
 */
public final class Constants {

    private Constants() {
    }

    /** 成功状态码 */
    public static final Integer SUCCESS_CODE = 200;
    /** 失败状态码 */
    public static final Integer FAIL_CODE = 500;

    /** MinIO 临时桶（自动清理） */
    public static final String BUCKET_UPLOAD_TEMP = "upload-temp";
    /** MinIO 正式归档桶 */
    public static final String BUCKET_ARCHIVE_FORMAL = "archive-formal";
    /** MinIO 历史版本桶 */
    public static final String BUCKET_ARCHIVE_HISTORY = "archive-history";

    /** 单文件最大大小：500MB */
    public static final long FILE_MAX_SIZE = 500L * 1024 * 1024;
    /** 分片大小：5MB */
    public static final long CHUNK_SIZE = 5L * 1024 * 1024;

    /** 签名URL有效期（秒）：10分钟 */
    public static final int PRESIGNED_URL_EXPIRE = 600;

    /** 请求头 Token 名称 */
    public static final String HEADER_TOKEN = "Authorization";
}
