package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.ProjectConstructionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 施工管理-施工日志/巡检记录 Mapper
 */
@Mapper
public interface ProjectConstructionLogMapper extends BaseMapper<ProjectConstructionLog> {
}
