<template>
  <div>
    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="操作人账号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="模块名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
          <el-button v-if="store.hasPerm('system:log:delete')" type="danger" icon="Delete" @click="handleClear">清空日志</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="operation" label="操作" width="120" />
        <el-table-column prop="method" label="请求方式" width="90" align="center">
          <template #default="{ row }">
            {{ methodLabel(row.method) }}
          </template>
        </el-table-column>
        <el-table-column prop="url" label="请求地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.resultCode === 200 ? 'success' : 'danger'">{{ row.resultCode === 200 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时(ms)" width="100" align="right" />
        <el-table-column prop="operTime" label="操作时间" width="180" />
      </el-table>
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="handleQuery"
        @current-change="handleQuery"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLogPage, clearLogs } from '@/api/log'
import { useUserStore } from '@/stores/user'
import type { OperLogVO } from '@/types/system'
import type { PageQuery } from '@/types/common'

const store = useUserStore()

const query = reactive<PageQuery & { username?: string; module?: string }>({ pageNum: 1, pageSize: 10 })
const total = ref(0)
const tableData = ref<OperLogVO[]>([])
const loading = ref(false)

/** HTTP 请求方式 → 中文语义映射（RESTful：GET查询 POST新增 PUT修改 DELETE删除） */
const METHOD_LABELS: Record<string, string> = {
  GET: '查询',
  POST: '新增',
  PUT: '修改',
  DELETE: '删除'
}

const methodLabel = (method: unknown): string => {
  const key = typeof method === 'string' ? method.toUpperCase() : ''
  return METHOD_LABELS[key] ?? String(method ?? '')
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLogPage(query)
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
  query.username = ''
  query.module = ''
  handleQuery()
}

const handleClear = () => {
  ElMessageBox.confirm('确认清空全部操作日志？', '警告', { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' }).then(async () => {
    await clearLogs()
    ElMessage.success('日志已清空')
    loadData()
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
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
