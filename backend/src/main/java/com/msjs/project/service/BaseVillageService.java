package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.RegionSaveDTO;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.vo.RegionTreeVO;

import java.util.List;

/**
 * 村委会信息服务
 */
public interface BaseVillageService {

    /** 乡镇/村/组区划树 */
    List<RegionTreeVO> tree();

    /** 村委会分页查询（nodeCode 非空时含所选节点及其直属下级） */
    PageVO<SysRegion> page(long pageNum, long pageSize, String name, String secretary,
                           String director, String nodeCode);

    /** 树形查询：返回可直接用于树形表格的层级数据（筛选时自动补全祖先路径） */
    List<RegionTreeVO> treePage(String name, String secretary, String director, String nodeCode);

    /** 区划详情（按区划编码查询，业务编码全局唯一） */
    SysRegion detail(String code);

    /** 新增区划 */
    Long add(RegionSaveDTO dto);

    /** 编辑区划（按区划编码定位，编码不可修改） */
    void edit(RegionSaveDTO dto);

    /** 删除区划（按区划编码定位，存在下级则禁止） */
    void delete(String code);
}
