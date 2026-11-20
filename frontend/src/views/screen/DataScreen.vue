<template>
  <div
    ref="rootRef"
    class="screen-root"
    :class="{ 'is-standalone': standalone }"
    v-loading="loading"
    element-loading-background="rgba(3, 10, 20, 0.72)"
  >
    <div class="screen-canvas" :style="canvasStyle">
      <!-- ========== 顶部标题栏 ========== -->
      <header class="screen-header">
        <div class="header-side">
          <span class="status-chip"><i class="chip-dot"></i>系统在线</span>
          <span class="status-chip">
            <el-icon><OfficeBuilding /></el-icon>{{ kpi.villageCount }} 个行政村
          </span>
          <span class="status-chip">
            <el-icon><CircleCheck /></el-icon>数据正常
          </span>
        </div>
        <div class="header-center">
          <h1 class="header-title">民生建设项目管理平台</h1>
        </div>
        <div class="header-side header-right">
          <div class="clock">
            <span class="clock-date">{{ dateText }}</span>
            <span class="clock-time">{{ timeText }}</span>
          </div>
          <button v-if="!standalone" class="exit-btn" @click="openCast">
            <el-icon><Monitor /></el-icon>投屏
          </button>
        </div>
      </header>

      <!-- ========== 主体三栏 ========== -->
      <main class="screen-body">
        <!-- 左栏 -->
        <section class="col col-left">
          <ScreenPanel title="各村项目分布">
            <div class="village-map">
              <div class="map-caption">
                <span class="map-town">{{ regionText }}</span>
              </div>
              <div class="map-ring map-ring-outer"></div>
              <div class="map-ring map-ring-inner"></div>
              <svg class="map-links" viewBox="0 0 100 100" preserveAspectRatio="none">
                <line
                  v-for="node in mapNodes"
                  :key="`link-${node.name}`"
                  :x1="MAP_CENTER.x"
                  :y1="MAP_CENTER.y"
                  :x2="node.x"
                  :y2="node.y"
                />
              </svg>
              <div
                v-for="node in mapNodes"
                :key="node.name"
                class="map-node"
                :style="{
                  left: `${node.x}%`,
                  top: `${node.y}%`,
                  '--node-size': `${node.size}px`,
                  '--node-color': node.color
                }"
              >
                <i class="map-node-core"></i>
                <span class="map-node-label">
                  {{ node.name }}
                  <em>{{ node.projectCount }} 个项目</em>
                </span>
              </div>
              <p v-if="!mapNodes.length" class="map-empty">暂无村庄数据</p>
            </div>
          </ScreenPanel>

          <ScreenPanel title="资金完成率">
            <div class="gauge-wrap">
              <div ref="fundGaugeRef" class="chart"></div>
              <div class="gauge-center">
                <b>{{ kpi.payRate.toFixed(1) }}%</b>
                <span>拨付完成率</span>
              </div>
            </div>
          </ScreenPanel>

          <ScreenPanel title="物资状态">
            <div v-if="materialStats.length" class="material-viewport">
              <div class="material-track" :class="{ scrolling: materialScrollable }" :style="materialScrollStyle">
                <ul v-for="copy in materialScrollable ? 2 : 1" :key="copy" class="material-list" :aria-hidden="copy === 2">
                  <li v-for="item in materialStats" :key="item.itemName" class="material-item">
                    <span class="material-name" :title="item.itemName">{{ item.itemName }}</span>
                    <span class="material-stock">
                      <b :class="{ danger: item.warning }">{{ item.stockQty }}</b>
                      {{ item.unit || '件' }}
                    </span>
                    <span class="material-warn">预警 {{ item.warningQty }}</span>
                    <i class="material-flag" :class="{ danger: item.warning }"></i>
                  </li>
                </ul>
              </div>
            </div>
            <p v-else class="panel-empty">暂无物资数据</p>
          </ScreenPanel>
        </section>

        <!-- 中栏 -->
        <section class="col col-center">
          <div class="kpi-row">
            <div
              v-for="(item, index) in kpiCards"
              :key="item.label"
              class="kpi-card"
              :class="[
                `kpi-tone-${item.tone}`,
                index % 2 === 0 ? 'kpi-card--back' : 'kpi-card--front'
              ]"
            >
              <div class="kpi-float">
                <!-- 底部悬浮光晕 -->
                <span class="kpi-halo" aria-hidden="true"></span>
                <!-- 椭圆全息扫描环 -->
                <span class="kpi-rings" aria-hidden="true">
                  <i></i>
                  <i></i>
                  <i></i>
                </span>
                <!-- 玻璃胶囊本体 -->
                <div class="kpi-glass">
                  <span class="kpi-value">{{ item.value }}<em>{{ item.unit }}</em></span>
                  <span class="kpi-label">{{ item.label }}</span>
                </div>
              </div>
            </div>
          </div>

          <ScreenPanel title="各村项目类型明细统计">
            <div class="detail-stage">
              <!-- 自定义图例：上排线型=项目类型，下排颜色=明细类别(单位) -->
              <div v-if="detailHasData" class="detail-legend">
                <div class="detail-legend__row">
                  <span
                    v-for="(label, i) in detailTypeLabels"
                    :key="`dtl-${label}`"
                    class="detail-legend__item"
                  >
                    <i
                      class="detail-legend__line"
                      :style="{ borderTopStyle: DETAIL_TYPE_LINE_STYLES[i % DETAIL_TYPE_LINE_STYLES.length] }"
                    ></i>
                    <em>{{ label }}</em>
                  </span>
                </div>
                <div class="detail-legend__row">
                  <span class="detail-legend__item">
                    <i
                      class="detail-legend__dot"
                      :style="{
                        background: DETAIL_CATEGORY_COLORS.household,
                        boxShadow: `0 0 6px ${DETAIL_CATEGORY_COLORS.household}`
                      }"
                    ></i>
                    <em>{{ DETAIL_CATEGORY_LABELS.household }}（{{ DETAIL_CATEGORY_UNITS.household }}）</em>
                  </span>
                  <span class="detail-legend__item">
                    <i
                      class="detail-legend__dot"
                      :style="{
                        background: DETAIL_CATEGORY_COLORS.facility,
                        boxShadow: `0 0 6px ${DETAIL_CATEGORY_COLORS.facility}`
                      }"
                    ></i>
                    <em>{{ DETAIL_CATEGORY_LABELS.facility }}（{{ DETAIL_CATEGORY_UNITS.facility }}）</em>
                  </span>
                </div>
              </div>
              <div ref="detailRef" class="chart"></div>
              <p v-if="!detailHasData" class="panel-empty detail-empty">暂无明细数据</p>
            </div>
          </ScreenPanel>

          <ScreenPanel title="各村项目类型分布">
            <div class="bar-stage">
              <div ref="barRef" class="chart"></div>
              <!-- 项目类型颜色标注：竖向排在图表右侧 -->
              <div class="type-legend">
                <span
                  v-for="(item, i) in typeSeriesNames"
                  :key="item.name"
                  class="type-legend-item"
                >
                  <i
                    :style="{
                      background: TYPE_LEGEND_COLORS[i % TYPE_LEGEND_COLORS.length],
                      boxShadow: `0 0 6px ${TYPE_LEGEND_COLORS[i % TYPE_LEGEND_COLORS.length]}`
                    }"
                  ></i>
                  <em>{{ item.name }}</em>
                </span>
              </div>
            </div>
          </ScreenPanel>
        </section>

        <!-- 右栏 -->
        <section class="col col-right">
          <ScreenPanel title="核心指标">
            <div class="metric-grid">
              <div v-for="item in metricItems" :key="item.label" class="metric-item">
                <el-icon class="metric-icon" :style="{ color: item.color }">
                  <component :is="item.icon" />
                </el-icon>
                <div class="metric-text">
                  <span class="metric-label">{{ item.label }}</span>
                  <span class="metric-value" :style="{ color: item.color }">
                    {{ formatNumber(item.value) }}<em>{{ item.unit }}</em>
                  </span>
                </div>
              </div>
            </div>
          </ScreenPanel>

          <ScreenPanel title="项目状态分布">
            <div class="donut-stage">
              <!-- 轨道层：纯 SVG 绘制，三层同心扁椭圆（X 轴拉伸、Y 轴压缩，模拟俯视透视） -->
              <svg class="donut-svg" viewBox="0 0 440 290" preserveAspectRatio="none" aria-hidden="true">
                <defs>
                  <!-- 中心径向蓝光光晕 -->
                  <radialGradient id="dn-halo-grad" cx="50%" cy="50%" r="50%">
                    <stop offset="0%" stop-color="#35d8ff" stop-opacity="0.5" />
                    <stop offset="42%" stop-color="#35d8ff" stop-opacity="0.16" />
                    <stop offset="100%" stop-color="#35d8ff" stop-opacity="0" />
                  </radialGradient>
                  <!-- 内环渐变描边：上暗下亮再回亮，形成流光底色 -->
                  <linearGradient id="dn-inner-grad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#a8f0ff" stop-opacity="0.95" />
                    <stop offset="48%" stop-color="#35a8ff" stop-opacity="0.6" />
                    <stop offset="100%" stop-color="#a8f0ff" stop-opacity="0.95" />
                  </linearGradient>
                  <!-- 中环渐变描边：透明度降低 -->
                  <linearGradient id="dn-mid-grad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#5ec8ff" stop-opacity="0.5" />
                    <stop offset="50%" stop-color="#2f86d8" stop-opacity="0.24" />
                    <stop offset="100%" stop-color="#5ec8ff" stop-opacity="0.5" />
                  </linearGradient>
                </defs>

                <!-- 中心光晕（向上浮起，作为视觉最高点底层） -->
                <ellipse class="dn-halo" cx="220" cy="140" rx="92" ry="54" fill="url(#dn-halo-grad)" />

                <!-- 外层：最淡、虚线弱化 -->
                <ellipse class="dn-orbit dn-orbit-outer" cx="220" cy="145" rx="146" ry="82" />
                <!-- 中层：淡光轨道 -->
                <ellipse class="dn-orbit dn-orbit-mid" cx="220" cy="145" rx="110" ry="62" />
                <!-- 内层：主发光轨道（最亮） -->
                <ellipse class="dn-orbit dn-orbit-inner" cx="220" cy="145" rx="74" ry="42" />
                <!-- 内层流动高光光带 -->
                <ellipse class="dn-flow" cx="220" cy="145" rx="74" ry="42" />

                <!-- 状态节点光点：有数据亮 + 外发光，数值 0 弱化 -->
                <g v-for="item in donutNodes" :key="item.name">
                  <circle
                    class="dn-dot-halo"
                    :cx="item.x"
                    :cy="item.y"
                    r="7.5"
                    :fill="item.color"
                    :opacity="item.value ? 0.3 : 0.08"
                  />
                  <circle
                    class="dn-dot-core"
                    :cx="item.x"
                    :cy="item.y"
                    r="3.6"
                    :fill="item.color"
                    :opacity="item.value ? 1 : 0.35"
                    :style="item.value ? { filter: `drop-shadow(0 0 5px ${item.color})` } : undefined"
                  />
                </g>
              </svg>

              <!-- 中心：大号高亮数字 + 副标题 -->
              <div class="dn-center">
                <b>{{ donutTotal }}</b>
                <span>项目总数</span>
              </div>

              <!-- 状态文字标签：保持正向可读，位于光点外侧，同色系 -->
              <div
                v-for="item in donutNodes"
                :key="`label-${item.name}`"
                class="dn-label"
                :class="{ 'is-left': item.left, 'is-zero': !item.value }"
                :style="{ left: `${item.labelX}%`, top: `${item.labelY}%` }"
              >
                <span class="dn-label-name" :style="{ color: item.color }">{{ item.name }}</span>
                <b class="dn-label-value" :style="{ color: item.color }">{{ item.value }}</b>
              </div>
            </div>
          </ScreenPanel>

          <ScreenPanel title="实时事件" badge="STREAM">
            <div v-if="eventStream.length" class="event-viewport">
              <div class="event-track" :class="{ scrolling: eventScrollable }" :style="eventScrollStyle">
                <ul v-for="copy in eventScrollable ? 2 : 1" :key="copy" class="event-list" :aria-hidden="copy === 2">
                  <li v-for="item in eventStream" :key="item.id" class="event-item">
                    <i class="event-dot" :style="{ background: eventDotColor(item.category), boxShadow: `0 0 8px ${eventDotColor(item.category)}` }"></i>
                    <div class="event-main">
                      <span class="event-text" :title="item.summary || item.operation">{{ item.summary || item.operation }}</span>
                      <span class="event-meta">{{ item.username || '系统' }}{{ item.module ? ` · ${item.module}` : '' }}</span>
                    </div>
                    <span class="event-time">{{ formatRelativeTime(item.operTime) }}</span>
                  </li>
                </ul>
              </div>
            </div>
            <p v-else class="panel-empty">暂无业务事件</p>
          </ScreenPanel>
        </section>
      </main>

      <!-- ========== 底部实时数据流 ========== -->
      <footer class="screen-ticker">
        <span class="ticker-label">综合数据流</span>
        <div class="ticker-viewport">
          <div class="ticker-track">
            <span v-for="(item, index) in tickerItems" :key="`a-${index}`" class="ticker-item">
              <em></em>{{ item.label }}<b>{{ item.value }}</b>
            </span>
            <span v-for="(item, index) in tickerItems" :key="`b-${index}`" class="ticker-item">
              <em></em>{{ item.label }}<b>{{ item.value }}</b>
            </span>
          </div>
        </div>
        <span class="ticker-update">更新 {{ overview?.updateTime?.slice(11) || '--:--:--' }}</span>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import ScreenPanel from '@/components/ScreenPanel.vue'
