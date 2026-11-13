import { get, put } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'
import type { FundNodeVO } from '@/api/fund'

/** 施工状态：未开工 */
export const CONSTRUCTION_STATUS_NOT_STARTED = 'NOT_STARTED'
/** 施工状态：施工中 */
export const CONSTRUCTION_STATUS_IN_PROGRESS = 'IN_PROGRESS'
/** 施工状态：已完工 */
export const CONSTRUCTION_STATUS_COMPLETED = 'COMPLETED'

/** 施工日志/巡检记录 */
export interface ConstructionLogVO {
  id: string
  /** 明细ID（三级施工记录） */
  detailId?: string
  /** 关联项目名称 */
  projectName?: string
  /** 农户/设施名称 */
  detailName?: string
  /** 日志编号 */
  logCode?: string
  /** 记录日期（施工日期） */
  constructionDate?: string
  /** 巡检人 */
  inspector?: string
  /** 巡检状态：PASS合格 FAIL整改 */
  inspectionStatus?: string
  /** 施工状态：NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工 */
  constructionStatus?: string
  /** 施工内容描述 */
  content?: string
  /** 施工记录附件（三级查看） */
  attachments?: ConstructionAttachmentVO[]
}

/** 施工资料附件（回显） */
export interface ConstructionAttachmentVO {
  id: string
  /** 附件类型（字典 shi_gong_fu_jian_lei_xing） */
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

/** 施工树形节点：一级项目 / 二级申报 / 三级农户或设施明细 */
export interface ConstructionNodeVO {
  /** 施工记录ID（可能为空） */
  id?: string
  /** 行标识：p_立项ID / d_申报ID / t_明细ID */
  nodeKey: string
  /** project项目 declare申报 detail明细 */
  nodeType: 'project' | 'declare' | 'detail'
  approvalId: string
  declareId?: string
  detailId?: string
  /** 项目编号/申报编号/户号或设施编码 */
  code?: string
  /** 项目名称/申报标题/农户或设施名称 */
  name?: string
  /** 项目类别字典值，或明细类型 */
  category?: string
  /** 项目来源字典值，或所属区划 */
  source?: string
  /** HOUSEHOLD农户 FACILITY公共基础设施 */
  detailType?: string
  /** NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工 */
  status: string
  /** 验收状态：一级 NONE待验收 APPLYING验收中 ARCHIVED已归档；二级 PENDING待验收 PARTIAL部分验收 ACCEPTED已验收；三级 PENDING待验收 ACCEPTED已验收 */
  acceptStatus?: string
  /** 全部户数/设施数 */
  totalCount: number
  /** 已开工数 */
  startedCount: number
  /** 已完工数 */
  completedCount: number
  /** 状态分子：施工中为已开工数，已完工为已完工数 */
  numerator: number
  /** 进度百分比 */
  percent: number
  /** 补助/投入金额（元）：三级为明细金额，一级/二级为明细合计 */
  amount?: number
  /** 是否可发起验收（全部明细已完工） */
  canAccept: boolean
  /** 三级节点施工状态原始值 */
  buildStatus?: string
  /** 最新一条施工记录的施工内容（二级查看明细使用） */
  latestContent?: string
  /** 施工单位（一级节点，即施工队名称） */
  builder?: string
  /** 承包人姓名 */
  contractorName?: string
  /** 承包人联系电话 */
  contractorPhone?: string
  /** 负责人姓名 */
  leaderName?: string
  /** 负责人联系电话 */
  leaderPhone?: string
  /** 施工记录数 */
  logCount?: number
  /** 巡检总数 */
  inspectionCount?: number
  /** 巡检合格数 */
  inspectionPassCount?: number
  /** 巡检合格率百分比 */
  inspectionPassRate?: number
  /** 施工日志/巡检记录（一级查看） */
  logs?: ConstructionLogVO[]
  /** 施工资料附件（一级查看/编辑） */
  attachments?: ConstructionAttachmentVO[]
  /** 开工日期 */
  startDate?: string
  /** 完工日期 */
  completeDate?: string
  /** 备注 */
  remark?: string
  children?: ConstructionNodeVO[]
}

/** 施工分页查询入参 */
export interface ConstructionQuery extends PageQuery {
  /** 项目编码 */
  projectCode?: string
  /** 项目名称 */
  projectName?: string
  /** 项目类别（字典 xiang_mu_lei_bie） */
  projectCategory?: string
  /** 项目类型（字典 xiang_mu_lei_xing） */
  projectType?: string
}

/** 施工资料附件子项（已有附件回传 id，新增不传） */
export interface ConstructionAttachmentItem {
  id?: string
  attachmentType?: string
  fileName?: string
  fileKey?: string
  bucket?: string
  fileSize?: number
  remark?: string
}

/** 施工信息编辑入参 */
export interface ConstructionEditDTO {
  approvalId: string
  declareId?: string
  detailId?: string
  /** 节点层级：1项目 3明细 */
  nodeLevel: 1 | 3
  /** 施工状态（三级节点必填） */
  buildStatus?: string
  /** 施工队名称（一级节点使用） */
  builder?: string
  /** 承包人姓名（一级节点使用） */
  contractorName?: string
  /** 承包人联系电话（一级节点使用） */
  contractorPhone?: string
  /** 负责人姓名（一级节点使用） */
  leaderName?: string
  /** 负责人联系电话（一级节点使用） */
  leaderPhone?: string
  /** 施工资料附件（一级节点使用，未传则不修改） */
  attachments?: ConstructionAttachmentItem[]
  startDate?: string
  completeDate?: string
  remark?: string
}

/** 三级施工记录编辑入参（关联项目只读，由后端按明细反查） */
export interface ConstructionLogEditDTO {
  approvalId: string
  detailId: string
  /** 记录日期 */
  constructionDate: string
  /** 巡检人 */
  inspector?: string
  /** 巡检状态：PASS合格 FAIL整改 */
  inspectionStatus: string
  /** 施工状态 */
  constructionStatus: string
  /** 施工内容描述 */
  content?: string
  /** 施工记录附件（未传则不修改） */
  attachments?: ConstructionAttachmentItem[]
}

/** 施工分页查询 */
export function getConstructionPage(params: ConstructionQuery) {
  return get<Result<PageVO<ConstructionNodeVO>>>('/api/project/construction/page', { params })
}

/** 项目施工详情（施工队信息、施工日志、巡检合格率、附件及申报明细进度） */
export function getConstructionDetail(approvalId: string) {
  return get<Result<ConstructionNodeVO>>(`/api/project/construction/${approvalId}`)
}

/** 编辑施工信息（一级：施工单位；三级：施工状态/工期） */
export function updateConstruction(data: ConstructionEditDTO) {
  return put<Result<void>>('/api/project/construction', data)
}

/** 三级施工记录详情（关联项目、记录日期、巡检人、巡检状态、施工状态、施工内容及附件） */
export function getDetailLog(detailId: string) {
  return get<Result<ConstructionLogVO>>(`/api/project/construction/detail/${detailId}/log`)
}

/** 三级施工记录历史（每次编辑留痕，倒序） */
export function getDetailLogHistory(detailId: string) {
  return get<Result<ConstructionLogVO[]>>(`/api/project/construction/detail/${detailId}/log/history`)
}

/** 一级项目资金查看（含立项资料附件与资金流水） */
export function getConstructionProjectFund(approvalId: string) {
  return get<Result<FundNodeVO>>(`/api/project/construction/approval/${approvalId}/fund`)
}

/** 二级事项申报资金查看（含申报材料附件与资金流水） */
export function getConstructionDeclareFund(declareId: string) {
  return get<Result<FundNodeVO>>(`/api/project/construction/declare/${declareId}/fund`)
}

/** 三级明细资金查看（含申报材料附件与资金流水） */
export function getConstructionFundDetail(detailId: string) {
  return get<Result<FundNodeVO>>(`/api/project/construction/detail/${detailId}/fund`)
}

/** 保存三级施工记录（含附件，并同步明细施工状态） */
export function saveDetailLog(data: ConstructionLogEditDTO) {
  return put<Result<void>>('/api/project/construction/detail/log', data)
}
