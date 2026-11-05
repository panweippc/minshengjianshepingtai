<template>
  <div class="household-page">
    <!-- 左侧：区划树（乡镇/村/组） -->
    <el-card shadow="never" class="tree-card">
      <template #header>
        <div class="card-header">
          <span>村委信息</span>
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
      <!-- 筛选区 -->
      <el-card shadow="never" class="query-card">
        <el-form :inline="true" :model="query" @submit.prevent>
          <el-form-item label="姓名">
            <el-input v-model="query.headName" placeholder="请输入户主姓名" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="身份证号">
            <el-input v-model="query.headIdCard" placeholder="请输入户主身份证号" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetQuery">刷新</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 列表区 -->
      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="card-header">
            <span>农户列表</span>
            <el-button
              v-if="store.hasPerm('base:household:add')"
              type="primary"
              icon="Plus"
              @click="openAdd()"
            >
              新增
            </el-button>
          </div>
        </template>
        <el-table v-loading="loading" :data="tableData" border stripe>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="regionName" label="行政村" min-width="130" />
          <el-table-column prop="householdNo" label="户号" min-width="170" />
          <el-table-column prop="headName" label="户主" width="100" />
          <el-table-column prop="headIdCard" label="身份证号" min-width="180" />
          <el-table-column prop="contactPhone" label="联系电话" width="130" />
          <el-table-column prop="familyPopulation" label="家庭人口" width="90" align="center" />
          <el-table-column label="房屋面积" width="110" align="center">
            <template #default="{ row }">{{ row.houseArea != null ? row.houseArea + ' ㎡' : '—' }}</template>
          </el-table-column>
          <el-table-column label="房屋类型" width="120" align="center">
            <template #default="{ row }">{{ dictLabel('fang_wu_jie_gou_lei_xing', row.houseType) }}</template>
          </el-table-column>
          <el-table-column label="户类型" width="110" align="center">
            <template #default="{ row }">{{ dictLabel('hu_lei_xing', row.householdType) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" icon="View" @click="openDetail(row)">查看</el-button>
              <el-button
                v-if="store.hasPerm('base:household:edit')"
                link
                type="primary"
                icon="Edit"
                @click="openEdit(row)"
              >编辑</el-button>
              <el-button
                v-if="store.hasPerm('base:household:delete')"
                link
                type="danger"
                icon="Delete"
                @click="handleDelete(row)"
              >删除</el-button>
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

    <!-- 新增/编辑/查看弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="820px" destroy-on-close>
      <!-- 查看模式 -->
      <div v-if="dialogMode === 'detail'" class="detail-mode">
        <el-tabs v-model="detailTab">
          <el-tab-pane label="档案信息" name="archive">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="行政村">{{ selectedRegionName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="户号">{{ form.householdNo || '—' }}</el-descriptions-item>
              <el-descriptions-item label="户主">{{ form.headName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="身份证号">{{ form.headIdCard || '—' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ form.contactPhone || '—' }}</el-descriptions-item>
              <el-descriptions-item label="家庭人口">{{ form.familyPopulation ?? '—' }}</el-descriptions-item>
              <el-descriptions-item label="房屋面积">{{ form.houseArea != null ? form.houseArea + ' ㎡' : '—' }}</el-descriptions-item>
              <el-descriptions-item label="房屋类型">{{ dictLabel('fang_wu_jie_gou_lei_xing', form.houseType) }}</el-descriptions-item>
              <el-descriptions-item label="户类型">{{ dictLabel('hu_lei_xing', form.householdType) }}</el-descriptions-item>
              <el-descriptions-item label="产权编号">{{ form.propertyNo || '—' }}</el-descriptions-item>
              <el-descriptions-item label="建档时间">{{ form.archiveTime || '—' }}</el-descriptions-item>
            </el-descriptions>
            <div class="attachment-block">
              <div class="attachment-title">附件列表</div>
              <el-table :data="attachmentRows" border size="small" empty-text="暂无附件">
                <el-table-column label="附件类型" width="140">
                  <template #default="{ row }">{{ dictLabel('fu_jian_lei_xing', row.attachmentType) }}</template>
                </el-table-column>
                <el-table-column prop="fileName" label="文件名" min-width="200" />
                <el-table-column prop="remark" label="备注" min-width="150" />
              </el-table>
            </div>
          </el-tab-pane>
          <el-tab-pane label="项目信息" name="project">
            <!-- 项目模块暂未接入，页签先占位，暂无数据 -->
            <el-table :data="projectRows" border size="small" empty-text="暂无项目数据">
              <el-table-column prop="projectCode" label="项目编码" width="120" />
              <el-table-column prop="projectName" label="项目名称" min-width="140" show-overflow-tooltip />
              <el-table-column prop="projectType" label="类型" width="90" />
              <el-table-column prop="progress" label="进度" width="80" />
              <el-table-column prop="budget" label="预算" width="100" />
              <el-table-column prop="status" label="状态" width="90" />
              <el-table-column prop="approvalYear" label="立项年度" width="90" />
              <el-table-column label="操作" width="80" fixed="right">
                <template #default>
                  <el-button link type="primary" icon="View">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 编辑/新增模式 -->
      <div v-else>
        <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="行政村" prop="regionCode">
                <el-select
                  v-model="form.regionCode"
                  placeholder="请选择行政村"
                  filterable
                  style="width: 100%"
                  @change="onRegionChange"
                >
                  <el-option
                    v-for="item in villageOptions"
                    :key="item.code"
                    :label="`${item.name}（${item.code}）`"
                    :value="item.code"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="户号" prop="householdNo">
                <el-input
                  v-model="form.householdNo"
                  :disabled="dialogMode === 'edit'"
                  placeholder="请输入户号"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="户主" prop="headName">
                <el-input v-model="form.headName" placeholder="请输入户主姓名" maxlength="32" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="身份证号" prop="headIdCard">
                <el-input v-model="form.headIdCard" placeholder="请输入户主身份证号" maxlength="20" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="联系电话" prop="contactPhone">
                <el-input v-model="form.contactPhone" placeholder="请输入联系电话" maxlength="20" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="家庭人口" prop="familyPopulation">
                <el-input-number
                  v-model="form.familyPopulation"
                  :min="1"
                  :max="999"
                  style="width: 100%"
                  placeholder="家庭人口"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="房屋面积(㎡)" prop="houseArea">
                <el-input-number
                  v-model="form.houseArea"
                  :min="0"
                  :precision="2"
                  :step="10"
                  style="width: 100%"
                  placeholder="房屋面积"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="房屋类型" prop="houseType">
                <el-select v-model="form.houseType" placeholder="请选择" style="width: 100%">
                  <el-option
                    v-for="item in houseTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="户类型" prop="householdType">
                <el-select v-model="form.householdType" placeholder="请选择" style="width: 100%">
                  <el-option
                    v-for="item in householdTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="产权编号" prop="propertyNo">
                <el-input v-model="form.propertyNo" placeholder="请输入产权编号" maxlength="64" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="建档时间" prop="archiveTime">
                <el-date-picker
                  v-model="form.archiveTime"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择建档时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 附件列表：卡片式多条录入 -->
          <div class="attachment-section">
            <div class="attachment-header">
              <span class="attachment-divider-title">附件列表</span>
              <el-button link type="primary" icon="Plus" @click="addAttachment">添加附件</el-button>
            </div>

            <div v-for="(row, idx) in form.attachments" :key="idx" class="attachment-card">
              <div class="attachment-card-header">
                <span class="attachment-index">{{ idx + 1 }}</span>
                <span class="attachment-card-title">附件 #{{ idx + 1 }}</span>
                <el-button link type="danger" icon="Delete" @click="removeAttachment(idx)">删除</el-button>
              </div>
              <div class="attachment-card-body">
                <div class="attachment-field">
                  <label class="attachment-field-label">附件类型</label>
                  <el-select
                    v-model="row.attachmentType"
                    placeholder="请选择类型..."
                    style="width: 100%"
                  >
                    <el-option
                      v-for="item in attachmentTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </div>
                <div class="attachment-field">
                  <label class="attachment-field-label">上传附件</label>
                  <el-button
                    class="attachment-upload"
                    icon="UploadFilled"
                    @click="handleUpload(idx)"
                  >
                    <span class="attachment-upload-text">
                      {{ row.fileName || '点击选择文件...' }}
                    </span>
                  </el-button>
                </div>
                <div class="attachment-field">
                  <label class="attachment-field-label">备注</label>
                  <el-input v-model="row.remark" placeholder="选填" maxlength="256" />
                </div>
              </div>
            </div>
          </div>
        </el-form>
      </div>

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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getDictByType } from '@/api/dict'
import type { DictVO, VillageTreeVO } from '@/types/system'
import { getVillageTree } from '@/api/village'
import {
  getHouseholdPage,
  getHouseholdDetail,
  getHouseholdAttachments,
  createHousehold,
  updateHousehold,
  deleteHousehold,
  type HouseholdQuery,
  type HouseholdVO,
  type HouseholdAttachmentVO,
  type HouseholdSaveDTO
} from '@/api/household'

const store = useUserStore()

type DialogMode = 'add' | 'edit' | 'detail'

/* ============ 区划树 ============ */
const treeRef = ref<{ setCurrentKey: (key?: string) => void }>()
const treeProps = { label: 'name', children: 'children' }
const treeData = ref<VillageTreeVO[]>([])

/** 树节点点击时，若选择到村/组（regionType 2/3 或 level>=5），直接用该编码筛农户；
 * 若选择到乡镇（regionType 1），则其下所有村都在筛选范围内 */
const handleTreeClick = (data: VillageTreeVO) => {
  query.nodeCode = data.code
  handleQuery()
}
const handleTreeAll = () => {
  query.nodeCode = undefined
  treeRef.value?.setCurrentKey()
  handleQuery()
}

/* ============ 字典加载 ============ */
const dictMap = ref<Record<string, DictVO[]>>({})

/** 加载指定类型字典 */
const loadDict = async (type: string) => {
  if (dictMap.value[type]) return
  try {
    const res = await getDictByType(type)
    dictMap.value[type] = res.data || []
  } catch {
    dictMap.value[type] = []
  }
}

/** 字典选项列表 */
const dictOptions = (type: string) => (dictMap.value[type] || []).map((d) => ({
  label: d.dictLabel,
  value: d.dictValue
}))

const houseTypeOptions = computed(() => dictOptions('fang_wu_jie_gou_lei_xing'))
const householdTypeOptions = computed(() => dictOptions('hu_lei_xing'))
const attachmentTypeOptions = computed(() => dictOptions('fu_jian_lei_xing'))

/** 字典翻译：value → label */
const dictLabel = (type: string, value?: string) => {
  if (!value) return '—'
  const hit = (dictMap.value[type] || []).find((d) => d.dictValue === value)
  return hit ? hit.dictLabel : value
}

const regionTypeLabel = (value?: string) => dictLabel('qu_hua_lei_xing', value)

/* ============ 查询与列表 ============ */
const query = reactive<HouseholdQuery>({ pageNum: 1, pageSize: 10 })
const total = ref(0)
const tableData = ref<HouseholdVO[]>([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getHouseholdPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const handleQuery = () => { query.pageNum = 1; loadData() }
const resetQuery = () => {
  query.headName = undefined
  query.headIdCard = undefined
  query.nodeCode = undefined
  treeRef.value?.setCurrentKey()
  handleQuery()
}

/* ============ 弹窗 & 表单 ============ */
const dialogVisible = ref(false)
const dialogMode = ref<DialogMode>('add')
/** 详情页签：archive=档案信息，project=项目信息 */
const detailTab = ref<'archive' | 'project'>('archive')

/** 项目信息行（项目模块暂未接入，页签暂以空表格占位） */
interface ProjectInfoRow {
  projectCode?: string
  projectName?: string
  projectType?: string
  progress?: string
  budget?: number
  status?: string
  approvalYear?: string
}
const projectRows = ref<ProjectInfoRow[]>([])
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<HouseholdSaveDTO>({ regionCode: '' })
const attachmentRows = ref<HouseholdAttachmentVO[]>([])

const dialogTitle = computed(() => {
  if (dialogMode.value === 'detail') return '户基础信息详情'
  return dialogMode.value === 'edit' ? '编辑户基础信息' : '新增户基础信息'
})

/** 行政村下拉选项：从区划树中过滤出 行政村（regionType=2，level=5） */
const villageOptions = computed<VillageTreeVO[]>(() => {
  const result: VillageTreeVO[] = []
  const walk = (nodes: VillageTreeVO[]) => {
    for (const node of nodes) {
      if (node.regionType === '2') result.push(node)
      if (node.children?.length) walk(node.children)
    }
  }
  walk(treeData.value)
  return result
})

const selectedRegionName = computed(() => {
  if (!form.regionCode) return ''
  const hit = villageOptions.value.find((v) => v.code === form.regionCode)
  return hit?.name || ''
})

const onRegionChange = () => {
  // 行政村变更时可在此做联动（如自动填充户号预览）
}

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
  householdNo: [{ required: true, message: '请输入户号', trigger: 'blur' }],
  regionCode: [{ required: true, message: '请选择行政村', trigger: 'change' }],
  headName: [{ required: true, message: '请输入户主姓名', trigger: 'blur' }],
  headIdCard: [
    {
      pattern: /^$|^[0-9Xx]{15,20}$/,
      message: '身份证号格式不正确',
      trigger: 'blur'
    }
  ],
  contactPhone: [{ validator: validatePhone, trigger: 'blur' }]
}

/* ============ 附件增删 ============ */
const addAttachment = () => {
  form.attachments = form.attachments || []
  form.attachments.push({ attachmentType: '', fileName: '', remark: '' })
}
const removeAttachment = (idx: number) => {
  form.attachments?.splice(idx, 1)
}

/**
 * 上传附件：目前项目尚未实现真实的 MinIO 分片上传接口，此方法预留钩子。
 * 后续接入 /api/file/upload 时，将点击后弹出文件选择框、上传成功后写回 fileName/fileKey/bucket/fileSize。
 * 此处先用临时文件名占位，方便表单整体提交流转。
 */
const handleUpload = (idx: number) => {
  // 创建隐藏 input 触发文件选择（与后端文件上传接口解耦）
  const input = document.createElement('input')
  input.type = 'file'
  input.addEventListener('change', () => {
    const file = input.files?.[0]
    if (!file) return
    form.attachments = form.attachments || []
    const row = form.attachments[idx]
    row.fileName = file.name
    row.fileSize = file.size
    // 真实 fileKey/bucket 等上传接口接入后赋值；当前留空不阻塞表单提交
    ElMessage.success(`已选择文件：${file.name}（${(file.size / 1024).toFixed(1)} KB）`)
  })
  input.click()
}

/* ============ 弹窗操作 ============ */
const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    householdNo: '',
    regionCode: '',
    headName: '',
    headIdCard: '',
    contactPhone: '',
    familyPopulation: undefined,
    houseArea: undefined,
    houseType: '',
    householdType: '',
    propertyNo: '',
    archiveTime: '',
    remark: '',
    attachments: []
  })
  attachmentRows.value = []
}

const openAdd = () => {
  dialogMode.value = 'add'
  resetForm()
  dialogVisible.value = true
}

const openEdit = async (row: HouseholdVO) => {
  dialogMode.value = 'edit'
  const detailRes = await getHouseholdDetail(row.id)
  const attRes = await getHouseholdAttachments(row.id)
  const d = detailRes.data
  Object.assign(form, {
    id: d.id,
    householdNo: d.householdNo,
    regionCode: d.regionCode,
    headName: d.headName,
    headIdCard: d.headIdCard ?? '',
    contactPhone: d.contactPhone ?? '',
    familyPopulation: d.familyPopulation,
    houseArea: d.houseArea,
    houseType: d.houseType ?? '',
    householdType: d.householdType ?? '',
    propertyNo: d.propertyNo ?? '',
    archiveTime: d.archiveTime ?? '',
    remark: d.remark ?? '',
    attachments: (attRes.data || []).map((a) => ({
      id: a.id,
      attachmentType: a.attachmentType ?? '',
      fileName: a.fileName ?? '',
      fileKey: a.fileKey,
      bucket: a.bucket,
      fileSize: a.fileSize,
      remark: a.remark ?? ''
    }))
  })
  dialogVisible.value = true
}

const openDetail = async (row: HouseholdVO) => {
  dialogMode.value = 'detail'
  detailTab.value = 'archive'
  const detailRes = await getHouseholdDetail(row.id)
  const attRes = await getHouseholdAttachments(row.id)
  const d = detailRes.data
  Object.assign(form, {
    id: d.id,
    householdNo: d.householdNo,
    regionCode: d.regionCode,
    headName: d.headName,
    headIdCard: d.headIdCard,
    contactPhone: d.contactPhone,
    familyPopulation: d.familyPopulation,
    houseArea: d.houseArea,
    houseType: d.houseType,
    householdType: d.householdType,
    propertyNo: d.propertyNo,
    archiveTime: d.archiveTime,
    remark: d.remark
  })
  attachmentRows.value = attRes.data || []
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (dialogMode.value === 'edit') {
      await updateHousehold({ ...form })
    } else {
      await createHousehold({ ...form })
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row: HouseholdVO) => {
  ElMessageBox.confirm(`确认删除户基础信息「${row.headName}」（${row.householdNo}）？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteHousehold(row.id)
    ElMessage.success('删除成功')
    await loadData()
  })
}

/* ============ 初始化 ============ */
const loadTree = async () => {
  const res = await getVillageTree()
  treeData.value = res.data || []
}

onMounted(async () => {
  await Promise.all([
    loadDict('fang_wu_jie_gou_lei_xing'),
    loadDict('hu_lei_xing'),
    loadDict('fu_jian_lei_xing'),
    loadDict('qu_hua_lei_xing'),
    loadTree()
  ])
  loadData()
})
</script>

<style scoped>
.household-page {
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

/* 筛选项和按钮同一水平线 */
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

/* 弹窗：附件列表（卡片式） */
.attachment-section {
  width: 100%;
  margin: 4px 0 18px;
}
.attachment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.attachment-divider-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}
.attachment-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px 16px 16px;
  margin-bottom: 12px;
  background: #fff;
}
.attachment-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.attachment-index {
  width: 20px;
  height: 20px;
  flex: 0 0 20px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.attachment-card-title {
  flex: 1;
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}
.attachment-card-body {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 12px;
}
.attachment-field-label {
  display: block;
  color: #606266;
  font-size: 13px;
  margin-bottom: 6px;
}
.attachment-upload {
  width: 100%;
  border-style: dashed;
  color: #909399;
}
.attachment-upload-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 详情模式：附件块 */
.attachment-block {
  margin-top: 16px;
}
.attachment-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.detail-mode :deep(.el-descriptions__body .el-descriptions__table) {
  margin-bottom: 4px;
}
</style>
