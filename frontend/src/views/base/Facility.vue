<template>
  <div class="facility-page">
    <!-- 左侧：区划树（乡镇/村/组） -->
    <el-card shadow="never" class="tree-card">
      <template #header>
        <div class="card-header">
          <span>区划树</span>
          <el-button link type="primary" @click="handleTreeAll">全部</el-button>
        </div>
      </template>
      <div class="tree-body">
        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="treeProps"
          node-key="code"
          highlight-current
          default-expand-all
          :expand-on-click-node="false"
          @node-click="handleTreeClick"
        >
          <template #default="{ data }">
            <span class="tree-node">
              <span class="tree-node-name">{{ data.name }}</span>
              <el-tag v-if="data.regionType" size="small" type="info" effect="plain">
                {{ regionTypeLabel(data.regionType) }}
              </el-tag>
            </span>
          </template>
        </el-tree>
        <el-empty v-if="!treeData.length" description="暂无区划数据" :image-size="80" />
      </div>
    </el-card>

    <!-- 右侧：筛选区 + 列表区 -->
    <div class="right-area">
      <el-card shadow="never" class="query-card">
        <el-form :inline="true" :model="query" @submit.prevent>
          <el-form-item label="设施编号">
            <el-input v-model="query.facilityCode" placeholder="请输入设施编号" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="设施名称">
            <el-input v-model="query.facilityName" placeholder="请输入设施名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="设施类型">
            <el-select v-model="query.facilityType" placeholder="请选择设施类型" clearable style="width: 100%">
              <el-option
                v-for="item in facilityTypeOptions"
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

      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="card-header">
            <span>公共基础设施列表</span>
            <el-button
              v-if="store.hasPerm('base:facility:add')"
              type="primary"
              icon="Plus"
              @click="openAdd()"
            >
              新增
            </el-button>
          </div>
        </template>
        <el-table v-loading="loading" :data="tableData" border stripe>
          <el-table-column prop="regionName" label="行政村" min-width="120" />
          <el-table-column prop="facilityCode" label="设施编号" min-width="130" />
          <el-table-column prop="facilityName" label="设施名称" min-width="160" />
          <el-table-column label="设施类型" width="110" align="center">
            <template #default="{ row }">{{ facilityTypeLabel(row.facilityType) }}</template>
          </el-table-column>
          <el-table-column prop="buildYear" label="建成年份" width="100" align="center">
            <template #default="{ row }">{{ row.buildYear || '—' }}</template>
          </el-table-column>
          <el-table-column label="总数" width="120" align="center">
            <template #default="{ row }">
              {{ row.quantity ?? '—' }}<span v-if="row.unit"> {{ row.unit }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button link type="info" icon="View" @click="openDetail(row)">查看</el-button>
              <el-button
                v-if="store.hasPerm('base:facility:edit')"
                link
                type="primary"
                icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="store.hasPerm('base:facility:delete')"
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
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" :title="editDialogMode === 'edit' ? '编辑公共基础设施' : '新增公共基础设施'" width="800px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设施编号" prop="facilityCode">
              <el-input
                v-model="form.facilityCode"
                :disabled="editDialogMode === 'edit'"
                placeholder="请输入设施编号"
                maxlength="32"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设施名称" prop="facilityName">
              <el-input v-model="form.facilityName" placeholder="请输入设施名称" maxlength="128" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设施类型" prop="facilityType">
              <el-select v-model="form.facilityType" placeholder="请选择设施类型" style="width: 100%">
                <el-option
                  v-for="item in facilityTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行政村" prop="regionCode">
              <el-tree-select
                v-model="form.regionCode"
                :data="villageTreeSelectOptions"
                :props="{ label: 'name', value: 'code', children: 'children' }"
                check-strictly
                default-expand-all
                clearable
                filterable
                placeholder="请选择行政村"
                style="width: 100%"
                @change="handleRegionChange"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="具体位置" prop="location">
              <el-input v-model="form.location" placeholder="请输入具体位置" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="建成年份" prop="buildYear">
              <el-input-number
                v-model="form.buildYear"
                :min="1900"
                :max="2100"
                placeholder="请输入建成年份"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数量" prop="quantity">
              <el-input-number
                v-model="form.quantity"
                :min="0"
                :precision="2"
                placeholder="请输入数量"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如：个/公里/平方米" maxlength="16" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 附件列表 -->
        <el-divider content-position="left">
          附件列表
          <el-button type="primary" link icon="Plus" style="margin-left: 12px" @click="addAttachment">
            添加附件
          </el-button>
        </el-divider>
        <div v-if="!form.attachments || form.attachments.length === 0" class="empty-tip">
          暂无附件，请点击上方「添加附件」
        </div>
        <div v-for="(att, idx) in form.attachments" :key="idx" class="attachment-item">
          <el-row :gutter="12" align="middle">
            <el-col :span="6">
              <el-form-item :prop="`attachments.${idx}.attachmentType`">
                <el-select v-model="att.attachmentType" placeholder="附件类型" style="width: 100%">
                  <el-option
                    v-for="item in attachmentTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :prop="`attachments.${idx}.fileName`">
                <el-input v-model="att.fileName" placeholder="文件名" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item :prop="`attachments.${idx}.remark`">
                <el-input v-model="att.remark" placeholder="备注" />
              </el-form-item>
            </el-col>
            <el-col :span="4">
              <el-button type="danger" link icon="Delete" @click="removeAttachment(idx)">删除</el-button>
            </el-col>
          </el-row>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="公共基础设施详情" width="800px" destroy-on-close>
      <el-tabs v-model="detailActiveTab">
        <!-- 档案信息 -->
        <el-tab-pane label="档案信息" name="info">
          <el-descriptions v-if="detail" :column="2" border>
            <el-descriptions-item label="行政村">{{ detail.regionName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="设施编号">{{ detail.facilityCode || '—' }}</el-descriptions-item>
            <el-descriptions-item label="设施名称">{{ detail.facilityName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="设施类型">{{ facilityTypeLabel(detail.facilityType) }}</el-descriptions-item>
            <el-descriptions-item label="具体位置" :span="2">{{ detail.location || '—' }}</el-descriptions-item>
            <el-descriptions-item label="建成年份">{{ detail.buildYear || '—' }}</el-descriptions-item>
            <el-descriptions-item label="数量">
              {{ detail.quantity ?? '—' }}<span v-if="detail.unit"> {{ detail.unit }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <h4 class="section-title">附件列表</h4>
          <el-table :data="detail?.attachments || []" border size="small">
            <el-table-column label="附件类型" width="120" align="center">
              <template #default="{ row }">{{ attachmentTypeLabel(row.attachmentType) }}</template>
            </el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="200">
              <template #default="{ row }">{{ row.fileName || '—' }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150">
              <template #default="{ row }">{{ row.remark || '—' }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!detail?.attachments?.length" description="暂无附件" :image-size="60" />
        </el-tab-pane>

        <!-- 项目信息 -->
        <el-tab-pane label="项目信息" name="project">
          <el-table :data="detail?.projects || []" border>
            <el-table-column prop="projectCode" label="项目编码" min-width="130" />
            <el-table-column prop="projectName" label="项目名称" min-width="180" />
            <el-table-column prop="projectType" label="类型" width="100" align="center">
              <template #default="{ row }">{{ row.projectType || '—' }}</template>
            </el-table-column>
            <el-table-column label="进度" width="120">
              <template #default="{ row }">
                <el-progress
                  v-if="row.progress != null"
                  :percentage="row.progress"
                  :stroke-width="10"
                  :show-text="true"
                />
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="预算" width="130" align="right">
              <template #default="{ row }">{{ row.budget != null ? formatMoney(row.budget) : '—' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">{{ row.status || '—' }}</template>
            </el-table-column>
            <el-table-column prop="startYear" label="立项年度" width="100" align="center">
              <template #default="{ row }">{{ row.startYear || '—' }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!detail?.projects?.length" description="暂无关联项目" :image-size="80" />
        </el-tab-pane>
      </el-tabs>
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
import { useUserStore } from '@/stores/user'
import { getDictByType } from '@/api/dict'
import {
  getVillageTree
} from '@/api/village'
import {
  getFacilityPage,
  getFacilityDetail,
  createFacility,
  updateFacility,
  deleteFacility
} from '@/api/facility'
import type { FacilityQuery } from '@/api/facility'
import type {
  DictVO,
  VillageTreeVO,
  FacilityVO,
  FacilityDetailVO,
  FacilitySaveDTO,
  FacilityAttachmentSaveDTO
} from '@/types/system'

const store = useUserStore()

type EditDialogMode = 'add' | 'edit'

/** 区划树 */
const treeRef = ref<{ setCurrentKey: (key?: string) => void }>()
const treeProps = { label: 'name', children: 'children' }
const treeData = ref<VillageTreeVO[]>([])
const selectedTreeNode = ref<VillageTreeVO | null>(null)

/** 字典 */
const DEFAULT_FACILITY_TYPES = [
  { label: '水利设施', value: '1' },
  { label: '交通设施', value: '2' },
  { label: '电力设施', value: '3' },
  { label: '通讯设施', value: '4' },
  { label: '公共服务', value: '5' },
  { label: '环卫设施', value: '6' }
]
const DEFAULT_ATTACHMENT_TYPES = [
  { label: '照片', value: '1' },
  { label: '合同', value: '2' },
  { label: '图纸', value: '3' },
  { label: '检测报告', value: '4' },
  { label: '其他', value: '5' }
]
const regionDictItems = ref<DictVO[]>([])
const facilityTypeDictItems = ref<DictVO[]>([])
const attachmentTypeDictItems = ref<DictVO[]>([])

const regionTypeOptions = computed(() =>
  regionDictItems.value.length
    ? regionDictItems.value.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    : [{ label: '乡镇', value: '1' }, { label: '村', value: '2' }, { label: '组', value: '3' }]
)
const facilityTypeOptions = computed(() =>
  facilityTypeDictItems.value.length
    ? facilityTypeDictItems.value.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    : DEFAULT_FACILITY_TYPES
)
const attachmentTypeOptions = computed(() =>
  attachmentTypeDictItems.value.length
    ? attachmentTypeDictItems.value.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    : DEFAULT_ATTACHMENT_TYPES
)

/** 树形下拉选项（行政村级联，直接复用区划树） */
const villageTreeSelectOptions = computed(() => treeData.value)

/** 根据选中的区划编码设置行政村名称 */
const handleRegionChange = (code: string) => {
  if (!code) {
    form.regionName = ''
    return
  }
  const find = (nodes: VillageTreeVO[], target: string): VillageTreeVO | undefined => {
    for (const n of nodes) {
      if (n.code === target) return n
      const hit = find(n.children || [], target)
      if (hit) return hit
    }
    return undefined
  }
  const node = find(treeData.value, code)
  form.regionName = node?.name || ''
}

/** 查询与列表 */
const query = reactive<FacilityQuery>({
  pageNum: 1,
  pageSize: 10
})
const total = ref(0)
const tableData = ref<FacilityVO[]>([])
const loading = ref(false)

/** 新增/编辑弹窗 */
const editDialogVisible = ref(false)
const editDialogMode = ref<EditDialogMode>('add')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const emptyAttachment = (): FacilityAttachmentSaveDTO => ({
  attachmentType: '',
  fileName: '',
  remark: ''
})

const form = reactive<FacilitySaveDTO>({
  facilityCode: '',
  facilityName: '',
  facilityType: '',
  regionCode: '',
  regionName: '',
  location: '',
  buildYear: undefined,
  quantity: undefined,
  unit: '',
  remark: '',
  attachments: []
})

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    facilityCode: '',
    facilityName: '',
    facilityType: '',
    regionCode: '',
    regionName: '',
    location: '',
    buildYear: undefined,
    quantity: undefined,
    unit: '',
    remark: '',
    attachments: [] as FacilityAttachmentSaveDTO[]
  })
}

const formRules: FormRules = {
  facilityCode: [
    { required: true, message: '请输入设施编号', trigger: 'blur' },
    { pattern: /^[0-9A-Za-z_-]+$/, message: '仅支持数字、字母、下划线和短横线', trigger: 'blur' }
  ],
  facilityName: [{ required: true, message: '请输入设施名称', trigger: 'blur' }],
  regionCode: [{ required: true, message: '请选择行政村', trigger: 'change' }]
}

/** 查看弹窗 */
const detailDialogVisible = ref(false)
const detailActiveTab = ref('info')
const detail = ref<FacilityDetailVO | null>(null)

/** 字典标签 */
const regionTypeLabel = (value?: string) => {
  if (!value) return '—'
  const hit = regionTypeOptions.value.find((item) => item.value === value)
  return hit ? hit.label : value
}
const facilityTypeLabel = (value?: string) => {
  if (!value) return '—'
  const hit = facilityTypeOptions.value.find((item) => item.value === value)
  return hit ? hit.label : value
}
const attachmentTypeLabel = (value?: string) => {
  if (!value) return '—'
  const hit = attachmentTypeOptions.value.find((item) => item.value === value)
  return hit ? hit.label : value
}

/** 金额格式化 */
const formatMoney = (value: number) => {
  return '¥' + value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 加载字典 */
const loadDict = async () => {
  const [res1, res2, res3] = await Promise.all([
    getDictByType('qu_hua_lei_xing'),
    getDictByType('she_shi_lei_xing'),
    getDictByType('fu_jian_lei_xing')
  ])
  regionDictItems.value = res1.data || []
  facilityTypeDictItems.value = res2.data || []
  attachmentTypeDictItems.value = res3.data || []
}

/** 加载区划树 */
const loadTree = async () => {
  const res = await getVillageTree()
  treeData.value = res.data || []
}

/** 加载列表数据 */
const loadData = async () => {
  loading.value = true
  try {
    const res = await getFacilityPage(query)
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
  query.facilityCode = undefined
  query.facilityName = undefined
  query.facilityType = undefined
  handleQuery()
}

/** 点击树节点 */
const handleTreeClick = (data: VillageTreeVO) => {
  selectedTreeNode.value = data
  query.nodeCode = data.code
  handleQuery()
}

/** 全部：清除树节点筛选 */
const handleTreeAll = () => {
  selectedTreeNode.value = null
  query.nodeCode = undefined
  treeRef.value?.setCurrentKey()
  handleQuery()
}

/** 添加附件 */
const addAttachment = () => {
  form.attachments = form.attachments || []
  form.attachments.push(emptyAttachment())
}

/** 删除附件 */
const removeAttachment = (idx: number) => {
  form.attachments?.splice(idx, 1)
}

/** 新增 */
const openAdd = () => {
  editDialogMode.value = 'add'
  resetForm()
  editDialogVisible.value = true
}

/** 编辑 */
const openEdit = async (row: FacilityVO) => {
  editDialogMode.value = 'edit'
  const res = await getFacilityDetail(row.id)
  const data = res.data
  Object.assign(form, {
    id: data.id,
    facilityCode: data.facilityCode,
    facilityName: data.facilityName,
    facilityType: data.facilityType || '',
    regionCode: data.regionCode,
    regionName: data.regionName || '',
    location: data.location || '',
    buildYear: data.buildYear,
    quantity: data.quantity,
    unit: data.unit || '',
    remark: data.remark || '',
    attachments: (data.attachments || []).map((a) => ({
      id: a.id,
      attachmentType: a.attachmentType || '',
      fileName: a.fileName || '',
      filePath: a.filePath || '',
      fileHash: undefined,
      fileSize: a.fileSize,
      remark: a.remark || ''
    }))
  })
  editDialogVisible.value = true
}

/** 查看 */
const openDetail = async (row: FacilityVO) => {
  const res = await getFacilityDetail(row.id)
  detail.value = res.data
  detailActiveTab.value = 'info'
  detailDialogVisible.value = true
}

/** 提交 */
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (editDialogMode.value === 'edit') {
      await updateFacility({ ...form })
    } else {
      await createFacility({ ...form })
    }
    ElMessage.success('操作成功')
    editDialogVisible.value = false
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

/** 删除 */
const handleDelete = (row: FacilityVO) => {
  ElMessageBox.confirm(`确认删除设施「${row.facilityName}」？`, '提示', { type: 'warning' }).then(
    async () => {
      await deleteFacility(row.id)
      ElMessage.success('删除成功')
      await loadData()
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
.facility-page {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.tree-card {
  flex: 0 0 280px;
  width: 280px;
}
.tree-body {
  max-height: calc(100vh - 190px);
  overflow: auto;
}
.right-area {
  flex: 1;
  min-width: 0;
}
.query-card {
  margin-bottom: 0;
}
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
}
.empty-tip {
  color: #909399;
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
}
.attachment-item {
  padding: 8px 12px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
}
.section-title {
  margin: 16px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
</style>
