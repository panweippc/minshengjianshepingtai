<template>
  <div>
    <!-- ==================== 主列表筛选区 ==================== -->
    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="报表名称">
          <el-input
            v-model="query.reportName"
            placeholder="请输入报表名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="生成时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item label="报表类型">
          <el-select
            v-model="query.reportType"
            placeholder="请选择报表类型"
            clearable
          >
            <el-option
              v-for="item in reportTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ==================== 主列表区 ==================== -->
    <el-card shadow="never" class="list-card">
      <template #header>
        <div class="card-header">
          <span>统计表列表</span>
          <div class="header-actions">
            <el-button
              type="primary"
              icon="Plus"
              @click="openAdd('INVENTORY')"
            >
              新增盘点表
            </el-button>
            <el-button
              type="success"
              icon="Plus"
              @click="openAdd('HANDOVER')"
            >
              新增交接表
            </el-button>
            <el-button
              type="warning"
              icon="Plus"
              @click="openAdd('DAMAGE')"
            >
              新增损毁表
            </el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="reportName" label="报表名称" min-width="260" show-overflow-tooltip />
        <el-table-column label="时间" min-width="180" align="center">
          <template #default="{ row }">{{ row.reportDate || row.createTime?.slice(0, 10) || '—' }}</template>
        </el-table-column>
        <el-table-column label="报表类型" min-width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="reportTypeTag(row.reportType)" effect="light">
              {{ reportTypeLabel(row.reportType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handlePrint(row)">打印</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="handleQuery"
        @current-change="loadData"
      />
    </el-card>

    <!-- ==================== 新增/编辑弹窗 ==================== -->
    <el-dialog
      v-model="editDialogVisible"
      :title="editDialogTitle"
      width="86%"
      top="5vh"
      destroy-on-close
      class="report-dialog"
    >
      <!-- 明细筛选区 -->
      <el-card shadow="never" class="detail-query-card">
        <el-form :inline="true" :model="detailQuery" @submit.prevent>
          <el-form-item v-if="editType !== 'DAMAGE'" label="物资名称">
            <el-input
              v-model="detailQuery.itemName"
              placeholder="请输入物资名称"
              clearable
              @keyup.enter="handleDetailQuery"
            />
          </el-form-item>
          <el-form-item label="库房名称">
            <el-select
              v-model="detailQuery.warehouseName"
              placeholder="请选择库房"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="w in warehouseOptions"
                :key="w"
                :label="w"
                :value="w"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="editType !== 'DAMAGE'" label="物资分类">
            <el-select
              v-model="detailQuery.itemCategory"
              placeholder="请选择物资分类"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in categoryOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <!-- 损毁表时间：系统自动带出当前时间，置灰不可修改 -->
          <el-form-item v-if="editType === 'DAMAGE'" label="时间">
            <el-date-picker
              v-model="detailQuery.damageTime"
              type="datetime"
              placeholder="系统自动带出"
              value-format="YYYY-MM-DD HH:mm:ss"
              disabled
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleDetailQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetDetailQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 明细表格区：盘点表 / 交接表 / 损毁表（表格纸样式：名称+生成时间 → 明细 → 备注/签字/部门） -->
      <div class="form-sheet">
        <!-- 报表名称 + 生成时间 -->
        <div class="sheet-head">
          <el-input v-model="headerForm.reportName" placeholder="请输入名称" maxlength="128" class="sheet-name" />
          <div class="sheet-time">{{ editType === 'DAMAGE' ? (detailQuery.damageTime || '—') : sheetTimeText }}</div>
        </div>

        <!-- 明细表格 -->
        <el-table
          v-loading="detailLoading"
          :data="sheetItems"
          border
          max-height="520"
          class="sheet-table"
        >
          <el-table-column type="index" label="序号" width="64" align="center" />
          <el-table-column
            :label="editType === 'DAMAGE' ? '物资名' : '物资名称'"
            min-width="170"
            align="center"
            show-overflow-tooltip
          >
            <template #default="{ row }">{{ row.itemName }}</template>
          </el-table-column>
          <el-table-column label="库房" min-width="170" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.warehouseName || '—' }}</template>
          </el-table-column>
          <template v-if="editType === 'DAMAGE'">
            <el-table-column label="数量" min-width="130" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.bookQty" :min="0" :controls="false" size="small" class="cell-control" />
              </template>
            </el-table-column>
          </template>
          <el-table-column label="单位" min-width="100" align="center">
            <template #default="{ row }">{{ row.unit || '—' }}</template>
          </el-table-column>
          <template v-if="editType === 'DAMAGE'">
            <el-table-column label="经办人" min-width="160" align="center">
              <template #default="{ row }">
                <el-input v-model="row.handler" placeholder="请输入" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="备注" width="190" align="center">
              <template #default="{ row }">
                <el-input v-model="row.remark" placeholder="请输入" size="small" />
              </template>
            </el-table-column>
          </template>
          <template v-else>
            <el-table-column label="账面数量" min-width="130" align="center">
              <template #default="{ row }">{{ row.bookQty }}</template>
            </el-table-column>
            <el-table-column label="盘点数量" min-width="130" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.actualQty" :controls="false" size="small" class="cell-control" />
              </template>
            </el-table-column>
            <el-table-column label="盈亏数量" width="190" align="center">
              <template #default="{ row }">{{ calcProfit(row) }}</template>
            </el-table-column>
          </template>
        </el-table>

        <!-- 备注 -->
        <div class="sheet-line">
          <div class="sheet-cell sheet-label">备注</div>
          <div class="sheet-cell sheet-fill">
            <el-input v-model="headerForm.remark" placeholder="请输入备注" maxlength="255" class="sheet-input" />
          </div>
        </div>

        <!-- 经办人 / 交接人 / 审核人 / 审核日期 -->
        <div class="sheet-line signature">
          <div class="sheet-cell sheet-label">经办人</div>
          <div class="sheet-cell sheet-fill">
            <el-input v-model="footerForm.handler" placeholder="请输入" maxlength="32" class="sheet-input" />
          </div>
          <template v-if="editType === 'HANDOVER'">
            <div class="sheet-cell sheet-label">交接人</div>
            <div class="sheet-cell sheet-fill">
              <el-input v-model="footerForm.handoverPerson" placeholder="请输入" maxlength="32" class="sheet-input" />
            </div>
          </template>
          <div class="sheet-cell sheet-label">审核人</div>
          <div class="sheet-cell sheet-fill">
            <el-input v-model="footerForm.auditor" placeholder="请输入" maxlength="32" class="sheet-input" />
          </div>
          <div class="sheet-cell sheet-label">审核日期</div>
          <div class="sheet-cell sheet-fill">
            <el-date-picker
              v-model="headerForm.reportDate"
              type="date"
              placeholder="请选择日期"
              value-format="YYYY-MM-DD"
              class="sheet-input"
            />
          </div>
        </div>

        <!-- 部门 -->
        <div class="sheet-line">
          <div class="sheet-cell sheet-label">部门</div>
          <div class="sheet-cell sheet-fill">
            <el-input v-model="footerForm.department" placeholder="请输入部门" maxlength="64" class="sheet-input" />
          </div>
        </div>
      </div>

      <template #footer>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
        <el-button @click="editDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 查看弹窗 ==================== -->
    <el-dialog
      v-model="detailDialogVisible"
      title="详情数据"
      width="86%"
      top="5vh"
      destroy-on-close
      class="report-dialog"
    >
      <div v-if="detail" class="view-sheet">
        <!-- 打印按钮（表格纸右上角） -->
        <div class="view-print-bar">
          <el-button type="primary" size="small" icon="Printer" @click="handlePrint(detail as MaterialReportDetailVO)">
            打印
          </el-button>
        </div>

        <div class="form-sheet">
          <!-- 报表名称 + 时间 -->
          <div class="sheet-head">
            <div class="sheet-name-text" :title="detail.reportName">{{ detail.reportName || '—' }}</div>
            <div class="sheet-time">{{ viewTimeText }}</div>
          </div>

          <!-- 明细表格 -->
          <el-table :data="detail.items || []" border class="sheet-table view-table">
            <el-table-column type="index" label="序号" width="64" align="center" />
            <el-table-column
              :label="detail.reportType === 'DAMAGE' ? '物资名' : '物资名称'"
              min-width="170"
              align="center"
              show-overflow-tooltip
            >
              <template #default="{ row }">{{ row.itemName }}</template>
            </el-table-column>
            <el-table-column label="库房" min-width="170" align="center" show-overflow-tooltip>
              <template #default="{ row }">{{ row.warehouseName || '—' }}</template>
            </el-table-column>
            <el-table-column label="单位" min-width="100" align="center">
              <template #default="{ row }">{{ row.unit || '—' }}</template>
            </el-table-column>
            <template v-if="detail.reportType === 'DAMAGE'">
              <el-table-column label="数量" min-width="130" align="center">
                <template #default="{ row }">{{ row.bookQty }}</template>
              </el-table-column>
              <el-table-column label="经办人" min-width="160" align="center">
                <template #default="{ row }">{{ row.handler || '—' }}</template>
              </el-table-column>
              <el-table-column label="备注" width="190" align="center">
                <template #default="{ row }">{{ row.remark || '—' }}</template>
              </el-table-column>
            </template>
            <template v-else>
              <el-table-column label="账面数量" min-width="130" align="center">
                <template #default="{ row }">{{ row.bookQty }}</template>
              </el-table-column>
              <el-table-column label="盘点数量" min-width="130" align="center">
                <template #default="{ row }">{{ row.actualQty }}</template>
              </el-table-column>
              <el-table-column label="盈亏数量" width="190" align="center">
                <template #default="{ row }">{{ (row.actualQty || 0) - (row.bookQty || 0) }}</template>
              </el-table-column>
            </template>
          </el-table>

          <!-- 备注 -->
          <div class="sheet-line remark">
            <div class="sheet-cell sheet-label">备注</div>
            <div class="sheet-cell sheet-fill">{{ detail.remark || '' }}</div>
          </div>

          <!-- 经办人 / 交接人 / 审核人 / 审核日期 -->
          <div class="sheet-line signature">
            <div class="sheet-cell sheet-label">经办人</div>
            <div class="sheet-cell sheet-fill">{{ detail.handler || '' }}</div>
            <template v-if="detail.reportType === 'HANDOVER'">
              <div class="sheet-cell sheet-label">交接人</div>
              <div class="sheet-cell sheet-fill">{{ detail.handoverPerson || '' }}</div>
            </template>
            <div class="sheet-cell sheet-label">审核人</div>
            <div class="sheet-cell sheet-fill">{{ detail.auditor || '' }}</div>
            <div class="sheet-cell sheet-label">审核日期</div>
            <div class="sheet-cell sheet-fill">{{ detail.reportDate || '' }}</div>
          </div>

          <!-- 部门 -->
          <div class="sheet-line">
            <div class="sheet-cell sheet-label">部门</div>
            <div class="sheet-cell sheet-fill">{{ detail.department || '' }}</div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDictByType } from '@/api/dict'
