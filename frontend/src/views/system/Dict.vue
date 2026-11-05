<template>
  <div>
    <!-- 筛选区 -->
    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="字典类型">
          <el-select
            v-model="query.dictType"
            filterable
            allow-create
            default-first-option
            clearable
            placeholder="请选择或输入字典类型"
          >
            <el-option v-for="item in dictTypes" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="query.description" placeholder="请输入描述" clearable />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="query.dictLabel" placeholder="请输入标签" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表单列表区 -->
    <el-card shadow="never" style="margin-top: 12px">
      <template #header>
        <div class="card-header">
          <span>字典列表</span>
          <el-button v-if="store.hasPerm('system:dict:add')" type="primary" icon="Plus" @click="openDialog()">新增字典</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
        <el-table-column prop="dictType" label="字典类型" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="130" />
        <el-table-column prop="dictLabel" label="标签" min-width="120" />
        <el-table-column prop="dictValue" label="键值" min-width="120" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('system:dict:edit')" link type="primary" icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('system:dict:add')" link type="success" icon="Plus" @click="openAddKeyDialog(row)">添加键值</el-button>
            <el-button v-if="store.hasPerm('system:dict:delete')" link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="form.dictValue" placeholder="请输入字典键值" />
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="form.dictLabel" placeholder="请输入字典标签" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="form.dictType" :disabled="typeLocked" placeholder="请输入字典类型" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" style="width: 180px" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            :disabled="remarkLocked"
            :placeholder="remarkLocked ? '已自动关联上级字典键值' : '填写上级字典键值可实现多级联动'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getDictPage, getDictTypes, createDict, updateDict, deleteDict } from '@/api/dict'
import { useUserStore } from '@/stores/user'
import type { DictVO, DictSaveDTO } from '@/types/system'
import type { PageQuery } from '@/types/common'

const store = useUserStore()

const query = reactive<PageQuery & { dictType?: string; description?: string; dictLabel?: string }>({
  pageNum: 1,
  pageSize: 10
})
const total = ref(0)
const tableData = ref<DictVO[]>([])
const dictTypes = ref<string[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const submitLoading = ref(false)
const typeLocked = ref(false)
/** 备注锁定：添加键值时自动写入上级字典键值 */
const remarkLocked = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<DictSaveDTO>({ dictType: '', description: '', dictLabel: '', dictValue: '', sort: 0, remark: '' })

const dialogTitle = computed(() => (form.id ? '编辑字典' : '新增字典'))

const formRules: FormRules = {
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}

/** 序号按分页连续递增 */
const indexMethod = (index: number) => (query.pageNum - 1) * query.pageSize + index + 1

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDictPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadTypes = async () => {
  const res = await getDictTypes()
  dictTypes.value = res.data
}

const handleQuery = () => {
  query.pageNum = 1
  loadData()
}

const resetQuery = () => {
  query.dictType = undefined
  query.description = undefined
  query.dictLabel = undefined
  handleQuery()
}

const openDialog = (row?: DictVO) => {
  typeLocked.value = false
  remarkLocked.value = false
  if (row) {
    Object.assign(form, {
      id: row.id,
      dictType: row.dictType,
      description: row.description,
      dictLabel: row.dictLabel,
      dictValue: row.dictValue,
      sort: row.sort,
      remark: row.remark
    })
  } else {
    Object.assign(form, { id: undefined, dictType: '', description: '', dictLabel: '', dictValue: '', sort: 0, remark: '' })
  }
  dialogVisible.value = true
}

/** 添加键值：锁定字典类型，备注自动写入上级（一级）字典键值，实现多级联动 */
const openAddKeyDialog = (row: DictVO) => {
  typeLocked.value = true
  remarkLocked.value = true
  Object.assign(form, {
    id: undefined,
    dictType: row.dictType,
    description: row.description ?? '',
    dictLabel: '',
    dictValue: '',
    sort: 0,
    // 备注保存一级字典键值：二级项沿用其一级键值，新增子级时取当前行键值
    remark: row.remark?.trim() || row.dictValue
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) {
      await updateDict(form)
    } else {
      await createDict(form)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    await Promise.all([loadData(), loadTypes()])
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row: DictVO) => {
  ElMessageBox.confirm(`确认删除字典「${row.dictType} / ${row.dictLabel}」？`, '提示', { type: 'warning' }).then(async () => {
    await deleteDict(row.id)
    ElMessage.success('删除成功')
    await Promise.all([loadData(), loadTypes()])
  })
}

onMounted(() => {
  loadData()
  loadTypes()
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
.query-card :deep(.el-select),
.query-card :deep(.el-input) {
  width: 100%;
}
.query-card :deep(.el-form-item:last-child) {
  flex: 0 0 auto;
  margin-right: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
