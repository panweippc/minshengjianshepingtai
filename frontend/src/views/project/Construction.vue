<template>
  <div class="construction-page">
    <!-- 筛选区 -->
    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="项目编码">
          <el-input
            v-model="queryForm.projectCode"
            placeholder="请输入项目编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input
            v-model="queryForm.projectName"
            placeholder="请输入项目名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="项目类别">
          <el-select v-model="queryForm.projectCategory" placeholder="全部" clearable>
            <el-option
              v-for="item in dictOptions('xiang_mu_lei_bie')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="queryForm.projectType" placeholder="全部" clearable>
            <el-option
              v-for="item in dictOptions('xiang_mu_lei_xing')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="query-item--actions">
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 三级施工进度表格 -->
    <el-card shadow="never" class="list-card">
      <el-table
        v-loading="loading"
        :data="visibleRows"
        row-key="nodeKey"
        border
        stripe
        class="construction-table"
        :row-class-name="rowClassName"
      >
        <el-table-column label="项目编号" width="200">
          <template #default="{ row }">
            <span
              v-if="row.hasChildren"
              class="tree-toggle"
              :class="{ 'tree-toggle--declare': row.nodeType === 'declare' }"
              :title="collapsedKeys.has(row.nodeKey) ? '展开' : '收起'"
              @click.stop="toggleRow(row.nodeKey)"
            >
              <el-icon :class="{ 'is-expanded': !collapsedKeys.has(row.nodeKey) }">
                <ArrowRight />
              </el-icon>
            </span>
            <span
              v-else
              class="tree-toggle"
              :class="{ 'tree-toggle--detail': row.nodeType === 'detail' }"
            />
            <!-- 三级明细按类型区分图标：农户为头像图标，公共基础设施为设施图标 -->
            <el-icon
              v-if="row.nodeType === 'detail'"
              class="detail-type-icon"
              :class="row.detailType === 'FACILITY' ? 'detail-type-icon--facility' : 'detail-type-icon--household'"
            >
              <OfficeBuilding v-if="row.detailType === 'FACILITY'" />
              <UserFilled v-else />
            </el-icon>
            <span class="tree-code" :class="{ 'tree-code--detail': row.nodeType === 'detail' }">
              {{ row.code || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="项目名称" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.name || '-' }}</template>
        </el-table-column>
        <el-table-column label="项目类别" width="150">
          <template #default="{ row }">{{ categoryLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="项目来源" width="140">
          <template #default="{ row }">{{ sourceLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="230">
          <template #default="{ row }">
            <div class="status-cell">
              <span class="status-tag" :class="`status-tag--${row.status}`">{{ statusLabel(row.status) }}</span>
              <el-progress
                class="status-progress"
                :percentage="row.percent"
                :stroke-width="6"
                :show-text="false"
                :color="progressColor(row.status)"
              />
              <span class="status-fraction">{{ row.numerator }}/{{ row.totalCount }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button v-if="canView" link type="primary" @click="openView(row)">查看</el-button>
              <el-button
                v-if="canEdit && (row.nodeType === 'project' || row.nodeType === 'detail')"
                link
                type="primary"
                @click="openEdit(row)"
              >编辑</el-button>
              <el-button
                v-if="row.nodeType === 'project' && row.canAccept"
                link
                type="success"
                @click="openAcceptance(row)"
              >验收</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleQuery"
        @current-change="loadData"
      />
    </el-card>

    <!-- 查看弹窗 -->
    <el-dialog v-model="viewVisible" :title="viewTitle" width="1000px" destroy-on-close class="construction-view-dialog">
      <template v-if="viewNode">
        <!-- 一级：施工名称 + 总体情况卡片 + 施工记录 + 申报明细施工情况 -->
        <template v-if="viewNode.nodeType === 'project'">
          <div v-loading="viewLoading" class="project-view">
            <!-- 施工名称：标题 + 类别/类型/编号 + 状态标签 -->
            <div class="construction-name">
              <div class="construction-name__main">
                <div class="construction-name__title">{{ projectView?.name || '-' }}</div>
                <div class="construction-name__meta">
                  <span>{{ projectView ? categoryLabel(projectView) : '-' }}</span>
                  <span class="construction-name__dot">·</span>
                  <span>{{ projectView ? sourceLabel(projectView) : '-' }}</span>
                  <span class="construction-name__dot">·</span>
                  <span>{{ projectView?.code || '-' }}</span>
                </div>
              </div>
              <div class="construction-name__tags">
                <span class="construction-name__tag construction-name__tag--approved">立项已通过</span>
                <span class="construction-name__tag" :class="`construction-name__tag--${projectView?.status}`">
                  {{ statusLabel(projectView?.status) }}
                </span>
              </div>
            </div>

            <!-- 该项目施工记录 -->
            <div class="section-header">
              <span class="section-header__title">该项目施工记录</span>
              <span class="section-header__extra">共 {{ projectView?.logCount ?? 0 }} 条</span>
            </div>
            <el-empty v-if="!projectView?.logs?.length" description="暂无施工记录" :image-size="60" />
            <el-table
              v-else
              :data="projectView?.logs || []"
              border
              size="small"
              class="construction-view-table"
            >
              <el-table-column prop="logCode" label="日志编号" width="160" />
              <el-table-column prop="constructionDate" label="施工日期" width="120" />
              <el-table-column label="巡检人" width="120">
                <template #default="{ row }">{{ row.inspector || '-' }}</template>
              </el-table-column>
              <el-table-column label="巡检状态" width="110">
                <template #default="{ row }">
                  <span class="status-tag" :class="`status-tag--${row.inspectionStatus === 'PASS' ? 'COMPLETED' : 'FAIL'}`">
                    {{ inspectionLabel(row.inspectionStatus) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="施工状态" width="110">
                <template #default="{ row }">
                  <!-- 巡检状态为整改时，施工状态同步展示为暂停施工 -->
                  <span
                    class="status-tag"
                    :class="row.inspectionStatus === 'FAIL' ? 'status-tag--SUSPENDED' : `status-tag--${row.constructionStatus}`"
                  >
                    {{ row.inspectionStatus === 'FAIL' ? '暂停施工' : statusLabel(row.constructionStatus) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="施工内容" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">{{ row.content || '-' }}</template>
              </el-table-column>
            </el-table>

            <!-- 申报明细施工情况 -->
            <div class="section-header section-header--inline">
              <span class="section-header__title">申报明细施工情况</span>
              <span class="section-header__extra">
                （共 {{ projectView?.children?.length ?? 0 }} 条申报）
              </span>
            </div>
            <el-empty v-if="!projectView?.children?.length" description="暂无申报明细" :image-size="60" />
            <el-table
              v-else
              :data="projectView.children"
              border
              class="declare-detail-table"
            >
              <el-table-column label="申报编码" width="150">
                <template #default="{ row }">{{ row.code || '-' }}</template>
              </el-table-column>
              <el-table-column label="申报标题" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">{{ row.name || '-' }}</template>
              </el-table-column>
              <el-table-column label="申报类型" width="130">
                <template #default="{ row }">{{ sourceLabel(row) }}</template>
              </el-table-column>
              <el-table-column label="申报类别" width="130">
                <template #default="{ row }">{{ categoryLabel(row) }}</template>
              </el-table-column>
              <el-table-column label="申报金额（元）" width="150" align="right">
                <template #default="{ row }">
                  {{ row.amount ? formatMoney(row.amount) : '-' }}
                </template>
              </el-table-column>
              <!-- 施工内容展示该项目施工记录中最新一条记录的施工内容 -->
              <el-table-column label="施工内容" min-width="260">
                <template #default>{{ latestLogContent || '-' }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>

        <!-- 二级：申报信息 + 明细进度 -->
        <template v-else-if="viewNode.nodeType === 'declare'">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="申报编号">{{ viewNode.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申报标题">{{ viewNode.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申报类别">{{ categoryLabel(viewNode) }}</el-descriptions-item>
            <el-descriptions-item label="申报类型">{{ sourceLabel(viewNode) }}</el-descriptions-item>
            <el-descriptions-item label="施工状态">
              <span class="status-tag" :class="`status-tag--${viewNode.status}`">{{ statusLabel(viewNode.status) }}</span>
              <span class="status-fraction status-fraction--inline">{{ viewNode.numerator }}/{{ viewNode.totalCount }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">明细进度</el-divider>
          <el-empty v-if="!viewNode.children?.length" description="暂无农户/设施明细" :image-size="60" />
          <el-table v-else :data="viewNode.children" border size="small" class="construction-view-table">
            <el-table-column label="类型" width="130">
              <template #default="{ row }">{{ detailTypeLabel(row.detailType) }}</template>
            </el-table-column>
            <el-table-column label="农户/设施名称" min-width="160">
              <template #default="{ row }">{{ row.name || '-' }}</template>
            </el-table-column>
            <el-table-column label="户号/编码" width="150">
              <template #default="{ row }">{{ row.code || '-' }}</template>
            </el-table-column>
            <el-table-column label="所属区划" width="130">
              <template #default="{ row }">{{ row.source || '-' }}</template>
            </el-table-column>
            <el-table-column label="施工状态" width="220">
              <template #default="{ row }">
                <div class="status-cell">
                  <span class="status-tag" :class="`status-tag--${row.status}`">{{ statusLabel(row.status) }}</span>
                  <el-progress
                    class="status-progress"
                    :percentage="row.percent"
                    :stroke-width="5"
                    :show-text="false"
                    :color="progressColor(row.status)"
                  />
                  <span class="status-fraction">{{ row.numerator }}/{{ row.totalCount }}</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 三级：明细信息 -->
        <template v-else>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="明细类型">{{ detailTypeLabel(viewNode.detailType) }}</el-descriptions-item>
            <el-descriptions-item label="农户/设施名称">{{ viewNode.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="户号/编码">{{ viewNode.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属区划">{{ viewNode.source || '-' }}</el-descriptions-item>
            <el-descriptions-item label="施工状态">
              <span class="status-tag" :class="`status-tag--${viewNode.status}`">{{ statusLabel(viewNode.status) }}</span>
              <span class="status-fraction status-fraction--inline">{{ viewNode.numerator }}/{{ viewNode.totalCount }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="开工日期">{{ viewNode.startDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完工日期">{{ viewNode.completeDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ viewNode.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </template>

      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editTitle" width="900px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="130px">
        <!-- 一级：施工队信息 + 施工资料上传 -->
        <template v-if="editForm.nodeLevel === 1">
          <el-divider content-position="left">施工队信息</el-divider>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="施工队名称" prop="builder">
                <el-input v-model="editForm.builder" placeholder="请输入施工队名称" maxlength="128" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="承包人姓名" prop="contractorName">
                <el-input v-model="editForm.contractorName" placeholder="请输入承包人姓名" maxlength="64" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="承包人联系电话" prop="contractorPhone">
                <el-input v-model="editForm.contractorPhone" placeholder="请输入承包人联系电话" maxlength="20" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="负责人姓名" prop="leaderName">
                <el-input v-model="editForm.leaderName" placeholder="请输入负责人姓名" maxlength="64" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="负责人联系电话" prop="leaderPhone">
                <el-input v-model="editForm.leaderPhone" placeholder="请输入负责人联系电话" maxlength="20" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">施工资料上传</el-divider>
          <div class="attachment-list">
            <div class="attachment-row attachment-row--header">
              <span class="attachment-col--type">附件类型</span>
              <span class="attachment-col--file">上传附件</span>
              <span class="attachment-col--remark">备注</span>
              <span class="attachment-col--action">操作</span>
            </div>
            <div v-for="(item, index) in attachmentRows" :key="index" class="attachment-row">
              <el-select
                v-model="item.attachmentType"
                placeholder="请选择附件类型"
                class="attachment-col--type"
                clearable
              >
                <el-option
                  v-for="opt in dictOptions('shi_gong_fu_jian_lei_xing')"
                  :key="opt.dictValue"
                  :label="opt.dictLabel"
                  :value="opt.dictValue"
                />
              </el-select>
              <div class="attachment-col--file">
                <template v-if="item.fileName">
                  <el-link type="primary" :underline="false" @click="previewAttachment(item)">
                    {{ item.fileName }}
                  </el-link>
                  <el-button link type="info" size="small" @click="clearAttachmentFile(item)">重新上传</el-button>
                </template>
                <el-upload
                  v-else
                  :auto-upload="false"
                  :show-file-list="false"
                  @change="(file: UploadFile) => handleAttachmentChange(item, file)"
                >
                  <el-button size="small" :loading="item.uploading" icon="Upload">点击上传</el-button>
                </el-upload>
              </div>
              <el-input
                v-model="item.remark"
                placeholder="请输入备注"
                maxlength="128"
                class="attachment-col--remark"
              />
              <div class="attachment-col--action">
                <el-button link type="danger" icon="Delete" @click="removeAttachmentRow(index)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="!attachmentRows.length" description="暂无施工资料" :image-size="50" />
            <el-button link type="primary" icon="Plus" class="attachment-add" @click="addAttachmentRow">
              添加附件
            </el-button>
          </div>
        </template>

        <!-- 三级：施工进度 -->
        <template v-else>
          <el-form-item label="施工状态" prop="buildStatus">
            <el-select v-model="editForm.buildStatus" placeholder="请选择施工状态" style="width: 100%">
              <el-option
                v-for="item in buildStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="开工日期" prop="startDate">
            <el-date-picker
              v-model="editForm.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择开工日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="完工日期" prop="completeDate">
            <el-date-picker
              v-model="editForm.completeDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择完工日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="editForm.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入备注"
              maxlength="512"
              show-word-limit
            />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getDictByType } from '@/api/dict'
import type { DictVO } from '@/types/system'
import {
  getConstructionPage,
  updateConstruction,
  CONSTRUCTION_STATUS_NOT_STARTED,
  CONSTRUCTION_STATUS_IN_PROGRESS,
  CONSTRUCTION_STATUS_COMPLETED,
  getConstructionDetail,
  type ConstructionNodeVO,
  type ConstructionEditDTO,
  type ConstructionAttachmentItem
} from '@/api/construction'
import { uploadFile, getPresignUrl } from '@/api/file'

/**
 * 施工管理
 * 所属菜单：项目全周期管控 → 施工管理
 * 数据为三级树：立项项目 → 申报内容 → 农户/公共基础设施明细
 * 施工状态仅记录在三级明细，一级/二级进度由其明细实时聚合
 */
const router = useRouter()
const userStore = useUserStore()

const canView = computed(() => userStore.hasPerm('project:construction:list'))
const canEdit = computed(() => userStore.hasPerm('project:construction:edit'))

/* ============ 字典 ============ */
const dictMap = ref<Record<string, DictVO[]>>({})
const DICT_TYPES = ['xiang_mu_lei_bie', 'xiang_mu_lei_xing', 'shen_bao_lei_bie', 'shen_bao_lei_xing', 'shi_gong_fu_jian_lei_xing']
const dictOptions = (type: string): DictVO[] => dictMap.value[type] || []
const dictLabel = (type: string, value?: string): string => {
  if (!value) return ''
  const hit = dictOptions(type).find((item) => item.dictValue === value)
  return hit?.dictLabel || value
}

const loadDicts = async () => {
  const results = await Promise.all(DICT_TYPES.map((type) => getDictByType(type)))
  const map: Record<string, DictVO[]> = {}
  DICT_TYPES.forEach((type, index) => {
    map[type] = results[index].data || []
  })
  dictMap.value = map
}

const detailTypeLabel = (value?: string): string => {
  if (value === 'HOUSEHOLD') return '农户'
  if (value === 'FACILITY') return '公共基础设施'
  return value || '-'
}

/** 类别列：一级取项目类别字典，二级取申报类别字典，三级取明细类型 */
const categoryLabel = (row: ConstructionNodeVO): string => {
  if (row.nodeType === 'project') return dictLabel('xiang_mu_lei_bie', row.category) || '-'
  if (row.nodeType === 'declare') return dictLabel('shen_bao_lei_bie', row.category) || '-'
  return detailTypeLabel(row.detailType || row.category)
}

/** 来源列：一级取项目类型字典，二级取申报类型字典，三级取所属区划 */
const sourceLabel = (row: ConstructionNodeVO): string => {
  if (row.nodeType === 'project') return dictLabel('xiang_mu_lei_xing', row.source) || '-'
  if (row.nodeType === 'declare') return dictLabel('shen_bao_lei_xing', row.source) || '-'
  return row.source || '-'
}

/* ============ 筛选与列表 ============ */
interface DisplayRow extends Omit<ConstructionNodeVO, 'children'> {
  /** 是否含子节点，用于渲染展开箭头（传入表格前剥离 children，避免 el-table 走树形模式） */
  hasChildren: boolean
  /** 原始节点（含子节点），查看弹窗使用 */
  raw: ConstructionNodeVO
}

const queryForm = reactive({
  projectCode: '',
  projectName: '',
  projectCategory: '',
  projectType: ''
})
const loading = ref(false)
const tableData = ref<ConstructionNodeVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

/** 已收起的节点集合（默认全部展开） */
const collapsedKeys = ref<Set<string>>(new Set())

const toggleRow = (key: string) => {
  const next = new Set(collapsedKeys.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  collapsedKeys.value = next
}

/** 手写展开/收起，保证三个层级缩进一致 */
const visibleRows = computed<DisplayRow[]>(() => {
  const rows: DisplayRow[] = []
  const walk = (list: ConstructionNodeVO[]) => {
    list.forEach((node) => {
      const { children, ...rest } = node
      rows.push({ ...rest, hasChildren: !!children?.length, raw: node })
      if (children?.length && !collapsedKeys.value.has(node.nodeKey)) {
        walk(children)
      }
    })
  }
  walk(tableData.value)
  return rows
})

const rowClassName = ({ row }: { row: { nodeType: string } }): string =>
  row.nodeType === 'detail' ? 'construction-detail-row' : ''

const loadData = async () => {
  loading.value = true
  try {
    const res = await getConstructionPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      projectCode: queryForm.projectCode || undefined,
      projectName: queryForm.projectName || undefined,
      projectCategory: queryForm.projectCategory || undefined,
      projectType: queryForm.projectType || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  pageNum.value = 1
  loadData()
}

const handleReset = () => {
  queryForm.projectCode = ''
  queryForm.projectName = ''
  queryForm.projectCategory = ''
  queryForm.projectType = ''
  pageNum.value = 1
  loadData()
}

/* ============ 状态展示 ============ */
const statusLabel = (status?: string): string => {
  if (status === CONSTRUCTION_STATUS_COMPLETED) return '已完工'
  if (status === CONSTRUCTION_STATUS_IN_PROGRESS) return '施工中'
  return '未开工'
}

const progressColor = (status?: string): string => {
  if (status === CONSTRUCTION_STATUS_COMPLETED) return '#67c23a'
  if (status === CONSTRUCTION_STATUS_IN_PROGRESS) return '#409eff'
  return '#c0c4cc'
}

/** 巡检状态展示：合格 / 整改 */
const inspectionLabel = (status?: string): string => {
  if (status === 'PASS') return '合格'
  if (status === 'FAIL') return '整改'
  return '-'
}

/** 金额展示：千分位 + 两位小数（元） */
const formatMoney = (amount?: number): string => {
  if (amount === null || amount === undefined) return '-'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/* ============ 查看弹窗 ============ */
const viewVisible = ref(false)
const viewNode = ref<ConstructionNodeVO | null>(null)
const viewDetail = ref<ConstructionNodeVO | null>(null)
const viewLoading = ref(false)

const viewTitle = computed(() => {
  if (viewNode.value?.nodeType === 'declare') return '申报施工详情'
  if (viewNode.value?.nodeType === 'detail') return '明细施工详情'
  return '项目施工详情'
})

/** 一级查看使用详情接口数据（含日志、巡检合格率、附件），加载完成前回退到列表行数据 */
const projectView = computed(() => viewDetail.value || viewNode.value)

/** 最新一条施工记录的施工内容（日志已按施工日期倒序，取第一条） */
const latestLogContent = computed(() => projectView.value?.logs?.[0]?.content || '')

const openView = async (row: DisplayRow) => {
  viewNode.value = row.raw
  viewDetail.value = null
  viewVisible.value = true
  if (row.nodeType === 'project') {
    viewLoading.value = true
    try {
      const res = await getConstructionDetail(row.approvalId)
      viewDetail.value = res.data
    } finally {
      viewLoading.value = false
    }
  }
}

/* ============ 验收 ============ */
const openAcceptance = async (row: DisplayRow) => {
  await ElMessageBox.confirm(`「${row.name}」全部明细已完工，是否进入验收管理？`, '发起验收', {
    type: 'success'
  })
  router.push({ path: '/project/acceptance', query: { approvalId: row.approvalId } })
}

/* ============ 编辑弹窗 ============ */
interface EditForm {
  approvalId: string
  declareId: string
  detailId: string
  nodeLevel: 1 | 3
  buildStatus: string
  builder: string
  contractorName: string
  contractorPhone: string
  leaderName: string
  leaderPhone: string
  startDate: string
  completeDate: string
  remark: string
}

/** 施工资料附件行：一行对应一条附件 */
interface AttachmentRow {
  id?: string
  attachmentType: string
  remark: string
  fileName: string
  fileKey: string
  bucket: string
  fileSize?: number
  uploading: boolean
}

const buildStatusOptions = [
  { value: CONSTRUCTION_STATUS_NOT_STARTED, label: '未开工' },
  { value: CONSTRUCTION_STATUS_IN_PROGRESS, label: '施工中' },
  { value: CONSTRUCTION_STATUS_COMPLETED, label: '已完工' }
]

const editVisible = ref(false)
const submitLoading = ref(false)
const editFormRef = ref<FormInstance>()
const attachmentRows = ref<AttachmentRow[]>([])
const editForm = reactive<EditForm>({
  approvalId: '',
  declareId: '',
  detailId: '',
  nodeLevel: 1,
  buildStatus: CONSTRUCTION_STATUS_NOT_STARTED,
  builder: '',
  contractorName: '',
  contractorPhone: '',
  leaderName: '',
  leaderPhone: '',
  startDate: '',
  completeDate: '',
  remark: ''
})

const editTitle = computed(() => (editForm.nodeLevel === 1 ? '编辑施工信息' : '编辑施工进度'))

const editRules = computed<FormRules>(() => ({
  buildStatus: editForm.nodeLevel === 3
    ? [{ required: true, message: '请选择施工状态', trigger: 'change' }]
    : []
}))

const openEdit = async (row: DisplayRow) => {
  Object.assign(editForm, {
    approvalId: row.approvalId,
    declareId: row.declareId || '',
    detailId: row.detailId || '',
    nodeLevel: row.nodeType === 'project' ? 1 : 3,
    buildStatus: row.buildStatus || CONSTRUCTION_STATUS_NOT_STARTED,
    builder: row.builder || '',
    contractorName: row.contractorName || '',
    contractorPhone: row.contractorPhone || '',
    leaderName: row.leaderName || '',
    leaderPhone: row.leaderPhone || '',
    startDate: row.startDate || '',
    completeDate: row.completeDate || '',
    remark: row.remark || ''
  })
  attachmentRows.value = []
  // 一级编辑需要回显施工队信息与已上传资料，先拉取项目施工详情
  if (row.nodeType === 'project') {
    try {
      const res = await getConstructionDetail(row.approvalId)
      const detail = res.data
      Object.assign(editForm, {
        builder: detail.builder || '',
        contractorName: detail.contractorName || '',
        contractorPhone: detail.contractorPhone || '',
        leaderName: detail.leaderName || '',
        leaderPhone: detail.leaderPhone || ''
      })
      attachmentRows.value = (detail.attachments || []).map((item) => ({
        id: item.id,
        attachmentType: item.attachmentType || '',
        remark: item.remark || '',
        fileName: item.fileName || '',
        fileKey: item.fileKey || '',
        bucket: item.bucket || '',
        fileSize: item.fileSize,
        uploading: false
      }))
    } catch {
      return
    }
  }
  editVisible.value = true
}

const addAttachmentRow = () => {
  attachmentRows.value.push({
    attachmentType: '',
    remark: '',
    fileName: '',
    fileKey: '',
    bucket: '',
    uploading: false
  })
}

const removeAttachmentRow = (index: number) => {
  attachmentRows.value.splice(index, 1)
}

const clearAttachmentFile = (item: AttachmentRow) => {
  item.fileName = ''
  item.fileKey = ''
  item.bucket = ''
  item.fileSize = undefined
}

/** 选择文件后立即上传至 MinIO，仅保存返回的存储信息 */
const handleAttachmentChange = async (item: AttachmentRow, file: UploadFile) => {
  if (!file.raw) return
  item.uploading = true
  try {
    const res = await uploadFile(file.raw)
    item.fileName = res.data.fileName
    item.fileKey = res.data.fileKey
    item.bucket = res.data.bucket
    item.fileSize = res.data.fileSize
  } finally {
    item.uploading = false
  }
}

/** 使用时效签名URL预览附件，禁止永久直链 */
const previewAttachment = async (item: AttachmentRow) => {
  if (!item.bucket || !item.fileKey) return
  const res = await getPresignUrl({
    bucket: item.bucket,
    fileKey: item.fileKey,
    fileName: item.fileName,
    download: false
  })
  window.open(res.data, '_blank')
}

const handleSubmit = async () => {
  if (!editFormRef.value) return
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  const isProject = editForm.nodeLevel === 1
  const attachments: ConstructionAttachmentItem[] | undefined = isProject
    ? attachmentRows.value
        .filter((item) => !!item.fileKey)
        .map((item) => ({
          id: item.id,
          attachmentType: item.attachmentType || undefined,
          fileName: item.fileName,
          fileKey: item.fileKey,
          bucket: item.bucket,
          fileSize: item.fileSize,
          remark: item.remark || undefined
        }))
    : undefined
  const payload: ConstructionEditDTO = {
    approvalId: editForm.approvalId,
    declareId: isProject ? undefined : editForm.declareId,
    detailId: isProject ? undefined : editForm.detailId,
    nodeLevel: editForm.nodeLevel,
    buildStatus: isProject ? undefined : editForm.buildStatus,
    builder: isProject ? editForm.builder : undefined,
    contractorName: isProject ? editForm.contractorName : undefined,
    contractorPhone: isProject ? editForm.contractorPhone : undefined,
    leaderName: isProject ? editForm.leaderName : undefined,
    leaderPhone: isProject ? editForm.leaderPhone : undefined,
    attachments,
    startDate: isProject ? undefined : editForm.startDate || undefined,
    completeDate: isProject ? undefined : editForm.completeDate || undefined,
    remark: isProject ? undefined : editForm.remark || undefined
  }
  submitLoading.value = true
  try {
    await updateConstruction(payload)
    ElMessage.success('保存成功')
    editVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

onMounted(async () => {
  await loadDicts()
  await loadData()
})
</script>

<style scoped>
.construction-page {
  padding: 0 16px 16px;
}

.query-card {
  margin-bottom: 12px;
}

/* 筛选项与查询/刷新按钮强制同一行，控件按剩余宽度自适应 */
.query-form {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}

.query-form :deep(.el-form-item) {
  flex: 1 1 0;
  min-width: 0;
  margin-right: 16px;
  margin-bottom: 0;
}

.query-form :deep(.el-form-item__content) {
  flex: 1;
  min-width: 0;
}

.query-form :deep(.el-input),
.query-form :deep(.el-select) {
  width: 100%;
}

/* 操作按钮组不参与拉伸 */
.query-form :deep(.query-item--actions) {
  flex: 0 0 auto;
  margin-right: 0;
}

.construction-table {
  margin-top: 4px;
}

/* 表头首列与层级文字起点对齐（单元格 12px + 展开按钮 20px） */
.construction-table :deep(.el-table__header th:first-child .cell) {
  padding-left: 32px;
}

/* 展开箭头：一级黑色、二级与"查看"按钮同为主题蓝 */
.tree-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  vertical-align: middle;
  cursor: pointer;
  color: #000000;
  font-weight: 700;
}

.tree-toggle--declare {
  color: var(--el-color-primary, #409eff);
  margin-left: 15px;
}

.tree-toggle--detail {
  margin-left: 15px;
}

.tree-toggle .el-icon {
  font-size: 14px;
  transition: transform 0.2s;
}

/* 图标为填充路径，描边叠加当前色实现加粗效果 */
.tree-toggle .el-icon svg {
  stroke: currentColor;
  stroke-width: 3;
  stroke-linejoin: round;
}

.tree-toggle .el-icon.is-expanded {
  transform: rotate(90deg);
}

.tree-code {
  display: inline-block;
  vertical-align: middle;
  white-space: nowrap;
}

.tree-code--detail {
  padding-left: 6px;
}

/* 三级明细行文字字号略小于一级、二级 */
.construction-table :deep(.construction-detail-row) {
  font-size: 12px;
}

/* 三级明细类型图标：农户为浅蓝头像、公共基础设施为浅橙设施图标 */
.detail-type-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  vertical-align: middle;
  font-size: 11px;
}

.detail-type-icon--household {
  background: #e8f3ff;
  color: #409eff;
}

.detail-type-icon--facility {
  background: #fdf6ec;
  color: #e6a23c;
}

/* 状态列：状态标签 + 进度条 + 分数 */
.status-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-tag {
  flex: none;
  padding: 0 6px;
  height: 20px;
  line-height: 20px;
  border-radius: 3px;
  font-size: 12px;
}

.status-tag--NOT_STARTED {
  color: #909399;
  background: #f4f4f5;
}

.status-tag--IN_PROGRESS {
  color: #409eff;
  background: #ecf5ff;
}

.status-tag--COMPLETED {
  color: #67c23a;
  background: #f0f9eb;
}

.status-progress {
  flex: 1;
  min-width: 60px;
}

.status-fraction {
  flex: none;
  min-width: 34px;
  text-align: right;
  color: #606266;
  font-weight: 600;
}

.status-fraction--inline {
  margin-left: 8px;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.construction-view-table {
  margin-top: 4px;
}

/* ============ 一级查看：施工名称 + 总体情况卡片 ============ */
.project-view {
  min-height: 200px;
}

.construction-name {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px 14px 24px;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

/* 左侧主题色竖条装饰 */
.construction-name::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 16px;
  bottom: 16px;
  width: 3px;
  border-radius: 2px;
  background: var(--el-color-primary, #409eff);
}

.construction-name__main {
  flex: 1;
  min-width: 0;
}

.construction-name__title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
}

.construction-name__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}

.construction-name__dot {
  color: #c0c4cc;
}

.construction-name__tags {
  display: flex;
  flex: none;
  align-items: center;
  gap: 8px;
}

.construction-name__tag {
  padding: 0 10px;
  height: 24px;
  line-height: 22px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 12px;
}

.construction-name__tag--approved,
.construction-name__tag--COMPLETED {
  border-color: #e1f3d8;
  background: #f0f9eb;
  color: #67c23a;
}

.construction-name__tag--IN_PROGRESS {
  border-color: #d9ecff;
  background: #ecf5ff;
  color: #409eff;
}

.construction-name__tag--NOT_STARTED {
  border-color: #e9e9eb;
  background: #f4f4f5;
  color: #909399;
}

/* 查看弹窗区块标题：主题色竖条 + 右侧统计 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 10px;
}

.section-header__title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.section-header__title::before {
  content: '';
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--el-color-primary, #409eff);
}

.section-header__extra {
  color: #909399;
  font-size: 12px;
}

/* 标题与说明文字同行展示（申报明细施工情况） */
.section-header--inline {
  justify-content: flex-start;
  gap: 4px;
}

/* 巡检整改标签 */
.status-tag--FAIL {
  color: #e6a23c;
  background: #fdf6ec;
}

/* 暂停施工标签 */
.status-tag--SUSPENDED {
  color: #909399;
  background: #f4f4f5;
}

/* ============ 申报明细施工情况（表格） ============ */
.declare-detail-table {
  --el-table-header-bg-color: #fafafa;
  --el-table-border-color: #ebeef5;
  --el-table-header-text-color: #303133;
  font-size: 13px;
}

.declare-detail-table :deep(.el-table__header th) {
  height: 44px;
  padding: 0;
  font-weight: 600;
}

.declare-detail-table :deep(.el-table__body td) {
  height: 44px;
  padding: 0;
}

/* ============ 一级编辑：施工资料上传 ============ */
.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attachment-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.attachment-row--header {
  color: #909399;
  font-size: 13px;
}

.attachment-col--type {
  flex: none;
  width: 180px;
}

.attachment-col--file {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.attachment-col--file .el-link {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-col--remark {
  flex: none;
  width: 200px;
}

.attachment-col--action {
  flex: none;
  width: 70px;
  text-align: right;
}

.attachment-add {
  align-self: flex-start;
}
</style>

<style>
/* 查看弹窗固定高度：弹窗被 teleport 到 body，需使用非 scoped 样式 */
.construction-view-dialog {
  height: 80vh;
  margin-top: 8vh;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
}

.construction-view-dialog .el-dialog__header,
.construction-view-dialog .el-dialog__footer {
  flex: none;
}

.construction-view-dialog .el-dialog__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
</style>
