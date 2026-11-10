<template>
  <div class="facility-page">
    <!-- 左侧：区划树（乡镇/村/组） -->
    <el-card shadow="never" class="tree-card">
      <div class="tree-search">
        <el-input
          v-model="treeKeyword"
          placeholder="搜索区划名称"
          prefix-icon="Search"
          clearable
        />
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
              <span v-if="countMap[data.code]" class="tree-node-count">
                {{ countMap[data.code] }}项
              </span>
            </span>
          </template>
        </el-tree>
        <el-empty v-if="!treeData.length" description="暂无区划数据" :image-size="80" />
      </div>
      <div class="tree-footer">
        <span>共 {{ totalFacilityCount }} 项设施 · {{ groupCount }} 个村民组</span>
        <el-button link type="primary" class="tree-all-btn" @click="handleTreeAll">全部</el-button>
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
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">查看</el-button>
              <el-button
                v-if="store.hasPerm('base:facility:edit')"
                link
                type="primary"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="store.hasPerm('base:facility:delete')"
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
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" :title="editDialogMode === 'edit' ? '编辑公共基础设施' : '新增公共基础设施'" width="800px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-row :gutter="16">
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
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设施名称" prop="facilityName">
              <el-input v-model="form.facilityName" placeholder="请输入设施名称" maxlength="128" />
            </el-form-item>
          </el-col>
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
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="具体位置" prop="location">
              <el-input v-model="form.location" placeholder="请输入具体位置" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
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
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
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
          <el-col :span="12">
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
          <!-- 附件卡片头部：序号 + 标题 + 删除 -->
          <div class="attachment-header">
            <span class="attachment-index">{{ idx + 1 }}</span>
            <span class="attachment-title">附件 #{{ idx + 1 }}</span>
            <el-button
              type="danger"
              link
              icon="Delete"
              class="attachment-remove"
              @click="removeAttachment(idx)"
            >
              删除
            </el-button>
          </div>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="附件类型" label-position="top" :prop="`attachments.${idx}.attachmentType`">
                <el-select v-model="att.attachmentType" placeholder="请选择类型..." style="width: 100%">
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
              <el-form-item label="上传附件" label-position="top">
                <el-upload
                  class="upload-full"
                  style="display: block; width: 100%"
                  :auto-upload="false"
                  :show-file-list="false"
                  :disabled="attachmentUploading"
                  multiple
                  @change="(file: UploadFile) => handleAttachmentFileChange(file, att)"
                >
                  <div class="upload-trigger">
                    <el-icon><Upload /></el-icon>
                    <span>点击选择文件...</span>
                  </div>
                </el-upload>
                <!-- 已选文件列表：支持逐个删除 -->
                <div v-if="att.files.length" class="upload-file-list">
                  <div
                    v-for="(file, fIdx) in att.files"
                    :key="`${file.fileName}_${fIdx}`"
                    class="upload-file-item"
                  >
                    <el-icon class="upload-file-icon"><Document /></el-icon>
                    <span class="upload-file-name" :title="file.fileName">{{ file.fileName }}</span>
                    <el-icon
                      v-if="file.filePath"
                      class="upload-file-download"
                      title="下载"
                      @click="handleDownloadAttachment(file)"
                    >
                      <Download />
                    </el-icon>
                    <el-icon class="upload-file-remove" @click="removeAttachmentFile(att, fIdx)">
                      <Close />
                    </el-icon>
                  </div>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="备注" label-position="top" :prop="`attachments.${idx}.remark`">
                <el-input v-model="att.remark" placeholder="选填" />
              </el-form-item>
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
            <el-descriptions-item label="具体位置">{{ detail.location || '—' }}</el-descriptions-item>
            <el-descriptions-item label="建成年份">{{ detail.buildYear || '—' }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ detail.quantity ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="单位">{{ detail.unit || '—' }}</el-descriptions-item>
          </el-descriptions>
          <h4 class="section-title">附件列表</h4>
          <el-table :data="groupedDetailAttachments" border size="small">
            <el-table-column label="附件类型" width="120" align="center">
              <template #default="{ row }">{{ row.typeLabel || '—' }}</template>
            </el-table-column>
            <el-table-column label="文件名" min-width="260">
              <template #default="{ row }">
                <div
                  v-for="file in row.files"
                  :key="file.filePath || file.fileName"
                  class="attachment-file-line"
                >
                  <el-link
                    v-if="file.fileName && file.filePath"
                    type="primary"
                    :underline="false"
                    @click="handlePreviewAttachment(file)"
                  >
                    {{ file.fileName }}
                  </el-link>
                  <span v-else>{{ file.fileName || '—' }}</span>
                  <el-icon
                    v-if="file.filePath"
                    class="attachment-file-download"
                    title="下载"
                    @click="handleDownloadAttachment(file)"
                  >
                    <Download />
                  </el-icon>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="150">
              <template #default="{ row }">{{ row.remark || '—' }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!groupedDetailAttachments.length" description="暂无附件" :image-size="60" />
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

    <!-- 附件图片预览 -->
    <el-dialog
      v-model="previewVisible"
      :title="previewName || '附件预览'"
      width="820px"
      destroy-on-close
      @closed="handlePreviewClosed"
    >
      <div class="preview-body">
        <img v-if="previewUrl" :src="previewUrl" class="preview-image" alt="附件预览" />
      </div>
      <!-- 预览弹窗内提供下载入口，避免只能看图无法保存 -->
      <template #footer>
        <el-button v-if="previewFile" type="primary" @click="handleDownloadAttachment(previewFile)">下载</el-button>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
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
  deleteFacility,
  getFacilityRegionCounts
} from '@/api/facility'
import { uploadFile as uploadFileApi, getPresignUrl } from '@/api/file'
import type { FacilityQuery } from '@/api/facility'
import type {
  DictVO,
  VillageTreeVO,
  FacilityVO,
  FacilityDetailVO,
  FacilitySaveDTO,
  FacilityAttachmentVO
} from '@/types/system'

