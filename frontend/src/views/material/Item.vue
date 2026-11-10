<template>
  <div>
    <!-- 筛选区 -->
    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="物品名称">
          <el-input
            v-model="query.itemName"
            placeholder="请输入物品名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="物品分类">
          <el-select
            v-model="query.itemCategory"
            placeholder="请选择物品分类"
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
        <el-form-item label="低于预警">
          <el-switch
            v-model="query.lowWarning"
            active-text="是"
            inactive-text="否"
            inline-prompt
            @change="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表单列表区 -->
    <el-card shadow="never" class="list-card">
      <template #header>
        <div class="card-header">
          <span>物品信息列表</span>
          <div class="header-actions">
            <el-button
              type="success"
              icon="Download"
              :disabled="selectedIds.length === 0"
              @click="handleExport"
            >
              导出选中（{{ selectedIds.length }}）
            </el-button>
            <el-button
              v-if="store.hasPerm('material:item:add')"
              type="primary"
              icon="Plus"
              @click="openAdd"
            >
              新增
            </el-button>
          </div>
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" reserve-selection />
        <el-table-column prop="itemName" label="物品名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="物品分类" min-width="110" align="center">
          <template #default="{ row }">{{ categoryLabel(row.itemCategory) }}</template>
        </el-table-column>
        <el-table-column label="预警数量" min-width="100" align="center">
          <template #default="{ row }">
            {{ row.warningQty ?? 0 }}<span v-if="row.unit"> {{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存数量" min-width="110" align="center">
          <template #default="{ row }">
            <span :class="{ 'stock-warning': isLowWarning(row) }">
              {{ row.stockQty ?? 0 }}<span v-if="row.unit"> {{ row.unit }}</span>
            </span>
            <el-tag v-if="isLowWarning(row)" type="danger" size="small" effect="plain" class="warning-tag">
              预警
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button
              v-if="store.hasPerm('material:item:edit')"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="store.hasPerm('material:item:delete')"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="editMode === 'edit' ? '编辑物品' : '新增物品'"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="物品名称" prop="itemName">
          <el-input v-model="form.itemName" placeholder="请输入物品名称" maxlength="128" />
        </el-form-item>
        <el-form-item label="物品分类" prop="itemCategory">
          <el-select
            v-model="form.itemCategory"
            placeholder="请选择物品分类"
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
        <el-form-item label="预警数量" prop="warningQty">
          <el-input-number
            v-model="form.warningQty"
            :min="0"
            :step="1"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如：个/件/箱/公斤" maxlength="16" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看弹窗：固定两列布局，每行两个字段 -->
    <el-dialog v-model="detailDialogVisible" title="物品详情" width="640px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border class="detail-desc">
        <el-descriptions-item label="物品名称">{{ detail.itemName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="物品分类">{{ categoryLabel(detail.itemCategory) }}</el-descriptions-item>
        <el-descriptions-item label="预警数量">
          {{ detail.warningQty ?? 0 }}<span v-if="detail.unit"> {{ detail.unit }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="库存数量">
          <span :class="{ 'stock-warning': isLowWarning(detail) }">
            {{ detail.stockQty ?? 0 }}<span v-if="detail.unit"> {{ detail.unit }}</span>
          </span>
          <el-tag v-if="isLowWarning(detail)" type="danger" size="small" effect="plain" class="warning-tag">
            预警
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="单位">{{ detail.unit || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
// ExcelJS 使用浏览器构建版本，避免 Node 内置模块导致 Vite 打包失败
import type ExcelJSType from 'exceljs'
import { useUserStore } from '@/stores/user'
import { getDictByType } from '@/api/dict'
import {
  getMaterialItemPage,
  getMaterialItemListByIds,
  createMaterialItem,
  updateMaterialItem,
  deleteMaterialItem
} from '@/api/material'
import type { MaterialItemQuery } from '@/api/material'
import type { DictVO, MaterialItemVO, MaterialItemSaveDTO } from '@/types/system'

const store = useUserStore()

type EditMode = 'add' | 'edit'

/** 物品分类字典缺失时兜底选项（与字典 wu_pin_fen_lei 保持一致） */
const DEFAULT_CATEGORIES = [
  { label: '办公物品', value: '1' },
  { label: '食堂用品', value: '2' },
  { label: '办公家具', value: '3' },
  { label: '办公设备', value: '4' }
]
const categoryDictItems = ref<DictVO[]>([])
const categoryOptions = computed(() =>
  categoryDictItems.value.length
    ? categoryDictItems.value.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    : DEFAULT_CATEGORIES
)
const categoryLabel = (value?: string) => {
  if (!value) return '—'
  const hit = categoryOptions.value.find((item) => item.value === value)
  return hit ? hit.label : value
}

/** 查询与列表 */
const query = reactive<MaterialItemQuery>({
  pageNum: 1,
  pageSize: 10,
  lowWarning: false
})
const total = ref(0)
const tableData = ref<MaterialItemVO[]>([])
const loading = ref(false)

/** 复选框选中的物品ID集合（跨页保留） */
const selectedIds = ref<string[]>([])
const selectedRows = ref<MaterialItemVO[]>([])

/** 库存是否低于预警 */
const isLowWarning = (row: { stockQty?: number; warningQty?: number }) =>
  (row.stockQty ?? 0) < (row.warningQty ?? 0)

const handleSelectionChange = (rows: MaterialItemVO[]) => {
  selectedRows.value = rows
  selectedIds.value = rows.map((r) => r.id)
}

/** 加载字典（物品分类联动字典 wu_pin_fen_lei） */
const loadDict = async () => {
  const res = await getDictByType('wu_pin_fen_lei')
  categoryDictItems.value = res.data || []
}

/** 加载列表数据 */
const loadData = async () => {
  loading.value = true
  try {
    const res = await getMaterialItemPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  query.pageNum = 1
  loadData()
}

const resetQuery = () => {
  query.itemName = undefined
  query.itemCategory = undefined
  query.lowWarning = false
  handleQuery()
}

/** 新增/编辑弹窗 */
const editDialogVisible = ref(false)
const editMode = ref<EditMode>('add')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<MaterialItemSaveDTO>({
  itemName: '',
  itemCategory: '',
  warningQty: 0,
  unit: '',
  remark: ''
})

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    itemName: '',
    itemCategory: '',
    warningQty: 0,
    unit: '',
    remark: ''
  })
}

const formRules: FormRules = {
  itemName: [{ required: true, message: '请输入物品名称', trigger: 'blur' }],
  warningQty: [{ required: true, message: '请输入预警数量', trigger: 'blur' }]
}

const openAdd = () => {
  editMode.value = 'add'
  resetForm()
  editDialogVisible.value = true
}

const openEdit = (row: MaterialItemVO) => {
  editMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    itemName: row.itemName,
    itemCategory: row.itemCategory || '',
    warningQty: row.warningQty ?? 0,
    unit: row.unit || '',
    remark: row.remark || ''
  })
  editDialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload: MaterialItemSaveDTO = {
      itemName: form.itemName,
      itemCategory: form.itemCategory || undefined,
      warningQty: form.warningQty,
      unit: form.unit || undefined,
      remark: form.remark || undefined
    }
    if (editMode.value === 'edit') {
      payload.id = form.id
      await updateMaterialItem(payload)
    } else {
      await createMaterialItem(payload)
    }
    ElMessage.success('操作成功')
    editDialogVisible.value = false
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

/** 查看弹窗 */
const detailDialogVisible = ref(false)
const detail = ref<MaterialItemVO | null>(null)

const openDetail = (row: MaterialItemVO) => {
  detail.value = row
  detailDialogVisible.value = true
}

/** 删除 */
const handleDelete = (row: MaterialItemVO) => {
  ElMessageBox.confirm(`确认删除物品「${row.itemName}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteMaterialItem(row.id)
      ElMessage.success('删除成功')
      // 若删除的是已选中项，从选中集合中移除
      selectedIds.value = selectedIds.value.filter((id) => id !== row.id)
      await loadData()
    })
    .catch((err) => {
      // 取消确认不提示错误
      if (err !== 'cancel' && err !== 'close') {
        console.error(err)
      }
    })
}

/**
 * 导出选中物品为 Excel
 * 实现：前端使用 ExcelJS 生成 .xlsx，避免后端导出接口；
 * 跨页选中时通过 ID 集合从后端拉取完整数据再导出。
 */
const handleExport = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先勾选要导出的物品')
    return
  }
  try {
    // 动态加载 ExcelJS 浏览器构建版本（UMD），按需加载避免首屏体积过大
    const ExcelJS = (await import('exceljs/dist/exceljs.min.js')) as unknown as typeof ExcelJSType

    // 若当前页选中项已包含全部选中 ID，直接用表格数据；否则按 ID 拉取
    const currentPageIds = new Set(selectedRows.value.map((r) => r.id))
    const needFetch = selectedIds.value.some((id) => !currentPageIds.has(id))
    const rows = needFetch
      ? (await getMaterialItemListByIds(selectedIds.value)).data
      : selectedRows.value

    const workbook = new ExcelJS.Workbook()
    workbook.creator = '民生建设项目管理平台'
    const sheet = workbook.addWorksheet('物品信息')
    sheet.columns = [
      { header: '物品名称', key: 'itemName', width: 24 },
      { header: '物品分类', key: 'itemCategoryLabel', width: 14 },
      { header: '预警数量', key: 'warningQty', width: 12 },
      { header: '库存数量', key: 'stockQty', width: 12 },
      { header: '单位', key: 'unit', width: 10 },
      { header: '备注', key: 'remark', width: 32 }
    ]
    // 表头样式
    sheet.getRow(1).font = { bold: true }
    sheet.getRow(1).alignment = { vertical: 'middle', horizontal: 'center' }
    // 数据行
    for (const row of rows) {
      sheet.addRow({
        itemName: row.itemName,
        itemCategoryLabel: categoryLabel(row.itemCategory),
        warningQty: row.warningQty ?? 0,
        stockQty: row.stockQty ?? 0,
        unit: row.unit || '',
        remark: row.remark || ''
      })
    }
    // 数值列居中
    sheet.getColumn('warningQty').alignment = { horizontal: 'center' }
    sheet.getColumn('stockQty').alignment = { horizontal: 'center' }

    const buffer = await workbook.xlsx.writeBuffer()
    const blob = new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `物品信息_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
    ElMessage.success(`已导出 ${rows.length} 条物品信息`)
  } catch (e) {
    console.error(e)
    ElMessage.error('导出失败，请重试')
  }
}

onMounted(() => {
  loadDict()
  loadData()
})
</script>

<style scoped>
.query-card {
  margin-bottom: 0;
}
/* 筛选项与操作按钮强制同一行，控件按剩余宽度自适应 */
.query-card :deep(.el-form--inline) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}
.query-card :deep(.el-form-item) {
  flex: 1 1 auto;
  min-width: 0;
  margin-right: 16px;
  margin-bottom: 0;
}
.query-card :deep(.el-form-item__content) {
  flex: 1;
  min-width: 0;
}
.query-card :deep(.el-input),
.query-card :deep(.el-select) {
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
.stock-warning {
  color: #f56c6c;
  font-weight: 600;
}
.warning-tag {
  margin-left: 6px;
}
/* 查看弹窗：两列布局，标签列固定宽度、内容列自适应并允许换行 */
.detail-desc :deep(.el-descriptions__label) {
  width: 110px;
  vertical-align: middle;
}
.detail-desc :deep(.el-descriptions__content) {
  vertical-align: middle;
  word-break: break-all;
}
</style>
