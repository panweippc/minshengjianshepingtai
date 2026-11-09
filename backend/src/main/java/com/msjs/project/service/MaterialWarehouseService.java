package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.MaterialWarehouseSaveDTO;
import com.msjs.project.entity.MaterialWarehouse;

/**
 * 库房信息服务
 */
public interface MaterialWarehouseService {

    /**
     * 库房分页查询
     *
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @param warehouseName 库房名称（模糊）
     * @param location      库房位置（模糊）
     * @param manager       负责人（模糊）
     * @param phone         联系电话（模糊）
     */
    PageVO<MaterialWarehouse> page(long pageNum, long pageSize, String warehouseName,
                                   String location, String manager, String phone);

    /** 新增库房 */
    Long add(MaterialWarehouseSaveDTO dto);

    /** 编辑库房 */
    void edit(MaterialWarehouseSaveDTO dto);

    /** 删除库房 */
    void delete(Long id);
}
