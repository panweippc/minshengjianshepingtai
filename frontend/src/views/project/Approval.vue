<template>
  <div class="approval-page">
    <!-- 筛选区 -->
    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="queryForm" class="query-form">
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
          <el-select v-model="queryForm.projectSource" placeholder="全部" clearable>
            <el-option
              v-for="item in dictOptions('xiang_mu_lei_xing')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="提交时间" class="query-item--date">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item class="query-item--actions">
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 页签 + 树形表格 -->
    <el-card shadow="never" class="list-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane name="pending">
          <template #label>待审核<span class="tab-count">({{ counts.pending }})</span></template>
        </el-tab-pane>
        <el-tab-pane name="rejected">
          <template #label>已驳回<span class="tab-count">({{ counts.rejected }})</span></template>
        </el-tab-pane>
      </el-tabs>

      <el-table
        v-loading="loading"
        :data="visibleRows"
        row-key="rowKey"
        border
        stripe
        class="approval-table"
        :row-class-name="rowClassName"
      >
        <el-table-column label="项目编号" width="180">
          <template #default="{ row }">
            <span
              v-if="row.hasChildren"
              class="tree-toggle"
              :class="{ 'tree-toggle--declare': row.nodeType === 'declare' }"
              :title="collapsedKeys.has(row.rowKey) ? '展开' : '收起'"
              @click.stop="toggleRow(row.rowKey)"
            >
              <el-icon :class="{ 'is-expanded': !collapsedKeys.has(row.rowKey) }">
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
              {{ row.code }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="项目名称" min-width="240" show-overflow-tooltip />
        <el-table-column prop="category" label="项目类别" width="140" />
        <el-table-column prop="source" label="项目类型" width="120" />
        <el-table-column label="项目预算（元）" width="150" align="right">
          <template #default="{ row }">{{ formatMoney(row.budget) }}</template>
        </el-table-column>
        <el-table-column prop="leader" label="负责人" width="100" />
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button v-if="canView" link type="primary" @click="openView(row)">查看</el-button>
              <template v-if="activeTab === 'pending' && row.nodeType === 'approval'">
                <el-button v-if="canEdit" link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button v-if="canApprove" link type="success" @click="handleApprove(row)">通过</el-button>
                <el-button v-if="canReject" link type="danger" @click="handleReject(row)">驳回</el-button>
              </template>
              <template v-if="activeTab === 'rejected' && row.nodeType === 'approval'">
                <el-button v-if="canReturn" link type="warning" @click="handleReturn(row)">退回</el-button>
              </template>
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

    <!-- 查看弹窗：项目信息 + 项目明细 / 申报信息 / 明细信息 -->
    <el-dialog v-model="viewVisible" :title="viewTitle" width="1000px" destroy-on-close class="approval-view-dialog">
      <!-- 一级：项目立项 / 项目申报 双页签 -->
      <template v-if="viewMode === 'approval' && viewApproval">
        <el-tabs v-model="viewTab">
          <el-tab-pane label="项目立项" name="info">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="项目编号">{{ viewApproval.approvalCode }}</el-descriptions-item>
              <el-descriptions-item label="项目名称">{{ viewApproval.projectName }}</el-descriptions-item>
              <el-descriptions-item label="项目类别">
                {{ dictLabel('xiang_mu_lei_bie', viewApproval.projectCategory) }}
              </el-descriptions-item>
              <el-descriptions-item label="项目类型">
                {{ dictLabel('xiang_mu_lei_xing', viewApproval.projectSource) }}
              </el-descriptions-item>
              <el-descriptions-item label="项目预算">{{ formatMoney(viewApproval.budget) }} 元</el-descriptions-item>
              <el-descriptions-item label="负责人">{{ viewApproval.leader || '-' }}</el-descriptions-item>
              <el-descriptions-item label="提交时间">{{ viewApproval.submitTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="审核状态">
                <el-tag :type="statusTagType(viewApproval.status)" size="small">{{ statusLabel(viewApproval.status) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ viewApproval.remark || '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-divider content-position="left">立项资料附件</el-divider>
            <el-empty v-if="!viewApprovalAttachments.length" description="暂无附件" :image-size="60" />
            <el-table v-else :data="viewApprovalAttachments" size="small" border>
              <el-table-column label="附件类型" width="150">
                <template #default="{ row }">
                  {{ dictLabel('li_xiang_fu_jian_lei_xing', row.attachmentType) || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="文件名称" min-width="220">
                <template #default="{ row }">
                  <div class="attachment-file-cell">
                    <span
                      class="attachment-file-name"
                      :title="row.fileName"
                      @click="openViewAttachment(row)"
                    >{{ row.fileName }}</span>
                    <el-icon
                      class="attachment-file-download"
                      title="下载"
                      @click.stop="openViewAttachment(row, true)"
                    >
                      <Download />
                    </el-icon>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
              <el-table-column prop="createTime" label="上传时间" width="170" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="项目申报" name="declare" class="declare-pane">
            <el-empty v-if="!viewDeclareTree.length" description="暂无申报明细" :image-size="80" />
            <el-table
              v-else
              :data="pagedDeclareTree"
              row-key="rowKey"
              :tree-props="{ children: 'children' }"
              default-expand-all
              border
              class="declare-tree-table"
              :row-class-name="rowClassName"
            >
              <el-table-column label="申报编号" width="160">
                <template #default="{ row }">
                  <!-- 农户/设施明细前显示对应类型图标，图标与编号强制同行 -->
                  <span class="detail-code-line" :class="{ 'detail-code-line--detail': row.nodeType === 'detail' }">
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
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="申报标题" min-width="190" show-overflow-tooltip>
                <template #default="{ row }">{{ row.title || '-' }}</template>
              </el-table-column>
              <el-table-column label="申报类型" width="110">
                <template #default="{ row }">
                  <template v-if="row.nodeType === 'declare'">
                    {{ dictLabel('shen_bao_lei_xing', row.declareType) || '-' }}
                  </template>
                  <template v-else>{{ detailTypeLabel(row.detailType) }}</template>
                </template>
              </el-table-column>
              <el-table-column label="申报类别" width="140">
                <template #default="{ row }">
                  <!-- 一级申报显示申报类别；农户/设施明细的事项说明同列展示 -->
                  <template v-if="row.nodeType === 'declare'">
                    {{ dictLabel('shen_bao_lei_bie', row.declareCategory) || '-' }}
                  </template>
                  <template v-else>{{ row.remark || '-' }}</template>
                </template>
              </el-table-column>
              <el-table-column label="申报金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column label="所属村组" width="100">
                <template #default="{ row }">{{ row.villageName || '-' }}</template>
              </el-table-column>
              <el-table-column label="申报材料附件" min-width="240">
                <template #default="{ row }">
                  <template v-if="row.nodeType === 'declare'">
                    <div v-if="row.attachments?.length">
                      <div v-for="file in row.attachments" :key="file.id" class="attachment-file-cell">
                        <!-- 显示附件类型 + 附件文件 -->
                        <span class="attachment-type-label">
                          {{ dictLabel('li_xiang_fu_jian_lei_xing', file.attachmentType) || '-' }}：
                        </span>
                        <span
                          class="attachment-file-name"
                          :title="file.fileName"
                          @click="openViewAttachment(file)"
                        >{{ file.fileName }}</span>
                        <el-icon
                          class="attachment-file-download"
                          title="下载"
                          @click.stop="openViewAttachment(file, true)"
                        >
                          <Download />
                        </el-icon>
                      </div>
                    </div>
                    <span v-else class="declare-no-attachment-text">暂无附件</span>
                  </template>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
            <!-- 项目申报分页：仅对一级申报节点分页，明细随父节点展开 -->
            <el-pagination
              v-if="viewDeclareTree.length"
              v-model:current-page="declarePageNum"
              v-model:page-size="declarePageSize"
              :page-sizes="[10, 20, 50]"
              :total="viewDeclareTree.length"
              layout="total, sizes, prev, pager, next"
              class="declare-pagination"
              @size-change="handleDeclareSizeChange"
            />
          </el-tab-pane>
        </el-tabs>
      </template>

      <!-- 二级：事项申报 / 申报明细 双页签 -->
      <template v-else-if="viewMode === 'declare' && viewDeclare">
        <el-tabs v-model="declareViewTab">
          <el-tab-pane label="事项申报" name="info">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="申报编号">{{ viewDeclare.declareCode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申报标题">{{ viewDeclare.declareName }}</el-descriptions-item>
              <el-descriptions-item label="申报类别">
                {{ dictLabel('shen_bao_lei_bie', viewDeclare.declareCategory) || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="申报类型">
                {{ dictLabel('shen_bao_lei_xing', viewDeclare.declareType) || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="申报金额">{{ formatMoney(viewDeclare.budget) }} 元</el-descriptions-item>
              <el-descriptions-item label="申报内容" :span="2">{{ viewDeclare.declareContent || '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-divider content-position="left">申报材料附件</el-divider>
            <el-empty v-if="!viewDeclare.attachments?.length" description="暂无申报材料附件" :image-size="60" />
            <el-table v-else :data="viewDeclare.attachments" size="small" border>
              <el-table-column label="附件类型" width="150">
                <template #default="{ row }">
                  {{ dictLabel('li_xiang_fu_jian_lei_xing', row.attachmentType) || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="文件名称" min-width="220">
                <template #default="{ row }">
                  <div class="attachment-file-cell">
                    <span
                      class="attachment-file-name"
                      :title="row.fileName"
                      @click="openViewAttachment(row)"
                    >{{ row.fileName }}</span>
                    <el-icon
                      class="attachment-file-download"
                      title="下载"
                      @click.stop="openViewAttachment(row, true)"
                    >
                      <Download />
                    </el-icon>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
              <el-table-column prop="createTime" label="上传时间" width="170" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="申报明细" name="detail">
            <el-table :data="pagedDeclareDetails" size="small" border>
              <el-table-column label="明细类型" width="110">
                <template #default="{ row }">{{ detailTypeLabel(row.detailType) }}</template>
              </el-table-column>
              <el-table-column prop="targetName" label="农户/设施名称" min-width="140" />
              <el-table-column prop="targetCode" label="户号/编码" width="140" />
              <el-table-column prop="regionName" label="所属区划" width="120" />
              <el-table-column label="金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="remark" label="事项说明" min-width="140" show-overflow-tooltip />
              <!-- 明细暂无独立附件数据，先占位展示 -->
              <el-table-column label="附件" width="120" align="center">
                <template #default>-</template>
              </el-table-column>
            </el-table>
            <!-- 申报明细分页 -->
            <el-pagination
              v-if="detailTotal"
              v-model:current-page="detailPageNum"
              v-model:page-size="detailPageSize"
              :page-sizes="[10, 20, 50]"
              :total="detailTotal"
              layout="total, sizes, prev, pager, next"
              class="declare-pagination"
              @size-change="handleDetailSizeChange"
            />
          </el-tab-pane>
        </el-tabs>
      </template>

      <!-- 三级：明细信息 -->
      <template v-else-if="viewMode === 'detail' && viewDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="明细类型">{{ detailTypeLabel(viewDetail.detailType) }}</el-descriptions-item>
          <el-descriptions-item label="农户/设施名称">{{ viewDetail.targetName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="户号/编码">{{ viewDetail.targetCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属区划">{{ viewDetail.regionName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申报金额">{{ formatMoney(viewDetail.amount) }} 元</el-descriptions-item>
          <el-descriptions-item label="事项说明">{{ viewDetail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗：项目信息 + 立项资料附件 -->
    <el-dialog v-model="editVisible" title="编辑立项" width="860px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目名称" prop="projectName">
              <el-input v-model="editForm.projectName" placeholder="请输入项目名称" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类别" prop="projectCategory">
              <el-select v-model="editForm.projectCategory" placeholder="请选择项目类别" style="width: 100%">
                <el-option
                  v-for="item in dictOptions('xiang_mu_lei_bie')"
                  :key="item.dictValue"
                  :label="item.dictLabel"
                  :value="item.dictValue"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目来源" prop="projectSource">
              <el-select v-model="editForm.projectSource" placeholder="请选择项目来源" style="width: 100%">
                <el-option
                  v-for="item in dictOptions('xiang_mu_lai_yuan')"
                  :key="item.dictValue"
                  :label="item.dictLabel"
                  :value="item.dictValue"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目预算" prop="budget">
              <el-input-number
                v-model="editForm.budget"
                :min="0"
                :precision="2"
                :controls="false"
                placeholder="请输入项目预算"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="负责人" prop="leader">
              <el-input v-model="editForm.leader" placeholder="请输入负责人" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="editForm.remark" placeholder="请输入备注" maxlength="512" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 立项资料附件：分隔线标题 + 卡片式多条录入 -->
        <div class="attachment-section">
          <el-divider content-position="left" class="attachment-divider">
            <span class="attachment-divider-title">立项资料附件</span>
            <el-button link type="primary" icon="Plus" @click="addAttachment">添加附件</el-button>
          </el-divider>

          <div v-if="!attachmentCards.length" class="attachment-empty">
            暂无附件，请点击上方「添加附件」
          </div>

          <div v-for="(card, idx) in attachmentCards" :key="idx" class="attachment-card">
            <div class="attachment-card-header">
              <span class="attachment-index">{{ idx + 1 }}</span>
              <span class="attachment-card-title">附件 #{{ idx + 1 }}</span>
              <el-button link type="danger" icon="Delete" @click="removeAttachment(idx)">删除</el-button>
            </div>
            <div class="attachment-card-body">
              <div class="attachment-field">
                <label class="attachment-field-label">附件类型</label>
                <el-select v-model="card.attachmentType" placeholder="请选择类型..." style="width: 100%">
                  <el-option
                    v-for="item in dictOptions('li_xiang_fu_jian_lei_xing')"
                    :key="item.dictValue"
                    :label="item.dictLabel"
                    :value="item.dictValue"
                  />
                </el-select>
              </div>
              <div class="attachment-field">
                <label class="attachment-field-label">上传附件</label>
                <el-upload
                  :auto-upload="false"
                  :show-file-list="false"
                  multiple
                  @change="(file: UploadFile) => handleAttachmentFileChange(file, card)"
                >
                  <div class="upload-trigger">
                    <el-icon><Upload /></el-icon>
                    <span>点击选择文件...</span>
                  </div>
                </el-upload>
                <div v-if="card.files.length" class="upload-file-list">
                  <div
                    v-for="(file, fIdx) in card.files"
                    :key="`${file.fileName}_${fIdx}`"
                    class="upload-file-item"
                  >
                    <el-icon class="upload-file-icon"><Document /></el-icon>
                    <span
                      class="upload-file-name upload-file-preview"
                      :title="file.fileName"
                      @click="openAttachmentFile(file)"
                    >{{ file.fileName }}</span>
                    <el-icon class="upload-file-download" title="下载" @click="openAttachmentFile(file, true)">
                      <Download />
                    </el-icon>
                    <el-icon class="upload-file-remove" @click="removeAttachmentFile(card, fIdx)">
                      <Close />
                    </el-icon>
                  </div>
                </div>
              </div>
              <div class="attachment-field">
                <label class="attachment-field-label">备注</label>
                <el-input v-model="card.remark" placeholder="选填" maxlength="256" />
              </div>
            </div>
          </div>
        </div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getDictByType } from '@/api/dict'
import type { DictVO } from '@/types/system'
import { uploadFile as uploadFileToServer, getPresignUrl } from '@/api/file'
import {
  getApprovalPage,
  getApprovalCounts,
  getApprovalAttachments,
  updateApproval,
  approveApproval,
  rejectApproval,
  returnApproval,
  type ApprovalVO,
  type ApprovalDeclareNode,
  type ApprovalDetailNode,
  type ApprovalAttachmentVO,
  type ApprovalEditDTO
} from '@/api/approval'

/**
 * 立项管理
 * 所属菜单：项目全周期管控 → 立项管理
 * 数据为三级树形：立项项目 → 申报内容 → 农户/公共基础设施明细
 */
const userStore = useUserStore()

const canView = computed(() => userStore.hasPerm('project:approval:view'))
const canEdit = computed(() => userStore.hasPerm('project:approval:edit'))
const canApprove = computed(() => userStore.hasPerm('project:approval:approve'))
const canReject = computed(() => userStore.hasPerm('project:approval:reject'))
const canReturn = computed(() => userStore.hasPerm('project:approval:return'))

/* ============ 字典 ============ */
const dictMap = ref<Record<string, DictVO[]>>({})
const DICT_TYPES = [
  'xiang_mu_lei_bie',
  'xiang_mu_lei_xing',
  'xiang_mu_lai_yuan',
  'li_xiang_fu_jian_lei_xing',
  'shen_bao_lei_bie',
  'shen_bao_lei_xing'
]
const dictOptions = (type: string): DictVO[] => dictMap.value[type] || []
const dictLabel = (type: string, value?: string): string => {
  if (!value) return ''
  const hit = dictOptions(type).find((item) => item.dictValue === value)
  return hit?.dictLabel || value
}
const detailTypeLabel = (value?: string): string => {
  if (value === 'HOUSEHOLD') return '农户'
  if (value === 'FACILITY') return '公共基础设施'
  return value || '-'
}

const loadDicts = async () => {
  const results = await Promise.all(DICT_TYPES.map((type) => getDictByType(type)))
  const map: Record<string, DictVO[]> = {}
  DICT_TYPES.forEach((type, index) => {
    map[type] = results[index].data || []
  })
  dictMap.value = map
}

/* ============ 筛选与列表 ============ */
type TabKey = 'pending' | 'rejected'

interface DisplayRow {
  rowKey: string
  nodeType: 'approval' | 'declare' | 'detail'
  /** 三级明细类型：HOUSEHOLD 农户 / FACILITY 公共基础设施 */
  detailType?: string
  code: string
  name: string
  category: string
  source: string
  budget?: number
  leader: string
  submitTime: string
  raw: ApprovalVO | ApprovalDeclareNode | ApprovalDetailNode
  children?: DisplayRow[]
  /** 是否含子节点，用于渲染展开箭头（表格数据传入前会剥离 children，避免 el-table 走树形模式） */
  hasChildren?: boolean
}

const activeTab = ref<TabKey>('pending')
const queryForm = reactive({
  projectName: '',
  projectCategory: '',
  /** 项目类型：沿用后端 projectSource 字段传参，字典为 xiang_mu_lei_xing */
  projectSource: ''
})
const dateRange = ref<[string, string] | null>(null)
const loading = ref(false)
const tableData = ref<DisplayRow[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
/** 三个页签的项目数量（按当前筛选条件） */
const counts = reactive<Record<'pending' | 'rejected', number>>({ pending: 0, rejected: 0 })

/** 已收起的节点集合（默认全部展开，与树形表格 default-expand-all 行为一致） */
const collapsedKeys = ref<Set<string>>(new Set())

/** 手写展开/收起，替代 el-table 树形模式，保证三个层级的缩进完全一致 */
const toggleRow = (key: string) => {
  const next = new Set(collapsedKeys.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  collapsedKeys.value = next
}

const visibleRows = computed(() => {
  const rows: DisplayRow[] = []
  const walk = (list: DisplayRow[]) => {
    list.forEach((row) => {
      // 剥离 children 再交给表格，避免 el-table 因检测到 children 自动进入树形模式
      rows.push({ ...row, children: undefined })
      if (row.children?.length && !collapsedKeys.value.has(row.rowKey)) {
        walk(row.children)
      }
    })
  }
  walk(tableData.value)
  return rows
})

/** 三级明细行加类名，便于单独调小字号 */
const rowClassName = ({ row }: { row: { nodeType: string } }): string =>
  row.nodeType === 'detail' ? 'approval-detail-row' : ''

const toDisplay = (approval: ApprovalVO): DisplayRow => ({
  rowKey: `a_${approval.id}`,
  nodeType: 'approval',
  code: approval.approvalCode,
  name: approval.projectName,
  category: dictLabel('xiang_mu_lei_bie', approval.projectCategory),
  source: dictLabel('xiang_mu_lei_xing', approval.projectSource),
  budget: approval.budget,
  leader: approval.leader || '',
  submitTime: approval.submitTime || '',
  raw: approval,
  hasChildren: (approval.children || []).length > 0,
  children: (approval.children || []).map((declare) => ({
    rowKey: `d_${declare.id}`,
    nodeType: 'declare',
    code: declare.declareCode || '',
    name: declare.declareName,
    category: dictLabel('shen_bao_lei_bie', declare.declareCategory),
    source: dictLabel('shen_bao_lei_xing', declare.declareType),
    budget: declare.budget,
    leader: '',
    submitTime: '',
    raw: declare,
    hasChildren: (declare.children || []).length > 0,
    children: [...(declare.children || [])]
      // 三级明细按类型分组：农户在前、公共基础设施在后
      .sort((a, b) => (a.detailType === 'FACILITY' ? 1 : 0) - (b.detailType === 'FACILITY' ? 1 : 0))
      .map((detail) => ({
        rowKey: `t_${detail.id}`,
        nodeType: 'detail',
        detailType: detail.detailType,
        code: detail.targetCode || '',
        name: detail.targetName || '',
        category: detailTypeLabel(detail.detailType),
        source: detail.regionName || '',
        budget: detail.amount,
        leader: '',
        submitTime: '',
        raw: detail
      }))
  }))
})

const loadData = async () => {
  loading.value = true
  // 同时刷新页签计数（不依赖当前页签，共享相同筛选条件）
  loadCounts()
  try {
    const res = await getApprovalPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      tab: activeTab.value,
      projectName: queryForm.projectName || undefined,
      projectCategory: queryForm.projectCategory || undefined,
      projectSource: queryForm.projectSource || undefined,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1]
    })
    const records = res.data.records || []
    tableData.value = records.map(toDisplay)
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

/** 按当前筛选条件刷新三个页签的数量（静默失败，不阻塞主流程） */
const loadCounts = async () => {
  try {
    const res = await getApprovalCounts({
      projectName: queryForm.projectName || undefined,
      projectCategory: queryForm.projectCategory || undefined,
      projectSource: queryForm.projectSource || undefined,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1]
    })
    if (res.data) {
      counts.pending = res.data.pending ?? 0
      counts.rejected = res.data.rejected ?? 0
    }
  } catch {
    // 计数失败不影响主表格展示
  }
}

const handleQuery = () => {
  pageNum.value = 1
  loadData()
}

const handleReset = () => {
  queryForm.projectName = ''
  queryForm.projectCategory = ''
  queryForm.projectSource = ''
  dateRange.value = null
  pageNum.value = 1
  loadData()
}

const handleTabChange = () => {
  pageNum.value = 1
  loadData()
}

/* ============ 状态与格式化 ============ */
const formatMoney = (value?: number): string => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const statusLabel = (status?: string): string => {
  if (status === 'PENDING') return '待审核'
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已驳回'
  return status || '-'
}

const statusTagType = (status?: string): 'success' | 'warning' | 'danger' | 'info' => {
  if (status === 'APPROVED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

/* ============ 查看弹窗 ============ */
type ViewMode = 'approval' | 'declare' | 'detail'
const viewVisible = ref(false)
const viewMode = ref<ViewMode>('approval')
/** 立项查看弹窗的页签：info 项目立项 / declare 项目申报 */
const viewTab = ref<'info' | 'declare'>('info')
/** 申报查看弹窗的页签：info 事项申报 / detail 申报明细 */
const declareViewTab = ref<'info' | 'detail'>('info')
const viewApproval = ref<ApprovalVO | null>(null)
const viewDeclare = ref<ApprovalDeclareNode | null>(null)
const viewDetail = ref<ApprovalDetailNode | null>(null)
/** 查看弹窗中展示的立项资料附件（编辑保存后需同步可见） */
const viewAttachments = ref<ApprovalAttachmentVO[]>([])

/** 立项资料附件：仅 declareId 为空的整单级附件，申报材料附件在各自申报块中展示 */
const viewApprovalAttachments = computed(() => viewAttachments.value.filter((item) => !item.declareId))

/** 项目明细树形行：一级申报，二级农户/设施明细 */
interface DeclareTreeRow {
  rowKey: string
  nodeType: 'declare' | 'detail'
  code: string
  title: string
  declareType?: string
  declareCategory?: string
  detailType?: string
  /** 申报金额：申报节点取预算，明细节点取金额 */
  amount?: number
  townName?: string
  villageName?: string
  remark?: string
  attachments?: ApprovalAttachmentVO[]
  children?: DeclareTreeRow[]
}

/** 查看弹窗的项目明细树：申报为一级节点，农户/设施明细为二级节点 */
const viewDeclareTree = computed<DeclareTreeRow[]>(() =>
  (viewApproval.value?.children || []).map((declare) => ({
    rowKey: `d_${declare.id}`,
    nodeType: 'declare' as const,
    code: declare.declareCode || '',
    title: declare.declareName,
    declareType: declare.declareType,
    declareCategory: declare.declareCategory,
    amount: declare.budget,
    townName: declare.townName,
    villageName: declare.villageName,
    attachments: declare.attachments || [],
    children: (declare.children || []).map((detail) => ({
      rowKey: `t_${detail.id}`,
      nodeType: 'detail' as const,
      code: detail.targetCode || '',
      title: detail.targetName || '',
      detailType: detail.detailType,
      amount: detail.amount,
      villageName: detail.regionName,
      remark: detail.remark || ''
    }))
  }))
)

/** 项目申报页签分页状态 */
const declarePageNum = ref(1)
const declarePageSize = ref(10)
/** 当前页的一级申报节点，明细随父节点一起展示 */
const pagedDeclareTree = computed<DeclareTreeRow[]>(() => {
  const start = (declarePageNum.value - 1) * declarePageSize.value
  return viewDeclareTree.value.slice(start, start + declarePageSize.value)
})

const handleDeclareSizeChange = () => {
  declarePageNum.value = 1
}

/** 申报查看弹窗「申报明细」页签分页状态 */
const detailPageNum = ref(1)
const detailPageSize = ref(10)
const detailTotal = computed(() => viewDeclare.value?.children?.length || 0)
const pagedDeclareDetails = computed<ApprovalDetailNode[]>(() => {
  const start = (detailPageNum.value - 1) * detailPageSize.value
  return (viewDeclare.value?.children || []).slice(start, start + detailPageSize.value)
})

const handleDetailSizeChange = () => {
  detailPageNum.value = 1
}

const viewTitle = computed(() => {
  if (viewMode.value === 'declare') return '申报详情'
  if (viewMode.value === 'detail') return '明细详情'
  return '项目详情'
})

const openView = async (row: DisplayRow) => {
  viewMode.value = row.nodeType
  viewTab.value = 'info'
  declareViewTab.value = 'info'
  declarePageNum.value = 1
  detailPageNum.value = 1
  viewApproval.value = null
  viewDeclare.value = null
  viewDetail.value = null
  viewAttachments.value = []
  if (row.nodeType === 'approval') {
    viewApproval.value = row.raw as ApprovalVO
  } else if (row.nodeType === 'declare') {
    viewDeclare.value = row.raw as ApprovalDeclareNode
  } else {
    viewDetail.value = row.raw as ApprovalDetailNode
  }
  viewVisible.value = true
  // 立项维度的查看需要同步加载附件，保证编辑上传后在查看弹窗中可见
  if (row.nodeType === 'approval' && viewApproval.value) {
    const res = await getApprovalAttachments(viewApproval.value.id)
    viewAttachments.value = res.data || []
  }
}

/** 查看弹窗内预览/下载附件：复用时效签名URL逻辑 */
const openViewAttachment = (row: ApprovalAttachmentVO, download = false) => {
  openAttachmentFile({ fileName: row.fileName || '', fileKey: row.fileKey, bucket: row.bucket }, download)
}

/* ============ 编辑弹窗 ============ */
interface AttachmentFileItem {
  id?: string
  fileName: string
  fileKey?: string
  bucket?: string
  fileSize?: number
}

interface AttachmentEditCard {
  attachmentType?: string
  remark?: string
  files: AttachmentFileItem[]
}

const editVisible = ref(false)
const submitLoading = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: '',
  projectName: '',
  projectCategory: '',
  projectSource: '',
  budget: undefined as number | undefined,
  leader: '',
  remark: ''
})
const attachmentCards = ref<AttachmentEditCard[]>([])

const editRules: FormRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }]
}

const groupAttachmentsToCards = (list: ApprovalAttachmentVO[]): AttachmentEditCard[] => {
  const cards: AttachmentEditCard[] = []
  const index = new Map<string, AttachmentEditCard>()
  for (const item of list) {
    const key = `${item.attachmentType ?? ''}|${item.remark ?? ''}`
    let card = index.get(key)
    if (!card) {
      card = { attachmentType: item.attachmentType ?? '', remark: item.remark ?? '', files: [] }
      index.set(key, card)
      cards.push(card)
    }
    if (item.fileName) {
      card.files.push({
        id: item.id,
        fileName: item.fileName,
        fileKey: item.fileKey,
        bucket: item.bucket,
        fileSize: item.fileSize
      })
    }
  }
  return cards
}

const openEdit = async (row: DisplayRow) => {
  const approval = row.raw as ApprovalVO
  Object.assign(editForm, {
    id: approval.id,
    projectName: approval.projectName,
    projectCategory: approval.projectCategory ?? '',
    projectSource: approval.projectSource ?? '',
    budget: approval.budget,
    leader: approval.leader ?? '',
    remark: approval.remark ?? ''
  })
  attachmentCards.value = []
  const res = await getApprovalAttachments(approval.id)
  attachmentCards.value = groupAttachmentsToCards(res.data || [])
  editVisible.value = true
}

const addAttachment = () => {
  attachmentCards.value.push({ attachmentType: '', remark: '', files: [] })
}

const removeAttachment = (idx: number) => {
  attachmentCards.value.splice(idx, 1)
}

/** 选择附件文件：支持多选，按文件名+大小去重；选中后立即上传到 MinIO 正式归档桶 */
const handleAttachmentFileChange = async (uploadFile: UploadFile, card: AttachmentEditCard) => {
  const raw = uploadFile.raw
  if (!raw) return
  const duplicated = card.files.some((file) => file.fileName === raw.name && file.fileSize === raw.size)
  if (duplicated) return
  try {
    const res = await uploadFileToServer(raw)
    card.files.push({
      fileName: res.data.fileName,
      fileKey: res.data.fileKey,
      bucket: res.data.bucket,
      fileSize: res.data.fileSize
    })
  } catch {
    // 上传失败提示由 axios 拦截器统一处理
  }
}

const removeAttachmentFile = (card: AttachmentEditCard, idx: number) => {
  card.files.splice(idx, 1)
}

/** 预览/下载附件：通过时效签名URL在新标签页打开 */
const openAttachmentFile = async (file: AttachmentFileItem, download = false) => {
  if (!file.fileKey || !file.bucket) {
    ElMessage.warning('该附件无实际文件，无法预览/下载')
    return
  }
  const res = await getPresignUrl({
    bucket: file.bucket,
    fileKey: file.fileKey,
    fileName: file.fileName,
    download
  })
  window.open(res.data, '_blank')
}

const handleSubmit = async () => {
  if (!editFormRef.value) return
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  const attachments = attachmentCards.value.flatMap((card) =>
    card.files.map((file) => ({
      id: file.id,
      attachmentType: card.attachmentType,
      fileName: file.fileName,
      fileKey: file.fileKey,
      bucket: file.bucket,
      fileSize: file.fileSize,
      remark: card.remark
    }))
  )
  const payload: ApprovalEditDTO = {
    id: editForm.id,
    projectName: editForm.projectName,
    projectCategory: editForm.projectCategory,
    projectSource: editForm.projectSource,
    budget: editForm.budget,
    leader: editForm.leader,
    remark: editForm.remark,
    attachments
  }
  submitLoading.value = true
  try {
    await updateApproval(payload)
    ElMessage.success('保存成功')
    editVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

/* ============ 通过 / 驳回 / 退回 ============ */
const handleApprove = async (row: DisplayRow) => {
  const approval = row.raw as ApprovalVO
  await ElMessageBox.confirm(`确认通过「${approval.projectName}」？`, '审核通过', { type: 'warning' })
  await approveApproval(approval.id)
  ElMessage.success('审核通过')
  loadData()
}

const handleReject = async (row: DisplayRow) => {
  const approval = row.raw as ApprovalVO
  await ElMessageBox.confirm(`确认驳回「${approval.projectName}」？`, '审核驳回', { type: 'warning' })
  await rejectApproval(approval.id)
  ElMessage.success('已驳回')
  loadData()
}

const handleReturn = async (row: DisplayRow) => {
  const approval = row.raw as ApprovalVO
  await ElMessageBox.confirm(`确认将「${approval.projectName}」退回至待审核？`, '退回', { type: 'warning' })
  await returnApproval(approval.id)
  ElMessage.success('已退回待审核')
  loadData()
}

onMounted(async () => {
  await loadDicts()
  await loadData()
})
</script>

<style scoped>
.approval-page {
  /* 顶部不再留白，筛选卡片紧贴内容区上沿 */
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
.query-form :deep(.el-select),
.query-form :deep(.el-date-editor) {
  width: 100%;
}

/* 日期范围选择器需要更宽的展示空间 */
.query-form :deep(.query-item--date) {
  flex: 1.6 1 0;
}

/* 操作按钮组不参与拉伸 */
.query-form :deep(.query-item--actions) {
  flex: 0 0 auto;
  margin-right: 0;
}

.approval-table {
  margin-top: 4px;
}

/* 表头首列与层级文字起点对齐（单元格 12px + 展开按钮 20px） */
.approval-table :deep(.el-table__header th:first-child .cell) {
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
  /* 与 el-button type="primary" 的链接按钮保持同色 */
  color: var(--el-color-primary, #409eff);
  /* 二级行的箭头与文字整体右移，形成层级错位 */
  margin-left: 15px;
}

/* 三级行的箭头保持与二级同样的基准偏移，保证层级相对缩进不变 */
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

/* 三级明细（农户/设施）在二级基础上向右错位展示 */
.tree-code--detail {
  padding-left: 6px;
}

/* 三级明细行文字字号略小于一级、二级 */
.approval-table :deep(.approval-detail-row) {
  font-size: 12px;
}

/* 查看弹窗中的明细行（表格为 small 尺寸）字号再小一号 */
.declare-tree-table :deep(.approval-detail-row) {
  font-size: 11px;
}

/* 查看弹窗「项目申报」页签分页器 */
.declare-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}

/* 查看弹窗「项目申报」页签：表头字体黑色 + 明细行/图标字号提升 */
:deep(.declare-pane) .declare-tree-table .el-table__header th .cell {
  color: #000;
}
:deep(.declare-pane) .declare-tree-table :deep(.approval-detail-row) {
  font-size: 13px;
}
:deep(.declare-pane) .declare-tree-table .detail-type-icon {
  font-size: 13px;
  width: 20px;
  height: 20px;
}
/* 附件区域字体保持小一号，避免多附件时占高过多 */
:deep(.declare-pane) .attachment-file-cell,
:deep(.declare-pane) .attachment-type-label,
:deep(.declare-pane) .attachment-file-name,
:deep(.declare-pane) .declare-no-attachment-text {
  font-size: 11px;
}
:deep(.declare-pane) .attachment-file-download {
  font-size: 11px;
}

/* 明细编号行：图标与编号同一行显示，不换行 */
.detail-code-line {
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  white-space: nowrap;
  vertical-align: middle;
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

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.declare-block {
  margin-bottom: 16px;
}

.declare-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.declare-desc {
  margin-bottom: 8px;
}

/* 附件录入区 */
.attachment-section {
  margin-top: 8px;
}

.attachment-divider {
  margin: 16px 0 12px;
}

.attachment-divider :deep(.el-divider__text) {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.attachment-divider-title {
  font-weight: 600;
  color: #303133;
}

.attachment-empty {
  padding: 20px;
  text-align: center;
  color: #909399;
  background: #f5f7fa;
  border-radius: 4px;
}

.attachment-card {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 12px;
}

.attachment-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #f0f2f5;
}

.attachment-index {
  width: 20px;
  height: 20px;
  line-height: 20px;
  text-align: center;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
}

.attachment-card-title {
  flex: 1;
  font-weight: 500;
}

.attachment-card-body {
  display: grid;
  /* 三列等宽，与参考样式保持一致；minmax(0,1fr) 防止表单控件撑破弹窗宽度 */
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
  padding: 12px;
}

/* 上传组件默认 inline-block，需撑满所在列才能与下拉、输入框对齐 */
.attachment-field :deep(.el-upload) {
  display: block;
  width: 100%;
}

.attachment-field-label {
  display: block;
  margin-bottom: 6px;
  color: #606266;
  font-size: 13px;
}

.upload-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 32px;
  box-sizing: border-box;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #909399;
  cursor: pointer;
}

.upload-trigger:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-file-list {
  margin-top: 6px;
}

/* 已上传文件行：浅灰底 + 细边框，与参考样式一致 */
.upload-file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 30px;
  padding: 0 8px;
  margin-bottom: 6px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fafafa;
  font-size: 13px;
  box-sizing: border-box;
}

.upload-file-icon {
  color: #909399;
  font-size: 14px;
}

.upload-file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
}

.upload-file-preview {
  cursor: pointer;
}

.upload-file-preview:hover {
  color: #409eff;
}

.upload-file-download,
.upload-file-remove {
  cursor: pointer;
  color: #909399;
}

.upload-file-download {
  color: #409eff;
}

.upload-file-remove:hover {
  color: #f56c6c;
}

/* 查看弹窗：文件名点击预览 + 后置下载箭头 */
.attachment-file-cell {
  display: flex;
  align-items: center;
  min-width: 0;
}

/* 申报材料附件为空时的提示 */
.declare-no-attachment-text {
  color: #909399;
  font-size: 12px;
}

/* 附件类型标签：显示在文件名前，避免挤压文件名 */
.attachment-type-label {
  flex: none;
  color: #606266;
}

.attachment-file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #409eff;
  cursor: pointer;
}

.attachment-file-name:hover {
  text-decoration: underline;
}

.attachment-file-download {
  flex: none;
  margin-left: 4px;
  font-size: 14px;
  color: #409eff;
  cursor: pointer;
}

.attachment-file-download:hover {
  color: #337ecc;
}

/* 页签右侧的数量徽标 */
.tab-count {
  margin-left: 2px;
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}
</style>

<style>
/* 查看弹窗固定高度：弹窗被 teleport 到 body，需使用非 scoped 样式；
   内容区独立滚动，避免树形展开/收起导致弹窗高度跳动 */
.approval-view-dialog {
  height: 80vh;
  margin-top: 8vh;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
}

.approval-view-dialog .el-dialog__header,
.approval-view-dialog .el-dialog__footer {
  flex: none;
}

.approval-view-dialog .el-dialog__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
</style>
