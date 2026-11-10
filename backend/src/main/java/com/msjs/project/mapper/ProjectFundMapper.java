package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.ProjectFund;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资金管理-拨付记录 Mapper
 */
@Mapper
public interface ProjectFundMapper extends BaseMapper<ProjectFund> {
}