import { getScreenOverview } from '@/api/dashboard'
import type {
  ScreenChartItemVO,
  ScreenEventVO,
  ScreenMaterialVO,
  ScreenOverviewVO
} from '@/api/dashboard'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

/** 是否投屏独立页（全屏展示，不显示投屏按钮） */
const standalone = computed(() => route.meta.standalone === true)

/** 设计稿宽度：整体按屏幕宽度等比缩放，高度按屏幕比例自动延展 */
const DESIGN_WIDTH = 1920
/** 设计稿基准高度 */
const DESIGN_HEIGHT = 1080
/** 数据自动刷新间隔（毫秒） */
const REFRESH_INTERVAL = 60 * 1000
/** 各村项目分布中心点（百分比坐标） */
const MAP_CENTER = { x: 48, y: 50 }
/** 各村项目分布节点固定布局（百分比坐标，避免每次渲染位置抖动） */
const MAP_POSITIONS = [
  { x: 24, y: 32 },
  { x: 62, y: 24 },
  { x: 78, y: 48 },
  { x: 60, y: 70 },
  { x: 30, y: 66 },
  { x: 44, y: 46 },
  { x: 16, y: 52 },
  { x: 74, y: 76 }
]
/** 各村光点配色（RGB 三元组，按节点顺序循环取用，保证各村颜色互不相同） */
const MAP_NODE_COLORS = [
  '38, 224, 164',
  '96, 200, 255',
  '255, 200, 92',
  '255, 128, 158',
  '180, 140, 255',
  '120, 240, 220',
  '255, 160, 100',
  '140, 240, 120'
]
/** 项目状态配色 */
const STATUS_COLORS: Record<string, string> = {
  待审核: '#35d8ff',
  待开工: '#7a8fa6',
  施工中: '#23e6a0',
  待验收: '#ffc857',
  已归档: '#a37bff',
  已驳回: '#ff6b81'
}

const loading = ref(false)
const overview = ref<ScreenOverviewVO | null>(null)
const scale = ref(1)
/** 画布设计高度：随容器高度/缩放比动态计算，保证缩放后正好铺满容器 */
const designHeight = ref(DESIGN_HEIGHT)
const dateText = ref('')
const timeText = ref('')
/** 大屏容器：内嵌模式下按内容区尺寸等比缩放 */
const rootRef = ref<HTMLDivElement>()

const fundGaugeRef = ref<HTMLDivElement>()
const detailRef = ref<HTMLDivElement>()
const barRef = ref<HTMLDivElement>()

let fundGaugeChart: ReturnType<typeof echarts.init> | null = null
let detailChart: ReturnType<typeof echarts.init> | null = null
let barChart: ReturnType<typeof echarts.init> | null = null
let clockTimer: number | undefined
let refreshTimer: number | undefined
let resizeObserver: ResizeObserver | null = null

/** 区划名称（沿用登录用户所属区划） */
const regionText = computed(
  () => store.userInfo?.regionName || store.userInfo?.regionCode || '全域视图'
)

/** 核心指标（空数据兜底，避免模板取值报错） */
const kpi = computed(
  () =>
    overview.value?.kpi ?? {
      totalBudget: 0,
      paidAmount: 0,
      payRate: 0,
      projectCount: 0,
      buildingCount: 0,
      constructionCount: 0,
      villageCount: 0,
      householdCount: 0,
      facilityCount: 0,
      materialTotal: 0,
      materialWarnCount: 0,
      declarationCount: 0
    }
)

/** 项目状态分布 */
const statusStats = computed<ScreenChartItemVO[]>(() => overview.value?.statusStats ?? [])
/** 物资状态 */
const materialStats = computed<ScreenMaterialVO[]>(() => overview.value?.materialStats ?? [])
/** 各村项目类型明细统计（横轴村庄，系列为 项目类型×农户/公建；仅立项已通过） */
const villageTypeDetailStats = computed(() => overview.value?.villageTypeDetailStats)
/** 明细图是否有数据（至少一个村） */
const detailHasData = computed(() => !!villageTypeDetailStats.value?.villages?.length)
/** 明细图：项目类型线型（按字典序：常规项目→实线 / 应急抢险→虚线 / 灾后重建→点线） */
const DETAIL_TYPE_LINE_STYLES = ['solid', 'dashed', 'dotted'] as const
/** 明细图：明细类别 → 线条颜色（农户=绿，公建=青） */
const DETAIL_CATEGORY_COLORS: Record<'household' | 'facility', string> = {
  household: '#23e6a0',
  facility: '#35d8ff'
}
/** 明细图：明细类别 → 数值单位（农户=户，公建=处） */
const DETAIL_CATEGORY_UNITS: Record<'household' | 'facility', string> = {
  household: '户',
  facility: '处'
}
/** 明细图：明细类别 → 中文名 */
const DETAIL_CATEGORY_LABELS: Record<'household' | 'facility', string> = {
  household: '农户',
  facility: '公建'
}
/** 明细图：项目类型名称列表（从系列按固定顺序提取：每 2 条取农户系列「·」前半段） */
const detailTypeLabels = computed<string[]>(() => {
  const s = villageTypeDetailStats.value?.series ?? []
  const labels: string[] = []
  for (let i = 0; i < s.length; i += 2) {
    labels.push(s[i].name.split('·')[0])
  }
  return labels
})

