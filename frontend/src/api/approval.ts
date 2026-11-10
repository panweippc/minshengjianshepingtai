import { get, post, put } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'

/** 立项附件子项（与后端 ApprovalEditDTO.AttachmentItem 对齐） */
export interface ApprovalAttachmentItem {
  /** 已有附件ID（编辑时回传，新增附件时不传） */
  id?: string
  /** 附件类型（字典 li_xiang_fu_jian_lei_xing） */
  attachmentType?: string
  /** 原始文件名 */
  fileName?: string
  /** MinIO 对象存储 key */
  fileKey?: string
  /** 存储桶 */
  bucket?: string
  /** 文件大小（字节） */
  fileSize?: number
  /** 附件备注 */
  remark?: string
}

/** 申报明细（树形三级：农户/公共基础设施） */
export interface ApprovalDetailNode {
  id: string
  declareId: string
  /** HOUSEHOLD农户 / FACILITY公共基础设施 */
  detailType: string
  targetId?: string
  targetName?: string
  targetCode?: string
  regionName?: string
  amount?: number
  quantity?: number
  remark?: string
  sort?: number
}

/** 申报内容（树形二级） */
export interface ApprovalDeclareNode {
  id: string
  approvalId: string
  /** 申报编号（业务编码） */
  declareCode?: string
  declareName: string
  declareCategory?: string
  declareType?: string
  /** 所属乡镇名称 */
  townName?: string
  /** 所属村组名称 */
  villageName?: string
  declareContent?: string
  budget?: number
  sort?: number
  children?: ApprovalDetailNode[]
  /** 申报材料附件（declare_id 指向该申报的附件） */
  attachments?: ApprovalAttachmentVO[]
}

/** 立项项目（树形一级） */
export interface ApprovalVO {
  id: string
  approvalCode: string
  projectName: string
  projectCategory?: string
  projectSource?: string
  budget?: number
  leader?: string
  submitTime?: string
  /** PENDING待审核 APPROVED已通过 REJECTED已驳回 */
  status: string
  regionCode?: string
  regionName?: string
  remark?: string
  createBy?: string
  createTime?: string
  children?: ApprovalDeclareNode[]
}

/** 立项附件 VO */
export interface ApprovalAttachmentVO {
  id: string
  approvalId: string
  /** 申报ID，为空表示立项资料附件 */
  declareId?: string
  attachmentType?: string
  fileName?: string
  fileKey?: string
  bucket?: string
  fileSize?: number
  remark?: string
  createTime?: string
}

/** 立项查询参数 */
export interface ApprovalQuery extends PageQuery {
  /** 页签：pending待审核 rejected已驳回 */
  tab: 'pending' | 'rejected'
  projectName?: string
  projectCategory?: string
  projectSource?: string
  /** 提交开始日期 yyyy-MM-dd */
  startTime?: string
  /** 提交结束日期 yyyy-MM-dd */
  endTime?: string
}

/** 立项编辑入参 */
export interface ApprovalEditDTO {
  id: string
  projectName: string
  projectCategory?: string
  projectSource?: string
  budget?: number
  leader?: string
  remark?: string
  attachments?: ApprovalAttachmentItem[]
}

/** 立项分页查询 */
export function getApprovalPage(params: ApprovalQuery) {
  return get<Result<PageVO<ApprovalVO>>>('/api/project/approval/page', { params })
}

/** 立项详情（含申报内容与明细树） */
export function getApprovalDetail(id: string) {
  return get<Result<ApprovalVO>>(`/api/project/approval/detail/${id}`)
}

/** 查询立项附件列表 */
export function getApprovalAttachments(approvalId: string) {
  return get<Result<ApprovalAttachmentVO[]>>(`/api/project/approval/attachments/${approvalId}`)
}

/** 编辑立项（仅待审核状态） */
export function updateApproval(data: ApprovalEditDTO) {
  return put<Result<void>>('/api/project/approval', data)
}

/** 审核通过 */
export function approveApproval(id: string) {
  return post<Result<void>>(`/api/project/approval/${id}/approve`)
}

/** 审核驳回 */
export function rejectApproval(id: string) {
  return post<Result<void>>(`/api/project/approval/${id}/reject`)
}

/** 立项各页签数量统计（按当前筛选条件） */
export function getApprovalCounts(params: {
  projectName?: string
  projectCategory?: string
  projectSource?: string
  startTime?: string
  endTime?: string
}) {
  return get<Result<Record<'all' | 'pending' | 'rejected', number>>>('/api/project/approval/counts', { params })
}

/** 退回（已驳回 → 待审核） */
export function returnApproval(id: string) {
  return post<Result<void>>(`/api/project/approval/${id}/return`)
}
