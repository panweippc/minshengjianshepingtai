package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.ProjectArchive;
import org.apache.ibatis.annotations.Mapper;

/**
 * 验收管理-项目归档记录 Mapper
 */
@Mapper
public interface ProjectArchiveMapper extends BaseMapper<ProjectArchive> {
}