/** 物资状态面板可视条数 */
const MATERIAL_VISIBLE_COUNT = 4
/** 物资条数超过可视条数时开启自动滚动 */
const materialScrollable = computed(() => materialStats.value.length > MATERIAL_VISIBLE_COUNT)
/** 滚动周期：按物资条数递增，保证滚动速度稳定 */
const materialScrollStyle = computed(() => ({
  '--scroll-duration': `${Math.max(materialStats.value.length * 2.4, 10)}s`
}))
/** ========== 实时事件（真实业务写操作流） ========== */
/** 事件流本地缓存上限：轮询时新事件前置、旧事件截断，保留流式观感且避免闪烁 */
const EVENT_STREAM_MAX = 12
/** 事件面板可视条数：超过则开启无缝向上滚动 */
const EVENT_VISIBLE_COUNT = 6
/** 事件类别 → 彩色圆点颜色映射（与后端 category 对齐） */
const EVENT_DOT_COLORS: Record<string, string> = {
  declaration: '#5ad8ff',
  approval: '#ffcf5a',
  construction: '#4ade80',
  acceptance: '#a78bfa',
  fund: '#f472b6',
  material: '#fbbf24',
  archive: '#c084fc',
  base: '#38bdf8',
  file: '#22d3ee',
  system: '#94a3b8'
}
const EVENT_DOT_DEFAULT = '#60b8f4'
/** 事件流：跨轮询去重合并，新事件前置 */
const eventStream = ref<ScreenEventVO[]>([])
/** 合并新拉取的事件：按 id 去重，新事件前置，按时间倒序，截断到上限 */
function mergeEvents(incoming: ScreenEventVO[]) {
  if (!incoming?.length) return
  const seen = new Set(eventStream.value.map((e) => e.id))
  const fresh = incoming.filter((e) => e.id && !seen.has(e.id))
  const merged = [...fresh, ...eventStream.value]
  merged.sort((a, b) => (a.operTime < b.operTime ? 1 : -1))
  eventStream.value = merged.slice(0, EVENT_STREAM_MAX)
}
/** 相对时间：刚刚 / X分钟前 / X小时前 / X天前，更早回退 MM-dd HH:mm */
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
/** 取事件圆点颜色 */
function eventDotColor(category: string): string {
  return EVENT_DOT_COLORS[category] || EVENT_DOT_DEFAULT
}
/** 事件条数超过可视条数时开启自动滚动 */
const eventScrollable = computed(() => eventStream.value.length > EVENT_VISIBLE_COUNT)
/** 滚动周期：按事件条数递增，保证滚动速度稳定 */
const eventScrollStyle = computed(() => ({
  '--event-scroll-duration': `${Math.max(eventStream.value.length * 2.2, 12)}s`
}))

/** 各村项目分布节点：位置固定、大小按项目数映射 */
const mapNodes = computed(() => {
  const villages = overview.value?.villageStats ?? []
  return villages.map((village, index) => {
    const position = MAP_POSITIONS[index % MAP_POSITIONS.length]
    return {
      ...village,
      x: position.x,
      y: position.y,
      size: 10 + Math.min(village.projectCount, 6) * 3,
      color: MAP_NODE_COLORS[index % MAP_NODE_COLORS.length]
    }
  })
})

/** 中栏四张 KPI 卡片：主数值 + 主标题 */
const kpiCards = computed(() => {
  const budget = moneyParts(kpi.value.totalBudget)
  const paid = moneyParts(kpi.value.paidAmount)
  return [
    {
      label: '项目总预算',
      value: budget.value,
      unit: budget.unit,
      tone: 'cyan'
    },
    {
      label: '已拨付资金',
      value: paid.value,
      unit: paid.unit,
      tone: 'green'
    },
    {
      label: '拨付完成率',
      value: kpi.value.payRate.toFixed(1),
      unit: '%',
      tone: 'blue'
    },
    {
      label: '在建项目',
      value: String(kpi.value.constructionCount),
      unit: '个',
      tone: 'purple'
    }
  ]
})

/** 右栏核心指标 */
const metricItems = computed(() => [
  { label: '农户档案', value: kpi.value.householdCount, unit: '户', icon: 'House', color: '#35d8ff' },
  { label: '在管项目', value: kpi.value.buildingCount, unit: '个', icon: 'SetUp', color: '#23e6a0' },
  { label: '物资预警', value: kpi.value.materialWarnCount, unit: '项', icon: 'Warning', color: '#ff6b81' },
  { label: '申报受理', value: kpi.value.declarationCount, unit: '件', icon: 'Document', color: '#ffc857' }
])

/** 底部实时数据流 */
const tickerItems = computed(() => {
  const item = kpi.value
  return [
    { label: '立项项目', value: `${item.projectCount} 个` },
    { label: '项目总预算', value: formatMoney(item.totalBudget) },
    { label: '已拨付资金', value: formatMoney(item.paidAmount) },
    { label: '拨付完成率', value: `${item.payRate.toFixed(1)}%` },
    { label: '行政村', value: `${item.villageCount} 个` },
    { label: '农户档案', value: `${item.householdCount} 户` },
    { label: '公共设施', value: `${item.facilityCount} 处` },
    { label: '物资预警', value: `${item.materialWarnCount} 项` }
  ]
})

/** 画布样式：宽度固定设计稿、高度按屏幕比例延展，再整体按屏幕宽度等比缩放铺满 */
const canvasStyle = computed(() => ({
  width: `${DESIGN_WIDTH}px`,
  height: `${designHeight.value}px`,
  transform: `scale(${scale.value})`
}))

/** 项目状态颜色 */
function statusColor(name: string) {
  return STATUS_COLORS[name] ?? '#35d8ff'
}

/** 数字千分位 */
function formatNumber(value: number) {
  return (value ?? 0).toLocaleString('zh-CN')
}

/** 金额拆分：亿/万/元，返回数值与单位用于 KPI 展示 */
function moneyParts(value: number): { value: string; unit: string } {
  const amount = value ?? 0
  if (amount >= 100000000) return { value: (amount / 100000000).toFixed(2), unit: '亿' }
  if (amount >= 10000) return { value: (amount / 10000).toFixed(1), unit: '万' }
  return { value: amount.toFixed(0), unit: '元' }
}

/** 金额格式化 */
function formatMoney(value: number) {
  const parts = moneyParts(value)
  return `${parts.value}${parts.unit}`
}

/** 尺寸自适应：按容器（内嵌=内容区，投屏=窗口）宽度等比缩放，高度延展后铺满整个可视区 */
async function resizeCanvas() {
  const width = rootRef.value?.clientWidth || window.innerWidth
  const height = rootRef.value?.clientHeight || window.innerHeight
  // 以宽度为基准整体缩放，保证字号、图表、间距同步放大，不出现两侧留白
  scale.value = width > 0 ? width / DESIGN_WIDTH : 1
  // 反推设计稿高度：缩放后正好等于容器高度，铺满上下区域
  designHeight.value = height > 0 ? Math.round(height / scale.value) : DESIGN_HEIGHT
  // 画布高度变化后图表容器高度同步变化，需要重算图表尺寸
  await nextTick()
  resizeCharts()
}

/** 重算图表尺寸 */
function resizeCharts() {
  ;[fundGaugeChart, detailChart, barChart].forEach((chart) => chart?.resize())
}

/** 顶部时钟 */
function updateClock() {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  dateText.value = `${now.getFullYear()}.${pad(now.getMonth() + 1)}.${pad(now.getDate())}`
  timeText.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

/** 仪表盘配置：双层发光弧 + 立体刻度 + 动态光晕 */
function buildGaugeOption(value: number) {
  const pct = Math.max(0, Math.min(100, value)) / 100
  return {
    backgroundColor: 'transparent',
    series: [
      // 外层装饰环（旋转刻度感）
      {
        type: 'gauge' as const,
        startAngle: 210,
        endAngle: -30,
        radius: '100%',
        center: ['50%', '58%'],
        min: 0,
        max: 100,
        axisLine: { show: false },
        progress: { show: false },
        pointer: { show: false },
        axisTick: {
          show: true,
          distance: 8,
          splitNumber: 10,
          length: 5,
          lineStyle: { color: 'rgba(100, 200, 255, 0.55)', width: 1 }
        },
        splitLine: { show: false },
        axisLabel: { show: false },
        anchor: { show: false },
        title: { show: false },
        detail: { show: false },
        data: [{ value: pct * 100 }]
      },
      // 主进度仪表盘（高亮 + 发光）
      {
        type: 'gauge' as const,
        startAngle: 210,
        endAngle: -30,
        radius: '88%',
        center: ['50%', '58%'],
        min: 0,
        max: 100,
        axisLine: {
          lineStyle: {
            width: 14,
            color: [[1, 'rgba(38, 224, 164, 0.08)'] as [number, string]]
          }
        },
        progress: {
          show: true,
          width: 14,
          roundCap: true,
          itemStyle: {
            shadowBlur: 28,
            shadowColor: 'rgba(38, 224, 164, 0.75)',
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 1,
              y2: 0,
              colorStops: [
                { offset: 0, color: '#0a6b4c' },
                { offset: 0.5, color: '#23e6a0' },
                { offset: 1, color: '#7dffd0' }
              ]
            }
          }
        },
        pointer: { show: false },
        axisTick: {
          show: true,
          distance: -20,
          length: 6,
          splitNumber: 5,
          lineStyle: { color: 'rgba(120, 200, 255, 0.6)', width: 1.2 }
        },
        splitLine: {
          show: true,
          distance: -24,
          length: 12,
          splitNumber: 5,
          lineStyle: { color: 'rgba(120, 200, 255, 0.85)', width: 1.8 }
        },
        axisLabel: { show: false },
        anchor: { show: false },
        title: { show: false },
        detail: { show: false },
        data: [{ value: pct * 100 }]
      },
      // 内层发光底弧（增强层次）
      {
        type: 'gauge' as const,
        startAngle: 210,
        endAngle: -30,
        radius: '76%',
        center: ['50%', '58%'],
        min: 0,
        max: 100,
        axisLine: {
          lineStyle: {
            width: 4,
            color: [[1, 'rgba(53, 216, 255, 0.18)'] as [number, string]]
          }
        },
        progress: {
          show: true,
          width: 4,
          roundCap: true,
          itemStyle: {
            shadowBlur: 18,
            shadowColor: 'rgba(53, 216, 255, 0.7)',
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: 'rgba(28, 134, 255, 0.9)' },
              { offset: 1, color: '#35d8ff' }
            ])
          }
        },
        pointer: { show: false },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        anchor: { show: false },
        title: { show: false },
        detail: { show: false },
        data: [{ value: pct * 100 }]
      }
    ]
  }
}

