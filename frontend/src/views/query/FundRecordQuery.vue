<template>
  <div class="fund-record-query">
    <!-- ==================== 顶部筛选区 ==================== -->
    <el-card shadow="never" class="top-filter">
      <div class="filter-row">
        <div class="filter-left">
          <div class="filter-item">
            <span class="filter-label">项目名称</span>
            <el-input
              v-model="query.projectName"
              placeholder="模糊"
              clearable
              style="width: 150px"
              @keyup.enter="handleQuery"
            />
          </div>
          <div class="filter-item">
            <span class="filter-label">项目编号</span>
            <el-input
              v-model="query.approvalCode"
              placeholder="模糊"
              clearable
              style="width: 150px"
              @keyup.enter="handleQuery"
            />
          </div>
          <div class="filter-item">
            <span class="filter-label">申报编号</span>
            <el-input
              v-model="query.declareCode"
              placeholder="模糊"
              clearable
              style="width: 150px"
              @keyup.enter="handleQuery"
            />
          </div>
          <div class="filter-item">
            <span class="filter-label">明细名称</span>
            <el-input
              v-model="query.detailName"
              placeholder="农户/设施"
              clearable
              style="width: 150px"
              @keyup.enter="handleQuery"
            />
          </div>
          <div class="filter-item">
            <span class="filter-label">层级</span>
            <el-select v-model="query.nodeLevel" placeholder="全部" clearable style="width: 120px">
              <el-option label="1 资金登记" :value="1" />
              <el-option label="2 二级下拨" :value="2" />
              <el-option label="3 三级下拨" :value="3" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">资金类型</span>
            <el-select v-model="query.fundType" placeholder="全部" clearable style="width: 140px">
              <el-option
                v-for="item in dictOptions('zi_jin_lei_xing')"
                :key="item.dictValue"
                :label="'[登记] ' + item.dictLabel"
                :value="item.dictValue"
              />
              <el-option-group label="二级/三级下拨">
                <el-option
                  v-for="item in dictOptions('xia_bo_lei_xing')"
                  :key="item.dictValue"
                  :label="item.dictLabel"
                  :value="item.dictValue"
                />
              </el-option-group>
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">资金来源</span>
            <el-select
              v-model="query.fundSource"
              placeholder="全部"
              clearable
              filterable
              style="width: 150px"
            >
              <el-option
                v-for="item in dictOptions('zi_jin_lai_yuan')"
                :key="item.dictValue"
                :label="item.dictLabel"
                :value="item.dictValue"
              />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">区划</span>
            <el-cascader
              v-model="query.regionCodePath"
              :options="regionTree"
              :props="cascaderProps"
              placeholder="请选择"
              clearable
              style="width: 220px"
            />
          </div>
          <div class="filter-item">
            <span class="filter-label">经办时间</span>
            <el-date-picker
              v-model="query.timeRange"
              type="datetimerange"
              value-format="YYYY-MM-DD HH:mm:ss"
              range-separator="~"
              start-placeholder="起"
              end-placeholder="止"
              style="width: 360px"
            />
          </div>
          <div class="filter-item">
            <span class="filter-label">经办人</span>
            <el-input
              v-model="query.createBy"
              placeholder="精确"
              clearable
              style="width: 120px"
              @keyup.enter="handleQuery"
            />
          </div>
        </div>
        <div class="filter-right">
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button type="success" icon="Download" :loading="exporting" @click="handleExport">导出CSV</el-button>
        </div>
      </div>
    </el-card>

    <!-- ==================== 汇总卡 ==================== -->
    <el-card shadow="never" class="summary-card">
      <div class="summary-row">
        <div class="summary-cell">
          <div class="summary-value">{{ summary.recordCount }}</div>
          <div class="summary-label">资金记录数（条）</div>
        </div>
        <div class="summary-cell">
          <div class="summary-value">{{ formatMoney(summary.registerTotal) }}</div>
          <div class="summary-label">登记合计（元）</div>
        </div>
        <div class="summary-cell">
          <div class="summary-value">{{ formatMoney(summary.allocateTotal) }}</div>
          <div class="summary-label">下拨合计（元）</div>
        </div>
        <div class="summary-cell">
          <div class="summary-value">{{ summary.projectCount }}</div>
          <div class="summary-label">涉及项目数</div>
        </div>
        <div class="summary-cell">
          <div class="summary-value">{{ summary.declareCount }}</div>
          <div class="summary-label">涉及申报数</div>
        </div>
        <div class="summary-cell">
          <div class="summary-value">{{ summary.detailCount }}</div>
          <div class="summary-label">涉及明细数</div>
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
              label="fundType"
              @change="onGroupByChange('fundType')"
            >
              <span class="section-title-inline">按资金类型</span>
            </el-radio>
          </div>
          <div
            v-for="item in sidebar.byFundType"
            :key="item.value"
            class="stat-item"
            :class="{
              active: query.groupBy === 'fundType' && query.fundType === item.value,
              disabled: query.groupBy !== 'fundType'
            }"
            @click="onSidebarItemClick('fundType', 'fundType', item.value)"
          >
            <span class="stat-label">{{ item.label || item.value }}</span>
            <span class="stat-count">
              {{ item.count }} / {{ formatMoney(item.amount) }}
            </span>
          </div>
          <div v-if="sidebar.byFundType.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <el-divider class="sidebar-divider" />

        <div class="filter-section">
          <div class="section-header">
            <el-radio
              :model-value="query.groupBy"
              label="fundSource"
              @change="onGroupByChange('fundSource')"
            >
              <span class="section-title-inline">按资金来源</span>
            </el-radio>
          </div>
          <div class="stat-scroll">
            <div
              v-for="item in sidebar.byFundSource"
              :key="item.value"
              class="stat-item"
              :class="{
                active: query.groupBy === 'fundSource' && query.fundSource === item.value,
                disabled: query.groupBy !== 'fundSource'
              }"
              @click="onSidebarItemClick('fundSource', 'fundSource', item.value)"
            >
              <span class="stat-label">{{ item.label || item.value }}</span>
              <span class="stat-count">
                {{ item.count }} / {{ formatMoney(item.amount) }}
              </span>
            </div>
          </div>
          <div v-if="sidebar.byFundSource.length === 0" class="empty-hint">暂无数据</div>
        </div>

        <el-divider class="sidebar-divider" />

        <div class="filter-section">
          <div class="section-header">
            <el-radio
              :model-value="query.groupBy"
              label="nodeLevel"
              @change="onGroupByChange('nodeLevel')"
            >
              <span class="section-title-inline">按节点层级</span>
            </el-radio>
          </div>
          <div
            v-for="item in sidebar.byNodeLevel"
            :key="item.value"
            class="stat-item"
            :class="{
              active: query.groupBy === 'nodeLevel' && String(query.nodeLevel) === item.value,
              disabled: query.groupBy !== 'nodeLevel'
            }"
            @click="onSidebarItemClick('nodeLevel', 'nodeLevel', item.value)"
          >
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-count">
              {{ item.count }} / {{ formatMoney(item.amount) }}
            </span>
          </div>
          <div v-if="sidebar.byNodeLevel.length === 0" class="empty-hint">暂无数据</div>
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
                active: query.groupBy === 'village' && query.villageNameFilter === item.value,
                disabled: query.groupBy !== 'village'
              }"
              @click="onSidebarItemClick('villageNameFilter', 'village', item.value)"
            >
              <span class="stat-label">{{ item.label }}</span>
              <span class="stat-count">
                {{ item.count }} / {{ formatMoney(item.amount) }}
              </span>
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
          stripe
          size="default"
        >
          <el-table-column label="层级" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                size="small"
                :type="nodeLevelTagType(row.nodeLevel)"
                effect="light"
              >
                {{ row.nodeLevelLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="approvalCode" label="项目编号" width="200" show-overflow-tooltip />
          <el-table-column prop="projectName" label="项目名称" min-width="220" show-overflow-tooltip />
          <el-table-column prop="projectCategoryLabel" label="项目类别" width="120" />
          <el-table-column prop="projectTypeLabel" label="项目类型" width="120" />
          <el-table-column prop="declareCode" label="申报编号" width="180">
            <template #default="{ row }">{{ row.declareCode || '-' }}</template>
          </el-table-column>
          <el-table-column prop="declareName" label="申报标题" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.declareName || '-' }}</template>
          </el-table-column>
          <el-table-column label="明细" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <template v-if="row.detailName">
                <el-tag
                  v-if="row.detailType === 'HOUSEHOLD'"
                  size="small"
                  type="primary"
                  style="margin-right: 4px"
                >农户</el-tag>
                <el-tag
                  v-else-if="row.detailType === 'FACILITY'"
                  size="small"
                  type="success"
                  style="margin-right: 4px"
                >设施</el-tag>
                {{ row.detailName }}
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="fundTypeLabel" label="资金类型" width="130">
            <template #default="{ row }">{{ row.fundTypeLabel || '-' }}</template>
          </el-table-column>
          <el-table-column prop="fundSourceLabel" label="资金来源" width="140">
            <template #default="{ row }">{{ row.fundSourceLabel || '-' }}</template>
          </el-table-column>
          <el-table-column label="金额（元）" width="140" align="right">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="townName" label="乡镇" width="120">
            <template #default="{ row }">{{ row.townName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="villageName" label="村组" width="140">
            <template #default="{ row }">{{ row.villageName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createBy" label="经办人" width="100">
            <template #default="{ row }">{{ row.createBy || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="经办时间" width="165" />
          <el-table-column prop="remark" label="拨付说明" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
          <el-table-column label="附件" width="90" align="center">
            <template #default="{ row }">
              <template v-if="row.attachCount > 0">
                <el-link type="primary" :underline="false" @click="openDetail(row)">
                  {{ row.attachCount }} 份
                </el-link>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="handleQuery"
          @current-change="loadData"
        />

        <div class="caliber-note">
          <el-icon><InfoFilled /></el-icon>
          <span>
            统计口径：资金记录按单条流水计数；登记合计指 nodeLevel=1 的金额合计；下拨合计指 nodeLevel∈{2,3} 的金额合计；
            涉及申报数不区分层级，按 declare_id 去重；区划以明细末级村组为准（nodeLevel=1/2 时回退到申报的村组）。
          </span>
        </div>
      </el-card>
    </div>

    <!-- 查看弹窗：复用资金管理详情（按 nodeLevel 决定显示的详情类型） -->
    <el-dialog
      v-model="detailVisible"
      :title="detailTitle"
      width="900px"
      destroy-on-close
    >
      <FundDetailDialog
        v-if="detailRow"
        :node-type="detailRow.nodeLevel === 1 ? 'approval' : detailRow.nodeLevel === 2 ? 'declare' : 'detail'"
        :node-id="(detailRow.nodeLevel === 1 ? detailRow.approvalId : detailRow.nodeLevel === 2 ? detailRow.declareId : detailRow.detailId) || ''"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { getRegionList } from '@/api/region'
