import { get, post, put, del } from './request'
import type { PageVO, Result } from '@/types/common'

// ================== 类型定义（与后端 DeclarationSaveDTO / DeclarationDetailVO 对齐） ==================

export interface DeclarationQuery {
  pageNum: number
  pageSize: number
  title?: string
  declarationType?: string
  declarationCategory?: string
  status?: string
  townCode?: string
  villageGroupCode?: string
}

export interface DeclarationAttachmentItem {
  id?: string
  attachmentType?: string
  fileName?: string
  fileKey?: string
  bucket?: string
  fileSize?: number
  remark?: string
}

export interface DeclarationSaveDTO {
  id?: string
  /** 申报编号（业务编码），可选，弹窗预生成后回传；不传则后端自动生成 */
  businessCode?: string
  title: string
  townCode: string
  villageGroupCode: string
  declarationType: string
  declarationCategory: '1' | '2' | ''
  amount: number
  remark?: string
  /** 新增时可选：指定接收审核人用户ID，传了则直接进入待审核(submitted)状态 */
  recipientUserId?: string
  householdIds?: string[]
  facilityIds?: string[]
  attachments?: DeclarationAttachmentItem[]
}

export interface DeclarationVO {
  id: string
  businessCode: string
  title: string
  townCode: string
  townName: string
  villageGroupCode: string
  villageGroupName: string
  declarationType: string
  declarationCategory: string
  amount: number
  remark?: string
  status: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}

export interface UserRegionVO {
  currentRegionCode?: string
  currentRegionName?: string
  currentRegionType?: string
  currentRegionLevel?: number
  townCode?: string
  townName?: string
}

export interface VillageGroupOption {
  code: string
  name: string
  level: number
  regionType: string
}

export interface HouseholdOption {
  id: string
  householdNo: string
  headName: string
  contactPhone?: string
  householdType?: string
  householdTypeName?: string
}

export interface FacilityOption {
  id: string
  facilityCode: string
  facilityName: string
  facilityType?: string
  facilityTypeName?: string
  location?: string
}

// ================== 明细级类型（展开行用，与 DeclarationDetailVO 对齐） ==================

export interface DeclarationAttachment {
  id?: string
  attachmentType?: string
  fileName?: string
  fileKey?: string
  bucket?: string
  fileSize?: number
  remark?: string
  createTime?: string
}

export interface DeclarationHouseholdDetail {
  id: string                         // 明细ID（t_declaration_household.id）
  householdId?: string               // 农户档案ID
  snapshotHeadName?: string          // 快照-户主姓名
  snapshotHouseholdNo?: string      // 快照-户号（业务编码）
  snapshotHeadIdCard?: string
  snapshotContactPhone?: string
  snapshotHouseholdType?: string
  snapshotRegionName?: string
  amount?: number                    // 单户申报金额（可编辑）
  remark?: string                    // 事项说明（该户申报原因/情况说明，可编辑）
  /** 明细审核状态：pending 待审核 / approved 已通过 / rejected 已驳回 */
  auditStatus?: string
  auditBy?: string
  auditTime?: string
  auditRemark?: string
  attachmentCount?: number           // 附件数量
  attachments?: DeclarationAttachment[]  // 附件完整列表
}

export interface DeclarationFacilityDetail {
  id: string                         // 明细ID
  facilityId?: string                // 设施档案ID
  snapshotFacilityCode?: string
  snapshotFacilityName?: string
  snapshotFacilityType?: string
  snapshotLocation?: string
  snapshotRegionName?: string
  amount?: number                    // 单设施申报金额（可编辑）
  remark?: string                    // 事项说明（该设施申报原因/情况说明，可编辑）
  /** 明细审核状态：pending 待审核 / approved 已通过 / rejected 已驳回 */
  auditStatus?: string
  auditBy?: string
  auditTime?: string
  auditRemark?: string
  attachmentCount?: number
  attachments?: DeclarationAttachment[]
}

export interface DeclarationDetailFull extends DeclarationVO {
  attachments?: DeclarationAttachment[]        // 主表级附件
  households?: DeclarationHouseholdDetail[]   // 农户明细（category=1 时有）
  facilities?: DeclarationFacilityDetail[]    // 公建设施明细（category=2 时有）
}

export interface DeclarationDetailEditDTO {
  detailId: string
  declarationId: string
  amount?: number
  remark?: string
  attachments?: Array<{
    attachmentId?: string           // 编辑已有附件时传
    attachmentType?: string
    remark?: string
    fileKey?: string
    fileName?: string
    bucket?: string
    fileSize?: number
  }>
}

export interface DeclarationSubmitDTO {
  id: string
  /** 接收审核人（雪花ID，前端从 /api/declaration/recipients 下拉选） */
  recipientUserId: string
}

export interface DeclarationAuditDTO {
  id: string
  /** approve 通过 / reject 驳回 */
  action: 'approve' | 'reject'
  /** 驳回时必填，通过时可空 */
  auditRemark?: string
}

export interface DeclarationDetailAuditDTO {
  detailId: string
  /** household 农户 / public_facility 公建设施 */
  detailKind: 'household' | 'public_facility'
  /** approve 通过 / reject 驳回 / revoke 撤销 */
  action: 'approve' | 'reject' | 'revoke'
  /** 驳回 / 撤销时必填，通过时可空 */
  auditRemark?: string
}

