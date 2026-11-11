package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.DeclarationAuditDTO;
import com.msjs.project.dto.DeclarationDetailAuditDTO;
import com.msjs.project.dto.DeclarationDetailEditDTO;
import com.msjs.project.dto.DeclarationDetailRevertDTO;
import com.msjs.project.dto.DeclarationSaveDTO;
import com.msjs.project.dto.DeclarationSubmitDTO;
import com.msjs.project.vo.DeclarationDetailVO;
import com.msjs.project.vo.UserRegionVO;

import java.util.List;
import java.util.Map;

/**
 * 事项申报管理 Service
 */
public interface DeclarationService {

    /** 分页查询申报单（创建人视角：默认按创建人过滤） */
    PageVO<DeclarationDetailVO> page(long pageNum, long pageSize, String title,
                                     String declarationType, String declarationCategory,
                                     String status, String townCode, String villageGroupCode);

    /**
     * 分页查询"接收申报"（审核人视角：recipient_user_id = 当前登录人）
     * 状态一般传 submitted 看"待审核"，但也可以查 approved/rejected 看历史
     */
    PageVO<DeclarationDetailVO> receivedPage(long pageNum, long pageSize, String title,
                                              String declarationType, String declarationCategory,
                                              String status);

    /** 申报详情（含农户/公建明细 + 附件树） */
    DeclarationDetailVO detail(Long id);

    /** 新增申报（主表 + 明细 + 附件同事务） */
    Long add(DeclarationSaveDTO dto);

    /** 编辑申报（主表 + 明细 + 附件同事务） */
    void edit(DeclarationSaveDTO dto);

    /** 删除申报（逻辑删除主表 + 明细 + 附件，事务保护）
     * 仅草稿状态允许删除
     */
    void delete(Long id);

    /**
     * 提交申报单（草稿 draft → 待审核 submitted）
     * 提交时必须指定接收审核人（recipientUserId）
     * 仅草稿状态允许提交
     */
    void submit(DeclarationSubmitDTO dto);

    /**
     * 审核申报：approve 通过 / reject 驳回
     * 仅 submitted 状态允许审核；驳回时 auditRemark 必填
     * 只有被指派的 recipient_user_id 或更高权限的用户能审核
     * 注意：此接口是"申报单整体快速审核"；推荐用明细级审核接口逐行审核
     */
    void audit(DeclarationAuditDTO dto);

    /**
     * 明细级审核：approve 通过 / reject 驳回（农户明细 / 公建设施明细通用）
     * 每个明细独立流转；所有明细 approved 时父申报单自动推进到 approved，
     * 任一明细 rejected 且无 pending 时父申报单自动推进到 rejected
     * 驳回时 auditRemark 必填；仅 submitted 状态的父申报单允许明细审核
     */
    void auditDetail(DeclarationDetailAuditDTO dto);

    /**
     * 明细撤销：收回某条明细已做出的审核结论，把 audit_status 置回 pending 并清空审核痕迹，
     * 同时父申报单回到 submitted（待审核）、保留 recipient_user_id 审核指派。
     * 撤销后该单回到"申报审核-待审核"页签可重新通过/驳回，申报管理侧显示"待审核"且不可编辑。
     */
    void revertDetail(DeclarationDetailRevertDTO dto);

    /**
     * 审核人下拉数据源：返回 status=1 的启用用户（id + 姓名 + 区划名）
     * keyword 支持按姓名/账号模糊搜索
     */
    List<Map<String, Object>> listRecipients(String keyword);

    /**
     * 预生成下一个业务编号（前端弹窗打开时调用，只读展示）
     */
    String generateBusinessCode();

    /**
     * 校验业务编号是否唯一（保存前可调用，保存时后端也会再校验一次）
     * @return true 唯一可用；false 已存在
     */
    boolean isBusinessCodeUnique(String businessCode);

    /**
     * 编辑农户明细：改单户金额 + 事项说明 + 明细级附件
     * 仅草稿/待审核状态允许修改
     */
    void editHouseholdDetail(DeclarationDetailEditDTO dto);

    /**
     * 编辑公建设施明细：改单设施金额 + 事项说明 + 明细级附件
     */
    void editPublicFacilityDetail(DeclarationDetailEditDTO dto);

    /**
     * 解析当前登录人区划，向上递归找所属乡镇
     * 弹窗初始化时调用，前端拿到 townCode 回填只读字段
     */
    UserRegionVO resolveUserTown();

    /**
     * 获取指定乡镇下的全部村组（下拉数据源）
     */
    List<Map<String, Object>> listVillageGroupsByTown(String townCode);

    /**
     * 获取指定村组下的全部农户（多选数据源，弹窗用）
     */
    List<Map<String, Object>> listHouseholdsByVillageGroup(String villageGroupCode);

    /**
     * 获取指定村组下的全部公建设施（多选数据源，弹窗用）
     */
    List<Map<String, Object>> listFacilitiesByVillageGroup(String villageGroupCode);
}
