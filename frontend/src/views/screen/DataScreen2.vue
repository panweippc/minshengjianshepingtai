<template>
  <div
    ref="rootRef"
    class="screen-root"
    :class="{ 'is-standalone': standalone }"
    v-loading="loading"
    element-loading-background="rgba(3, 10, 24, 0.85)"
  >
    <div class="screen-canvas" :style="canvasStyle">
      <!-- ========== 顶部标题区 ========== -->
      <header class="screen-header">
        <div class="header-side header-left">
          <span class="chip"><i class="chip-dot"></i>系统在线</span>
          <span class="chip"><el-icon><OfficeBuilding /></el-icon>{{ kpi.villageCount }} 个行政村</span>
          <span class="chip"><el-icon><CircleCheck /></el-icon>数据正常</span>
        </div>
        <div class="header-mid">
          <div class="title-bar"></div>
          <h1 class="header-title">民生建设项目管理平台</h1>
          <div class="title-bar"></div>
        </div>
        <div class="header-side header-right">
          <div class="clock">
            <span class="clock-date">{{ dateText }}</span>
            <span class="clock-time">{{ timeText }}</span>
          </div>
          <button v-if="!standalone" class="cast-btn" @click="openCast">
            <el-icon><Monitor /></el-icon>投屏
          </button>
        </div>
      </header>

      <!-- ========== 主体三栏 ========== -->
      <main class="screen-body">
        <!-- ===== 左列 ===== -->
        <section class="col col-left">
          <!-- 1. 各村项目分布 -->
          <ScreenPanel title="各村项目分布">
            <div class="village-map">
              <!-- 淡路网线条 -->
              <svg class="map-grid" viewBox="0 0 100 100" preserveAspectRatio="none">
                <path d="M0,50 C20,45 30,55 50,50 C70,45 80,55 100,50" />
                <path d="M50,0 C45,25 55,35 50,50 C45,65 55,75 50,100" />
                <path d="M15,20 C35,30 45,20 55,30 C65,40 75,25 90,32" />
                <path d="M20,80 C35,70 55,78 70,70 C80,66 88,74 96,68" />
              </svg>
              <!-- 辐散线：中心→各点 -->
              <svg class="map-links" viewBox="0 0 100 100" preserveAspectRatio="none">
                <line
                  v-for="node in mapNodes"
                  :key="`lnk-${node.name}`"
                  :x1="48" :y1="50"
                  :x2="node.x" :y2="node.y"
                />
              </svg>
              <!-- 中心节点 -->
              <div class="map-center"></div>
              <!-- 各村庄节点 -->
              <div
                v-for="node in mapNodes"
                :key="node.name"
                class="map-node"
                :style="{ left: `${node.x}%`, top: `${node.y}%`, color: node.color }"
              >
                <span class="node-halo"></span>
                <span class="node-core"></span>
                <span class="node-label">
                  <b>{{ node.name }}</b>
                  <em>{{ node.projectCount }} 个项目</em>
                </span>
              </div>
              <p v-if="!mapNodes.length" class="empty-tip">暂无村庄数据</p>
            </div>
          </ScreenPanel>

          <!-- 2. 资金完成率 -->
          <ScreenPanel title="资金完成率">
            <div class="fund-wrap">
              <div class="fund-ring">
                <svg viewBox="0 0 160 160" class="fund-svg">
                  <defs>
                    <linearGradient id="fundGrad" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%" stop-color="#00f6ff" />
                      <stop offset="55%" stop-color="#00c8ff" />
                      <stop offset="100%" stop-color="#1b8fff" />
                    </linearGradient>
                  </defs>
                  <circle class="ring-base" cx="80" cy="80" r="60" />
                  <circle class="ring-progress" cx="80" cy="80" r="60"
                    :stroke-dasharray="ringStrokeArray" />
                  <circle class="ring-inner" cx="80" cy="80" r="42" />
                </svg>
                <div class="fund-center">
                  <b>{{ kpi.payRate.toFixed(1) }}<em>%</em></b>
                  <span>拨付完成率</span>
                </div>
              </div>
              <div class="fund-stats">
                <div class="fund-stat">
                  <span class="fs-label">批复金额</span>
                  <span class="fs-value"><b>{{ formatNumber(kpi.totalBudget / 10000) }}</b><small>万元</small></span>
                </div>
                <div class="fund-stat">
                  <span class="fs-label">拨付金额</span>
                  <span class="fs-value"><b>{{ formatNumber(kpi.paidAmount / 10000) }}</b><small>万元</small></span>
                </div>
              </div>
            </div>
          </ScreenPanel>

          <!-- 3. 物资状态 -->
          <ScreenPanel title="物资状态">
            <div v-if="materialStats.length" class="scroll-viewport">
              <div class="scroll-track" :class="{ 'scrolling': materialScrollable }">
                <ul v-for="copy in materialScrollable ? 2 : 1" :key="copy" class="scroll-list">
                  <li v-for="(item, i) in materialStats" :key="item.itemName"
                    class="mat-item" :class="{ 'mat-alt': i % 2 === 1 }">
                    <span class="mat-name">{{ item.itemName }}</span>
                    <span class="mat-stock">
                      <b :class="{ 'warn': item.warning }">{{ item.stockQty }}</b>
                      <small>{{ item.unit || '件' }}</small>
                    </span>
                    <span class="mat-warn">预警 {{ item.warningQty }}</span>
                    <i class="mat-dot" :class="{ 'dot-warn': item.warning }"></i>
                  </li>
                </ul>
              </div>
            </div>
            <p v-else class="empty-tip">暂无物资数据</p>
          </ScreenPanel>
        </section>

        <!-- ===== 中间列 ===== -->
        <section class="col col-center">
          <!-- 顶部 4 指标卡片 -->
          <div class="kpi-row">
            <div
              v-for="(card, i) in kpiCards"
              :key="card.label"
              class="kpi-card"
              :class="`kpi-${kpiAccent[i]}`"
            >
              <div class="kpi-ic-wrap">
                <component :is="card.icon" />
              </div>
              <div class="kpi-body">
                <span class="kpi-value">{{ card.value }}<em>{{ card.unit }}</em></span>
                <span class="kpi-label">{{ card.label }}</span>
              </div>
            </div>
          </div>

          <!-- 各村项目类型明细统计 -->
          <ScreenPanel title="各村项目类型明细统计">
            <div class="chart-stage">
              <!-- 图例 -->
              <div class="legend-box" v-if="detailHasData">
                <div class="legend-row">
                  <span v-for="(l, i) in LEGEND_TYPE_LINES" :key="`tl-${i}`" class="lg-item">
                    <i class="lg-line" :style="{ borderTopStyle: l.style, borderTopColor: l.color }"></i>
                    <em>{{ l.label }}</em>
                  </span>
                </div>
                <div class="legend-row">
                  <span v-for="(c, i) in LEGEND_CATEGORY_DOTS" :key="`cd-${i}`" class="lg-item">
                    <i class="lg-dot" :style="{ background: c.color }"></i>
                    <em>{{ c.label }}（{{ c.unit }}）</em>
                  </span>
                </div>
              </div>
              <div ref="detailRef" class="chart"></div>
              <p v-if="!detailHasData" class="empty-tip detail-empty">暂无明细数据</p>
            </div>
          </ScreenPanel>

          <!-- 各村项目类型分布 -->
          <ScreenPanel title="各村项目类型分布">
            <div class="chart-stage">
              <div ref="barRef" class="chart"></div>
              <div class="bar-legend">
                <span v-for="(l, i) in BAR_STACK_LEGEND" :key="`bsl-${i}`" class="bl-item">
                  <i :style="{ background: l.color }"></i>
                  <em>{{ l.label }}</em>
                </span>
              </div>
            </div>
          </ScreenPanel>
        </section>

        <!-- ===== 右列 ===== -->
        <section class="col col-right">
          <!-- 核心指标 -->
          <ScreenPanel title="核心指标">
            <div class="metric-grid">
              <div v-for="(item, i) in metricItems" :key="item.label"
                class="metric-card" :class="`metric-${metricAccent[i]}`">
                <span class="metric-deco"></span>
                <span class="metric-deco metric-deco--rev"></span>
                <div class="metric-num-wrap">
                  <b class="metric-num">{{ formatNumber(metricDisplay[i] ?? 0) }}</b>
                  <em class="metric-unit">{{ item.unit }}</em>
                </div>
                <span class="metric-label">{{ item.label }}</span>
              </div>
            </div>
          </ScreenPanel>

          <!-- 项目状态分布 -->
          <ScreenPanel title="项目状态分布">
            <div class="donut-wrap">
              <svg class="donut-svg" viewBox="0 0 440 280" preserveAspectRatio="none">
                <defs>
                  <radialGradient id="dnHalo" cx="50%" cy="50%" r="50%">
                    <stop offset="0%" stop-color="#00c8ff" stop-opacity="0.12" />
                    <stop offset="100%" stop-color="#00c8ff" stop-opacity="0" />
                  </radialGradient>
                </defs>
                <ellipse cx="220" cy="151" rx="158" ry="88" fill="url(#dnHalo)" />
                <ellipse class="orbit-a" cx="220" cy="151" rx="146" ry="82" />
                <ellipse class="orbit-b" cx="220" cy="151" rx="122" ry="68" />
                <ellipse class="orbit-flow" cx="220" cy="151" rx="122" ry="68" />

                <!-- 平台底座 -->
                <g class="platform">
                  <path class="platform-base" d="M160 140 C160 158.225 186.863 173 220 173 C253.137 173 280 158.225 280 140 L280 162 C280 180.225 253.137 195 220 195 C186.863 195 160 180.225 160 162 Z" />
                  <ellipse class="platform-top" cx="220" cy="140" rx="60" ry="33" />
                </g>

                <!-- 各状态节点 -->
                <g v-for="n in donutNodes" :key="`dot-${n.name}`">
                  <circle class="dot-halo" :cx="n.x" :cy="n.y" r="10"
                    :style="{ fill: n.color, opacity: n.value ? 0.28 : 0.08 }" />
                  <circle class="dot-core" :cx="n.x" :cy="n.y" r="3.6"
                    :style="{ fill: n.color }" />
                </g>
              </svg>

              <div class="donut-center">
                <b>{{ donutTotal }}</b>
                <span>项目总数</span>
              </div>

              <!-- 各状态标签 -->
              <div
                v-for="n in donutNodes"
                :key="`lbl-${n.name}`"
                class="donut-label"
                :class="{ 'is-left': n.left, 'is-zero': !n.value }"
                :style="{ left: `${n.labelX}%`, top: `${n.labelY}%`, '--lc': n.color }"
              >
                <span class="dlbl-name">{{ n.name }}</span>
                <b class="dlbl-value">{{ n.value }}</b>
              </div>
            </div>
          </ScreenPanel>

          <!-- 实时事件 -->
          <ScreenPanel title="实时事件">
            <div v-if="eventStream.length" class="scroll-viewport">
              <div class="scroll-track" :class="{ scrolling: eventScrollable }">
                <ul v-for="copy in eventScrollable ? 2 : 1" :key="copy" class="scroll-list">
                  <li v-for="item in eventStream" :key="item.id" class="ev-item">
                    <i class="ev-dot" :style="{ background: eventDotColor(item.category) }"></i>
                    <span class="ev-text">{{ item.summary || item.operation }}</span>
                    <span class="ev-time">{{ formatRelativeTime(item.operTime) }}</span>
                  </li>
                </ul>
              </div>
            </div>
            <p v-else class="empty-tip">暂无业务事件</p>
          </ScreenPanel>
        </section>
      </main>

      <!-- ========== 底部数据流 ========== -->
      <footer class="screen-ticker">
        <span class="ticker-label">综合数据流</span>
        <div class="ticker-viewport">
          <div class="ticker-track">
            <template v-for="k in 2" :key="k">
              <span v-for="(item, i) in tickerItems" :key="`${k}-${i}`" class="ticker-item">
                <em></em>{{ item.label }}<b>{{ item.value }}</b>
              </span>
            </template>
          </div>
        </div>
        <span class="ticker-update">更新 {{ overview?.updateTime?.slice(11) || '--:--:--' }}</span>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { OfficeBuilding, CircleCheck, Monitor, Money, Box, PieChart, HomeFilled } from '@element-plus/icons-vue'
