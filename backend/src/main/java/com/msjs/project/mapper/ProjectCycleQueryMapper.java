package com.msjs.project.mapper;

import com.msjs.project.vo.ProjectCycleRawVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 项目全周期查询 Mapper
 *
 * <p>以 declare（申报单）为查询粒度，关联 approval、detail、fund 表。
 *    Service 层基于原始行做：字典翻译、项目状态计算、5套方案的分组聚合。</p>
 */
@Mapper
public interface ProjectCycleQueryMapper {

    /**
     * 查询全部原始明细行（以 declare 为粒度）
     */
    @Select("<script>" +
            " SELECT " +
            "   a.id AS approvalId, " +
            "   a.approval_code AS approvalCode, " +
            "   a.project_name AS projectName, " +
            "   a.project_source AS projectSource, " +
            "   a.project_category AS projectCategory, " +
            "   a.project_type AS projectType, " +
            "   a.status AS approvalStatus, " +
            "   a.accept_status AS acceptStatus, " +
            "   a.budget AS budget, " +
            "   a.region_code AS regionCode, " +
            "   a.submit_time AS submitTime, " +
            "   ar.create_time AS archiveCreateTime, " +
            "   d.id AS declareId, " +
            "   d.declare_category AS declareCategory, " +
            "   d.village_name AS villageName, " +
            "   IFNULL(h.household_cnt, 0) AS householdCount, " +
            "   IFNULL(f.facility_cnt, 0) AS facilityCount, " +
            "   IFNULL(fund.total_fund, 0) AS totalFund " +
            " FROM t_project_approval a " +
            " LEFT JOIN t_project_approval_declare d ON d.approval_id = a.id AND d.deleted = 0 " +
            " LEFT JOIN t_project_archive ar ON ar.approval_id = a.id AND ar.deleted = 0 " +
            " LEFT JOIN (" +
            "   SELECT declare_id, COUNT(*) AS household_cnt " +
            "   FROM t_project_approval_detail " +
            "   WHERE deleted = 0 AND detail_type = 'HOUSEHOLD' " +
            "   GROUP BY declare_id" +
            " ) h ON h.declare_id = d.id " +
            " LEFT JOIN (" +
            "   SELECT declare_id, COUNT(*) AS facility_cnt " +
            "   FROM t_project_approval_detail " +
            "   WHERE deleted = 0 AND detail_type = 'FACILITY' " +
            "   GROUP BY declare_id" +
            " ) f ON f.declare_id = d.id " +
            " LEFT JOIN (" +
            "   SELECT approval_id, SUM(amount) AS total_fund " +
            "   FROM t_project_fund " +
            "   WHERE deleted = 0 AND node_level = 1 AND fund_type = '2' " +
            "   GROUP BY approval_id" +
            " ) fund ON fund.approval_id = a.id " +
            " WHERE a.deleted = 0 AND a.status = 'APPROVED' " +
            " <if test='p.year != null and p.year != \"\"'>" +
            "   AND YEAR(a.submit_time) = #{p.year}" +
            " </if>" +
            " <if test='p.regionCodes != null and p.regionCodes.size() &gt; 0'>" +
            "   AND a.region_code IN " +
            "   <foreach collection='p.regionCodes' item='rc' open='(' separator=',' close=')'>" +
            "     #{rc}" +
            "   </foreach>" +
            " </if>" +
            " ORDER BY a.id DESC, d.id DESC" +
            "</script>")
    List<ProjectCycleRawVO> selectRawList(@Param("p") QueryParams p);

