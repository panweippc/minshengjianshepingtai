-- ============================================================
-- 民生建设项目管理平台 变更脚本 V2
-- 内容：系统管理新增二级菜单「字典管理」（数据字典表 + 菜单 + 按钮权限）
-- 字符集：utf8mb4；主键：雪花ID（BIGINT，禁止自增）
-- ============================================================
USE msjs;

-- ----------------------------
-- 数据字典表（同一字典类型下键值唯一，由服务层校验）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_sys_dict (
    id          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    dict_type   VARCHAR(64)  NOT NULL COMMENT '字典类型',
    description VARCHAR(128) NULL COMMENT '字典描述',
    dict_label  VARCHAR(64)  NOT NULL COMMENT '字典标签',
    dict_value  VARCHAR(64)  NOT NULL COMMENT '字典键值',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (id),
    KEY idx_dict_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据字典表';

-- ----------------------------
-- 菜单：系统管理 → 字典管理（含按钮权限）
-- ----------------------------
INSERT IGNORE INTO t_sys_menu (id, parent_id, menu_name, menu_type, path, perm, icon, sort) VALUES
(1105, 1005, '字典管理', 'C', '/system/dict', 'system:dict:list', 'Collection', 5),
(1241, 1105, '字典新增', 'F', NULL, 'system:dict:add', NULL, 1),
(1242, 1105, '字典编辑', 'F', NULL, 'system:dict:edit', NULL, 2),
(1243, 1105, '字典删除', 'F', NULL, 'system:dict:delete', NULL, 3);

-- 普通角色如需字典管理权限，可在「角色管理-分配权限」中勾选；超级管理员默认可见全部菜单