import ScreenPanel from '@/components/ScreenPanel.vue'
import { getScreenOverview } from '@/api/dashboard'
import type { ScreenOverviewVO } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const standalone = computed(() => route.meta.standalone === true)
const DESIGN_WIDTH = 1920, DESIGN_HEIGHT = 1080
const REFRESH_INTERVAL = 60_000

/* ===== 语义配色表（严格按清单） ===== */
// 地图节点：按村名分色
const VILLAGE_COLOR_MAP: Record<string, string> = {
  '城关村': '#00f0b0',   // 绿
  '田野村': '#00d8ff',   // 浅蓝
  '绿水村': '#ff9e3c',   // 橙
  '红星村': '#a37bff',   // 紫
  '幸福村': '#ff6b81',   // 红
  '光明村': '#bfe9ff',   // 浅白
  '胜利村': '#ffd080',   // 浅橙
  '建设村': '#c7a6ff'    // 浅紫
}
const VILLAGE_DEFAULT = '#6cb4d8'
const MAP_POSITIONS = [
  { x: 24, y: 32 }, { x: 62, y: 24 }, { x: 78, y: 48 }, { x: 60, y: 70 },
  { x: 30, y: 66 }, { x: 44, y: 46 }, { x: 16, y: 52 }, { x: 74, y: 76 }
]

// KPI 四卡：按标题语义着色（清单要求）
const kpiAccent = ['cyan', 'green', 'cyan', 'purple'] as const

// 折线图三条线：线型 + 颜色（清单要求）
const LEGEND_TYPE_LINES = [
  { label: '常规项目',  color: '#00d8ff', style: 'solid'  as const },
  { label: '应急抢险',  color: '#ffc857', style: 'dashed' as const },
  { label: '灾后重建',  color: '#a37bff', style: 'dotted' as const }
]
// 数据点类别
const LEGEND_CATEGORY_DOTS = [
  { label: '农户',  color: '#00f0b0', unit: '户', category: 'household' as const },
  { label: '公建',  color: '#00d8ff', unit: '处', category: 'facility'  as const }
]

// 圆柱分层颜色：从下到上（底青/中黄/顶紫）
const BAR_STACK_LEGEND = [
  { label: '常规项目',  color: '#00d8ff' },  // 底层
  { label: '应急抢险',  color: '#ffc857' },  // 中层
  { label: '灾后重建',  color: '#a37bff' }   // 顶层
]

// 项目状态圆盘节点颜色（清单要求）
const STATUS_COLORS: Record<string, string> = {
  '待开工':   '#1b8fff',   // 蓝
  '待审核':   '#00f0b0',   // 绿
  '施工中':   '#2ef0b0',   // 浅绿
  '待验收':   '#ffc857',   // 黄
  '已归档':   '#a37bff',   // 紫
  '已发包':   '#ff6b81'    // 红
}
const STATUS_ANGLES: Record<string, number> = {
  '待审核': 60, '待开工': 120, '施工中': 0,
  '待验收': 180, '已归档': 240, '已发包': 300
}
const STATUS_ORDER = ['待开工', '待审核', '施工中', '待验收', '已归档', '已发包']

// 事件圆点颜色（清单要求）
const EVENT_DOT_COLORS: Record<string, string> = {
  declaration: '#00d8ff',
  approval: '#ffc857',
  construction: '#00f0b0',
  acceptance: '#a37bff',
  fund: '#ff6b81',
  material: '#ffc857',
  archive: '#a37bff',
  base: '#00d8ff',
  file: '#00d8ff',
  system: '#64748b'
}

// 核心指标 accent（4个色）
const METRIC_ACCENT_COLORS = ['cyan', 'cyan', 'yellow', 'pink'] as const
const metricAccent = [...METRIC_ACCENT_COLORS]

const loading = ref(false)
const overview = ref<ScreenOverviewVO | null>(null)
const scale = ref(1)
const designHeight = ref(DESIGN_HEIGHT)
const dateText = ref('')
const timeText = ref('')
const rootRef = ref<HTMLDivElement>()
const detailRef = ref<HTMLDivElement>()
const barRef = ref<HTMLDivElement>()

let detailChart: ReturnType<typeof echarts.init> | null = null
let barChart: ReturnType<typeof echarts.init> | null = null
let clockTimer: number | undefined
let refreshTimer: number | undefined
let resizeObserver: ResizeObserver | null = null
let metricRaf = 0

const kpi = computed(() => overview.value?.kpi ?? {
  totalBudget: 0, paidAmount: 0, payRate: 0, projectCount: 0,
  buildingCount: 0, constructionCount: 0, villageCount: 0, householdCount: 0,
  facilityCount: 0, materialTotal: 0, materialWarnCount: 0, declarationCount: 0
})
const statusStats = computed(() => overview.value?.statusStats ?? [])
const materialStats = computed(() => overview.value?.materialStats ?? [])
const villageTypeDetailStats = computed(() => overview.value?.villageTypeDetailStats)
const detailHasData = computed(() => !!villageTypeDetailStats.value?.villages?.length)

const ringStrokeArray = computed(() => {
  const r = Math.max(0, Math.min(100, kpi.value.payRate))
  const c = 2 * Math.PI * 60
  return `${(c * r) / 100} ${c}`
})

