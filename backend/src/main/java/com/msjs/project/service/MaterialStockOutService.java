package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.MaterialStockOutSaveDTO;
import com.msjs.project.vo.MaterialStockOutDetailVO;
import com.msjs.project.vo.MaterialStockOutItemVO;
import com.msjs.project.vo.MaterialStockOutVO;

import java.util.List;

/**
 * 出库单服务
 */
public interface MaterialStockOutService {

    /**
     * 出库单分页查询（出库单页签）
     *
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @param orderName 出库单名称（模糊）
     * @param handler   经办人（模糊）
     */
    PageVO<MaterialStockOutVO> pageOrders(long pageNum, long pageSize, String orderName, String handler);

    /**
     * 出库单明细分页查询（明细页签）
     *
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param itemName     物品名称（模糊）
     * @param warehouseId  库房ID
     * @param itemCategory 物品分类
     * @param outType      出库类型
     */
    PageVO<MaterialStockOutItemVO> pageItems(long pageNum, long pageSize, String itemName,
                                             Long warehouseId, String itemCategory, String outType);

    /** 根据明细ID集合查询（导出用） */
    List<MaterialStockOutItemVO> listItemsByIds(List<Long> ids);

    /** 出库单详情（查看/编辑回显） */
    MaterialStockOutDetailVO detail(Long id);

    /** 新增出库单（主表与明细同事务保存） */
    Long add(MaterialStockOutSaveDTO dto);

    /** 编辑出库单（主表更新、明细整体覆盖，同事务） */
    void edit(MaterialStockOutSaveDTO dto);

    /** 删除出库单（主表与明细同事务逻辑删除） */
    void delete(Long id);
}
