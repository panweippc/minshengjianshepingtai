<template>
  <div>
    <!-- 筛选区 -->
    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="库房名称">
          <el-input
            v-model="query.warehouseName"
            placeholder="请输入库房名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="库房位置">
          <el-input
            v-model="query.location"
            placeholder="请输入库房位置"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input
            v-model="query.manager"
            placeholder="请输入负责人"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input
            v-model="query.phone"
            placeholder="请输入联系电话"
            clearable
            @keyup.enter="handleQuery"
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
          <span>库房信息列表</span>
          <el-button
            v-if="store.hasPerm('material:warehouse:add')"
            type="primary"
            icon="Plus"
            @click="openAdd"
          >
            新增
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="warehouseName" label="库房名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="location" label="库房位置" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.location || '—' }}</template>
        </el-table-column>
        <el-table-column prop="manager" label="负责人" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.manager || '—' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" min-width="150">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button
              v-if="store.hasPerm('material:warehouse:edit')"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="store.hasPerm('material:warehouse:delete')"
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
      :title="editMode === 'edit' ? '编辑库房' : '新增库房'"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="库房名称" prop="warehouseName">
          <el-input v-model="form.warehouseName" placeholder="请输入库房名称" maxlength="128" />
        </el-form-item>
        <el-form-item label="库房位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入库房位置" maxlength="255" />
        </el-form-item>
        <el-form-item label="负责人" prop="manager">
          <el-input v-model="form.manager" placeholder="请输入负责人" maxlength="32" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看弹窗：固定两列布局，每行两个字段 -->
    <el-dialog v-model="detailDialogVisible" title="库房详情" width="640px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border class="detail-desc">
        <el-descriptions-item label="库房名称">{{ detail.warehouseName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail.manager || '—' }}</el-descriptions-item>
        <el-descriptions-item label="库房位置">{{ detail.location || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone || '—' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getMaterialWarehousePage,
  createMaterialWarehouse,
  updateMaterialWarehouse,
  deleteMaterialWarehouse
} from '@/api/material'
import type { MaterialWarehouseQuery } from '@/api/material'
import type { MaterialWarehouseVO, MaterialWarehouseSaveDTO } from '@/types/system'

const store = useUserStore()

type EditMode = 'add' | 'edit'

/** 查询与列表 */
const query = reactive<MaterialWarehouseQuery>({
  pageNum: 1,
  pageSize: 10
})
const total = ref(0)
const tableData = ref<MaterialWarehouseVO[]>([])
const loading = ref(false)

/** 加载列表数据 */
const loadData = async () => {
  loading.value = true
  try {
    const res = await getMaterialWarehousePage(query)
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
  query.warehouseName = undefined
  query.location = undefined
  query.manager = undefined
  query.phone = undefined
  handleQuery()
}

/** 新增/编辑弹窗 */
const editDialogVisible = ref(false)
const editMode = ref<EditMode>('add')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<MaterialWarehouseSaveDTO>({
  warehouseName: '',
  location: '',
  manager: '',
  phone: ''
})

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    warehouseName: '',
    location: '',
    manager: '',
    phone: ''
  })
}

const formRules: FormRules = {
  warehouseName: [{ required: true, message: '请输入库房名称', trigger: 'blur' }],
  phone: [
    {
      pattern: /^[0-9-]{6,20}$/,
      message: '联系电话仅支持数字和短横线（6-20位）',
      trigger: 'blur'
    }
  ]
}

const openAdd = () => {
  editMode.value = 'add'
  resetForm()
  editDialogVisible.value = true
}

const openEdit = (row: MaterialWarehouseVO) => {
  editMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    warehouseName: row.warehouseName,
    location: row.location || '',
    manager: row.manager || '',
    phone: row.phone || ''
  })
  editDialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload: MaterialWarehouseSaveDTO = {
      warehouseName: form.warehouseName,
      location: form.location || undefined,
      manager: form.manager || undefined,
      phone: form.phone || undefined
    }
    if (editMode.value === 'edit') {
      payload.id = form.id
      await updateMaterialWarehouse(payload)
    } else {
      await createMaterialWarehouse(payload)
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
const detail = ref<MaterialWarehouseVO | null>(null)

const openDetail = (row: MaterialWarehouseVO) => {
  detail.value = row
  detailDialogVisible.value = true
}

/** 删除 */
const handleDelete = (row: MaterialWarehouseVO) => {
  ElMessageBox.confirm(`确认删除库房「${row.warehouseName}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteMaterialWarehouse(row.id)
      ElMessage.success('删除成功')
      await loadData()
    })
    .catch((err) => {
      // 取消确认不提示错误
      if (err !== 'cancel' && err !== 'close') {
        console.error(err)
      }
    })
}

onMounted(() => {
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
.query-card :deep(.el-input) {
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
