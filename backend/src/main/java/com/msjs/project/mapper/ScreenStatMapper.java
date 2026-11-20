package com.msjs.project.mapper;

import com.msjs.project.vo.ScreenBudgetRankVO;
import com.msjs.project.vo.ScreenChartItemVO;
import com.msjs.project.vo.ScreenEventVO;
import com.msjs.project.vo.ScreenFundTrendVO;
import com.msjs.project.vo.ScreenMaterialVO;
import com.msjs.project.vo.ScreenProjectStatusVO;
import com.msjs.project.vo.ScreenVillageTypeDetailVO;
import com.msjs.project.vo.ScreenVillageTypeVO;
import com.msjs.project.vo.ScreenVillageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据大屏统计 Mapper
 * 仅做聚合查询，统计口径与首页概览保持一致：逻辑删除数据不参与统计
 */
@Mapper
public interface ScreenStatMapper {

    /** 项目总预算：全部立项项目预算合计 */
    @Select("SELECT COALESCE(SUM(budget), 0) FROM t_project_approval WHERE deleted = 0")
    BigDecimal sumBudget();

    /** 已拨付资金：一级资金登记中"拨付"类型的金额合计 */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_project_fund "
            + "WHERE deleted = 0 AND node_level = 1 AND fund_type = '2'")
    BigDecimal sumPaid();

    /** 批复金额：一级资金登记中"批复"类型的金额合计，作为拨付完成率分母 */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_project_fund "
            + "WHERE deleted = 0 AND node_level = 1 AND fund_type = '3'")
    BigDecimal sumApproved();

    /** 近7个月按月立项预算合计 */
    @Select("SELECT DATE_FORMAT(COALESCE(pa.submit_time, pa.create_time), '%Y-%m') AS month, "
            + "COALESCE(SUM(pa.budget), 0) AS budget "
            + "FROM t_project_approval pa "
            + "WHERE pa.deleted = 0 AND pa.budget IS NOT NULL "
            + "AND COALESCE(pa.submit_time, pa.create_time) >= "
            + "DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 6 MONTH), '%Y-%m-01') "
            + "GROUP BY DATE_FORMAT(COALESCE(pa.submit_time, pa.create_time), '%Y-%m')")
    List<ScreenFundTrendVO> selectMonthlyBudget();

    /** 近7个月按月拨付金额合计（一级拨付登记） */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, "
            + "COALESCE(SUM(amount), 0) AS paid "
            + "FROM t_project_fund "
            + "WHERE deleted = 0 AND node_level = 1 AND fund_type = '2' "
            + "AND create_time >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 6 MONTH), '%Y-%m-01') "
            + "GROUP BY DATE_FORMAT(create_time, '%Y-%m')")
    List<ScreenFundTrendVO> selectMonthlyPaid();

    /** 项目状态判定原始数据：立项状态、验收状态、施工中明细数 */
    @Select("SELECT UPPER(pa.status) AS approvalStatus, UPPER(pa.accept_status) AS acceptStatus, "
            + "(SELECT COUNT(1) FROM t_project_construction c "
            + " WHERE c.approval_id = pa.id AND c.deleted = 0 "
            + " AND UPPER(c.build_status) = 'IN_PROGRESS') AS progressFlag "
            + "FROM t_project_approval pa WHERE pa.deleted = 0")
    List<ScreenProjectStatusVO> selectProjectStatuses();

    /** 在建项目数：已录入施工管理数据（施工记录）且未归档的项目，按项目去重 */
    @Select("SELECT COUNT(DISTINCT c.approval_id) FROM t_project_construction c "
            + "JOIN t_project_approval pa ON pa.id = c.approval_id AND pa.deleted = 0 "
            + "WHERE c.deleted = 0 "
            + "AND UPPER(IFNULL(pa.accept_status, 'NONE')) <> 'ARCHIVED'")
    Long countConstructionProjects();

    /** 村域态势：各行政村关联项目数与农户数，按项目数倒序 */
    @Select("SELECT r.name AS name, "
            + "(SELECT COUNT(DISTINCT d.approval_id) FROM t_project_approval_declare d "
            + " JOIN t_project_approval pa ON pa.id = d.approval_id AND pa.deleted = 0 "
            + " WHERE d.deleted = 0 AND d.village_name = r.name COLLATE utf8mb4_unicode_ci "
            + " AND UPPER(pa.status) <> 'REJECTED') AS projectCount, "
            + "(SELECT COUNT(1) FROM t_base_household h "
            + " WHERE h.deleted = 0 AND h.region_code = r.code COLLATE utf8mb4_unicode_ci) AS householdCount "
            + "FROM t_sys_region r "
            + "WHERE r.deleted = 0 AND r.level = 5 "
            + "ORDER BY projectCount DESC, householdCount DESC LIMIT 8")
    List<ScreenVillageVO> selectVillageStats();

    /** 各村项目类型计数：按申报单所属村 + 立项项目类型分组，统计去重立项数 */
    @Select("SELECT d.village_name AS villageName, pa.project_type AS projectType, "
            + "COUNT(DISTINCT d.approval_id) AS projectCount "
            + "FROM t_project_approval_declare d "
            + "JOIN t_project_approval pa ON pa.id = d.approval_id AND pa.deleted = 0 "
            + "WHERE d.deleted = 0 AND d.village_name IS NOT NULL AND d.village_name <> '' "
            + "AND UPPER(pa.status) <> 'REJECTED' "
            + "GROUP BY d.village_name, pa.project_type")
    List<ScreenVillageTypeVO> selectVillageTypeCounts();

    /**
     * 各村项目类型明细数（仅立项已通过）：按「行政村 × 项目类型」分组，
     * 分别统计农户明细数（单位户）与公建设施明细数（单位处）。
     * 仅计入立项状态为 APPROVED 的申报单；两表各自 COUNT(DISTINCT) 避免 JOIN 笛卡尔积膨胀。
     */
    @Select("SELECT d.village_name AS villageName, pa.project_type AS projectType, "
            + "COUNT(DISTINCT dh.id) AS householdCount, "
            + "COUNT(DISTINCT pf.id) AS facilityCount "
            + "FROM t_project_approval_declare d "
            + "JOIN t_project_approval pa ON pa.id = d.approval_id AND pa.deleted = 0 "
            + "LEFT JOIN t_declaration_household dh "
            + "  ON dh.declaration_id = d.declaration_id AND dh.deleted = 0 "
            + "LEFT JOIN t_declaration_public_facility pf "
            + "  ON pf.declaration_id = d.declaration_id AND pf.deleted = 0 "
            + "WHERE d.deleted = 0 AND UPPER(pa.status) = 'APPROVED' "
            + "AND d.village_name IS NOT NULL AND d.village_name <> '' "
            + "GROUP BY d.village_name, pa.project_type")
    List<ScreenVillageTypeDetailVO> selectVillageTypeDetailCounts();

    /** 预算投入排名：预算前5的立项项目 */
    @Select("SELECT project_name AS projectName, budget AS budget "
            + "FROM t_project_approval "
            + "WHERE deleted = 0 AND budget IS NOT NULL AND budget > 0 "
            + "ORDER BY budget DESC LIMIT 5")
    List<ScreenBudgetRankVO> selectBudgetRank();

    /** 物资状态：预警物资优先，其次按库存从低到高 */
    @Select("SELECT item_name AS itemName, IFNULL(stock_qty, 0) AS stockQty, "
            + "IFNULL(warning_qty, 0) AS warningQty, unit AS unit "
            + "FROM t_material_item WHERE deleted = 0 "
            + "ORDER BY (IFNULL(warning_qty, 0) > 0 AND IFNULL(stock_qty, 0) <= IFNULL(warning_qty, 0)) DESC, "
            + "IFNULL(stock_qty, 0) ASC LIMIT 12")
    List<ScreenMaterialVO> selectMaterialStats();

    /** 物资预警数量：库存低于预警值的物品数 */
    @Select("SELECT COUNT(1) FROM t_material_item "
            + "WHERE deleted = 0 AND IFNULL(warning_qty, 0) > 0 AND IFNULL(stock_qty, 0) <= warning_qty")
    Long countMaterialWarn();

    /**
     * 实时事件：仅业务写操作（POST/PUT/DELETE 且返回成功），
     * 排除查询、登录鉴权、大屏轮询、验证码等噪声，按时间倒序取最近 N 条。
     * 富文本摘要在 Service 层基于 params 提取，此处仅返回原始字段。
     */
    @Select("SELECT id, username, module, operation, method, url, params, "
            + "oper_time AS operTime FROM t_sys_oper_log "
            + "WHERE method IN ('POST','PUT','DELETE') "
            + "AND result_code = 200 "
            + "AND module <> '登录鉴权' "
            + "AND operation NOT LIKE '%查询%' "
            + "AND url NOT IN ('/api/dashboard/screen','/api/auth/captcha') "
            + "ORDER BY oper_time DESC, id DESC LIMIT #{limit}")
    List<ScreenEventVO> selectRecentEvents(@Param("limit") int limit);
}
