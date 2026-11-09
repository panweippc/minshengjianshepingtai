<template>
  <div>
    <!-- 查询栏 -->
    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="登录账号">
          <el-input v-model="query.username" placeholder="登录账号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="姓名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never" style="margin-top: 12px">
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
          <el-button v-if="store.hasPerm('system:user:add')" type="primary" icon="Plus" @click="openDialog()">新增用户</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="username" label="登录账号" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="regionName" label="所属区划" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('system:user:edit')" link type="primary" icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('system:user:edit')" link type="warning" icon="Key" @click="openPasswordDialog(row)">重置密码</el-button>
            <el-button v-if="store.hasPerm('system:user:delete')" link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="3-32位字母数字下划线" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="登录密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="初始密码" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="所属区划" prop="regionCode">
          <el-select v-model="form.regionCode" placeholder="选择行政区划" clearable filterable style="width: 100%">
            <el-option v-for="r in regionList" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="分配角色" style="width: 100%">
            <el-option v-for="r in roleList" :key="r.id" :label="r.roleName" :value="r.id" />
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

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="重置密码" width="400px" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" placeholder="新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handleResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getUserPage, createUser, updateUser, deleteUser, resetPassword } from '@/api/user'
import { getRoleAll } from '@/api/role'
import { getRegionList } from '@/api/region'
import { useUserStore } from '@/stores/user'
import type { UserVO, UserSaveDTO, RoleVO, RegionVO } from '@/types/system'
import type { PageQuery } from '@/types/common'

const store = useUserStore()

// 查询与分页
const query = reactive<PageQuery & { username?: string; realName?: string }>({ pageNum: 1, pageSize: 10 })
const total = ref(0)
const tableData = ref<UserVO[]>([])
const loading = ref(false)

// 弹窗
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<UserSaveDTO>({ username: '', password: '', realName: '', regionCode: '', phone: '', status: 1, remark: '', roleIds: [] })

// 重置密码
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ id: '', password: '' })

// 下拉数据
const roleList = ref<RoleVO[]>([])
const regionList = ref<RegionVO[]>([])

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,32}$/, message: '须为3-32位字母数字下划线', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const pwdRules: FormRules = {
  password: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserPage(query)
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
  query.realName = ''
  handleQuery()
}

const openDialog = (row?: UserVO) => {
  if (row) {
    Object.assign(form, { id: row.id, username: row.username, realName: row.realName, regionCode: row.regionCode, phone: row.phone, status: row.status, remark: row.remark, roleIds: [] })
  } else {
    Object.assign(form, { id: undefined, username: '', password: '', realName: '', regionCode: '', phone: '', status: 1, remark: '', roleIds: [] })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) {
      await updateUser(form)
    } else {
      await createUser(form)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const openPasswordDialog = (row: UserVO) => {
  pwdForm.id = row.id
  pwdForm.password = ''
  pwdVisible.value = true
}

const handleResetPassword = async () => {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    await resetPassword(pwdForm.id, pwdForm.password)
    ElMessage.success('密码重置成功')
    pwdVisible.value = false
  } finally {
    pwdLoading.value = false
  }
}

const handleDelete = (row: UserVO) => {
  ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' }).then(async () => {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(() => {
  loadData()
  getRoleAll().then((res) => (roleList.value = res.data))
  getRegionList().then((res) => (regionList.value = res.data))
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
