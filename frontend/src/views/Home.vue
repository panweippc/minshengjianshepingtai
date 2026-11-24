<template>
  <div class="home">
    <!-- 欢迎横幅 -->
    <section class="welcome">
      <div class="welcome-content">
        <div class="welcome-title">
          {{ greeting }}，{{ userName }}
          <span class="welcome-wave">👋</span>
        </div>
        <div class="welcome-meta">
          <span class="meta-chip">{{ regionText }}</span>
          <span class="meta-chip">{{ roleText }}</span>
          <span class="meta-date">{{ dateText }}</span>
        </div>
      </div>
    </section>

    <!-- 第一行：核心指标卡片 -->
    <div v-loading="statsLoading" class="stat-row">
      <el-row :gutter="16">
        <el-col
          v-for="(item, index) in statCards"
          :key="item.key"
          class="stat-col"
          :xs="24"
          :sm="12"
          :lg="6"
          :style="{ animationDelay: `${index * 70 + 60}ms` }"
        >
          <el-card
            class="stat-card"
            shadow="never"
            :style="{ background: item.tint, borderColor: item.border }"
          >
            <div class="stat-body">
              <div
                class="stat-icon"
                :style="{ background: item.gradient, boxShadow: `0 6px 14px ${item.shadow}` }"
              >
                <el-icon :size="26"><component :is="item.icon" /></el-icon>
              </div>
              <div class="stat-text">
                <div class="stat-label">{{ item.label }}</div>
                <div class="stat-value">
                  {{ formatNumber(item.value) }}<span class="stat-unit">{{ item.unit }}</span>
                </div>
              </div>
              <el-icon class="stat-watermark" :size="80" :style="{ color: item.color }">
                <component :is="item.icon" />
              </el-icon>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 项目进度概览：一个项目一条记录，展示当前阶段 -->
    <el-card v-loading="statsLoading" class="progress-card" shadow="never">
      <template #header>
        <div class="progress-header">
          <div class="progress-heading">
            <span class="chart-mark"></span>
            <span class="chart-title">项目进度概览</span>
          </div>
          <span class="progress-total">共 {{ progressProjects.length }} 个项目</span>
        </div>
      </template>
      <div class="progress-legend">
        <span class="legend-item"><i class="legend-dot done"></i>已完成阶段</span>
        <span class="legend-item"><i class="legend-dot current"></i>当前阶段</span>
        <span class="legend-item"><i class="legend-dot todo"></i>未开始</span>
      </div>
      <div v-if="progressProjects.length" class="project-list">
        <div
          v-for="item in progressProjects"
          :key="item.approvalCode || item.id"
          class="project-row"
        >
          <div class="project-info">
            <span class="project-name" :title="item.projectName">{{ item.projectName }}</span>
            <span class="project-code">{{ item.approvalCode }}</span>
          </div>
          <div class="stage-track">
            <span
              v-for="step in STAGE_STEPS"
              :key="step.index"
              class="stage-cell"
              :class="{
                done: isDoneStage(step.index, item),
                current: item.currentStages.includes(step.index)
              }"
            >
              {{ step.label }}
            </span>
          </div>
          <span class="stage-text" :class="{ archived: latestStageOf(item) === 5 }">
            {{ stageLabelsOf(item) }}
          </span>
        </div>
      </div>
      <el-empty v-else description="暂无项目" :image-size="70" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getDashboardOverview } from '@/api/dashboard'
import type { DashboardOverviewVO, ProjectProgressVO } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'

const store = useUserStore()
const { userInfo } = storeToRefs(store)

const statsLoading = ref(false)
const overview = ref<DashboardOverviewVO | null>(null)

/** 当前登录用户显示名 */
const userName = computed(() => userInfo.value?.realName || userInfo.value?.username || '用户')

/** 按时段展示问候语 */
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

/** 所属区划 */
const regionText = computed(() => userInfo.value?.regionName || userInfo.value?.regionCode || '未分配区划')

/** 当前角色 */
const roleText = computed(() => userInfo.value?.roles?.join('、') || '无角色')

