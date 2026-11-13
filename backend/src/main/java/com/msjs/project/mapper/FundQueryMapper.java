package com.msjs.project.mapper;

import com.msjs.project.vo.FundQueryVO;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资金记录查询 Mapper
 *
 * <p>原本设计用 UNION ALL 把 t_project_fund 的 nodeLevel=1/2/3 三段在 SQL 层拉平，
 * 但踩了两个坑：
 *   1. 生产库三张表字符串字段的实际 COLLATE 不统一（有的 utf8mb4_unicode_ci，
 *      有的 utf8mb4_general_ci），MySQL 在 UNION 时会抛
 *      "Illegal mix of collations for operation 'UNION'"。
 *   2. 加 COLLATE 归一化之后，MyBatis-Plus 注册的 DataPermissionInterceptor
 *      底层 JSqlParser 又不认识 COLLATE 语法，还没到 MySQL 就 parse 失败。
 *
 * 因此最终方案拆成三条独立查询，Service 层内存合并。SQL 纯净到不会触发
 * JSqlParser 的任何边缘 case，同时 Service 层对三条查询应用完全相同的筛选，
 * 合并后再内存分页 + summary + sidebar 聚合，语义与原 UNION ALL 完全一致。</p>
 */
@Mapper
public interface FundQueryMapper {

    /** nodeLevel = 1：资金登记（从立项表 a 直接取乡镇、region_code） */
    @Select("<script>" +
            " SELECT " +
            "   f.id AS id, f.node_level AS nodeLevel, " +
            "   f.fund_type AS fundType, f.fund_source AS fundSource, " +
            "   f.amount AS amount, f.create_time AS createTime, " +
            "   f.create_by AS createBy, f.remark AS remark, " +
            "   a.id AS approvalId, a.approval_code AS approvalCode, " +
            "   a.project_name AS projectName, a.project_category AS projectCategory, " +
            "   a.project_type AS projectType, " +
            "   NULL AS declareId, NULL AS declareCode, NULL AS declareName, " +
            "   NULL AS detailId, NULL AS detailName, NULL AS detailType, " +
            "   a.region_name AS townName, NULL AS villageName, " +
            "   a.region_code AS regionCode " +
            " FROM t_project_fund f " +
            " JOIN t_project_approval a ON a.id = f.approval_id AND a.deleted = 0 " +
            " WHERE f.deleted = 0 AND f.node_level = 1 " +
            " <if test='projectName != null and projectName != \"\"'>" +
            "   AND a.project_name LIKE CONCAT('%', #{projectName}, '%')" +
            " </if>" +
            " <if test='approvalCode != null and approvalCode != \"\"'>" +
            "   AND a.approval_code LIKE CONCAT('%', #{approvalCode}, '%')" +
            " </if>" +
            " <if test='fundType != null and fundType != \"\"'>" +
            "   AND f.fund_type = #{fundType}" +
            " </if>" +
            " <if test='fundSource != null and fundSource != \"\"'>" +
            "   AND f.fund_source = #{fundSource}" +
            " </if>" +
            " <if test='createBy != null and createBy != \"\"'>" +
            "   AND f.create_by = #{createBy}" +
            " </if>" +
            " <if test='startTime != null and startTime != \"\"'>" +
            "   AND f.create_time &gt;= #{startTime}" +
            " </if>" +
            " <if test='endTime != null and endTime != \"\"'>" +
            "   AND f.create_time &lt; CAST(#{endTime} AS DATETIME) + INTERVAL 1 DAY" +
            " </if>" +
            " <if test='minAmount != null'>" +
            "   AND f.amount &gt;= #{minAmount}" +
            " </if>" +
            " <if test='maxAmount != null'>" +
            "   AND f.amount &lt;= #{maxAmount}" +
            " </if>" +
            " <if test='regionCodes != null and regionCodes.size() &gt; 0'>" +
            "   AND a.region_code IN " +
            "   <foreach collection='regionCodes' item='rc' open='(' separator=',' close=')'>" +
            "     #{rc}" +
            "   </foreach>" +
            " </if>" +
            " ORDER BY f.create_time DESC, f.id DESC" +
            "</script>")
    List<FundQueryVO.Record> selectLevel1(CommonQueryParams p);

