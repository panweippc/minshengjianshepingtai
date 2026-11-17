<template>
  <div class="project-cycle-query">
    <!-- ========= 顶部筛选区 ========= -->
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
          <el-button type="success" @click="handleExport">导出Excel</el-button>
        </div>
      </div>
    </el-card>

    <!-- ========= 左侧方案面板 + 右侧主区域 ========= -->
    <div class="main-body">
      <!-- 左侧维度面板（和资金记录查询风格一致：可展开 section + 子项） -->
      <el-card shadow="never" class="sidebar">

        <!-- 方案1：按项目类别统计 -->
        <div class="filter-section">
          <div class="section-header">
            <span class="section-title-inline"
                  :class="{ 'section-title-inline--active': query.queryType === 'category' }"
                  @click="switchQueryTypeOnly('category')">按项目类别</span>
          </div>
          <div class="stat-scroll">
            <div
              v-for="item in sidebar.byCategory"
              :key="item.value"
              class="stat-item"
              :class="{
                active: query.queryType === 'category' && query.groupValue && item.value === query.groupValue,
                disabled: query.queryType !== 'category'
              }"
              @click="onSidebarItemClick('category', item.value)"
            >
              <span class="stat-label">{{ item.label }}</span>
              <span class="stat-count">{{ item.count }}</span>
            </div>
            <div v-if="sidebar.byCategory.length === 0" class="empty-hint">暂无数据</div>
          </div>
        </div>

        <el-divider class="sidebar-divider" />

        <!-- 方案2：按项目类型统计 -->
        <div class="filter-section">
          <div class="section-header">
            <span class="section-title-inline"
                  :class="{ 'section-title-inline--active': query.queryType === 'source' }"
                  @click="switchQueryTypeOnly('source')">按项目类型</span>
          </div>
          <div class="stat-scroll stat-scroll--tall">
            <div
              v-for="item in sidebar.bySource"
              :key="item.value"
              class="stat-item"
              :class="{
                active: query.queryType === 'source' && query.groupValue && item.value === query.groupValue,
                disabled: query.queryType !== 'source'
              }"
              @click="onSidebarItemClick('source', item.value)"
            >
              <span class="stat-label">{{ item.label }}</span>
              <span class="stat-count">{{ item.count }}</span>
            </div>
            <div v-if="sidebar.bySource.length === 0" class="empty-hint">暂无数据</div>
          </div>
        </div>

        <el-divider class="sidebar-divider" />

        <!-- 方案3：按立项状态统计 -->
        <div class="filter-section">
          <div class="section-header">
            <span class="section-title-inline"
                  :class="{ 'section-title-inline--active': query.queryType === 'establish' }"
                  @click="switchQueryTypeOnly('establish')">按立项状态</span>
          </div>
          <div class="stat-scroll">
            <div
              v-for="item in sidebar.byEstablish"
              :key="item.value"
              class="stat-item"
              :class="{
                active: query.queryType === 'establish' && query.groupValue && item.value === query.groupValue,
                disabled: query.queryType !== 'establish'
              }"
              @click="onSidebarItemClick('establish', item.value)"
            >
              <span class="stat-label">{{ item.label }}</span>
              <span class="stat-count">{{ item.count }}</span>
            </div>
            <div v-if="sidebar.byEstablish.length === 0" class="empty-hint">暂无数据</div>
          </div>
        </div>

        <el-divider class="sidebar-divider" />

        <!-- 方案4：按项目状态统计 -->
        <div class="filter-section">
          <div class="section-header">
            <span class="section-title-inline"
                  :class="{ 'section-title-inline--active': query.queryType === 'status' }"
                  @click="switchQueryTypeOnly('status')">按项目状态</span>
          </div>
          <div class="stat-scroll">
            <div
              v-for="item in sidebar.byStatus"
              :key="item.value"
              class="stat-item"
              :class="{
                active: query.queryType === 'status' && query.groupValue && item.value === query.groupValue,
                disabled: query.queryType !== 'status'
              }"
              @click="onSidebarItemClick('status', item.value)"
            >
              <span class="stat-label">{{ item.label }}</span>
              <span class="stat-count">{{ item.count }}</span>
            </div>
            <div v-if="sidebar.byStatus.length === 0" class="empty-hint">暂无数据</div>
          </div>
        </div>

        <el-divider class="sidebar-divider" />

        <!-- 方案5：按所属村组统计 -->
        <div class="filter-section">
          <div class="section-header">
            <span class="section-title-inline"
                  :class="{ 'section-title-inline--active': query.queryType === 'village' }"
                  @click="switchQueryTypeOnly('village')">按所属村组</span>
          </div>
          <div class="stat-scroll stat-scroll--village">
            <div
              v-for="item in sidebar.byVillage"
              :key="item.value"
              class="stat-item"
              :class="{
                active: query.queryType === 'village' && query.groupValue && item.value === query.groupValue,
                disabled: query.queryType !== 'village'
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

      <!-- 右侧主区域：主表格 -->
      <el-card shadow="never" class="result-card">
        <!-- 主统计表 -->
        <el-table
          :data="rows"
          v-loading="loading"
          border
          stripe
          :row-class-name="rowClassName"
          style="flex: 1"
          @row-contextmenu="onRowContextMenu"
          @row-click="closeContextMenu"
        >
          <el-table-column :label="groupHeader" min-width="140">
            <template #default="{ row }">
              <span :class="{
                'grand-total': row.grandTotal,
                'group-summary': row.groupSummary && !row.grandTotal
              }">{{ row.groupLabel }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="villageName" label="村组" min-width="140">
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">{{ row.villageName }}</span>
            </template>
          </el-table-column>

          <!-- 方案3（立项状态）和方案4（项目状态）专用表头 -->
          <template v-if="query.queryType === 'establish' || query.queryType === 'status'">
            <el-table-column prop="householdCount" label="涉及户数（个）" min-width="130" align="right">
              <template #default="{ row }">
                <span :class="{ 'grand-total': row.grandTotal }">{{ row.householdCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="facilityCount" label="公建设施数（个）" min-width="130" align="right">
              <template #default="{ row }">
                <span :class="{ 'grand-total': row.grandTotal }">{{ row.facilityCount }}</span>
              </template>
            </el-table-column>
          </template>

          <!-- 通用表头（方案1/2/4/5） -->
          <template v-else>
            <el-table-column prop="projectCount" label="项目数量（个）" min-width="130" align="right">
              <template #default="{ row }">
                <span :class="{ 'grand-total': row.grandTotal }">{{ row.projectCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="villageCoverage" label="覆盖村组（个）" min-width="130" align="right">
              <template #default="{ row }">
                <span :class="{ 'grand-total': row.grandTotal }">{{ row.villageCoverage }}</span>
              </template>
            </el-table-column>
            <el-table-column label="项目预算（元）" min-width="160" align="right">
              <template #default="{ row }">
                <span class="amount-cell" :class="{ 'grand-total': row.grandTotal }">
                  {{ formatMoney(row.budget) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="累计拨付（元）" min-width="160" align="right">
              <template #default="{ row }">
                <span class="amount-cell" :class="{ 'grand-total': row.grandTotal }">
                  {{ formatMoney(row.totalFund) }}
                </span>
              </template>
            </el-table-column>
          </template>
        </el-table>

        <!-- 统计口径说明 -->
        <div class="caliber-note">
          <el-icon><InfoFilled /></el-icon>
          <div class="caliber-text">
            <div>统计口径：各类显示小计，村组行显示该分类在对应村组的汇总结果；</div>
            <div v-if="query.queryType === 'village'">按所属村组统计时，项目数量为该村组关联项目ID去重后的数量；其余方案项目数量为项目记录条数；</div>
            <div v-else-if="query.queryType === 'establish'">按立项状态统计：待立项/已立项按立项日期筛选、已归档按归档时间筛选；仅统计涉及户数、公建设施数；</div>
            <div v-else-if="query.queryType === 'status'">按项目状态统计仅统计涉及户数、公建设施数；总计按当前筛选条件重新计算；</div>
            <div v-else>覆盖村组数量不做去重，同一村组在多个项目/多次申报中重复计入；</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 表格行右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="row-context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @click.stop
    >
      <div class="row-context-menu__item" @click="openDetail">
        <el-icon><Document /></el-icon>
        <span>查看明细</span>
      </div>
    </div>

    <!-- 明细核对弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="detailTitle"
      width="90%"
      top="6vh"
      append-to-body
      destroy-on-close
    >
      <el-table :data="detailRows" v-loading="detailLoading" border stripe max-height="560">
        <el-table-column prop="approvalCode" label="立项编号" min-width="150" />
        <el-table-column prop="projectName" label="项目名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="categoryLabel" label="项目类别" min-width="100" />
        <el-table-column prop="typeLabel" label="项目类型" min-width="110" />
        <el-table-column prop="villageName" label="村组" min-width="110" />
        <el-table-column label="项目预算（元）" min-width="140" align="right">
          <template #default="{ row }">{{ formatMoney(row.budget) }}</template>
        </el-table-column>
        <el-table-column prop="householdCount" label="涉及户数" min-width="90" align="right" />
        <el-table-column prop="facilityCount" label="公建设施数" min-width="110" align="right" />
        <el-table-column label="累计拨付（元）" min-width="140" align="right">
          <template #default="{ row }">{{ formatMoney(row.totalFund) }}</template>
        </el-table-column>
        <el-table-column prop="establishStatusLabel" label="立项状态" min-width="95" />
        <el-table-column prop="projectStatusLabel" label="项目状态" min-width="95" />
        <el-table-column prop="submitTime" label="立项日期" min-width="160" />
        <el-table-column prop="archiveCreateTime" label="归档时间" min-width="160" />
      </el-table>
      <div class="detail-tip">
        共 {{ detailRows.length }} 条申报单记录；同一立项单存在多条申报单（到户/公建）时会分行列出。
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, InfoFilled } from '@element-plus/icons-vue'
import { getRegionList } from '@/api/region'
import {
  getProjectCycleStat, getProjectCycleDetail, downloadProjectCycleCsv,
  type CycleRow, type CycleDetailRow, type QueryType
} from '@/api/projectCycleQuery'

// ===================== 区划树 =====================
interface RegionNode { code: string; name: string; parent_code?: string; children?: RegionNode[] }
const regionTree = ref<RegionNode[]>([])

async function loadRegionTree() {
  try {
    const res: any = await getRegionList()
    const list: RegionNode[] = res?.data ?? res ?? []
    const parents = new Map<string, RegionNode>()
    for (const r of list) {
      parents.set(r.code, { code: r.code, name: r.name })
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
  /** 主分组维度 */
  queryType: 'category' as QueryType,
  /** 左侧面板子项点击后的二级过滤 */
  groupValue: '' as string
})

const loading = ref(false)
const rows = ref<CycleRow[]>([])
const globalSummary = ref<{
  projectCount: number; declareCount: number; villageCoverage: number;
  totalBudget: number | string; totalFund: number | string
}>({
  projectCount: 0,
  declareCount: 0,
  villageCoverage: 0,
  totalBudget: 0,
  totalFund: 0
})

// ===================== 左侧面板数据 =====================
interface SidebarItem { value: string; label: string; count: number }
const sidebar = ref<{
  byCategory:   SidebarItem[]
  bySource:     SidebarItem[]
  byEstablish:  SidebarItem[]
  byStatus:     SidebarItem[]
  byVillage:    SidebarItem[]
}>({
  byCategory: [], bySource: [], byEstablish: [], byStatus: [], byVillage: []
})

// ===================== 计算属性 =====================

/** 动态表头第一列 */
const groupHeader = computed(() => {
  const m: Record<QueryType, string> = {
    category: '项目类别',
    source: '项目类型',
    establish: '立项状态',
    status: '项目状态',
    village: '所属村组'
  }
  return m[query.queryType] || '分组字段'
})

function rowClassName({ row }: { row: CycleRow }) {
  if (row.grandTotal) return 'row-grand-total'
  if (row.groupSummary) return 'row-group-summary'
  return ''
}

// ===================== 左侧面板交互 =====================

/** 点击 section 标题：切 queryType + 清空 groupValue + 重新查询 */
function switchQueryTypeOnly(newType: QueryType) {
  query.queryType = newType
  query.groupValue = ''
  handleQuery()
}

/** 点击左侧面板子项 */
function onSidebarItemClick(groupType: QueryType, value: string) {
  if (query.queryType !== groupType) {
    // 切到该方案 + 选中该子项
    query.queryType = groupType
    query.groupValue = query.groupValue === value ? '' : value
  } else {
    // 同方案 toggle
    query.groupValue = query.groupValue === value ? '' : value
  }
  handleQuery()
}

function clearSidebarFilter() {
  query.groupValue = ''
  handleQuery()
}

// ===================== 右键查看明细 =====================
const contextMenu = reactive({ visible: false, x: 0, y: 0 })
const contextRow = ref<CycleRow | null>(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRows = ref<CycleDetailRow[]>([])
const detailTitle = ref('查看明细')

function onRowContextMenu(row: CycleRow, _column: unknown, event: MouseEvent) {
  event.preventDefault()
  contextRow.value = row
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.visible = true
}

function closeContextMenu() {
  contextMenu.visible = false
}

/** 查看当前统计行对应的原始申报单记录 */
async function openDetail() {
  const row = contextRow.value
  closeContextMenu()
  if (!row) return

  // 合计行不限分组/村组；大分组小计行只限分组；村组行分组+村组都限
  const groupValue = row.grandTotal ? undefined : (row.groupValue || undefined)
  const villageName = row.grandTotal || row.groupSummary ? undefined : (row.villageName || undefined)

  detailTitle.value = row.grandTotal
    ? `查看明细 - ${row.groupLabel}`
    : `查看明细 - ${row.groupLabel} / ${row.villageName}`
  detailVisible.value = true
  detailLoading.value = true
  detailRows.value = []
  try {
    const res = await getProjectCycleDetail({
      year: query.year || undefined,
      regionCode: query.regionCode || undefined,
      queryType: query.queryType,
      groupValue,
      villageName
    })
    detailRows.value = (res?.data ?? (res as any)) ?? []
  } catch (e: any) {
    ElMessage.error('明细加载失败：' + (e?.message || '未知错误'))
  } finally {
    detailLoading.value = false
  }
}

// ===================== 主查询 =====================

async function handleQuery() {
  query.regionCode = query.regionCodePath.length > 0 ? query.regionCodePath[query.regionCodePath.length - 1] : ''
  loading.value = true
  try {
    const res = await getProjectCycleStat({
      year: query.year || undefined,
      regionCode: query.regionCode || undefined,
      queryType: query.queryType
    })
    const data = res?.data ?? (res as any)
    rows.value = data.rows ?? []
    if (data.globalSummary) globalSummary.value = data.globalSummary
    // 同时刷新左侧面板 5 个方案的子项
    buildSidebarFromStat()
  } catch (e: any) {
    ElMessage.error('查询失败：' + (e?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

/**
 * 左侧面板 5 个 section 全部从「当前筛选条件下、不带 groupValue 过滤」的
 * 独立 stat 结果里抽取 groupSummary=true 的大分组汇总行，
 * 每行提取 groupValue + groupLabel + projectCount（子项显示用）。
 */
async function buildSidebarFromStat() {
  const setOne = (type: QueryType, list: SidebarItem[]) => {
    if (type === 'category')   sidebar.value.byCategory   = list
    if (type === 'source')     sidebar.value.bySource     = list
    if (type === 'establish')  sidebar.value.byEstablish  = list
    if (type === 'status')     sidebar.value.byStatus     = list
    if (type === 'village')    sidebar.value.byVillage    = list
  }
  const allTypes: QueryType[] = ['category', 'source', 'establish', 'status', 'village']
  for (const t of allTypes) {
    try {
      const r = await getProjectCycleStat({
        year: query.year || undefined,
        regionCode: query.regionCode || undefined,
        queryType: t
      })
      const data = r?.data ?? (r as any)
      // 只抽 groupSummary=true 且 !grandTotal 的大分组汇总行
      const bigRows: CycleRow[] = (data.rows ?? []).filter(
        (row: CycleRow) => row.groupSummary && !row.grandTotal
      )
      const items: SidebarItem[] = bigRows.map(s => ({
        value: s.groupValue,
        label: s.groupLabel,
        // 方案3/4 用涉及户数显示计数，其他用项目数量
        count: (t === 'establish' || t === 'status') ? (s.householdCount ?? 0) : (s.projectCount ?? 0)
      }))
      setOne(t, items)
    } catch { /* 降级：保留旧值 */ }
  }
}

function handleExport() {
  query.regionCode = query.regionCodePath.length > 0 ? query.regionCodePath[query.regionCodePath.length - 1] : ''
  downloadProjectCycleCsv({
    year: query.year || undefined,
    regionCode: query.regionCode || undefined,
    queryType: query.queryType
  })
}

function resetQuery() {
  query.year = ''
  query.regionCodePath = []
  query.regionCode = ''
  query.queryType = 'category'
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
  window.addEventListener('click', closeContextMenu)
  await Promise.all([loadRegionTree(), handleQuery()])
})

onBeforeUnmount(() => {
  window.removeEventListener('click', closeContextMenu)
})
</script>

<style scoped>
.project-cycle-query { padding: 0; }
.top-filter { margin-bottom: 12px; }
.filter-row { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
.filter-item { display: flex; align-items: center; gap: 8px; }
.filter-item--btns { margin-left: auto; }
.filter-label { font-size: 13px; color: #606266; white-space: nowrap; }

.main-body { display: flex; gap: 12px; height: calc(100vh - 260px); min-height: 520px; }
.sidebar { flex: 0 0 240px; overflow-y: auto; }
.sidebar > :deep(.el-card__body) { padding: 12px 14px 10px; display: flex; flex-direction: column; gap: 4px; }

.filter-section { margin-bottom: 4px; }
.section-header { margin-bottom: 6px; display: flex; justify-content: space-between; align-items: center; }
.section-title-inline { font-size: 13px; color: #606266; font-weight: 500; margin-left: 4px; cursor: pointer; user-select: none; }
.section-title-inline:hover { color: #409eff; }
.section-title-inline--active { color: #409eff; }

.sidebar-divider { margin: 8px 0; }

.stat-scroll { max-height: 120px; overflow-y: auto; }
.stat-scroll--tall { max-height: 160px; }
.stat-scroll--village { max-height: 240px; }

.stat-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 10px; margin-bottom: 4px;
  cursor: pointer; border-radius: 4px;
  font-size: 13px; color: #606266; transition: background 0.15s;
}
.stat-item:hover { background: #f5f7fa; }
.stat-item.active { background: #ecf5ff; color: #409eff; font-weight: 500; }
.stat-item.disabled { color: #909399; }
.stat-label { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.stat-count { flex: 0 0 auto; margin-left: 8px; color: #c0c4cc; font-size: 12px; font-variant-numeric: tabular-nums; }
.stat-item.active .stat-count { color: #409eff; }

.empty-hint { font-size: 12px; color: #c0c4cc; padding: 6px 10px; text-align: center; }

.sidebar-actions { padding-top: 8px; border-top: 1px solid #ebeef5; margin-top: 4px; text-align: center; }

.result-card { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; padding-bottom: 10px; }
.result-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; overflow: hidden; padding-bottom: 10px; }

/* 汇总条 */
.summary-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 4px 4px 12px; margin-bottom: 0; gap: 16px; flex-wrap: wrap;
}
.summary-title { font-size: 14px; color: #606266; font-weight: 500; }
.summary-stats { display: flex; gap: 16px; flex-wrap: wrap; }
.stat-chip {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 12px; background: #f5f7fa; border-radius: 4px;
  font-size: 13px;
}
.stat-chip-label { color: #909399; }
.stat-chip-value { color: #409eff; font-weight: 600; font-variant-numeric: tabular-nums; }
.stat-chip--amount .stat-chip-value { color: #52c41a; }

/* 行类型样式 */
:deep(.row-grand-total) { background: #fdf6ec !important; font-weight: 600; }
:deep(.row-grand-total td.el-table__cell),
:deep(.row-grand-total td.el-table__cell .cell) { background: #fdf6ec !important; }
:deep(.row-group-summary) { background: #fafafa !important; }
:deep(.row-group-summary td.el-table__cell),
:deep(.row-group-summary td.el-table__cell .cell) { background: #fafafa !important; font-weight: 500; }

.grand-total { font-weight: 700; color: #303133; }
.group-summary { font-weight: 600; color: #303133; }

.amount-cell { color: #52c41a; font-variant-numeric: tabular-nums; }
.amount-cell.grand-total { color: #52c41a; font-weight: 700; }

.caliber-note {
  margin-top: 12px; padding: 10px 14px;
  background: #f4f5f5; border-radius: 4px;
  font-size: 12px; color: #909399;
  display: flex; align-items: flex-start; gap: 8px; line-height: 1.6;
}
.caliber-text { display: flex; flex-direction: column; gap: 2px; }

/* 右键菜单 */
.row-context-menu {
  position: fixed;
  z-index: 3000;
  min-width: 132px;
  padding: 4px 0;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
}
.row-context-menu__item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; font-size: 13px; color: #303133;
  cursor: pointer;
}
.row-context-menu__item:hover { background: #ecf5ff; color: #409eff; }

.detail-tip { margin-top: 8px; font-size: 12px; color: #909399; }
</style>
