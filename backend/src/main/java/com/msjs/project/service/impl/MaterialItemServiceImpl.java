package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.PageVO;
import com.msjs.project.dto.MaterialItemSaveDTO;
import com.msjs.project.entity.MaterialItem;
import com.msjs.project.mapper.MaterialItemMapper;
import com.msjs.project.service.MaterialItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 物品信息服务实现
 */
@Service
@RequiredArgsConstructor
public class MaterialItemServiceImpl implements MaterialItemService {

    private final MaterialItemMapper itemMapper;

    @Override
    public PageVO<MaterialItem> page(long pageNum, long pageSize, String itemName,
                                     String itemCategory, Boolean lowWarning) {
        Page<MaterialItem> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaterialItem> wrapper = new LambdaQueryWrapper<MaterialItem>()
                .like(itemName != null && !itemName.isBlank(), MaterialItem::getItemName, itemName)
                .eq(itemCategory != null && !itemCategory.isBlank(), MaterialItem::getItemCategory, itemCategory)
                // 低于预警：库存数量 < 预警数量
                .apply(Boolean.TRUE.equals(lowWarning), "stock_qty < warning_qty")
                .orderByDesc(MaterialItem::getCreateTime)
                .orderByAsc(MaterialItem::getId);
        return PageVO.of(itemMapper.selectPage(page, wrapper));
    }

    @Override
    public List<MaterialItem> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return itemMapper.selectBatchIds(ids);
    }

    @Override
    public Long add(MaterialItemSaveDTO dto) {
        MaterialItem item = new MaterialItem();
        BeanUtils.copyProperties(dto, item);
        item.setId(null);
        // 新增时库存数量默认 0（库存通过出入库流程变更，此处不允许直接设置）
        item.setStockQty(0);
        if (item.getWarningQty() == null) {
            item.setWarningQty(0);
        }
        itemMapper.insert(item);
        return item.getId();
    }

    @Override
    public void edit(MaterialItemSaveDTO dto) {
        MaterialItem exist = itemMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("物品不存在");
        }
        // 库存数量不通过编辑接口修改，保留原值
        Integer stockQty = exist.getStockQty();
        BeanUtils.copyProperties(dto, exist);
        exist.setStockQty(stockQty);
        if (exist.getWarningQty() == null) {
            exist.setWarningQty(0);
        }
        itemMapper.updateById(exist);
    }

    @Override
    public void delete(Long id) {
        MaterialItem exist = itemMapper.selectById(id);
        if (exist == null) {
            throw new BizException("物品不存在");
        }
        itemMapper.deleteById(id);
    }
}