/**
 * 各村项目类型明细统计：每个项目类型拆为「农户(户)」与「公建(处)」两条折线，共 6 条。
 * 颜色按明细类别（农户绿 / 公建青），线型按项目类型（实 / 虚 / 点），
 * 图例 / 线条 / tooltip 三者共用 DETAIL_CATEGORY_* 与 DETAIL_TYPE_LINE_STYLES 常量，保证映射一致。
 * 维度：横轴=村，系列=项目类型×农户/公建，数值=明细数（仅立项已通过）。
 */
function buildDetailOption() {
  const stats = villageTypeDetailStats.value
  const villages = stats?.villages ?? []
  const typeSeries = stats?.series ?? []
  // seriesName → 单位，供 tooltip 按系列拼接正确单位（农户=户，公建=处）
  const unitMap = new Map<string, string>()
  typeSeries.forEach((s) => unitMap.set(s.name, DETAIL_CATEGORY_UNITS[s.category]))

  return {
    backgroundColor: 'transparent',
    grid: { top: 64, left: 44, right: 22, bottom: 32 },
    tooltip: {
      trigger: 'axis' as const,
      backgroundColor: 'rgba(6, 24, 44, 0.96)',
      borderColor: 'rgba(53, 216, 255, 0.45)',
      borderWidth: 1,
      textStyle: { color: '#eaf7ff', fontSize: 12 },
      formatter: (
        params: { axisValue: string; marker: string; seriesName: string; value: number }[]
      ) => {
        if (!params || !params.length) return ''
        const rows = params
          .map((p) => `${p.marker}${p.seriesName}：${p.value}${unitMap.get(p.seriesName) ?? ''}`)
          .join('<br/>')
        return `${params[0].axisValue}<br/>${rows}`
      },
      extraCssText: 'box-shadow: 0 0 20px rgba(53, 216, 255, 0.35);'
    },
    legend: { show: false },
    xAxis: {
      type: 'category' as const,
      boundaryGap: false,
      data: villages,
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#bfe9ff', fontSize: 11, fontWeight: 500 },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value' as const,
      minInterval: 1,
      axisLabel: { color: '#7fa8c6', fontSize: 10 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: {
        show: true,
        lineStyle: { color: 'rgba(84, 150, 200, 0.14)', type: 'dashed' as const }
      }
    },
    series: typeSeries.map((s, i) => {
      const color = DETAIL_CATEGORY_COLORS[s.category]
      const unit = DETAIL_CATEGORY_UNITS[s.category]
      // 系列顺序固定为 类型1农户,类型1公建,类型2农户,类型2公建... → 每 2 条同属一个项目类型
      const typeIndex = Math.floor(i / 2)
      const lineType = DETAIL_TYPE_LINE_STYLES[typeIndex % DETAIL_TYPE_LINE_STYLES.length]
      return {
        name: s.name,
        type: 'line' as const,
        smooth: true,
        showSymbol: true,
        symbolSize: 7,
        z: 4,
        lineStyle: {
          width: 2.5,
          type: lineType,
          color,
          shadowBlur: 12,
          shadowColor: hexToRgba(color, 0.6)
        },
        itemStyle: {
          color,
          borderColor: '#ffffff',
          borderWidth: 1,
          shadowBlur: 8,
          shadowColor: hexToRgba(color, 0.75)
        },
        areaStyle: {
          origin: 'start' as const,
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: hexToRgba(color, 0.18) },
              { offset: 1, color: hexToRgba(color, 0) }
            ]
          }
        },
        // 数值标注：非 0 才显示，带对应单位
        label: {
          show: true,
          position: 'top',
          color,
          fontSize: 10,
          fontWeight: 600,
          formatter: (params: { value: number }) =>
            params.value > 0 ? `${params.value}${unit}` : ''
        },
        data: s.data
      }
    })
  }
}

/** 各村项目类型分布：类型配色（右侧 HTML 图例与柱体分段共用） */
const TYPE_LEGEND_COLORS = ['#00fff7', '#ffc857', '#b07bff']

/** 各村项目类型分布：类型系列列表（用于渲染右侧颜色标注） */
const typeSeriesNames = computed(() => overview.value?.typeStats?.series ?? [])

/** 十六进制颜色转 rgba 字符串 */
function hexToRgba(hex: string, alpha: number): string {
  const c = hex.replace('#', '')
  const num = parseInt(
    c.length === 3 ? c.split('').map((ch) => ch + ch).join('') : c,
    16
  )
  return `rgba(${(num >> 16) & 0xff}, ${(num >> 8) & 0xff}, ${num & 0xff}, ${alpha})`
}

/** custom 系列渲染函数入参（ECharts 类型过于复杂，此处只约束本图表用到的能力） */
interface BarRenderAPI {
  /** 取当前数据项指定维度的值 */
  value: (dim: number) => number
  /** 数据坐标 → 像素坐标 */
  coord: (data: number[]) => number[]
}

