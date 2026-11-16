import { get } from './request'
import type { Result } from '@/types/common'

// ================== 类型（与后端 ProjectCycleStatVO 对齐） ==================

/** 主统计表行 */
export interface CycleRow {
  groupValue: string
  groupLabel: string
  villageName: string

  // 通用指标（category/source/status/village 方案使用）
  projectCount: number
  villageCoverage: number
  budget: number | string
  totalFund: number | string

  // 立项状态方案专用指标（establish 方案使用）
  householdCount: number
  facilityCount: number

  groupSummary: boolean
  grandTotal: boolean
}

/** 全局汇总 */
export interface CycleGlobalSummary {
  projectCount: number
  declareCount: number
  villageCoverage: number
  totalBudget: number | string
  totalFund: number | string
}

export interface ProjectCycleStatVO {
  rows: CycleRow[]
  total: number
  globalSummary: CycleGlobalSummary
}

/** 查询方案类型 */
export type QueryType = 'category' | 'source' | 'establish' | 'status' | 'village'

// ================== API ==================

/** 主统计汇总 */
export function getProjectCycleStat(params: {
  year?: string; regionCode?: string; queryType?: QueryType
}) {
  return get<Result<ProjectCycleStatVO>>('/api/project/cycle/query/stat', { params })
}

/** 导出当前筛选 + 当前方案下的全部统计到 CSV */
export function downloadProjectCycleCsv(params: {
  year?: string; regionCode?: string; queryType?: QueryType
}) {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') q.append(k, String(v)) })
  const url = '/api/project/cycle/query/export?' + q.toString()
  window.open(url, '_blank')
}
