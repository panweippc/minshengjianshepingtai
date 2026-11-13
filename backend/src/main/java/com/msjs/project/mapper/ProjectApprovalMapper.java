package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.vo.ProjectProgressVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProjectApprovalMapper extends BaseMapper<ProjectApproval> {

    /**
     * 首页项目进度概览：每个立项项目一条记录，按生命周期判定当前阶段
     * 阶段优先级：待审核（含已驳回，未通过审核）→ 待验收（已发起验收）
     * → 已归档（验收入档）→ 施工中（存在施工中明细）→ 已通过（其余已批准项目）
     * 已归档项目仅保留一周（按归档操作更新时间），超过一周不再展示
     */
    @Select("""
            SELECT
                pa.id AS id,
                pa.approval_code AS approvalCode,
                pa.project_name AS projectName,
                CASE
                    WHEN UPPER(pa.status) IN ('PENDING', 'REJECTED') THEN '待审核'
                    WHEN UPPER(pa.accept_status) = 'APPLYING' THEN '待验收'
                    WHEN UPPER(pa.accept_status) = 'ARCHIVED' THEN '已归档'
                    WHEN EXISTS (
                        SELECT 1 FROM t_project_construction pc
                        WHERE pc.approval_id = pa.id AND pc.deleted = 0
                          AND UPPER(pc.build_status) = 'IN_PROGRESS'
                    ) THEN '施工中'
                    ELSE '已通过'
                END AS stage,
                CASE
                    WHEN UPPER(pa.status) IN ('PENDING', 'REJECTED') THEN 1
                    WHEN UPPER(pa.accept_status) = 'APPLYING' THEN 4
                    WHEN UPPER(pa.accept_status) = 'ARCHIVED' THEN 5
                    WHEN EXISTS (
                        SELECT 1 FROM t_project_construction pc
                        WHERE pc.approval_id = pa.id AND pc.deleted = 0
                          AND UPPER(pc.build_status) = 'IN_PROGRESS'
                    ) THEN 3
                    ELSE 2
                END AS stageIndex
            FROM t_project_approval pa
            WHERE pa.deleted = 0
              AND NOT (
                  UPPER(pa.accept_status) = 'ARCHIVED'
                  AND COALESCE(pa.update_time, pa.create_time) < DATE_SUB(NOW(), INTERVAL 7 DAY)
              )
            ORDER BY pa.create_time DESC
            """)
    List<ProjectProgressVO> selectProjectProgress();
}
