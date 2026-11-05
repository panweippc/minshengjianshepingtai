package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.DictSaveDTO;
import com.msjs.project.entity.SysDict;

import java.util.List;

/**
 * 数据字典服务
 */
public interface SysDictService {

    /** 字典分页查询 */
    PageVO<SysDict> page(long pageNum, long pageSize, String dictType, String description, String dictLabel);

    /** 全部字典类型（筛选下拉用） */
    List<String> types();

    /** 按字典类型查询字典项（业务下拉联动用） */
    List<SysDict> listByType(String dictType);

    /** 新增字典 */
    Long add(DictSaveDTO dto);

    /** 编辑字典 */
    void edit(DictSaveDTO dto);

    /** 删除字典 */
    void delete(Long id);
}
