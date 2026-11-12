import { get, post } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'

/** 资金节点类型：approval项目 declare申报 detail明细 */
export type FundNodeType = 'approval' | 'declare' | 'detail'

/** 资金凭证附件 */
export interface FundAttachmentVO {
  id?: string
  attachmentType?: string
  fileName?: string
  fileKey: string
  bucket?: string
  fileSize?: number
  remark?: string
  createTime?: string
}

/** 申报材料附件（立项管理申报附件） */
export interface DeclareAttachmentVO {
  id: string
  attachmentType?: string
  fileName?: string
  fileKey: string
  bucket?: string
  fileSize?: number
  remark?: string
  createTime?: string
}

/** 资金树形节点（一级项目 → 二级申报 → 三级明细） */
export interface FundNodeVO {
  id: string
  nodeType: FundNodeType
  /** HOUSEHOLD农户 / FACILITY公共基础设施（仅三级） */
  detailType?: string
  /** 项目编号 / 申报编号 / 户号或设施编码 */
  code?: string
  /** 项目名称 / 申报名称 / 农户或设施名称 */
  name?: string
  /** 一级：字典 xiang_mu_lei_bie；二级：字典 shen_bao_lei_bie；三级：明细类型 */
  category?: string
  /** 一级：字典 xiang_mu_lei_xing；二级：字典 shen_bao_lei_xing；三级：所属区划 */
  type?: string
  /** 项目预算 / 申报金额 / 补助金额 */
  budget?: number
  /** 资金来源（字典 zi_jin_lai_yuan） */
  fundSource?: string
  /** 拨付金额（本级累计） */
  allocatedAmount?: number
  /** 结余（仅一级：上级拨付金额 - 二级下拨金额） */
  balance?: number
  /** 负责人（仅一级） */
  leader?: string
  /** 提交时间（仅一级） */
  submitTime?: string
  /** 审核状态（仅一级） */
  status?: string
  /** 备注（仅一级） */
  remark?: string
  /** 所属乡镇（仅二级） */
  townName?: string
  /** 所属村组（仅二级） */
  villageName?: string
  /** 申报内容（仅二级） */
  declareContent?: string
  /** 附件：一级立项资料附件 / 二级申报材料附件 */
  attachments?: DeclareAttachmentVO[]
  /** 事项说明（仅三级） */
  detailRemark?: string
  /** 拨付说明（仅三级：最近一次下拨说明） */
  fundRemark?: string
  /** 资金凭证附件（二级下拨、三级下拨） */
  fundAttachments?: FundAttachmentVO[]
  /** 申报材料附件（仅三级：关联农户/公共基础设施的附件） */
  materialAttachments?: DeclareAttachmentVO[]
  /** 资金流水（一级/二级/三级查看） */
  records?: FundRecordVO[]
  children?: FundNodeVO[]
}

/** 资金拨付记录 */
export interface FundRecordVO {
  id: string
  approvalId: string
  declareId?: string
  detailId?: string
  /** 1一级资金登记 2二级下拨 3三级下拨 */
  nodeLevel: number
  /** 资金类型（字典 zi_jin_lei_xing） */
  fundType?: string
  fundSource?: string
  amount?: number
  remark?: string
  createBy?: string
  createTime?: string
  attachments?: FundAttachmentVO[]
}

/** 资金查询参数 */
export interface FundQuery extends PageQuery {
  projectName?: string
  projectCategory?: string
  projectSource?: string
}

/** 资金登记/下拨入参 */
export interface FundSaveDTO {
  nodeType: FundNodeType
  approvalId: string
  declareId?: string
  detailId?: string
  /** 资金类型/下拨类型（一级字典 zi_jin_lei_xing，二级/三级字典 xia_bo_lei_xing，均必填） */
  fundType?: string
  /** 资金来源（字典 zi_jin_lai_yuan，一级必填） */
  fundSource?: string
  amount: number
  /** 操作人（人工录入） */
  operator?: string
  /** 时间（人工选择，yyyy-MM-dd HH:mm:ss） */
  operateTime?: string
  remark?: string
  /** 资金凭证附件 */
  attachments?: FundAttachmentVO[]
}

/** 资金分页查询（树形数据） */
export function getFundPage(params: FundQuery) {
  return get<Result<PageVO<FundNodeVO>>>('/api/project/fund/page', { params })
}

/** 查询节点资金拨付记录 */
export function getFundRecords(nodeType: FundNodeType, nodeId: string) {
  return get<Result<FundRecordVO[]>>('/api/project/fund/records', { params: { nodeType, nodeId } })
}

/** 资金登记（一级项目节点） */
export function registerFund(data: FundSaveDTO) {
  return post<Result<void>>('/api/project/fund/register', data)
}

/** 下拨资金（二级申报、三级明细节点） */
export function allocateFund(data: FundSaveDTO) {
  return post<Result<void>>('/api/project/fund/allocate', data)
}
