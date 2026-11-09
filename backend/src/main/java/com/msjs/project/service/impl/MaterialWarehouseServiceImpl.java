package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.PageVO;
import com.msjs.project.dto.MaterialWarehouseSaveDTO;
import com.msjs.project.entity.MaterialWarehouse;
import com.msjs.project.mapper.MaterialWarehouseMapper;
import com.msjs.project.service.MaterialWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 库房信息服务实现
 */
@Service
@RequiredArgsConstructor
public class MaterialWarehouseServiceImpl implements MaterialWarehouseService {

    private final MaterialWarehouseMapper warehouseMapper;

    @Override
    public PageVO<MaterialWarehouse> page(long pageNum, long pageSize, String warehouseName,
                                          String location, String manager, String phone) {
        Page<MaterialWarehouse> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaterialWarehouse> wrapper = new LambdaQueryWrapper<MaterialWarehouse>()
                .like(warehouseName != null && !warehouseName.isBlank(),
                        MaterialWarehouse::getWarehouseName, warehouseName)
                .like(location != null && !location.isBlank(), MaterialWarehouse::getLocation, location)
                .like(manager != null && !manager.isBlank(), MaterialWarehouse::getManager, manager)
                .like(phone != null && !phone.isBlank(), MaterialWarehouse::getPhone, phone)
                .orderByDesc(MaterialWarehouse::getCreateTime)
                .orderByAsc(MaterialWarehouse::getId);
        return PageVO.of(warehouseMapper.selectPage(page, wrapper));
    }

    @Override
    public Long add(MaterialWarehouseSaveDTO dto) {
        MaterialWarehouse warehouse = new MaterialWarehouse();
        BeanUtils.copyProperties(dto, warehouse);
        warehouse.setId(null);
        warehouseMapper.insert(warehouse);
        return warehouse.getId();
    }

    @Override
    public void edit(MaterialWarehouseSaveDTO dto) {
        MaterialWarehouse exist = warehouseMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("库房不存在");
        }
        BeanUtils.copyProperties(dto, exist);
        warehouseMapper.updateById(exist);
    }

    @Override
    public void delete(Long id) {
        MaterialWarehouse exist = warehouseMapper.selectById(id);
        if (exist == null) {
            throw new BizException("库房不存在");
        }
        warehouseMapper.deleteById(id);
    }
}
