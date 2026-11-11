<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>菜单树</span>
          <el-button v-if="store.hasPerm('system:menu:add')" type="primary" icon="Plus" @click="openDialog()">新增菜单</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="id" :tree-props="{ children: 'children' }">
        <el-table-column prop="menuName" label="菜单名称" min-width="200" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.menuType === 'M'" type="danger">目录</el-tag>
            <el-tag v-else-if="row.menuType === 'C'" type="warning">菜单</el-tag>
            <el-tag v-else type="info">按钮</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" width="160" />
        <el-table-column prop="perm" label="权限标识" width="180" />
        <el-table-column prop="icon" label="图标" width="100" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('system:menu:edit')" link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('system:menu:delete')" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="menuTree"
            :props="{ label: 'menuName', children: 'children', value: 'id' }"
            check-strictly
            :render-after-expand="false"
            style="width: 100%"
            placeholder="选择上级菜单（0为顶级）"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="菜单名称" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="路由路径" prop="path">
          <el-input v-model="form.path" placeholder="如 /system/user" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perm">
          <el-input v-model="form.perm" placeholder="如 system:user:list" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/menu'
import { useUserStore } from '@/stores/user'
import type { MenuVO, MenuSaveDTO } from '@/types/system'

const store = useUserStore()

const tableData = ref<MenuVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<MenuSaveDTO>({ parentId: '0', menuName: '', menuType: 'C', path: '', perm: '', icon: '', sort: 0, status: 1 })
const menuTree = ref<MenuVO[]>([])

const formRules: FormRules = {
  parentId: [{ required: true, message: '请选择上级菜单', trigger: 'change' }],
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMenuTree()
    tableData.value = res.data
    menuTree.value = [{ id: '0', menuName: '顶级菜单', parentId: '-1', menuType: 'M', path: '', perm: '', icon: '', sort: 0, status: 1, children: res.data } as MenuVO]
  } finally {
    loading.value = false
  }
}

const openDialog = (row?: MenuVO) => {
  if (row) {
    Object.assign(form, { id: row.id, parentId: row.parentId, menuName: row.menuName, menuType: row.menuType, path: row.path, perm: row.perm, icon: row.icon, sort: row.sort, status: row.status })
  } else {
    Object.assign(form, { id: undefined, parentId: '0', menuName: '', menuType: 'C', path: '', perm: '', icon: '', sort: 0, status: 1 })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) {
      await updateMenu(form)
    } else {
      await createMenu(form)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row: MenuVO) => {
  ElMessageBox.confirm(`确认删除菜单「${row.menuName}」？`, '提示', { type: 'warning' }).then(async () => {
    await deleteMenu(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
