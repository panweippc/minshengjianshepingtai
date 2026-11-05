package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.BaseFacilityProject;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设施-项目关联 Mapper
 */
@Mapper
public interface BaseFacilityProjectMapper extends BaseMapper<BaseFacilityProject> {
}
