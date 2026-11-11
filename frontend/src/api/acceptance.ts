import { get, post } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'

/** 验收节点类型：project项目 declare申报 detail明细 */
export type AcceptanceNodeType = 'project' | 'declare' | 'detail'

/** 验收资料附件 */
export interface AcceptanceAttachmentVO {
  id?: string
  acceptanceId?: string
  attachmentType?: string
  fileName?: string
  fileKey: string
  bucket?: string
  fileSize?: number
  remark?: string
  createTime?: string
}

/** 立项资料附件 */
export interface ApprovalAttachmentVO {
  id?: string
  attachmentType?: string
  fileName?: string
  fileKey: string
  bucket?: string
  fileSize?: number
  remark?: string
  createTime?: string
}

/** 验收树形节点（一级项目 → 二级申报 → 三级明细） */
export interface AcceptanceNodeVO {
  id: string
  nodeType: AcceptanceNodeType
  /** 明细类型：HOUSEHOLD农户 FACILITY公共基础设施（仅三级） */
  detailType?: string
  /** 立项ID */
  approvalId?: string
  /** 申报ID */
  declareId?: string
  /** 明细ID（仅三级） */
  detailId?: string
  /** 编号：项目编号/申报编号/户号或设施编码 */
  code?: string
  /** 名称：项目名称/申报标题/农户或设施名称 */
  name?: string
  /** 项目类别（一级：字典 xiang_mu_lei_bie；二级：字典 shen_bao_lei_bie；三级：明细类型） */
  category?: string
  /** 项目类型（一级：字典 xiang_mu_lei_xing；二级：字典 shen_bao_lei_xing；三级：所属区划） */
  source?: string
  /** 提交时间（仅一级） */
  submitTime?: string
  /** 项目预算 / 申报金额 / 补助金额 */
  budget?: number
  /** 全部明细数 */
  totalCount?: number
  /** 已验收明细数 */
  acceptedCount?: number
  /** 验收进度百分比（0-100） */
  percent?: number
  /** 验收状态（仅三级：PENDING待验收 ACCEPTED已验收） */
  acceptStatus?: string
  /** 验收时间（仅三级已验收时） */
  acceptTime?: string
  /** 验收人（仅三级已验收时） */
  acceptBy?: string
  /** 验收备注（仅三级） */
  acceptRemark?: string
  /** 事项说明（仅三级） */
  detailRemark?: string
  /** 申报内容（仅二级） */
  declareContent?: string
  /** 验收资料附件（仅三级） */
  acceptanceAttachments?: AcceptanceAttachmentVO[]
  /** 立项资料附件（仅一级查看弹窗使用） */
  approvalAttachments?: ApprovalAttachmentVO[]
  /** 所属乡镇（仅二级） */
  townName?: string
  /** 所属村组（仅二三级） */
  villageName?: string
  /** 是否已归档（仅一级） */
  archived?: boolean
  children?: AcceptanceNodeVO[]
}

/** 验收查询参数 */
export interface AcceptanceQuery extends PageQuery {
  projectName?: string
  projectCategory?: string
  /** 项目类型：后端字段为 projectSource，字典为 xiang_mu_lei_xing */
  projectSource?: string
}

/** 明细验收入参 */
export interface AcceptDetailDTO {
  approvalId: string
  detailId: string
  acceptBy: string
  acceptTime?: string
  remark?: string
  attachments?: AttachmentItem[]
}

export interface AttachmentItem {
  attachmentType?: string
  fileName?: string
  fileKey: string
  bucket?: string
  fileSize?: number
  remark?: string
}

/** 项目归档入参 */
export interface ArchiveDTO {
  approvalId: string
  archiveBy: string
  archiveName?: string
  archiveType?: string
  remark?: string
}

/** 验收分页查询（树形数据，仅已发起验收的项目） */
export function getAcceptancePage(params: AcceptanceQuery) {
  return get<Result<PageVO<AcceptanceNodeVO>>>('/api/project/acceptance/page', { params })
}

/** 施工管理发起验收（把项目标记为"验收中"，进入验收管理） */
export function applyAcceptance(approvalId: string, applyBy?: string) {
  return post<Result<void>>('/api/project/acceptance/apply', null, {
    params: { approvalId, applyBy: applyBy || undefined }
  })
}

/** 三级明细验收确认 */
export function acceptDetail(data: AcceptDetailDTO) {
  return post<Result<void>>('/api/project/acceptance/accept', data)
}

/** 项目归档 */
export function archive(data: ArchiveDTO) {
  return post<Result<void>>('/api/project/acceptance/archive', data)
}
