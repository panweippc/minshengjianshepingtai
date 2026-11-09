package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.MaterialStockOutItemSaveDTO;
import com.msjs.project.dto.MaterialStockOutSaveDTO;
import com.msjs.project.entity.MaterialItem;
import com.msjs.project.entity.MaterialStockOut;
import com.msjs.project.entity.MaterialStockOutItem;
import com.msjs.project.entity.MaterialWarehouse;
import com.msjs.project.mapper.MaterialItemMapper;
import com.msjs.project.mapper.MaterialStockOutItemMapper;
import com.msjs.project.mapper.MaterialStockOutMapper;
import com.msjs.project.mapper.MaterialWarehouseMapper;
import com.msjs.project.service.MaterialStockOutService;
import com.msjs.project.vo.MaterialStockOutDetailVO;
import com.msjs.project.vo.MaterialStockOutItemVO;
import com.msjs.project.vo.MaterialStockOutVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 出库单服务实现
 * 出库单主表与明细表同事务写入，避免出现无明细的脏单或明细孤儿数据
 * 主表经办人优先取明细中非空的经办人，全部为空时取当前登录人
 */
@Service
@RequiredArgsConstructor
public class MaterialStockOutServiceImpl implements MaterialStockOutService {

    private final MaterialStockOutMapper orderMapper;
    private final MaterialStockOutItemMapper itemMapper;
    private final MaterialWarehouseMapper warehouseMapper;
    private final MaterialItemMapper materialItemMapper;

    @Override
    public PageVO<MaterialStockOutVO> pageOrders(long pageNum, long pageSize, String orderName, String handler) {
        Page<MaterialStockOut> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaterialStockOut> wrapper = new LambdaQueryWrapper<MaterialStockOut>()
                .like(StringUtils.hasText(orderName), MaterialStockOut::getOrderName, orderName)
                .like(StringUtils.hasText(handler), MaterialStockOut::getHandler, handler)
                .orderByDesc(MaterialStockOut::getOutDate)
                .orderByDesc(MaterialStockOut::getCreateTime)
                .orderByAsc(MaterialStockOut::getId);
        IPage<MaterialStockOut> orderPage = orderMapper.selectPage(page, wrapper);

        List<MaterialStockOutVO> records = orderPage.getRecords().stream()
                .map(this::toOrderVO)
                .toList();

        PageVO<MaterialStockOutVO> result = new PageVO<>();
        result.setRecords(records);
        result.setTotal(orderPage.getTotal());
        result.setPageNum(orderPage.getCurrent());
        result.setPageSize(orderPage.getSize());
        return result;
    }

    @Override
    public PageVO<MaterialStockOutItemVO> pageItems(long pageNum, long pageSize, String itemName,
                                                    Long warehouseId, String itemCategory, String outType) {
        Page<MaterialStockOutItem> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaterialStockOutItem> wrapper = new LambdaQueryWrapper<MaterialStockOutItem>()
                .like(StringUtils.hasText(itemName), MaterialStockOutItem::getItemName, itemName)
                .eq(warehouseId != null, MaterialStockOutItem::getWarehouseId, warehouseId)
                .eq(StringUtils.hasText(itemCategory), MaterialStockOutItem::getItemCategory, itemCategory)
                .eq(StringUtils.hasText(outType), MaterialStockOutItem::getOutType, outType)
                .orderByDesc(MaterialStockOutItem::getOutTime)
                .orderByDesc(MaterialStockOutItem::getCreateTime)
                .orderByAsc(MaterialStockOutItem::getId);
        IPage<MaterialStockOutItem> itemPage = itemMapper.selectPage(page, wrapper);

        Map<Long, MaterialStockOut> orderMap = loadOrderMap(itemPage.getRecords());
        List<MaterialStockOutItemVO> records = itemPage.getRecords().stream()
                .map(item -> toItemVO(item, orderMap.get(item.getOrderId())))
                .toList();

        PageVO<MaterialStockOutItemVO> result = new PageVO<>();
        result.setRecords(records);
        result.setTotal(itemPage.getTotal());
        result.setPageNum(itemPage.getCurrent());
        result.setPageSize(itemPage.getSize());
        return result;
    }

