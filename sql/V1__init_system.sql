-- ============================================================
-- 民生建设项目管理平台 初始化脚本 V1
-- 内容：系统管理（用户/角色/菜单/操作日志/行政区划）+ 种子数据
-- 字符集：utf8mb4；主键：雪花ID（BIGINT，禁止自增）
-- ============================================================
USE msjs;

-- ----------------------------
-- 行政区划表（行级数据隔离基础）
-- ----------------------------
DROP TABLE IF EXISTS t_sys_region;
CREATE TABLE t_sys_region (
    id          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    code        VARCHAR(12)  NOT NULL COMMENT '行政区划代码（GB/T 2260，6位）',
    name        VARCHAR(64)  NOT NULL COMMENT '区划名称',
    parent_code VARCHAR(12)  NULL COMMENT '上级区划代码',
    level       TINYINT      NOT NULL COMMENT '层级：1省 2市 3县区',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_region_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '行政区划表';

-- ----------------------------
-- 系统用户表
-- ----------------------------
DROP TABLE IF EXISTS t_sys_user;
CREATE TABLE t_sys_user (
    id          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    username    VARCHAR(32)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name   VARCHAR(32)  NOT NULL COMMENT '姓名',
    region_code VARCHAR(12)  NULL COMMENT '所属行政区划代码（数据权限用）',
    phone       VARCHAR(20)  NULL COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

-- ----------------------------
-- 系统角色表
-- ----------------------------
DROP TABLE IF EXISTS t_sys_role;
CREATE TABLE t_sys_role (
    id          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    role_name   VARCHAR(32)  NOT NULL COMMENT '角色名称',
    role_key    VARCHAR(32)  NOT NULL COMMENT '角色标识',
    data_scope  TINYINT      NOT NULL DEFAULT 3 COMMENT '数据权限范围：1全部 2本区及以下 3仅本区',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统角色表';

-- ----------------------------
-- 用户-角色关联表
-- ----------------------------
DROP TABLE IF EXISTS t_sys_user_role;
CREATE TABLE t_sys_user_role (
    id      BIGINT NOT NULL COMMENT '主键（雪花ID）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    KEY idx_ur_user (user_id),
    KEY idx_ur_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户-角色关联表';

-- ----------------------------
-- 系统菜单表（操作权限载体：目录/菜单/按钮）
-- ----------------------------
DROP TABLE IF EXISTS t_sys_menu;
CREATE TABLE t_sys_menu (
    id          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为根',
    menu_name   VARCHAR(32)  NOT NULL COMMENT '菜单名称',
    menu_type   CHAR(1)      NOT NULL COMMENT '类型：M目录 C菜单 F按钮',
    path        VARCHAR(128) NULL COMMENT '路由地址',
    perm        VARCHAR(64)  NULL COMMENT '权限标识，如 system:user:list',
    icon        VARCHAR(32)  NULL COMMENT '图标',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统菜单表';

-- ----------------------------
-- 角色-菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS t_sys_role_menu;
CREATE TABLE t_sys_role_menu (
    id      BIGINT NOT NULL COMMENT '主键（雪花ID）',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (id),
    KEY idx_rm_role (role_id),
    KEY idx_rm_menu (menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色-菜单关联表';

-- ----------------------------
-- 操作日志表（全量留存，全程可追溯）
-- ----------------------------
DROP TABLE IF EXISTS t_sys_oper_log;
CREATE TABLE t_sys_oper_log (
    id          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    user_id     BIGINT       NULL COMMENT '操作人ID',
    username    VARCHAR(32)  NULL COMMENT '操作人账号',
    module      VARCHAR(32)  NULL COMMENT '所属模块',
    operation   VARCHAR(64)  NULL COMMENT '操作描述',
    method      VARCHAR(10)  NULL COMMENT 'HTTP方法',
    url         VARCHAR(255) NULL COMMENT '请求地址',
    ip          VARCHAR(64)  NULL COMMENT '操作IP',
    params      TEXT         NULL COMMENT '请求参数（截断存储）',
    result_code INT          NULL COMMENT '返回状态码',
    error_msg   TEXT         NULL COMMENT '异常信息',
    oper_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    cost_ms     BIGINT       NULL COMMENT '耗时（毫秒）',
    PRIMARY KEY (id),
    KEY idx_log_user (user_id),
    KEY idx_log_time (oper_time),
    KEY idx_log_module (module)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志表';

-- ============================================================
-- 种子数据
-- ============================================================

-- 行政区划示例数据
INSERT INTO t_sys_region (id, code, name, parent_code, level, sort) VALUES
(1001, '110000', '北京市', NULL, 1, 1),
(1002, '110101', '东城区', '110000', 3, 1),
(1003, '110102', '西城区', '110000', 3, 2),
(1004, '120000', '天津市', NULL, 1, 2),
(1005, '120101', '和平区', '120000', 3, 1),
(1006, '120102', '河东区', '120000', 3, 2);

-- 角色：超级管理员（数据权限=全部）
INSERT INTO t_sys_role (id, role_name, role_key, data_scope, remark) VALUES
(1001, '超级管理员', 'admin', 1, '内置角色，拥有全部权限'),
(1002, '区级经办人', 'operator', 3, '仅可操作本行政区划数据');

-- 菜单：五大模块 + 系统管理子项
INSERT INTO t_sys_menu (id, parent_id, menu_name, menu_type, path, perm, icon, sort) VALUES
(1001, 0, '基础信息管理', 'M', '/base', NULL, 'Folder', 1),
(1002, 0, '事项申报管理', 'M', '/declare', NULL, 'Document', 2),
(1003, 0, '项目全周期管控', 'M', '/project', NULL, 'SetUp', 3),
(1004, 0, '档案管理', 'M', '/archive', NULL, 'Files', 4),
(1005, 0, '系统管理', 'M', '/system', NULL, 'Setting', 5),
-- 系统管理子菜单
(1101, 1005, '用户管理', 'C', '/system/user', 'system:user:list', 'User', 1),
(1102, 1005, '角色管理', 'C', '/system/role', 'system:role:list', 'UserFilled', 2),
(1103, 1005, '菜单管理', 'C', '/system/menu', 'system:menu:list', 'Menu', 3),
(1104, 1005, '操作日志', 'C', '/system/log', 'system:log:list', 'List', 4),
-- 用户管理按钮
(1201, 1101, '用户新增', 'F', NULL, 'system:user:add', NULL, 1),
(1202, 1101, '用户编辑', 'F', NULL, 'system:user:edit', NULL, 2),
(1203, 1101, '用户删除', 'F', NULL, 'system:user:delete', NULL, 3),
(1204, 1101, '分配角色', 'F', NULL, 'system:user:assign', NULL, 4),
-- 角色管理按钮
(1211, 1102, '角色新增', 'F', NULL, 'system:role:add', NULL, 1),
(1212, 1102, '角色编辑', 'F', NULL, 'system:role:edit', NULL, 2),
(1213, 1102, '角色删除', 'F', NULL, 'system:role:delete', NULL, 3),
(1214, 1102, '分配权限', 'F', NULL, 'system:role:assign', NULL, 4),
-- 菜单管理按钮
(1221, 1103, '菜单新增', 'F', NULL, 'system:menu:add', NULL, 1),
(1222, 1103, '菜单编辑', 'F', NULL, 'system:menu:edit', NULL, 2),
(1223, 1103, '菜单删除', 'F', NULL, 'system:menu:delete', NULL, 3),
-- 日志按钮
(1231, 1104, '日志删除', 'F', NULL, 'system:log:delete', NULL, 1);

-- 超级管理员拥有全部菜单
INSERT INTO t_sys_role_menu (id, role_id, menu_id)
SELECT 10000 + id, 1001, id FROM t_sys_menu;
-- 区级经办人默认仅查询权限（示例：系统管理不可见，业务模块可见目录）
INSERT INTO t_sys_role_menu (id, role_id, menu_id) VALUES
(20001, 1002, 1001), (20002, 1002, 1002), (20003, 1002, 1003), (20004, 1002, 1004);
