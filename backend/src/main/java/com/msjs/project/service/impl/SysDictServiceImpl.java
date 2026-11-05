package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.PageVO;
import com.msjs.project.dto.DictSaveDTO;
import com.msjs.project.entity.SysDict;
import com.msjs.project.mapper.SysDictMapper;
import com.msjs.project.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 数据字典服务实现
 * 同一字典类型下键值唯一，由服务层校验（逻辑删除后允许重建同键值）
 */
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements SysDictService {

    private final SysDictMapper dictMapper;

    @Override
    public PageVO<SysDict> page(long pageNum, long pageSize, String dictType, String description, String dictLabel) {
        Page<SysDict> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<SysDict>()
                .like(dictType != null && !dictType.isBlank(), SysDict::getDictType, dictType)
                .like(description != null && !description.isBlank(), SysDict::getDescription, description)
                .like(dictLabel != null && !dictLabel.isBlank(), SysDict::getDictLabel, dictLabel)
                .orderByAsc(SysDict::getDictType)
                .orderByAsc(SysDict::getSort)
                .orderByAsc(SysDict::getId);
        return PageVO.of(dictMapper.selectPage(page, wrapper));
    }

    @Override
    public List<String> types() {
        QueryWrapper<SysDict> wrapper = new QueryWrapper<SysDict>()
                .select("DISTINCT dict_type")
                .orderByAsc("dict_type");
        return dictMapper.selectObjs(wrapper).stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public Long add(DictSaveDTO dto) {
        checkValueUnique(dto.getDictType(), dto.getDictValue(), null);
        SysDict dict = new SysDict();
        BeanUtils.copyProperties(dto, dict);
        dict.setId(null);
        if (dict.getSort() == null) {
            dict.setSort(0);
        }
        dictMapper.insert(dict);
        return dict.getId();
    }

    @Override
    public void edit(DictSaveDTO dto) {
        SysDict exist = dictMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("字典不存在");
        }
        checkValueUnique(dto.getDictType(), dto.getDictValue(), dto.getId());
        BeanUtils.copyProperties(dto, exist);
        if (exist.getSort() == null) {
            exist.setSort(0);
        }
        dictMapper.updateById(exist);
    }

    @Override
    public void delete(Long id) {
        SysDict exist = dictMapper.selectById(id);
        if (exist == null) {
            throw new BizException("字典不存在");
        }
        dictMapper.deleteById(id);
    }

    /** 校验同一字典类型下键值唯一 */
    private void checkValueUnique(String dictType, String dictValue, Long excludeId) {
        Long count = dictMapper.selectCount(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, dictType)
                .eq(SysDict::getDictValue, dictValue)
                .ne(excludeId != null, SysDict::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("字典类型「" + dictType + "」下键值「" + dictValue + "」已存在");
        }
    }
}
