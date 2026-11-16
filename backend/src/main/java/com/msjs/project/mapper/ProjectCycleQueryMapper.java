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
            " JOIN t_project_approval_declare d ON d.approval_id = a.id AND d.deleted = 0 " +
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
            "   WHERE deleted = 0 " +
            "   GROUP BY approval_id" +
            " ) fund ON fund.approval_id = a.id " +
            " WHERE a.deleted = 0 AND a.status = 'APPROVED' " +
            " <if test='year != null and year != \"\"'>" +
            "   AND YEAR(a.submit_time) = #{year}" +
            " </if>" +
            " <if test='regionCodes != null and regionCodes.size() &gt; 0'>" +
            "   AND a.region_code IN " +
            "   <foreach collection='regionCodes' item='rc' open='(' separator=',' close=')'>" +
            "     #{rc}" +
            "   </foreach>" +
            " </if>" +
            " ORDER BY a.id DESC, d.id DESC" +
            "</script>")
    List<ProjectCycleRawVO> selectRawList(QueryParams p);

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
            " JOIN t_project_approval_declare d ON d.approval_id = a.id AND d.deleted = 0 " +
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
            "   WHERE deleted = 0 " +
            "   GROUP BY approval_id" +
            " ) fund ON fund.approval_id = a.id " +
            " WHERE a.deleted = 0 " +  // 包含所有立项状态
            " <if test='regionCodes != null and regionCodes.size() &gt; 0'>" +
            "   AND a.region_code IN " +
            "   <foreach collection='regionCodes' item='rc' open='(' separator=',' close=')'>" +
            "     #{rc}" +
            "   </foreach>" +
            " </if>" +
            " ORDER BY a.id DESC, d.id DESC" +
            "</script>")
    List<ProjectCycleRawVO> selectRawListAllStatus(QueryParams p);

    /**
     * 查询施工状态聚合（项目级）
     * 返回 approval_id + 施工状态汇总
     */
    @Select("SELECT approval_id, " +
            "  MAX(CASE WHEN build_status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS hasInProgress, " +
            "  MAX(CASE WHEN build_status = 'COMPLETED' THEN 1 ELSE 0 END) AS hasCompleted, " +
            "  COUNT(*) AS totalNodes, " +
            "  SUM(CASE WHEN build_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedNodes " +
            "FROM t_project_construction " +
            "WHERE deleted = 0 AND node_level = 3 " +
            "GROUP BY approval_id")
    List<Map<String, Object>> selectConstructionStatus();

    /**
     * 查询验收状态聚合（项目级）
     */
    @Select("SELECT approval_id, " +
            "  MAX(CASE WHEN accept_result = 'PASS' THEN 1 ELSE 0 END) AS hasPass, " +
            "  MAX(CASE WHEN accept_result = 'FAIL' THEN 1 ELSE 0 END) AS hasFail, " +
            "  COUNT(*) AS totalAccepts " +
            "FROM t_project_acceptance " +
            "WHERE deleted = 0 " +
            "GROUP BY approval_id")
    List<Map<String, Object>> selectAcceptanceStatus();

    @lombok.Data
    class QueryParams {
        String year;
        List<String> regionCodes;
    }
}