const store = useUserStore()

type EditDialogMode = 'add' | 'edit'

/** 附件卡片中的单个文件 */
interface AttachmentFileItem {
  fileName: string
  fileSize?: number
  filePath?: string
  fileHash?: string
}

/** 附件卡片编辑模型：一张卡片可上传多个文件 */
interface AttachmentEditItem {
  attachmentType: string
  remark: string
  files: AttachmentFileItem[]
}

/** 设施表单模型（提交时再展开为后端入参） */
interface FacilityFormModel {
  id?: string
  facilityCode: string
  facilityName: string
  facilityType: string
  regionCode: string
  regionName: string
  location: string
  buildYear?: number
  quantity?: number
  unit: string
  remark: string
  attachments: AttachmentEditItem[]
}

/** 区划树 */
const treeRef = ref<{ setCurrentKey: (key?: string) => void; filter: (value: string) => void }>()
const treeProps = { label: 'name', children: 'children' }
const treeData = ref<VillageTreeVO[]>([])
const selectedTreeNode = ref<VillageTreeVO | null>(null)

/** 区划树搜索关键字 */
const treeKeyword = ref('')
const filterTree = (value: string, data: VillageTreeVO) => {
  if (!value) return true
  return data.name.includes(value)
}
watch(treeKeyword, (val) => treeRef.value?.filter(val))

/** 各区划设施数量（含所有下级区划汇总） */
const regionCounts = ref<Record<string, number>>({})
const countMap = computed(() => {
  const result: Record<string, number> = {}
  const walk = (nodes: VillageTreeVO[]): number => {
    let sum = 0
    for (const node of nodes) {
      const childSum = node.children?.length ? walk(node.children) : 0
      const own = regionCounts.value[node.code] || 0
      result[node.code] = own + childSum
      sum += own + childSum
    }
    return sum
  }
  walk(treeData.value)
  return result
})

