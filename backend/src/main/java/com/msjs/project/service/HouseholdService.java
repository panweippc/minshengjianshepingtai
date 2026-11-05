package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.HouseholdSaveDTO;
import com.msjs.project.entity.Household;
import com.msjs.project.entity.HouseholdAttachment;

import java.util.List;

/**
 * 户基础信息服务接口
 */
public interface HouseholdService {

    /** 分页查询 */
    PageVO<Household> page(long pageNum, long pageSize, String headName, String headIdCard, String nodeCode);

    /** 详情（含附件） */
    Household detail(Long id);

    /** 查询附件列表 */
    List<HouseholdAttachment> listAttachments(Long householdId);

    /** 新增（含附件批量保存，户号由用户录入） */
    Long add(HouseholdSaveDTO dto);

    /** 编辑（含附件更新） */
    void edit(HouseholdSaveDTO dto);

    /** 删除（主表 + 附件，事务保护） */
    void delete(Long id);
}