const EVENT_HIDDEN = new Set(['base', 'material', 'system', 'file'])
const EVENT_HIDDEN_OPS = ['编辑', '查看', '查询', '修改']
const EVENT_VISIBLE = 6
const eventStream = ref<any[]>([])
function isHidden(e: any) {
  if (EVENT_HIDDEN.has(e.category)) return true
  return EVENT_HIDDEN_OPS.some((k) => (e.operation ?? '').includes(k))
}
function mergeEvents(incoming: any[]) {
  const map = new Map<string, any>()
  for (const e of eventStream.value) map.set(e.summary || e.id, e)
  for (const e of incoming ?? []) { if (isHidden(e)) continue; map.set(e.summary || e.id, e) }
  const arr = Array.from(map.values()).filter((e) => !isHidden(e))
  arr.sort((a, b) => (a.operTime < b.operTime ? 1 : -1))
  eventStream.value = arr.slice(0, 12)
}
function formatRelativeTime(time: string): string {
  if (!time) return ''
  const t = new Date(time.replace(' ', 'T'))
  if (Number.isNaN(t.getTime())) return time
  const diff = Date.now() - t.getTime()
  if (diff < 0) return '刚刚'
  const sec = Math.floor(diff / 1000)
  if (sec < 60) return '刚刚'
  const min = Math.floor(sec / 60)
  if (min < 60) return `${min}分钟前`
  const hr = Math.floor(min / 60)
  if (hr < 24) return `${hr}小时前`
  const day = Math.floor(hr / 24)
  if (day < 30) return `${day}天前`
  return time.slice(5, 16)
}
function eventScrollable() { return eventStream.value.length > EVENT_VISIBLE }
function eventDotColor(cat: string) { return EVENT_DOT_COLORS[cat] || '#00d8ff' }

const MATERIAL_VISIBLE = 4
const materialScrollable = computed(() => materialStats.value.length > MATERIAL_VISIBLE)

/* 地图节点：按村名取色 */
const mapNodes = computed(() => {
  const villages = overview.value?.villageStats ?? []
  return villages.map((v, i) => {
    const p = MAP_POSITIONS[i % MAP_POSITIONS.length]
    return {
      ...v, x: p.x, y: p.y,
      size: 10 + Math.min(v.projectCount, 5) * 3,
      color: VILLAGE_COLOR_MAP[v.name] ?? VILLAGE_DEFAULT
    }
  })
})

function moneyParts(v: number) {
  const x = v ?? 0
  if (x >= 100_000_000) return { value: (x / 100_000_000).toFixed(2), unit: '亿' }
  if (x >= 10_000) return { value: (x / 10_000).toFixed(1), unit: '万' }
  return { value: x.toFixed(0), unit: '元' }
}
function formatNumber(v: number) { return (v ?? 0).toLocaleString('zh-CN') }

const kpiCards = computed(() => [
  { label: '项目总预算', value: moneyParts(kpi.value.totalBudget).value, unit: moneyParts(kpi.value.totalBudget).unit, icon: Money },
  { label: '已拨付资金', value: moneyParts(kpi.value.paidAmount).value, unit: moneyParts(kpi.value.paidAmount).unit, icon: Box },
  { label: '拨付完成率', value: kpi.value.payRate.toFixed(1), unit: '%', icon: PieChart },
  { label: '在建项目', value: String(kpi.value.constructionCount), unit: '个', icon: HomeFilled }
])

const metricItems = computed(() => [
  { label: '农户档案', value: kpi.value.householdCount, unit: '户' },
  { label: '在管项目', value: kpi.value.buildingCount, unit: '个' },
  { label: '物资预警', value: kpi.value.materialWarnCount, unit: '项' },
  { label: '申报受理', value: kpi.value.declarationCount, unit: '件' }
])
const metricDisplay = ref<number[]>([0, 0, 0, 0])
function animateMetrics() {
  const targets = metricItems.value.map((m) => m.value ?? 0)
  const from = metricDisplay.value.slice()
  const start = performance.now()
  window.cancelAnimationFrame(metricRaf)
  const step = (now: number) => {
    const p = Math.min(1, (now - start) / 900)
    const e = 1 - Math.pow(1 - p, 3)
    metricDisplay.value = targets.map((t, i) => Math.round((from[i] ?? 0) + (t - (from[i] ?? 0)) * e))
    if (p < 1) metricRaf = window.requestAnimationFrame(step)
  }
  metricRaf = window.requestAnimationFrame(step)
}
watch(metricItems, animateMetrics, { immediate: true })

const tickerItems = computed(() => {
  const k = kpi.value
  return [
    { label: '立项项目', value: `${k.projectCount} 个` },
    { label: '项目总预算', value: moneyParts(k.totalBudget).value + moneyParts(k.totalBudget).unit },
    { label: '已拨付资金', value: moneyParts(k.paidAmount).value + moneyParts(k.paidAmount).unit },
    { label: '拨付完成率', value: `${k.payRate.toFixed(1)}%` },
    { label: '行政村', value: `${k.villageCount} 个` },
    { label: '农户档案', value: `${k.householdCount} 户` },
    { label: '公共设施', value: `${k.facilityCount} 处` },
    { label: '物资预警', value: `${k.materialWarnCount} 项` }
  ]
})

const canvasStyle = computed(() => ({
  width: `${DESIGN_WIDTH}px`, height: `${designHeight.value}px`,
  transform: `scale(${scale.value})`
}))

function statusColor(name: string) { return STATUS_COLORS[name] ?? '#00d8ff' }

const DONUT_VIEW_W = 440, DONUT_VIEW_H = 280
const DONUT_CX = 220, DONUT_CY = 151
const DONUT_RX = 122, DONUT_RY = 68
const DONUT_LABEL_GAP = 26
const donutTotal = computed(() => statusStats.value.reduce((s, i) => s + i.value, 0))

/* 状态圆盘：按清单定义的顺序和颜色渲染 */
const donutNodes = computed(() => {
  // 先把后端返回按 STATUS_ORDER 排序，缺的补零
  const svMap = new Map<string, number>()
  statusStats.value.forEach((s) => svMap.set(s.name, s.value))
  const ordered = STATUS_ORDER.map((name) => ({ name, value: svMap.get(name) ?? 0 }))

  return ordered.map((item) => {
    const angle = STATUS_ANGLES[item.name] ?? 0
    const rad = (angle * Math.PI) / 180
    const x = DONUT_CX + Math.cos(rad) * DONUT_RX
    const y = DONUT_CY - Math.sin(rad) * DONUT_RY
    const dx = x - DONUT_CX, dy = y - DONUT_CY
    const len = Math.hypot(dx, dy) || 1
    const labelX = x + (dx / len) * DONUT_LABEL_GAP
    const labelY = y + (dy / len) * DONUT_LABEL_GAP
    return {
      name: item.name, value: item.value, color: statusColor(item.name),
      x, y,
      labelX: (labelX / DONUT_VIEW_W) * 100,
      labelY: (labelY / DONUT_VIEW_H) * 100,
      left: Math.cos(rad) < 0
    }
  })
})

async function resizeCanvas() {
  const w = rootRef.value?.clientWidth || window.innerWidth
  const h = rootRef.value?.clientHeight || window.innerHeight
  scale.value = w > 0 ? w / DESIGN_WIDTH : 1
  designHeight.value = h > 0 ? Math.round(h / scale.value) : DESIGN_HEIGHT
  await nextTick(); resizeCharts()
}
function resizeCharts() { ;[detailChart, barChart].forEach((c) => c?.resize()) }

