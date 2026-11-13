<template>
  <div class="declaration-query">
    <!-- ==================== 顶部筛选区 ==================== -->
    <el-card shadow="never" class="top-filter">
      <div class="filter-row">
        <div class="filter-left">
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
              :props="cascaderProps"
              placeholder="请选择区划"
              clearable
              style="width: 220px"
            />
          </div>
        </div>
        <div class="filter-right">
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button type="success" icon="Download" :loading="exporting" @click="handleExport">导出Excel</el-button>
        </div>
      </div>
    </el-card>

    <!-- ==================== 主体：左侧筛选面板 + 右侧表格 ==================== -->
    <div class="main-body">
      <!-- 左侧筛选面板 -->
      <el-card shadow="never" class="sidebar">
        <template #header>
          <div class="sidebar-title">统计筛选</div>
        </template>

        <div class="filter-section">
          <div class="section-header">
            <el-radio
              :model-value="query.groupBy"
              label="category"
              @change="onGroupByChange('category')"
            >
              <span class="section-title-inline">按申报类别</span>
            </el-radio>
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
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">{{ item.count }}</span>
          </div>
          <div v-if="sidebar.byCategory.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <el-divider class="sidebar-divider" />

        <div class="filter-section">
          <div class="section-header">
            <el-radio
              :model-value="query.groupBy"
              label="status"
              @change="onGroupByChange('status')"
            >
              <span class="section-title-inline">按审批状态</span>
            </el-radio>
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
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">{{ item.count }}</span>
          </div>
          <div v-if="sidebar.byStatus.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <el-divider class="sidebar-divider" />

        <div class="filter-section">
          <div class="section-header">
            <el-radio
              :model-value="query.groupBy"
              label="village"
              @change="onGroupByChange('village')"
            >
              <span class="section-title-inline">按所属村组</span>
            </el-radio>
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
        <el-table
          v-loading="loading"
          :data="rows"
          border
          :row-class-name="rowClassName"
          :span-method="objectSpanMethod"
        >
          <el-table-column
            :label="groupHeader"
            min-width="140"
          >
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal, 'group-summary': row.groupSummary && !row.grandTotal }">
                {{ query.groupBy === 'village' ? (row.villageName || row.groupLabel) : row.groupLabel }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="query.groupBy !== 'village'"
            prop="villageName"
            label="村组"
            min-width="140"
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
            min-width="130"
            align="right"
          >
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">{{ row.declarationCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="householdCount" label="涉及户数（户）" min-width="130" align="right">
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">{{ row.householdCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="facilityCount" label="公建设施数（个）" min-width="140" align="right">
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">{{ row.facilityCount }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="query.groupBy !== 'status'"
            label="申报金额（元）"
            min-width="160"
            align="right"
          >
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">
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
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { getRegionList } from '@/api/region'
import {
  getDeclarationStatistics,
  type DeclarationStatRow,
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
  villageGroupCode: '' as string | null
})

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
      villageGroupCode: query.villageGroupCode || undefined
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

// ==================== 生命周期 ====================
onMounted(async () => {
  await Promise.all([loadRegionTree(), handleQuery()])
})
</script>

<style scoped>
.declaration-query {
  padding: 0;
}
.top-filter {
  margin-bottom: 12px;
}
.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.filter-right {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.main-body {
  display: flex;
  gap: 12px;
  height: calc(100vh - 160px);
  min-height: 520px;
}

/* 左侧筛选面板 */
.sidebar {
  flex: 0 0 260px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}
.sidebar :deep(.el-card__body) {
  padding: 12px 14px 10px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.sidebar-title {
  font-size: 14px;
  font-weight: 600;
}
.filter-section {
  margin-bottom: 4px;
}
.section-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
  font-weight: 500;
}
.section-header {
  margin-bottom: 6px;
}
.section-title-inline {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  margin-left: 4px;
}
.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  margin-bottom: 4px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 13px;
  color: #303133;
  transition: background 0.15s;
}
.stat-item:hover {
  background: #f5f7fa;
}
.stat-item.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 500;
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
  margin-left: 8px;
  color: #909399;
}
.stat-item.active .stat-count {
  color: #409eff;
}
.stat-item.disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.stat-item.disabled:hover {
  background: transparent;
}
.stat-scroll {
  max-height: 260px;
  overflow-y: auto;
}
.empty-hint {
  font-size: 12px;
  color: #c0c4cc;
  padding: 6px 10px;
}
.sidebar-divider {
  margin: 10px 0;
}
.sidebar-actions {
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
  margin-top: 6px;
  text-align: center;
}

/* 右侧表格区 */
.result-card {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.result-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-bottom: 10px;
}
.result-card :deep(.el-table) {
  flex: 1;
}

/* 行样式 */
.row-grand-total {
  background: #f0f4fa !important;
  font-weight: 600;
}
.row-group-summary {
  background: #fafbfc !important;
  font-weight: 500;
}
.grand-total {
  font-weight: 600;
  color: #303133;
}
.group-summary {
  font-weight: 500;
  color: #303133;
}

/* 口径说明 */
.caliber-note {
  margin-top: 12px;
  padding: 8px 12px;
  background: #f4f4f5;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.6;
}
</style>
