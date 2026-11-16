<template>
  <div class="fund-stat-query">
    <!-- ========= 顶部筛选区（参考图：只有年度 + 区划 + 查询按钮） ========= -->
    <el-card shadow="never" class="top-filter">
      <div class="filter-row">
        <div class="filter-item">
          <span class="filter-label">年度</span>
          <el-select v-model="query.year" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="String(y)" />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">区划</span>
          <el-cascader
            v-model="query.regionCodePath"
            :options="regionTree"
            :props="{ value: 'code', label: 'name', children: 'children', checkStrictly: true }"
            placeholder="请选择区划"
            clearable
            filterable
            style="width: 260px"
          />
        </div>
        <div class="filter-item filter-item--btns">
          <el-button type="primary" :loading="loading" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- ========= 左侧维度面板 + 右侧主区域 ========= -->
    <div class="main-body">
      <!-- 左侧维度面板（和 DeclarationQuery 风格一致） -->
      <el-card shadow="never" class="sidebar">
        <div class="filter-section">
          <div class="section-header">
            <span class="section-title-inline" :class="{ 'section-title-inline--active': query.groupType === 'fundType' }" @click="switchGroupTypeOnly('fundType')">按资金类型</span>
          </div>
          <div class="stat-scroll">
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
              <span class="stat-label">{{ item.label }}</span>
              <span class="stat-count">{{ item.count }}</span>
            </div>
            <div v-if="sidebar.byFundType.length === 0" class="empty-hint">暂无数据</div>
          </div>
        </div>

        <el-divider class="sidebar-divider" />

        <div class="filter-section">
          <div class="section-header">
            <span class="section-title-inline" :class="{ 'section-title-inline--active': query.groupType === 'fundSource' }" @click="switchGroupTypeOnly('fundSource')">按资金来源</span>
          </div>
          <div class="stat-scroll">
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
              <span class="stat-label">{{ item.label }}</span>
              <span class="stat-count">{{ item.count }}</span>
            </div>
            <div v-if="sidebar.byFundSource.length === 0" class="empty-hint">暂无数据</div>
          </div>
        </div>

        <el-divider class="sidebar-divider" />

        <div class="filter-section">
          <div class="section-header">
            <span class="section-title-inline" :class="{ 'section-title-inline--active': query.groupType === 'nodeLevel' }" @click="switchGroupTypeOnly('nodeLevel')">按节点层级</span>
          </div>
          <div class="stat-scroll">
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
              <span class="stat-label">{{ item.label }}</span>
              <span class="stat-count">{{ item.count }}</span>
            </div>
          </div>
        </div>

        <el-divider class="sidebar-divider" />

        <div class="filter-section">
          <div class="section-header">
            <span class="section-title-inline" :class="{ 'section-title-inline--active': query.groupType === 'village' }" @click="switchGroupTypeOnly('village')">按所属村组</span>
          </div>
          <div class="stat-scroll stat-scroll--village">
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
              <span class="stat-label">{{ item.label }}</span>
              <span class="stat-count">{{ item.count }}</span>
            </div>
            <div v-if="sidebar.byVillage.length === 0" class="empty-hint">暂无数据</div>
          </div>
        </div>

        <div class="sidebar-actions">
          <el-button link type="primary" @click="clearSidebarFilter">清除筛选</el-button>
        </div>
      </el-card>

      <!-- 右侧主区域：主统计汇总表（参考图嵌套结构 + DeclarationQuery 模式） -->
      <el-card shadow="never" class="result-card">
        <!-- 表头顶部：副标题 + 右上角总条数 -->
        <div class="table-header-bar">
          <div class="table-subtitle">
            {{ query.year || '全部' }}年 · {{ regionLabel }} · 统计结果
          </div>
          <div class="table-total">
            共 <span class="table-total-num">{{ globalSummary.recordCount }}</span> 条源记录
          </div>
        </div>

        <!-- 主统计表：嵌套展平 + grandTotal 总计行 -->
        <el-table
          :data="rows"
          v-loading="loading"
          border
          stripe
          :row-class-name="rowClassName"
          style="flex: 1"
        >
          <el-table-column :label="groupHeader" min-width="140">
            <template #default="{ row }">
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
            min-width="140"
          >
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">{{ row.villageName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="recordCount" label="资金笔数（笔）" min-width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">{{ row.recordCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="projectCount" label="涉及项目（个）" min-width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">{{ row.projectCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="villageCoverage" label="覆盖村组（个）" min-width="130" align="right">
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">{{ row.villageCoverage }}</span>
            </template>
          </el-table-column>
          <el-table-column label="资金金额（元）" min-width="180" align="right">
            <template #default="{ row }">
              <span class="amount-cell" :class="{ 'grand-total': row.grandTotal }">
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
.fund-stat-query { padding: 0; }
.top-filter { margin-bottom: 12px; }
.filter-row { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
.filter-item { display: flex; align-items: center; gap: 8px; }
.filter-item--btns { margin-left: auto; }
.filter-label { font-size: 13px; color: #606266; white-space: nowrap; }

.main-body { display: flex; gap: 12px; height: calc(100vh - 260px); min-height: 520px; }
.sidebar { flex: 0 0 260px; overflow-y: auto; }
.sidebar > :deep(.el-card__body) { padding: 12px 14px 10px; display: flex; flex-direction: column; gap: 4px; }

.filter-section { margin-bottom: 4px; }
.section-header { margin-bottom: 6px; display: flex; justify-content: space-between; align-items: center; }
.section-title-inline { font-size: 13px; color: #606266; font-weight: 500; margin-left: 4px; cursor: pointer; user-select: none; }
.section-title-inline:hover { color: #409eff; }
.section-title-inline--active { color: #409eff; }

.stat-scroll { max-height: 160px; overflow-y: auto; }
.stat-scroll--village { max-height: 260px; }

.stat-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 10px; margin-bottom: 4px;
  cursor: pointer; border-radius: 4px; font-size: 13px; color: #606266; transition: background 0.15s;
}
.stat-item:hover { background: #f5f7fa; }
.stat-item.active { background: #ecf5ff; color: #409eff; font-weight: 500; }
.stat-item.disabled { color: #909399; }
.stat-label { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.stat-count { flex: 0 0 auto; margin-left: 8px; color: #c0c4cc; font-size: 12px; font-variant-numeric: tabular-nums; }
.stat-item.active .stat-count { color: #409eff; }

.empty-hint { font-size: 12px; color: #c0c4cc; padding: 6px 10px; text-align: center; }
.sidebar-divider { margin: 10px 0; }
.sidebar-actions { padding-top: 10px; border-top: 1px solid #ebeef5; margin-top: 6px; text-align: center; }

.result-card { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; padding-bottom: 10px; }
.result-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; overflow: hidden; padding-bottom: 10px; }

/* 表头顶部副标题条 */
.table-header-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 4px 10px; font-size: 13px; margin-bottom: 0;
}
.table-subtitle { color: #606266; font-weight: 500; }
.table-total { color: #909399; }
.table-total-num { color: #409eff; font-weight: 600; font-variant-numeric: tabular-nums; margin: 0 2px; }

/* 行类型样式 */
:deep(.row-grand-total) { background: #fdf6ec !important; font-weight: 600; }
:deep(.row-grand-total td.el-table__cell),
:deep(.row-grand-total td.el-table__cell .cell) { background: #fdf6ec !important; }
:deep(.row-group-summary) { background: #fafafa !important; }
:deep(.row-group-summary td.el-table__cell),
:deep(.row-group-summary td.el-table__cell .cell) { background: #fafafa !important; font-weight: 500; }

/* grandTotal / groupSummary span 样式 */
.grand-total { font-weight: 700; color: #303133; }
.group-summary { font-weight: 600; color: #303133; }

/* 金额列 —— 参考图：绿色 */
.amount-cell { color: #52c41a; font-variant-numeric: tabular-nums; }
.amount-cell.grand-total { color: #52c41a; font-weight: 700; }

.caliber-note { margin-top: 12px; padding: 8px 12px; background: #f4f5f5; border-radius: 4px; font-size: 12px; color: #909399; display: flex; align-items: center; gap: 6px; line-height: 1.6; }
</style>
