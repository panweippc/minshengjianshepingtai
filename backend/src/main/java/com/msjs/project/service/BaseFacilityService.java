package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.FacilitySaveDTO;
import com.msjs.project.entity.BaseFacility;
import com.msjs.project.vo.FacilityDetailVO;

/**
 * 公共基础设施服务接口
 */
public interface BaseFacilityService {

    /**
     * 分页查询
     */
    PageVO<BaseFacility> page(long pageNum, long pageSize, String facilityCode, String facilityName,
                              String facilityType, String nodeCode);

    /**
     * 详情（含附件和关联项目）
     */
    FacilityDetailVO detail(Long id);

    /**
     * 新增（含附件）
     */
    Long add(FacilitySaveDTO dto);

    /**
     * 编辑（含附件）
     */
    void edit(FacilitySaveDTO dto);

    /**
     * 删除（含附件级联删除）
     */
    void delete(Long id);
}