/** 各村项目类型分布：立体玻璃柱 + 发光底座 + 数据虚影 + 透视轨道 */
/** 各村项目类型分布：透明玻璃罩 + 水晶柱 + 多层光环底座 + 透视网格地面 */
function buildBarOption() {
  const stats = overview.value?.typeStats
  const villages = stats?.villages ?? []
  const series = stats?.series ?? []
  // 类型配色与右侧 HTML 图例共用同一常量
  const typeColors = TYPE_LEGEND_COLORS
  const maxVal = Math.max(1, ...series.flatMap((s) => s.data.map((v) => v)))
  const cols = Math.max(1, villages.length)
  // 玻璃罩宽度随类目数自适应
  const glassW = cols <= 4 ? 78 : cols <= 7 ? 58 : 44

  // 每个村的分段数据与总高度
  const segs = villages.map((_, vi) =>
    series.map((s, si) => ({ value: s.data[vi] ?? 0, color: typeColors[si % typeColors.length] }))
  )
  const totals = segs.map((list) => list.reduce((sum, seg) => sum + seg.value, 0))
  // 柱组配色取本柱最上面一段有值的类型
  const topColors = segs.map((list) => {
    let color = typeColors[0]
    list.forEach((seg) => {
      if (seg.value > 0) color = seg.color
    })
    return color
  })

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item' as const,
      backgroundColor: 'rgba(5, 16, 36, 0.96)',
      borderColor: 'rgba(0, 255, 247, 0.5)',
      borderWidth: 1,
      textStyle: { color: '#eaf7ff', fontSize: 12 },
      extraCssText: 'box-shadow: 0 0 22px rgba(0, 255, 247, 0.45);'
    },
    legend: {
      // 图例已改为右侧 HTML 标注，这里关闭 ECharts 内置图例
      show: false
    },
    grid: { top: 34, left: 24, right: 96, bottom: 62 },
    xAxis: {
      type: 'category' as const,
      data: villages,
      boundaryGap: true,
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: {
        color: '#bfe9ff',
        fontSize: 12,
        fontWeight: 600,
        margin: 26
      },
      splitLine: { show: false },
      axisPointer: { show: false }
    },
    yAxis: {
      type: 'value' as const,
      min: 0,
      max: maxVal * 1.18,
      axisLabel: { show: false },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { show: false }
    },
    series: [
      // ① 透视网格地面：地平线 + 纵向消失线 + 横向网格
      {
        type: 'custom' as const,
        silent: true,
        clip: false,
        z: 0,
        data: [[0, 0]],
        renderItem: (_params: unknown, api: BarRenderAPI) => {
          const left = api.coord([0, 0])
          const right = api.coord([cols - 1, 0])
          const gy = left[1]
          const xL = left[0] - glassW * 0.95
          const xR = right[0] + glassW * 0.95
          const depth = 22
          const children: Record<string, unknown>[] = [
            // 地平线：贯穿全部柱组的发光横线
            {
              type: 'line',
              shape: { x1: xL, y1: gy, x2: xR, y2: gy },
              style: {
                stroke: 'rgba(0, 255, 247, 0.8)',
                lineWidth: 2,
                shadowBlur: 18,
                shadowColor: 'rgba(0, 255, 247, 0.95)'
              }
            }
          ]
          // 纵向消失线：从地平线向外下方发散，形成透视网格
          const N = 10
          for (let i = 0; i <= N; i++) {
            const t = i / N
            const xt = xL + (xR - xL) * t
            const spread = (t - 0.5) * 2
            children.push({
              type: 'line',
              shape: { x1: xt, y1: gy, x2: xt + spread * depth * 1.15, y2: gy + depth },
              style: {
                stroke: `rgba(0, 255, 247, ${0.22 - Math.abs(spread) * 0.08})`,
                lineWidth: 1
              }
            })
          }
          // 横向透视网格：近疏远密、越远越淡
          for (let i = 1; i <= 3; i++) {
            const t = i / 3
            const yy = gy + depth * t
            const sp = depth * 1.15 * t
            children.push({
              type: 'line',
              shape: { x1: xL - sp, y1: yy, x2: xR + sp, y2: yy },
              style: { stroke: `rgba(0, 255, 247, ${0.18 - t * 0.11})`, lineWidth: 1 }
            })
          }
          return { type: 'group', children }
        }
      },

      // ② 柱组：虚影 → 多层光环底座 → 玻璃罩 → 水晶柱 → 罩体轮廓 → 数值
      {
        type: 'custom' as const,
        z: 3,
        clip: false,
        data: villages.map((_, vi) => [vi, totals[vi]]),
        tooltip: {
          formatter: (p: { dataIndex: number }) => {
            const vi = p.dataIndex
            const rows = series
              .filter((s) => (s.data[vi] ?? 0) > 0)
              .map((s) => `${s.name}：<b>${s.data[vi]}</b> 个`)
              .join('<br/>')
            return `<b>${villages[vi]}</b><br/>${rows || '暂无数据'}<br/><span style="color:#7fa8c6">合计：${totals[vi]} 个</span>`
          }
        },
        renderItem: (_params: unknown, api: BarRenderAPI) => {
          const vi = api.value(0)
          const total = api.value(1)
          const topColor = topColors[vi] ?? typeColors[0]
          const basePt = api.coord([vi, 0])
          const cx = basePt[0]
          const baseY = basePt[1]
          // 可用高度：网格顶部到 0 线
          const gridTopY = api.coord([vi, maxVal * 1.18])[1]
          const availH = Math.max(60, baseY - gridTopY)
          const ratio = maxVal > 0 ? total / maxVal : 0
          // 水晶柱高度：按占比缩放，同时保留基础视觉高度
          const crystalH = availH * 0.42 * (0.34 + 0.66 * ratio)
          const crystalTopY = baseY - crystalH
          // 玻璃罩比水晶柱高近一倍
          const glassH = crystalH * 2 + 10
          const glassTopY = baseY - glassH
          const gRx = glassW / 2
          const gRy = glassW * 0.085
          const cw = glassW * 0.46
          const cRy = cw * 0.2
          const children: Record<string, unknown>[] = []

          // 数据虚影：罩体后方更大更淡的残影
          children.push({
            type: 'rect',
            shape: { x: cx - gRx * 0.9 + 10, y: glassTopY + 6, width: glassW * 0.9, height: glassH },
            style: {
              fill: hexToRgba(topColor, 0.07),
              shadowBlur: 34,
              shadowColor: hexToRgba(topColor, 0.28)
            }
          })

          // 多层光环底座：外环 + 虚线中环 + 内环 + 中心光台
          const rings = [
            { k: 1.0, w: 1.8, a: 0.62, dash: undefined },
            { k: 0.78, w: 1.1, a: 0.4, dash: [4, 5] },
            { k: 0.56, w: 1, a: 0.26, dash: undefined }
          ]
          rings.forEach((ring) => {
            children.push({
              type: 'ellipse',
              shape: { cx, cy: baseY, rx: gRx * ring.k, ry: gRy * ring.k },
              style: {
                fill: 'transparent',
                stroke: hexToRgba('#00fff7', ring.a),
                lineWidth: ring.w,
                lineDash: ring.dash,
                shadowBlur: 14,
                shadowColor: hexToRgba('#00fff7', ring.a * 0.85)
              }
            })
          })
          children.push({
            type: 'ellipse',
            shape: { cx, cy: baseY, rx: gRx * 0.94, ry: gRy * 0.94 },
            style: {
              fill: new echarts.graphic.RadialGradient(0.5, 0.5, 0.5, [
                { offset: 0, color: hexToRgba(topColor, 0.52) },
                { offset: 0.6, color: hexToRgba(topColor, 0.16) },
                { offset: 1, color: hexToRgba(topColor, 0) }
              ])
            }
          })

          // 玻璃罩填充：顶部略亮、向下渐隐
          children.push({
            type: 'rect',
            shape: { x: cx - gRx, y: glassTopY, width: glassW, height: glassH },
            style: {
              fill: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(125, 251, 255, 0.16)' },
                { offset: 0.5, color: 'rgba(0, 255, 247, 0.05)' },
                { offset: 1, color: 'rgba(0, 255, 247, 0.02)' }
              ])
            }
          })

          // 水晶柱主体：按项目类型从下往上分段堆叠，每段用各自的类型色
          if (total > 0) {
            const filledSegs = (segs[vi] ?? []).filter((seg) => seg.value > 0)
            let accVal = 0
            filledSegs.forEach((seg, si) => {
              const yBottom = baseY - (crystalH * accVal) / total
              const yTopSeg = baseY - (crystalH * (accVal + seg.value)) / total
              accVal += seg.value
              children.push({
                type: 'rect',
                shape: {
                  x: cx - cw / 2,
                  y: yTopSeg,
                  width: cw,
                  height: Math.max(1, yBottom - yTopSeg)
                },
                style: {
                  fill: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: hexToRgba(seg.color, 0.3) },
                    { offset: 0.4, color: hexToRgba(seg.color, 0.74) },
                    { offset: 0.54, color: 'rgba(255, 255, 255, 0.85)' },
                    { offset: 0.68, color: hexToRgba(seg.color, 0.72) },
                    { offset: 1, color: hexToRgba(seg.color, 0.28) }
                  ]),
                  shadowBlur: 18,
                  shadowColor: hexToRgba(seg.color, 0.7)
                }
              })
              // 段间截面：亮色椭圆分隔，强化水晶分节质感
              if (si < filledSegs.length - 1) {
                children.push({
                  type: 'ellipse',
                  shape: { cx, cy: yTopSeg, rx: cw / 2, ry: cRy },
                  style: {
                    fill: hexToRgba(seg.color, 0.6),
                    stroke: 'rgba(255, 255, 255, 0.55)',
                    lineWidth: 0.8
                  }
                })
              }
            })
            // 水晶柱底面
            children.push({
              type: 'ellipse',
              shape: { cx, cy: baseY, rx: cw / 2, ry: cRy },
              style: { fill: hexToRgba(topColor, 0.5) }
            })
            // 水晶柱顶面：取最上段类型色的亮白高光
            children.push({
              type: 'ellipse',
              shape: { cx, cy: crystalTopY, rx: cw / 2, ry: cRy },
              style: {
                fill: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(255, 255, 255, 0.95)' },
                  { offset: 0.5, color: hexToRgba(topColor, 0.88) },
                  { offset: 1, color: hexToRgba(topColor, 0.7) }
                ]),
                shadowBlur: 16,
                shadowColor: hexToRgba(topColor, 0.95)
              }
            })
          }

          // 罩体轮廓：左右竖边 + 顶/底椭圆，营造透明圆柱
          children.push({
            type: 'line',
            shape: { x1: cx - gRx, y1: glassTopY, x2: cx - gRx, y2: baseY },
            style: { stroke: 'rgba(125, 251, 255, 0.4)', lineWidth: 1 }
          })
          children.push({
            type: 'line',
            shape: { x1: cx + gRx, y1: glassTopY, x2: cx + gRx, y2: baseY },
            style: { stroke: 'rgba(125, 251, 255, 0.4)', lineWidth: 1 }
          })
          children.push({
            type: 'ellipse',
            shape: { cx, cy: glassTopY, rx: gRx, ry: gRy },
            style: {
              fill: 'transparent',
              stroke: 'rgba(125, 251, 255, 0.55)',
              lineWidth: 1.2,
              shadowBlur: 10,
              shadowColor: 'rgba(125, 251, 255, 0.7)'
            }
          })
          children.push({
            type: 'ellipse',
            shape: { cx, cy: baseY, rx: gRx, ry: gRy },
            style: { fill: 'transparent', stroke: 'rgba(125, 251, 255, 0.2)', lineWidth: 1 }
          })

          // 数值：固定在罩体顶部上方，亮白发光
          if (total > 0) {
            children.push({
              type: 'text',
              style: {
                text: String(total),
                x: cx,
                y: glassTopY - 6,
                textAlign: 'center',
                textVerticalAlign: 'bottom',
                fill: '#ffffff',
                fontSize: 14,
                fontWeight: 'bold',
                textShadowBlur: 12,
                textShadowColor: 'rgba(0, 255, 247, 0.95)'
              }
            })
          }

          return { type: 'group', children }
        }
      }
    ]
  }
}

/** 状态节点角度：0°=正右、逆时针为正，6 个节点环绕分布并避开正上(90°)与正下(270°) */
const DONUT_NODE_ANGLES = [60, 120, 0, 180, 240, 300]
/** SVG viewBox 尺寸与中心点（必须与模板 viewBox="0 0 440 290" 保持一致） */
const DONUT_VIEW_W = 440
const DONUT_VIEW_H = 290
const DONUT_CX = 220
const DONUT_CY = 145
/** 节点所在椭圆半径：X 轴拉伸、Y 轴压缩，形成俯视透视 */
const DONUT_NODE_RX = 116
const DONUT_NODE_RY = 66
/** 标签沿径向向外偏移量，保证文字落在光点外侧 */
const DONUT_LABEL_GAP = 22

