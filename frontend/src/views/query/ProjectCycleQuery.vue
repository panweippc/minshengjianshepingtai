<template>
  <div class="project-cycle-query">
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
          <el-button icon="Download" @click="handleExport">导出Excel</el-button>
        </div>
      </div>
    </el-card>

    <!-- ========= 左侧方案面板 + 右侧主区域 ========= -->
    <div class="main-body">
      <!-- 左侧维度面板 -->
      <el-card shadow="never" class="sidebar">
        <template #header>
          <div class="sidebar-title">查询方案</div>
        </template>

        <!-- 方案1：按项目类别统计 -->
        <div class="filter-section">
          <div class="section-header">
            <el-radio :model-value="query.queryType" label="category" @change="switchQueryTypeOnly('category')">
              <span class="section-title-inline">按项目类别统计</span>
            </el-radio>
          </div>
          <div class="section-sub">
            <span>项目类别（{{ sidebar.byCategory.length }}）</span>
          </div>
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
            <span class="stat-check" />
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">{{ item.count }}</span>
          </div>
          <div v-if="sidebar.byCategory.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <!-- 方案2：按项目类型统计 -->
        <div class="filter-section">
          <div class="section-header">
            <el-radio :model-value="query.queryType" label="source" @change="switchQueryTypeOnly('source')">
              <span class="section-title-inline">按项目类型统计</span>
            </el-radio>
          </div>
          <div class="section-sub">
            <span>项目类型（{{ sidebar.bySource.length }}）</span>
          </div>
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
            <span class="stat-check" />
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">{{ item.count }}</span>
          </div>
          <div v-if="sidebar.bySource.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <!-- 方案3：按立项状态统计 -->
        <div class="filter-section">
          <div class="section-header">
            <el-radio :model-value="query.queryType" label="establish" @change="switchQueryTypeOnly('establish')">
              <span class="section-title-inline">按立项状态统计</span>
            </el-radio>
          </div>
          <div class="section-sub">
            <span>立项状态（{{ sidebar.byEstablish.length }}）</span>
          </div>
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
            <span class="stat-check" />
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">{{ item.count }}</span>
          </div>
          <div v-if="sidebar.byEstablish.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <!-- 方案4：按项目状态统计 -->
        <div class="filter-section">
          <div class="section-header">
            <el-radio :model-value="query.queryType" label="status" @change="switchQueryTypeOnly('status')">
              <span class="section-title-inline">按项目状态统计</span>
            </el-radio>
          </div>
          <div class="section-sub">
            <span>项目状态（{{ sidebar.byStatus.length }}）</span>
          </div>
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
            <span class="stat-check" />
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">{{ item.count }}</span>
          </div>
          <div v-if="sidebar.byStatus.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <!-- 方案5：按所属村组统计 -->
        <div class="filter-section">
          <div class="section-header">
            <el-radio :model-value="query.queryType" label="village" @change="switchQueryTypeOnly('village')">
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
                active: query.queryType === 'village' && query.groupValue && item.value === query.groupValue,
                disabled: query.queryType !== 'village'
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
            <div class="record-badge">共 {{ globalSummary.declareCount }} 条源记录</div>
          </div>
        </template>

        <el-table
          :data="rows"
          v-loading="loading"
          :row-class-name="rowClassName"
          @row-contextmenu="onRowContextMenu"
          @row-click="closeContextMenu"
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

          <el-table-column prop="villageName" label="村组" min-width="100">
            <template #default="{ row }">
              <span :class="{ 'grand-total': row.grandTotal }">{{ row.villageName }}</span>
            </template>
          </el-table-column>

          <!-- 方案3（立项状态）和方案4（项目状态）专用表头 -->
          <template v-if="query.queryType === 'establish' || query.queryType === 'status'">
            <el-table-column prop="householdCount" label="涉及户数（个）" min-width="110" align="right">
              <template #default="{ row }">
                <span class="num-text" :class="{ 'grand-total': row.grandTotal }">{{ row.householdCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="facilityCount" label="公建设施数（个）" min-width="120" align="right">
              <template #default="{ row }">
                <span class="num-text" :class="{ 'grand-total': row.grandTotal }">{{ row.facilityCount }}</span>
              </template>
            </el-table-column>
          </template>

          <!-- 通用表头（方案1/2/5） -->
          <template v-else>
            <el-table-column prop="projectCount" label="项目数量（个）" min-width="110" align="right">
              <template #default="{ row }">
                <span class="num-text" :class="{ 'grand-total': row.grandTotal }">{{ row.projectCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="villageCoverage" label="覆盖村组（个）" min-width="120" align="right">
              <template #default="{ row }">
                <span class="num-text" :class="{ 'grand-total': row.grandTotal }">{{ row.villageCoverage }}</span>
              </template>
            </el-table-column>
            <el-table-column label="项目预算（元）" min-width="130" align="right">
              <template #default="{ row }">
                <span class="amount-text" :class="{ 'grand-total': row.grandTotal }">
                  {{ formatMoney(row.budget) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="累计拨付（元）" min-width="130" align="right">
              <template #default="{ row }">
                <span class="amount-text" :class="{ 'grand-total': row.grandTotal }">
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

/** 区划名称（选中区划或全部区划） */
const regionLabel = computed(() => {
  if (!query.regionCode) return '全部区划'
  function walk(nodes: RegionNode[] | undefined, target: string): string {
    if (!nodes) return ''
    for (const n of nodes) {
      if (n.code === target) return n.name
      const sub = walk(n.children, target)
      if (sub) return sub
    }
    return ''
  }
  return walk(regionTree.value, query.regionCode) || '全部区划'
})

/** 结果区副标题：年度 · 区划 · 统计结果 */
const resultSubtitle = computed(
  () => `${query.year ? query.year + '年' : '全部年度'} · ${regionLabel.value} · 统计结果`
)

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
        // sidebar 子项统一显示 projectCount（approval 粒度项目数），
        // householdCount/facilityCount 留给主表格列用
        count: s.projectCount ?? 0
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
.project-cycle-query {
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

/* 行类型样式 */
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
  align-items: flex-start;
  gap: 8px;
  line-height: 1.6;
}
.caliber-text { display: flex; flex-direction: column; gap: 2px; }

/* 右键菜单 */
.row-context-menu {
  position: fixed;
  z-index: 3000;
  min-width: 132px;
  padding: 6px 0;
  background: #fff;
  border: 1px solid #eef1f6;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.12);
}
.row-context-menu__item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; font-size: 13px; color: #3a4557;
  cursor: pointer;
}
.row-context-menu__item:hover { background: #eef4ff; color: #2f6bff; }

.detail-tip { margin-top: 8px; font-size: 12px; color: #909399; }
</style>
