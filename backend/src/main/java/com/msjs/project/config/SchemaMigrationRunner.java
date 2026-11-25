package com.msjs.project.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.entity.SysMenu;
import com.msjs.project.entity.SysRoleMenu;
import com.msjs.project.mapper.SysMenuMapper;
import com.msjs.project.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 一次性数据库迁移（幂等）
 * 变更内容：
 *   1. t_declaration_household 新增 snapshot_household_no 列并回填户号
 *   2. t_project_approval_detail 历史数据中农户 target_code 由身份证号修正为户号
 *   3. t_project_approval 新增 project_type 列 + 字典初始化（V15）
 *   4. 左侧菜单新增一级菜单「首页」（V15）
 * 跑完可保留，再次启动会被幂等检查短路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigrationRunner implements ApplicationRunner {

    /** 角色ID：区级经办人（首页对其同样可见） */
    private static final long ROLE_OPERATOR = 1002L;

    private final JdbcTemplate jdbcTemplate;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public void run(ApplicationArguments args) {
        normalizeApprovalStatusCase();
        if (tableExists("t_declaration_household") && tableExists("t_base_household")) {
            migrateSnapshotHouseholdNo();
        } else {
            log.info("[Migration] t_declaration_household 或 t_base_household 不存在，跳过申报明细迁移");
        }
        if (tableExists("t_declaration_household") && tableExists("t_project_approval_detail")) {
            fixApprovalDetailTargetCode();
        } else {
            log.info("[Migration] 依赖表不存在，跳过立项明细 target_code 修正");
        }
        migrateApprovalProjectType();
        ensureHomeMenu();
        dropSysFileSha256Unique();
        ensureMissingPermMenus();
    }

    /** 判断指定表是否存在于当前数据库 */
    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?", Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 立项状态大小写归一化：存量数据 status 可能存了小写 pending/approved/rejected，
     * 与 Java 常量 APPROVAL_STATUS_PENDING(大写) 做 equals 比较会失败，
     * 导致无法驳回/通过。统一改为大写。
     * 注意：MySQL 默认 collation(utf8mb4_general_ci) 大小写不敏感，
     * IN/比较运算都要用 BINARY 才能精确匹配小写存量。
     */
    private void normalizeApprovalStatusCase() {
        int affected = jdbcTemplate.update(
                "UPDATE t_project_approval SET status = UPPER(status) " +
                "WHERE BINARY status IN ('pending','approved','rejected') " +
                "AND BINARY status <> UPPER(status)");
        if (affected > 0) {
            log.info("[Migration] 已将 t_project_approval.status 归一化为大写（{} 条）", affected);
        } else {
            log.info("[Migration] 立项状态已是大写，无需归一化");
        }
    }

    /** 步骤1：申报-农户明细表补 snapshot_household_no */
    private void migrateSnapshotHouseholdNo() {
        // 幂等检查：列是否已存在
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_declaration_household' " +
                "AND COLUMN_NAME = 'snapshot_household_no'", Integer.class);
        if (count != null && count > 0) {
            log.info("[Migration] snapshot_household_no 列已存在，跳过 ADD COLUMN");
        } else {
            try {
                jdbcTemplate.update(
                        "ALTER TABLE t_declaration_household " +
                        "ADD COLUMN snapshot_household_no VARCHAR(32) NULL COMMENT '快照-户号（业务编码，保存时从 t_base_household 冗余）' " +
                        "AFTER snapshot_head_name");
                log.info("[Migration] 已为 t_declaration_household 新增 snapshot_household_no 列");
            } catch (Exception e) {
                log.warn("[Migration] ALTER TABLE 失败：{}", e.getMessage());
                return;
            }
        }

        // 步骤1b：回填已有申报明细的户号（从农户档案表反查）
        int affected = jdbcTemplate.update(
                "UPDATE t_declaration_household dh " +
                "INNER JOIN t_base_household bh ON bh.id = dh.household_id " +
                "SET dh.snapshot_household_no = bh.household_no " +
                "WHERE dh.snapshot_household_no IS NULL AND bh.household_no IS NOT NULL");
        if (affected > 0) {
            log.info("[Migration] 已回填 {} 条申报农户明细的户号", affected);
        } else {
            log.info("[Migration] 所有申报农户明细的户号均已回填，无需更新");
        }
    }

    /** 步骤2：立项明细历史数据农户 target_code 修正为户号 */
    private void fixApprovalDetailTargetCode() {
        // 先确保 snapshot_household_no 已回填（若刚跑完上一段，这里只是再保险）
        jdbcTemplate.update(
                "UPDATE t_declaration_household dh " +
                "INNER JOIN t_base_household bh ON bh.id = dh.household_id " +
                "SET dh.snapshot_household_no = bh.household_no " +
                "WHERE dh.snapshot_household_no IS NULL AND bh.household_no IS NOT NULL");

        int affected = jdbcTemplate.update(
                "UPDATE t_project_approval_detail pad " +
                "INNER JOIN t_declaration_household dh ON dh.id = pad.household_id " +
                "SET pad.target_code = dh.snapshot_household_no " +
                "WHERE pad.detail_type = 'HOUSEHOLD' " +
                "  AND dh.snapshot_household_no IS NOT NULL " +
                "  AND pad.target_code <> dh.snapshot_household_no");
        if (affected > 0) {
            log.info("[Migration] 已修正 {} 条立项农户明细的 target_code（身份证号 → 户号）", affected);
        } else {
            log.info("[Migration] 立项农户明细 target_code 已是户号，无需修正");
        }
    }

    /**
     * 立项主表补 project_type 列并初始化字典数据（V15）。
     * 修复前端"一级表单项目类型取不到数据"——之前前端错误地把 project_source
     * （项目来源字典 xiang_mu_lai_yuan）当成项目类型字典 xiang_mu_lei_xing 去查，
     * 且数据库确实缺失 project_type 列。
     */
    private void migrateApprovalProjectType() {
        // 幂等检查：project_type 列是否已存在
        Integer colExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_project_approval' " +
                "AND COLUMN_NAME = 'project_type'", Integer.class);

        if (colExists != null && colExists > 0) {
            log.info("[Migration] project_type 列已存在，跳过 ADD COLUMN");
        } else {
            try {
                jdbcTemplate.update(
                        "ALTER TABLE t_project_approval ADD COLUMN project_type VARCHAR(8) DEFAULT NULL " +
                        "COMMENT '项目类型（字典 xiang_mu_lei_xing）' AFTER project_category");
                log.info("[Migration] 已为 t_project_approval 新增 project_type 列");
            } catch (Exception e) {
                log.warn("[Migration] ALTER TABLE 失败：{}", e.getMessage());
                return;
            }
        }

        // 回填已有立项的 project_type（按 project_category 做合理默认值）
        int filled = jdbcTemplate.update(
                "UPDATE t_project_approval SET project_type = '1' " +
                "WHERE project_type IS NULL AND project_category IN ('1','2')");
        jdbcTemplate.update(
                "UPDATE t_project_approval SET project_type = '2' " +
                "WHERE project_type IS NULL AND project_category = '3'");
        if (filled > 0) {
            log.info("[Migration] 已回填 {} 条立项记录的 project_type", filled);
        } else {
            log.info("[Migration] 立项记录的 project_type 已全部回填，无需更新");
        }
    }

    /**
     * 左侧菜单新增一级菜单「首页」：sort=0 置于「基础信息管理」等一级目录之前。
     * 幂等：已存在则仅校正排序，不存在才插入，可重复启动不产生重复数据。
     */
    private void ensureHomeMenu() {
        SysMenu home = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuName, "首页")
                .eq(SysMenu::getParentId, 0L)
                .last("LIMIT 1"));
        if (home == null) {
            home = new SysMenu();
            home.setParentId(0L);
            home.setMenuName("首页");
            home.setMenuType("C");
            home.setPath("/home");
            home.setIcon("HomeFilled");
            home.setSort(0);
            home.setStatus(1);
            menuMapper.insert(home);
            log.info("[Migration] 已新增一级菜单「首页」");
        } else if (home.getSort() == null || home.getSort() != 0) {
            home.setSort(0);
            home.setIcon("HomeFilled");
            home.setPath("/home");
            menuMapper.updateById(home);
            log.info("[Migration] 已校正一级菜单「首页」排序为 0");
        } else {
            log.info("[Migration] 一级菜单「首页」已存在，无需处理");
        }

        // 区级经办人授予首页菜单，保证非管理员登录同样可见
        Long granted = roleMenuMapper.selectCount(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, ROLE_OPERATOR)
                .eq(SysRoleMenu::getMenuId, home.getId()));
        if (granted == null || granted == 0) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(ROLE_OPERATOR);
            rm.setMenuId(home.getId());
            roleMenuMapper.insert(rm);
            log.info("[Migration] 已为区级经办人授予「首页」菜单");
        }
    }

    /**
     * 业务变更：相同文件允许重复上传，删除 t_sys_file.sha256 上的唯一索引。
     * 保留普通索引即可（若后续查询性能不足可按需改为普通索引）。
     * 幂等：索引不存在会被 MySQL 报错，catch 后仅 info 记录。
     */
    private void dropSysFileSha256Unique() {
        // 先查询该列上是否存在唯一索引（不同建表脚本可能起了不同名字，遍历所有 UQ 类型索引）
        try {
            Integer uniqueCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_sys_file' " +
                    "AND COLUMN_NAME = 'sha256' AND NON_UNIQUE = 0", Integer.class);
            if (uniqueCount == null || uniqueCount == 0) {
                log.info("[Migration] t_sys_file.sha256 上不存在唯一索引，跳过");
                return;
            }
            // 查出具体索引名（可能是 uk_sha256，也可能是其他脚本起的名字）
            java.util.List<String> idxNames = jdbcTemplate.queryForList(
                    "SELECT DISTINCT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_sys_file' " +
                    "AND COLUMN_NAME = 'sha256' AND NON_UNIQUE = 0", String.class);
            for (String idx : idxNames) {
                try {
                    jdbcTemplate.update("ALTER TABLE t_sys_file DROP INDEX `" + idx + "`");
                    log.info("[Migration] 已删除 t_sys_file.sha256 上的唯一索引：{}", idx);
                } catch (Exception e) {
                    log.warn("[Migration] DROP INDEX {} 失败：{}", idx, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[Migration] 查询 INFORMATION_SCHEMA 失败：{}", e.getMessage());
        }
    }

    /**
     * 一次性补全 SQL 初始化脚本里缺失的按钮权限菜单。
     *
     * 背景：后端 Controller 的 @RequirePerm 用了以下语义化 key，
     * 但 V6/V19 等 SQL 脚本按 CRUD 习惯只建了 add/edit/delete，两边完全对不上，
     * 导致镇级/区级/村级账号登录后"验收""归档""登记""下拨"等按钮全部 403。
     *
     * 本次补全清单（必须全部插入，按钮类型 F）：
     * ┌──────────┬──────────────────────────────────────────────────────┐
     * │ 父菜单   │ 缺失的 perm key                                      │
     * ├──────────┼──────────────────────────────────────────────────────┤
     * │ 验收管理 │ acceptance:accept, acceptance:archive                │
     * │ 资金管理 │ fund:view, fund:register, fund:allocate               │
     * │ 施工管理 │ construction:list（菜单级已存在，无需补）              │
     * │ 档案管理 │ archive:view, archive:edit, archive:export            │
     * └──────────┴──────────────────────────────────────────────────────┘
     *
     * 策略：
     *   1) 用 JDBC 查现有 perm 集合，缺失才 INSERT；
     *   2) 找到正确的父菜单（按路径或 perm 匹配），避免 parent_id 硬编码冲突；
     *   3) 给数据库里**所有现有角色**自动授一遍新菜单（已存在 role_menu 记录跳过）。
     *      理由：后端已加 region_code 数据权限兜底，多授按钮不会造成跨镇越权，
     *            反而能让镇级/村级/区级各角色都拿到自己需要的按钮，不用逐个角色手动配。
     */
    private void ensureMissingPermMenus() {
        if (!tableExists("t_sys_menu") || !tableExists("t_sys_role_menu")) {
            log.info("[Migration] 系统菜单表不存在，跳过权限补全");
            return;
        }

        // 现有 perm 集合
        java.util.Set<String> existingPerms = new java.util.HashSet<>(jdbcTemplate.queryForList(
                "SELECT perm FROM t_sys_menu WHERE perm IS NOT NULL AND perm <> ''", String.class));

        // 目标补全清单：(perm, 菜单显示名, 父菜单匹配条件)
        // 用 List.of + Map.of 方便后续处理，避免硬编码 ID
        java.util.List<String[]> plan = java.util.List.of(
                // 验收管理（父：path=/project/acceptance 或 perm 含 acceptance）
                new String[]{"project:acceptance:accept", "验收", "project:acceptance"},
                new String[]{"project:acceptance:archive", "归档", "project:acceptance"},
                // 资金管理（父：path=/project/fund）
                new String[]{"project:fund:view", "资金查看", "project:fund"},
                new String[]{"project:fund:register", "资金登记", "project:fund"},
                new String[]{"project:fund:allocate", "资金下拨", "project:fund"},
                // 档案管理（父：path=/archive 或 path=/project/archive）
                new String[]{"archive:view", "档案查看", "archive"},
                new String[]{"archive:edit", "档案编辑", "archive"},
                new String[]{"archive:export", "档案导出", "archive"}
        );

        java.util.List<Long> insertedIds = new java.util.ArrayList<>();
        for (String[] row : plan) {
            String perm = row[0];
            String name = row[1];
            String parentPermLike = row[2];
            if (existingPerms.contains(perm)) {
                continue; // 已存在，跳过
            }
            // 找父菜单 ID：用 perm LIKE 'parentPermLike%' 匹配（验收父菜单 perm 是 project:acceptance:list）
            Long parentId = jdbcTemplate.queryForObject(
                    "SELECT id FROM t_sys_menu WHERE perm LIKE ? LIMIT 1",
                    Long.class, parentPermLike + "%");
            if (parentId == null) {
                // 找不到父菜单就跳过（可能数据库里这个模块都没建）
                log.warn("[Migration] 找不到父菜单 perm LIKE {}%，跳过 {}", parentPermLike, perm);
                continue;
            }
            // 插入菜单：雪花ID用时间戳+随机（避免硬编码冲突），sort 追加到末尾
            long newId = System.currentTimeMillis() * 10000 + (long) (Math.random() * 10000);
            Integer maxSort = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(sort),0) FROM t_sys_menu WHERE parent_id = ?",
                    Integer.class, parentId);
            int sort = (maxSort == null ? 0 : maxSort) + 1;
            jdbcTemplate.update(
                    "INSERT INTO t_sys_menu (id, parent_id, menu_name, menu_type, path, perm, icon, sort, status) " +
                    "VALUES (?, ?, ?, 'F', NULL, ?, NULL, ?, 1)",
                    newId, parentId, name, perm, sort);
            insertedIds.add(newId);
            existingPerms.add(perm);
            log.info("[Migration] 已补按钮权限菜单：perm={}（父id={}，新id={}）", perm, parentId, newId);
        }

        if (insertedIds.isEmpty()) {
            log.info("[Migration] 所有后端 @RequirePerm 对应的菜单均已存在，无需补全");
            return;
        }

        // 批量：给数据库里所有角色授权这些新菜单（幂等）
        java.util.List<Long> roleIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_sys_role WHERE status = 1", Long.class);
        long rmIdBase = System.currentTimeMillis() * 100000;
        int grantedCount = 0;
        for (Long rid : roleIds) {
            for (Long mid : insertedIds) {
                Integer exists = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM t_sys_role_menu WHERE role_id = ? AND menu_id = ?",
                        Integer.class, rid, mid);
                if (exists != null && exists > 0) continue;
                jdbcTemplate.update(
                        "INSERT INTO t_sys_role_menu (id, role_id, menu_id) VALUES (?, ?, ?)",
                        ++rmIdBase, rid, mid);
                grantedCount++;
            }
        }
        log.info("[Migration] 已为 {} 个角色批量授予 {} 条新菜单（插入 {} 条 role_menu 关联）",
                roleIds.size(), insertedIds.size(), grantedCount);
    }
}