function updateClock() {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  dateText.value = `${now.getFullYear()}.${pad(now.getMonth() + 1)}.${pad(now.getDate())}`
  timeText.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

function initChart(el: HTMLDivElement) {
  const dpr = Math.min(3, (window.devicePixelRatio || 1) * Math.max(1, scale.value))
  return echarts.init(el, undefined, { devicePixelRatio: dpr })
}

interface BarAPI { value: (i: number) => number; coord: (d: number[]) => number[] }
function hexToRgba(hex: string, alpha: number) {
  const c = hex.replace('#', '')
  const n = parseInt(c.length === 3 ? c.split('').map((ch) => ch + ch).join('') : c, 16)
  return `rgba(${(n >> 16) & 0xff}, ${(n >> 8) & 0xff}, ${n & 0xff}, ${alpha})`
}

/* ========== 折线图 ========== */
function buildDetailOption() {
  const stats = villageTypeDetailStats.value
  const villages = stats?.villages ?? []
  const series = stats?.series ?? []

  // series 顺序：按 LEGEND_TYPE_LINES 类型分组，每组2条（农户+公建）
  return {
    backgroundColor: 'transparent',
    grid: { top: 70, left: 44, right: 20, bottom: 32 },
    tooltip: {
      trigger: 'axis' as const,
      backgroundColor: 'rgba(4, 14, 34, 0.96)',
      borderColor: 'rgba(0, 240, 255, 0.65)',
      borderWidth: 1,
      padding: [10, 14],
      boxShadow: '0 4px 20px rgba(0, 160, 255, 0.35)',
      textStyle: { color: '#e4faff', fontSize: 12, fontWeight: 500 },
      formatter: (params: { axisValue: string; marker: string; seriesName: string; value: number }[]) => {
        if (!params?.length) return ''
        return `<div style="margin-bottom:4px;color:#7ddfff">${params[0].axisValue}</div>` +
          params.map((p) => `${p.marker} ${p.seriesName}：<b style="color:#fff">${p.value}</b>`).join('<br/>')
      }
    },
    xAxis: {
      type: 'category' as const, boundaryGap: false, data: villages,
      axisTick: { show: false }, axisLine: { show: false },
      axisLabel: { color: '#b6e4f8', fontSize: 12, fontWeight: 500, padding: [12, 0, 0, 0] },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value' as const, minInterval: 1,
      axisLabel: { color: '#558ab0', fontSize: 11, fontWeight: 500 },
      axisLine: { show: false }, axisTick: { show: false },
      splitLine: {
        show: true,
        lineStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: 'rgba(0, 180, 240, 0.08)' },
            { offset: 0.5, color: 'rgba(0, 220, 255, 0.18)' },
            { offset: 1, color: 'rgba(0, 180, 240, 0.08)' }
          ]),
          type: 'dashed' as const,
          width: 0.8
        }
      }
    },
    series: series.map((s, i) => {
      const typeIdx = Math.floor(i / 2)
      const typeSpec = LEGEND_TYPE_LINES[typeIdx % LEGEND_TYPE_LINES.length]
      const catIdx = i % 2
      const catSpec = LEGEND_CATEGORY_DOTS[catIdx]
      const isHousehold = catIdx === 0

      return {
        name: s.name, type: 'line' as const, smooth: true,
        showSymbol: true, symbol: 'circle' as const, symbolSize: 7.5, z: 4,

        // 线芯 + 外发光柔光
        lineStyle: {
          width: 2.8,
          type: typeSpec.style,
          color: typeSpec.color,
          shadowBlur: 24,
          shadowColor: hexToRgba(typeSpec.color, 0.85),
          shadowOffsetX: 0,
          shadowOffsetY: 0
        },

        // 玻璃光珠节点：中心白色高亮 + 边缘透明 + 青蓝发光
        itemStyle: {
          color: catSpec.color,
          borderColor: '#ffffff',
          borderWidth: 2,
          opacity: 0.92,
          shadowBlur: 14,
          shadowColor: hexToRgba(catSpec.color, 1)
        },

        // 光幕式透明渐变填充
        areaStyle: {
          origin: 'start' as const,
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0.00, color: hexToRgba(typeSpec.color, 0.35) },
            { offset: 0.25, color: hexToRgba(typeSpec.color, 0.18) },
            { offset: 0.60, color: hexToRgba(typeSpec.color, 0.06) },
            { offset: 1.00, color: hexToRgba(typeSpec.color, 0) }
          ]},
          opacity: 0.85
        },

        label: {
          show: true, position: 'top', distance: 8,
          color: isHousehold ? '#6cf0c4' : '#7feaff',
          fontSize: 11, fontWeight: 600,
          textShadowBlur: 6, textShadowColor: hexToRgba(catSpec.color, 0.55),
          formatter: (p: { value: number }) => p.value > 0 ? String(p.value) : ''
        },
        emphasis: { focus: 'series' as const, scale: true },
        data: s.data
      }
    })
  }
}

/* ========== 玻璃水晶柱 ========== */
function buildBarOption() {
  const stats = overview.value?.typeStats
  const villages = stats?.villages ?? []
  const series = stats?.series ?? []
  const maxVal = Math.max(1, ...series.flatMap((s) => s.data.map((v) => v)))
  const cols = Math.max(1, villages.length)
  const glassW = cols <= 4 ? 86 : cols <= 7 ? 66 : 52

  const layerColors = BAR_STACK_LEGEND.map((l) => l.color)
  const totals = villages.map((_, vi) => series.reduce((sum, s) => sum + (s.data[vi] ?? 0), 0))

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item' as const,
      backgroundColor: 'rgba(4, 14, 34, 0.96)',
      borderColor: 'rgba(0, 240, 255, 0.6)', borderWidth: 1,
      padding: [10, 14],
      boxShadow: '0 4px 20px rgba(0, 160, 255, 0.35)',
      textStyle: { color: '#e4faff', fontSize: 12, fontWeight: 500 }
    },
    grid: { top: 30, left: 20, right: 108, bottom: 62 },
    xAxis: {
      type: 'category' as const, data: villages, boundaryGap: true,
      axisTick: { show: false }, axisLine: { show: false },
      axisLabel: { color: '#ffffff', fontSize: 13, fontWeight: 600, margin: 28 },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value' as const, min: 0, max: maxVal * 1.3,
      axisLabel: { show: false }, axisLine: { show: false }, axisTick: { show: false },
      splitLine: { show: false }
    },
    series: [
      // 底部透视网格承托（8 条射线 + 发光地面线）
      {
        type: 'custom' as const, silent: true, clip: false, z: 0, data: [[0, 0]],
        renderItem: (_: unknown, api: BarAPI) => {
          const L = api.coord([0, 0]), R = api.coord([cols - 1, 0])
          const xL = L[0] - glassW * 0.9, xR = R[0] + glassW * 0.9
          const gy = L[1]
          const cxW = (xR - xL) / 2
          const centerX = (xL + xR) / 2
          const children: Record<string, unknown>[] = []
          // 两侧透视斜线
          for (let i = -3; i <= 3; i++) {
            if (i === 0) continue
            const fwd = i / 3
            children.push({ type: 'line',
              shape: { x1: centerX, y1: gy - 18, x2: centerX + cxW * fwd, y2: gy },
              style: { stroke: 'rgba(0, 220, 255, 0.12)', lineWidth: 0.7 } })
          }
          // 地面发光线 + 渐变
          children.push({ type: 'line',
            shape: { x1: xL, y1: gy, x2: xR, y2: gy },
            style: { stroke: 'rgba(0, 240, 255, 0.95)', lineWidth: 2.2,
              shadowBlur: 22, shadowColor: 'rgba(0, 240, 255, 0.95)' } })
          children.push({ type: 'rect',
            shape: { x: xL, y: gy - 6, width: xR - xL, height: 6 },
            style: { fill: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(0, 220, 255, 0.22)' },
              { offset: 1, color: 'rgba(0, 220, 255, 0)' }
            ])} })
          return { type: 'group', children }
        }
      },
      // 水晶柱
      {
        type: 'custom' as const, z: 3, clip: false,
        data: villages.map((_, vi) => [vi, totals[vi]]),
        tooltip: { formatter: (p: { dataIndex: number }) => {
          const vi = p.dataIndex
          const rows = series.filter((s) => (s.data[vi] ?? 0) > 0)
            .map((s, si) => `${BAR_STACK_LEGEND[si]?.label || s.name}：<b style="color:#fff">${s.data[vi]}</b> 个`)
            .join('<br/>')
          return `<b style="color:#fff">${villages[vi]}</b><br/>${rows || '暂无数据'}<br/><span style="color:#7fb0d4">合计：${totals[vi]} 个</span>`
        }},
        renderItem: (_: unknown, api: BarAPI) => {
          const vi = api.value(0), total = api.value(1)
          const basePt = api.coord([vi, 0])
          const cx = basePt[0], baseY = basePt[1]
          const gridTopY = api.coord([vi, maxVal * 1.3])[1]
          const availH = Math.max(60, baseY - gridTopY)
          const ratio = maxVal > 0 ? total / maxVal : 0
          const crystalH = availH * 0.46 * (0.34 + 0.66 * ratio)
          const crystalTopY = baseY - crystalH
          const glassH = crystalH * 2 + 14
          const glassTopY = baseY - glassH
          const gRx = glassW / 2, gRy = glassW * 0.09
          const cw = glassW * 0.44, cRy = cw * 0.2

          const children: Record<string, unknown>[] = []

          // 底部透视：大椭圆发光环 + 渐变光圈
          children.push({ type: 'ellipse',
            shape: { cx, cy: baseY, rx: gRx * 1.35, ry: gRy * 1.35 },
            style: { fill: new echarts.graphic.RadialGradient(0.5, 0.5, 0.5, [
              { offset: 0, color: 'rgba(0, 240, 255, 0.22)' },
              { offset: 0.5, color: 'rgba(0, 240, 255, 0.08)' },
              { offset: 1, color: 'rgba(0, 240, 255, 0)' }
            ])} })
          children.push({ type: 'ellipse',
            shape: { cx, cy: baseY, rx: gRx * 0.98, ry: gRy * 0.98 },
            style: { fill: new echarts.graphic.RadialGradient(0.5, 0.5, 0.5, [
              { offset: 0, color: 'rgba(0, 240, 255, 0.55)' },
              { offset: 0.7, color: 'rgba(0, 220, 255, 0.2)' },
              { offset: 1, color: 'rgba(0, 180, 240, 0)' }
            ])} })
          children.push({ type: 'ellipse',
            shape: { cx, cy: baseY, rx: gRx * 0.98, ry: gRy * 0.98 },
            style: { stroke: 'rgba(0, 240, 255, 0.5)', lineWidth: 0.8, fill: 'none' } })

          if (total > 0) {
            // 玻璃外壳（带左右渐变高光）
            children.push({ type: 'rect',
              shape: { x: cx - gRx, y: glassTopY, width: glassW, height: glassH },
              style: { fill: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: 'rgba(0, 220, 255, 0.14)' },
                { offset: 0.35, color: 'rgba(0, 240, 255, 0.03)' },
                { offset: 0.65, color: 'rgba(0, 240, 255, 0.03)' },
                { offset: 1, color: 'rgba(0, 220, 255, 0.14)' }
              ]),
              stroke: 'rgba(0, 240, 255, 0.25)', lineWidth: 0.7 } })

            // 分层柱体（从底到顶画，保证视觉顺序）
            let acc = 0
            const layers: { color: string; val: number }[] = []
            for (let si = 0; si < series.length; si++) {
              const val = series[si].data[vi] ?? 0
              if (val > 0) layers.push({ color: layerColors[si] ?? '#00d8ff', val })
            }
            layers.forEach((layer) => {
              const yBot = baseY - (crystalH * acc) / total
              acc += layer.val
              const yTop = baseY - (crystalH * acc) / total
              // 主体
              children.push({ type: 'rect',
                shape: { x: cx - cw / 2, y: yTop, width: cw, height: Math.max(1, yBot - yTop) },
                style: { fill: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                  { offset: 0, color: hexToRgba(layer.color, 0.2) },
                  { offset: 0.35, color: hexToRgba(layer.color, 0.75) },
                  { offset: 0.52, color: 'rgba(255,255,255,0.92)' },
                  { offset: 0.68, color: hexToRgba(layer.color, 0.72) },
                  { offset: 1, color: hexToRgba(layer.color, 0.2) }
                ]), shadowBlur: 18, shadowColor: hexToRgba(layer.color, 0.8) } })
            })

            // 顶层椭圆高光 + 顶部小光圈
            const topColor = layers[layers.length - 1]?.color ?? layerColors[0]
            children.push({ type: 'ellipse',
              shape: { cx, cy: crystalTopY, rx: cw / 2, ry: cRy },
              style: { fill: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(255,255,255,0.95)' },
                { offset: 0.45, color: hexToRgba(topColor, 0.88) },
                { offset: 1, color: hexToRgba(topColor, 0.6) }
              ]), shadowBlur: 18, shadowColor: hexToRgba(topColor, 1) } })
            children.push({ type: 'ellipse',
              shape: { cx, cy: crystalTopY, rx: cw * 0.38, ry: cRy * 0.5 },
              style: { fill: 'rgba(255,255,255,0.55)', opacity: 0.9 } })

            // 顶部数字（发光白色）
            children.push({ type: 'text',
              style: { text: String(total), x: cx, y: glassTopY - 8, textAlign: 'center',
                textVerticalAlign: 'bottom', fill: '#ffffff', fontSize: 15, fontWeight: 'bold',
                textShadowBlur: 14, textShadowColor: 'rgba(0, 240, 255, 0.95)' } })
          }
          return { type: 'group', children }
        }
      }
    ]
  }
}