/** 项目总数：中心大号高亮数字 */
const donutTotal = computed(() =>
  statusStats.value.reduce((sum, item) => sum + item.value, 0)
)

/** 状态节点：按角度在扁椭圆上算坐标，并换算成容器百分比供 HTML 标签绝对定位 */
const donutNodes = computed(() =>
  statusStats.value.map((item, i) => {
    const rad = (DONUT_NODE_ANGLES[i % DONUT_NODE_ANGLES.length] * Math.PI) / 180
    const cos = Math.cos(rad)
    const sin = Math.sin(rad)
    // 椭圆坐标：SVG 的 Y 轴向下，纵向取负号
    const x = DONUT_CX + cos * DONUT_NODE_RX
    const y = DONUT_CY - sin * DONUT_NODE_RY
    // 标签沿「中心 → 节点」方向再外推固定距离，避免压在轨道线上
    const dx = x - DONUT_CX
    const dy = y - DONUT_CY
    const len = Math.hypot(dx, dy) || 1
    const labelX = x + (dx / len) * DONUT_LABEL_GAP
    const labelY = y + (dy / len) * DONUT_LABEL_GAP
    return {
      name: item.name,
      value: item.value,
      color: statusColor(item.name),
      x,
      y,
      labelX: (labelX / DONUT_VIEW_W) * 100,
      labelY: (labelY / DONUT_VIEW_H) * 100,
      // 左半区标签反向对齐（文字朝左延伸），右半区正向
      left: cos < 0
    }
  })
)

/** 初始化图表：按缩放比提升像素密度，避免大屏放大后图表模糊 */
function initChart(el: HTMLDivElement) {
  const dpr = Math.min(3, (window.devicePixelRatio || 1) * Math.max(1, scale.value))
  return echarts.init(el, undefined, { devicePixelRatio: dpr })
}

/** 渲染全部图表（首次初始化，刷新时仅更新配置） */
function renderCharts() {
  if (fundGaugeRef.value) {
    fundGaugeChart = fundGaugeChart ?? initChart(fundGaugeRef.value)
    fundGaugeChart.setOption(buildGaugeOption(kpi.value.payRate), true)
  }
  if (detailRef.value) {
    detailChart = detailChart ?? initChart(detailRef.value)
    detailChart.setOption(buildDetailOption(), true)
  }
  if (barRef.value) {
    barChart = barChart ?? initChart(barRef.value)
    barChart.setOption(buildBarOption(), true)
  }
}

/** 加载大屏数据 */
async function loadData() {
  try {
    const res = await getScreenOverview()
    overview.value = res.data
    // 实时事件流：跨轮询去重合并，新事件前置，避免整表替换造成闪烁
    mergeEvents(res.data?.events)
    await nextTick()
    renderCharts()
  } catch {
    // 拦截器已统一提示错误信息
  }
}

/** 投屏：在新窗口打开独立全屏大屏页面 */
function openCast() {
  const { href } = router.resolve({ name: 'DataScreenCast' })
  window.open(href, '_blank', 'noopener')
}

onMounted(async () => {
  // 监听容器尺寸变化（窗口缩放、浏览器缩放），实时重算缩放比例与图表尺寸
  resizeObserver = new ResizeObserver(async () => {
    await resizeCanvas()
  })
  if (rootRef.value) resizeObserver.observe(rootRef.value)
  window.addEventListener('resize', resizeCanvas)
  resizeCanvas()
  updateClock()
  clockTimer = window.setInterval(updateClock, 1000)
  refreshTimer = window.setInterval(loadData, REFRESH_INTERVAL)
  loading.value = true
  await loadData()
  loading.value = false
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  window.removeEventListener('resize', resizeCanvas)
  if (clockTimer) window.clearInterval(clockTimer)
  if (refreshTimer) window.clearInterval(refreshTimer)
  ;[fundGaugeChart, detailChart, barChart].forEach((chart) => chart?.dispose())
  fundGaugeChart = null
  detailChart = null
  barChart = null
})
</script>

<style scoped>
/* ========== 大屏骨架 ========== */
/* 内嵌模式：绝对定位铺满主内容区（含内边距区域），不遮挡侧边栏与顶栏 */
.screen-root {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 50% -10%, rgba(38, 224, 164, 0.08), transparent 55%),
    radial-gradient(ellipse at 50% 110%, rgba(24, 110, 130, 0.1), transparent 60%),
    #030a10;
}

/* 投屏模式：独立整屏，覆盖整个浏览器窗口 */
.screen-root.is-standalone {
  position: fixed;
  inset: 0;
  z-index: 2000;
}

/* 画布：左上角对齐后整体等比缩放，宽高经缩放后正好铺满容器 */
.screen-canvas {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  transform-origin: top left;
  background:
    radial-gradient(ellipse 75% 50% at 50% -8%, rgba(32, 108, 190, 0.42) 0%, rgba(3, 10, 20, 0) 62%),
    radial-gradient(ellipse 60% 42% at 50% 106%, rgba(18, 92, 160, 0.24) 0%, rgba(3, 10, 20, 0) 62%),
    linear-gradient(180deg, #02060e 0%, #071528 46%, #030a14 100%);
  color: #cfe9ff;
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
}

/* 背景网格 + 中心冷光 */
.screen-canvas::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(ellipse 62% 46% at 50% 44%, rgba(46, 130, 210, 0.14) 0%, transparent 70%),
    linear-gradient(rgba(86, 180, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(86, 180, 255, 0.05) 1px, transparent 1px);
  background-size: 100% 100%, 44px 44px, 44px 44px;
  pointer-events: none;
}

/* 底部透视网格：营造空间纵深 */
.screen-canvas::after {
  content: '';
  position: absolute;
  left: -15%;
  right: -15%;
  bottom: -2%;
  height: 34%;
  background-image:
    linear-gradient(rgba(70, 170, 255, 0.22) 1px, transparent 1px),
    linear-gradient(90deg, rgba(70, 170, 255, 0.22) 1px, transparent 1px);
  background-size: 60px 42px;
  transform: perspective(700px) rotateX(66deg);
  transform-origin: bottom center;
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, #000 58%, #000 100%);
  mask-image: linear-gradient(180deg, transparent 0%, #000 58%, #000 100%);
  opacity: 0.35;
  pointer-events: none;
  z-index: 0;
}

/* ========== 顶部标题栏 ========== */
.screen-header {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  height: 96px;
  padding: 0 26px;
  flex-shrink: 0;
}

.screen-header::after {
  content: '';
  position: absolute;
  left: 26px;
  right: 26px;
  bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(38, 224, 164, 0.5), transparent);
}

.header-side {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.header-right {
  justify-content: flex-end;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 11px;
  border: 1px solid rgba(53, 216, 255, 0.28);
  border-radius: 4px;
  background: rgba(11, 46, 80, 0.55);
  font-size: 12px;
  color: #9fd4ef;
  white-space: nowrap;
}

.chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #23e6a0;
  box-shadow: 0 0 8px #23e6a0;
  animation: chip-pulse 1.8s ease-in-out infinite;
}

.header-center {
  text-align: center;
  padding: 0 32px;
}

.header-title {
  margin: 0;
  font-size: 38px;
  font-weight: 700;
  letter-spacing: 8px;
  line-height: 1.15;
  background: linear-gradient(180deg, #ffffff 8%, #a8e9ff 52%, #35b8ff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 26px rgba(53, 216, 255, 0.35);
}

.clock {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.25;
}

.clock-date {
  font-size: 12px;
  color: #8fb8d4;
  letter-spacing: 1px;
}

.clock-time {
  font-size: 22px;
  font-weight: 700;
  color: #eaf7ff;
  letter-spacing: 2px;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 12px rgba(53, 216, 255, 0.45);
}

.exit-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 14px;
  padding: 7px 14px;
  border: 1px solid rgba(38, 224, 164, 0.45);
  border-radius: 4px;
  background: rgba(10, 42, 34, 0.6);
  color: #a8f5dd;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.exit-btn:hover {
  border-color: #26e0a4;
  background: rgba(16, 78, 62, 0.75);
  color: #ffffff;
  box-shadow: 0 0 14px rgba(38, 224, 164, 0.35);
}

/* ========== 主体三栏 ========== */
.screen-body {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 25% 1fr 25%;
  gap: 12px;
  flex: 1;
  min-height: 0;
  padding: 10px 16px;
  box-sizing: border-box;
}

.col {
  display: grid;
  gap: 12px;
  min-height: 0;
}

.col-left {
  grid-template-rows: 1.16fr 1.04fr 1.3fr;
}

.col-center {
  grid-template-rows: 98px 1.32fr 1fr;
}

.col-right {
  grid-template-rows: 166px 1fr 1fr;
}

/* ========== 中栏 KPI：悬浮玻璃胶囊标签 ========== */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

/* KPI 卡片：承载悬浮胶囊、光晕、扫描环与倒影 */
.kpi-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  height: 100%;
  min-width: 0;
  --kpi-accent: 110, 235, 245;
  --kpi-fill: 12, 70, 86;
  --kpi-glow: 0.55;
  --kpi-ring-opacity: 0.6;
}

/* 四卡平行静态分布：同一基线，不做前后错落 */
.kpi-card--back {
  --kpi-glow: 0.55;
  --kpi-ring-opacity: 0.55;
}

.kpi-card--front {
  --kpi-glow: 1;
  --kpi-ring-opacity: 1;
}