    /**
     * 查询审批通过但尚未归档的立项单（用于统计"待立项"/"已立项"中的已立项部分）
     * 同时查询尚未进入审批流程的申报单（用于"待立项"统计）
     */
    @Select("<script>" +
            " SELECT " +
            "   a.id AS approvalId, " +
            "   a.project_source AS projectSource, " +
            "   a.project_type AS projectType, " +
            "   a.status AS approvalStatus, " +
            "   a.accept_status AS acceptStatus, " +
            "   a.budget AS budget, " +
            "   a.region_code AS regionCode, " +
            "   a.submit_time AS submitTime, " +
            "   ar.create_time AS archiveCreateTime, " +
            "   d.id AS declareId, " +
            "   d.declare_category AS declareCategory, " +
            "   d.village_name AS villageName, " +
            "   IFNULL(h.household_cnt, 0) AS householdCount, " +
            "   IFNULL(f.facility_cnt, 0) AS facilityCount, " +
            "   IFNULL(fund.total_fund, 0) AS totalFund " +
            " FROM t_project_approval a " +
            " LEFT JOIN t_project_approval_declare d ON d.approval_id = a.id AND d.deleted = 0 " +
            " LEFT JOIN t_project_archive ar ON ar.approval_id = a.id AND ar.deleted = 0 " +
            " LEFT JOIN (" +
            "   SELECT declare_id, COUNT(*) AS household_cnt " +
            "   FROM t_project_approval_detail " +
            "   WHERE deleted = 0 AND detail_type = 'HOUSEHOLD' " +
            "   GROUP BY declare_id" +
            " ) h ON h.declare_id = d.id " +
            " LEFT JOIN (" +
            "   SELECT declare_id, COUNT(*) AS facility_cnt " +
            "   FROM t_project_approval_detail " +
            "   WHERE deleted = 0 AND detail_type = 'FACILITY' " +
            "   GROUP BY declare_id" +
            " ) f ON f.declare_id = d.id " +
            " LEFT JOIN (" +
            "   SELECT approval_id, SUM(amount) AS total_fund " +
            "   FROM t_project_fund " +
            "   WHERE deleted = 0 AND node_level = 1 AND fund_type = '2' " +
            "   GROUP BY approval_id" +
            " ) fund ON fund.approval_id = a.id " +
            " WHERE a.deleted = 0 " +  // 包含所有立项状态
            " <if test='p.regionCodes != null and p.regionCodes.size() &gt; 0'>" +
            "   AND a.region_code IN " +
            "   <foreach collection='p.regionCodes' item='rc' open='(' separator=',' close=')'>" +
            "     #{rc}" +
            "   </foreach>" +
            " </if>" +
            " ORDER BY a.id DESC, d.id DESC" +
            "</script>")
    List<ProjectCycleRawVO> selectRawListAllStatus(@Param("p") QueryParams p);

    /**
     * 查询施工状态聚合（项目级）
     * 取 t_project_construction.build_status 明细状态：NOT_STARTED / IN_PROGRESS / COMPLETED
     */
    @Select("SELECT approval_id AS approvalId, " +
            "  MAX(CASE WHEN build_status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS hasInProgress, " +
            "  MAX(CASE WHEN build_status = 'COMPLETED' THEN 1 ELSE 0 END) AS hasCompleted, " +
            "  COUNT(*) AS totalNodes, " +
            "  SUM(CASE WHEN build_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedNodes " +
            "FROM t_project_construction " +
            "WHERE deleted = 0 AND build_status IS NOT NULL " +
            "GROUP BY approval_id")
    List<ConstructionStat> selectConstructionStatus();

    /**
     * 查询验收状态聚合（明细级汇总，口径与施工状态一致）：
     * 按三级明细（node_type='DETAIL'）聚合：
     *   全部 ACCEPTED → PASS（已验收）
     *   存在 REJECTED → FAIL（未通过）
     *   部分 ACCEPTED + 部分 PENDING → APPLYING（待验收）
     * 有 PROJECT 级记录也一并纳入（不做过滤）。
     */
    @Select("SELECT approval_id AS approvalId, " +
            "  COUNT(*) AS totalAccepts, " +
            "  CASE " +
            "    WHEN SUM(CASE WHEN accept_status = 'REJECTED' THEN 1 ELSE 0 END) > 0 THEN 'FAIL' " +
            "    WHEN SUM(CASE WHEN accept_status = 'ACCEPTED' THEN 1 ELSE 0 END) = COUNT(*) THEN 'PASS' " +
            "    WHEN SUM(CASE WHEN accept_status = 'ACCEPTED' THEN 1 ELSE 0 END) > 0 THEN 'APPLYING' " +
            "    ELSE 'APPLYING' " +
            "  END AS latestProjectStatus " +
            "FROM t_project_acceptance " +
            "WHERE deleted = 0 " +
            "GROUP BY approval_id")
    List<AcceptanceStat> selectAcceptanceStatus();