export interface DeclarationDetailRevertDTO {
  detailId: string
  /** household 农户 / public_facility 公建设施 */
  detailKind: 'household' | 'public_facility'
  /** 退回备注（可空） */
  remark?: string
}

export interface RecipientOption {
  id: string
  username: string
  realName: string
  phone?: string
  regionCode?: string
  regionName?: string
}

// ================== API ==================

/** 分页查询（创建人视角） */
export function getDeclarationPage(params: DeclarationQuery) {
  return get<Result<PageVO<DeclarationVO>>>('/api/declaration/page', { params })
}

/** 接收申报分页（审核人视角：recipient_user_id=当前登录人） */
export function getReceivedDeclarationPage(params: DeclarationQuery) {
  return get<Result<PageVO<DeclarationVO>>>('/api/declaration/received-page', { params })
}

/** 详情（含农户/公建明细 + 附件树） */
export function getDeclarationDetail(id: string) {
  return get<Result<DeclarationDetailFull>>(`/api/declaration/detail/${id}`)
}

/** 新增 */
export function createDeclaration(data: DeclarationSaveDTO) {
  return post<Result<string>>('/api/declaration', data)
}

/** 编辑 */
export function updateDeclaration(data: DeclarationSaveDTO) {
  return put<Result<void>>('/api/declaration', data)
}

/** 删除 */
export function deleteDeclaration(id: string) {
  return del<Result<void>>(`/api/declaration/${id}`)
}

/**
 * 提交申报：指定接收审核人（draft → submitted）
 * 先调 getRecipients 让用户选，把选中的 id 放进 recipientUserId
 */
export function submitDeclaration(data: DeclarationSubmitDTO) {
  return post<Result<void>>('/api/declaration/submit', data)
}

/** 审核申报：approve 通过 / reject 驳回 */
export function auditDeclaration(data: DeclarationAuditDTO) {
  return post<Result<void>>('/api/declaration/audit', data)
}

/** 明细级审核：approve 通过 / reject 驳回（农户明细 / 公建设施明细通用） */
export function auditDeclarationDetail(data: DeclarationDetailAuditDTO) {
  return post<Result<void>>('/api/declaration/audit-detail', data)
}

/** 明细退回：把单条明细置回 pending，父申报单回滚到 draft（让申报人可重新编辑） */
export function revertDeclarationDetail(data: DeclarationDetailRevertDTO) {
  return post<Result<void>>('/api/declaration/revert-detail', data)
}

/** 审核人下拉数据源（keyword 支持按姓名/账号模糊搜，最多 100 条） */
export function getRecipients(keyword?: string) {
  return get<Result<RecipientOption[]>>('/api/declaration/recipients', { params: { keyword } })
}

/** 预生成下一个申报编号（新建弹窗打开时调用，只读展示） */
export function nextBusinessCode() {
  return get<Result<string>>('/api/declaration/next-business-code')
}

/** 校验申报编号是否唯一 */
export function checkBusinessCode(businessCode: string) {
  return get<Result<boolean>>('/api/declaration/check-business-code', { params: { businessCode } })
}

/** 编辑农户明细（改单户金额 + 明细级附件） */
export function editHouseholdDetail(data: DeclarationDetailEditDTO) {
  return put<Result<void>>('/api/declaration/household-detail', data)
}

/** 编辑公建设施明细（改单设施金额 + 明细级附件） */
export function editPublicFacilityDetail(data: DeclarationDetailEditDTO) {
  return put<Result<void>>('/api/declaration/public-facility-detail', data)
}

/** 当前登录人区划上下文（弹窗初始化） */
export function getCurrentUserRegion() {
  return get<Result<UserRegionVO>>('/api/declaration/user-region')
}

/** 乡镇下村组列表 */
export function getVillageGroups(townCode: string) {
  return get<Result<VillageGroupOption[]>>('/api/declaration/village-groups', { params: { townCode } })
}

/** 村组下农户列表（多选用） */
export function getHouseholdsByVillageGroup(villageGroupCode: string) {
  return get<Result<HouseholdOption[]>>('/api/declaration/households-by-vg', { params: { villageGroupCode } })
}

/** 村组下公建设施列表 */
export function getFacilitiesByVillageGroup(villageGroupCode: string) {
  return get<Result<FacilityOption[]>>('/api/declaration/facilities-by-vg', { params: { villageGroupCode } })
}

// ================== 立项对接（后端 /api/project/approval/submit） ==================

export interface ApprovalAttachmentItem {
  attachmentType?: string
  fileKey?: string
  bucket?: string
  fileName?: string
  fileSize?: number
  remark?: string
}

export interface ProjectApprovalSubmitDTO {
  approvalCode: string
  projectName: string
  projectCategory?: string
  projectType?: string
  projectSource?: string
  budget?: number | null
  leader?: string
  applyBy?: string
  remark?: string
  townCode?: string
  townName?: string
  villageCodes?: string[]
  declarationIds: string[]
  attachments?: ApprovalAttachmentItem[]
}

export function submitProjectApproval(data: ProjectApprovalSubmitDTO) {
  return post<Result<{
    id: string
    approvalCode: string
    status: string
    declareCount: number
    detailCount: number
    attachmentCount: number
  }>>('/api/project/approval/submit', data)
}