function renderCharts() {
  if (detailRef.value) {
    detailChart = detailChart ?? initChart(detailRef.value)
    detailChart.setOption(buildDetailOption(), true)
  }
  if (barRef.value) {
    barChart = barChart ?? initChart(barRef.value)
    barChart.setOption(buildBarOption(), true)
  }
}

async function loadData() {
  try {
    const res = await getScreenOverview()
    overview.value = res.data
    mergeEvents(res.data?.events)
    await nextTick(); renderCharts()
  } catch { /* 拦截器已处理 */ }
}

function openCast() {
  const { href } = router.resolve({ name: 'DataScreen2Cast' })
  window.open(href, '_blank', 'noopener')
}

onMounted(async () => {
  resizeObserver = new ResizeObserver(async () => { await resizeCanvas() })
  if (rootRef.value) resizeObserver.observe(rootRef.value)
  window.addEventListener('resize', resizeCanvas)
  resizeCanvas()
  updateClock()
  clockTimer = window.setInterval(updateClock, 1000)
  refreshTimer = window.setInterval(loadData, REFRESH_INTERVAL)
  loading.value = true; await loadData(); loading.value = false
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect(); resizeObserver = null
  window.removeEventListener('resize', resizeCanvas)
  if (clockTimer) window.clearInterval(clockTimer)
  if (refreshTimer) window.clearInterval(refreshTimer)
  window.cancelAnimationFrame(metricRaf)
  ;[detailChart, barChart].forEach((c) => c?.dispose())
  detailChart = null; barChart = null
})
</script>

<!-- ================= 样式：严格按清单 ================= -->
<style scoped>
/* ============================================================
   仅数据大屏2用的面板视觉升级（:deep 穿透到共享组件，不影响大屏1）
   核心：对角渐变发光边框 + 左上亮右下暗的方向性辉光
   ============================================================ */

/* 覆盖外层 .screen-panel：去掉原四边 border，改用 clip-path 切角 + drop-shadow 辉光 */
.screen-root :deep(.screen-panel) {
  border-top: none !important;
  border-right: none !important;
  border-bottom: none !important;
  border-left: none !important;
  padding: 0 !important;
  background: transparent !important;
  display: flex;
  flex: 1;
  /* 切角 */
  clip-path: polygon(18px 0, 100% 0, 100% calc(100% - 18px), calc(100% - 18px) 100%, 0 100%, 0 18px);
  /* 左上方向偏置的复合辉光 */
  filter:
    drop-shadow(0 14px 26px rgba(0, 6, 18, 0.8))
    drop-shadow(-10px -10px 18px rgba(150, 230, 255, 0.35))
    drop-shadow(0 0 14px rgba(0, 220, 255, 0.55))
    drop-shadow(0 0 32px rgba(0, 180, 255, 0.35));
}
/* ::before：整幅 135deg 对角渐变底色（被 panel-inner 盖住中间，四周自然露出 2px 边框） */
.screen-root :deep(.screen-panel)::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: linear-gradient(
    135deg,
    #f4fdff 0%,        /* 左上：接近纯白的冰蓝 */
    #c5f1ff 12%,
    #72daff 30%,
    #28a7f0 52%,
    #0e6cc0 76%,
    #073270 100%       /* 右下：深蓝 */
  );
  clip-path: polygon(18px 0, 100% 0, 100% calc(100% - 18px), calc(100% - 18px) 100%, 0 100%, 0 18px);
}
/* ::after：左上额外高光 + 右下额外压暗，让方向感更强 */
.screen-root :deep(.screen-panel)::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
    radial-gradient(circle at 25% 25%, rgba(244, 253, 255, 0.55) 0%, transparent 48%),
    radial-gradient(circle at 85% 85%, rgba(4, 20, 50, 0.45) 0%, transparent 55%);
  mix-blend-mode: normal;
  clip-path: polygon(18px 0, 100% 0, 100% calc(100% - 18px), calc(100% - 18px) 100%, 0 100%, 0 18px);
}
/* 覆盖内层 .panel-inner：padding 2px 露出 ::before 的 2px 渐变边框底色 */
.screen-root :deep(.panel-inner) {
  position: relative;
  z-index: 2 !important;
  padding: 2px !important;
  clip-path: polygon(16px 0, 100% 0, 100% calc(100% - 16px), calc(100% - 16px) 100%, 0 100%, 0 16px) !important;
  background:
    radial-gradient(ellipse 90% 60% at 12% -12%, rgba(56, 132, 200, 0.22) 0%, rgba(10, 22, 36, 0) 62%),
    linear-gradient(180deg, rgba(8, 24, 46, 0.8) 0%, rgba(4, 14, 30, 0.85) 45%, rgba(3, 9, 20, 0.8) 100%) !important;
  box-shadow:
    inset 0 1px 0 rgba(180, 230, 255, 0.32),
    inset 0 0 30px rgba(40, 140, 230, 0.12),
    inset 0 -20px 40px rgba(0, 0, 0, 0.4) !important;
  backdrop-filter: blur(2px) saturate(110%);
  -webkit-backdrop-filter: blur(2px) saturate(110%);
}

