package com.msjs.project.common;

import java.util.Set;

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

    /** 上传文件后缀黑名单（可执行文件，防安全风险） */
    public static final java.util.Set<String> FILE_SUFFIX_BLACKLIST = java.util.Set.of(
            "exe", "bat", "cmd", "sh", "msi", "dll", "com", "scr", "vbs", "ps1", "jar");

    /** 签名URL有效期（秒）：10分钟 */
    public static final int PRESIGNED_URL_EXPIRE = 600;

    /** 允许上传的文件后缀白名单（小写，含点） */
    public static final Set<String> ALLOWED_UPLOAD_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp",
            ".zip", ".rar", ".7z", ".txt", ".csv"
    );

    /** 请求头 Token 名称 */
    public static final String HEADER_TOKEN = "Authorization";

    /** 区划层级：乡镇 */
    public static final int REGION_LEVEL_TOWN = 4;
    /** 区划层级：村 */
    public static final int REGION_LEVEL_VILLAGE = 5;
    /** 区划层级：组 */
    public static final int REGION_LEVEL_GROUP = 6;
    /** 村委会管理层级起点（乡镇） */
    public static final int REGION_MANAGE_MIN_LEVEL = REGION_LEVEL_TOWN;

    /** 文件状态：临时（分片上传中，未归档） */
    public static final int FILE_STATUS_TEMP = 0;
    /** 文件状态：正式归档 */
    public static final int FILE_STATUS_FORMAL = 1;
    /** 默认业务类型 */
    public static final String DEFAULT_BIZ_TYPE = "common";
    /** 分片上传最大分片数 */
    public static final int CHUNK_MAX_COUNT = 2000;
    /** 签名URL最大有效期（秒）：1小时，防止签名链接被长期复用 */
    public static final int MAX_PRESIGNED_EXPIRE = 3600;

    /** 立项审核状态：待审核 */
    public static final String APPROVAL_STATUS_PENDING = "PENDING";
    /** 立项审核状态：已通过 */
    public static final String APPROVAL_STATUS_APPROVED = "APPROVED";
    /** 立项审核状态：已驳回 */
    public static final String APPROVAL_STATUS_REJECTED = "REJECTED";

    /** 立项明细类型：农户 */
    public static final String APPROVAL_DETAIL_TYPE_HOUSEHOLD = "HOUSEHOLD";
    /** 立项明细类型：公共基础设施 */
    public static final String APPROVAL_DETAIL_TYPE_FACILITY = "FACILITY";

    /** 资金节点类型：项目（一级资金登记） */
    public static final String FUND_NODE_APPROVAL = "approval";
    /** 资金节点类型：申报内容（二级下拨） */
    public static final String FUND_NODE_DECLARE = "declare";
    /** 资金节点类型：申报明细（三级下拨） */
    public static final String FUND_NODE_DETAIL = "detail";

    /** 资金层级：一级资金登记 */
    public static final int FUND_LEVEL_APPROVAL = 1;
    /** 资金层级：二级下拨 */
    public static final int FUND_LEVEL_DECLARE = 2;
    /** 资金层级：三级下拨 */
    public static final int FUND_LEVEL_DETAIL = 3;

    /** 施工状态：未开工 */
    public static final String CONSTRUCTION_STATUS_NOT_STARTED = "NOT_STARTED";
    /** 施工状态：施工中 */
    public static final String CONSTRUCTION_STATUS_IN_PROGRESS = "IN_PROGRESS";
    /** 施工状态：已完工 */
    public static final String CONSTRUCTION_STATUS_COMPLETED = "COMPLETED";

    /** 巡检状态：合格 */
    public static final String CONSTRUCTION_INSPECTION_PASS = "PASS";
    /** 巡检状态：不合格 */
    public static final String CONSTRUCTION_INSPECTION_FAIL = "FAIL";

    /** 施工节点层级：项目（一级） */
    public static final int CONSTRUCTION_LEVEL_PROJECT = 1;
    /** 施工节点层级：农户/设施明细（三级） */
    public static final int CONSTRUCTION_LEVEL_DETAIL = 3;

    /** 立项页签：全部 */
    public static final String APPROVAL_TAB_ALL = "all";
    /** 立项页签：待审核 */
    public static final String APPROVAL_TAB_PENDING = "pending";
    /** 立项页签：已驳回 */
    public static final String APPROVAL_TAB_REJECTED = "rejected";

    /** 验收节点类型：项目（一级） */
    public static final String ACCEPTANCE_NODE_PROJECT = "project";
    /** 验收节点类型：申报内容（二级） */
    public static final String ACCEPTANCE_NODE_DECLARE = "declare";
    /** 验收节点类型：明细（三级） */
    public static final String ACCEPTANCE_NODE_DETAIL = "detail";

    /** 验收状态：待验收 */
    public static final String ACCEPTANCE_STATUS_PENDING = "PENDING";
    /** 验收状态：已验收 */
    public static final String ACCEPTANCE_STATUS_ACCEPTED = "ACCEPTED";
    /** 验收状态：部分验收（施工管理二级节点聚合展示用） */
    public static final String ACCEPTANCE_STATUS_PARTIAL = "PARTIAL";

    /** 验收状态：已归档 */
    public static final String ARCHIVE_STATUS_ARCHIVED = "ARCHIVED";

    /** 立项验收状态：未发起验收 */
    public static final String APPROVAL_ACCEPT_NONE = "NONE";
    /** 立项验收状态：验收中（已发起验收） */
    public static final String APPROVAL_ACCEPT_APPLYING = "APPLYING";
    /** 立项验收状态：已归档 */
    public static final String APPROVAL_ACCEPT_ARCHIVED = "ARCHIVED";
}
