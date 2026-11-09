package com.msjs.project.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.entity.SysDict;
import com.msjs.project.entity.SysMenu;
import com.msjs.project.entity.SysRoleMenu;
import com.msjs.project.mapper.SysDictMapper;
import com.msjs.project.mapper.SysMenuMapper;
import com.msjs.project.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 出库单模块数据初始化
 * 启动时自动建表、插入字典、菜单及权限，保证出库单模块"开箱即用"
 * 所有写入均带幂等判断，可重复执行不产生脏数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockOutDataInitRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final SysDictMapper dictMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /** 角色ID：超级管理员 / 区级经办人 */
    private static final long ROLE_ADMIN = 1001L;
    private static final long ROLE_OPERATOR = 1002L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        createTablesIfNotExist();
        initOutTypeDict();
        initMenus();
        grantRoles();
    }

    // ==================== 建表 ====================

    private void createTablesIfNotExist() {
        // 出库单主表
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS t_material_stock_out (
                    id          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
                    order_name  VARCHAR(128) NOT NULL COMMENT '出库单名称',
                    handler     VARCHAR(32)  NULL COMMENT '经办人',
                    out_date    DATE         NULL COMMENT '出库日期',
                    remark      VARCHAR(255) NULL COMMENT '备注',
                    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
                    PRIMARY KEY (id),
                    KEY idx_stock_out_order_name (order_name),
                    KEY idx_stock_out_handler (handler)
                ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出库单主表'
                """);
        // 出库单明细表
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS t_material_stock_out_item (
                    id             BIGINT       NOT NULL COMMENT '主键（雪花ID）',
                    order_id       BIGINT       NOT NULL COMMENT '所属出库单ID',
                    warehouse_id   BIGINT       NULL COMMENT '仓库ID',
                    warehouse_name VARCHAR(128) NULL COMMENT '仓库名称',
                    item_id        BIGINT       NULL COMMENT '物品ID',
                    item_name      VARCHAR(128) NOT NULL COMMENT '物品名称',
                    item_category  VARCHAR(32)  NULL COMMENT '物品分类（字典 wu_pin_fen_lei）',
                    quantity       INT          NOT NULL DEFAULT 0 COMMENT '出库数量',
                    out_type       VARCHAR(32)  NULL COMMENT '出库类型（字典 chu_ku_lei_xing）',
                    out_time       DATE         NULL COMMENT '出库时间',
                    handler        VARCHAR(32)  NULL COMMENT '经办人',
                    remark         VARCHAR(255) NULL COMMENT '备注',
                    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
                    PRIMARY KEY (id),
                    KEY idx_stock_out_item_order (order_id),
                    KEY idx_stock_out_item_warehouse (warehouse_id),
                    KEY idx_stock_out_item_name (item_name)
                ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出库单明细表'
                """);
        log.info("出库单模块数据表初始化完成");
    }

    // ==================== 出库类型字典 ====================

    private void initOutTypeDict() {
        String[][] items = {
                {"领用出库", "1", "1"},
                {"调拨出库", "2", "2"},
                {"报废出库", "3", "3"},
                {"其他出库", "4", "4"}
        };
        for (String[] it : items) {
            long count = dictMapper.selectCount(new LambdaQueryWrapper<SysDict>()
                    .eq(SysDict::getDictType, "chu_ku_lei_xing")
                    .eq(SysDict::getDictValue, it[1]));
            if (count == 0) {
                SysDict dict = new SysDict();
                dict.setDictType("chu_ku_lei_xing");
                dict.setDescription("出库类型");
                dict.setDictLabel(it[0]);
                dict.setDictValue(it[1]);
                dict.setSort(Integer.parseInt(it[2]));
                dictMapper.insert(dict);
            }
        }
        log.info("出库类型字典初始化完成");
    }

    // ==================== 菜单（4 个） ====================

    private void initMenus() {
        // 找到物资管理目录的 ID
        SysMenu materialDir = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuName, "物资管理")
                .eq(SysMenu::getParentId, 0L));
        if (materialDir == null) {
            log.warn("未找到「物资管理」目录菜单，跳过出库单菜单初始化");
            return;
        }

        // 1) 出库单菜单
        SysMenu orderMenu = ensureMenu(
                "出库单", materialDir.getId(), "C",
                "/material/stockout", "material:stockout:list",
                "TakeawayBox", 4
        );

        // 2-4) 按钮权限
        ensureMenu("出库单新增", orderMenu.getId(), "F", null,
                "material:stockout:add", null, 1);
        ensureMenu("出库单编辑", orderMenu.getId(), "F", null,
                "material:stockout:edit", null, 2);
        ensureMenu("出库单删除", orderMenu.getId(), "F", null,
                "material:stockout:delete", null, 3);

        log.info("出库单菜单初始化完成（菜单ID={}）", orderMenu.getId());
    }

    /** 幂等插入菜单：同 menu_name + parent_id 已存在则直接返回 */
    private SysMenu ensureMenu(String name, Long parentId, String type,
                               String path, String perm, String icon, int sort) {
        SysMenu existing = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuName, name)
                .eq(SysMenu::getParentId, parentId));
        if (existing != null) {
            return existing;
        }
        SysMenu menu = new SysMenu();
        menu.setParentId(parentId);
        menu.setMenuName(name);
        menu.setMenuType(type);
        menu.setPath(path);
        menu.setPerm(perm);
        menu.setIcon(icon);
        menu.setSort(sort);
        menu.setStatus(1);
        menuMapper.insert(menu);
        return menu;
    }

    // ==================== 授权 ====================

    private void grantRoles() {
        // 查出刚插入的 4 个菜单
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getMenuName,
                        "出库单", "出库单新增", "出库单编辑", "出库单删除"));

        // 给超级管理员和区级经办人都授权
        long[] roleIds = {ROLE_ADMIN, ROLE_OPERATOR};
        for (SysMenu menu : menus) {
            for (long roleId : roleIds) {
                long count = roleMenuMapper.selectCount(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId)
                        .eq(SysRoleMenu::getMenuId, menu.getId()));
                if (count == 0) {
                    SysRoleMenu rm = new SysRoleMenu();
                    rm.setRoleId(roleId);
                    rm.setMenuId(menu.getId());
                    roleMenuMapper.insert(rm);
                }
            }
        }
        log.info("出库单菜单角色授权完成（共 {} 个菜单 x {} 个角色）", menus.size(), roleIds.length);
    }
}
