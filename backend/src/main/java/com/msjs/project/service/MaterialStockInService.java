package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.MaterialStockInSaveDTO;
import com.msjs.project.vo.MaterialStockInDetailVO;
import com.msjs.project.vo.MaterialStockInItemVO;
import com.msjs.project.vo.MaterialStockInVO;

import java.util.List;

/**
 * 入库单服务
 */
public interface MaterialStockInService {

    /**
     * 入库单分页查询（入库单页签）
     *
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @param orderName 入库单名称（模糊）
     * @param handler   经办人（模糊）
     */
    PageVO<MaterialStockInVO> pageOrders(long pageNum, long pageSize, String orderName, String handler);

    /**
     * 入库单明细分页查询（明细页签）
     *
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param itemName     物品名称（模糊）
     * @param warehouseId  库房ID
     * @param itemCategory 物品分类
     * @param inType       入库类型
     */
    PageVO<MaterialStockInItemVO> pageItems(long pageNum, long pageSize, String itemName,
                                           Long warehouseId, String itemCategory, String inType);

    /** 根据明细ID集合查询（导出用） */
    List<MaterialStockInItemVO> listItemsByIds(List<Long> ids);

    /** 入库单详情（查看/编辑回显） */
    MaterialStockInDetailVO detail(Long id);

    /** 新增入库单（主表与明细同事务保存） */
    Long add(MaterialStockInSaveDTO dto);

    /** 编辑入库单（主表更新、明细整体覆盖，同事务） */
    void edit(MaterialStockInSaveDTO dto);

    /** 删除入库单（主表与明细同事务逻辑删除） */
    void delete(Long id);
}
