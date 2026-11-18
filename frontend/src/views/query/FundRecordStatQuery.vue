<template>
  <div class="fund-stat-query">
    <!-- ========= 顶部筛选区 ========= -->
    <el-card shadow="never" class="top-filter">
      <div class="filter-row">
        <div class="filter-left">
          <div class="filter-item">
            <span class="filter-label">年度筛选</span>
            <el-select v-model="query.year" placeholder="全部年度" clearable style="width: 140px">
              <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="String(y)" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">区划选择</span>
            <el-cascader
              v-model="query.regionCodePath"
              :options="regionTree"
              :props="{ value: 'code', label: 'name', children: 'children', checkStrictly: true }"
              placeholder="全部区划"
              clearable
              filterable
              style="width: 240px"
            />
          </div>
        </div>
        <div class="filter-right">
          <el-button type="primary" icon="Search" :loading="loading" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- ========= 左侧维度面板 + 右侧主区域 ========= -->
    <div class="main-body">
      <!-- 左侧维度面板 -->
      <el-card shadow="never" class="sidebar">
        <template #header>
          <div class="sidebar-title">查询方案</div>
        </template>

        <div class="filter-section">
          <div class="section-header">
            <el-radio :model-value="query.groupType" label="fundType" @change="switchGroupTypeOnly('fundType')">
              <span class="section-title-inline">按资金类型统计</span>
            </el-radio>
          </div>
          <div class="section-sub">
            <span>资金类型（{{ sidebar.byFundType.length }}）</span>
          </div>
          <div
            v-for="item in sidebar.byFundType"
            :key="item.value"
            class="stat-item"
            :class="{
              active: query.groupType === 'fundType' && query.groupValue && String(item.value) === String(query.groupValue),
              disabled: query.groupType !== 'fundType'
            }"
            @click="onSidebarItemClick('fundType', item.value)"
          >
            <span class="stat-check" />
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">{{ item.count }}</span>
          </div>
          <div v-if="sidebar.byFundType.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <div class="filter-section">
          <div class="section-header">
            <el-radio :model-value="query.groupType" label="fundSource" @change="switchGroupTypeOnly('fundSource')">
              <span class="section-title-inline">按资金来源统计</span>
            </el-radio>
          </div>
          <div class="section-sub">
            <span>资金来源（{{ sidebar.byFundSource.length }}）</span>
          </div>
          <div
            v-for="item in sidebar.byFundSource"
            :key="item.value"
            class="stat-item"
            :class="{
              active: query.groupType === 'fundSource' && query.groupValue && String(item.value) === String(query.groupValue),
              disabled: query.groupType !== 'fundSource'
            }"
            @click="onSidebarItemClick('fundSource', item.value)"
          >
            <span class="stat-check" />
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">{{ item.count }}</span>
          </div>
          <div v-if="sidebar.byFundSource.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <div class="filter-section">
          <div class="section-header">
            <el-radio :model-value="query.groupType" label="nodeLevel" @change="switchGroupTypeOnly('nodeLevel')">
              <span class="section-title-inline">按节点层级统计</span>
            </el-radio>
          </div>
          <div class="section-sub">
            <span>节点层级（{{ sidebar.byNodeLevel.length }}）</span>
          </div>
          <div
            v-for="item in sidebar.byNodeLevel"
            :key="item.value"
            class="stat-item"
            :class="{
              active: query.groupType === 'nodeLevel' && query.groupValue && String(item.value) === String(query.groupValue),
              disabled: query.groupType !== 'nodeLevel'
            }"
            @click="onSidebarItemClick('nodeLevel', item.value)"
          >
            <span class="stat-check" />
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">{{ item.count }}</span>
          </div>
          <div v-if="sidebar.byNodeLevel.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <div class="filter-section">
          <div class="section-header">
            <el-radio :model-value="query.groupType" label="village" @change="switchGroupTypeOnly('village')">
              <span class="section-title-inline">按所属村组统计</span>
            </el-radio>
          </div>
          <div class="section-sub">
            <span>所属村组（{{ sidebar.byVillage.length }}）</span>
          </div>
          <div class="stat-scroll">
            <div
              v-for="item in sidebar.byVillage"
              :key="item.value"
              class="stat-item"
              :class="{
                active: query.groupType === 'village' && query.groupValue && String(item.value) === String(query.groupValue),
                disabled: query.groupType !== 'village'
              }"
              @click="onSidebarItemClick('village', item.value)"
            >
              <span class="stat-check" />
              <span class="stat-label">{{ item.label }}</span>
              <span class="stat-count">{{ item.count }}</span>
            </div>
          </div>
          <div v-if="sidebar.byVillage.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <div v-if="query.groupValue" class="sidebar-actions">
          <el-button size="small" @click="clearSidebarFilter">清除筛选</el-button>
        </div>
      </el-card>

      <!-- 右侧表格区 -->
      <el-card shadow="never" class="result-card">
        <template #header>
          <div class="result-header">
            <div class="result-title-wrap">
              <div class="result-sub">{{ resultSubtitle }}</div>
            </div>
            <div class="record-badge">共 {{ globalSummary.recordCount }} 条源记录</div>
          </div>
        </template>

        <el-table
          :data="rows"
          v-loading="loading"
          :row-class-name="rowClassName"
        >
          <el-table-column :label="groupHeader" min-width="120">
            <template #default="{ row }">
              <span v-if="!row.groupSummary && !row.grandTotal" class="tree-line" />
              <span :class="{
                'grand-total': row.grandTotal,
                'group-summary': row.groupSummary && !row.grandTotal
              }">{{ row.groupLabel }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="query.groupType !== 'village'"
            prop="villageName"
            label="村组"
            min-width="100"
          >
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">{{ row.villageName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="recordCount" label="资金笔数（笔）" min-width="110" align="right">
            <template #default="{ row }">
              <span class="num-text" :class="{ 'grand-total': row.grandTotal }">{{ row.recordCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="projectCount" label="涉及项目（个）" min-width="110" align="right">
            <template #default="{ row }">
              <span class="num-text" :class="{ 'grand-total': row.grandTotal }">{{ row.projectCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="villageCoverage" label="覆盖村组（个）" min-width="120" align="right">
            <template #default="{ row }">
              <span class="num-text" :class="{ 'grand-total': row.grandTotal }">{{ row.villageCoverage }}</span>
            </template>
          </el-table-column>
          <el-table-column label="资金金额（元）" min-width="130" align="right">
            <template #default="{ row }">
              <span class="amount-text" :class="{ 'grand-total': row.grandTotal }">
                {{ formatMoney(row.amount) }}
              </span>
            </template>
          </el-table-column>
        </el-table>

        <div class="caliber-note">
          <el-icon><InfoFilled /></el-icon>
          <span>
            统计口径：各分类显示小计，村组行显示该分类在对应村组的汇总，总计按当前筛选条件重新计算。
          </span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { getRegionList } from '@/api/region'
import { getFundStat, type Row } from '@/api/fundQuery'

// ===================== 区划树 =====================
interface RegionNode { value: string; label: string; children?: RegionNode[] }
const regionTree = ref<RegionNode[]>([])

async function loadRegionTree() {
  try {
    const res: any = await getRegionList()
    const list: any[] = res?.data ?? res ?? []
    const parents = new Map<string, RegionNode>()
    for (const r of list) {
      parents.set(r.code, { value: r.code, label: r.name })
    }
    const roots: RegionNode[] = []
    for (const r of list) {
      const node = parents.get(r.code)!
      if (r.parent_code && parents.has(r.parent_code)) {
        const parent = parents.get(r.parent_code)!
        if (!parent.children) parent.children = []
        parent.children.push(node)
      } else {
        roots.push(node)
      }
    }
    regionTree.value = roots
  } catch { /* 区划失败降级为空树 */ }
}

// ===================== 年度选项 =====================
const yearOptions: number[] = []
const now = new Date().getFullYear()
for (let y = now - 5; y <= now + 1; y++) yearOptions.push(y)

// ===================== 查询态 =====================
const query = reactive({
  year: '' as string,
  regionCodePath: [] as string[],
  regionCode: '' as string,
  /** 分组维度：fundType / fundSource / nodeLevel / village */
  groupType: 'fundType' as 'fundType' | 'fundSource' | 'nodeLevel' | 'village',
  /** 左侧面板 item.toggle 用：fundType / fundSource / nodeLevel / village 之一的选中值 */
  groupValue: '' as string
})

const loading = ref(false)
/** 主统计表嵌套展平行（后端 FundStatVO.Row） */
const rows = ref<Row[]>([])
/** 全局 summary（用于右上角 "共 N 条源记录"） */
const globalSummary = ref<{ recordCount: number; projectCount: number; declareCount: number }>(
  { recordCount: 0, projectCount: 0, declareCount: 0 }
)
const sidebar = ref<{
  byFundType:   { value: string; label: string; count: number }[]
  byFundSource: { value: string; label: string; count: number }[]
  byNodeLevel:  { value: string; label: string; count: number }[]
  byVillage:    { value: string; label: string; count: number }[]
}>({ byFundType: [], byFundSource: [], byNodeLevel: [], byVillage: [] })

// ===================== 主表列相关 computed =====================

/** 动态表头第一列（跟着 groupType 变） */
const groupHeader = computed(() => {
  switch (query.groupType) {
    case 'fundSource': return '资金来源'
    case 'nodeLevel':  return '下拨类型'
    case 'village':    return '所属村组'
    default:           return '资金类型'
  }
})

/** 区划副标题（显示选中的区划名或"全部区划"） */
const regionLabel = computed(() => {
  if (!query.regionCode) return '全部区划'
  // 从 regionTree 里找 name
  function walk(nodes: RegionNode[] | undefined, target: string): string {
    if (!nodes) return ''
    for (const n of nodes) {
      if (n.value === target) return n.label
      const sub = walk(n.children, target)
      if (sub) return sub
    }
    return ''
  }
  return walk(regionTree.value, query.regionCode) || '全部区划'
})

/** 结果区副标题：年度 · 区划 · 统计结果 */
const resultSubtitle = computed(() => {
  const year = query.year ? `${query.year}年` : '全部年度'
  return `${year} · ${regionLabel.value} · 统计结果`
})

/** 根据 row 标记加 class（grandTotal 最底总计、groupSummary 大分组汇总） */
function rowClassName({ row }: { row: Row }) {
  if (row.grandTotal) return 'row-grand-total'
  if (row.groupSummary) return 'row-group-summary'
  return ''
}

// ===================== 分组维度切换 =====================

/**
 * 点 section 标题 → 切维度 + 清空 groupValue（主表展示该维度全部分组）
 */
function switchGroupTypeOnly(newDim: typeof query.groupType) {
  query.groupType = newDim
  query.groupValue = ''
  handleQuery()
}

/**
 * 左侧面板 item 点击 —— 和 DeclarationQuery 逻辑一致：
 *   - 点击非当前 groupType 的 item → 直接切换 groupType + groupValue
 *   - 点击当前 groupType 下的 item → toggle 清空/选中
 */
function onSidebarItemClick(groupDim: typeof query.groupType, value: string) {
  if (query.groupType !== groupDim) {
    // 点击的是"非当前激活分组"的 item → 直接切换 groupType，groupValue 设为该 item
    query.groupType = groupDim
    query.groupValue = query.groupValue === value ? '' : value
  } else {
    // 同分组 → toggle
    query.groupValue = query.groupValue === value ? '' : value
  }
  handleQuery()
}

function clearSidebarFilter() {
  query.groupValue = ''
  handleQuery()
}

// ===================== 主查询 =====================
async function handleQuery() {
  query.regionCode = query.regionCodePath.length > 0 ? query.regionCodePath[query.regionCodePath.length - 1] : ''
  loading.value = true
  try {
    const res = await getFundStat({
      year: query.year || undefined,
      regionCode: query.regionCode || undefined,
      groupType: query.groupType || undefined,
      groupValue: query.groupValue || undefined
    })
    const data = res?.data ?? (res as any)
    rows.value = data.rows ?? []
    if (data.globalSummary) globalSummary.value = data.globalSummary
    buildSidebarFromStat()
  } catch (e: any) {
    ElMessage.error('查询失败：' + (e?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

/**
 * 左侧面板四个维度**全部**从「不带 groupValue 过滤」的 stat 结果拉取。
 * 主表的 rows 可能被 groupValue 过滤只剩一条 sidebar 永远展示该维度下的
 * **全部分组**（未被选中的项仍显示灰色）。
 * 现在后端返回嵌套 rows（大分组 + 村组明细 + grandTotal），sidebar 只抽
 * groupSummary=true 的大分组汇总行（不带 grandTotal=true 的总计行）。
 */
async function buildSidebarFromStat() {
  const setOne = (dim: typeof query.groupType, list: any[]) => {
    if (dim === 'fundType')   sidebar.value.byFundType   = list
    if (dim === 'fundSource') sidebar.value.byFundSource = list
    if (dim === 'nodeLevel')  sidebar.value.byNodeLevel  = list
    if (dim === 'village')    sidebar.value.byVillage    = list
  }
  // 四个维度全量拉（都不带 groupValue）
  const allDims: typeof query.groupType[] = ['fundType', 'fundSource', 'nodeLevel', 'village']
  for (const dim of allDims) {
    try {
      const r = await getFundStat({
        year: query.year || undefined,
        regionCode: query.regionCode || undefined,
        groupType: dim
      })
      const data = r?.data ?? (r as any)
      // 只抽 groupSummary=true 的大分组汇总行（过滤掉 grandTotal 行和普通 village 明细行）
      const bigRows: Row[] = (data.rows ?? []).filter((row: Row) => row.groupSummary && !row.grandTotal)
      const list = bigRows.map((s: Row) => ({ value: s.groupValue, label: s.groupLabel, count: s.recordCount }))
      setOne(dim, list)
    } catch { /* 降级 */ }
  }
}

function resetQuery() {
  query.year = ''
  query.regionCodePath = []
  query.regionCode = ''
  query.groupType = 'fundType'
  query.groupValue = ''
  handleQuery()
}

// ===================== 工具 =====================
function formatMoney(v: any): string {
  if (v == null) return '0.00'
  const n = typeof v === 'string' ? Number(v) : v
  if (Number.isNaN(n)) return '0.00'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2, minimumFractionDigits: 2 })
}

// ===================== 生命周期 =====================
onMounted(async () => {
  await Promise.all([loadRegionTree(), handleQuery()])
})
</script>

<style scoped>
.fund-stat-query {
  padding: 0;
}

/* 通用卡片：圆角 + 柔和阴影 */
.top-filter,
.sidebar,
.result-card {
  border: none;
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(31, 45, 61, 0.06);
  background: #fff;
}
.top-filter {
  margin-bottom: 14px;
}
.top-filter :deep(.el-card__body) {
  padding: 18px 22px;
}
.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 12px;
}
.filter-left {
  display: flex;
  align-items: flex-end;
  gap: 26px;
  flex-wrap: wrap;
}
.filter-right {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.filter-right :deep(.el-button) {
  border-radius: 8px;
  padding: 9px 18px;
}
.filter-right :deep(.el-button--primary) {
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.28);
}
.filter-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.filter-label {
  font-size: 12px;
  color: #8a94a6;
  letter-spacing: 0.4px;
  white-space: nowrap;
}

.main-body {
  display: flex;
  gap: 14px;
  height: calc(100vh - 174px);
  min-height: 520px;
}

/* 左侧筛选面板 */
.sidebar {
  flex: 0 0 206px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.sidebar :deep(.el-card__header) {
  padding: 16px 18px;
  border-bottom: 1px solid #f0f2f5;
}
.sidebar :deep(.el-card__body) {
  padding: 10px 10px 10px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  scrollbar-width: none;
}
.sidebar :deep(.el-card__body)::-webkit-scrollbar {
  display: none;
}
.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2d3d;
}
.filter-section {
  border: 1px solid #eef1f6;
  border-radius: 12px;
  padding: 10px 8px 6px;
  margin-bottom: 10px;
  background: #fcfdff;
}
.section-header {
  margin-bottom: 4px;
}
.section-header :deep(.el-radio) {
  height: auto;
  margin-right: 0;
}
.section-header :deep(.el-radio__label) {
  padding-left: 6px;
}
.section-title-inline {
  font-size: 13px;
  font-weight: 600;
  color: #3a4557;
}
.section-sub {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #a0a8b9;
  margin: 2px 0 8px;
  padding-left: 0;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 4px;
  border-radius: 8px;
  font-size: 13px;
  color: #3a4557;
  cursor: pointer;
  transition: all 0.15s;
}
.stat-item:hover {
  background: #f4f7fd;
}
.stat-check {
  position: relative;
  width: 14px;
  height: 14px;
  border-radius: 4px;
  border: 1px solid #d6dbe6;
  background: #fff;
  flex: 0 0 auto;
  transition: all 0.15s;
}
.stat-item.active {
  background: #eef4ff;
  color: #2f6bff;
  font-weight: 500;
}
.stat-item.active .stat-check {
  background: #2f6bff;
  border-color: #2f6bff;
}
.stat-item.active .stat-check::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 1px;
  width: 4px;
  height: 8px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
.stat-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.stat-count {
  flex: 0 0 auto;
  font-size: 12px;
  color: #8a94a6;
  background: #f2f4f8;
  border-radius: 10px;
  padding: 1px 8px;
}
.stat-item.active .stat-count {
  color: #2f6bff;
  background: #e0eaff;
}
.stat-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.stat-item.disabled:hover {
  background: transparent;
}
.stat-scroll {
  max-height: 240px;
  overflow-y: auto;
  padding-right: 2px;
  scrollbar-width: none;
}
.stat-scroll::-webkit-scrollbar {
  display: none;
}
.empty-hint {
  font-size: 12px;
  color: #c0c4cc;
  padding: 6px 10px;
}
.sidebar-actions {
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;
  margin-top: auto;
  text-align: center;
}
.sidebar-actions :deep(.el-button) {
  border-radius: 8px;
}

/* 右侧表格区 */
.result-card {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.result-card :deep(.el-card__header) {
  padding: 16px 22px;
  border-bottom: 1px solid #f0f2f5;
}
.result-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 4px 22px 12px;
}
.result-card :deep(.el-table) {
  flex: 1;
}
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.result-sub {
  margin-top: 0;
  font-size: 12px;
  color: #a0a8b9;
}
.record-badge {
  font-size: 12px;
  color: #409eff;
  background: #eef4ff;
  border-radius: 14px;
  padding: 5px 12px;
  white-space: nowrap;
}

/* 表格：隐藏竖线，仅保留浅色横线 */
.result-card :deep(.el-table) {
  --el-table-border-color: #f0f2f5;
}
.result-card :deep(.el-table::before) {
  display: none;
}
.result-card :deep(.el-table th.el-table__cell) {
  background: #f8fafd;
  color: #8a94a6;
  font-weight: 500;
  font-size: 13px;
  border-bottom: 1px solid #eef1f6;
  padding: 12px 0;
}
.result-card :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #f4f6fa;
  padding: 12px 0;
  font-size: 13px;
  color: #3a4557;
}
.result-card :deep(.el-table tr:last-child td.el-table__cell) {
  border-bottom: none;
}
.result-card :deep(.el-table__row:hover > td.el-table__cell) {
  background: #f8fbff !important;
}

/* 行样式 */
.row-grand-total {
  background: #f5f8fd !important;
}
.row-group-summary {
  background: #fafcff !important;
}
.grand-total {
  font-weight: 700;
  color: #1f2d3d;
}
.group-summary {
  font-weight: 600;
  color: #1f2d3d;
}
.num-text {
  font-variant-numeric: tabular-nums;
}
.amount-text {
  color: #16a34a;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.tree-line {
  display: inline-block;
  width: 2px;
  height: 12px;
  background: #e3e8f0;
  margin-right: 10px;
  vertical-align: -1px;
  border-radius: 1px;
}

/* 口径说明 */
.caliber-note {
  margin-top: 10px;
  padding: 10px 14px;
  background: #f8fafd;
  border: 1px solid #eef1f6;
  border-radius: 10px;
  font-size: 12px;
  color: #8a94a6;
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.6;
}
</style>
