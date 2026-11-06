-- ============================================================
-- 民生建设项目管理平台 变更脚本 V4
-- 内容：基础信息管理新增二级菜单「公共基础设施」
--       1. 公共基础设施主表 t_base_facility
--       2. 设施附件表 t_base_facility_attachment
--       3. 项目表 t_project（简化版，供设施查看页签关联展示）
--       4. 设施-项目关联表 t_base_facility_project
--       5. 设施类型字典 she_shi_lei_xing、附件类型字典 fu_jian_lei_xing
--       6. 菜单与按钮权限
--       7. 示例设施数据
-- 字符集：utf8mb4；主键：雪花ID（BIGINT，禁止自增）
-- 说明：本脚本为一次性变更脚本，请按 V1 → V2 → V3 → V4 顺序执行
-- ============================================================
USE msjs;

-- ----------------------------
-- 公共基础设施主表
-- ----------------------------
DROP TABLE IF EXISTS t_base_facility;
CREATE TABLE t_base_facility (
    id              BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    facility_code   VARCHAR(32)   NOT NULL COMMENT '设施编号（业务编码，全局唯一）',
    facility_name   VARCHAR(128)  NOT NULL COMMENT '设施名称',
    facility_type   VARCHAR(16)   NULL COMMENT '设施类型（字典 she_shi_lei_xing）',
    region_code     VARCHAR(20)   NOT NULL COMMENT '所属行政村区划代码',
    region_name     VARCHAR(64)   NULL COMMENT '所属行政村名称（冗余，便于展示）',
    location        VARCHAR(255)  NULL COMMENT '具体位置',
    build_year      INT           NULL COMMENT '建成年份',
    quantity        DECIMAL(12,2) NULL COMMENT '数量',
    unit            VARCHAR(16)   NULL COMMENT '单位',
    remark          VARCHAR(500)  NULL COMMENT '备注',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_facility_code (facility_code),
    KEY idx_facility_region (region_code),
    KEY idx_facility_type (facility_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '公共基础设施表';

-- ----------------------------
-- 设施附件表
-- ----------------------------
DROP TABLE IF EXISTS t_base_facility_attachment;
CREATE TABLE t_base_facility_attachment (
    id              BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    facility_id     BIGINT       NOT NULL COMMENT '设施ID',
    attachment_type VARCHAR(16)  NULL COMMENT '附件类型（字典 fu_jian_lei_xing）',
    file_name       VARCHAR(255) NULL COMMENT '原始文件名',
    file_path       VARCHAR(512) NULL COMMENT '存储路径',
    file_hash       VARCHAR(64)  NULL COMMENT 'SHA256哈希（去重用）',
    file_size       BIGINT       NULL COMMENT '文件大小（字节）',
    remark          VARCHAR(500) NULL COMMENT '备注',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_attachment_facility (facility_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设施附件表';

-- ----------------------------
-- 项目表（简化版，供设施查看页签关联展示）
-- ----------------------------
DROP TABLE IF EXISTS t_project;
CREATE TABLE t_project (
    id          BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    project_code VARCHAR(32)  NOT NULL COMMENT '项目编码（业务编码，全局唯一）',
    project_name VARCHAR(255) NOT NULL COMMENT '项目名称',
    project_type VARCHAR(16)  NULL COMMENT '项目类型',
    progress    DECIMAL(5,2)  NULL COMMENT '进度（百分比）',
    budget      DECIMAL(15,2) NULL COMMENT '预算（元）',
    status      VARCHAR(16)   NULL COMMENT '状态',
    start_year  INT           NULL COMMENT '立项年度',
    region_code VARCHAR(20)   NULL COMMENT '所属区划代码',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_code (project_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目表（简化版）';

-- ----------------------------
-- 设施-项目关联表
-- ----------------------------
DROP TABLE IF EXISTS t_base_facility_project;
CREATE TABLE t_base_facility_project (
    id          BIGINT NOT NULL COMMENT '主键（雪花ID）',
    facility_id BIGINT NOT NULL COMMENT '设施ID',
    project_id  BIGINT NOT NULL COMMENT '项目ID',
    PRIMARY KEY (id),
    KEY idx_fp_facility (facility_id),
    KEY idx_fp_project (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设施-项目关联表';

-- ----------------------------
-- 字典：设施类型 she_shi_lei_xing
-- ----------------------------
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9101, 'she_shi_lei_xing', '设施类型', '水利设施', '1', 1, '公共基础设施-设施类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'she_shi_lei_xing' AND dict_value = '1');
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9102, 'she_shi_lei_xing', '设施类型', '交通设施', '2', 2, '公共基础设施-设施类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'she_shi_lei_xing' AND dict_value = '2');
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9103, 'she_shi_lei_xing', '设施类型', '电力设施', '3', 3, '公共基础设施-设施类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'she_shi_lei_xing' AND dict_value = '3');
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9104, 'she_shi_lei_xing', '设施类型', '通讯设施', '4', 4, '公共基础设施-设施类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'she_shi_lei_xing' AND dict_value = '4');
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9105, 'she_shi_lei_xing', '设施类型', '公共服务', '5', 5, '公共基础设施-设施类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'she_shi_lei_xing' AND dict_value = '5');
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9106, 'she_shi_lei_xing', '设施类型', '环卫设施', '6', 6, '公共基础设施-设施类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'she_shi_lei_xing' AND dict_value = '6');

-- ----------------------------
-- 字典：附件类型 fu_jian_lei_xing
-- ----------------------------
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9201, 'fu_jian_lei_xing', '附件类型', '照片', '1', 1, '公共基础设施-附件类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'fu_jian_lei_xing' AND dict_value = '1');
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9202, 'fu_jian_lei_xing', '附件类型', '合同', '2', 2, '公共基础设施-附件类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'fu_jian_lei_xing' AND dict_value = '2');
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9203, 'fu_jian_lei_xing', '附件类型', '图纸', '3', 3, '公共基础设施-附件类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'fu_jian_lei_xing' AND dict_value = '3');
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9204, 'fu_jian_lei_xing', '附件类型', '检测报告', '4', 4, '公共基础设施-附件类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'fu_jian_lei_xing' AND dict_value = '4');
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9205, 'fu_jian_lei_xing', '附件类型', '其他', '5', 5, '公共基础设施-附件类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'fu_jian_lei_xing' AND dict_value = '5');

-- ----------------------------
-- 菜单：基础信息管理 → 公共基础设施（含按钮权限）
-- 基础信息管理父菜单ID = 1001
-- ----------------------------
INSERT IGNORE INTO t_sys_menu (id, parent_id, menu_name, menu_type, path, perm, icon, sort) VALUES
(1107, 1001, '公共基础设施', 'C', '/base/facility', 'base:facility:list', 'Connection', 2),
(1271, 1107, '设施新增', 'F', NULL, 'base:facility:add', NULL, 1),
(1272, 1107, '设施编辑', 'F', NULL, 'base:facility:edit', NULL, 2),
(1273, 1107, '设施删除', 'F', NULL, 'base:facility:delete', NULL, 3);

-- 超级管理员默认拥有新菜单权限
INSERT IGNORE INTO t_sys_role_menu (id, role_id, menu_id) VALUES
(30011, 1001, 1107),
(30012, 1001, 1271),
(30013, 1001, 1272),
(30014, 1001, 1273);

-- ----------------------------
-- 示例项目数据
-- ----------------------------
INSERT IGNORE INTO t_project (id, project_code, project_name, project_type, progress, budget, status, start_year, region_code) VALUES
(5001, 'PRJ2024001', '城关村自来水管道改造', '1', 85.00, 580000.00, '进行中', 2024, '110101100001'),
(5002, 'PRJ2024002', '幸福村道路硬化工程', '2', 100.00, 1200000.00, '已完成', 2024, '110101200001'),
(5003, 'PRJ2025001', '西关村电网升级改造', '3', 30.00, 2100000.00, '进行中', 2025, '110101100002');

-- ----------------------------
-- 示例公共基础设施数据
-- ----------------------------
INSERT IGNORE INTO t_base_facility
(id, facility_code, facility_name, facility_type, region_code, region_name, location, build_year, quantity, unit, remark) VALUES
(6001, 'SS2024001', '城关村主供水管道', '1', '110101100001', '城关村', '城关村主干道东侧', 2018, 2.50, '公里', 'PE管，管径300mm'),
(6002, 'SS2024002', '幸福村1号变压器', '3', '110101200001', '幸福村', '幸福村村委会院内', 2020, 1.00, '台', '200KVA油浸式变压器'),
(6003, 'SS2024003', '西关村文化广场', '5', '110101100002', '西关村', '西关村中心街', 2019, 500.00, '平方米', '含健身器材一套'),
(6004, 'SS2024004', '城关村公厕', '6', '110101100001', '城关村', '城关村农贸市场旁', 2021, 2.00, '座', '水冲式，男女分开'),
(6005, 'SS2024005', '幸福村主干道', '2', '110101200001', '幸福村', '幸福村南北主干道', 2020, 1.20, '公里', '水泥路面，宽6米'),
(6006, 'SS2024006', '城关村一组垃圾中转箱', '6', '110101100001001', '城关村一组', '城关村一组东头', 2022, 3.00, '个', '240L标准中转箱'),
(6007, 'SS2024007', '幸福村一组通信基站', '4', '110101200001001', '幸福村一组', '幸福村一组后山', 2021, 1.00, '座', '中国移动4G基站');

-- ----------------------------
-- 示例设施-项目关联
-- ----------------------------
INSERT IGNORE INTO t_base_facility_project (id, facility_id, project_id) VALUES
(7001, 6001, 5001),
(7002, 6005, 5002),
(7003, 6002, 5003);
