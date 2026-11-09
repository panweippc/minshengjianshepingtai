package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.MaterialStockInItemSaveDTO;
import com.msjs.project.dto.MaterialStockInSaveDTO;
import com.msjs.project.entity.MaterialStockIn;
import com.msjs.project.entity.MaterialStockInItem;
import com.msjs.project.entity.MaterialWarehouse;
import com.msjs.project.entity.MaterialItem;
import com.msjs.project.mapper.MaterialStockInItemMapper;
import com.msjs.project.mapper.MaterialStockInMapper;
import com.msjs.project.mapper.MaterialWarehouseMapper;
import com.msjs.project.mapper.MaterialItemMapper;
import com.msjs.project.service.MaterialStockInService;
import com.msjs.project.vo.MaterialStockInDetailVO;
import com.msjs.project.vo.MaterialStockInItemVO;
import com.msjs.project.vo.MaterialStockInVO;
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
 * 入库单服务实现
 * 入库单主表与明细表同事务写入，避免出现无明细的脏单或明细孤儿数据
 */
@Service
@RequiredArgsConstructor
public class MaterialStockInServiceImpl implements MaterialStockInService {

    private final MaterialStockInMapper orderMapper;
    private final MaterialStockInItemMapper itemMapper;
    private final MaterialWarehouseMapper warehouseMapper;
    private final MaterialItemMapper materialItemMapper;

    @Override
    public PageVO<MaterialStockInVO> pageOrders(long pageNum, long pageSize, String orderName, String handler) {
        Page<MaterialStockIn> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaterialStockIn> wrapper = new LambdaQueryWrapper<MaterialStockIn>()
                .like(StringUtils.hasText(orderName), MaterialStockIn::getOrderName, orderName)
                .like(StringUtils.hasText(handler), MaterialStockIn::getHandler, handler)
                .orderByDesc(MaterialStockIn::getInDate)
                .orderByDesc(MaterialStockIn::getCreateTime)
                .orderByAsc(MaterialStockIn::getId);
        IPage<MaterialStockIn> orderPage = orderMapper.selectPage(page, wrapper);

        List<MaterialStockInVO> records = orderPage.getRecords().stream()
                .map(this::toOrderVO)
                .toList();

        PageVO<MaterialStockInVO> result = new PageVO<>();
        result.setRecords(records);
        result.setTotal(orderPage.getTotal());
        result.setPageNum(orderPage.getCurrent());
        result.setPageSize(orderPage.getSize());
        return result;
    }

    @Override
    public PageVO<MaterialStockInItemVO> pageItems(long pageNum, long pageSize, String itemName,
                                                  Long warehouseId, String itemCategory, String inType) {
        Page<MaterialStockInItem> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaterialStockInItem> wrapper = new LambdaQueryWrapper<MaterialStockInItem>()
                .like(StringUtils.hasText(itemName), MaterialStockInItem::getItemName, itemName)
                .eq(warehouseId != null, MaterialStockInItem::getWarehouseId, warehouseId)
                .eq(StringUtils.hasText(itemCategory), MaterialStockInItem::getItemCategory, itemCategory)
                .eq(StringUtils.hasText(inType), MaterialStockInItem::getInType, inType)
                .orderByDesc(MaterialStockInItem::getInTime)
                .orderByDesc(MaterialStockInItem::getCreateTime)
                .orderByAsc(MaterialStockInItem::getId);
        IPage<MaterialStockInItem> itemPage = itemMapper.selectPage(page, wrapper);

        Map<Long, MaterialStockIn> orderMap = loadOrderMap(itemPage.getRecords());
        List<MaterialStockInItemVO> records = itemPage.getRecords().stream()
                .map(item -> toItemVO(item, orderMap.get(item.getOrderId())))
                .toList();

        PageVO<MaterialStockInItemVO> result = new PageVO<>();
        result.setRecords(records);
        result.setTotal(itemPage.getTotal());
        result.setPageNum(itemPage.getCurrent());
        result.setPageSize(itemPage.getSize());
        return result;
    }