/** 设施总数（所有区划合计） */
const totalFacilityCount = computed(() =>
  Object.values(regionCounts.value).reduce((sum, item) => sum + item, 0)
)

/** 村民组数量（区划类型字典：3=组） */
const groupCount = computed(() => {
  let count = 0
  const walk = (nodes: VillageTreeVO[]) => {
    for (const node of nodes) {
      if (node.regionType === '3') count++
      if (node.children?.length) walk(node.children)
    }
  }
  walk(treeData.value)
  return count
})

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
const facilityTypeDictItems = ref<DictVO[]>([])
const attachmentTypeDictItems = ref<DictVO[]>([])

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

const emptyAttachment = (): AttachmentEditItem => ({
  attachmentType: '',
  remark: '',
  files: []
})

const form = reactive<FacilityFormModel>({
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
    attachments: [] as AttachmentEditItem[]
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

/** 附件预览弹窗（图片） */
const previewVisible = ref(false)
const previewUrl = ref('')
const previewName = ref('')
/** 当前预览中的文件，供弹窗内“下载”按钮复用 */
const previewFile = ref<FacilityAttachmentVO | null>(null)

/** 图片类后缀 */
const IMAGE_EXTENSIONS = ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp']

/** 点击附件文件名：图片弹窗预览、PDF 新窗口打开、其它类型触发下载 */
const handlePreviewAttachment = async (row: FacilityAttachmentVO) => {
  if (!row.filePath) {
    ElMessage.warning('该附件未上传文件，请编辑后重新上传')
    return
  }
  const ext = (row.fileName || '').split('.').pop()?.toLowerCase() || ''
  const isImage = IMAGE_EXTENSIONS.includes(ext)
  // 附件访问统一走时效签名URL，禁止永久直链；非图片/PDF 强制下载
  const res = await getPresignUrl({
    bucket: 'archive-formal',
    fileKey: row.filePath,
    fileName: row.fileName,
    download: !isImage && ext !== 'pdf'
  })
  const url = res.data
  if (isImage) {
    previewUrl.value = url
    previewName.value = row.fileName || '附件预览'
    previewFile.value = row
    previewVisible.value = true
    return
  }
  window.open(url, '_blank')
}

/**
 * 下载附件：通过时效签名URL携带 attachment 响应头强制下载
 * 说明：签名URL有效期 600 秒，每次下载重新签发，禁止永久直链
 */
const handleDownloadAttachment = async (file: AttachmentFileItem) => {
  if (!file.filePath) {
    ElMessage.warning('该附件未上传文件，请编辑后重新上传')
    return
  }
  const res = await getPresignUrl({
    bucket: 'archive-formal',
    fileKey: file.filePath,
    fileName: file.fileName,
    download: true
  })
  // 跨域签名URL的 download 属性不生效，实际文件名由服务端 Content-Disposition 决定
  const link = document.createElement('a')
  link.href = res.data
  link.target = '_blank'
  link.rel = 'noopener'
  link.download = file.fileName || ''
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

/** 关闭预览弹窗（签名URL无需释放） */
const handlePreviewClosed = () => {
  previewUrl.value = ''
  previewName.value = ''
  previewFile.value = null
}

/** 查看弹窗附件：按「附件类型 + 备注」归并，与编辑弹窗保持一致（一个附件含多个文件） */
interface AttachmentGroup {
  attachmentType: string
  typeLabel: string
  remark: string
  files: FacilityAttachmentVO[]
}

const groupedDetailAttachments = computed<AttachmentGroup[]>(() => {
  const groups = new Map<string, AttachmentGroup>()
  for (const attachment of detail.value?.attachments || []) {
    const key = `${attachment.attachmentType || ''}||${attachment.remark || ''}`
    let group = groups.get(key)
    if (!group) {
      group = {
        attachmentType: attachment.attachmentType || '',
        typeLabel: attachmentTypeLabel(attachment.attachmentType),
        remark: attachment.remark || '',
        files: []
      }
      groups.set(key, group)
    }
    group.files.push(attachment)
  }
  return [...groups.values()]
})

/** 字典标签 */
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
  const [res1, res2] = await Promise.all([
    getDictByType('she_shi_lei_xing'),
    getDictByType('fu_jian_lei_xing')
  ])
  facilityTypeDictItems.value = res1.data || []
  attachmentTypeDictItems.value = res2.data || []
}

/** 加载区划树 */
const loadTree = async () => {
  const res = await getVillageTree()
  treeData.value = res.data || []
}

/** 加载各区划设施数量（左侧区划树徽标） */
const loadRegionCounts = async () => {
  const res = await getFacilityRegionCounts()
  const map: Record<string, number> = {}
  for (const item of res.data || []) {
    map[item.regionCode] = item.count
  }
  regionCounts.value = map
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

/** 附件上传中状态（上传完成前禁止重复选择） */
const attachmentUploading = ref(false)

/** 选择附件文件：立即上传，成功后记录文件元信息（支持多选，按文件名+大小去重） */
const handleAttachmentFileChange = async (uploadFile: UploadFile, att: AttachmentEditItem) => {
  const raw = uploadFile.raw
  if (!raw) return
  const duplicated = att.files.some((f) => f.fileName === raw.name && f.fileSize === raw.size)
  if (duplicated) {
    ElMessage.warning('该文件已添加')
    return
  }
  attachmentUploading.value = true
  try {
    const res = await uploadFileApi(raw)
    const data = res.data
    att.files.push({
      fileName: data.fileName,
      fileSize: data.fileSize,
      filePath: data.fileKey,
      fileHash: data.fileHash
    })
  } finally {
    attachmentUploading.value = false
  }
}

/** 删除附件卡片中的某个文件 */
const removeAttachmentFile = (att: AttachmentEditItem, idx: number) => {
  att.files.splice(idx, 1)
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
  // 附件按「附件类型 + 备注」归并为同一张卡片（一个附件可含多个文件）
  const attachmentCards = new Map<string, AttachmentEditItem>()
  for (const attachment of data.attachments || []) {
    const key = `${attachment.attachmentType || ''}||${attachment.remark || ''}`
    let card = attachmentCards.get(key)
    if (!card) {
      card = {
        attachmentType: attachment.attachmentType || '',
        remark: attachment.remark || '',
        files: []
      }
      attachmentCards.set(key, card)
    }
    if (attachment.fileName) {
      card.files.push({
        fileName: attachment.fileName,
        fileSize: attachment.fileSize,
        filePath: attachment.filePath
      })
    }
  }
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
    attachments: [...attachmentCards.values()]
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

/** 组装提交参数：把附件卡片的多文件展开为后端附件记录（同一卡片共用附件类型与备注） */
const buildSubmitPayload = (): FacilitySaveDTO => ({
  id: form.id,
  facilityCode: form.facilityCode,
  facilityName: form.facilityName,
  facilityType: form.facilityType,
  regionCode: form.regionCode,
  regionName: form.regionName,
  location: form.location,
  buildYear: form.buildYear,
  quantity: form.quantity,
  unit: form.unit,
  remark: form.remark,
  attachments: form.attachments.flatMap((card) =>
    card.files.map((file) => ({
      attachmentType: card.attachmentType,
      fileName: file.fileName,
      filePath: file.filePath,
      fileHash: file.fileHash,
      fileSize: file.fileSize,
      remark: card.remark
    }))
  )
})

/** 提交 */
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload = buildSubmitPayload()
    if (editDialogMode.value === 'edit') {
      await updateFacility(payload)
    } else {
      await createFacility(payload)
    }
    ElMessage.success('操作成功')
    editDialogVisible.value = false
    await Promise.all([loadData(), loadRegionCounts()])
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
      await Promise.all([loadData(), loadRegionCounts()])
    }
  )
}

onMounted(() => {
  loadDict()
  loadTree()
  loadRegionCounts()
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
.query-card :deep(.el-form--inline) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}
/* 筛选控件固定宽度，保证设施类型与查询/重置按钮同处一行且不换行 */
.query-card :deep(.el-form-item) {
  flex: 0 0 auto;
  margin-right: 16px;
  margin-bottom: 0;
}
.query-card :deep(.el-form-item:not(:last-child)) {
  width: 210px;
}
.query-card :deep(.el-form-item__content) {
  width: 100%;
  min-width: 0;
}
.query-card :deep(.el-input),
.query-card :deep(.el-select) {
  width: 100%;
}
/* 查询/重置靠右，与下方列表区右侧对齐 */
.query-card :deep(.el-form-item:last-child) {
  margin-right: 0;
  margin-left: auto;
}
.list-card {
  margin-top: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
/* 区划树节点行样式（复刻原型：图标 + 名称 + 数量徽标） */
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
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  overflow: hidden;
}
.tree-node-icon {
  flex: 0 0 auto;
  font-size: 15px;
  color: #e6a23c;
}
.tree-node-icon.is-town {
  color: #3b82f6;
}
.tree-node-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #303133;
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
.empty-tip {
  color: #909399;
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
}
.attachment-item {
  padding: 12px 16px 2px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 12px;
}
.attachment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.attachment-index {
  width: 20px;
  height: 20px;
  line-height: 20px;
  border-radius: 50%;
  background: #e8f0fe;
  color: #3b82f6;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
}
.attachment-title {
  font-size: 14px;
  color: #303133;
}
.attachment-remove {
  margin-left: auto;
}
.attachment-item :deep(.el-form-item) {
  margin-bottom: 16px;
}
.attachment-item :deep(.el-form-item__label) {
  padding: 0;
  color: #606266;
  line-height: 24px;
  margin-bottom: 6px;
}
.attachment-item :deep(.el-upload) {
  width: 100%;
}
/* 上传附件触发区：虚线框 + 上传图标 */
/* 上传组件占满所在列宽，避免右侧留白 */
.upload-full {
  display: block;
  width: 100%;
}
.upload-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 32px;
  box-sizing: border-box;
  width: 100%;
  padding: 0 12px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #606266;
  font-size: 13px;
  background: #fff;
  overflow: hidden;
  transition: border-color 0.2s, color 0.2s;
}
.upload-trigger:hover {
  border-color: #409eff;
  color: #409eff;
}
.upload-trigger span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* 已选文件列表 */
.upload-file-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.upload-file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 26px;
  padding: 0 8px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
  font-size: 12px;
  color: #606266;
}
.upload-file-icon {
  flex: 0 0 auto;
  color: #909399;
}
.upload-file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-file-remove {
  flex: 0 0 auto;
  color: #909399;
  cursor: pointer;
}
.upload-file-remove:hover {
  color: #f56c6c;
}
/* 附件下载图标：统一使用小箭头样式（蓝色可点击） */
.upload-file-download {
  flex: 0 0 auto;
  color: #409eff;
  cursor: pointer;
}
.upload-file-download:hover {
  color: #337ecc;
}
.section-title {
  margin: 16px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
/* 附件预览：图片等比缩放并居中 */
.preview-body {
  display: flex;
  align-items: center;
  justify-content: center;
  max-height: 70vh;
  overflow: auto;
}
.preview-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}
/* 查看弹窗附件：同一附件下的多个文件逐行展示 */
.attachment-file-line {
  line-height: 24px;
}
/* 查看弹窗附件下载图标：与户基础信息页保持一致的小箭头 */
.attachment-file-download {
  margin-left: 6px;
  color: #409eff;
  cursor: pointer;
  vertical-align: middle;
}
.attachment-file-download:hover {
  color: #337ecc;
}
</style>
