package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.MaterialItemSaveDTO;
import com.msjs.project.entity.MaterialItem;

import java.util.List;

/**
 * 物品信息服务
 */
public interface MaterialItemService {

    /**
     * 物品分页查询
     *
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param itemName     物品名称（模糊）
     * @param itemCategory 物品分类
     * @param lowWarning   是否仅查询低于预警数量的物品
     */
    PageVO<MaterialItem> page(long pageNum, long pageSize, String itemName, String itemCategory, Boolean lowWarning);

    /** 根据ID集合查询物品（导出用） */
    List<MaterialItem> listByIds(List<Long> ids);

    /** 新增物品 */
    Long add(MaterialItemSaveDTO dto);

    /** 编辑物品 */
    void edit(MaterialItemSaveDTO dto);

    /** 删除物品 */
    void delete(Long id);
}
