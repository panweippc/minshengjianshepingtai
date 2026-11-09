<template>
  <div>
    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="角色名称" clearable style="width: 180px" />
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
          <span>角色列表</span>
          <el-button v-if="store.hasPerm('system:role:add')" type="primary" icon="Plus" @click="openDialog()">新增角色</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="roleName" label="角色名称" width="160" />
        <el-table-column prop="roleKey" label="角色标识" width="140" />
        <el-table-column label="数据权限" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.dataScope === 1" type="danger">全部数据</el-tag>
            <el-tag v-else-if="row.dataScope === 2" type="warning">本区及以下</el-tag>
            <el-tag v-else type="info">仅本区</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('system:role:edit')" link type="primary" icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('system:role:edit')" link type="warning" icon="Menu" @click="openMenuDialog(row)">分配权限</el-button>
            <el-button v-if="store.hasPerm('system:role:delete')" link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
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
        @current-change="handleQuery"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="角色名称" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="form.roleKey" :disabled="!!form.id" placeholder="如 operator" />
        </el-form-item>
        <el-form-item label="数据权限" prop="dataScope">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="全部数据" :value="1" />
            <el-option label="本区及以下" :value="2" />
            <el-option label="仅本区" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="menuVisible" title="分配菜单权限" width="400px" destroy-on-close>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'menuName', children: 'children' }"
        default-expand-all
      />
      <template #footer>
        <el-button @click="menuVisible = false">取消</el-button>
        <el-button type="primary" :loading="menuLoading" @click="handleAssignMenus">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getRolePage, createRole, updateRole, deleteRole, assignMenus } from '@/api/role'
import { getMenuTree } from '@/api/menu'
import { useUserStore } from '@/stores/user'
import type { RoleVO, RoleSaveDTO, MenuVO } from '@/types/system'
import type { PageQuery } from '@/types/common'

const store = useUserStore()

const query = reactive<PageQuery & { roleName?: string }>({ pageNum: 1, pageSize: 10 })
const total = ref(0)
const tableData = ref<RoleVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<RoleSaveDTO>({ roleName: '', roleKey: '', dataScope: 3, status: 1, remark: '' })

const menuVisible = ref(false)
const menuLoading = ref(false)
const menuTreeRef = ref()
const menuTree = ref<MenuVO[]>([])
const currentRoleId = ref('')

const formRules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
  dataScope: [{ required: true, message: '请选择数据权限', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRolePage(query)
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
  query.roleName = ''
  handleQuery()
}

const openDialog = (row?: RoleVO) => {
  if (row) {
    Object.assign(form, { id: row.id, roleName: row.roleName, roleKey: row.roleKey, dataScope: row.dataScope, status: row.status, remark: row.remark })
  } else {
    Object.assign(form, { id: undefined, roleName: '', roleKey: '', dataScope: 3, status: 1, remark: '' })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) {
      await updateRole(form)
    } else {
      await createRole(form)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const openMenuDialog = async (row: RoleVO) => {
  currentRoleId.value = row.id
  const res = await getMenuTree()
  menuTree.value = res.data
  menuVisible.value = true
}

const handleAssignMenus = async () => {
  const checked = menuTreeRef.value?.getCheckedKeys() as string[]
  const halfChecked = menuTreeRef.value?.getHalfCheckedKeys() as string[]
  const ids = [...(checked || []), ...(halfChecked || [])]
  menuLoading.value = true
  try {
    await assignMenus(currentRoleId.value, ids)
    ElMessage.success('权限分配成功')
    menuVisible.value = false
  } finally {
    menuLoading.value = false
  }
}

const handleDelete = (row: RoleVO) => {
  ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' }).then(async () => {
    await deleteRole(row.id)
    ElMessage.success('删除成功')
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
