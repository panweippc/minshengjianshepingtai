<template>
  <div class="village-page">
    <!-- 左侧：区划树（乡镇/村/组） -->
    <el-card shadow="never" class="tree-card">
      <div class="tree-search">
        <el-input v-model="treeKeyword" placeholder="搜索区划名称" prefix-icon="Search" clearable />
      </div>
      <div class="tree-body">
        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="treeProps"
          node-key="code"
          highlight-current
          default-expand-all
          :expand-on-click-node="false"
          :filter-node-method="filterTree"
          @node-click="handleTreeClick"
        >
          <template #default="{ data }">
            <span class="tree-node">
              <el-icon class="tree-node-icon" :class="{ 'is-town': data.regionType === '1' }">
                <LocationFilled v-if="data.regionType === '1'" />
                <Folder v-else />
              </el-icon>
              <span class="tree-node-name">{{ data.name }}</span>
              <span v-if="data.regionType" class="tree-node-count">{{ regionTypeLabel(data.regionType) }}</span>
            </span>
          </template>
        </el-tree>
        <el-empty v-if="!treeData.length" description="暂无区划数据" :image-size="80" />
      </div>
      <div class="tree-footer">
        <span>共 {{ total }} 条村委会记录</span>
        <el-button link type="primary" class="tree-all-btn" @click="handleTreeAll">全部</el-button>
      </div>
    </el-card>

    <!-- 右侧：筛选区 + 表单区 -->
    <div class="right-area">
      <el-card shadow="never" class="query-card">
        <el-form :inline="true" :model="query" @submit.prevent>
          <el-form-item label="村名">
            <el-input v-model="query.name" placeholder="请输入村名" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="党支部书记">
            <el-input v-model="query.secretary" placeholder="请输入党支部书记" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="村委会主任">
            <el-input v-model="query.director" placeholder="请输入村委会主任" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="card-header">
            <span>村委会列表</span>
            <el-button
              v-if="store.hasPerm('base:village:add')"
              type="primary"
              icon="Plus"
              @click="openAdd()"
            >
              新增
            </el-button>
          </div>
        </template>
        <el-table
          v-loading="loading"
          :data="tableData"
          row-key="code"
          :tree-props="{ children: 'children' }"
          :default-expand-all="true"
          border
          stripe
        >
          <el-table-column prop="name" label="区划名称" min-width="180" />
          <el-table-column prop="code" label="区划编码" min-width="150" />
          <el-table-column label="区划类型" width="110" align="center">
            <template #default="{ row }">{{ regionTypeLabel(row.regionType) }}</template>
          </el-table-column>
          <el-table-column label="村干部" min-width="270">
            <template #default="{ row }">
              <div class="leader-line">
                <span class="leader-label">党支部书记：</span>
                <span>{{ row.secretary || '—' }}</span>
                <span v-if="row.secretaryPhone" class="leader-phone">{{ row.secretaryPhone }}</span>
              </div>
              <div class="leader-line">
                <span class="leader-label">村委会主任：</span>
                <span>{{ row.director || '—' }}</span>
                <span v-if="row.directorPhone" class="leader-phone">{{ row.directorPhone }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="{ row }">
              <el-button link type="info" icon="View" @click="openDetail(row)">查看</el-button>
              <el-button
                v-if="store.hasPerm('base:village:edit')"
                link
                type="primary"
                icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="store.hasPerm('base:village:add')"
                link
                type="success"
                icon="Plus"
                @click="openAdd(row)"
              >
                添加下级
              </el-button>
              <el-button
                v-if="store.hasPerm('base:village:delete')"
                link
                type="danger"
                icon="Delete"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 新增/编辑/详情弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
      <el-descriptions v-if="dialogMode === 'detail'" :column="1" border>
        <el-descriptions-item label="区划名称">{{ detail?.name || '—' }}</el-descriptions-item>
        <el-descriptions-item label="区划编码">{{ detail?.code || '—' }}</el-descriptions-item>
        <el-descriptions-item label="区划类型">{{ regionTypeLabel(detail?.regionType) }}</el-descriptions-item>
        <el-descriptions-item label="党支部书记">{{ detail?.secretary || '—' }}</el-descriptions-item>
        <el-descriptions-item label="党支部书记电话">{{ detail?.secretaryPhone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="村委会主任">{{ detail?.director || '—' }}</el-descriptions-item>
        <el-descriptions-item label="村委会主任电话">{{ detail?.directorPhone || '—' }}</el-descriptions-item>
      </el-descriptions>

      <el-form v-else ref="formRef" :model="form" :rules="formRules" label-width="140px">
        <el-form-item label="上级区划">
          <el-cascader
            v-model="cascaderValue"
            :options="cascaderOptions"
            :props="cascaderProps"
            :disabled="dialogMode === 'edit'"
            clearable
            filterable
            placeholder="请选择上级区划（新增下级）或当前区划（编辑）"
            style="width: 100%"
            @change="handleCascaderChange"
          />
        </el-form-item>
        <el-form-item label="区划名称" prop="name">
          <el-input
            v-model="form.name"
            :disabled="dialogMode === 'edit'"
            placeholder="请输入区划名称"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="区划编码" prop="code">
          <el-input
            v-model="form.code"
            :disabled="dialogMode === 'edit'"
            placeholder="请输入区划编码"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="区划类型" prop="regionType">
          <el-select
            v-model="form.regionType"
            :disabled="dialogMode === 'edit'"
            placeholder="请选择区划类型"
            style="width: 100%"
          >
            <el-option
              v-for="item in regionTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="党支部书记" prop="secretary">
          <el-input v-model="form.secretary" placeholder="请输入党支部书记" maxlength="32" />
        </el-form-item>
        <el-form-item label="党支部书记电话" prop="secretaryPhone">
          <el-input v-model="form.secretaryPhone" placeholder="请输入党支部书记电话" maxlength="11" />
        </el-form-item>
        <el-form-item label="村委会主任" prop="director">
          <el-input v-model="form.director" placeholder="请输入村委会主任" maxlength="32" />
        </el-form-item>
        <el-form-item label="村委会主任电话" prop="directorPhone">
          <el-input v-model="form.directorPhone" placeholder="请输入村委会主任电话" maxlength="11" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">
          {{ dialogMode === 'detail' ? '关闭' : '取消' }}
        </el-button>
        <el-button
          v-if="dialogMode !== 'detail'"
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { LocationFilled, Folder } from '@element-plus/icons-vue'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getDictByType } from '@/api/dict'
import {
  getVillageTree,
  getVillageTreePage,
  getVillageDetail,
  createVillage,
  updateVillage,
  deleteVillage
} from '@/api/village'
import type { VillageQuery } from '@/api/village'
import type { DictVO, VillageTreeVO, VillageVO, VillageSaveDTO } from '@/types/system'

const store = useUserStore()

type DialogMode = 'add' | 'edit' | 'detail'

/** 区划树 ref，仅使用清除选中项与节点过滤方法 */
const treeRef = ref<{ setCurrentKey: (key?: string) => void; filter: (value: string) => void }>()
const treeProps = { label: 'name', children: 'children' }
const treeData = ref<VillageTreeVO[]>([])

/** 区划树搜索关键字 */
const treeKeyword = ref('')

/** 区划树节点过滤：按名称模糊匹配 */
const filterTree = (value: string, data: VillageTreeVO) => {
  if (!value) return true
  return (data.name || '').includes(value)
}

watch(treeKeyword, (val) => {
  treeRef.value?.filter(val)
})

/** 字典缺失时的兜底区划类型选项 */
const DEFAULT_REGION_TYPES = [
  { label: '乡镇', value: '1' },
  { label: '村', value: '2' },
  { label: '组', value: '3' }
]
const dictItems = ref<DictVO[]>([])
const regionTypeOptions = computed(() =>
  dictItems.value.length
    ? dictItems.value.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    : DEFAULT_REGION_TYPES
)

/** 级联选择器：从区划树转换 options 格式 */
const cascaderProps = { value: 'code', label: 'name', children: 'children' }
const cascaderValue = ref<string[]>([])
const cascaderOptions = computed(() => treeData.value)

/** 根据编码在区划树中查找完整路径 */
const findPathInTree = (code: string, nodes: VillageTreeVO[], path: string[] = []): string[] | null => {
  for (const node of nodes) {
    const next = [...path, node.code]
    if (node.code === code) return next
    if (node.children?.length) {
      const hit = findPathInTree(code, node.children, next)
      if (hit) return hit
    }
  }
  return null
}

/** 级联选择变化：最后一级作为父级（新增模式），自动填充 parentCode 与 regionType */
const handleCascaderChange = (val: string[]) => {
  if (!val?.length) {
    form.parentCode = undefined
    return
  }
  const lastCode = val[val.length - 1]
  // 从 treeData 中找到最后一级节点
  const findNode = (nodes: VillageTreeVO[]): VillageTreeVO | undefined => {
    for (const n of nodes) {
      if (n.code === lastCode) return n
      if (n.children?.length) {
        const hit = findNode(n.children)
        if (hit) return hit
      }
    }
    return undefined
  }
  const node = findNode(treeData.value)
  if (!node) return

  if (dialogMode.value === 'add') {
    // 新增模式：选中的是父级，自动填充 parentCode 和下一级区划类型
    form.parentCode = node.code
    form.regionType = String(Math.min(Number(node.regionType) + 1, 3))
  }
}

const query = reactive<VillageQuery>({
  pageNum: 1,
  pageSize: 10
})
const total = ref(0)
const tableData = ref<VillageTreeVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref<DialogMode>('add')
const submitLoading = ref(false)
const detail = ref<VillageVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<VillageSaveDTO>({
  code: '',
  name: '',
  regionType: '1',
  secretary: '',
  secretaryPhone: '',
  director: '',
  directorPhone: '',
  sort: 0
})

const dialogTitle = computed(() => {
  if (dialogMode.value === 'detail') return '区划详情'
  if (dialogMode.value === 'edit') return '编辑区划'
  return form.parentCode ? '新增下级区划' : '新增区划'
})

/** 手机号校验：非必填，填写时须为11位手机号 */
const validatePhone: FormItemRule['validator'] = (_rule, value, callback) => {
  const phone = String(value ?? '').trim()
  if (!phone || /^1[3-9]\d{9}$/.test(phone)) {
    callback()
  } else {
    callback(new Error('请输入正确的手机号'))
  }
}

const formRules: FormRules = {
  name: [{ required: true, message: '请输入区划名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入区划编码', trigger: 'blur' },
    { pattern: /^[0-9A-Za-z-]+$/, message: '仅支持数字、字母和短横线', trigger: 'blur' }
  ],
  regionType: [{ required: true, message: '请选择区划类型', trigger: 'change' }],
  secretaryPhone: [{ validator: validatePhone, trigger: 'blur' }],
  directorPhone: [{ validator: validatePhone, trigger: 'blur' }]
}

const regionTypeLabel = (value?: string) => {
  if (!value) return '—'
  const hit = regionTypeOptions.value.find((item) => item.value === value)
  return hit ? hit.label : value
}

const loadDict = async () => {
  const res = await getDictByType('qu_hua_lei_xing')
  dictItems.value = res.data || []
}

const loadTree = async () => {
  const res = await getVillageTree()
  treeData.value = res.data || []
}

/** 递归统计树节点总数 */
const countTreeNodes = (nodes: VillageTreeVO[]): number => {
  let n = 0
  for (const node of nodes) {
    n += 1 + countTreeNodes(node.children || [])
  }
  return n
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getVillageTreePage({
      name: query.name,
      secretary: query.secretary,
      director: query.director,
      nodeCode: query.nodeCode
    })
    tableData.value = res.data || []
    total.value = countTreeNodes(tableData.value)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  loadData()
}

const resetQuery = () => {
  query.name = undefined
  query.secretary = undefined
  query.director = undefined
  handleQuery()
}

/** 点击树节点：筛选所选节点及其直属下级 */
const handleTreeClick = (data: VillageTreeVO) => {
  query.nodeCode = data.code
  handleQuery()
}

/** 全部：清除树节点筛选 */
const handleTreeAll = () => {
  query.nodeCode = undefined
  treeRef.value?.setCurrentKey()
  handleQuery()
}

const resetForm = () => {
  Object.assign(form, {
    code: '',
    name: '',
    parentCode: undefined,
    regionType: '1',
    secretary: '',
    secretaryPhone: '',
    director: '',
    directorPhone: '',
    sort: 0
  })
  cascaderValue.value = []
}

/** 新增；传入父级时自动匹配下一级区划类型 */
const openAdd = (parent?: VillageVO) => {
  dialogMode.value = 'add'
  resetForm()
  if (parent) {
    form.parentCode = parent.code
    form.regionType = String(Math.min(Number(parent.regionType) + 1, 3))
    // 回显父级在级联选择器中的完整路径
    const path = findPathInTree(parent.code, treeData.value)
    cascaderValue.value = path ?? []
  }
  dialogVisible.value = true
}

const openEdit = (row: VillageVO) => {
  dialogMode.value = 'edit'
  Object.assign(form, {
    code: row.code,
    name: row.name,
    parentCode: row.parentCode,
    regionType: row.regionType,
    secretary: row.secretary ?? '',
    secretaryPhone: row.secretaryPhone ?? '',
    director: row.director ?? '',
    directorPhone: row.directorPhone ?? '',
    sort: row.sort ?? 0
  })
  // 回显当前区划在级联选择器中的完整路径
  const path = findPathInTree(row.code, treeData.value)
  cascaderValue.value = path ?? []
  dialogVisible.value = true
}

const openDetail = async (row: VillageVO) => {
  const res = await getVillageDetail(row.code)
  detail.value = res.data
  dialogMode.value = 'detail'
  dialogVisible.value = true
}

/** 递归在树形表格中查找节点并更新 */
const updateNodeInTree = (nodes: VillageTreeVO[], code: string, patch: Partial<VillageTreeVO>): boolean => {
  for (const node of nodes) {
    if (node.code === code) {
      Object.assign(node, patch)
      return true
    }
    if (node.children?.length && updateNodeInTree(node.children, code, patch)) {
      return true
    }
  }
  return false
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (dialogMode.value === 'edit') {
      await updateVillage({ ...form })
      // 编辑：直接更新现有树节点，保留展开状态和滚动位置
      updateNodeInTree(tableData.value, form.code!, {
        name: form.name,
        regionType: form.regionType,
        secretary: form.secretary,
        secretaryPhone: form.secretaryPhone,
        director: form.director,
        directorPhone: form.directorPhone
      })
      updateNodeInTree(treeData.value, form.code!, { name: form.name })
    } else {
      await createVillage({ ...form })
      // 新增：涉及结构变化，需要重新加载
      await Promise.all([loadTree(), loadData()])
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row: VillageVO) => {
  ElMessageBox.confirm(`确认删除区划「${row.name}」？`, '提示', { type: 'warning' }).then(
    async () => {
      await deleteVillage(row.code)
      ElMessage.success('删除成功')
      if (query.nodeCode === row.code) {
        query.nodeCode = undefined
        treeRef.value?.setCurrentKey()
      }
      await Promise.all([loadTree(), loadData()])
    }
  )
}

onMounted(() => {
  loadDict()
  loadTree()
  loadData()
})
</script>

<style scoped>
.village-page {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.tree-card {
  flex: 0 0 300px;
  width: 300px;
}
.tree-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  padding: 12px;
  min-height: calc(100vh - 170px);
}
.tree-search {
  margin-bottom: 8px;
}
.tree-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.tree-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #909399;
}
.tree-all-btn {
  font-size: 12px;
}
.right-area {
  flex: 1;
  min-width: 0;
}
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
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  overflow: hidden;
}
.tree-node-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #303133;
}
/* 区划树节点样式与公共基础设施保持一致 */
.tree-card :deep(.el-tree-node__content) {
  height: 36px;
  border-radius: 4px;
}
.tree-card :deep(.el-tree-node__content:hover) {
  background: #f5f7fa;
}
.tree-card :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: #e8f2ff;
}
.tree-node-icon {
  flex: 0 0 auto;
  font-size: 15px;
  color: #e6a23c;
}
.tree-node-icon.is-town {
  color: #3b82f6;
}
.tree-node-count {
  flex: 0 0 auto;
  min-width: 34px;
  height: 20px;
  padding: 0 6px;
  line-height: 20px;
  border-radius: 10px;
  background: #f2f3f5;
  color: #909399;
  font-size: 12px;
  text-align: center;
}
.leader-line {
  line-height: 22px;
}
.leader-label {
  color: #909399;
}
.leader-phone {
  margin-left: 8px;
  color: #909399;
}
</style>
