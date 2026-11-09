<template>
  <div>
    <!-- 顶部：入库操作 -->
    <el-card shadow="never" class="top-card">
      <div class="card-header">
        <span class="page-title">入库单管理</span>
        <el-button
          v-if="store.hasPerm('material:stockin:add')"
          type="primary"
          icon="Plus"
          @click="openCreate"
        >
          入库
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="tabs-card">
      <el-tabs v-model="activeTab">
        <!-- ==================== 明细页签 ==================== -->
        <el-tab-pane label="明细" name="detail">
          <el-form :inline="true" :model="detailQuery" class="query-form detail-query-form" @submit.prevent>
            <el-form-item label="物品名称">
              <el-input
                v-model="detailQuery.itemName"
                placeholder="请输入物品名称"
                clearable
                @keyup.enter="handleDetailQuery"
              />
            </el-form-item>
            <el-form-item label="库房">
              <el-select
                v-model="detailQuery.warehouseId"
                placeholder="请选择库房"
                clearable
              >
                <el-option
                  v-for="wh in warehouseOptions"
                  :key="wh.id"
                  :label="wh.warehouseName"
                  :value="wh.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="物品分类">
              <el-select
                v-model="detailQuery.itemCategory"
                placeholder="请选择物品分类"
                clearable
              >
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="入库类型">
              <el-select
                v-model="detailQuery.inType"
                placeholder="请选择入库类型"
                clearable
              >
                <el-option
                  v-for="item in inTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item class="query-buttons">
              <el-button type="primary" icon="Search" @click="handleDetailQuery">查询</el-button>
              <el-button icon="Refresh" @click="resetDetailQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <div class="table-toolbar">
            <span class="table-title">入库明细</span>
            <el-button
              type="success"
              icon="Download"
              :disabled="selectedDetailIds.length === 0"
              @click="handleExport"
            >
              导出选中（{{ selectedDetailIds.length }}）
            </el-button>
          </div>

          <el-table
            v-loading="detailLoading"
            :data="detailData"
            border
            stripe
            row-key="id"
            @selection-change="handleDetailSelectionChange"
          >
            <el-table-column type="selection" width="55" align="center" reserve-selection />
            <el-table-column type="index" label="序号" width="70" align="center" :index="detailIndexMethod" />
            <el-table-column prop="orderName" label="入库单名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="itemName" label="物品名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="quantity" label="入库数量" width="100" align="center" />
            <el-table-column prop="inTime" label="入库日期" width="120" align="center">
              <template #default="{ row }">{{ row.inTime || '—' }}</template>
            </el-table-column>
            <el-table-column prop="warehouseName" label="存储库房" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.warehouseName || '—' }}</template>
            </el-table-column>
            <el-table-column label="物品分类" width="120" align="center">
              <template #default="{ row }">{{ categoryLabel(row.itemCategory) }}</template>
            </el-table-column>
            <el-table-column prop="handler" label="经办人" width="110" align="center">
              <template #default="{ row }">{{ row.handler || '—' }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '—' }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="detailQuery.pageNum"
            v-model:page-size="detailQuery.pageSize"
            :total="detailTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            class="pagination"
            @size-change="handleDetailQuery"
            @current-change="loadDetailData"
          />
        </el-tab-pane>

        <!-- ==================== 入库单页签 ==================== -->
        <el-tab-pane label="入库单" name="order">
          <el-form :inline="true" :model="orderQuery" class="query-form" @submit.prevent>
            <el-form-item label="入库单">
              <el-input
                v-model="orderQuery.orderName"
                placeholder="请输入入库单名称"
                clearable
                @keyup.enter="handleOrderQuery"
              />
            </el-form-item>
            <el-form-item label="经办人">
              <el-input
                v-model="orderQuery.handler"
                placeholder="请输入经办人"
                clearable
                @keyup.enter="handleOrderQuery"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleOrderQuery">查询</el-button>
              <el-button icon="Refresh" @click="resetOrderQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <div class="table-toolbar">
            <span class="table-title">入库单列表</span>
          </div>

          <el-table v-loading="orderLoading" :data="orderData" border stripe row-key="id">
            <el-table-column type="index" label="序号" width="70" align="center" :index="orderIndexMethod" />
            <el-table-column prop="orderName" label="入库单名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="inDate" label="入库日期" width="120" align="center">
              <template #default="{ row }">{{ row.inDate || '—' }}</template>
            </el-table-column>
            <el-table-column prop="handler" label="经办人" width="120" align="center">
              <template #default="{ row }">{{ row.handler || '—' }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right" align="center">
              <template #default="{ row }">
                <div class="op-buttons">
                  <el-button link type="info" icon="View" @click="openView(row)">查看</el-button>
                  <el-button
                    v-if="store.hasPerm('material:stockin:edit')"
                    link
                    type="primary"
                    icon="Edit"
                    @click="openEdit(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-if="store.hasPerm('material:stockin:delete')"
                    link
                    type="danger"
                    icon="Delete"
                    @click="handleDelete(row)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="orderQuery.pageNum"
            v-model:page-size="orderQuery.pageSize"
            :total="orderTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            class="pagination"
            @size-change="handleOrderQuery"
            @current-change="loadOrderData"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ==================== 入库 / 编辑弹窗 ==================== -->
    <el-dialog
      v-model="formDialogVisible"
      :title="formMode === 'edit' ? '编辑入库单' : '入库'"
      width="1150px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="入库单名称" prop="orderName">
          <el-input v-model="form.orderName" placeholder="请输入入库单名称" maxlength="128" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <span class="table-title">物品信息</span>
        <el-button type="primary" plain icon="Plus" @click="addItemRow">添加物品</el-button>
      </div>

      <el-table :data="form.items" border stripe row-key="key" class="item-edit-table">
        <el-table-column label="仓库" width="170">
          <template #default="{ row }">
            <el-select v-model="row.warehouseId" placeholder="请选择仓库" clearable style="width: 100%">
              <el-option
                v-for="wh in warehouseOptions"
                :key="wh.id"
                :label="wh.warehouseName"
                :value="wh.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="物品分类" width="130">
          <template #default="{ row }">
            <el-select
              v-model="row.itemCategory"
              placeholder="请选择分类"
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
          </template>
        </el-table-column>
        <el-table-column label="物品名称" width="190">
          <template #default="{ row }">
            <el-select
              v-model="row.itemName"
              placeholder="请选择或输入"
              filterable
              allow-create
              default-first-option
              style="width: 100%"
              @change="handleItemNameChange(row)"
            >
              <el-option
                v-for="item in itemNameOptions(row.itemCategory)"
                :key="item.id"
                :label="item.itemName"
                :value="item.itemName"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" :step="1" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="入库类型" width="140">
          <template #default="{ row }">
            <el-select v-model="row.inType" placeholder="请选择类型" style="width: 100%">
              <el-option
                v-for="item in inTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="入库时间" width="150">
          <template #default="{ row }">
            <el-date-picker
              v-model="row.inTime"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择日期"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="170">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="请输入备注" maxlength="255" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" icon="Delete" @click="removeItemRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 查看弹窗 ==================== -->
    <el-dialog v-model="viewDialogVisible" title="入库单详情" width="1000px" destroy-on-close>
      <template v-if="viewDetail">
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="入库单名称">{{ viewDetail.orderName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="经办人">{{ viewDetail.handler || '—' }}</el-descriptions-item>
          <el-descriptions-item label="入库日期">{{ viewDetail.inDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ viewDetail.remark || '—' }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="viewDetail.items" border stripe class="view-item-table">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="warehouseName" label="仓库" min-width="130" show-overflow-tooltip />
          <el-table-column label="物品分类" width="110" align="center">
            <template #default="{ row }">{{ categoryLabel(row.itemCategory) }}</template>
          </el-table-column>
          <el-table-column prop="itemName" label="物品名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="quantity" label="数量" width="90" align="center" />
          <el-table-column label="入库类型" width="110" align="center">
            <template #default="{ row }">{{ inTypeLabel(row.inType) }}</template>
          </el-table-column>
          <el-table-column prop="inTime" label="入库时间" width="120" align="center">
            <template #default="{ row }">{{ row.inTime || '—' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || '—' }}</template>
          </el-table-column>
        </el-table>
      </template>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
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
  getMaterialWarehousePage,
  getMaterialStockInItemPage,
  getMaterialStockInItemListByIds,
  getMaterialStockInPage,
  getMaterialStockInDetail,
  createMaterialStockIn,
  updateMaterialStockIn,
  deleteMaterialStockIn
} from '@/api/material'
import type { MaterialStockInItemQuery, MaterialStockInOrderQuery } from '@/api/material'
import type {
  DictVO,
  MaterialItemVO,
  MaterialWarehouseVO,
  MaterialStockInItemVO,
  MaterialStockInVO,
  MaterialStockInDetailVO,
  MaterialStockInSaveDTO
} from '@/types/system'

const store = useUserStore()

type TabName = 'detail' | 'order'
type FormMode = 'add' | 'edit'

/** 物品分类字典缺失时兜底选项（与字典 wu_pin_fen_lei 保持一致） */
const DEFAULT_CATEGORIES = [
  { label: '办公物品', value: '1' },
  { label: '食堂用品', value: '2' },
  { label: '办公家具', value: '3' },
  { label: '办公设备', value: '4' }
]
/** 入库类型字典缺失时兜底选项（与字典 ru_ku_lei_xing 保持一致） */
const DEFAULT_IN_TYPES = [
  { label: '采购入库', value: '1' },
  { label: '调拨入库', value: '2' },
  { label: '归还入库', value: '3' },
  { label: '其他入库', value: '4' }
]

const activeTab = ref<TabName>('detail')
const categoryDictItems = ref<DictVO[]>([])
const inTypeDictItems = ref<DictVO[]>([])
const categoryOptions = computed(() =>
  categoryDictItems.value.length
    ? categoryDictItems.value.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    : DEFAULT_CATEGORIES
)
const inTypeOptions = computed(() =>
  inTypeDictItems.value.length
    ? inTypeDictItems.value.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    : DEFAULT_IN_TYPES
)
const categoryLabel = (value?: string) => {
  if (!value) return '—'
  const hit = categoryOptions.value.find((item) => item.value === value)
  return hit ? hit.label : value
}
const inTypeLabel = (value?: string) => {
  if (!value) return '—'
  const hit = inTypeOptions.value.find((item) => item.value === value)
  return hit ? hit.label : value
}

/** 仓库、物品下拉选项（物资数据量有限，一次拉取用于下拉联动） */
const warehouseOptions = ref<MaterialWarehouseVO[]>([])
const itemOptions = ref<MaterialItemVO[]>([])

/** ==================== 明细页签 ==================== */
const detailQuery = reactive<MaterialStockInItemQuery>({
  pageNum: 1,
  pageSize: 10
})
const detailTotal = ref(0)
const detailData = ref<MaterialStockInItemVO[]>([])
const detailLoading = ref(false)
const selectedDetailIds = ref<string[]>([])
const selectedDetailRows = ref<MaterialStockInItemVO[]>([])

const detailIndexMethod = (index: number) =>
  (detailQuery.pageNum - 1) * detailQuery.pageSize + index + 1

const handleDetailSelectionChange = (rows: MaterialStockInItemVO[]) => {
  selectedDetailRows.value = rows
  selectedDetailIds.value = rows.map((row) => row.id)
}

const loadDetailData = async () => {
  detailLoading.value = true
  try {
    const res = await getMaterialStockInItemPage(detailQuery)
    detailData.value = res.data.records
    detailTotal.value = res.data.total
  } finally {
    detailLoading.value = false
  }
}

const handleDetailQuery = () => {
  detailQuery.pageNum = 1
  loadDetailData()
}

const resetDetailQuery = () => {
  detailQuery.itemName = undefined
  detailQuery.warehouseId = undefined
  detailQuery.itemCategory = undefined
  detailQuery.inType = undefined
  handleDetailQuery()
}

/** ==================== 入库单页签 ==================== */
const orderQuery = reactive<MaterialStockInOrderQuery>({
  pageNum: 1,
  pageSize: 10
})
const orderTotal = ref(0)
const orderData = ref<MaterialStockInVO[]>([])
const orderLoading = ref(false)

const orderIndexMethod = (index: number) => (orderQuery.pageNum - 1) * orderQuery.pageSize + index + 1

const loadOrderData = async () => {
  orderLoading.value = true
  try {
    const res = await getMaterialStockInPage(orderQuery)
    orderData.value = res.data.records
    orderTotal.value = res.data.total
  } finally {
    orderLoading.value = false
  }
}

const handleOrderQuery = () => {
  orderQuery.pageNum = 1
  loadOrderData()
}

const resetOrderQuery = () => {
  orderQuery.orderName = undefined
  orderQuery.handler = undefined
  handleOrderQuery()
}

/** ==================== 入库 / 编辑表单 ==================== */
interface StockInItemForm {
  key: string
  warehouseId?: string
  itemName: string
  itemCategory?: string
  quantity: number
  inType: string
  inTime: string
  remark?: string
}

const formDialogVisible = ref(false)
const formMode = ref<FormMode>('add')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
/** 入库单名称必填校验 */
const formRules: FormRules = {
  orderName: [{ required: true, message: '请输入入库单名称', trigger: 'blur' }]
}
const form = reactive<{
  id?: string
  orderName: string
  remark: string
  items: StockInItemForm[]
}>({
  id: undefined,
  orderName: '',
  remark: '',
  items: []
})

let itemKeySeed = 0
const nextKey = () => {
  itemKeySeed += 1
  return `row-${itemKeySeed}`
}

/** 本地日期字符串（避免 toISOString 时区偏移） */
const todayStr = () => {
  const d = new Date()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

const createEmptyItem = (): StockInItemForm => ({
  key: nextKey(),
  warehouseId: undefined,
  itemName: '',
  itemCategory: undefined,
  quantity: 1,
  inType: inTypeOptions.value[0]?.value || '',
  inTime: todayStr(),
  remark: ''
})

const resetForm = () => {
  form.id = undefined
  form.orderName = ''
  form.remark = ''
  form.items = [createEmptyItem()]
}

const itemNameOptions = (category?: string) =>
  category ? itemOptions.value.filter((item) => item.itemCategory === category) : itemOptions.value

/** 选择物品名称后自动带出物品分类（字典联动） */
const handleItemNameChange = (row: StockInItemForm) => {
  const hit = itemOptions.value.find((item) => item.itemName === row.itemName)
  if (hit && hit.itemCategory) {
    row.itemCategory = hit.itemCategory
  }
}

const addItemRow = () => {
  form.items.push(createEmptyItem())
}

const removeItemRow = (index: number) => {
  if (form.items.length <= 1) {
    ElMessage.warning('至少保留一条物品信息')
    return
  }
  form.items.splice(index, 1)
}

const openCreate = () => {
  formMode.value = 'add'
  resetForm()
  formDialogVisible.value = true
}

const openEdit = async (row: MaterialStockInVO) => {
  try {
    const res = await getMaterialStockInDetail(row.id)
    const detail = res.data
    formMode.value = 'edit'
    form.id = detail.id
    form.orderName = detail.orderName
    form.remark = detail.remark || ''
    form.items = detail.items.length
      ? detail.items.map((item) => ({
          key: nextKey(),
          warehouseId: item.warehouseId,
          itemName: item.itemName,
          itemCategory: item.itemCategory,
          quantity: item.quantity || 1,
          inType: item.inType || inTypeOptions.value[0]?.value || '',
          inTime: item.inTime || todayStr(),
          remark: item.remark || ''
        }))
      : [createEmptyItem()]
    formDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

/** 表单校验：入库单名称与每条物品必填项逐项检查 */
const validateForm = () => {
  if (!form.orderName.trim()) {
    ElMessage.warning('请输入入库单名称')
    return false
  }
  for (let i = 0; i < form.items.length; i += 1) {
    const row = form.items[i]
    if (!row.warehouseId) {
      ElMessage.warning(`第 ${i + 1} 条物品：请选择仓库`)
      return false
    }
    if (!row.itemName || !row.itemName.trim()) {
      ElMessage.warning(`第 ${i + 1} 条物品：请选择或输入物品名称`)
      return false
    }
    if (!row.quantity || row.quantity < 1) {
      ElMessage.warning(`第 ${i + 1} 条物品：入库数量必须大于 0`)
      return false
    }
    if (!row.inType) {
      ElMessage.warning(`第 ${i + 1} 条物品：请选择入库类型`)
      return false
    }
    if (!row.inTime) {
      ElMessage.warning(`第 ${i + 1} 条物品：请选择入库时间`)
      return false
    }
  }
  return true
}

/** 物品ID按名称+分类回填，便于后续按物品聚合统计 */
const resolveItemId = (row: StockInItemForm) => {
  const hit = itemOptions.value.find(
    (item) => item.itemName === row.itemName && (!row.itemCategory || item.itemCategory === row.itemCategory)
  )
  return hit?.id
}

const handleSubmit = async () => {
  // 先触发表单必填校验（入库单名称为必填项），通过后再校验明细
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!validateForm()) return
  submitLoading.value = true
  try {
    const payload: MaterialStockInSaveDTO = {
      orderName: form.orderName.trim(),
      remark: form.remark || undefined,
      items: form.items.map((row) => ({
        warehouseId: row.warehouseId,
        itemId: resolveItemId(row),
        itemName: row.itemName.trim(),
        itemCategory: row.itemCategory,
        quantity: row.quantity,
        inType: row.inType,
        inTime: row.inTime,
        remark: row.remark || undefined
      }))
    }
    if (formMode.value === 'edit') {
      payload.id = form.id
      await updateMaterialStockIn(payload)
    } else {
      await createMaterialStockIn(payload)
    }
    ElMessage.success('操作成功')
    formDialogVisible.value = false
    await Promise.all([loadDetailData(), loadOrderData()])
  } finally {
    submitLoading.value = false
  }
}

/** ==================== 查看 ==================== */
const viewDialogVisible = ref(false)
const viewDetail = ref<MaterialStockInDetailVO | null>(null)

const openView = async (row: MaterialStockInVO) => {
  try {
    const res = await getMaterialStockInDetail(row.id)
    viewDetail.value = res.data
    viewDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

/** ==================== 删除 ==================== */
const handleDelete = (row: MaterialStockInVO) => {
  ElMessageBox.confirm(`确认删除入库单「${row.orderName}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteMaterialStockIn(row.id)
      ElMessage.success('删除成功')
      await Promise.all([loadDetailData(), loadOrderData()])
    })
    .catch((err) => {
      // 取消确认不提示错误
      if (err !== 'cancel' && err !== 'close') {
        console.error(err)
      }
    })
}

/**
 * 导出选中明细为 Excel
 * 实现：前端 ExcelJS 生成 .xlsx；跨页选中时按明细ID集合从后端拉取完整数据
 */
const handleExport = async () => {
  if (selectedDetailIds.value.length === 0) {
    ElMessage.warning('请先勾选要导出的入库明细')
    return
  }
  try {
    const ExcelJS = (await import('exceljs/dist/exceljs.min.js')) as unknown as typeof ExcelJSType

    const currentPageIds = new Set(selectedDetailRows.value.map((row) => row.id))
    const needFetch = selectedDetailIds.value.some((id) => !currentPageIds.has(id))
    const rows = needFetch
      ? (await getMaterialStockInItemListByIds(selectedDetailIds.value)).data
      : selectedDetailRows.value

    const workbook = new ExcelJS.Workbook()
    workbook.creator = '民生建设项目管理平台'
    const sheet = workbook.addWorksheet('入库明细')
    sheet.columns = [
      { header: '入库单名称', key: 'orderName', width: 24 },
      { header: '物品名称', key: 'itemName', width: 20 },
      { header: '入库数量', key: 'quantity', width: 12 },
      { header: '入库日期', key: 'inTime', width: 14 },
      { header: '存储库房', key: 'warehouseName', width: 18 },
      { header: '物品分类', key: 'itemCategoryLabel', width: 14 },
      { header: '经办人', key: 'handler', width: 12 },
      { header: '备注', key: 'remark', width: 30 }
    ]
    sheet.getRow(1).font = { bold: true }
    sheet.getRow(1).alignment = { vertical: 'middle', horizontal: 'center' }
    for (const row of rows) {
      sheet.addRow({
        orderName: row.orderName || '',
        itemName: row.itemName,
        quantity: row.quantity ?? 0,
        inTime: row.inTime || '',
        warehouseName: row.warehouseName || '',
        itemCategoryLabel: categoryLabel(row.itemCategory),
        handler: row.handler || '',
        remark: row.remark || ''
      })
    }
    sheet.getColumn('quantity').alignment = { horizontal: 'center' }
    sheet.getColumn('inTime').alignment = { horizontal: 'center' }

    const buffer = await workbook.xlsx.writeBuffer()
    const blob = new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `入库明细_${todayStr()}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
    ElMessage.success(`已导出 ${rows.length} 条入库明细`)
  } catch (e) {
    console.error(e)
    ElMessage.error('导出失败，请重试')
  }
}

/** 加载字典与下拉选项 */
const loadBaseData = async () => {
  const [categoryRes, inTypeRes, warehouseRes, itemRes] = await Promise.all([
    getDictByType('wu_pin_fen_lei'),
    getDictByType('ru_ku_lei_xing'),
    getMaterialWarehousePage({ pageNum: 1, pageSize: 1000 }),
    getMaterialItemPage({ pageNum: 1, pageSize: 1000 })
  ])
  categoryDictItems.value = categoryRes.data || []
  inTypeDictItems.value = inTypeRes.data || []
  warehouseOptions.value = warehouseRes.data.records || []
  itemOptions.value = itemRes.data.records || []
}

onMounted(async () => {
  await loadBaseData()
  await Promise.all([loadDetailData(), loadOrderData()])
})
</script>

<style scoped>
.top-card {
  margin-bottom: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.tabs-card :deep(.el-tabs__header) {
  margin-bottom: 12px;
}
.query-form :deep(.el-form-item) {
  margin-bottom: 12px;
}
/* 明细筛选区：四个筛选项同行展示，控件按可用宽度自适应，避免挤压变形 */
.detail-query-form {
  display: flex;
  flex-wrap: nowrap;
  align-items: flex-start;
}
.detail-query-form :deep(.el-form-item) {
  flex: 1 1 0;
  min-width: 0;
  margin-right: 12px;
  margin-bottom: 12px;
}
.detail-query-form :deep(.el-form-item__content) {
  min-width: 0;
}
.detail-query-form :deep(.el-input),
.detail-query-form :deep(.el-select) {
  width: 100%;
}
.detail-query-form .query-buttons {
  flex: 0 0 auto;
  margin-right: 0;
}
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.table-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
/* 操作列按钮不换行，保持查看/编辑/删除在同一行 */
.op-buttons {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}
.op-buttons :deep(.el-button) {
  margin-left: 0;
}
.op-buttons :deep(.el-button + .el-button) {
  margin-left: 10px;
}
.item-edit-table :deep(.el-table__row) {
  vertical-align: top;
}
.view-item-table {
  margin-top: 16px;
}
/* 查看弹窗：标签列固定宽度、内容列自适应并允许换行 */
.detail-desc :deep(.el-descriptions__label) {
  width: 110px;
  vertical-align: middle;
}
.detail-desc :deep(.el-descriptions__content) {
  vertical-align: middle;
  word-break: break-all;
}
</style>