.kpi-tone-cyan {
  --kpi-accent: 110, 235, 245;
  --kpi-fill: 12, 70, 86;
}

.kpi-tone-green {
  --kpi-accent: 140, 240, 120;
  --kpi-fill: 24, 74, 40;
}

.kpi-tone-blue {
  --kpi-accent: 115, 180, 255;
  --kpi-fill: 20, 56, 110;
}

.kpi-tone-purple {
  --kpi-accent: 180, 130, 255;
  --kpi-fill: 62, 38, 105;
}

/* 悬浮容器：四卡平行静态排列 */
.kpi-float {
  position: relative;
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  align-items: center;
  width: 100%;
  padding-top: 4px;
}

/* 底部悬浮光晕：宽度约卡片 1.3 倍，蓝青柔光 */
.kpi-halo {
  position: absolute;
  left: 50%;
  bottom: -8px;
  z-index: 0;
  width: 132%;
  height: 54px;
  transform: translateX(-50%);
  background: radial-gradient(
    ellipse at 50% 26%,
    rgba(var(--kpi-accent), 0.85) 0%,
    rgba(90, 190, 255, 0.42) 42%,
    rgba(0, 0, 0, 0) 74%
  );
  filter: blur(12px);
  opacity: var(--kpi-glow);
  pointer-events: none;
}

/* 椭圆全息扫描环：三圈横向椭圆，贴合底盘透视 */
.kpi-rings {
  position: absolute;
  left: 50%;
  bottom: -20px;
  z-index: 0;
  width: 126%;
  height: 40px;
  transform: translateX(-50%);
  opacity: var(--kpi-ring-opacity);
  pointer-events: none;
}

.kpi-rings i {
  position: absolute;
  left: 50%;
  top: 50%;
  border-radius: 50%;
  border: 1.5px solid rgba(var(--kpi-accent), 0.7);
  box-shadow:
    0 0 8px rgba(var(--kpi-accent), 0.45),
    inset 0 0 8px rgba(var(--kpi-accent), 0.3);
  transform: translate(-50%, -50%);
}

.kpi-rings i:nth-child(1) {
  width: 100%;
  height: 100%;
  opacity: 0.55;
}

.kpi-rings i:nth-child(2) {
  width: 78%;
  height: 72%;
  border-color: rgba(var(--kpi-accent), 0.8);
  opacity: 0.42;
}

.kpi-rings i:nth-child(3) {
  width: 56%;
  height: 46%;
  opacity: 0.3;
}

/* 玻璃胶囊本体：长圆角胶囊、半透明磨砂玻璃、细发光边框 */
.kpi-glass {
  position: relative;
  z-index: 2;
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: fit-content;
  min-width: 200px;
  max-width: 100%;
  height: 76px;
  padding: 0 30px;
  border-radius: 6px;
  /* 玻璃本体：半透明磨砂 + 主题色内透光 */
  background:
    linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.18) 0%,
      rgba(255, 255, 255, 0.03) 42%,
      rgba(var(--kpi-accent), 0.14) 100%
    ),
    linear-gradient(
      180deg,
      rgba(var(--kpi-fill), 0.55) 0%,
      rgba(16, 48, 82, 0.35) 55%,
      rgba(8, 26, 46, 0.42) 100%
    );
  backdrop-filter: blur(10px) saturate(170%);
  -webkit-backdrop-filter: blur(10px) saturate(170%);
  border: 1px solid rgba(80, 180, 255, 0.45);
  box-shadow:
    /* 边缘外发光 */
    0 0 16px rgba(var(--kpi-accent), 0.28),
    0 6px 14px rgba(0, 10, 24, 0.55),
    /* 上沿内高光 */
    inset 0 1px 0 rgba(255, 255, 255, 0.55),
    /* 下沿内发光 */
    inset 0 -1px 0 rgba(var(--kpi-accent), 0.5);
  transition: box-shadow 0.25s ease, transform 0.25s ease;
}

/* 底部发光棱 */
.kpi-glass::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 12%;
  right: 12%;
  height: 1px;
  background: linear-gradient(
    90deg,
    rgba(var(--kpi-accent), 0) 0%,
    rgba(var(--kpi-accent), 0.85) 30%,
    rgba(255, 255, 255, 0.9) 50%,
    rgba(var(--kpi-accent), 0.85) 70%,
    rgba(var(--kpi-accent), 0) 100%
  );
  box-shadow: 0 0 6px rgba(var(--kpi-accent), 0.8);
  pointer-events: none;
}

.kpi-card:hover .kpi-glass {
  transform: translateY(-2px);
  box-shadow:
    0 0 24px rgba(var(--kpi-accent), 0.5),
    0 8px 18px rgba(0, 10, 24, 0.7),
    inset 0 1px 0 rgba(255, 255, 255, 0.7),
    inset 0 -1px 0 rgba(var(--kpi-accent), 0.7);
}

/* 数值：白色发光，水平正向 */
.kpi-value {
  font-size: 34px;
  font-weight: 600;
  color: #ffffff;
  line-height: 1;
  letter-spacing: 0.5px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  text-shadow:
    0 0 8px rgba(var(--kpi-accent), 0.95),
    0 0 20px rgba(var(--kpi-accent), 0.5);
}

.kpi-value em {
  display: inline;
  margin-left: 3px;
  font-size: 14px;
  font-style: normal;
  font-weight: 500;
  color: rgba(236, 253, 255, 0.95);
  text-shadow: 0 0 6px rgba(var(--kpi-accent), 0.55);
}

/* 指标名称：浅青白色，水平正向 */
.kpi-label {
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 1.2px;
  line-height: 1;
  color: rgba(210, 242, 255, 0.92);
  white-space: nowrap;
  text-shadow: 0 0 6px rgba(var(--kpi-accent), 0.45);
}

/* ========== 图表容器 ========== */
.chart {
  width: 100%;
  height: 100%;
}

/* ========== 各村申报明细统计 ========== */
.detail-stage {
  position: relative;
  width: 100%;
  height: 100%;
}

/* 空态覆盖在图表之上，数据为空时展示 */
.detail-empty {
  position: absolute;
  inset: 0;
  z-index: 2;
}

/* 自定义图例：覆盖在图表顶部，两行分别说明线型(项目类型)与颜色(明细类别) */
.detail-legend {
  position: absolute;
  top: 2px;
  right: 4px;
  z-index: 3;
  display: flex;
  flex-direction: column;
  gap: 4px;
  pointer-events: none;
}

.detail-legend__row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.detail-legend__item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #bfe9ff;
  white-space: nowrap;
}

.detail-legend__item em {
  font-style: normal;
}

/* 线型示意：一段横线，通过 borderTopStyle 区分实 / 虚 / 点 */
.detail-legend__line {
  display: inline-block;
  width: 18px;
  border-top: 2px solid #cfeaff;
}

/* 颜色示意：圆点（颜色由内联 style 注入） */
.detail-legend__dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* ========== 各村项目类型分布 ========== */
.bar-stage {
  position: relative;
  width: 100%;
  height: 100%;
}

/* 类型颜色标注：竖向排在图表右侧，与柱组分离 */
.type-legend {
  position: absolute;
  right: 2px;
  top: 50%;
  display: flex;
  flex-direction: column;
  gap: 11px;
  transform: translateY(-50%);
  pointer-events: none;
}

.type-legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #bfe9ff;
  white-space: nowrap;
}

.type-legend-item i {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  flex-shrink: 0;
}

.type-legend-item em {
  font-style: normal;
}

/* 仪表盘中心数值覆盖层：保证数字始终在弧线之上 */
.gauge-wrap {
  position: relative;
  height: 100%;
}

.gauge-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  pointer-events: none;
  text-align: center;
}

.gauge-center b {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.1;
  color: #eaf7ff;
  text-shadow: 0 0 8px rgba(4, 18, 34, 0.95);
}

.gauge-center span {
  font-size: 10px;
  color: #7fa8c6;
}

/* ========== 各村项目分布 ========== */
.village-map {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 4px;
  background:
    radial-gradient(circle at 50% 50%, rgba(24, 96, 158, 0.28) 0%, rgba(4, 18, 34, 0.05) 65%),
    linear-gradient(180deg, rgba(10, 26, 44, 0.35) 0%, rgba(3, 10, 18, 0.2) 100%);
  box-shadow: inset 0 0 30px rgba(0, 6, 16, 0.5);
  overflow: hidden;
}

.map-caption {
  position: absolute;
  top: 2px;
  left: 2px;
  right: 2px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: #7fa8c6;
  z-index: 2;
}

.map-town {
  color: #bfe9ff;
  letter-spacing: 1px;
}

.map-ring {
  position: absolute;
  border: 1px dashed rgba(96, 184, 244, 0.2);
  border-radius: 50%;
  pointer-events: none;
}

.map-ring-outer {
  width: 76%;
  height: 78%;
  left: 12%;
  top: 11%;
  animation: ring-spin 28s linear infinite;
}

.map-ring-inner {
  width: 44%;
  height: 46%;
  left: 28%;
  top: 27%;
  border-style: solid;
  border-color: rgba(53, 216, 255, 0.12);
}

.map-links {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.map-links line {
  stroke: rgba(96, 184, 244, 0.28);
  stroke-width: 0.25;
  stroke-dasharray: 1.4 1.2;
}

.map-node {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 6px;
  transform: translate(-50%, -50%);
  white-space: nowrap;
}

.map-node-core {
  width: var(--node-size);
  height: var(--node-size);
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(var(--node-color), 1) 42%,
    rgba(var(--node-color), 0.15) 100%
  );
  box-shadow:
    0 0 0 3px rgba(var(--node-color), 0.1),
    0 0 14px rgba(var(--node-color), 0.85),
    inset -2px -2px 4px rgba(0, 12, 8, 0.35);
  animation: node-pulse 2.4s ease-in-out infinite;
}

