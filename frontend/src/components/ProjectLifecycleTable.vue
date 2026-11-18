<template>
  <div class="project-lifecycle">
    <!-- 立项日期筛选：按立项日期区间过滤上方汇总列表 -->
    <div class="filter-bar">
      <span class="filter-label">立项日期</span>
      <el-date-picker
        v-model="approvalDateRange"
        class="approval-date-filter"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        size="small"
        clearable
      />
    </div>
    <el-table
      v-loading="loading"
      :data="filteredRows"
      border
      size="small"
      empty-text="暂无关联项目"
    >
      <!-- 展开行：按阶段展示明细级完整信息（含各阶段附件与记录） -->
      <el-table-column type="expand" width="34">
        <template #default="{ row }">
          <div class="lifecycle-detail">
            <!-- ============ 申报阶段 ============ -->
            <div class="detail-section">
              <div class="detail-section-title">申报信息</div>
              <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="申报标题">{{ row.declarationTitle || '—' }}</el-descriptions-item>
                <el-descriptions-item label="申报类型">{{ dictLabel('shen_bao_lei_xing', row.declarationType) }}</el-descriptions-item>
                <el-descriptions-item label="申报日期">{{ row.declarationDate || '—' }}</el-descriptions-item>
                <el-descriptions-item label="明细申报金额">{{ formatMoney(row.amount) }}</el-descriptions-item>
                <el-descriptions-item label="明细审核状态">{{ declareStatusLabel(row) }}</el-descriptions-item>
                <el-descriptions-item label="事项说明">{{ row.remark || '—' }}</el-descriptions-item>
              </el-descriptions>
              <div class="stage-subtitle">申报附件</div>
              <el-table v-if="row.declarationAttachments?.length" :data="row.declarationAttachments" border size="small">
                <el-table-column label="来源" width="100" align="center">
                  <template #default="{ row: file }">{{ file.scopeLabel || '—' }}</template>
                </el-table-column>
                <el-table-column label="附件类型" width="130">
                  <template #default="{ row: file }">
                    {{ file.attachmentType ? dictLabel('fu_jian_lei_xing', file.attachmentType) : '—' }}
                  </template>
                </el-table-column>
                <el-table-column label="附件名称" min-width="260" show-overflow-tooltip>
                  <template #default="{ row: file }">
                    <div class="attachment-line">
                      <el-link type="primary" :underline="false" class="attachment-name" @click="openFile(file)">
                        {{ file.fileName || '—' }}
                      </el-link>
                      <el-icon
                        v-if="file.fileKey"
                        class="attachment-download"
                        title="下载"
                        @click="openFile(file, true)"
                      ><Download /></el-icon>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="备注" width="150" show-overflow-tooltip>
                  <template #default="{ row: file }">{{ file.remark || '—' }}</template>
                </el-table-column>
                <el-table-column label="上传时间" width="170" align="center">
                  <template #default="{ row: file }">{{ file.createTime || '—' }}</template>
                </el-table-column>
              </el-table>
              <div v-else class="empty-inline">暂无附件</div>
              <div class="stage-subtitle">申报记录</div>
              <el-table v-if="row.declarationAuditLogs?.length" :data="row.declarationAuditLogs" border size="small">
                <el-table-column label="操作时间" width="165" align="center">
                  <template #default="{ row: record }">{{ record.createTime || '—' }}</template>
                </el-table-column>
                <el-table-column label="动作" width="110" align="center">
                  <template #default="{ row: record }">{{ auditActionLabel(record.action) }}</template>
                </el-table-column>
                <el-table-column label="状态变化" width="180" align="center">
                  <template #default="{ row: record }">{{ auditStatusFlow(record) }}</template>
                </el-table-column>
                <el-table-column label="操作人" width="110">
                  <template #default="{ row: record }">{{ record.operatorName || '—' }}</template>
                </el-table-column>
                <el-table-column label="审核意见 / 修改说明" min-width="180" show-overflow-tooltip>
                  <template #default="{ row: record }">
                    {{ record.remark || '—' }}
                    <span v-if="record.attachmentChanged" class="attachment-remark">（附件有变更）</span>
                  </template>
                </el-table-column>
              </el-table>
              <div v-else class="empty-inline">暂无申报记录</div>
            </div>

            <!-- ============ 立项阶段 ============ -->
            <div class="detail-section">
              <div class="detail-section-title">立项信息</div>
              <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="立项编号">{{ row.approvalCode || '—' }}</el-descriptions-item>
                <el-descriptions-item label="项目名称">{{ row.projectName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="项目类别">{{ dictLabel('xiang_mu_lei_bie', row.projectCategory) }}</el-descriptions-item>
                <el-descriptions-item label="项目类型">{{ dictLabel('xiang_mu_lei_xing', row.projectType) }}</el-descriptions-item>
                <el-descriptions-item label="立项状态">{{ approvalStatusLabel(row.approvalStatus) }}</el-descriptions-item>
                <el-descriptions-item label="立项日期">{{ row.approvalSubmitTime ? row.approvalSubmitTime.slice(0, 10) : '—' }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- ============ 施工阶段 ============ -->
            <div class="detail-section">
              <div class="detail-section-title">施工信息</div>
              <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="施工状态">{{ buildStatusLabel(row) }}</el-descriptions-item>
                <el-descriptions-item label="开工日期">{{ row.startDate || '—' }}</el-descriptions-item>
                <el-descriptions-item label="完工日期">{{ row.completeDate || '—' }}</el-descriptions-item>
              </el-descriptions>
              <div class="stage-subtitle">施工记录</div>
              <el-table v-if="row.constructionRecords?.length" :data="row.constructionRecords" border size="small">
                <el-table-column label="施工日期" width="110" align="center">
                  <template #default="{ row: record }">{{ record.constructionDate || '—' }}</template>
                </el-table-column>
                <el-table-column label="施工状态" width="100" align="center">
                  <template #default="{ row: record }">{{ buildStatusText(record.constructionStatus) }}</template>
                </el-table-column>
                <el-table-column label="巡检人" width="110">
                  <template #default="{ row: record }">{{ record.inspector || '—' }}</template>
                </el-table-column>
                <el-table-column label="巡检结果" width="100" align="center">
                  <template #default="{ row: record }">{{ inspectionLabel(record.inspectionStatus) }}</template>
                </el-table-column>
                <el-table-column label="施工内容" min-width="200" show-overflow-tooltip>
                  <template #default="{ row: record }">{{ record.content || '—' }}</template>
                </el-table-column>
                <el-table-column label="附件" width="180">
                  <template #default="{ row: record }">
                    <el-button
                      v-if="record.attachments?.length"
                      link
                      type="primary"
                      @click="openAttachments('施工记录附件', record.attachments, 'shi_gong_fu_jian_lei_xing')"
                    >附件 ({{ record.attachments.length }})</el-button>
                    <span v-else class="empty-inline">—</span>
                  </template>
                </el-table-column>
              </el-table>
              <div v-else class="empty-inline">暂无施工记录</div>
              <template v-if="row.constructionAttachments?.length">
                <div class="stage-subtitle">其他施工资料</div>
                <div class="attachment-lines">
                  <div v-for="file in row.constructionAttachments" :key="file.id" class="attachment-line">
                    <el-link
                      type="primary"
                      :underline="false"
                      class="attachment-name"
                      :title="file.fileName"
                      @click="openFile(file)"
                    >{{ file.fileName || '—' }}</el-link>
                    <el-icon v-if="file.fileKey" class="attachment-download" title="下载" @click="openFile(file, true)">
                      <Download />
                    </el-icon>
                    <span v-if="file.remark" class="attachment-remark">（{{ file.remark }}）</span>
                  </div>
                </div>
              </template>
            </div>

            <!-- ============ 资金阶段 ============ -->
            <div class="detail-section">
              <div class="detail-section-title">资金信息</div>
              <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="累计拨付金额">{{ formatMoney(row.fundAmount) }}</el-descriptions-item>
                <el-descriptions-item label="拨付笔数">{{ row.fundRecords?.length || 0 }} 笔</el-descriptions-item>
              </el-descriptions>
              <div class="stage-subtitle">拨付记录</div>
              <el-table v-if="row.fundRecords?.length" :data="row.fundRecords" border size="small">
                <el-table-column label="登记时间" width="165" align="center">
                  <template #default="{ row: record }">{{ record.createTime || '—' }}</template>
                </el-table-column>
                <el-table-column label="下拨类型" width="120">
                  <template #default="{ row: record }">{{ dictLabel('xia_bo_lei_xing', record.fundType) }}</template>
                </el-table-column>
                <el-table-column label="拨付金额(元)" width="120" align="right">
                  <template #default="{ row: record }">{{ formatMoney(record.amount) }}</template>
                </el-table-column>
                <el-table-column label="经办人" width="100">
                  <template #default="{ row: record }">{{ record.createBy || '—' }}</template>
                </el-table-column>
                <el-table-column label="备注" min-width="160" show-overflow-tooltip>
                  <template #default="{ row: record }">{{ record.remark || '—' }}</template>
                </el-table-column>
                <el-table-column label="凭证附件" width="180">
                  <template #default="{ row: record }">
                    <el-button
                      v-if="record.attachments?.length"
                      link
                      type="primary"
                      @click="openAttachments('资金凭证附件', record.attachments, 'zi_jin_fu_jian_lei_xing')"
                    >附件 ({{ record.attachments.length }})</el-button>
                    <span v-else class="empty-inline">—</span>
                  </template>
                </el-table-column>
              </el-table>
              <div v-else class="empty-inline">暂无拨付记录</div>
            </div>

            <!-- ============ 验收阶段 ============ -->
            <div class="detail-section">
              <div class="detail-section-title">验收信息</div>
              <el-table v-if="row.acceptanceRecords?.length" :data="row.acceptanceRecords" border size="small">
                <el-table-column label="验收时间" width="165" align="center">
                  <template #default="{ row: record }">{{ record.acceptTime || '—' }}</template>
                </el-table-column>
                <el-table-column label="验收状态" width="100" align="center">
                  <template #default="{ row: record }">{{ acceptStatusText(record.acceptStatus) }}</template>
                </el-table-column>
                <el-table-column label="验收结果" width="120">
                  <template #default="{ row: record }">{{ dictLabel('yan_shou_jie_guo', record.acceptResult) }}</template>
                </el-table-column>
                <el-table-column label="验收人" width="100">
                  <template #default="{ row: record }">{{ record.acceptBy || '—' }}</template>
                </el-table-column>
                <el-table-column label="验收备注" min-width="160" show-overflow-tooltip>
                  <template #default="{ row: record }">{{ record.remark || '—' }}</template>
                </el-table-column>
                <el-table-column label="验收资料" width="180">
                  <template #default="{ row: record }">
                    <el-button
                      v-if="record.attachments?.length"
                      link
                      type="primary"
                      @click="openAttachments('验收资料附件', record.attachments, 'yan_shou_fu_jian_lei_xing')"
                    >附件 ({{ record.attachments.length }})</el-button>
                    <span v-else class="empty-inline">—</span>
                  </template>
                </el-table-column>
              </el-table>
              <div v-else class="empty-inline">暂无验收记录</div>
            </div>
          </div>
        </template>
      </el-table-column>

      <!-- 汇总列：一条明细对应同一农户/设施的一次项目全周期链路 -->
      <el-table-column prop="declarationCode" label="申报编号" width="96" show-overflow-tooltip>
        <template #default="{ row }">{{ row.declarationCode || '—' }}</template>
      </el-table-column>
      <el-table-column label="申报日期" width="100" align="center">
        <template #default="{ row }">{{ row.declarationDate || '—' }}</template>
      </el-table-column>
      <el-table-column label="申报金额(元)" width="100" align="right">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="申报状态" width="76" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="declareStatusTagType(row)">{{ declareStatusLabel(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="立项日期" width="100" align="center">
        <template #default="{ row }">{{ row.approvalSubmitTime ? row.approvalSubmitTime.slice(0, 10) : '—' }}</template>
      </el-table-column>
      <el-table-column label="立项项目" min-width="76" show-overflow-tooltip>
        <template #default="{ row }">{{ row.projectName || '—' }}</template>
      </el-table-column>
      <el-table-column label="立项状态" width="72" align="center">
        <template #default="{ row }">{{ approvalStatusLabel(row.approvalStatus) }}</template>
      </el-table-column>
      <el-table-column label="施工状态" width="72" align="center">
        <template #default="{ row }">{{ buildStatusLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="拨付金额(元)" width="100" align="right">
        <template #default="{ row }">{{ formatMoney(row.fundAmount) }}</template>
      </el-table-column>
      <el-table-column label="验收状态" width="76" align="center">
        <template #default="{ row }">{{ acceptStatusLabel(row) }}</template>
      </el-table-column>
    </el-table>

    <!-- 附件列表弹窗：点击记录行的附件按钮后展示对应附件 -->
    <el-dialog
      v-model="attachmentDialogVisible"
      :title="attachmentDialogTitle"
      width="720px"
      append-to-body
      destroy-on-close
    >
      <el-table :data="attachmentDialogFiles" border size="small" empty-text="暂无附件">
        <el-table-column label="附件类型" width="120">
          <template #default="{ row: file }">
            {{ file.attachmentType ? dictLabel(attachmentDialogDictType, file.attachmentType) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="附件名称" min-width="300" show-overflow-tooltip>
          <template #default="{ row: file }">
            <div class="attachment-line">
              <el-link type="primary" :underline="false" class="attachment-name" @click="openFile(file)">
                {{ file.fileName || '—' }}
              </el-link>
              <el-icon v-if="file.fileKey" class="attachment-download" title="下载" @click="openFile(file, true)">
                <Download />
              </el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="备注" width="150" show-overflow-tooltip>
          <template #default="{ row: file }">{{ file.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="上传时间" width="170" align="center">
          <template #default="{ row: file }">{{ file.createTime || '—' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getDictByType } from '@/api/dict'
import { getPresignUrl } from '@/api/file'
import { getHouseholdProjects } from '@/api/household'
import { getFacilityProjects } from '@/api/facility'
import type {
  DeclarationAuditLogVO,
  DictVO,
  LifecycleAttachmentVO,
  ProjectLifecycleVO
} from '@/types/system'

/**
 * 项目全周期明细表格（农户 / 公共基础设施共用）
 * 仅展示明细级信息：申报 → 立项 → 施工 → 资金拨付 → 验收
 * 每个阶段的附件与业务记录均完整展示
 */
const props = defineProps<{
  /** 关联对象类型：household农户 / facility公共基础设施 */
  bizType: 'household' | 'facility'
  /** 关联对象主键（农户ID / 设施ID） */
  targetId?: string
}>()

const loading = ref(false)
const rows = ref<ProjectLifecycleVO[]>([])
const dictMap = ref<Record<string, DictVO[]>>({})

/** 立项日期筛选区间（[开始, 结束]，YYYY-MM-DD） */
const approvalDateRange = ref<[string, string] | null>(null)

/** 按立项日期过滤后的列表；未选择区间时展示全部 */
const filteredRows = computed(() => {
  const range = approvalDateRange.value
  if (!range || !range[0] || !range[1]) {
    return rows.value
  }
  return rows.value.filter((row) => {
    const date = row.approvalSubmitTime ? row.approvalSubmitTime.slice(0, 10) : ''
    return !!date && date >= range[0] && date <= range[1]
  })
})

/** 加载字典（申报类型、项目类别/类型、下拨类型、验收结果、各阶段附件类型） */
const DICT_TYPES = [
  'shen_bao_lei_xing',
  'xiang_mu_lei_bie',
  'xiang_mu_lei_xing',
  'xia_bo_lei_xing',
  'yan_shou_jie_guo',
  'fu_jian_lei_xing',
  'shi_gong_fu_jian_lei_xing',
  'zi_jin_fu_jian_lei_xing',
  'yan_shou_fu_jian_lei_xing'
]
const loadDicts = async () => {
  await Promise.all(
    DICT_TYPES.map(async (type) => {
      try {
        const res = await getDictByType(type)
        dictMap.value[type] = res.data || []
      } catch {
        dictMap.value[type] = []
      }
    })
  )
}

const dictLabel = (type: string, value?: string) => {
  if (!value) return '—'
  const hit = (dictMap.value[type] || []).find((d) => d.dictValue === value)
  return hit ? hit.dictLabel : value
}

/** 加载明细级全周期数据（含各阶段附件与记录） */
const load = async () => {
  if (!props.targetId) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const res = props.bizType === 'household'
      ? await getHouseholdProjects(props.targetId)
      : await getFacilityProjects(props.targetId)
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

/** 附件预览/下载：统一使用时效签名URL，禁止永久直链 */
const openFile = async (file: LifecycleAttachmentVO, download = false) => {
  if (!file.fileKey) {
    ElMessage.warning('该附件无实际文件，无法预览/下载')
    return
  }
  const res = await getPresignUrl({
    bucket: file.bucket || 'archive-formal',
    fileKey: file.fileKey,
    fileName: file.fileName || '',
    download
  })
  window.open(res.data, '_blank')
}

/** 附件列表弹窗 */
const attachmentDialogVisible = ref(false)
const attachmentDialogTitle = ref('附件')
const attachmentDialogFiles = ref<LifecycleAttachmentVO[]>([])
/** 弹窗内附件类型使用的字典类型（按阶段区分） */
const attachmentDialogDictType = ref('')
const openAttachments = (title: string, files?: LifecycleAttachmentVO[], dictType = '') => {
  attachmentDialogTitle.value = title
  attachmentDialogFiles.value = files || []
  attachmentDialogDictType.value = dictType
  attachmentDialogVisible.value = true
}

/** 金额格式化 */
const formatMoney = (value?: number | null) => {
  if (value == null) return '—'
  return '¥' + Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/** 申报状态文案：已立项优先，其次明细审核状态，最后申报单状态 */
const DECLARE_STATUS_LABEL: Record<string, string> = {
  unsubmitted: '未提交',
  pending: '待审核',
  approved: '已通过',
  rejected: '已驳回',
  draft: '草稿',
  submitted: '待审核'
}
const declareStatusLabel = (row: ProjectLifecycleVO) => {
  if (row.detailAuditStatus === 'established') return '已立项'
  const key = row.detailAuditStatus || row.declarationStatus
  if (!key) return '—'
  return DECLARE_STATUS_LABEL[key] || key
}
const declareStatusTagType = (row: ProjectLifecycleVO): 'primary' | 'success' | 'warning' | 'danger' | 'info' => {
  if (row.detailAuditStatus === 'established') return 'primary'
  const key = row.detailAuditStatus || row.declarationStatus
  if (key === 'approved') return 'success'
  if (key === 'pending' || key === 'submitted') return 'warning'
  if (key === 'rejected') return 'danger'
  return 'info'
}

/** 立项状态文案 */
const approvalStatusLabel = (status?: string) => {
  if (!status) return '—'
  if (status === 'PENDING') return '待审核'
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已驳回'
  return status
}

/** 施工状态文案（原始状态值） */
const buildStatusText = (status?: string) => {
  if (!status) return '—'
  if (status === 'NOT_STARTED') return '未开工'
  if (status === 'IN_PROGRESS') return '施工中'
  if (status === 'COMPLETED') return '已完工'
  return status
}

/** 施工状态文案：已立项但无施工记录的明细视为未开工 */
const buildStatusLabel = (row: ProjectLifecycleVO) => {
  if (!row.detailId) return '—'
  return buildStatusText(row.buildStatus || 'NOT_STARTED')
}

/** 验收状态文案（原始状态值） */
const acceptStatusText = (status?: string) => {
  if (!status) return '—'
  if (status === 'PENDING') return '待验收'
  if (status === 'ACCEPTED') return '已验收'
  if (status === 'REJECTED') return '验收未通过'
  return status
}

/** 验收状态文案：已立项但无验收记录的明细视为待验收 */
const acceptStatusLabel = (row: ProjectLifecycleVO) => {
  if (!row.detailId) return '—'
  return acceptStatusText(row.acceptStatus || 'PENDING')
}

/** 巡检结果文案 */
const inspectionLabel = (status?: string) => {
  if (!status) return '—'
  if (status === 'PASS') return '合格'
  if (status === 'FAIL') return '不合格'
  return status
}

/** 申报明细审核状态文案 */
const detailAuditStatusText = (status?: string) => {
  if (!status) return '—'
  const map: Record<string, string> = {
    unsubmitted: '未提交',
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回',
    established: '已立项'
  }
  return map[status] || status
}

/** 申报日志动作文案（与后端 actionLabel 保持一致） */
const auditActionLabel = (action?: string) => {
  if (!action) return '—'
  const map: Record<string, string> = {
    AUDIT_PASS: '审核通过',
    AUDIT_REJECT: '审核驳回',
    AUDIT_REVOKE: '审核撤销',
    SUBMIT_FIRST: '首次提交审核',
    SUBMIT_RESUBMITTED: '重新提交审核',
    SUBMIT_UNSUBMITTED: '重新提交审核',
    USER_EDIT: '用户修改明细'
  }
  return map[action] || action
}

/** 申报日志状态流转文案 */
const auditStatusFlow = (record: DeclarationAuditLogVO) => {
  const before = detailAuditStatusText(record.beforeAuditStatus)
  const after = detailAuditStatusText(record.afterAuditStatus)
  return before === after ? after : `${before} → ${after}`
}

onMounted(() => {
  loadDicts()
  load()
})

watch(() => props.targetId, load)
</script>

<style scoped>
.project-lifecycle {
  width: 100%;
}
/* 列表上方筛选栏 */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.filter-label {
  font-size: 13px;
  color: #606266;
}
/* 固定筛选框宽度，避免被弹性布局拉满 */
.filter-bar :deep(.approval-date-filter.el-date-editor) {
  width: 230px;
  flex: 0 0 230px;
}
/* 展开行内容：按阶段分区 */
.lifecycle-detail {
  padding: 4px 12px 8px;
}
.detail-section + .detail-section {
  margin-top: 16px;
}
.detail-section-title {
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
/* 阶段内子标题（附件 / 记录） */
.stage-subtitle {
  margin: 12px 0 6px;
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}
/* 附件行 */
.attachment-lines {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.attachment-line {
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 22px;
  font-size: 12px;
}
.attachment-name {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}
.attachment-download {
  flex: 0 0 auto;
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
}
.attachment-download:hover {
  color: #337ecc;
}
.attachment-remark {
  color: #909399;
  font-size: 12px;
}
/* 空状态提示 */
.empty-inline {
  color: #a8abb2;
  font-size: 12px;
  line-height: 24px;
}
</style>