    /**
     * 查询方案4专用明细：t_project_construction 三级明细节点（node_level=3）粒度。
     * 每个农户/设施一条，自带 build_status 和 accept_status，用于按明细粒度决定项目状态。
     * 同时也 LEFT JOIN approval、declare、archive、fund 等表，让返回行尽量和 selectRawList 对齐
     * （Service 层的 buildGroupedRows / buildRow / buildGlobalSummary 可以直接复用）。
     */
    @Select("<script>" +
            " SELECT " +
            "   a.id AS approvalId, " +
            "   a.approval_code AS approvalCode, " +
            "   a.project_name AS projectName, " +
            "   a.project_source AS projectSource, " +
            "   a.project_type AS projectType, " +
            "   a.status AS approvalStatus, " +
            "   a.accept_status AS acceptStatus, " +
            "   a.budget AS budget, " +
            "   a.region_code AS regionCode, " +
            "   a.submit_time AS submitTime, " +
            "   ar.create_time AS archiveCreateTime, " +
            "   d.id AS declareId, " +
            "   d.declare_category AS declareCategory, " +
            "   d.village_name AS villageName, " +
            "   cd.id AS detailId, " +
            "   cd.target_name AS detailTargetName, " +
            "   cd.detail_type AS detailType, " +
            "   c.build_status AS buildStatus, " +
            "   IFNULL(ac.accept_status, 'PENDING') AS acceptStatusDetail, " +
            "   CASE WHEN cd.detail_type = 'HOUSEHOLD' THEN 1 ELSE 0 END AS householdCount, " +
            "   CASE WHEN cd.detail_type = 'FACILITY' THEN 1 ELSE 0 END AS facilityCount, " +
            "   IFNULL(fund.total_fund, 0) AS totalFund " +
            " FROM t_project_construction c " +
            " JOIN t_project_approval a ON a.id = c.approval_id AND a.deleted = 0 " +
            " LEFT JOIN t_project_approval_declare d ON d.id = c.declare_id AND d.deleted = 0 " +
            " LEFT JOIN t_project_approval_detail cd ON cd.id = c.detail_id AND cd.deleted = 0 " +
            " LEFT JOIN t_project_acceptance ac ON ac.approval_id = c.approval_id " +
            "   AND ac.detail_id = c.detail_id AND ac.deleted = 0 " +
            " LEFT JOIN t_project_archive ar ON ar.approval_id = a.id AND ar.deleted = 0 " +
            " LEFT JOIN (" +
            "   SELECT approval_id, SUM(amount) AS total_fund " +
            "   FROM t_project_fund " +
            "   WHERE deleted = 0 AND node_level = 1 AND fund_type = '2' " +
            "   GROUP BY approval_id" +
            " ) fund ON fund.approval_id = a.id " +
            " WHERE c.deleted = 0 AND c.node_level = 3 " +
            "   AND a.status = 'APPROVED'" +
            " <if test='p.regionCodes != null and p.regionCodes.size() &gt; 0'>" +
            "   AND a.region_code IN " +
            "   <foreach collection='p.regionCodes' item='rc' open='(' separator=',' close=')'>" +
            "     #{rc}" +
            "   </foreach>" +
            " </if>" +
            " ORDER BY a.id DESC, cd.id DESC" +
            "</script>")
    List<ProjectCycleRawVO> selectStatusDetailRaw(@Param("p") QueryParams p);

    /** 施工状态聚合结果 */
    @lombok.Data
    class ConstructionStat {
        private Long approvalId;
        private Integer hasInProgress;
        private Integer hasCompleted;
        private Integer totalNodes;
        private Integer completedNodes;
    }

    /** 验收状态聚合结果 */
    @lombok.Data
    class AcceptanceStat {
        private Long approvalId;
        private Integer totalAccepts;
        /** 最新一次项目级验收记录状态：ACCEPTED / REJECTED / PENDING，无记录为 null */
        private String latestProjectStatus;
    }

    @lombok.Data
    class QueryParams {
        String year;
        List<String> regionCodes;
    }
}
