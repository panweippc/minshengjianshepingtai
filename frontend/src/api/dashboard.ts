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

/** 数据大屏核心指标 */
export interface ScreenKpiVO {
  /** 项目总预算（元） */
  totalBudget: number
  /** 已拨付资金（元） */
  paidAmount: number
  /** 拨付完成率（百分比） */
  payRate: number
  /** 立项项目总数 */
  projectCount: number
  /** 在管项目数 */
  buildingCount: number
  /** 在建项目数（已进入施工管理） */
  constructionCount: number
  /** 行政村总数 */
  villageCount: number
  /** 农户总数 */
  householdCount: number
  /** 公共基础设施总数 */
  facilityCount: number
  /** 在库物资总数 */
  materialTotal: number
  /** 物资预警数量 */
  materialWarnCount: number
  /** 申报受理总数 */
  declarationCount: number
}

/** 资金拨付趋势月度数据 */
export interface ScreenFundTrendVO {
  /** 月份（yyyy-MM） */
  month: string
  /** 当月立项预算（元） */
  budget: number
  /** 当月拨付金额（元） */
  paid: number
}

/** 通用图表项 */
export interface ScreenChartItemVO {
  name: string
  value: number
}

/** 村域态势数据 */
export interface ScreenVillageVO {
  name: string
  projectCount: number
  householdCount: number
}

/** 预算投入排名项 */
export interface ScreenBudgetRankVO {
  projectName: string
  budget: number
  /** 占首位预算比例（百分比） */
  ratio: number
}

/** 物资状态项 */
export interface ScreenMaterialVO {
  itemName: string
  stockQty: number
  warningQty: number
  unit: string
  warning: boolean
}

/** 实时事件 */
export interface ScreenEventVO {
  /** 日志主键（雪花ID序列化为字符串，避免精度丢失，用于前端去重） */
  id: string
  /** 操作人账号 */
  username: string
  /** 所属模块 */
  module: string
  /** 操作描述 */
  operation: string
  /** 事件摘要：村/对象/动作/数量，富文本粒度 */
  summary: string
  /** 事件类别（彩色圆点映射）：declaration/approval/construction/acceptance/fund/material/archive/base/system/file */
  category: string
  /** 操作时间 yyyy-MM-dd HH:mm:ss */
  operTime: string
}

/** 项目类型柱状图系列 */
export interface ScreenTypeSeriesVO {
  /** 项目类型名称 */
  name: string
  /** 各村项目数（与 villages 顺序一致） */
  data: number[]
}

/** 各村项目类型统计 */
export interface ScreenTypeStatsVO {
  /** 横轴村庄 */
  villages: string[]
  /** 项目类型系列 */
  series: ScreenTypeSeriesVO[]
}

/** 各村项目类型明细统计系列（项目类型 × 农户/公建） */
export interface ScreenTypeDetailSeriesVO {
  /** 系列名称，如 常规项目·农户 */
  name: string
  /** 各村数值（与 villages 顺序一致） */
  data: number[]
  /** 明细类别：household=农户明细(单位户)，facility=公建设施明细(单位处) */
  category: 'household' | 'facility'
}

/** 各村项目类型明细统计 */
export interface ScreenTypeDetailStatsVO {
  /** 横轴村庄 */
  villages: string[]
  /** 系列（每个项目类型拆分为农户/公建两条） */
  series: ScreenTypeDetailSeriesVO[]
}

/** 数据大屏聚合数据 */
export interface ScreenOverviewVO {
  kpi: ScreenKpiVO
  fundTrend: ScreenFundTrendVO[]
  villageStats: ScreenVillageVO[]
  typeStats: ScreenTypeStatsVO
  /** 各村项目类型明细统计（横轴村庄，系列为项目类型，数值为申报明细数；仅立项已通过） */
  villageTypeDetailStats: ScreenTypeDetailStatsVO
  statusStats: ScreenChartItemVO[]
  budgetRank: ScreenBudgetRankVO[]
  materialStats: ScreenMaterialVO[]
  events: ScreenEventVO[]
  updateTime: string
}

/** 查询数据大屏聚合统计 */
export function getScreenOverview() {
  return get<Result<ScreenOverviewVO>>('/api/dashboard/screen')
}
