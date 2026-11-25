package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.vo.ProjectProgressVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ProjectApprovalMapper extends BaseMapper<ProjectApproval> {

    /**
     * 首页项目进度概览：每个立项项目一条记录，返回阶段判定所需原始状态
     * 由服务层计算当前阶段集合，同一项目可同时处于多个阶段
     * 已批准项目即视为已进入资金、施工、验收管理（三个页面均按立项状态展示）
     * 已驳回项目不参与进度展示；已归档项目仅保留一周（按归档操作更新时间）
     */
    @Select("""
            <script>
            SELECT
                pa.id AS id,
                pa.approval_code AS approvalCode,
                pa.project_name AS projectName,
                UPPER(pa.status) AS approvalStatus,
                UPPER(pa.accept_status) AS acceptStatus
            FROM t_project_approval pa
            WHERE pa.deleted = 0
              AND NOT (UPPER(pa.status) = 'REJECTED')
              AND NOT (
                  UPPER(pa.accept_status) = 'ARCHIVED'
                  AND COALESCE(pa.update_time, pa.create_time) &lt; DATE_SUB(NOW(), INTERVAL 7 DAY)
              )
            <if test="regionPrefix != null and regionPrefix != ''">
              AND EXISTS (
                  SELECT 1
                  FROM t_project_approval_declare pad
                  INNER JOIN t_declaration d ON d.id = pad.declaration_id AND d.deleted = 0
                  WHERE pad.approval_id = pa.id
                    AND d.village_group_code LIKE CONCAT(#{regionPrefix}, '%')
              )
            </if>
            ORDER BY pa.create_time DESC
            </script>
            """)
    List<ProjectProgressVO> selectProjectProgress(@Param("regionPrefix") String regionPrefix);

    /**
     * 统计指定 approvalId 集合的项目预算总额
     * 用于"按资金类型"分组维度中「预算」行的金额计算
     * WHERE id IN (...) AND deleted=0 AND budget IS NOT NULL
     */
    @Select({
            "<script>",
            "SELECT COALESCE(SUM(budget), 0)",
            "FROM t_project_approval",
            "WHERE deleted = 0 AND budget IS NOT NULL",
            "  <if test='ids != null and ids.size() > 0'>",
            "    AND id IN",
            "    <foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "      #{id}",
            "    </foreach>",
            "  </if>",
            "</script>"
    })
    BigDecimal selectBudgetSumByIds(@Param("ids") Collection<Long> ids);
}
