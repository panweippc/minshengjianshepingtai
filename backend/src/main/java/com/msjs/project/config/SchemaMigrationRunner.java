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
        migrateSnapshotHouseholdNo();
        fixApprovalDetailTargetCode();
        migrateApprovalProjectType();
        ensureHomeMenu();
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
}
