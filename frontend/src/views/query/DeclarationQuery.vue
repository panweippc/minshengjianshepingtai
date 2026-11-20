﻿<template>
  <div class="declaration-query">
    <!-- ==================== 顶部筛选区 ==================== -->
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
              :props="cascaderProps"
              placeholder="全部区划"
              clearable
              style="width: 240px"
            />
          </div>
          <div class="filter-item">
            <span class="filter-label">状态筛选</span>
            <el-select
              v-model="query.detailStatus"
              placeholder="全部状态"
              clearable
              style="width: 140px"
            >
              <el-option
                v-for="s in detailStatusOptions"
                :key="s.value"
                :label="s.label"
                :value="s.value"
              />
            </el-select>
          </div>
        </div>
        <div class="filter-right">
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button icon="Download" :loading="exporting" @click="handleExport">导出Excel</el-button>
        </div>
      </div>
    </el-card>

    <!-- ==================== 主体：左侧筛选面板 + 右侧表格 ==================== -->
    <div class="main-body">
      <!-- 左侧筛选面板 -->
      <el-card shadow="never" class="sidebar">
        <template #header>
          <div class="sidebar-title">查询方案</div>
        </template>

        <div class="filter-section">
          <div class="section-header">
            <el-radio
              :model-value="query.groupBy"
              label="category"
              @change="onGroupByChange('category')"
            >
              <span class="section-title-inline">按申报类别统计</span>
            </el-radio>
          </div>
          <div class="section-sub">
            <span>申报类别（{{ sidebar.byCategory.length }}）</span>
          </div>
          <div
            v-for="item in sidebar.byCategory"
            :key="item.value"
            class="stat-item"
            :class="{
              active: query.groupBy === 'category' && query.declarationCategory === item.value,
              disabled: query.groupBy !== 'category'
            }"
            @click="onSidebarItemClick('declarationCategory', 'category', item.value)"
          >
            <span class="stat-check" />
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">{{ item.count }}</span>
          </div>
          <div v-if="sidebar.byCategory.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <div class="filter-section">
          <div class="section-header">
            <el-radio
              :model-value="query.groupBy"
              label="status"
              @change="onGroupByChange('status')"
            >
              <span class="section-title-inline">按审批状态统计</span>
            </el-radio>
          </div>
          <div class="section-sub">
            <span>审批状态（{{ sidebar.byStatus.length }}）</span>
          </div>
          <div
            v-for="item in sidebar.byStatus"
            :key="item.value"
            class="stat-item"
            :class="{
              active: query.groupBy === 'status' && query.status === item.value,
              disabled: query.groupBy !== 'status'
            }"
            @click="onSidebarItemClick('status', 'status', item.value)"
          >
            <span class="stat-check" />
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">{{ item.count }}</span>
          </div>
          <div v-if="sidebar.byStatus.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <div class="filter-section">
          <div class="section-header">
            <el-radio
              :model-value="query.groupBy"
              label="village"
              @change="onGroupByChange('village')"
            >
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
                active: query.groupBy === 'village' && query.villageGroupCode === item.value,
                disabled: query.groupBy !== 'village'
              }"
              @click="onSidebarItemClick('villageGroupCode', 'village', item.value)"
            >
              <span class="stat-check" />
              <span class="stat-label">{{ item.label }}</span>
              <span class="stat-count">{{ item.count }}</span>
            </div>
          </div>
          <div v-if="sidebar.byVillage.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <div v-if="hasSidebarFilter" class="sidebar-actions">
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
            <div class="record-badge">共 {{ sourceRecordCount }} 条源记录</div>
          </div>
        </template>
        <el-table
          v-loading="loading"
          :data="rows"
          :row-class-name="rowClassName"
          :span-method="objectSpanMethod"
          @row-contextmenu="handleRowContextMenu"
        >
          <el-table-column
            :label="groupHeader"
            min-width="120"
          >
            <template #default="{ row }">
              <span v-if="!row.groupSummary && !row.grandTotal" class="tree-line" />
              <span :class="{ 'grand-total': row.grandTotal, 'group-summary': row.groupSummary && !row.grandTotal }">
                {{ query.groupBy === 'village' ? (row.villageName || row.groupLabel) : row.groupLabel }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="query.groupBy !== 'village'"
            prop="villageName"
            label="村组"
            min-width="100"
          >
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">
                {{ row.villageName }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="query.groupBy !== 'status'"
            prop="declarationCount"
            label="申报单数（单）"
            min-width="110"
            align="right"
          >
            <template #default="{ row }">
              <span class="num-text" :class="{ 'grand-total': row.grandTotal }">{{ row.declarationCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="householdCount" label="涉及户数（户）" min-width="110" align="right">
            <template #default="{ row }">
              <span class="num-text" :class="{ 'grand-total': row.grandTotal }">{{ row.householdCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="facilityCount" label="公建设施数（个）" min-width="120" align="right">
            <template #default="{ row }">
              <span class="num-text" :class="{ 'grand-total': row.grandTotal }">{{ row.facilityCount }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="query.groupBy !== 'status'"
            label="申报金额（元）"
            min-width="130"
            align="right"
          >
            <template #default="{ row }">
              <span class="amount-text" :class="{ 'grand-total': row.grandTotal }">
                {{ formatMoney(row.amount) }}
              </span>
            </template>
          </el-table-column>
        </el-table>

        <div class="caliber-note">
          <el-icon><InfoFilled /></el-icon>
          <span>统计口径：申报次数按申报记录计数；涉及户数按到户申报的申请人去重，公建申报按公建申报的公共基础设施去重。</span>
        </div>
      </el-card>
    </div>

    <!-- 右键菜单：查看明细 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
    >
      <div class="context-menu-item" @click="openDetail">查看明细</div>
    </div>

    <!-- 明细核对弹窗 -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="1000px" append-to-body>
      <el-table :data="detailRows" border max-height="460" size="small">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="businessCode" label="申报编号" min-width="150" />
        <el-table-column prop="categoryLabel" label="申报类别" min-width="90" />
        <el-table-column prop="villageName" label="村组" min-width="100" />
        <el-table-column prop="targetName" label="户主/公共设施" min-width="130" />
        <el-table-column prop="auditStatusLabel" label="审批状态" min-width="90" />
        <el-table-column prop="amount" label="金额（元）" min-width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
      </el-table>
      <div class="detail-summary">
        共 {{ detailRows.length }} 条明细，金额合计 {{ formatMoney(detailAmount) }} 元
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { getRegionList } from '@/api/region'
import {
  getDeclarationStatistics,
  type DeclarationStatRow,
  type DeclarationStatDetailItem,
  type DeclarationStatisticsVO
} from '@/api/declaration'

// ==================== 顶部筛选 ====================
const query = reactive({
  year: '' as string,
  regionCodePath: [] as string[],
  regionCode: '' as string,
  groupBy: 'category' as 'category' | 'status' | 'village',
  declarationCategory: '' as string,
  status: '' as string,
  villageGroupCode: '' as string | null,
  /** 顶部状态筛选：按申报单明细审批状态过滤统计 */
  detailStatus: '' as string
})

/** 顶部状态筛选可选项（与审批状态固定口径一致） */
const detailStatusOptions = [
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已立项', value: 'established' }
]

/** 年度选项：从 2020 到下一年 */
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: currentYear - 2020 + 2 }, (_, i) => 2020 + i)

/** 区划级联树 */
interface RegionNode {
  value: string
  label: string
  children?: RegionNode[]
}
const regionTree = ref<RegionNode[]>([])
const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  checkStrictly: true
}

/** 加载区划级联数据（从 t_sys_region 列表构建父子结构） */
async function loadRegionTree() {
  try {
    const res = await getRegionList()
    const list: any[] = res.data || []
    const byCode = new Map<string, RegionNode>()
    const roots: RegionNode[] = []
    // 先创建所有节点
    for (const r of list) {
      byCode.set(r.code, { value: r.code, label: r.name })
    }
    // 再挂载到父节点
    for (const r of list) {
      const node = byCode.get(r.code)!
      if (r.parentCode && byCode.has(r.parentCode)) {
        const parent = byCode.get(r.parentCode)!
        if (!parent.children) parent.children = []
        parent.children.push(node)
      } else {
        roots.push(node)
      }
    }
    // 按 sort 排一下（原数据已经 sort asc，这里保留顺序）
    regionTree.value = roots
  } catch {
    regionTree.value = []
  }
}

// ==================== 数据加载 ====================
const loading = ref(false)
const exporting = ref(false)
const rows = ref<DeclarationStatRow[]>([])
const sidebar = ref<DeclarationStatisticsVO['sidebar']>({
  byCategory: [],
  byStatus: [],
  byVillage: []
})

const groupHeader = computed(() => {
  if (query.groupBy === 'category') return '申报类别'
  if (query.groupBy === 'status') return '审批状态'
  return '村组'
})

/** 右侧卡片副标题：年度 · 区划 · 统计结果 */
const resultSubtitle = computed(() => {
  const year = query.year ? `${query.year}年` : '全部年度'
  const lastCode = query.regionCodePath[query.regionCodePath.length - 1]
  const region = query.regionCodePath.length
    ? findRegionLabel(regionTree.value, lastCode) || lastCode
    : '全部区划'
  return `${year} · ${region} · 统计结果`
})

/** 右侧卡片右上角：符合条件申报单总数 */
const sourceRecordCount = computed(
  () => rows.value.find((r) => r.grandTotal)?.declarationCount ?? 0
)

/** 在区划树中按 code 找名称（未命中返回空串，保证递归能继续搜兄弟节点） */
function findRegionLabel(nodes: RegionNode[], code: string): string {
  for (const n of nodes) {
    if (n.value === code) return n.label
    if (n.children && n.children.length) {
      const hit = findRegionLabel(n.children, code)
      if (hit) return hit
    }
  }
  return ''
}

/** 当前是否有左侧面板筛选条件 */
const hasSidebarFilter = computed(
  () => !!query.declarationCategory || !!query.status || !!query.villageGroupCode
)

/** 切换分组维度（点击左侧 section 标题前的 radio 触发） */
function onGroupByChange(newDim: 'category' | 'status' | 'village') {
  query.groupBy = newDim
  // 切换维度时，清空另外两个维度的子筛选条件（避免遗留脏条件）
  if (newDim !== 'category') query.declarationCategory = ''
  if (newDim !== 'status') query.status = ''
  if (newDim !== 'village') query.villageGroupCode = null
  handleQuery()
}

/** 点击左侧某个统计项（只有当前激活的分组维度才响应，否则忽略） */
function onSidebarItemClick(
  field: 'declarationCategory' | 'status' | 'villageGroupCode',
  belongsGroupBy: 'category' | 'status' | 'village',
  value: string | null
) {
  // 非激活分组维度下的子项不响应点击（避免用户以为能跨维度筛选）
  if (query.groupBy !== belongsGroupBy) return
  const current = query[field]
  if (current === value) {
    query[field] = field === 'villageGroupCode' ? null : ''
  } else {
    query[field] = value
  }
  handleQuery()
}

/** 切换左侧筛选（顶部"分组"下拉框变更时调用） */
function toggleFilter(field: 'declarationCategory' | 'status' | 'villageGroupCode', value: string | null) {
  const current = query[field]
  if (current === value) {
    query[field] = field === 'villageGroupCode' ? null : ''
  } else {
    query[field] = value
  }
  handleQuery()
}

function clearSidebarFilter() {
  query.declarationCategory = ''
  query.status = ''
  query.villageGroupCode = null
  handleQuery()
}

async function handleQuery() {
  // regionCode 取级联选中的末级 code
  query.regionCode = query.regionCodePath.length > 0 ? query.regionCodePath[query.regionCodePath.length - 1] : ''
  loading.value = true
  try {
    const res = await getDeclarationStatistics({
      year: query.year || undefined,
      regionCode: query.regionCode || undefined,
      groupBy: query.groupBy,
      declarationCategory: query.declarationCategory || undefined,
      status: query.status || undefined,
      villageGroupCode: query.villageGroupCode || undefined,
      detailStatus: query.detailStatus || undefined
    })
    rows.value = res.data.rows
    sidebar.value = res.data.sidebar
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.year = ''
  query.regionCodePath = []
  query.regionCode = ''
  query.groupBy = 'category'
  query.declarationCategory = ''
  query.status = ''
  query.villageGroupCode = null
  query.detailStatus = ''
  handleQuery()
}

// ==================== 表格合并（同一分组的村组行与汇总行首列合并） ====================
/**
 * 需求：分组汇总行（一级）要合并横跨 6 列？不，看示例表格：
 *  - 公建申报 | 全部村组 | 10 | 0 | 10 | 907000  （这一行每个 cell 都各占一列）
 *  - └        | 光明村   | 2  | 0 | 2  | 236000  （村组明细行）
 *
 * 这样看不需要 rowspan 合并，直接渲染就行。
 * 总计行也单独占一行。
 */
function rowClassName({ row }: { row: DeclarationStatRow }) {
  if (row.grandTotal) return 'row-grand-total'
  if (row.groupSummary) return 'row-group-summary'
  return ''
}
function objectSpanMethod() {
  // 不做跨列合并，保持每列独立
  return undefined
}

// ==================== 工具方法 ====================
function formatMoney(v: number | null | undefined): string {
  if (v == null || isNaN(v)) return '0'
  return v.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

// ==================== Excel 导出（CSV + BOM，Excel 直接识别） ====================
/** 把单元格转成安全的 CSV 字段（必要时加引号） */
function csvField(v: unknown): string {
  if (v == null || v === '') return ''
  const s = String(v)
  if (/[",\r\n]/.test(s)) return '"' + s.replace(/"/g, '""') + '"'
  return s
}
async function handleExport() {
  if (rows.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  exporting.value = true
  try {
    const isVillageGroup = query.groupBy === 'village'
    const isStatusGroup = query.groupBy === 'status'
    // 组装表头（分三种情况）
    let headers: string[]
    if (isVillageGroup) {
      headers = [groupHeader.value, '申报单数（单）', '涉及户数（户）', '公建设施数（个）', '申报金额（元）']
    } else if (isStatusGroup) {
      // 按审批状态：隐藏申报单数 + 隐藏申报金额
      headers = [groupHeader.value, '村组', '涉及户数（户）', '公建设施数（个）']
    } else {
      headers = [groupHeader.value, '村组', '申报单数（单）', '涉及户数（户）', '公建设施数（个）', '申报金额（元）']
    }
    const lines: string[] = [headers.map(csvField).join(',')]
    for (const r of rows.value) {
      const firstCol = isVillageGroup ? (r.villageName || r.groupLabel) : r.groupLabel
      let rowCells: (string | number | null | undefined)[]
      if (isVillageGroup) {
        rowCells = [firstCol, r.declarationCount, r.householdCount, r.facilityCount, r.amount]
      } else if (isStatusGroup) {
        rowCells = [firstCol, r.villageName, r.householdCount, r.facilityCount]
      } else {
        rowCells = [firstCol, r.villageName, r.declarationCount, r.householdCount, r.facilityCount, r.amount]
      }
      lines.push(rowCells.map(csvField).join(','))
    }
    lines.push('')
    lines.push('统计口径：申报次数按申报记录计数；涉及户数按到户申报的申请人去重，公建申报按公建申报的公共基础设施去重。')

    // UTF-8 BOM + CRLF，保证 Excel 双击能正确识别中文
    const bom = '\uFEFF'
    const csv = bom + lines.join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `申报综合查询_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

// ==================== 右键查看明细 ====================
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  row: null as DeclarationStatRow | null
})
const detailVisible = ref(false)
const detailTitle = ref('')
const detailRows = ref<DeclarationStatDetailItem[]>([])
const detailAmount = computed(() =>
  detailRows.value.reduce((sum, r) => sum + (Number(r.amount) || 0), 0)
)

/** 表格行右键：弹出"查看明细"菜单 */
function handleRowContextMenu(row: DeclarationStatRow, _column: unknown, event: MouseEvent) {
  event.preventDefault()
  contextMenu.row = row
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.visible = true
}

function closeContextMenu() {
  contextMenu.visible = false
}

/** 打开当前行的明细核对弹窗 */
function openDetail() {
  const row = contextMenu.row
  contextMenu.visible = false
  if (!row) return
  detailRows.value = row.details || []
  const village = row.villageName && row.villageName !== '全部村组' ? ` - ${row.villageName}` : ''
  detailTitle.value = `${row.groupLabel}${village} 明细核对`
  detailVisible.value = true
}

onMounted(async () => {
  await Promise.all([loadRegionTree(), handleQuery()])
  document.addEventListener('click', closeContextMenu)
  window.addEventListener('scroll', closeContextMenu, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeContextMenu)
  window.removeEventListener('scroll', closeContextMenu, true)
})
</script>

<style scoped>
.declaration-query {
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
.context-menu {
  position: fixed;
  z-index: 3000;
  min-width: 120px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  padding: 4px 0;
}
.context-menu-item {
  padding: 8px 16px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  white-space: nowrap;
}
.context-menu-item:hover {
  background: #f5f7fa;
  color: #409eff;
}
.detail-summary {
  margin-top: 10px;
  text-align: right;
  font-size: 13px;
  color: #606266;
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
.result-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
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