import { useDict } from '@/composables/useDict'
import { getFundQueryPage, downloadFundQueryExport } from '@/api/fundQuery'
import FundDetailDialog from '@/components/FundDetailDialog.vue'
import type {
  FundRecordRow,
  FundQuerySummary,
  FundQuerySidebar
} from '@/api/fundQuery'

const { dictOptions } = useDict([
  'zi_jin_lei_xing',
  'xia_bo_lei_xing',
  'zi_jin_lai_yuan',
  'xiang_mu_lei_bie',
  'xiang_mu_lei_xing'
])

// ==================== 顶部筛选 ====================
const query = reactive({
  projectName: '' as string,
  approvalCode: '' as string,
  declareCode: '' as string,
  detailName: '' as string,
  nodeLevel: null as null | 1 | 2 | 3,
  fundType: '' as string,
  fundSource: '' as string,
  regionCodePath: [] as string[],
  regionCode: '' as string,
  timeRange: null as [string, string] | null,
  startTime: '' as string,
  endTime: '' as string,
  createBy: '' as string,
  // 本地 sidebar 筛选状态（不是顶层筛选）
  groupBy: 'fundType' as 'fundType' | 'fundSource' | 'nodeLevel' | 'village',
  villageNameFilter: '' as string
})

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

async function loadRegionTree() {
  try {
    const res: any = await getRegionList()
    const list: any[] = res.data || []
    const byCode = new Map<string, RegionNode>()
    const roots: RegionNode[] = []
    for (const r of list) {
      byCode.set(r.code, { value: r.code, label: r.name })
    }
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
    regionTree.value = roots
  } catch {
    regionTree.value = []
  }
}

