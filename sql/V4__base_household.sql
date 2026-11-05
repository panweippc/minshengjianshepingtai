-- ============================================================
-- 民生建设项目管理平台 变更脚本 V4（修订版）
-- 内容：基础信息管理新增二级菜单「户基础信息」
--       1. 户基础信息主表 t_base_household
--       2. 户基础信息附件表 t_base_household_attachment
--       3. 菜单与按钮权限（字典已由其他分支预置，本脚本跳过）
-- 字符集：utf8mb4；主键：雪花ID（BIGINT，禁止自增）
-- 说明：脚本幂等可重复执行（建表 IF NOT EXISTS / 菜单 INSERT IGNORE）
-- ============================================================
USE msjs;

-- ----------------------------
-- 户基础信息主表（业务编码 household_no 由用户新增时手工录入，全局唯一）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_base_household (
    id BIGINT NOT NULL COMMENT '主键（雪花ID）',
    household_no VARCHAR(32) NOT NULL COMMENT '户号（业务编码，用户录入，全局唯一，编辑不可修改）',
    region_code VARCHAR(20) NOT NULL COMMENT '所属行政村区划编码（关联 t_sys_region.code，用于树形筛选与数据权限）',
    region_name VARCHAR(64) DEFAULT NULL COMMENT '所属行政村名称（冗余，便于列表展示）',
    head_name VARCHAR(32) NOT NULL COMMENT '户主姓名',
    head_id_card VARCHAR(20) DEFAULT NULL COMMENT '户主身份证号',
    contact_phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    family_population INT DEFAULT NULL COMMENT '家庭人口',
    house_area DECIMAL(10,2) DEFAULT NULL COMMENT '房屋面积（㎡）',
    house_type VARCHAR(8) DEFAULT NULL COMMENT '房屋类型（字典 fang_wu_jie_gou_lei_xing）',
    household_type VARCHAR(8) DEFAULT NULL COMMENT '户类型（字典 hu_lei_xing）',
    property_no VARCHAR(64) DEFAULT NULL COMMENT '产权编号',
    archive_time DATE DEFAULT NULL COMMENT '建档时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_household_no (household_no),
    KEY idx_region_code (region_code),
    KEY idx_head_id_card (head_id_card),
    KEY idx_head_name (head_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='户基础信息';

-- ----------------------------
-- 户基础信息附件表
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_base_household_attachment (
    id BIGINT NOT NULL COMMENT '主键（雪花ID）',
    household_id BIGINT NOT NULL COMMENT '户基础信息ID（关联 t_base_household.id）',
    attachment_type VARCHAR(8) DEFAULT NULL COMMENT '附件类型（字典 fu_jian_lei_xing）',
    file_name VARCHAR(256) DEFAULT NULL COMMENT '原始文件名',
    file_key VARCHAR(512) DEFAULT NULL COMMENT 'MinIO 对象存储 key',
    bucket VARCHAR(64) DEFAULT NULL COMMENT '存储桶',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    remark VARCHAR(256) DEFAULT NULL COMMENT '附件备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (id),
    KEY idx_household_id (household_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='户基础信息附件';

-- ----------------------------
-- 菜单：基础信息管理 → 户基础信息（含按钮权限）
-- ID 选择：避开已占用的 1101~1107 / 1201~1273 / 30001~30014
-- ----------------------------
INSERT IGNORE INTO t_sys_menu (id, parent_id, menu_name, menu_type, path, perm, icon, sort) VALUES
(1108, 1001, '户基础信息', 'C', '/base/household', 'base:household:list', 'House', 3);

INSERT IGNORE INTO t_sys_menu (id, parent_id, menu_name, menu_type, path, perm, icon, sort) VALUES
(1281, 1108, '户基础新增', 'F', NULL, 'base:household:add', NULL, 1),
(1282, 1108, '户基础编辑', 'F', NULL, 'base:household:edit', NULL, 2),
(1283, 1108, '户基础删除', 'F', NULL, 'base:household:delete', NULL, 3);

-- 超级管理员默认拥有新菜单权限
INSERT IGNORE INTO t_sys_role_menu (id, role_id, menu_id) VALUES
(30021, 1001, 1108),
(30022, 1001, 1281),
(30023, 1001, 1282),
(30024, 1001, 1283);

-- 普通角色如需户基础信息权限，可在「角色管理-分配权限」中勾选