/** 当前日期（周几） */
const dateText = computed(() => {
  const now = new Date()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${weekdays[now.getDay()]}`
})

/** 第一行指标卡片配置 */
const statCards = computed(() => [
  {
    key: 'village',
    label: '行政村总数',
    value: overview.value?.villageCount ?? 0,
    unit: '个',
    icon: 'OfficeBuilding',
    color: '#22619e',
    gradient: 'linear-gradient(135deg, #22619e 0%, #17518c 100%)',
    shadow: 'rgba(23, 81, 140, 0.32)',
    tint: 'linear-gradient(135deg, #eef5fd 0%, #ffffff 72%)',
    border: '#dbe8f7'
  },
  {
    key: 'household',
    label: '农户总数',
    value: overview.value?.householdCount ?? 0,
    unit: '户',
    icon: 'House',
    color: '#0f9d6c',
    gradient: 'linear-gradient(135deg, #0f9d6c 0%, #3ecf92 100%)',
    shadow: 'rgba(15, 157, 108, 0.32)',
    tint: 'linear-gradient(135deg, #edfaf4 0%, #ffffff 72%)',
    border: '#d5efe4'
  },
  {
    key: 'facility',
    label: '公共基础设施总数',
    value: overview.value?.facilityCount ?? 0,
    unit: '处',
    icon: 'Connection',
    color: '#e59114',
    gradient: 'linear-gradient(135deg, #e59114 0%, #ffc247 100%)',
    shadow: 'rgba(229, 145, 20, 0.32)',
    tint: 'linear-gradient(135deg, #fff8ec 0%, #ffffff 72%)',
    border: '#f6e6c9'
  },
  {
    key: 'material',
    label: '在库物资总数',
    value: overview.value?.materialStockTotal ?? 0,
    unit: '件',
    icon: 'Box',
    color: '#e35353',
    gradient: 'linear-gradient(135deg, #e35353 0%, #ff8a6e 100%)',
    shadow: 'rgba(227, 83, 83, 0.32)',
    tint: 'linear-gradient(135deg, #fef2f2 0%, #ffffff 72%)',
    border: '#f8dcdc'
  }
])

/** 数字千分位展示 */
function formatNumber(value: number) {
  return value.toLocaleString('zh-CN')
}

/** 项目进度阶段定义（顺序与后端 stageIndex 保持一致） */
const STAGE_STEPS = [
  { index: 1, label: '待审核' },
  { index: 2, label: '资金' },
  { index: 3, label: '施工中' },
  { index: 4, label: '待验收' },
  { index: 5, label: '已归档' }
]

/** 项目进度概览：一个项目一条记录 */
const progressProjects = computed(() => overview.value?.projectProgress ?? [])

/** 项目当前阶段中最靠后的序号（用于判断已完成阶段与右侧文字配色） */
function latestStageOf(item: ProjectProgressVO): number {
  return Math.max(...item.currentStages, 2)
}

/** 已完成阶段：序号小于最靠后的当前阶段，且本身不属于当前阶段 */
function isDoneStage(stepIndex: number, item: ProjectProgressVO): boolean {
  return stepIndex < latestStageOf(item) && !item.currentStages.includes(stepIndex)
}

/** 当前阶段文字：同一项目同时处于多个阶段时一并展示 */
function stageLabelsOf(item: ProjectProgressVO): string {
  return item.currentStages
    .map((index) => STAGE_STEPS.find((step) => step.index === index)?.label)
    .filter((label): label is string => Boolean(label))
    .join('·')
}

/** 加载首页概览统计 */
async function loadStats() {
  statsLoading.value = true
  try {
    const res = await getDashboardOverview()
    overview.value = res.data
  } catch {
    overview.value = null
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ========== 欢迎语 ========== */
.welcome {
  padding: 2px 4px;
  animation: rise-in 0.5s ease both;
}

.welcome-content {
  position: relative;
  z-index: 1;
}

.welcome-title {
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 0.5px;
  line-height: 1.3;
  color: #14384a;
}

.welcome-wave {
  display: inline-block;
  margin-left: 6px;
  animation: wave 1.6s ease-in-out infinite;
  transform-origin: 70% 70%;
}

@keyframes wave {
  0%,
  100% {
    transform: rotate(0deg);
  }
  25% {
    transform: rotate(14deg);
  }
  75% {
    transform: rotate(-8deg);
  }
}

.welcome-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.meta-chip {
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid #d3e2f2;
  background: #eef4fb;
  font-size: 12px;
  color: #17518c;
}

.meta-date {
  margin-left: 4px;
  font-size: 12px;
  color: #7d8e9b;
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 第一行指标卡片 ========== */
.stat-row {
  min-height: 112px;
}

.stat-col {
  animation: rise-in 0.45s ease both;
}

.stat-card {
  overflow: hidden;
  border: 1px solid #eef4f8;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(31, 95, 168, 0.06);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 28px rgba(23, 81, 140, 0.16);
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
  background: transparent;
}

.stat-body {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #ffffff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.stat-text {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #5d7182;
  letter-spacing: 0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-value {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 700;
  color: #112f4a;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.stat-unit {
  margin-left: 6px;
  font-size: 12px;
  font-weight: 400;
  color: #94a4b2;
}

.stat-watermark {
  position: absolute;
  right: 2px;
  bottom: -18px;
  opacity: 0.07;
  pointer-events: none;
}

/* ========== 项目进度概览 ========== */
.progress-card {
  border: 1px solid #eef4f8;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(31, 95, 168, 0.06);
  animation: rise-in 0.45s ease both;
  animation-delay: 340ms;
}

.progress-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #eef4f8;
}

.progress-card :deep(.el-card__body) {
  padding: 16px 20px 20px;
}

.progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.progress-heading {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-mark {
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: linear-gradient(180deg, #22619e 0%, #6aa9dd 100%);
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #123a5e;
  letter-spacing: 0.2px;
}

.progress-total {
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid #dfe9f6;
  background: #f4f8fd;
  font-size: 12px;
  color: #17518c;
}

/* 进度图例 */
.progress-legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 12px;
  color: #66798c;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border: 1px solid #e9eff7;
  border-radius: 999px;
  background: #f7fafd;
}

.legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.legend-dot.done {
  background: linear-gradient(135deg, #a7cbe9 0%, #7fb4df 100%);
}

.legend-dot.current {
  background: linear-gradient(135deg, #5a97cd 0%, #3a7bb8 100%);
  box-shadow: 0 0 0 3px rgba(58, 123, 184, 0.14);
}

.legend-dot.todo {
  background-color: #e4ebf4;
}

/* 项目列表：一个项目一条记录 */
.project-list {
  display: flex;
  flex-direction: column;
}

.project-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 2px;
  padding: 10px 12px;
  border-radius: 10px;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

.project-row:hover {
  background-color: #f3f7fc;
  box-shadow: inset 3px 0 0 rgba(23, 81, 140, 0.55);
}

.project-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  /* 固定左侧信息列宽度，保证各行进度条起点一致 */
  flex: 0 0 200px;
  min-width: 0;
}

.project-name {
  font-size: 13px;
  font-weight: 500;
  color: #14384a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.project-code {
  font-size: 12px;
  color: #8598a8;
  font-variant-numeric: tabular-nums;
}

.stage-track {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 100px;
}

.stage-cell {
  flex: 1;
  min-width: 0;
  height: 24px;
  border-radius: 8px;
  background-color: #f1f6fb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  letter-spacing: 0.2px;
  color: #94a7b9;
  white-space: nowrap;
  overflow: hidden;
  transition: background 0.3s, color 0.3s, box-shadow 0.3s;
}

.stage-cell.done {
  background: linear-gradient(135deg, #b9d8ef 0%, #93c1e4 100%);
  color: #ffffff;
}

.stage-cell.current {
  background: linear-gradient(135deg, #5a97cd 0%, #3a7bb8 100%);
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 3px 10px rgba(58, 123, 184, 0.28);
}

.stage-text {
  /* 固定右侧阶段文字宽度，保证各行进度条总宽度一致、分段对齐 */
  width: 128px;
  min-width: 128px;
  max-width: 128px;
  flex-shrink: 0;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  color: #1b5c9c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stage-text.archived {
  color: #17518c;
}

/* 适配窄屏 */
@media (max-width: 768px) {
  .home {
    gap: 16px;
  }

  .welcome-title {
    font-size: 20px;
  }

  .project-info {
    flex-basis: 140px;
  }

  .stage-text {
    width: 96px;
    min-width: 96px;
    max-width: 96px;
  }
}

/* 尊重系统减少动效设置 */
@media (prefers-reduced-motion: reduce) {
  .welcome,
  .stat-col,
  .progress-card,
  .welcome-wave {
    animation: none;
  }

  .stat-card,
  .stage-cell,
  .project-row {
    transition: none;
  }
}
</style>
