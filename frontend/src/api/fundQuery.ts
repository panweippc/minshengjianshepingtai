import { get } from './request'
import type { Result } from '@/types/common'

// ================== 类型（与后端 FundStatVO 对齐） ==================

export interface GlobalSummary {
  recordCount: number
  projectCount: number
  declareCount: number
}

/** 主统计表展平行：大分组 → 村组明细 → 总计行（groupSummary / grandTotal 标记） */
export interface Row {
  groupValue: string
  groupLabel: string
  villageName: string
  recordCount: number
  projectCount: number
  villageCoverage: number
  amount: number | string
  remark: string | null
  groupSummary: boolean
  grandTotal: boolean
}

export interface FundStatVO {
  rows: Row[]
  total: number
  globalSummary: GlobalSummary
}

export interface FundDetailRow {
  id: string
  nodeLevel: number | null
  nodeLevelLabel: string
  approvalId: string
  approvalCode: string
  projectName: string
  projectCategory: string
  projectCategoryLabel: string
  projectType: string
  projectTypeLabel: string
  declareId: string | null
  declareCode: string | null
  declareName: string | null
  detailId: string | null
  detailName: string | null
  detailType: string | null
  fundType: string | null
  fundTypeLabel: string
  fundSource: string | null
  fundSourceLabel: string
  amount: number | string | null
  townName: string | null
  villageName: string | null
  regionCode: string | null
  createTime: string | null
  createBy: string | null
  remark: string | null
  attachCount: number
}

// ================== API ==================

/** 主统计汇总（嵌套 rows + 全局 summary） */
export function getFundStat(params: {
  year?: string; regionCode?: string;
  groupType?: string; groupValue?: string
}) {
  return get<Result<FundStatVO>>('/api/project/fund/query/stat', { params })
}

/** 分组内明细流水（弹窗里一行一条的扁平列表） */
export function getFundDetailList(params: {
  year?: string; regionCode?: string;
  groupType?: string; groupValue?: string;
  villageName?: string
}) {
  return get<Result<FundDetailRow[]>>('/api/project/fund/query/detailList', { params })
}

/** 导出当前分组明细到 CSV（后端直接写流） */
export function downloadFundDetailCsv(params: {
  year?: string; regionCode?: string;
  groupType?: string; groupValue?: string;
  villageName?: string
}) {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') q.append(k, String(v)) })
  const url = '/api/project/fund/query/export?' + q.toString()
  window.open(url, '_blank')
}
