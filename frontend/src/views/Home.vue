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
          <el-card class="stat-card" shadow="never">
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
              :class="{ done: step.index < item.stageIndex, current: step.index === item.stageIndex }"
            >
              {{ step.label }}
            </span>
          </div>
          <span class="stage-text" :class="{ archived: item.stage === '已归档' }">
            {{ item.stage }}
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
import type { DashboardOverviewVO } from '@/api/dashboard'
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
    color: '#3d8bff',
    gradient: 'linear-gradient(135deg, #3d8bff 0%, #77b6ff 100%)',
    shadow: 'rgba(61, 139, 255, 0.28)'
  },
  {
    key: 'household',
    label: '农户总数',
    value: overview.value?.householdCount ?? 0,
    unit: '户',
    icon: 'House',
    color: '#34b37e',
    gradient: 'linear-gradient(135deg, #34b37e 0%, #7bd8a8 100%)',
    shadow: 'rgba(52, 179, 126, 0.28)'
  },
  {
    key: 'facility',
    label: '公共基础设施总数',
    value: overview.value?.facilityCount ?? 0,
    unit: '处',
    icon: 'Connection',
    color: '#f0a63c',
    gradient: 'linear-gradient(135deg, #f0a63c 0%, #ffd07a 100%)',
    shadow: 'rgba(240, 166, 60, 0.28)'
  },
  {
    key: 'material',
    label: '在库物资总数',
    value: overview.value?.materialStockTotal ?? 0,
    unit: '件',
    icon: 'Box',
    color: '#f56c6c',
    gradient: 'linear-gradient(135deg, #f56c6c 0%, #ff9f8f 100%)',
    shadow: 'rgba(245, 108, 108, 0.28)'
  }
])

/** 数字千分位展示 */
function formatNumber(value: number) {
  return value.toLocaleString('zh-CN')
}

/** 项目进度阶段定义（顺序与后端 stageIndex 保持一致） */
const STAGE_STEPS = [
  { index: 1, label: '待审核' },
  { index: 2, label: '已通过' },
  { index: 3, label: '施工中' },
  { index: 4, label: '待验收' },
  { index: 5, label: '已归档' }
]

/** 项目进度概览：一个项目一条记录 */
const progressProjects = computed(() => overview.value?.projectProgress ?? [])

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
  color: #1f2d3d;
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
  border: 1px solid #dbe8fc;
  background: #eef4ff;
  font-size: 12px;
  color: #2f6fd0;
}

.meta-date {
  margin-left: 4px;
  font-size: 12px;
  color: #8a94a6;
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
  border: none;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 26px rgba(31, 95, 191, 0.14);
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
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
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #ffffff;
}

.stat-text {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #7b8794;
  letter-spacing: 0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-value {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 700;
  color: #1f2d3d;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.stat-unit {
  margin-left: 6px;
  font-size: 12px;
  font-weight: 400;
  color: #a0a9b6;
}

.stat-watermark {
  position: absolute;
  right: 2px;
  bottom: -18px;
  opacity: 0.06;
  pointer-events: none;
}

/* ========== 项目进度概览 ========== */
.progress-card {
  border: none;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
  animation: rise-in 0.45s ease both;
  animation-delay: 340ms;
}

.progress-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f3f8;
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
  background: linear-gradient(180deg, #4392ff 0%, #8cc4ff 100%);
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2d3d;
  letter-spacing: 0.2px;
}

.progress-total {
  padding: 4px 12px;
  border-radius: 999px;
  background: #eef4ff;
  font-size: 12px;
  color: #2f6fd0;
}

/* 进度图例 */
.progress-legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #8a94a6;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-dot.done {
  background-color: #a0cfff;
}

.legend-dot.current {
  background-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
}

.legend-dot.todo {
  background-color: #eff2f7;
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
  background-color: #f6f9ff;
  box-shadow: inset 3px 0 0 rgba(64, 158, 255, 0.6);
}

.project-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 0 1 240px;
  min-width: 120px;
}

.project-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2d3d;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.project-code {
  font-size: 12px;
  color: #9aa3af;
  font-variant-numeric: tabular-nums;
}

.stage-track {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 100px;
}

.stage-cell {
  flex: 1;
  min-width: 0;
  height: 22px;
  border-radius: 6px;
  background-color: #f1f4f9;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #9aa3af;
  white-space: nowrap;
  overflow: hidden;
  transition: background-color 0.3s, color 0.3s;
}

.stage-cell.done {
  background-color: #a0cfff;
  color: #ffffff;
}

.stage-cell.current {
  background: linear-gradient(135deg, #409eff 0%, #6fb3ff 100%);
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.35);
}

.stage-text {
  width: 60px;
  flex-shrink: 0;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  color: #2f6fd0;
}

.stage-text.archived {
  color: #3aa675;
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
    flex-basis: 150px;
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