    @Override
    public List<MaterialStockInItemVO> listItemsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<MaterialStockInItem> items = itemMapper.selectBatchIds(ids);
        Map<Long, MaterialStockIn> orderMap = loadOrderMap(items);
        return items.stream()
                .map(item -> toItemVO(item, orderMap.get(item.getOrderId())))
                .toList();
    }

    @Override
    public MaterialStockInDetailVO detail(Long id) {
        MaterialStockIn order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("入库单不存在或已删除");
        }
        List<MaterialStockInItem> items = itemMapper.selectList(new LambdaQueryWrapper<MaterialStockInItem>()
                .eq(MaterialStockInItem::getOrderId, id)
                .orderByAsc(MaterialStockInItem::getInTime)
                .orderByAsc(MaterialStockInItem::getId));

        MaterialStockInDetailVO vo = new MaterialStockInDetailVO();
        vo.setId(order.getId());
        vo.setOrderName(order.getOrderName());
        vo.setRemark(order.getRemark());
        vo.setInDate(order.getInDate());
        vo.setHandler(order.getHandler());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items.stream().map(item -> toItemVO(item, order)).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(MaterialStockInSaveDTO dto) {
        MaterialStockIn order = new MaterialStockIn();
        order.setOrderName(dto.getOrderName());
        order.setRemark(dto.getRemark());
        // 经办人取当前登录人，入库日期取明细中最早的入库时间
        order.setHandler(currentHandler());
        order.setInDate(earliestInDate(dto.getItems()));
        orderMapper.insert(order);
        saveItems(order.getId(), dto.getItems());
        // 入库成功同步累加物品库存，与明细写入同事务，保证库存与单据一致
        applyStock(order.getId(), 1);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(MaterialStockInSaveDTO dto) {
        MaterialStockIn exist = orderMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("入库单不存在或已删除");
        }
        exist.setOrderName(dto.getOrderName());
        exist.setRemark(dto.getRemark());
        exist.setInDate(earliestInDate(dto.getItems()));
        if (!StringUtils.hasText(exist.getHandler())) {
            exist.setHandler(currentHandler());
        }
        orderMapper.updateById(exist);

        // 先按旧明细回退库存，再按新明细重新累加，避免编辑后库存与单据不符
        applyStock(exist.getId(), -1);
        // 明细整体覆盖：先逻辑删除旧明细，再写入新明细，保证编辑后明细与表单一致
        itemMapper.delete(new LambdaQueryWrapper<MaterialStockInItem>()
                .eq(MaterialStockInItem::getOrderId, exist.getId()));
        saveItems(exist.getId(), dto.getItems());
        applyStock(exist.getId(), 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MaterialStockIn exist = orderMapper.selectById(id);
        if (exist == null) {
            throw new BizException("入库单不存在或已删除");
        }
        // 删除入库单前先回退已累加的库存
        applyStock(id, -1);
        itemMapper.delete(new LambdaQueryWrapper<MaterialStockInItem>()
                .eq(MaterialStockInItem::getOrderId, id));
        orderMapper.deleteById(id);
    }

    /**
     * 按入库单明细同步物品库存
     * 明细优先用 itemId 关联物品库，未选择物品时按物品名称匹配，避免手工录入导致库存不更新
     *
     * @param orderId 入库单ID
     * @param sign    1 表示累加，-1 表示回退
     */
    private void applyStock(Long orderId, int sign) {
        List<MaterialStockInItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<MaterialStockInItem>()
                        .eq(MaterialStockInItem::getOrderId, orderId));
        if (items.isEmpty()) {
            return;
        }
        // 未关联物品ID的明细，按名称一次性匹配物品库，避免逐条查询
        Set<String> names = items.stream()
                .filter(item -> item.getItemId() == null)
                .map(MaterialStockInItem::getItemName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, Long> nameToId = new HashMap<>();
        if (!names.isEmpty()) {
            List<MaterialItem> matched = materialItemMapper.selectList(
                    new LambdaQueryWrapper<MaterialItem>().in(MaterialItem::getItemName, names));
            // 同名多条时取首条，避免同一名称重复累加
            matched.forEach(mi -> nameToId.putIfAbsent(mi.getItemName(), mi.getId()));
        }
        for (MaterialStockInItem item : items) {
            Long itemId = item.getItemId() != null ? item.getItemId() : nameToId.get(item.getItemName());
            Integer qty = item.getQuantity();
            if (itemId == null || qty == null || qty == 0) {
                continue;
            }
            materialItemMapper.addStock(itemId, sign * qty);
        }
    }

    /** 写入入库单明细，仓库名称以仓库表为准，防止前端传入不一致的名称 */
    private void saveItems(Long orderId, List<MaterialStockInItemSaveDTO> items) {
        Map<Long, String> warehouseNames = loadWarehouseNames(items);
        for (MaterialStockInItemSaveDTO dto : items) {
            MaterialStockInItem item = new MaterialStockInItem();
            BeanUtils.copyProperties(dto, item);
            item.setId(null);
            item.setOrderId(orderId);
            item.setWarehouseName(warehouseNames.get(dto.getWarehouseId()));
            itemMapper.insert(item);
        }
    }

    /** 批量校验并查询仓库名称 */
    private Map<Long, String> loadWarehouseNames(List<MaterialStockInItemSaveDTO> items) {
        List<Long> warehouseIds = items.stream()
                .map(MaterialStockInItemSaveDTO::getWarehouseId)
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

    /** 入库日期：明细中最早的入库时间，全部为空时取当天 */
    private LocalDate earliestInDate(List<MaterialStockInItemSaveDTO> items) {
        return items.stream()
                .map(MaterialStockInItemSaveDTO::getInTime)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    /** 当前登录人姓名（无姓名时取账号） */
    private String currentHandler() {
        LoginUser user = UserContext.get();
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    /** 批量加载入库单主表信息 */
    private Map<Long, MaterialStockIn> loadOrderMap(List<MaterialStockInItem> items) {
        List<Long> orderIds = items.stream()
                .map(MaterialStockInItem::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(MaterialStockIn::getId, Function.identity()));
    }

    private MaterialStockInVO toOrderVO(MaterialStockIn order) {
        MaterialStockInVO vo = new MaterialStockInVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    private MaterialStockInItemVO toItemVO(MaterialStockInItem item, MaterialStockIn order) {
        MaterialStockInItemVO vo = new MaterialStockInItemVO();
        BeanUtils.copyProperties(item, vo);
        if (order != null) {
            vo.setOrderName(order.getOrderName());
            vo.setHandler(order.getHandler());
        }
        return vo;
    }
}
