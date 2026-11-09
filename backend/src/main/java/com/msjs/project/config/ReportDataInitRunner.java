package com.msjs.project.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.entity.MaterialReport;
import com.msjs.project.entity.MaterialReportItem;
import com.msjs.project.entity.SysMenu;
import com.msjs.project.entity.SysRoleMenu;
import com.msjs.project.mapper.MaterialReportItemMapper;
import com.msjs.project.mapper.MaterialReportMapper;
import com.msjs.project.mapper.SysMenuMapper;
import com.msjs.project.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 统计报表模块数据初始化
 * 启动时自动建表（幂等 CREATE TABLE IF NOT EXISTS）、写入菜单及按钮权限
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportDataInitRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final MaterialReportMapper reportMapper;
    private final MaterialReportItemMapper reportItemMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        // 1. 建表
        createTablesIfNotExist();
        // 2. 初始化字典（baobiao_leixing 报表类型）
        initDict();
        // 3. 初始化菜单
        initMenu();
    }

    /**
     * 初始化报表类型字典（baobiao_leixing）
     * 三条记录：盘点表(INVENTORY)、交接表(HANDOVER)、损毁表(DAMAGE)
     * 按 dict_type + dict_value 幂等判断，避免重复插入
     */
    private void initDict() {
        jdbcTemplate.update("""
            INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort)
            SELECT 3301, 'baobiao_leixing', '报表类型', '盘点表', 'INVENTORY', 1
            WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'baobiao_leixing' AND dict_value = 'INVENTORY' AND deleted = 0)
        """);
        jdbcTemplate.update("""
            INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort)
            SELECT 3302, 'baobiao_leixing', '报表类型', '交接表', 'HANDOVER', 2
            WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'baobiao_leixing' AND dict_value = 'HANDOVER' AND deleted = 0)
        """);
        jdbcTemplate.update("""
            INSERT INTO t_sys_dict (id, dict_type, description, dict_label, dict_value, sort)
            SELECT 3303, 'baobiao_leixing', '报表类型', '损毁表', 'DAMAGE', 3
            WHERE NOT EXISTS (SELECT 1 FROM t_sys_dict WHERE dict_type = 'baobiao_leixing' AND dict_value = 'DAMAGE' AND deleted = 0)
        """);
        log.info("报表类型字典（baobiao_leixing）初始化完成");
    }

    private void createTablesIfNotExist() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS t_material_report (
                id               BIGINT       NOT NULL COMMENT '主键（雪花ID）',
                report_name      VARCHAR(128) NOT NULL COMMENT '报表名称',
                report_type      VARCHAR(32)  NOT NULL COMMENT '报表类型：INVENTORY/HANDOVER/DAMAGE',
                report_date      DATE         NULL COMMENT '报表日期',
                handler          VARCHAR(32)  NULL COMMENT '经办人',
                handover_person  VARCHAR(32)  NULL COMMENT '交接人',
                auditor          VARCHAR(32)  NULL COMMENT '审核人',
                department       VARCHAR(64)  NULL COMMENT '部门',
                create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                deleted          TINYINT      NOT NULL DEFAULT 0,
                PRIMARY KEY (id),
                KEY idx_report_name (report_name),
                KEY idx_report_type (report_type),
                KEY idx_report_date (report_date)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统计报表主表'
        """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS t_material_report_item (
                id               BIGINT       NOT NULL COMMENT '主键（雪花ID）',
                report_id        BIGINT       NOT NULL COMMENT '所属报表ID',
                warehouse_name   VARCHAR(128) NULL COMMENT '仓库名称',
                item_name        VARCHAR(128) NOT NULL COMMENT '物品名称',
                unit             VARCHAR(16)  NULL COMMENT '单位',
                book_qty         INT          NOT NULL DEFAULT 0 COMMENT '账面数量',
                actual_qty       INT          NULL COMMENT '盘点数量',
                profit_qty       INT          NULL COMMENT '盘盈数量',
                handler          VARCHAR(32)  NULL COMMENT '明细经办人',
                remark           VARCHAR(255) NULL COMMENT '备注',
                create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                deleted          TINYINT      NOT NULL DEFAULT 0,
                PRIMARY KEY (id),
                KEY idx_report_item_report (report_id),
                KEY idx_report_item_name (item_name),
                KEY idx_report_item_warehouse (warehouse_name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统计报表明细表'
        """);
        log.info("统计报表模块表结构初始化完成");
    }

    private void initMenu() {
        // 找物资管理目录的菜单ID
        SysMenu materialDir = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuName, "物资管理")
                .eq(SysMenu::getParentId, 0L)
                .eq(SysMenu::getDeleted, 0));
        if (materialDir == null) {
            log.warn("未找到「物资管理」目录菜单，跳过统计表菜单初始化");
            return;
        }

        // 检查表菜单是否已存在
        SysMenu existMenu = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuName, "统计表")
                .eq(SysMenu::getDeleted, 0));
        if (existMenu != null) {
            // 已存在则确保 sort 在出库单之后（出库单 sort=4，统计表 sort=5）
            if (existMenu.getSort() != 5) {
                existMenu.setSort(5);
                existMenu.setParentId(materialDir.getId());
                menuMapper.updateById(existMenu);
                log.info("统计表菜单已存在，已更新 sort=5");
            }
            return;
        }

        // 插入菜单
        SysMenu menu = new SysMenu();
        menu.setId(1020L);
        menu.setParentId(materialDir.getId());
        menu.setMenuName("统计表");
        menu.setMenuType("C");
        menu.setPath("/material/statistics");
        menu.setPerm("material:report:list");
        menu.setIcon("DataAnalysis");
        menu.setSort(5);
        menu.setStatus(1);
        menuMapper.insert(menu);

        // 按钮权限
        insertButton(1021L, menu.getId(), "统计表新增", "material:report:add", 1);
        insertButton(1022L, menu.getId(), "统计表编辑", "material:report:edit", 2);
        insertButton(1023L, menu.getId(), "统计表删除", "material:report:delete", 3);

        // 给区级经办人(role_id=1002)授予权限
        grantRoleMenu(1002L, 1020L);
        grantRoleMenu(1002L, 1021L);
        grantRoleMenu(1002L, 1022L);
        grantRoleMenu(1002L, 1023L);

        log.info("统计表菜单及权限初始化完成");
    }

    private void insertButton(Long id, Long parentId, String name, String perm, int sort) {
        SysMenu exist = menuMapper.selectById(id);
        if (exist != null) return;
        SysMenu btn = new SysMenu();
        btn.setId(id);
        btn.setParentId(parentId);
        btn.setMenuName(name);
        btn.setMenuType("F");
        btn.setPerm(perm);
        btn.setSort(sort);
        btn.setStatus(1);
        menuMapper.insert(btn);
    }

    private void grantRoleMenu(Long roleId, Long menuId) {
        Long exist = roleMenuMapper.selectCount(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId)
                .eq(SysRoleMenu::getMenuId, menuId));
        if (exist != null && exist > 0) return;
        SysRoleMenu rm = new SysRoleMenu();
        rm.setRoleId(roleId);
        rm.setMenuId(menuId);
        roleMenuMapper.insert(rm);
    }
}