    @Override
    public List<MaterialStockOutItemVO> listItemsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<MaterialStockOutItem> items = itemMapper.selectBatchIds(ids);
        Map<Long, MaterialStockOut> orderMap = loadOrderMap(items);
        return items.stream()
                .map(item -> toItemVO(item, orderMap.get(item.getOrderId())))
                .toList();
    }

    @Override
    public MaterialStockOutDetailVO detail(Long id) {
        MaterialStockOut order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("出库单不存在或已删除");
        }
        List<MaterialStockOutItem> items = itemMapper.selectList(new LambdaQueryWrapper<MaterialStockOutItem>()
                .eq(MaterialStockOutItem::getOrderId, id)
                .orderByAsc(MaterialStockOutItem::getOutTime)
                .orderByAsc(MaterialStockOutItem::getId));

        MaterialStockOutDetailVO vo = new MaterialStockOutDetailVO();
        vo.setId(order.getId());
        vo.setOrderName(order.getOrderName());
        vo.setRemark(order.getRemark());
        vo.setOutDate(order.getOutDate());
        vo.setHandler(order.getHandler());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items.stream().map(item -> toItemVO(item, order)).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(MaterialStockOutSaveDTO dto) {
        MaterialStockOut order = new MaterialStockOut();
        order.setOrderName(dto.getOrderName());
        order.setRemark(dto.getRemark());
        // 主表经办人优先取明细中非空的，全部为空取当前登录人；出库日期取明细最早的出库时间
        order.setHandler(resolveHandler(dto.getItems()));
        order.setOutDate(earliestOutDate(dto.getItems()));
        orderMapper.insert(order);
        saveItems(order.getId(), dto.getItems());
        // 出库成功同步扣减物品库存，与明细写入同事务，库存不足时整单回滚
        applyStock(order.getId(), 1);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(MaterialStockOutSaveDTO dto) {
        MaterialStockOut exist = orderMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("出库单不存在或已删除");
        }
        exist.setOrderName(dto.getOrderName());
        exist.setRemark(dto.getRemark());
        exist.setOutDate(earliestOutDate(dto.getItems()));
        exist.setHandler(resolveHandler(dto.getItems()));
        orderMapper.updateById(exist);

        // 先按旧明细回退库存，再按新明细重新扣减，避免编辑后库存与单据不符
        applyStock(exist.getId(), -1);
        // 明细整体覆盖：先逻辑删除旧明细，再写入新明细，保证编辑后明细与表单一致
        itemMapper.delete(new LambdaQueryWrapper<MaterialStockOutItem>()
                .eq(MaterialStockOutItem::getOrderId, exist.getId()));
        saveItems(exist.getId(), dto.getItems());
        applyStock(exist.getId(), 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MaterialStockOut exist = orderMapper.selectById(id);
        if (exist == null) {
            throw new BizException("出库单不存在或已删除");
        }
        // 删除出库单前先把已扣减的库存加回
        applyStock(id, -1);
        itemMapper.delete(new LambdaQueryWrapper<MaterialStockOutItem>()
                .eq(MaterialStockOutItem::getOrderId, id));
        orderMapper.deleteById(id);
    }

    /**
     * 按出库单明细同步物品库存
     * 明细优先用 itemId 关联物品库，未选择物品时按物品名称匹配，避免手工录入导致库存不更新
     *
     * @param orderId 出库单ID
     * @param sign    1 表示出库扣减，-1 表示删除/编辑回退
     */
    private void applyStock(Long orderId, int sign) {
        List<MaterialStockOutItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<MaterialStockOutItem>()
                        .eq(MaterialStockOutItem::getOrderId, orderId));
        if (items.isEmpty()) {
            return;
        }
        // 未关联物品ID的明细，按名称一次性匹配物品库，避免逐条查询
        Set<String> names = items.stream()
                .filter(item -> item.getItemId() == null)
                .map(MaterialStockOutItem::getItemName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, Long> nameToId = new HashMap<>();
        if (!names.isEmpty()) {
            List<MaterialItem> matched = materialItemMapper.selectList(
                    new LambdaQueryWrapper<MaterialItem>().in(MaterialItem::getItemName, names));
            matched.forEach(mi -> nameToId.putIfAbsent(mi.getItemName(), mi.getId()));
        }
        for (MaterialStockOutItem item : items) {
            Long itemId = item.getItemId() != null ? item.getItemId() : nameToId.get(item.getItemName());
            Integer qty = item.getQuantity();
            if (itemId == null || qty == null || qty == 0) {
                continue;
            }
            if (sign > 0) {
                // 出库扣减：库存不足时由 SQL 条件保证不更新，直接提示并回滚整单
                if (materialItemMapper.deductStock(itemId, qty) == 0) {
                    throw new BizException("物品「" + item.getItemName() + "」库存不足，当前无法出库 " + qty);
                }
            } else {
                materialItemMapper.addStock(itemId, qty);
            }
        }
    }

    /** 写入出库单明细，仓库名称以仓库表为准，防止前端传入不一致的名称 */
    private void saveItems(Long orderId, List<MaterialStockOutItemSaveDTO> items) {
        Map<Long, String> warehouseNames = loadWarehouseNames(items);
        for (MaterialStockOutItemSaveDTO dto : items) {
            MaterialStockOutItem item = new MaterialStockOutItem();
            BeanUtils.copyProperties(dto, item);
            item.setId(null);
            item.setOrderId(orderId);
            item.setWarehouseName(warehouseNames.get(dto.getWarehouseId()));
            itemMapper.insert(item);
        }
    }

    /** 批量校验并查询仓库名称 */
    private Map<Long, String> loadWarehouseNames(List<MaterialStockOutItemSaveDTO> items) {
        List<Long> warehouseIds = items.stream()
                .map(MaterialStockOutItemSaveDTO::getWarehouseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (warehouseIds.isEmpty()) {
            throw new BizException("请选择仓库");
        }
        List<MaterialWarehouse> warehouses = warehouseMapper.selectBatchIds(warehouseIds);
        Map<Long, String> nameMap = warehouses.stream()
                .collect(Collectors.toMap(MaterialWarehouse::getId, MaterialWarehouse::getWarehouseName));
        for (Long warehouseId : warehouseIds) {
            if (!nameMap.containsKey(warehouseId)) {
                throw new BizException("所选仓库不存在，请刷新后重试");
            }
        }
        return nameMap;
    }

    /** 出库日期：明细中最早的出库时间，全部为空时取当天 */
    private LocalDate earliestOutDate(List<MaterialStockOutItemSaveDTO> items) {
        return items.stream()
                .map(MaterialStockOutItemSaveDTO::getOutTime)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    /**
     * 主表经办人解析：优先取明细中第一个非空的经办人，全部为空时取当前登录人
     * 明细每行经办人独立，主表取代表性值用于出库单页签展示
     */
    private String resolveHandler(List<MaterialStockOutItemSaveDTO> items) {
        if (items != null) {
            for (MaterialStockOutItemSaveDTO item : items) {
                if (StringUtils.hasText(item.getHandler())) {
                    return item.getHandler().trim();
                }
            }
        }
        return currentHandler();
    }

    /** 当前登录人姓名（无姓名时取账号） */
    private String currentHandler() {
        LoginUser user = UserContext.get();
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    /** 批量加载出库单主表信息 */
    private Map<Long, MaterialStockOut> loadOrderMap(List<MaterialStockOutItem> items) {
        List<Long> orderIds = items.stream()
                .map(MaterialStockOutItem::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(MaterialStockOut::getId, Function.identity()));
    }

    private MaterialStockOutVO toOrderVO(MaterialStockOut order) {
        MaterialStockOutVO vo = new MaterialStockOutVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    private MaterialStockOutItemVO toItemVO(MaterialStockOutItem item, MaterialStockOut order) {
        MaterialStockOutItemVO vo = new MaterialStockOutItemVO();
        BeanUtils.copyProperties(item, vo);
        if (order != null) {
            vo.setOrderName(order.getOrderName());
        }
        // 明细行优先展示自身的经办人，未设置时回退到主表经办人
        if (!StringUtils.hasText(vo.getHandler()) && order != null) {
            vo.setHandler(order.getHandler());
        }
        return vo;
    }
}
