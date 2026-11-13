import { get } from './request'
import type { Result } from '@/types/common'

/**
 * 资金记录查询（综合查询模块，只读）
 * 接口：GET /api/project/fund/query/page
 * 一次返回 records（分页）+ summary（顶部汇总卡）+ sidebar（左侧面板统计）
 *
 * 注意：request.get 不拆 Result 外层，返回 Result<T>，页面里要取 .data 拿业务对象
 */

export interface FundRecordRow {
  id: string
  nodeLevel: 1 | 2 | 3 | number
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
  detailType: string | null // HOUSEHOLD / FACILITY
  fundType: string | null
  fundTypeLabel: string | null
  fundSource: string | null
  fundSourceLabel: string | null
  amount: number
  townName: string | null
  villageName: string | null
  createTime: string // yyyy-MM-dd HH:mm:ss（@JsonFormat 自动）
  createBy: string | null
  remark: string | null
  attachCount: number
}

export interface FundQuerySummary {
  recordCount: number
  registerTotal: number
  allocateTotal: number
  projectCount: number
  declareCount: number
  detailCount: number
}

export interface FundQueryStatItem {
  value: string
  label: string
  count: number
  amount: number
}

export interface FundQuerySidebar {
  byFundType: FundQueryStatItem[]
  byFundSource: FundQueryStatItem[]
  byNodeLevel: FundQueryStatItem[]
  byVillage: FundQueryStatItem[]
}

export interface FundQueryPageResult {
  records: FundRecordRow[]
  total: number
  pageNum: number
  pageSize: number
  summary: FundQuerySummary
  sidebar: FundQuerySidebar
}

export interface FundQueryParams {
  pageNum?: number
  pageSize?: number
  projectName?: string
  approvalCode?: string
  declareCode?: string
  detailName?: string
  nodeLevel?: 1 | 2 | 3
  fundType?: string
  fundSource?: string
  regionCode?: string
  startTime?: string
  endTime?: string
  createBy?: string
  minAmount?: number
  maxAmount?: number
}

export function getFundQueryPage(params: FundQueryParams) {
  return get<Result<FundQueryPageResult>>('/api/project/fund/query/page', { params })
}

/** 导出 CSV：浏览器直接下载 */
export function downloadFundQueryExport(params: FundQueryParams) {
  const qs = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') qs.append(k, String(v))
  })
  // request 实例 baseURL = '/'，后端全路径是 /api/project/fund/query/export
  const url = `/api/project/fund/query/export?${qs.toString()}`
  const a = document.createElement('a')
  a.href = url
  a.style.display = 'none'
  a.target = '_blank'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}