    /** nodeLevel = 2：二级下拨（从申报单 d 取乡镇/村组） */
    @Select("<script>" +
            " SELECT " +
            "   f.id AS id, f.node_level AS nodeLevel, " +
            "   f.fund_type AS fundType, f.fund_source AS fundSource, " +
            "   f.amount AS amount, f.create_time AS createTime, " +
            "   f.create_by AS createBy, f.remark AS remark, " +
            "   a.id AS approvalId, a.approval_code AS approvalCode, " +
            "   a.project_name AS projectName, a.project_category AS projectCategory, " +
            "   a.project_type AS projectType, " +
            "   d.id AS declareId, d.declare_code AS declareCode, d.declare_name AS declareName, " +
            "   NULL AS detailId, NULL AS detailName, NULL AS detailType, " +
            "   d.town_name AS townName, d.village_name AS villageName, " +
            "   a.region_code AS regionCode " +
            " FROM t_project_fund f " +
            " JOIN t_project_approval a ON a.id = f.approval_id AND a.deleted = 0 " +
            " JOIN t_project_approval_declare d ON d.id = f.declare_id AND d.deleted = 0 " +
            " WHERE f.deleted = 0 AND f.node_level = 2 " +
            " <if test='projectName != null and projectName != \"\"'>" +
            "   AND a.project_name LIKE CONCAT('%', #{projectName}, '%')" +
            " </if>" +
            " <if test='approvalCode != null and approvalCode != \"\"'>" +
            "   AND a.approval_code LIKE CONCAT('%', #{approvalCode}, '%')" +
            " </if>" +
            " <if test='declareCode != null and declareCode != \"\"'>" +
            "   AND d.declare_code LIKE CONCAT('%', #{declareCode}, '%')" +
            " </if>" +
            " <if test='fundType != null and fundType != \"\"'>" +
            "   AND f.fund_type = #{fundType}" +
            " </if>" +
            " <if test='fundSource != null and fundSource != \"\"'>" +
            "   AND f.fund_source = #{fundSource}" +
            " </if>" +
            " <if test='createBy != null and createBy != \"\"'>" +
            "   AND f.create_by = #{createBy}" +
            " </if>" +
            " <if test='startTime != null and startTime != \"\"'>" +
            "   AND f.create_time &gt;= #{startTime}" +
            " </if>" +
            " <if test='endTime != null and endTime != \"\"'>" +
            "   AND f.create_time &lt; CAST(#{endTime} AS DATETIME) + INTERVAL 1 DAY" +
            " </if>" +
            " <if test='minAmount != null'>" +
            "   AND f.amount &gt;= #{minAmount}" +
            " </if>" +
            " <if test='maxAmount != null'>" +
            "   AND f.amount &lt;= #{maxAmount}" +
            " </if>" +
            " <if test='regionCodes != null and regionCodes.size() &gt; 0'>" +
            "   AND a.region_code IN " +
            "   <foreach collection='regionCodes' item='rc' open='(' separator=',' close=')'>" +
            "     #{rc}" +
            "   </foreach>" +
            " </if>" +
            " ORDER BY f.create_time DESC, f.id DESC" +
            "</script>")
    List<FundQueryVO.Record> selectLevel2(CommonQueryParams p);