.map-node-label {
  display: flex;
  flex-direction: column;
  font-size: 11px;
  color: #bfe9ff;
  line-height: 1.25;
}

.map-node-label em {
  font-size: 10px;
  font-style: normal;
  color: #6fe4ff;
}

.map-empty {
  position: absolute;
  left: 50%;
  top: 52%;
  transform: translate(-50%, -50%);
  margin: 0;
  font-size: 12px;
  color: #6c8ba6;
}

/* ========== 物资状态 ========== */
.material-viewport {
  height: 100%;
  overflow: hidden;
}

.material-track {
  display: flex;
  flex-direction: column;
}

/* 物资超过可视条数时无缝向上滚动，鼠标悬停暂停 */
.material-track.scrolling {
  animation: material-scroll var(--scroll-duration, 14s) linear infinite;
}

.material-track.scrolling:hover {
  animation-play-state: paused;
}

@keyframes material-scroll {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(-50%);
  }
}

.material-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  padding: 0 0 6px;
  list-style: none;
  overflow: hidden;
}

.material-item {
  display: grid;
  grid-template-columns: 1fr 56px 66px 8px;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 1px solid rgba(56, 170, 255, 0.14);
  border-radius: 4px;
  background: rgba(9, 34, 60, 0.5);
  font-size: 12px;
}

.material-name {
  color: #cfe9ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-stock {
  color: #7fa8c6;
  text-align: right;
}

.material-stock b {
  margin-right: 2px;
  font-size: 14px;
  color: #6fe4ff;
}

.material-stock b.danger {
  color: #ff6b81;
}

.material-warn {
  font-size: 11px;
  color: #6c8ba6;
  text-align: right;
}

.material-flag {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #23e6a0;
}

.material-flag.danger {
  background: #ff6b81;
  box-shadow: 0 0 8px rgba(255, 107, 129, 0.9);
}

/* ========== 核心指标 ========== */
.metric-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-auto-rows: 1fr;
  gap: 8px;
  height: 100%;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  border-left: 2px solid rgba(96, 184, 244, 0.35);
  background: linear-gradient(90deg, rgba(16, 48, 78, 0.55) 0%, rgba(8, 22, 38, 0.1) 100%);
}

.metric-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.metric-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.metric-label {
  font-size: 11px;
  color: #8fb8d4;
}

.metric-value {
  font-size: 21px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.metric-value em {
  margin-left: 3px;
  font-size: 11px;
  font-style: normal;
  font-weight: 400;
  color: #8fb8d4;
}

/* ========== 项目状态分布：悬浮轨道圆盘 ========== */
.donut-stage {
  position: relative;
  width: 100%;
  height: 100%;
}

/* 圆盘底衬：一层柔和椭圆光垫，让整组轨道悬浮在卡片底之上 */
.donut-stage::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 78%;
  height: 74%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: radial-gradient(
    ellipse at 50% 50%,
    rgba(28, 104, 172, 0.16) 0%,
    rgba(12, 44, 76, 0.07) 52%,
    rgba(0, 0, 0, 0) 78%
  );
  pointer-events: none;
  z-index: 0;
}

/* SVG 轨道层：铺满容器（不保持比例，保证光点与 HTML 标签百分比严格对齐） */
.donut-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
  z-index: 1;
}

/* 中心径向光晕：周期性明暗呼吸，营造全息悬浮感 */
.dn-halo {
  transform-box: view-box;
  transform-origin: 220px 140px;
  animation: dn-breathe 4.6s ease-in-out infinite;
}

@keyframes dn-breathe {
  0%,
  100% {
    opacity: 0.62;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.07);
  }
}

.dn-orbit {
  fill: none;
}

/* 外层：最淡、虚线弱化，拉开纵深 */
.dn-orbit-outer {
  stroke: rgba(70, 152, 220, 0.26);
  stroke-width: 1;
  stroke-dasharray: 3 6;
}

/* 中层：淡光轨道 */
.dn-orbit-mid {
  stroke: url(#dn-mid-grad);
  stroke-width: 1.4;
  filter: drop-shadow(0 0 6px rgba(53, 180, 255, 0.32));
}

/* 内层：主发光轨道，亮度最高 */
.dn-orbit-inner {
  stroke: url(#dn-inner-grad);
  stroke-width: 2.2;
  filter: drop-shadow(0 0 10px rgba(53, 216, 255, 0.6));
}

/* 内层流动高光光带：一段亮色虚线沿椭圆缓慢循环 */
.dn-flow {
  fill: none;
  stroke: rgba(178, 244, 255, 0.95);
  stroke-width: 2.4;
  stroke-linecap: round;
  stroke-dasharray: 60 311;
  filter: drop-shadow(0 0 9px rgba(53, 216, 255, 0.9));
  animation: dn-flow-run 4.2s linear infinite;
}

@keyframes dn-flow-run {
  from {
    stroke-dashoffset: 371;
  }
  to {
    stroke-dashoffset: 0;
  }
}

/* 中心：大号白色高亮数字 + 副标题 */
.dn-center {
  position: absolute;
  left: 50%;
  top: 48.3%;
  display: flex;
  flex-direction: column;
  align-items: center;
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 3;
}

.dn-center b {
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
  color: #ffffff;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 14px rgba(53, 216, 255, 0.95), 0 0 32px rgba(53, 216, 255, 0.45);
}

.dn-center span {
  margin-top: 5px;
  font-size: 11px;
  letter-spacing: 2px;
  color: #8fb8d4;
}

/* 状态文字标签：正向可读，落在光点外侧 */
.dn-label {
  position: absolute;
  display: flex;
  align-items: baseline;
  gap: 5px;
  white-space: nowrap;
  transform: translate(2px, -50%);
  pointer-events: none;
  z-index: 2;
}

/* 左半区标签反向排列，文字朝左延伸，避免压到圆盘中心 */
.dn-label.is-left {
  flex-direction: row-reverse;
  transform: translate(calc(-100% - 2px), -50%);
}

.dn-label-name {
  font-size: 11px;
  font-weight: 500;
}

.dn-label-value {
  font-size: 17px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 8px currentColor;
}

/* 数值为 0 的节点整体弱化 */
.dn-label.is-zero {
  opacity: 0.42;
}

/* ========== 实时事件 ========== */
.event-viewport {
  height: 100%;
  overflow: hidden;
}

.event-track {
  display: flex;
  flex-direction: column;
}

/* 事件超过可视条数时无缝向上滚动，鼠标悬停暂停 */
.event-track.scrolling {
  animation: event-scroll var(--event-scroll-duration, 14s) linear infinite;
}

.event-track.scrolling:hover {
  animation-play-state: paused;
}

@keyframes event-scroll {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(-50%);
  }
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  padding: 0 0 6px;
  list-style: none;
  overflow: hidden;
}

.event-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 6px 8px;
  border-left: 2px solid rgba(96, 184, 244, 0.28);
  background: linear-gradient(90deg, rgba(14, 42, 70, 0.5) 0%, rgba(8, 22, 38, 0.14) 100%);
  box-shadow: inset 0 1px 0 rgba(130, 200, 255, 0.06);
}

/* 左侧彩色圆点：颜色按事件类别映射，带辉光 */
.event-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 50%;
}

.event-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.event-text {
  font-size: 12px;
  color: #e4f3ff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-meta {
  font-size: 10px;
  color: #6c8ba6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-time {
  flex-shrink: 0;
  font-size: 11px;
  color: #6fe4ff;
  font-variant-numeric: tabular-nums;
}

/* ========== 空状态 ========== */
.panel-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  margin: 0;
  font-size: 12px;
  color: #6c8ba6;
}

/* ========== 底部数据流 ========== */
.screen-ticker {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  height: 58px;
  margin: 0 16px 0;
  padding: 0 14px;
  border-top: 1px solid rgba(38, 224, 164, 0.2);
  background: linear-gradient(90deg, rgba(10, 42, 34, 0.55) 0%, rgba(5, 22, 18, 0.2) 100%);
  overflow: hidden;
}

.ticker-label {
  flex-shrink: 0;
  font-size: 12px;
  letter-spacing: 1px;
  color: #26e0a4;
  text-shadow: 0 0 10px rgba(38, 224, 164, 0.5);
}

.ticker-viewport {
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

.ticker-track {
  display: inline-flex;
  align-items: center;
  gap: 44px;
  white-space: nowrap;
  animation: ticker-move 38s linear infinite;
}

.ticker-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8fb8d4;
}

.ticker-item em {
  width: 5px;
  height: 5px;
  transform: rotate(45deg);
  background: #35d8ff;
  box-shadow: 0 0 6px rgba(53, 216, 255, 0.8);
}

.ticker-item b {
  font-weight: 600;
  color: #eaf7ff;
}

.ticker-update {
  flex-shrink: 0;
  font-size: 11px;
  color: #6c8ba6;
}

/* ========== 动效 ========== */
@keyframes chip-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.35;
  }
}

@keyframes node-pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.95;
  }
  50% {
    transform: scale(1.18);
    opacity: 0.7;
  }
}

@keyframes ring-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes ticker-move {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .chip-dot,
  .map-node-core,
  .map-ring-outer,
  .ticker-track {
    animation: none;
  }
}
</style>
