-- ============================================================
-- 民生建设项目管理平台 变更脚本 V3
-- 内容：基础信息管理新增二级菜单「村委会信息」
--       1. 行政区划表扩展村委会字段（区划类型/党支部书记/村委会主任）
--       2. 区划类型字典 qu_hua_lei_xing（乡镇/村/组）
--       3. 菜单与按钮权限
--       4. 示例区划数据（乡镇/村/组）
-- 字符集：utf8mb4；主键：雪花ID（BIGINT，禁止自增）
-- 说明：本脚本为一次性变更脚本，请按 V1 → V2 → V3 顺序执行
-- ============================================================
USE msjs;

-- ----------------------------
-- 行政区划表扩展：编码长度支持到组（15位），新增村委会字段
-- 层级扩展为：1省 2市 3县区 4乡镇 5村 6组
-- ----------------------------
ALTER TABLE t_sys_region
    MODIFY COLUMN code VARCHAR(20) NOT NULL COMMENT '行政区划代码（省市区6位/乡镇9位/村12位/组15位）',
    MODIFY COLUMN level TINYINT NOT NULL COMMENT '层级：1省 2市 3县区 4乡镇 5村 6组',
    ADD COLUMN region_type VARCHAR(8) NULL COMMENT '区划类型（字典 qu_hua_lei_xing：1乡镇 2村 3组）' AFTER level,
    ADD COLUMN secretary VARCHAR(32) NULL COMMENT '党支部书记' AFTER region_type,
    ADD COLUMN secretary_phone VARCHAR(20) NULL COMMENT '党支部书记电话' AFTER secretary,
    ADD COLUMN director VARCHAR(32) NULL COMMENT '村委会主任' AFTER secretary_phone,
    ADD COLUMN director_phone VARCHAR(20) NULL COMMENT '村委会主任电话' AFTER director;

-- 用户所属区划代码同步扩展，保证村/组级数据权限可用
ALTER TABLE t_sys_user
    MODIFY COLUMN region_code VARCHAR(20) NULL COMMENT '所属行政区划代码（数据权限用）';

-- ----------------------------
-- 字典：区划类型 qu_hua_lei_xing（供新增/编辑下拉联动）
-- 兼容库中已存在同类型字典项的环境，避免重复插入
-- ----------------------------
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9001, 'qu_hua_lei_xing', '区划类型', '乡镇', '1', 1, '村委会信息-区划类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'qu_hua_lei_xing' AND dict_value = '1');
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9002, 'qu_hua_lei_xing', '区划类型', '村', '2', 2, '村委会信息-区划类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'qu_hua_lei_xing' AND dict_value = '2');
INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort, remark)
SELECT 9003, 'qu_hua_lei_xing', '区划类型', '组', '3', 3, '村委会信息-区划类型'
WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'qu_hua_lei_xing' AND dict_value = '3');

-- ----------------------------
-- 菜单：基础信息管理 → 村委会信息（含按钮权限）
-- ----------------------------
INSERT IGNORE INTO t_sys_menu (id, parent_id, menu_name, menu_type, path, perm, icon, sort) VALUES
(1106, 1001, '村委会信息', 'C', '/base/village', 'base:village:list', 'OfficeBuilding', 1),
(1261, 1106, '村委会新增', 'F', NULL, 'base:village:add', NULL, 1),
(1262, 1106, '村委会编辑', 'F', NULL, 'base:village:edit', NULL, 2),
(1263, 1106, '村委会删除', 'F', NULL, 'base:village:delete', NULL, 3);

-- 超级管理员默认拥有新菜单权限
INSERT IGNORE INTO t_sys_role_menu (id, role_id, menu_id) VALUES
(30001, 1001, 1106),
(30002, 1001, 1261),
(30003, 1001, 1262),
(30004, 1001, 1263);

-- ----------------------------
-- 示例区划数据：乡镇 → 村 → 组（挂在东城区 110101 下）
-- ----------------------------
INSERT IGNORE INTO t_sys_region
(id, code, name, parent_code, level, region_type, secretary, secretary_phone, director, director_phone, sort, deleted) VALUES
(2001, '110101100', '城关镇', '110101', 4, '1', '张建国', '13800000001', '李国强', '13800000002', 1, 0),
(2002, '110101200', '幸福镇', '110101', 4, '1', '王卫东', '13800000003', '赵春梅', '13800000004', 2, 0),
(2003, '110101100001', '城关村', '110101100', 5, '2', '刘志强', '13900000001', '陈桂兰', '13900000002', 1, 0),
(2004, '110101100002', '西关村', '110101100', 5, '2', '孙建军', '13900000003', '周秀英', '13900000004', 2, 0),
(2005, '110101200001', '幸福村', '110101200', 5, '2', '马志刚', '13700000001', '吴玉兰', '13700000002', 1, 0),
(2006, '110101100001001', '城关村一组', '110101100001', 6, '3', '郑海涛', '13600000001', '冯秀兰', '13600000002', 1, 0),
(2007, '110101200001001', '幸福村一组', '110101200001', 6, '3', '许文斌', '13500000001', '何丽娟', '13500000002', 1, 0);

-- 普通角色如需村委会信息权限，可在「角色管理-分配权限」中勾选