    /** nodeLevel = 3：三级下拨（明细 target_name、detail_type） */
    @Select("<script>" +
            " SELECT " +
            "   f.id AS id, f.node_level AS nodeLevel, " +
            "   f.fund_type AS fundType, f.fund_source AS fundSource, " +
            "   f.amount AS amount, f.create_time AS createTime, " +
            "   f.create_by AS createBy, f.remark AS remark, " +
            "   a.id AS approvalId, a.approval_code AS approvalCode, " +
            "   a.project_name AS projectName, a.project_category AS projectCategory, " +
            "   a.project_type AS projectType, " +
            "   d.id AS declareId, d.declare_code AS declareCode, d.declare_name AS declareName, " +
            "   det.id AS detailId, det.target_name AS detailName, det.detail_type AS detailType, " +
            "   d.town_name AS townName, d.village_name AS villageName, " +
            "   a.region_code AS regionCode " +
            " FROM t_project_fund f " +
            " JOIN t_project_approval a ON a.id = f.approval_id AND a.deleted = 0 " +
            " JOIN t_project_approval_declare d ON d.id = f.declare_id AND d.deleted = 0 " +
            " JOIN t_project_approval_detail det ON det.id = f.detail_id AND det.deleted = 0 " +
            " WHERE f.deleted = 0 AND f.node_level = 3 " +
            " <if test='projectName != null and projectName != \"\"'>" +
            "   AND a.project_name LIKE CONCAT('%', #{projectName}, '%')" +
            " </if>" +
            " <if test='approvalCode != null and approvalCode != \"\"'>" +
            "   AND a.approval_code LIKE CONCAT('%', #{approvalCode}, '%')" +
            " </if>" +
            " <if test='declareCode != null and declareCode != \"\"'>" +
            "   AND d.declare_code LIKE CONCAT('%', #{declareCode}, '%')" +
            " </if>" +
            " <if test='detailName != null and detailName != \"\"'>" +
            "   AND det.target_name LIKE CONCAT('%', #{detailName}, '%')" +
            " </if>" +
            " <if test='fundType != null and fundType != \"\"'>" +
            "   AND f.fund_type = #{fundType}" +
            " </if>" +
            " <if test='fundSource != null and fundSource != \"\"'>" +
            "   AND f.fund_source = #{fundSource}" +
            " </if>" +
            " <if test='createBy != null and createBy != \"\"'>" +
            "   AND f.create_by = #{createBy}" +
            " </if>" +
            " <if test='startTime != null and startTime != \"\"'>" +
            "   AND f.create_time &gt;= #{startTime}" +
            " </if>" +
            " <if test='endTime != null and endTime != \"\"'>" +
            "   AND f.create_time &lt; CAST(#{endTime} AS DATETIME) + INTERVAL 1 DAY" +
            " </if>" +
            " <if test='minAmount != null'>" +
            "   AND f.amount &gt;= #{minAmount}" +
            " </if>" +
            " <if test='maxAmount != null'>" +
            "   AND f.amount &lt;= #{maxAmount}" +
            " </if>" +
            " <if test='regionCodes != null and regionCodes.size() &gt; 0'>" +
            "   AND a.region_code IN " +
            "   <foreach collection='regionCodes' item='rc' open='(' separator=',' close=')'>" +
            "     #{rc}" +
            "   </foreach>" +
            " </if>" +
            " ORDER BY f.create_time DESC, f.id DESC" +
            "</script>")
    List<FundQueryVO.Record> selectLevel3(CommonQueryParams p);

    /**
     * 三段查询共用的筛选参数 DTO（只有 3 段都一致的筛选项才放这里；
     * 只对某一级有效的筛选项由 Service 层决定是否跳过该段）。
     */
    @Data // lombok 生成 getter/setter，方便 MyBatis XML 与注解式 <if test>
    class CommonQueryParams {
        String projectName;
        String approvalCode;
        String declareCode;
        String detailName;
        String fundType;
        String fundSource;
        String startTime;
        String endTime;
        String createBy;
        java.math.BigDecimal minAmount;
        java.math.BigDecimal maxAmount;
        List<String> regionCodes;
    }

    /** 批量查询附件数量 */
    @Select("<script>" +
            " SELECT fund_id AS fundId, COUNT(*) AS cnt " +
            " FROM t_project_fund_attachment " +
            " WHERE fund_id IN " +
            " <foreach collection='fundIds' item='fid' open='(' separator=',' close=')'>" +
            "   #{fid}" +
            " </foreach>" +
            " GROUP BY fund_id" +
            "</script>")
    List<java.util.Map<String, Object>> selectAttachCount(@Param("fundIds") List<Long> fundIds);
}