/* ===== 基础容器 ===== */
.screen-root { position: absolute; inset: 0; overflow: hidden; background: #00030a; }
.screen-root.is-standalone { position: fixed; inset: 0; z-index: 2000; }

/* ===== 深邃午夜蓝背景 ===== */
.screen-canvas {
  position: absolute; top: 0; left: 0;
  width: 1920px; height: 1080px;
  display: flex; flex-direction: column;
  transform-origin: top left;
  color: #cfe9ff;
  font-family: 'Microsoft YaHei', 'PingFang SC', Helvetica, Arial, sans-serif;
  /* 主底：午夜蓝渐变 */
  background:
    radial-gradient(ellipse 90% 55% at 50% -10%, rgba(0, 80, 180, 0.38) 0%, rgba(0, 80, 180, 0) 70%),
    radial-gradient(ellipse 40% 30% at 50% 55%, rgba(0, 180, 255, 0.08) 0%, rgba(0, 180, 255, 0) 65%),
    radial-gradient(ellipse 35% 25% at 85% 10%, rgba(0, 150, 240, 0.1) 0%, rgba(0, 150, 240, 0) 60%),
    linear-gradient(180deg, #00030a 0%, #04111e 40%, #000a18 100%);
}
/* 细网格 + 微光粒子 */
.screen-canvas::before {
  content: ''; position: absolute; inset: 0; pointer-events: none; z-index: 0;
  background-image:
    linear-gradient(rgba(0, 200, 255, 0.05) 0.5px, transparent 0.5px),
    linear-gradient(90deg, rgba(0, 200, 255, 0.05) 0.5px, transparent 0.5px);
  background-size: 56px 56px; opacity: 0.7;
}
.screen-canvas::after {
  content: ''; position: absolute; inset: 0; pointer-events: none; z-index: 0;
  background:
    radial-gradient(circle at 18% 28%, rgba(0, 230, 255, 0.12) 0%, transparent 45%),
    radial-gradient(circle at 82% 68%, rgba(0, 180, 255, 0.08) 0%, transparent 40%),
    radial-gradient(circle at 50% 50%, rgba(0, 120, 220, 0.05) 0%, transparent 60%);
  animation: bg-drift 20s ease-in-out infinite alternate;
}
@keyframes bg-drift {
  0% { transform: translate(0, 0); }
  100% { transform: translate(14px, -10px); }
}

/* ===== 顶部标题区 ===== */
.screen-header {
  position: relative; z-index: 2;
  display: grid; grid-template-columns: 1fr auto 1fr;
  align-items: center; height: 96px;
  padding: 18px 36px 0; flex-shrink: 0;
}
/* 顶部发光光带 */
.screen-header::before {
  content: ''; position: absolute; left: 0; right: 0; top: 0; height: 3px;
  background: linear-gradient(90deg, transparent 0%, rgba(0, 220, 255, 0.35) 15%, #00f6ff 50%, rgba(0, 220, 255, 0.35) 85%, transparent 100%);
  box-shadow: 0 0 18px rgba(0, 240, 255, 0.8), 0 0 60px rgba(0, 180, 255, 0.35);
}
/* 标题下方的柔和光带 */
.screen-header::after {
  content: ''; position: absolute; left: 50%; bottom: -2px;
  width: 60%; height: 1px; transform: translateX(-50%);
  background: linear-gradient(90deg, transparent, rgba(0, 216, 255, 0.45), transparent);
}
.header-side { display: flex; align-items: center; gap: 10px; min-width: 0; }
.header-left { justify-content: flex-start; }
.header-right { justify-content: flex-end; gap: 14px; }

/* 左上角胶囊 */
.chip {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 5px 14px; border-radius: 2px;
  border: 1px solid rgba(0, 220, 255, 0.6);
  background: linear-gradient(135deg, rgba(6, 42, 80, 0.9) 0%, rgba(3, 18, 40, 0.75) 100%);
  color: #e3f6ff; font-size: 12px; white-space: nowrap;
  box-shadow: 0 0 8px rgba(0, 216, 255, 0.4), inset 0 1px 0 rgba(0, 240, 255, 0.22);
}
.chip-dot { width: 7px; height: 7px; border-radius: 50%; background: #00f0b0;
  box-shadow: 0 0 8px #00f0b0, 0 0 14px rgba(0, 240, 176, 0.6);
  animation: chip-pulse 1.8s ease-in-out infinite; }

/* 标题：对称折线结构 + 白色黑体 + 渐变光带 */
.header-mid { display: flex; align-items: center; gap: 14px; position: relative; }
.title-bar {
  position: relative; width: 100px; height: 2px;
  background: linear-gradient(90deg, rgba(0, 220, 255, 0.6), rgba(0, 240, 255, 0.15));
  box-shadow: 0 0 10px rgba(0, 216, 255, 0.45);
}
.title-bar::after {
  content: ''; position: absolute; top: -4px; left: -2px;
  width: 10px; height: 10px;
  border-top: 2px solid rgba(0, 240, 255, 0.9);
  border-left: 2px solid rgba(0, 240, 255, 0.9);
  transform: rotate(-45deg);
}
.header-mid .title-bar:last-child {
  background: linear-gradient(90deg, rgba(0, 240, 255, 0.15), rgba(0, 220, 255, 0.6));
}
.header-mid .title-bar:last-child::after {
  left: auto; right: -2px;
  border-top: none; border-bottom: 2px solid rgba(0, 240, 255, 0.9);
  border-right: 2px solid rgba(0, 240, 255, 0.9);
  border-left: none; top: -4px;
  transform: rotate(45deg);
}

.header-title {
  position: relative; z-index: 2;
  margin: 0; font-size: 38px; font-weight: 900; letter-spacing: 10px; line-height: 1.15;
  padding-left: 10px;
  /* 白色黑体 + 青蓝渐变底部发光 */
  color: #ffffff;
  text-shadow:
    0 0 14px rgba(0, 220, 255, 0.7),
    0 0 30px rgba(0, 180, 255, 0.45),
    0 2px 6px rgba(0, 0, 0, 0.85);
}

/* 右上角 */
.clock { display: flex; flex-direction: column; align-items: flex-end; line-height: 1.25; }
.clock-date { font-size: 12px; color: #84b2d0; letter-spacing: 1px; }
.clock-time { font-size: 22px; font-weight: 700; color: #ffffff; letter-spacing: 2px;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 8px rgba(0, 220, 255, 0.8), 0 0 20px rgba(0, 180, 255, 0.4); }

.cast-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 14px; border-radius: 2px; cursor: pointer; transition: all .2s;
  border: 1px solid rgba(0, 220, 255, 0.55);
  background: rgba(4, 32, 72, 0.75); color: #bfe9ff; font-size: 12px;
  box-shadow: inset 0 1px 0 rgba(0, 240, 255, 0.15);
}
.cast-btn:hover { background: rgba(4, 70, 140, 0.85); color: #fff;
  box-shadow: 0 0 18px rgba(0, 216, 255, 0.55), inset 0 1px 0 rgba(0, 240, 255, 0.3); }

/* ===== 三栏布局 ===== */
.screen-body {
  position: relative; z-index: 1;
  display: grid; grid-template-columns: 24% 1fr 24%;
  gap: 12px; flex: 1; min-height: 0;
  padding: 10px 16px; box-sizing: border-box;
}
.col { display: grid; gap: 12px; min-height: 0; }
.col-left { grid-template-rows: 1.16fr 1.04fr 1.3fr; }
.col-center { grid-template-rows: 98px 1.32fr 1fr; }
.col-right { grid-template-rows: 260px 1fr 1fr; }

/* ===== KPI 四卡：强发光渐变边框 ===== */
.kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.kpi-card {
  position: relative; z-index: 1;
  display: flex; align-items: center; gap: 18px;
  height: 100%; padding: 10px 20px;
  /* 切角 */
  clip-path: polygon(12px 0, 100% 0, 100% calc(100% - 12px), calc(100% - 12px) 100%, 0 100%, 0 12px);
  /* 玻璃底 */
  background:
    linear-gradient(135deg, rgba(8, 42, 78, 0.92) 0%, rgba(3, 16, 36, 0.95) 100%);
  /* 强发光：三层 drop-shadow + 青蓝光 */
  filter:
    drop-shadow(0 10px 18px rgba(0, 6, 18, 0.7))
    drop-shadow(0 0 6px rgba(0, 180, 255, 0.35))
    drop-shadow(0 0 18px rgba(0, 220, 255, 0.25));
}
/* 左侧竖条 + 顶部发光线 */
.kpi-card::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; z-index: 0;
  background: linear-gradient(180deg, rgba(0, 246, 255, 0) 0%, rgba(0, 246, 255, 1) 35%,
    rgba(0, 216, 255, 1) 65%, rgba(0, 246, 255, 0) 100%);
  box-shadow: 0 0 10px currentColor, 0 0 22px currentColor;
}
/* 渐变发光边框（顶+底+右）：用 border-image 不好切角，改用 overlay span */
.kpi-card::after {
  content: ''; position: absolute; inset: 0; pointer-events: none; z-index: 0;
  background:
    linear-gradient(90deg, transparent, rgba(0, 246, 255, 0.9) 30%, rgba(0, 216, 255, 0.5) 70%, transparent) 100% 0 / 100% 1.5px no-repeat,
    linear-gradient(90deg, transparent, rgba(0, 216, 255, 0.5) 30%, rgba(0, 246, 255, 0.9) 70%, transparent) 0 100% / 100% 1px no-repeat,
    linear-gradient(180deg, transparent, rgba(0, 180, 255, 0.6) 35%, rgba(0, 180, 255, 0.6) 65%, transparent) 100% 0 / 1px 100% no-repeat;
  box-shadow: inset 0 0 0 0.5px rgba(0, 200, 255, 0.3);
  clip-path: inherit;
}
.kpi-cyan { color: #00d8ff; }
.kpi-green { color: #00f0b0; }
.kpi-purple { color: #a37bff; }

.kpi-ic-wrap {
  position: relative; z-index: 2;
  flex: 0 0 54px; width: 54px; height: 54px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  /* 玻璃质感：外圈发光 + 内圈光 */
  background:
    radial-gradient(circle at 50% 50%, rgba(0, 240, 255, 0.18) 0%, rgba(0, 240, 255, 0.06) 50%, transparent 80%),
    linear-gradient(180deg, rgba(0, 60, 110, 0.8), rgba(0, 20, 40, 0.9));
  border: 1px solid currentColor; color: currentColor;
  box-shadow:
    0 0 18px currentColor,
    inset 0 0 14px rgba(0, 240, 255, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  font-size: 28px;
}

.kpi-body { position: relative; z-index: 2; display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.kpi-value { font-size: 34px; font-weight: 700; color: #ffffff; line-height: 1;
  letter-spacing: 0.5px; font-variant-numeric: tabular-nums;
  text-shadow: 0 0 10px currentColor, 0 0 22px currentColor; }
.kpi-value em { font-size: 13px; font-style: normal; font-weight: 500; margin-left: 3px; opacity: 0.9; }
.kpi-label { font-size: 13px; color: #c6e4f8; letter-spacing: 1px; }

/* ===== 左：村分布地图 ===== */
.village-map {
  position: relative; width: 100%; height: 100%;
  border-radius: 2px;
  background:
    radial-gradient(circle at 50% 50%, rgba(0, 90, 170, 0.28) 0%, rgba(2, 14, 32, 0) 68%),
    linear-gradient(180deg, rgba(4, 24, 48, 0.6) 0%, rgba(1, 6, 18, 0.25) 100%);
  box-shadow: inset 0 0 50px rgba(0, 10, 30, 0.85);
  overflow: hidden;
}
.village-map::before {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background-image:
    linear-gradient(rgba(0, 200, 255, 0.045) 0.5px, transparent 0.5px),
    linear-gradient(90deg, rgba(0, 200, 255, 0.045) 0.5px, transparent 0.5px);
  background-size: 36px 36px; opacity: 0.8;
}
.map-grid, .map-links { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 1; }
.map-grid path { fill: none; stroke: rgba(0, 220, 255, 0.18); stroke-width: 0.35; stroke-dasharray: 1.8 2; }
.map-links line { stroke: rgba(0, 220, 255, 0.32); stroke-width: 0.4; stroke-dasharray: 1.8 1.2; filter: drop-shadow(0 0 2px rgba(0, 220, 255, 0.4)); }

.map-center {
  position: absolute; left: 48%; top: 50%; width: 16px; height: 16px; z-index: 3;
  transform: translate(-50%, -50%); border-radius: 50%;
  background: radial-gradient(circle, #ffffff 0%, #00f6ff 35%, rgba(0, 220, 255, 0.35) 65%, transparent 90%);
  box-shadow: 0 0 20px rgba(0, 240, 255, 0.95);
  animation: node-pulse 2.2s ease-in-out infinite;
}
.map-node { position: absolute; display: flex; align-items: center; gap: 6px;
  transform: translate(-50%, -50%); white-space: nowrap; color: #fff; z-index: 2; }
.node-halo {
  position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%);
  width: calc(var(--ns, 24px) + 18px); height: calc(var(--ns, 24px) + 18px);
  border-radius: 50%; z-index: -1;
  background: radial-gradient(circle, currentColor 0%, rgba(0,0,0,0) 70%);
  opacity: 0.35; animation: halo-pulse 2.4s ease-in-out infinite;
}
.node-core {
  width: var(--ns, 24px); height: var(--ns, 24px); border-radius: 50%;
  background: radial-gradient(circle, #ffffff 0%, currentColor 55%, rgba(0,0,0,0) 100%);
  box-shadow: 0 0 14px currentColor, 0 0 28px rgba(0, 180, 255, 0.45);
}
.node-label { display: flex; flex-direction: column; font-size: 11px; line-height: 1.3; }
.node-label b { color: #eaf7ff; font-weight: 500; text-shadow: 0 0 6px rgba(0,0,0,0.9), 0 0 3px currentColor; }
.node-label em { font-size: 10px; font-style: normal; color: #b7dcf0; text-shadow: 0 0 5px rgba(0,0,0,0.85); }

/* ===== 资金仪表盘 ===== */
.fund-wrap { display: flex; align-items: center; justify-content: space-around; gap: 10px; height: 100%; min-height: 0; }
.fund-ring { flex: 0 0 44%; aspect-ratio: 1; position: relative; display: flex; align-items: center; justify-content: center; }
.fund-svg { width: 100%; height: 100%; filter: drop-shadow(0 0 12px rgba(0, 220, 255, 0.5)); }
.ring-base { fill: none; stroke: rgba(6, 28, 60, 0.95); stroke-width: 10; }
.ring-progress {
  fill: none; stroke: url(#fundGrad); stroke-width: 10; stroke-linecap: round;
  transform: rotate(-90deg); transform-origin: 80px 80px;
  filter: drop-shadow(0 0 8px rgba(0, 240, 255, 0.85));
}
.ring-inner { fill: rgba(1, 10, 26, 0.95); stroke: rgba(0, 220, 255, 0.25); stroke-width: 1.2; }

.fund-center { position: absolute; z-index: 3; text-align: center; }
.fund-center b { font-size: 28px; font-weight: 900; line-height: 1;
  background: linear-gradient(180deg, #ffffff 0%, #baf0ff 40%, #00c8ff 100%);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  text-shadow: 0 0 14px rgba(0, 216, 255, 0.9); }
.fund-center b em { font-size: 14px; font-style: normal; }
.fund-center span { display: block; margin-top: 6px; font-size: 11px; color: #84b2d0; letter-spacing: 1px; }

.fund-stats { flex: 1 1 auto; min-width: 0; display: flex; flex-direction: column; justify-content: space-around; align-items: flex-end; height: 80%; }
.fund-stat { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
.fs-label { font-size: 11px; color: #84b2d0; letter-spacing: 1px; }
.fs-value b { font-size: 20px; font-weight: 700; color: #ffffff; text-shadow: 0 0 8px rgba(0, 216, 255, 0.55); }
.fs-value small { font-size: 10px; color: #84b2d0; margin-left: 2px; opacity: 0.85; }

/* ===== 滚动列表通用 ===== */
.scroll-viewport { height: 100%; overflow: hidden; }
.scroll-track.scrolling { animation: scroll-var linear infinite; }
.scroll-track.scrolling:hover { animation-play-state: paused; }
@keyframes scroll-var { from { transform: translateY(0); } to { transform: translateY(-50%); } }
.scroll-list { display: flex; flex-direction: column; gap: 5px; margin: 0; padding: 0; list-style: none; overflow: hidden; }

/* ===== 物资 ===== */
.mat-item {
  display: grid; grid-template-columns: 1fr 60px 60px 10px; gap: 8px; align-items: center;
  padding: 7px 10px; font-size: 12px;
  border-left: 2px solid rgba(0, 220, 255, 0.4);
  background: linear-gradient(90deg, rgba(5, 40, 80, 0.55) 0%, rgba(2, 16, 38, 0.2) 100%);
}
.mat-item.mat-alt { background: linear-gradient(90deg, rgba(8, 56, 100, 0.48) 0%, rgba(3, 22, 46, 0.15) 100%); }
.mat-name { color: #eaf7ff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mat-stock { text-align: right; color: #84b2d0; }
.mat-stock b { font-size: 14px; margin-right: 2px; color: #00d8ff; text-shadow: 0 0 6px rgba(0, 216, 255, 0.55); }
.mat-stock b.warn { color: #ff6b81; text-shadow: 0 0 10px rgba(255, 107, 129, 0.8); }
.mat-stock small { font-size: 10px; opacity: 0.75; }
.mat-warn { font-size: 11px; color: #84b2d0; text-align: right; }
.mat-dot { width: 6px; height: 6px; border-radius: 50%; background: #00f0b0; justify-self: end; box-shadow: 0 0 8px #00f0b0; }
.mat-dot.dot-warn { background: #ff6b81; box-shadow: 0 0 10px #ff6b81; }

/* ===== 图例 ===== */
.chart-stage { position: relative; width: 100%; height: 100%; }
.chart { width: 100%; height: 100%; }
.legend-box { position: absolute; top: 2px; right: 2px; z-index: 3;
  display: flex; flex-direction: column; gap: 4px; align-items: flex-end; pointer-events: none; }
.legend-row { display: flex; flex-wrap: wrap; gap: 12px; }
.lg-item { display: inline-flex; align-items: center; gap: 5px; font-size: 11px; color: #c6e4f8; white-space: nowrap; }
.lg-line { display: inline-block; width: 18px; border-top: 2px solid #00d8ff; }
.lg-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; box-shadow: 0 0 4px currentColor; }
.detail-empty { position: absolute; inset: 0; z-index: 2; }

.bar-legend { position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  display: flex; flex-direction: column; gap: 14px; pointer-events: none; }
.bl-item { display: flex; align-items: center; gap: 6px; font-size: 11px; color: #ffffff; white-space: nowrap;
  text-shadow: 0 0 2px rgba(0,0,0,0.7); }
.bl-item i { width: 10px; height: 10px; border-radius: 2px; flex-shrink: 0; box-shadow: 0 0 4px currentColor; }

/* ===== 核心指标 ===== */
.metric-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; height: 100%; }
.metric-card {
  position: relative; padding: 8px 10px;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
  clip-path: polygon(0 10px, 10px 0, 100% 0, 100% calc(100% - 10px), calc(100% - 10px) 100%, 0 100%);
  background:
    linear-gradient(90deg, rgba(0, 220, 255, 0.12) 0%, transparent 25%, transparent 75%, rgba(0, 220, 255, 0.12) 100%),
    linear-gradient(180deg, rgba(4, 28, 56, 0.78) 0%, rgba(2, 12, 28, 0.92) 100%);
  overflow: hidden;
}
.metric-card::before { content: ''; position: absolute; inset: 0; pointer-events: none;
  box-shadow:
    inset 1px 1px 0 rgba(0, 240, 255, 0.22),
    inset -1px -1px 0 rgba(0, 80, 160, 0.45);
  clip-path: inherit; }
.metric-deco {
  position: absolute; left: 50%; top: -14px; transform: translateX(-50%);
  width: 85%; height: 44px; pointer-events: none;
  background: radial-gradient(ellipse 50% 100% at 50% 100%, currentColor 0%, transparent 75%); opacity: 0.45;
}
.metric-deco--rev { top: auto; bottom: -14px; transform: translateX(-50%) scaleY(-1); opacity: 0.28; }
.metric-cyan { color: #00d8ff; }
.metric-yellow { color: #ffc857; }
.metric-pink { color: #ff87b0; }

.metric-num-wrap { display: inline-flex; align-items: baseline; gap: 4px; line-height: 1.1; z-index: 2; }
.metric-num { font-size: 30px; font-weight: 700; color: #fff; font-variant-numeric: tabular-nums; text-shadow: 0 0 10px currentColor, 0 0 20px currentColor; }
.metric-unit { font-size: 12px; color: currentColor; font-weight: 500; }
.metric-label { font-size: 12px; color: #ffffff; letter-spacing: 1px; z-index: 2; text-shadow: 0 0 3px rgba(0,0,0,0.8); }

/* ===== 状态圆盘 ===== */
.donut-wrap { position: relative; width: 100%; height: 100%; }
.donut-svg { position: absolute; inset: 0; width: 100%; height: 100%; overflow: visible; z-index: 1; }
.orbit-a { stroke: rgba(0, 216, 255, 0.3); stroke-width: 1; stroke-dasharray: 3 6; }
.orbit-b { stroke: rgba(0, 220, 255, 0.65); stroke-width: 1.4; filter: drop-shadow(0 0 6px rgba(0, 220, 255, 0.4)); }
.orbit-flow { stroke: rgba(0, 245, 255, 0.95); stroke-width: 1.6; stroke-linecap: round;
  stroke-dasharray: 66 496; filter: drop-shadow(0 0 4px rgba(0, 245, 255, 0.55));
  animation: orbit-flow 4.8s linear infinite; }
@keyframes orbit-flow { from { stroke-dashoffset: 562; } to { stroke-dashoffset: 0; } }

.platform { z-index: 2; }
.platform-base { fill: rgba(0, 56, 120, 0.68); stroke: rgba(0, 220, 255, 0.92); stroke-width: 1;
  filter: drop-shadow(0 4px 6px rgba(0, 7, 18, 0.65)); }
.platform-top { fill: rgba(0, 180, 240, 0.2); stroke: rgba(0, 245, 255, 0.98); stroke-width: 1.1;
  filter: drop-shadow(0 0 10px rgba(0, 220, 255, 0.65)); }

.donut-center { position: absolute; left: 50%; top: 38%; z-index: 3;
  display: flex; flex-direction: column; align-items: center;
  transform: translate(-50%, -50%); pointer-events: none; }
.donut-center b { font-size: 44px; font-weight: 700; line-height: 1; color: #ffffff;
  font-variant-numeric: tabular-nums; text-shadow: 0 0 14px rgba(0, 220, 255, 0.95), 0 0 28px rgba(0, 180, 255, 0.4); }
.donut-center span { margin-top: 6px; font-size: 11px; letter-spacing: 3px; color: #84b2d0; }

.donut-label { position: absolute; display: flex; flex-direction: column; gap: 2px; white-space: nowrap;
  transform: translate(2px, -50%); pointer-events: none; z-index: 2; }
.donut-label.is-left { transform: translate(calc(-100% - 2px), -50%); align-items: flex-end; }
.dlbl-name { font-size: 11px; color: #ffffff; text-shadow: 0 0 3px rgba(0,0,0,0.8); }
.dlbl-value { font-size: 18px; font-weight: 700; line-height: 1; color: var(--lc, #00d8ff);
  text-shadow: 0 0 10px var(--lc, rgba(0, 216, 255, 0.8)); font-variant-numeric: tabular-nums; }
.donut-label.is-zero { opacity: 0.42; }

/* ===== 实时事件 ===== */
.ev-item {
  display: grid; grid-template-columns: 8px 1fr auto; gap: 8px; align-items: flex-start;
  padding: 6px 10px;
  border-left: 2px solid rgba(0, 220, 255, 0.55);
  background: linear-gradient(90deg, rgba(5, 40, 74, 0.55) 0%, rgba(2, 16, 34, 0.18) 100%);
  font-size: 12px;
}
.ev-dot { width: 8px; height: 8px; margin-top: 5px; border-radius: 50%;
  box-shadow: 0 0 8px currentColor, 0 0 14px currentColor; flex-shrink: 0; }
.ev-text { color: #ffffff; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2;
  line-height: 1.4; overflow: hidden; word-break: break-all; text-shadow: 0 0 2px rgba(0,0,0,0.9); }
.ev-time { font-size: 11px; color: #84b2d0; font-variant-numeric: tabular-nums; flex-shrink: 0; }

/* ===== 底部数据流 ===== */
.screen-ticker {
  position: relative; z-index: 1;
  display: flex; align-items: center; gap: 14px;
  height: 48px; margin: 0 16px 10px; padding: 0 14px;
  border-top: 1px solid rgba(0, 220, 255, 0.45);
  background:
    linear-gradient(90deg, rgba(4, 40, 74, 0.58) 0%, rgba(2, 16, 34, 0.2) 100%),
    linear-gradient(180deg, rgba(0, 100, 180, 0.12) 0%, transparent 100%);
  overflow: hidden;
  box-shadow: inset 0 1px 0 rgba(0, 240, 255, 0.15);
}
.ticker-label { flex-shrink: 0; font-size: 12px; letter-spacing: 2px; color: #00d8ff;
  text-shadow: 0 0 10px rgba(0, 220, 255, 0.6); }
.ticker-viewport { flex: 1; overflow: hidden; min-width: 0; }
.ticker-track { display: inline-flex; align-items: center; gap: 46px; white-space: nowrap;
  animation: ticker-slide 40s linear infinite; }
.ticker-item { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: #84b2d0; }
.ticker-item em { width: 5px; height: 5px; transform: rotate(45deg); background: #00d8ff;
  box-shadow: 0 0 6px rgba(0, 220, 255, 0.9); }
.ticker-item b { font-weight: 600; color: #ffffff; text-shadow: 0 0 2px rgba(0,0,0,0.8); }
.ticker-update { flex-shrink: 0; font-size: 11px; color: #4f7a9e; }
@keyframes ticker-slide { from { transform: translateX(0); } to { transform: translateX(-50%); } }

/* ===== 通用 ===== */
.empty-tip { position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%);
  margin: 0; font-size: 12px; color: #4f7a9e; }
@keyframes chip-pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }
@keyframes node-pulse { 0%,100% { transform: translate(-50%, -50%) scale(1); opacity: 1; } 50% { transform: translate(-50%, -50%) scale(1.3); opacity: 0.6; } }
@keyframes halo-pulse { 0%,100% { opacity: 0.75; transform: translate(-50%, -50%) scale(1); } 50% { opacity: 0.3; transform: translate(-50%, -50%) scale(1.25); } }
</style>
