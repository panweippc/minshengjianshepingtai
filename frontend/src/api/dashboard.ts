import { get } from './request'
import type { Result } from '@/types/common'

/** 项目进度概览项（一个项目一条记录，可同时处于多个阶段） */
export interface ProjectProgressVO {
  /** 立项主键 */
  id: number
  /** 立项编号 */
  approvalCode: string
  /** 项目名称 */
  projectName: string
  /** 当前阶段序号集合（1待审核 2资金 3施工中 4待验收 5已归档），可同时包含多个 */
  currentStages: number[]
}

/** 首页概览统计 */
export interface DashboardOverviewVO {
  /** 行政村总数 */
  villageCount: number
  /** 农户总数 */
  householdCount: number
  /** 公共基础设施总数 */
  facilityCount: number
  /** 在库物资总数 */
  materialStockTotal: number
  /** 项目进度概览（一个项目一条记录） */
  projectProgress: ProjectProgressVO[]
}

/** 查询首页概览统计 */
export function getDashboardOverview() {
  return get<Result<DashboardOverviewVO>>('/api/dashboard/overview')
}