import {
  getMaterialStockList,
  getMaterialWarehousePage,
  getMaterialReportPage,
  getMaterialReportDetail,
  createMaterialInventory,
  updateMaterialInventory,
  createMaterialHandover,
  updateMaterialHandover,
  createMaterialDamage,
  updateMaterialDamage,
  deleteMaterialReport
} from '@/api/material'
import type { MaterialReportQuery } from '@/api/material'
import type {
  MaterialReportVO,
  MaterialReportDetailVO,
  MaterialReportType,
  MaterialInventoryItemSaveDTO,
  MaterialHandoverItemSaveDTO,
  MaterialDamageItemSaveDTO
} from '@/types/system'

// ==================== 报表类型辅助 ====================
const reportTypeLabel = (type?: string) => {
  const hit = reportTypeOptions.value.find((o) => o.value === type)
  return hit ? hit.label : type || '—'
}

const reportTypeTag = (type?: string) => {
  const map: Record<string, string> = {
    INVENTORY: 'primary',
    HANDOVER: 'success',
    DAMAGE: 'warning'
  }
  return map[type || ''] || 'info'
}

// ==================== 主列表 ====================
const query = reactive<MaterialReportQuery>({
  pageNum: 1,
  pageSize: 10
})
const dateRange = ref<string[]>([])
const total = ref(0)
const tableData = ref<MaterialReportVO[]>([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMaterialReportPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleDateChange = (val: string[]) => {
  if (val && val.length === 2) {
    query.startTime = val[0]
    query.endTime = val[1]
  } else {
    query.startTime = undefined
    query.endTime = undefined
  }
}

const handleQuery = () => {
  query.pageNum = 1
  loadData()
}

const resetQuery = () => {
  query.reportName = undefined
  query.reportType = undefined
  query.startTime = undefined
  query.endTime = undefined
  dateRange.value = []
  handleQuery()
}

// ==================== 字典和库房选项 ====================
/** 报表类型字典（baobiao_leixing）—— 字典缺失时兜底选项 */
const DEFAULT_REPORT_TYPES = [
  { label: '盘点表', value: 'INVENTORY' },
  { label: '交接表', value: 'HANDOVER' },
  { label: '损毁表', value: 'DAMAGE' }
]
const reportTypeDictItems = ref<{ dictLabel: string; dictValue: string }[]>([])
const reportTypeOptions = computed(() =>
  reportTypeDictItems.value.length
    ? reportTypeDictItems.value.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    : DEFAULT_REPORT_TYPES
)

const DEFAULT_CATEGORIES = [
  { label: '应急物资', value: '1' },
  { label: '生活物资', value: '2' },
  { label: '食品物资', value: '3' },
  { label: '防汛物资', value: '4' }
]
const categoryDictItems = ref<{ dictLabel: string; dictValue: string }[]>([])
const categoryOptions = computed(() =>
  categoryDictItems.value.length
    ? categoryDictItems.value.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    : DEFAULT_CATEGORIES
)
const warehouseOptions = ref<string[]>([])

const loadDict = async () => {
  try {
    const [categoryRes, reportTypeRes] = await Promise.all([
      getDictByType('wu_zi_fen_lei'),
      getDictByType('baobiao_leixing')
    ])
    categoryDictItems.value = (categoryRes.data as any[]) || []
    reportTypeDictItems.value = (reportTypeRes.data as any[]) || []
  } catch {
    // 忽略错误，使用默认值
  }
}

const loadWarehouseOptions = async () => {
  try {
    // 联动库房信息模块，取已维护的库房名称
    const res = await getMaterialWarehousePage({ pageNum: 1, pageSize: 500 })
    warehouseOptions.value = (res.data.records || [])
      .map((w) => w.warehouseName)
      .filter(Boolean)
  } catch {
    warehouseOptions.value = []
  }
}

// ==================== 新增/编辑弹窗 ====================
const editDialogVisible = ref(false)
const editType = ref<MaterialReportType>('INVENTORY')
const editMode = ref<'add' | 'edit'>('add')
const editDialogTitle = computed(() => {
  const typeLabel = reportTypeLabel(editType.value)
  return `${editMode.value === 'edit' ? '编辑' : '添加'}${typeLabel}`
})

const submitLoading = ref(false)

// 头部表单
const headerForm = reactive({
  reportName: '',
  reportDate: '',
  reportType: 'INVENTORY' as MaterialReportType,
  remark: ''
})

/** 表格纸右上角生成时间（新增取当前时间，编辑取创建时间） */
const sheetTimeText = ref(formatNow())

function formatNow() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// 备注等字段（三类报表共用）
const footerForm = reactive({
  handler: '',
  auditor: '',
  handoverPerson: '',
  department: ''
})

// 明细筛选
const detailQuery = reactive({
  itemName: '',
  itemCategory: '',
  warehouseName: '',
  // 损毁表时间（系统自动带出，只读）
  damageTime: ''
})

/** 查看弹窗首行时间文案：优先展示创建时间，其次报表日期 */
const viewTimeText = computed(() => {
  const createTime = detail.value?.createTime
  if (createTime) return String(createTime).replace('T', ' ').slice(0, 19)
  return detail.value?.reportDate || '—'
})


/** 从入库/出库按物资+库房汇总库存，自动带出库房与账面数量 */
const loadInventorySourceItems = async () => {
  detailLoading.value = true
  try {
    const res = await getMaterialStockList({
      itemName: detailQuery.itemName || undefined,
      itemCategory: detailQuery.itemCategory || undefined,
      warehouseName: detailQuery.warehouseName || undefined
    })
    const list = res.data || []
    inventoryItems.value = list.map((it) => ({
      _tempId: Date.now() + Math.random(),
      warehouseName: it.warehouseName || '',
      itemName: it.itemName,
      unit: it.unit || '',
      bookQty: it.bookQty ?? 0,
      actualQty: it.bookQty ?? 0,
      remark: ''
    }))
    handoverItems.value = list.map((it) => ({
      _tempId: Date.now() + Math.random(),
      warehouseName: it.warehouseName || '',
      itemName: it.itemName,
      unit: it.unit || '',
      bookQty: it.bookQty ?? 0,
      actualQty: it.bookQty ?? 0,
      remark: ''
    }))
    damageItems.value = list.map((it) => ({
      _tempId: Date.now() + Math.random(),
      warehouseName: it.warehouseName || '',
      itemName: it.itemName,
      unit: it.unit || '',
      bookQty: it.bookQty ?? 0,
      handler: '',
      remark: ''
    }))
    if (!list.length) {
      ElMessage.info('未查询到物资库存数据')
    }
  } finally {
    detailLoading.value = false
  }
}

const handleDetailQuery = () => {
  loadInventorySourceItems()
}

const resetDetailQuery = () => {
  detailQuery.itemName = ''
  detailQuery.itemCategory = ''
  detailQuery.warehouseName = ''
  // 损毁表时间自动带出当前时间，不可修改
  detailQuery.damageTime = editType.value === 'DAMAGE' ? (sheetTimeText.value || formatNow()) : ''
  if (editMode.value === 'add') {
    loadInventorySourceItems()
  }
}

// 盘点表明细
interface InventoryRow extends MaterialInventoryItemSaveDTO {
  _tempId: number
}
const inventoryItems = ref<InventoryRow[]>([])

const calcProfit = (row: { bookQty: number; actualQty?: number }) => {
  return (row.actualQty || 0) - (row.bookQty || 0)
}

// 交接表明细
interface HandoverRow extends MaterialHandoverItemSaveDTO {
  _tempId: number
}
const handoverItems = ref<HandoverRow[]>([])

/** 表格纸模式下的明细数据源：盘点表 / 交接表 / 损毁表共用同一套表格结构 */
const sheetItems = computed<InventoryRow[] | HandoverRow[] | DamageRow[]>(() => {
  if (editType.value === 'HANDOVER') return handoverItems.value
  if (editType.value === 'DAMAGE') return damageItems.value
  return inventoryItems.value
})

// 损毁表明细
interface DamageRow extends MaterialDamageItemSaveDTO {
  _tempId: number
}
const damageItems = ref<DamageRow[]>([])

const detailLoading = ref(false)
/** 当前编辑的报表ID（编辑时用于提交主键） */
const editId = ref<string>()

// 打开新增
const openAdd = (type: MaterialReportType) => {
  editMode.value = 'add'
  editType.value = type
  editId.value = undefined
  Object.assign(headerForm, {
    reportName: '',
    // 审核日期由用户手动选择，默认留空
    reportDate: '',
    reportType: type,
    remark: ''
  })
  Object.assign(footerForm, {
    handler: '',
    auditor: '',
    handoverPerson: '',
    department: ''
  })
  sheetTimeText.value = formatNow()
  inventoryItems.value = []
  handoverItems.value = []
  damageItems.value = []
  resetDetailQuery()
  editDialogVisible.value = true
}

// 打开编辑
const openEdit = async (row: MaterialReportVO) => {
  editMode.value = 'edit'
  editType.value = row.reportType
  editId.value = row.id
  detailLoading.value = true
  try {
    const res = await getMaterialReportDetail(row.id)
    const d = res.data
    const detailItems = (d as any).items || []
    Object.assign(headerForm, {
      reportName: d.reportName,
      reportDate: d.reportDate || '',
      reportType: d.reportType,
      remark: (d as any).remark || ''
    })
    Object.assign(footerForm, {
      handler: d.handler || '',
      auditor: d.auditor || '',
      handoverPerson: (d as any).handoverPerson || '',
      department: (d as any).department || ''
    })
    sheetTimeText.value = d.createTime ? String(d.createTime).replace('T', ' ').slice(0, 19) : formatNow()

    if (d.reportType === 'INVENTORY') {
      inventoryItems.value = detailItems.map((it: any) => ({
        _tempId: it.id || Date.now() + Math.random(),
        warehouseName: it.warehouseName || '',
        itemName: it.itemName,
        unit: it.unit || '',
        bookQty: it.bookQty,
        actualQty: it.actualQty,
        remark: it.remark || ''
      }))
    } else if (d.reportType === 'HANDOVER') {
      handoverItems.value = detailItems.map((it: any) => ({
        _tempId: it.id || Date.now() + Math.random(),
        warehouseName: it.warehouseName || '',
        itemName: it.itemName,
        unit: it.unit || '',
        bookQty: it.bookQty,
        actualQty: it.actualQty,
        remark: it.remark || ''
      }))
    } else if (d.reportType === 'DAMAGE') {
      damageItems.value = detailItems.map((it: any) => ({
        _tempId: it.id || Date.now() + Math.random(),
        warehouseName: it.warehouseName || '',
        itemName: it.itemName,
        unit: it.unit || '',
        bookQty: it.bookQty,
        handler: it.handler || '',
        remark: it.remark || ''
      }))
      // 编辑时回显报表生成时间（只读展示）
      detailQuery.damageTime = sheetTimeText.value
    }
    editDialogVisible.value = true
  } finally {
    detailLoading.value = false
  }
}

// 提交保存
const handleSubmit = async () => {
  // 三类报表均为表格纸布局，报表名称与明细在此统一校验
  if (!headerForm.reportName.trim()) {
    ElMessage.warning('请输入报表名称')
    return
  }

  // 校验明细：至少有一行且物资名称不为空
  let items: any[] = []
  if (editType.value === 'INVENTORY') {
    items = inventoryItems.value.filter((it) => it.itemName?.trim())
  } else if (editType.value === 'HANDOVER') {
    items = handoverItems.value.filter((it) => it.itemName?.trim())
  } else if (editType.value === 'DAMAGE') {
    items = damageItems.value.filter((it) => it.itemName?.trim())
  }
  if (items.length === 0) {
    ElMessage.warning('请至少添加一条明细')
    return
  }

  submitLoading.value = true
  try {
    if (editType.value === 'INVENTORY') {
      const payload = {
        id: editId.value,
        reportName: headerForm.reportName,
        reportDate: headerForm.reportDate || undefined,
        handler: footerForm.handler || undefined,
        auditor: footerForm.auditor || undefined,
        department: footerForm.department || undefined,
        remark: headerForm.remark || undefined,
        items: items.map(({ _tempId, ...rest }) => rest)
      }
      if (editMode.value === 'edit') {
        await updateMaterialInventory(payload)
      } else {
        await createMaterialInventory(payload)
      }
    } else if (editType.value === 'HANDOVER') {
      const payload = {
        id: editId.value,
        reportName: headerForm.reportName,
        reportDate: headerForm.reportDate || undefined,
        handler: footerForm.handler || undefined,
        handoverPerson: footerForm.handoverPerson || undefined,
        auditor: footerForm.auditor || undefined,
        department: footerForm.department || undefined,
        remark: headerForm.remark || undefined,
        items: items.map(({ _tempId, ...rest }) => rest)
      }
      if (editMode.value === 'edit') {
        await updateMaterialHandover(payload)
      } else {
        await createMaterialHandover(payload)
      }
    } else if (editType.value === 'DAMAGE') {
      const payload = {
        id: editId.value,
        reportName: headerForm.reportName,
        reportDate: headerForm.reportDate || undefined,
        handler: footerForm.handler || undefined,
        auditor: footerForm.auditor || undefined,
        department: footerForm.department || undefined,
        remark: headerForm.remark || undefined,
        items: items.map(({ _tempId, ...rest }) => rest)
      }
      if (editMode.value === 'edit') {
        await updateMaterialDamage(payload)
      } else {
        await createMaterialDamage(payload)
      }
    }
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

// ==================== 查看弹窗 ====================
const detailDialogVisible = ref(false)
const detail = ref<MaterialReportDetailVO | null>(null)

const openDetail = async (row: MaterialReportVO) => {
  const res = await getMaterialReportDetail(row.id)
  detail.value = res.data
  detailDialogVisible.value = true
}

// ==================== 打印 ====================
const handlePrint = async (row: MaterialReportVO) => {
  try {
    const res = await getMaterialReportDetail(row.id)
    const d = res.data

    const win = window.open('', '_blank')
    if (!win) {
      ElMessage.error('无法打开打印窗口，请允许弹窗')
      return
    }

    const isDamage = d.reportType === 'DAMAGE'
    const headers = isDamage
      ? ['序号', '物资名', '库房', '单位', '数量', '经办人', '备注']
      : ['序号', '物资名称', '库房', '单位', '账面数量', '盘点数量', '盈亏数量']

    const bodyRows = (d.items || []).map((it, i) => {
      const cells = isDamage
        ? [i + 1, it.itemName, it.warehouseName || '', it.unit || '', it.bookQty ?? '', it.handler || '', it.remark || '']
        : [
            i + 1,
            it.itemName,
            it.warehouseName || '',
            it.unit || '',
            it.bookQty ?? '',
            it.actualQty ?? '',
            (it.actualQty || 0) - (it.bookQty || 0)
          ]
      return '<tr>' + cells.map((c) => `<td>${c}</td>`).join('') + '</tr>'
    }).join('')

    // 列宽显式固定：两张表共用同一套 colgroup，保证标题行与明细表竖线严格对齐
    const colWidths = isDamage
      ? ['8%', '20%', '16%', '9%', '11%', '16%', '20%']
      : ['8%', '20%', '18%', '9%', '15%', '15%', '15%']
    const colGroup = '<colgroup>' + colWidths.map((w) => `<col style="width:${w}">`).join('') + '</colgroup>'

    // 首行：报表名称 + 右侧时间（与查看弹窗一致：优先创建时间，其次报表日期），单独一张表，仅第一页显示
    const createTimeText = d.createTime ? String(d.createTime).replace('T', ' ').slice(0, 19) : ''
    const headTimeText = createTimeText || d.reportDate || ''
    const headTable =
      '<table class="head-table">' + colGroup +
      '<tbody><tr>' +
      `<td class="title" colspan="6">${d.reportName || ''}</td>` +
      `<td class="date-cell">${headTimeText}</td>` +
      '</tr></tbody></table>'

    const headerRow = '<tr class="col-head">' + headers.map((h) => `<th>${h}</th>`).join('') + '</tr>'

    // 签字行：交接表多一个「交接人」
    const signRow = d.reportType === 'HANDOVER'
      ? '<tr class="sign-row">' +
        `<td colspan="2" class="sign-cell">经办人${d.handler ? '　' + d.handler : ''}</td>` +
        `<td colspan="2" class="sign-cell">交接人${d.handoverPerson ? '　' + d.handoverPerson : ''}</td>` +
        `<td colspan="2" class="sign-cell">审核人${d.auditor ? '　' + d.auditor : ''}</td>` +
        `<td class="sign-cell">审核日期${d.reportDate ? '　' + d.reportDate : ''}</td>` +
        '</tr>'
      : '<tr class="sign-row">' +
        `<td colspan="2" class="sign-cell">经办人${d.handler ? '　' + d.handler : ''}</td>` +
        `<td colspan="3" class="sign-cell">审核人${d.auditor ? '　' + d.auditor : ''}</td>` +
        `<td colspan="2" class="sign-cell">审核日期${d.reportDate ? '　' + d.reportDate : ''}</td>` +
        '</tr>'

    const remarkRow =
      '<tr class="remark-row">' +
      '<td colspan="7" class="label-cell">' +
      '<span class="row-label">备注</span>' +
      `<span class="row-value">${d.remark || ''}</span>` +
      '</td>' +
      '</tr>'

    const deptRow =
      '<tr class="dept-row">' +
      '<td colspan="7" class="label-cell">' +
      '<span class="row-label">部门</span>' +
      `<span class="row-value">${d.department || ''}</span>` +
      '</td>' +
      '</tr>'

    const scriptTag = '<scr' + 'ipt>window.onload = function() { window.print(); window.close(); }</scr' + 'ipt>'

    win.document.write(
      '<html>' +
      '<head>' +
      '<title>打印内容</title>' +
      '<style>' +
      // 不锁定 size，让浏览器打印面板里的「横向 / 纵向」切换生效
      '@page { margin: 8mm; }' +
      'body { margin: 0; font-family: "SimSun", "宋体", serif; color: #000; }' +
      'table { width: 100%; border-collapse: collapse; table-layout: fixed; }' +
      'th, td { border: 1px solid #000; padding: 4px 6px; font-size: 12px; text-align: center; word-break: break-all; }' +
      'tr { page-break-inside: avoid; }' +
      // 表名行单独成表，避免随表头每页重复；并保证与表头不拆页
      '.head-table { page-break-after: avoid; break-after: avoid; }' +
      '.head-table td { border-bottom: none; height: 40px; }' +
      '.head-table .title { font-size: 18px; font-weight: 700; letter-spacing: 1px; }' +
      '.head-table .date-cell { font-size: 12px; line-height: 1.5; }' +
      // 明细表头每页自动重复，多页打印时连续输出
      '.main-table thead { display: table-header-group; }' +
      '.col-head th { height: 26px; font-weight: 700; }' +
      '.remark-row td, .sign-row td, .dept-row td { height: 30px; }' +
      // 备注、签字、部门行去掉中间竖线，仅保留整表左右外框
      '.remark-row td, .sign-row td, .dept-row td { border-left: none; border-right: none; }' +
      '.remark-row td:first-child, .sign-row td:first-child, .dept-row td:first-child { border-left: 1px solid #000; }' +
      '.remark-row td:last-child, .sign-row td:last-child, .dept-row td:last-child { border-right: 1px solid #000; }' +
      '.label-cell, .sign-cell { text-align: left; }' +
      '.value-cell { text-align: left; }' +
      // 备注、部门标签与内容紧邻显示
      '.row-label { display: inline-block; width: 56px; }' +
      '.row-value { padding-left: 4px; }' +
      '</style>' +
      '</head>' +
      '<body>' +
      headTable +
      '<table class="main-table">' +
      colGroup +
      '<thead>' + headerRow + '</thead>' +
      '<tbody>' +
      bodyRows + remarkRow + signRow + deptRow +
      '</tbody>' +
      '</table>' +
      scriptTag +
      '</body>' +
      '</html>'
    )
    win.document.close()
  } catch (e) {
    console.error(e)
    ElMessage.error('打印预览失败')
  }
}

// ==================== 删除 ====================
const handleDelete = (row: MaterialReportVO) => {
  ElMessageBox.confirm(`确认删除报表「${row.reportName}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteMaterialReport(row.id)
      ElMessage.success('删除成功')
      await loadData()
    })
    .catch((err) => {
      if (err !== 'cancel' && err !== 'close') {
        console.error(err)
      }
    })
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadDict()
  loadWarehouseOptions()
  loadData()
})
</script>

<style scoped>
.query-card {
  margin-bottom: 0;
}
.query-card :deep(.el-form--inline) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}
.query-card :deep(.el-form-item) {
  flex: 1 1 0;
  min-width: 0;
  margin-right: 12px;
  margin-bottom: 0;
}
.query-card :deep(.el-form-item__content) {
  flex: 1;
  min-width: 0;
}
/* 报表名称 */
.query-card :deep(.el-form-item:nth-child(1)) {
  flex: 1 1 0;
  min-width: 160px;
}
/* 生成时间（日期范围需要更宽） */
.query-card :deep(.el-form-item:nth-child(2)) {
  flex: 1.8 1 0;
  min-width: 260px;
}
/* 报表类型 */
.query-card :deep(.el-form-item:nth-child(3)) {
  flex: 1 1 0;
  min-width: 170px;
}
.query-card :deep(.el-input),
.query-card :deep(.el-select),
.query-card :deep(.el-date-editor) {
  width: 100%;
}
.query-card :deep(.el-form-item:last-child) {
  flex: 0 0 auto;
  margin-right: 0;
}

.list-card {
  margin-top: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  gap: 8px;
}

/* 弹窗样式 */
.report-dialog :deep(.el-dialog__body) {
  max-height: 75vh;
  overflow-y: auto;
}

.header-form {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.detail-query-card {
  margin: 14px 0 16px;
  border: none;
  background: transparent;
  box-shadow: none;
}
.detail-query-card :deep(.el-card__body) {
  padding: 0 0 8px;
}
.detail-query-card :deep(.el-form--inline) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}
.detail-query-card :deep(.el-form-item) {
  flex: 1 1 0;
  min-width: 0;
  margin-right: 12px;
  margin-bottom: 0;
}
.detail-query-card :deep(.el-form-item__content) {
  flex: 1;
  min-width: 0;
}
.detail-query-card :deep(.el-input),
.detail-query-card :deep(.el-select),
.detail-query-card :deep(.el-date-editor) {
  width: 100%;
}
/* 查询/重置按钮不参与拉伸 */
.detail-query-card :deep(.el-form-item:last-child) {
  flex: 0 0 auto;
  margin-right: 0;
}

.detail-table-wrapper {
  margin-bottom: 12px;
}

/* ==================== 盘点表：表格纸样式 ==================== */
.form-sheet {
  border: 1px solid #dcdfe6;
}
.sheet-head {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #dcdfe6;
}
.sheet-head .sheet-name {
  flex: 1;
}
.sheet-head .sheet-name :deep(.el-input__wrapper) {
  box-shadow: none;
  background: #fff;
}
.sheet-time {
  box-sizing: border-box;
  width: 190px;
  flex: 0 0 190px;
  text-align: center;
  padding: 8px 0;
  font-size: 13px;
  color: #303133;
  border-left: 1px solid #dcdfe6;
}
.sheet-table {
  width: 100%;
}
.sheet-table :deep(.el-table__header th) {
  background: #f5f7fa;
  color: #303133;
  font-weight: 600;
  padding: 8px 0;
}
.sheet-table :deep(.el-table__cell) {
  padding: 4px 0;
}
.sheet-table :deep(.el-input-number) {
  width: 100%;
}
.sheet-table :deep(.el-select .el-select__wrapper) {
  box-shadow: none;
  background: transparent;
}
.sheet-line {
  display: flex;
  align-items: center;
  padding: 3px 0;
  border-top: 1px solid #dcdfe6;
  background: #fff;
}
/* 与表格直接相连的首行不再重复描边，避免出现双线 */
.sheet-table + .sheet-line {
  border-top: none;
}
.sheet-cell {
  display: flex;
  align-items: center;
  min-height: 30px;
}
.sheet-label {
  flex: 0 0 78px;
  justify-content: flex-start;
  padding-left: 12px;
  background: transparent;
  font-size: 13px;
  color: #303133;
}
.sheet-fill {
  flex: 1 1 0;
  min-width: 0;
  padding: 0 8px 0 0;
}
/* 行内输入框：保留描边，与图中表单一致 */
.sheet-fill :deep(.el-input__wrapper),
.sheet-fill :deep(.el-select__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  background: #fff;
  border-radius: 3px;
}
.sheet-fill :deep(.el-date-editor) {
  width: 100%;
}
/* 签字行：输入框保持紧凑宽度，右侧留白 */
.signature .sheet-fill :deep(.el-input),
.signature .sheet-fill :deep(.el-input-number) {
  width: 120px;
}
.signature .sheet-fill :deep(.el-date-editor) {
  width: 150px;
}
.table-footer {
  margin-top: 8px;
  text-align: left;
}

.footer-form {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

/* 查看弹窗 */
.view-print-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}
.sheet-name-text {
  flex: 1;
  min-width: 0;
  padding: 0 10px;
  overflow: hidden;
  font-size: 18px;
  font-weight: 600;
  line-height: 32px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* 报表名称输入框：字号加大、加粗、居中 */
.sheet-name :deep(.el-input__inner) {
  font-size: 18px;
  font-weight: 600;
  text-align: center;
}
</style>
