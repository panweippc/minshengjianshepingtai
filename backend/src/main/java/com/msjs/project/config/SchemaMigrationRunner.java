package com.msjs.project.config;

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
 * 跑完可保留，再次启动会被幂等检查短路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        migrateSnapshotHouseholdNo();
        fixApprovalDetailTargetCode();
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
}