// ==================== 分页与汇总 ====================
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const rows = ref<FundRecordRow[]>([])
const summary = ref<FundQuerySummary>({
  recordCount: 0,
  registerTotal: 0,
  allocateTotal: 0,
  projectCount: 0,
  declareCount: 0,
  detailCount: 0
})
const sidebar = ref<FundQuerySidebar>({
  byFundType: [],
  byFundSource: [],
  byNodeLevel: [],
  byVillage: []
})

const hasSidebarFilter = computed(
  () =>
    !!query.fundType ||
    !!query.fundSource ||
    query.nodeLevel !== null ||
    !!query.villageNameFilter
)

function onGroupByChange(dim: 'fundType' | 'fundSource' | 'nodeLevel' | 'village') {
  query.groupBy = dim
  if (dim !== 'fundType') query.fundType = ''
  if (dim !== 'fundSource') query.fundSource = ''
  if (dim !== 'nodeLevel') query.nodeLevel = null
  if (dim !== 'village') query.villageNameFilter = ''
  handleQuery()
}

function onSidebarItemClick(
  topField: 'fundType' | 'fundSource' | 'nodeLevel' | 'villageNameFilter',
  belongsGroupBy: 'fundType' | 'fundSource' | 'nodeLevel' | 'village',
  value: string
) {
  if (query.groupBy !== belongsGroupBy) return
  if (topField === 'nodeLevel') {
    const num = Number(value) as 1 | 2 | 3
    query.nodeLevel = query.nodeLevel === num ? null : num
  } else {
    // 其他三个是字符串，toggle
    const cur = (query as any)[topField]
    ;(query as any)[topField] = cur === value ? '' : value
  }
  handleQuery()
}

