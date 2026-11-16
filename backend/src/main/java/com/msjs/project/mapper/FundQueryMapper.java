package com.msjs.project.mapper;

import com.msjs.project.vo.FundDetailRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资金记录查询 Mapper
 *
 * <p>拆为 3 段独立查询（node_level = 1 / 2 / 3），由 Service 层内存合并 + 排序。
 * 这么做是为了避开：
 *   ① 三张表字符串字段实际 COLLATE 不统一，直接 UNION ALL 会抛
 *      "Illegal mix of collations for operation 'UNION'"；
 *   ② 加 COLLATE 归一化之后 MyBatis-Plus DataPermissionInterceptor 底层
 *      JSqlParser 又不认识 COLLATE 语法，还没到 MySQL 就 parse 失败。
 *
 * 三段 SQL 纯净不含 UNION / COLLATE / 跨表 LEFT JOIN 以外的花招，JSqlParser
 * 能顺畅通过，MySQL 也不会因为 collation 报错。</p>
 */
@Mapper
public interface FundQueryMapper {

    /** nodeLevel = 1：资金登记（从立项表取乡镇 region_code/region_name） */
    @Select("<script>" +
            " SELECT " +
            "   f.id AS id, f.node_level AS nodeLevel, " +
            "   f.fund_type AS fundType, f.fund_source AS fundSource, " +
            "   f.amount AS amount, f.create_time AS createTime, " +
            "   f.create_by AS createBy, f.remark AS remark, " +
            "   a.id AS approvalId, a.approval_code AS approvalCode, " +
            "   a.project_name AS projectName, a.project_category AS projectCategory, " +
            "   a.project_type AS projectType, " +
            "   CAST(NULL AS SIGNED) AS declareId, " +
            "   CAST(NULL AS CHAR) AS declareCode, CAST(NULL AS CHAR) AS declareName, " +
            "   CAST(NULL AS SIGNED) AS detailId, " +
            "   CAST(NULL AS CHAR) AS detailName, CAST(NULL AS CHAR) AS detailType, " +
            "   a.region_name AS townName, CAST(NULL AS CHAR) AS villageName, " +
            "   a.region_code AS regionCode " +
            " FROM t_project_fund f " +
            " JOIN t_project_approval a ON a.id = f.approval_id AND a.deleted = 0 " +
            " WHERE f.deleted = 0 AND f.node_level = 1 " +
            " <if test='year != null and year != \"\"'>" +
            "   AND YEAR(f.create_time) = #{year}" +
            " </if>" +
            " <if test='regionCodes != null and regionCodes.size() &gt; 0'>" +
            "   AND a.region_code IN " +
            "   <foreach collection='regionCodes' item='rc' open='(' separator=',' close=')'>" +
            "     #{rc}" +
            "   </foreach>" +
            " </if>" +
            " <if test='fundType != null and fundType != \"\"'>" +
            "   AND f.fund_type = #{fundType}" +
            " </if>" +
            " <if test='fundSource != null and fundSource != \"\"'>" +
            "   AND f.fund_source = #{fundSource}" +
            " </if>" +
            " <if test='createBy != null and createBy != \"\"'>" +
            "   AND f.create_by LIKE CONCAT('%', #{createBy}, '%')" +
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
            " ORDER BY f.create_time DESC, f.id DESC" +
            "</script>")
    List<FundDetailRowVO> selectLevel1(QueryParams p);

    /** nodeLevel = 2：二级下拨（从申报单取 town_name / village_name） */
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
            "   CAST(NULL AS SIGNED) AS detailId, " +
            "   CAST(NULL AS CHAR) AS detailName, CAST(NULL AS CHAR) AS detailType, " +
            "   d.town_name AS townName, d.village_name AS villageName, " +
            "   a.region_code AS regionCode " +
            " FROM t_project_fund f " +
            " JOIN t_project_approval a ON a.id = f.approval_id AND a.deleted = 0 " +
            " JOIN t_project_approval_declare d ON d.id = f.declare_id AND d.deleted = 0 " +
            " WHERE f.deleted = 0 AND f.node_level = 2 " +
            " <if test='year != null and year != \"\"'>" +
            "   AND YEAR(f.create_time) = #{year}" +
            " </if>" +
            " <if test='regionCodes != null and regionCodes.size() &gt; 0'>" +
            "   AND a.region_code IN " +
            "   <foreach collection='regionCodes' item='rc' open='(' separator=',' close=')'>" +
            "     #{rc}" +
            "   </foreach>" +
            " </if>" +
            " <if test='fundType != null and fundType != \"\"'>" +
            "   AND f.fund_type = #{fundType}" +
            " </if>" +
            " <if test='fundSource != null and fundSource != \"\"'>" +
            "   AND f.fund_source = #{fundSource}" +
            " </if>" +
            " <if test='createBy != null and createBy != \"\"'>" +
            "   AND f.create_by LIKE CONCAT('%', #{createBy}, '%')" +
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
            " ORDER BY f.create_time DESC, f.id DESC" +
            "</script>")
    List<FundDetailRowVO> selectLevel2(QueryParams p);

    /** nodeLevel = 3：三级下拨（从明细取 target_name / detail_type） */
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
            " <if test='year != null and year != \"\"'>" +
            "   AND YEAR(f.create_time) = #{year}" +
            " </if>" +
            " <if test='regionCodes != null and regionCodes.size() &gt; 0'>" +
            "   AND a.region_code IN " +
            "   <foreach collection='regionCodes' item='rc' open='(' separator=',' close=')'>" +
            "     #{rc}" +
            "   </foreach>" +
            " </if>" +
            " <if test='fundType != null and fundType != \"\"'>" +
            "   AND f.fund_type = #{fundType}" +
            " </if>" +
            " <if test='fundSource != null and fundSource != \"\"'>" +
            "   AND f.fund_source = #{fundSource}" +
            " </if>" +
            " <if test='createBy != null and createBy != \"\"'>" +
            "   AND f.create_by LIKE CONCAT('%', #{createBy}, '%')" +
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
            " ORDER BY f.create_time DESC, f.id DESC" +
            "</script>")
    List<FundDetailRowVO> selectLevel3(QueryParams p);

    /**
     * 三段查询共用的筛选参数 DTO（只有 3 段都一致的筛选项才放这里）。
     * lombok @Data 自动生成 getter/setter，MyBatis 注解式 XML <if test> 能直接访问。
     */
    @lombok.Data
    class QueryParams {
        String year;
        String fundType;
        String fundSource;
        String createBy;
        String startTime;
        String endTime;
        java.math.BigDecimal minAmount;
        java.math.BigDecimal maxAmount;
        List<String> regionCodes;
    }

    /** 批量查附件数量，Service 合并后再回填 */
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