function clearSidebarFilter() {
  query.fundType = ''
  query.fundSource = ''
  query.nodeLevel = null
  query.villageNameFilter = ''
  handleQuery()
}

async function handleQuery() {
  pageNum.value = 1
  loadData()
}

async function loadData() {
  query.regionCode = query.regionCodePath.length > 0 ? query.regionCodePath[query.regionCodePath.length - 1] : ''
  query.startTime = query.timeRange ? query.timeRange[0] : ''
  query.endTime = query.timeRange ? query.timeRange[1] : ''
  loading.value = true
  try {
    const res = await getFundQueryPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      projectName: query.projectName || undefined,
      approvalCode: query.approvalCode || undefined,
      declareCode: query.declareCode || undefined,
      detailName: query.detailName || undefined,
      nodeLevel: query.nodeLevel ?? undefined,
      fundType: query.fundType || undefined,
      fundSource: query.fundSource || undefined,
      regionCode: query.regionCode || undefined,
      startTime: query.startTime || undefined,
      endTime: query.endTime || undefined,
      createBy: query.createBy || undefined
    })
    rows.value = res.data.records
    total.value = res.data.total
    summary.value = res.data.summary
    sidebar.value = res.data.sidebar
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.projectName = ''
  query.approvalCode = ''
  query.declareCode = ''
  query.detailName = ''
  query.nodeLevel = null
  query.fundType = ''
  query.fundSource = ''
  query.regionCodePath = []
  query.regionCode = ''
  query.timeRange = null
  query.startTime = ''
  query.endTime = ''
  query.createBy = ''
  query.villageNameFilter = ''
  query.groupBy = 'fundType'
  handleQuery()
}

// ==================== 导出 ====================
async function handleExport() {
  if (summary.value.recordCount === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  exporting.value = true
  try {
    const params: any = {
      ...(query.projectName && { projectName: query.projectName }),
      ...(query.approvalCode && { approvalCode: query.approvalCode }),
      ...(query.declareCode && { declareCode: query.declareCode }),
      ...(query.detailName && { detailName: query.detailName }),
      ...(query.nodeLevel != null && { nodeLevel: query.nodeLevel }),
      ...(query.fundType && { fundType: query.fundType }),
      ...(query.fundSource && { fundSource: query.fundSource }),
      ...(query.regionCode && { regionCode: query.regionCode }),
      ...(query.startTime && { startTime: query.startTime }),
      ...(query.endTime && { endTime: query.endTime }),
      ...(query.createBy && { createBy: query.createBy })
    }
    downloadFundQueryExport(params)
    ElMessage.success('已触发下载')
  } catch (e: any) {
    ElMessage.error(e?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ==================== 详情弹窗 ====================
const detailVisible = ref(false)
const detailRow = ref<FundRecordRow | null>(null)
const detailTitle = computed(() => {
  if (!detailRow.value) return '详情'
  const r = detailRow.value
  const parts = [r.nodeLevelLabel]
  if (r.projectName) parts.push(r.projectName)
  return parts.join(' · ')
})
function openDetail(row: FundRecordRow) {
  detailRow.value = row
  detailVisible.value = true
}

// ==================== 工具方法 ====================
function formatMoney(v: number | null | undefined): string {
  if (v == null || isNaN(v)) return '0'
  return v.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}
function nodeLevelTagType(level: number | null | undefined): string {
  if (level === 1) return 'primary'
  if (level === 2) return 'success'
  if (level === 3) return 'warning'
  return 'info'
}

// ==================== 生命周期 ====================
onMounted(async () => {
  try {
    await Promise.all([loadRegionTree(), loadData()])
  } catch (e: any) {
    console.error('[FundRecordQuery] onMounted failed:', e)
    ElMessage.error('页面初始化失败：' + (e?.message || '未知错误，请打开控制台查看'))
    loading.value = false
  }
})
</script>

<style scoped>
.fund-record-query {
  padding: 0;
}
.top-filter {
  margin-bottom: 12px;
}
.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.filter-right {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.filter-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.filter-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.summary-card {
  margin-bottom: 12px;
}
.summary-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: space-around;
}
.summary-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 120px;
  padding: 4px 12px;
}
.summary-value {
  font-size: 22px;
  font-weight: 600;
  color: #409eff;
  font-variant-numeric: tabular-nums;
}
.summary-label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.main-body {
  display: flex;
  gap: 12px;
  height: calc(100vh - 360px);
  min-height: 520px;
}
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
  font-size: 12px;
  font-variant-numeric: tabular-nums;
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
