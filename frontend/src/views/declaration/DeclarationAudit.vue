<template>
  <div class="declaration-page">
    <!-- 筛选区（一行） -->
    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="申报标题">
          <el-input v-model="query.title" placeholder="请输入标题" clearable @keyup.enter="handleQuery" style="width: 200px" />
        </el-form-item>
        <el-form-item label="申报类型">
          <el-select v-model="query.declarationType" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="d in declarationTypeOpts" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="申报类别">
          <el-select v-model="query.declarationCategory" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="d in declarationCategoryOpts" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 视角切换 + 状态页签 + 新建按钮 -->
    <el-card shadow="never" class="list-card">
      <div class="tab-bar">
        <el-tabs v-model="activeStatusTab" @tab-change="onStatusTabChange" class="status-tabs">
          <el-tab-pane
            v-for="t in statusTabs"
            :key="t.name"
            :label="t.label"
            :name="t.name"
          />
        </el-tabs>
        <div class="tab-actions">
          <el-button
            v-if="activeStatusTab === 'approved'"
            type="primary"
            icon="Promotion"
            :disabled="selectedDeclarationIds.length === 0"
            @click="openApprovalDialog"
          >新建立项{{ selectedDeclarationIds.length > 0 ? `（已选 ${selectedDeclarationIds.length}）` : '' }}</el-button>
        </div>
      </div>
      <el-table
        v-loading="loading"
        :data="treeTableData"
        border stripe
        row-key="_rowKey"
        :tree-props="{ children: '_children', hasChildren: '_hasChildren' }"
        default-expand-all
        :span-method="detailMergeMethod"
        :row-class-name="isDetailRowClass"
        @selection-change="onSelectionChange"
      >
        <!-- selection：只在已通过页签生效，且只选父申报单行 -->
        <el-table-column v-if="activeStatusTab === 'approved'" type="selection" width="45" :selectable="isSelectableRow" />

        <!-- 第 0 列：树形展开箭头列（空 label），Element Plus 自动把箭头放这 -->
        <el-table-column width="40" label="">
          <template #default="{ row }">
            <template v-if="row._isDetail">
              <!-- 子明细行：不展示展开箭头前缀文字，缩进交给 Element Plus 树形 -->
            </template>
          </template>
        </el-table-column>

        <!-- 第 1 列：申报编号（父行）/ 明细行合并内容（子行） -->
        <el-table-column label="申报编号" min-width="400">
          <template #default="{ row }">
            <template v-if="!row._isDetail">{{ row.businessCode }}</template>
            <template v-else-if="row._detailKind === 'household'">
              <span class="detail-line merged">
                <span class="detail-inline-label">户主姓名：</span><span>{{ row.snapshotHeadName || '—' }}</span>
                <span class="sep">｜</span>
                <span class="detail-inline-label">联系电话：</span><span>{{ maskPhone(row.snapshotContactPhone) }}</span>
                <span class="sep">｜</span>
                <span class="detail-inline-label">户类型：</span><span>{{ dictLabel('hu_lei_xing', row.snapshotHouseholdType) }}</span>
                <span class="sep">｜</span>
                <span class="detail-inline-label">单户申报金额：</span>
                <span class="detail-money" v-if="row.amount != null">¥ {{ formatAmount(row.amount) }}</span>
                <span v-else class="detail-empty">—</span>
                <span class="sep">｜</span>
                <span class="detail-inline-label">审核状态：</span>
                <el-tag v-if="row.auditStatus === 'pending' || !row.auditStatus" type="warning" size="small">待审核</el-tag>
                <el-tag v-else-if="row.auditStatus === 'approved'" type="success" size="small">已通过</el-tag>
                <el-tag v-else-if="row.auditStatus === 'rejected'" type="danger" size="small">已驳回</el-tag>
                <el-tag v-else-if="row.auditStatus === 'established'" type="primary" size="small">已立项</el-tag>
                <el-tag v-else size="small">{{ row.auditStatus || '—' }}</el-tag>
              </span>
            </template>
            <template v-else>
              <span class="detail-line merged">
                <span class="detail-inline-label">设施名称：</span><span>{{ row.snapshotFacilityName || '—' }}</span>
                <span class="sep">｜</span>
                <span class="detail-inline-label">设施类型：</span><span>{{ dictLabel('she_shi_lei_xing', row.snapshotFacilityType) }}</span>
                <span class="sep">｜</span>
                <span class="detail-inline-label">所在位置：</span><span>{{ row.snapshotLocation || '—' }}</span>
                <span class="sep">｜</span>
                <span class="detail-inline-label">单设施申报金额：</span>
                <span class="detail-money" v-if="row.amount != null">¥ {{ formatAmount(row.amount) }}</span>
                <span v-else class="detail-empty">—</span>
                <span class="sep">｜</span>
                <span class="detail-inline-label">审核状态：</span>
                <el-tag v-if="row.auditStatus === 'pending' || !row.auditStatus" type="warning" size="small">待审核</el-tag>
                <el-tag v-else-if="row.auditStatus === 'approved'" type="success" size="small">已通过</el-tag>
                <el-tag v-else-if="row.auditStatus === 'rejected'" type="danger" size="small">已驳回</el-tag>
                <el-tag v-else-if="row.auditStatus === 'established'" type="primary" size="small">已立项</el-tag>
                <el-tag v-else size="small">{{ row.auditStatus || '—' }}</el-tag>
              </span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="申报标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ !row._isDetail ? row.title : '' }}</template>
        </el-table-column>
        <el-table-column label="申报日期" width="120">
          <template #default="{ row }">{{ !row._isDetail ? (row.declarationDate || '') : '' }}</template>
        </el-table-column>
        <el-table-column label="所属乡镇" width="120">
          <template #default="{ row }">{{ !row._isDetail ? row.townName : '' }}</template>
        </el-table-column>
        <el-table-column label="所属村组" width="130">
          <template #default="{ row }">{{ !row._isDetail ? row.villageGroupName : '' }}</template>
        </el-table-column>
        <el-table-column label="申报类型" width="110">
          <template #default="{ row }">{{ !row._isDetail ? dictLabel('shen_bao_lei_xing', row.declarationType) : '' }}</template>
        </el-table-column>
        <el-table-column label="申报类别" width="100">
          <template #default="{ row }">{{ !row._isDetail ? dictLabel('shen_bao_lei_bie', row.declarationCategory) : '' }}</template>
        </el-table-column>
        <el-table-column label="申报金额" width="140" align="right">
          <template #default="{ row }">{{ !row._isDetail ? '¥ ' + row.amount?.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ !row._isDetail ? row.createTime : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <!-- ===== 父行：申报单只允许查看 ===== -->
            <template v-if="!row._isDetail">
              <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            </template>
            <!-- ===== 子行：农户/公建设施明细，按 auditStatus 分流 ===== -->
            <template v-else>
              <!-- 查看按钮 -->
              <el-button
                v-if="row._detailKind === 'household'"
                link type="primary" size="small" @click="openHouseholdDetailView(row)"
              >查看</el-button>
              <el-button
                v-else
                link type="primary" size="small" @click="openFacilityDetailView(row)"
              >查看</el-button>

              <!-- 待审核（pending/空）→ 通过 + 驳回 -->
              <template v-if="row.auditStatus === 'pending' || !row.auditStatus">
                <el-button link type="success" size="small" @click="openDetailAuditDialog(row, 'approve')">通过</el-button>
                <el-button link type="danger" size="small" @click="openDetailAuditDialog(row, 'reject')">驳回</el-button>
              </template>
              <!-- 已通过且未立项 → 撤销（收回审核结论，回到待审核重审） -->
              <template v-else-if="row.auditStatus === 'approved'">
                <el-button link type="warning" size="small" @click="openRevertDialog(row)">撤销</el-button>
              </template>
              <!-- 已驳回 → 撤销（收回驳回结论，回到待审核重审） -->
              <template v-else-if="row.auditStatus === 'rejected'">
                <el-button link type="warning" size="small" @click="openRevertDialog(row)">撤销</el-button>
              </template>
            </template>
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
      />
    </el-card>

    <!-- ========== 详情 弹窗（审核页只读） ========== -->
    <el-dialog
      v-model="dialogVisible"
      title="申报详情"
      width="880px"
      destroy-on-close
      append-to-body
    >
      <div class="detail-mode">
        <el-tabs v-model="detailTab">
          <!-- ============ Tab 1：基本信息 ============ -->
          <el-tab-pane label="基本信息" name="info">
            <div class="info-card-header"><span>基础信息</span></div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="申报编号">{{ detailData.businessCode || '—' }}</el-descriptions-item>
              <el-descriptions-item label="申报日期">{{ detailData.declarationDate || '—' }}</el-descriptions-item>
              <el-descriptions-item label="申报标题" :span="2">{{ detailData.title || '—' }}</el-descriptions-item>
              <el-descriptions-item label="所属乡镇">{{ detailData.townName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="所属村组">{{ detailData.villageGroupName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="申报类型">{{ dictLabel('shen_bao_lei_xing', detailData.declarationType) }}</el-descriptions-item>
              <el-descriptions-item label="申报类别">{{ dictLabel('shen_bao_lei_bie', detailData.declarationCategory) }}</el-descriptions-item>
              <el-descriptions-item label="申报金额">¥ {{ formatAmount(detailData.amount) }}</el-descriptions-item>
              <el-descriptions-item label="备注说明" :span="2">{{ detailData.remark || '—' }}</el-descriptions-item>
              <el-descriptions-item label="附件" :span="2">
                <el-button v-if="detailData.attachments?.length" link type="primary" @click="openAttachmentDialog">附件（{{ detailData.attachments.length }}）</el-button>
                <span v-else class="attachment-empty">无</span>
              </el-descriptions-item>
            </el-descriptions>

            <div v-if="detailData.declarationCategory === '1'" class="info-card-header"><span>申报单明细（{{ detailData.households?.length || 0 }}）</span></div>
            <el-table v-if="detailData.declarationCategory === '1'" :data="detailData.households || []" border size="small" empty-text="暂无农户">
              <el-table-column label="户主" min-width="80" prop="snapshotHeadName" />
              <el-table-column label="户号" min-width="140" prop="snapshotHouseholdNo" />
              <el-table-column label="身份证号" min-width="160" prop="snapshotHeadIdCard" />
              <el-table-column label="联系电话" min-width="120" prop="snapshotContactPhone" />
              <el-table-column label="户类型" min-width="100">
                <template #default="{ row }">{{ dictLabel('hu_lei_xing', row.snapshotHouseholdType) }}</template>
              </el-table-column>
              <el-table-column label="村组" min-width="110" prop="snapshotRegionName" />
              <el-table-column label="事项说明" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ row.remark || '—' }}</template>
              </el-table-column>
              <el-table-column label="申报金额" min-width="110" align="right">
                <template #default="{ row }">{{ row.amount != null ? '¥ ' + formatAmount(row.amount) : '—' }}</template>
              </el-table-column>
              <el-table-column label="附件" min-width="90" align="center">
                <template #default="{ row }">
                  <el-button v-if="row.attachments?.length" link type="primary" @click="openDetailAttachmentDialog(row)">附件（{{ row.attachments.length }}）</el-button>
                  <span v-else class="attachment-empty">无</span>
                </template>
              </el-table-column>
            </el-table>

            <div v-if="detailData.declarationCategory === '2'" class="info-card-header"><span>申报单明细（{{ detailData.facilities?.length || 0 }}）</span></div>
            <el-table v-if="detailData.declarationCategory === '2'" :data="detailData.facilities || []" border size="small" empty-text="暂无设施">
              <el-table-column label="设施编号" min-width="120" prop="snapshotFacilityCode" />
              <el-table-column label="设施名称" min-width="150" prop="snapshotFacilityName" />
              <el-table-column label="类型" min-width="100">
                <template #default="{ row }">{{ dictLabel('she_shi_lei_xing', row.snapshotFacilityType) }}</template>
              </el-table-column>
              <el-table-column label="位置" min-width="150" prop="snapshotLocation" />
              <el-table-column label="村组" min-width="110" prop="snapshotRegionName" />
              <el-table-column label="事项说明" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ row.remark || '—' }}</template>
              </el-table-column>
              <el-table-column label="申报金额" min-width="110" align="right">
                <template #default="{ row }">{{ row.amount != null ? '¥ ' + formatAmount(row.amount) : '—' }}</template>
              </el-table-column>
              <el-table-column label="附件" min-width="90" align="center">
                <template #default="{ row }">
                  <el-button v-if="row.attachments?.length" link type="primary" @click="openDetailAttachmentDialog(row)">附件（{{ row.attachments.length }}）</el-button>
                  <span v-else class="attachment-empty">无</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- ============ Tab 2：明细审核记录（时间线） ============ -->
          <el-tab-pane label="明细审核记录" name="audit-log">
            <div v-if="auditLogsLoading" class="audit-log-loading">
              <el-icon class="is-loading"><Loading /></el-icon> 加载中...
            </div>
            <el-empty v-else-if="auditLogsList.length === 0" description="暂无审核/修改历史" />
            <el-timeline v-else class="audit-timeline">
              <el-timeline-item
                v-for="item in auditLogsList"
                :key="item.id"
                :type="auditLogType(item.action)"
                :hollow="item.action === 'SUBMIT_FIRST' || item.action === 'SUBMIT_RESUBMITTED'"
                :timestamp="item.createTime"
                placement="top"
              >
                <div class="audit-log-head">
                  <el-tag :type="auditLogType(item.action)" size="small" effect="light">
                    {{ auditLogLabel(item.action) }}
                  </el-tag>
                  <el-tag size="small" class="audit-log-kind" effect="plain">
                    {{ item.detailKind === 'household' ? '农户' : '公建设施' }}
                  </el-tag>
                  <span class="audit-log-target">{{ item.detailSnapshotName || '—' }}</span>
                  <span class="audit-log-operator">{{ item.operatorName || '系统' }}</span>
                </div>
                <div v-if="item.remark" class="audit-log-remark">
                  <span class="audit-log-label">意见：</span>{{ item.remark }}
                </div>
                <div v-if="item.beforeAuditStatus || item.afterAuditStatus" class="audit-log-status">
                  <span class="audit-log-label">状态变化：</span>
                  <el-tag size="small" type="info" effect="plain">{{ auditStatusLabel(item.beforeAuditStatus) || '—' }}</el-tag>
                  <span class="audit-log-arrow">→</span>
                  <el-tag size="small" :type="auditStatusTagType(item.afterAuditStatus)" effect="plain">{{ auditStatusLabel(item.afterAuditStatus) || '—' }}</el-tag>
                </div>
                <div v-if="item.action === 'USER_EDIT'" class="audit-log-user-edit">
                  <div v-if="item.fieldDiff" class="audit-log-diff">
                    <span class="audit-log-label">字段变更：</span>
                    <div v-for="(df, j) in parseFieldDiff(item.fieldDiff)" :key="j" class="audit-log-diff-item">
                      <code>{{ df.field }}</code>：
                      <span class="diff-old">{{ df.old === null || df.old === undefined ? '（无）' : df.old }}</span>
                      <span class="diff-new">{{ df.new === null || df.new === undefined ? '（无）' : df.new }}</span>
                    </div>
                  </div>
                  <div v-if="item.attachmentChanged" class="audit-log-att">
                    <el-icon><Paperclip /></el-icon> 附件已变更（增/删/改）
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ========== 申报单级附件查看弹窗 ========== -->
    <el-dialog v-model="attachmentDialogVisible" title="📎 申报单级附件" width="720px" append-to-body :close-on-click-modal="true">
      <el-table :data="detailData.attachments || []" border size="small" empty-text="暂无附件">
        <el-table-column label="附件类型" width="140">
          <template #default="{ row: a }">{{ dictLabel('fu_jian_lei_xing', a.attachmentType) }}</template>
        </el-table-column>
        <el-table-column label="文件名" min-width="200">
          <template #default="{ row: a }">
            <el-link v-if="a.fileKey" type="primary" @click="openAttachmentFile(a, true)">{{ a.fileName }}</el-link>
            <span v-else>{{ a.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="110">
          <template #default="{ row: a }">{{ formatFileSize(a.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="150">
          <template #default="{ row: a }">{{ a.remark || '—' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- ========== 农户/公建设施行的附件查看弹窗 ========== -->
    <el-dialog v-model="detailAttachmentDialogVisible" :title="detailAttachmentDialogTitle" width="720px" append-to-body :close-on-click-modal="true">
      <el-table :data="currentDetailAttachments" border size="small" empty-text="暂无附件">
        <el-table-column label="附件类型" width="140">
          <template #default="{ row: a }">{{ dictLabel('fu_jian_lei_xing', a.attachmentType) }}</template>
        </el-table-column>
        <el-table-column label="文件名" min-width="200">
          <template #default="{ row: a }">
            <el-link v-if="a.fileKey" type="primary" @click="openAttachmentFile(a, true)">{{ a.fileName }}</el-link>
            <span v-else>{{ a.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="110">
          <template #default="{ row: a }">{{ formatFileSize(a.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="150">
          <template #default="{ row: a }">{{ a.remark || '—' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 农户明细查看弹窗（只读） -->
    <el-dialog v-model="householdViewDialog.visible" title="农户申报详情" width="560px">
      <template v-if="householdViewDialog.data">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="户主姓名">{{ householdViewDialog.data.snapshotHeadName }}</el-descriptions-item>
          <el-descriptions-item label="户号">{{ householdViewDialog.data.snapshotHouseholdNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ householdViewDialog.data.snapshotContactPhone }}</el-descriptions-item>
          <el-descriptions-item label="户类型">{{ dictLabel('hu_lei_xing', householdViewDialog.data.snapshotHouseholdType) }}</el-descriptions-item>
          <el-descriptions-item label="所属村组">{{ householdViewDialog.data.snapshotRegionName }}</el-descriptions-item>
          <el-descriptions-item label="身份证号" :span="2">{{ householdViewDialog.data.snapshotHeadIdCard }}</el-descriptions-item>
          <el-descriptions-item label="单户申报金额">
            <span v-if="householdViewDialog.data.amount != null" style="color:#d4380d;font-weight:600">¥ {{ formatAmount(householdViewDialog.data.amount) }}</span>
            <span v-else class="detail-empty">—</span>
          </el-descriptions-item>
          <el-descriptions-item label="事项说明" :span="2">{{ householdViewDialog.data.remark || '—' }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px">
          <div class="detail-attach-header">农户级附件（{{ householdViewDialog.data.attachments?.length || 0 }}）</div>
          <el-table v-if="householdViewDialog.data.attachments?.length" :data="householdViewDialog.data.attachments" size="small" border>
            <el-table-column label="附件类型" width="120">
              <template #default="{ row }">{{ dictLabel('fu_jian_lei_xing', row.attachmentType) }}</template>
            </el-table-column>
            <el-table-column label="文件名" show-overflow-tooltip>
              <template #default="{ row }">
                <el-link v-if="row.fileKey" type="primary" @click="openAttachmentFile(row, true)">{{ row.fileName }}</el-link>
                <span v-else>{{ row.fileName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" show-overflow-tooltip />
          </el-table>
          <div v-else class="detail-attach-empty">暂无附件</div>
        </div>
      </template>
      <template #footer>
        <el-button @click="householdViewDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 公建设施明细查看弹窗（只读） -->
    <el-dialog v-model="facilityViewDialog.visible" title="公建设施申报详情" width="560px">
      <template v-if="facilityViewDialog.data">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="设施名称" :span="2">{{ facilityViewDialog.data.snapshotFacilityName }}</el-descriptions-item>
          <el-descriptions-item label="设施编号">{{ facilityViewDialog.data.snapshotFacilityCode }}</el-descriptions-item>
          <el-descriptions-item label="设施类型">{{ dictLabel('she_shi_lei_xing', facilityViewDialog.data.snapshotFacilityType) }}</el-descriptions-item>
          <el-descriptions-item label="所在位置" :span="2">{{ facilityViewDialog.data.snapshotLocation }}</el-descriptions-item>
          <el-descriptions-item label="所属村组">{{ facilityViewDialog.data.snapshotRegionName }}</el-descriptions-item>
          <el-descriptions-item label="单设施申报金额">
            <span v-if="facilityViewDialog.data.amount != null" style="color:#d4380d;font-weight:600">¥ {{ formatAmount(facilityViewDialog.data.amount) }}</span>
            <span v-else class="detail-empty">—</span>
          </el-descriptions-item>
          <el-descriptions-item label="事项说明" :span="2">{{ facilityViewDialog.data.remark || '—' }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px">
          <div class="detail-attach-header">公建设施级附件（{{ facilityViewDialog.data.attachments?.length || 0 }}）</div>
          <el-table v-if="facilityViewDialog.data.attachments?.length" :data="facilityViewDialog.data.attachments" size="small" border>
            <el-table-column label="附件类型" width="120">
              <template #default="{ row }">{{ dictLabel('fu_jian_lei_xing', row.attachmentType) }}</template>
            </el-table-column>
            <el-table-column label="文件名" show-overflow-tooltip>
              <template #default="{ row }">
                <el-link v-if="row.fileKey" type="primary" @click="openAttachmentFile(row, true)">{{ row.fileName }}</el-link>
                <span v-else>{{ row.fileName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" show-overflow-tooltip />
          </el-table>
          <div v-else class="detail-attach-empty">暂无附件</div>
        </div>
      </template>
      <template #footer>
        <el-button @click="facilityViewDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 明细审核弹窗：通过 / 驳回（待审核行的操作入口） -->
    <el-dialog
      v-model="detailAuditDialog.visible"
      :title="detailAuditDialog.action === 'approve' ? '通过明细审核' : '驳回明细审核'"
      width="480px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-descriptions :column="1" border size="small">
        <template v-if="detailAuditDialog.detailKind === 'household'">
          <el-descriptions-item label="户主姓名">{{ detailAuditDialog.row?.snapshotHeadName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ maskPhone(detailAuditDialog.row?.snapshotContactPhone) }}</el-descriptions-item>
          <el-descriptions-item label="户类型">{{ dictLabel('hu_lei_xing', detailAuditDialog.row?.snapshotHouseholdType) }}</el-descriptions-item>
        </template>
        <template v-else>
          <el-descriptions-item label="设施名称">{{ detailAuditDialog.row?.snapshotFacilityName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="设施类型">{{ dictLabel('she_shi_lei_xing', detailAuditDialog.row?.snapshotFacilityType) }}</el-descriptions-item>
          <el-descriptions-item label="所在位置">{{ detailAuditDialog.row?.snapshotLocation || '--' }}</el-descriptions-item>
        </template>
        <el-descriptions-item label="申报金额">¥ {{ formatAmount(detailAuditDialog.row?.amount) }}</el-descriptions-item>
      </el-descriptions>
      <el-form :label-position="'top'" style="margin-top:12px">
        <el-form-item :label="detailAuditDialog.action === 'reject' ? '驳回理由（必填）' : '审核意见（选填）'" required>
          <el-input
            v-model="detailAuditDialog.auditRemark"
            type="textarea"
            :rows="3"
            :placeholder="detailAuditDialog.action === 'reject' ? '请填写驳回理由，申报人重新编辑时能看到' : '可写审核意见'"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detailAuditDialog.visible = false">取消</el-button>
        <el-button
          :type="detailAuditDialog.action === 'approve' ? 'success' : 'danger'"
          :loading="detailAuditDialog.submitting"
          :disabled="detailAuditDialog.action === 'reject' && !detailAuditDialog.auditRemark.trim()"
          @click="doAuditDetailDeclaration"
        >
          {{ detailAuditDialog.action === 'approve' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 明细撤销弹窗：收回审核结论（明细 → pending，父申报单 → submitted 待审核，保留审核指派） -->
    <el-dialog
      v-model="revertDialog.visible"
      title="撤销审核（回到待审核，可重新审核）"
      width="480px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-alert
        type="warning" :closable="false" show-icon
        title="撤销后，该明细将回到'待审核'状态，申报单同步回到待审核队列，审核人可重新做通过/驳回；申报人侧不可编辑。"
        style="margin-bottom:12px"
      />
      <el-descriptions :column="1" border size="small">
        <template v-if="revertDialog.detailKind === 'household'">
          <el-descriptions-item label="户主姓名">{{ revertDialog.row?.snapshotHeadName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ maskPhone(revertDialog.row?.snapshotContactPhone) }}</el-descriptions-item>
          <el-descriptions-item label="户类型">{{ dictLabel('hu_lei_xing', revertDialog.row?.snapshotHouseholdType) }}</el-descriptions-item>
        </template>
        <template v-else>
          <el-descriptions-item label="设施名称">{{ revertDialog.row?.snapshotFacilityName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="设施类型">{{ dictLabel('she_shi_lei_xing', revertDialog.row?.snapshotFacilityType) }}</el-descriptions-item>
          <el-descriptions-item label="所在位置">{{ revertDialog.row?.snapshotLocation || '—' }}</el-descriptions-item>
        </template>
        <el-descriptions-item label="当前明细状态">
          <el-tag v-if="revertDialog.row?.auditStatus === 'approved'" type="success" size="small">已通过</el-tag>
          <el-tag v-else-if="revertDialog.row?.auditStatus === 'rejected'" type="danger" size="small">已驳回</el-tag>
          <el-tag v-else-if="revertDialog.row?.auditStatus === 'pending' || !revertDialog.row?.auditStatus" type="warning" size="small">待审核</el-tag>
          <el-tag v-else-if="revertDialog.row?.auditStatus === 'established'" type="primary" size="small">已立项</el-tag>
          <el-tag v-else size="small">{{ revertDialog.row?.auditStatus }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申报金额">¥ {{ formatAmount(revertDialog.row?.amount) }}</el-descriptions-item>
      </el-descriptions>
      <el-form :label-position="'top'" style="margin-top:12px">
        <el-form-item label="撤销备注（可选）">
          <el-input
            v-model="revertDialog.remark"
            type="textarea"
            :rows="3"
            placeholder="可填写撤销原因，留痕后在重新审核时可见"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revertDialog.visible = false">取消</el-button>
        <el-button type="warning" :loading="revertDialog.submitting" @click="doRevertDetailDeclaration">
          确认撤销（回到待审核）
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== 立项申请弹窗（已通过页签 → 新建立项） ========== -->
    <el-dialog
      v-model="approvalDialog.visible"
      title="新建立项申请"
      width="780px"
      :close-on-click-modal="false"
      destroy-on-close
      append-to-body
    >
      <el-form :model="approvalDialog.form" :rules="approvalRules" ref="approvalFormRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目编号" prop="approvalCode">
              <el-input v-model="approvalDialog.form.approvalCode" placeholder="请填写项目编号" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目名称" prop="projectName">
              <el-input v-model="approvalDialog.form.projectName" placeholder="请填写立项项目名称" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目类型" prop="projectType">
              <el-select v-model="approvalDialog.form.projectType" placeholder="请选择" style="width:100%">
                <el-option v-for="d in approvalProjectTypeOpts" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类别" prop="projectCategory">
              <el-select v-model="approvalDialog.form.projectCategory" placeholder="请选择" style="width:100%">
                <el-option v-for="d in approvalProjectCategoryOpts" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目预算（元）">
              <el-input-number v-model="approvalDialog.form.budget" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-input v-model="approvalDialog.form.leader" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="申报人">
              <el-input v-model="approvalDialog.form.applyBy" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属乡镇">
              <el-input :model-value="approvalDialog.form.townName" disabled placeholder="自动带出（从申报单提取）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="建设依据" prop="remark">
          <el-input v-model="approvalDialog.form.remark" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请填写立项依据 / 必要性 / 预期效果" />
        </el-form-item>

        <!-- 关联申报单（卡片式，和立项资料附件风格统一） -->
        <el-form-item label="关联申报单">
          <div class="approval-section">
            <div class="section-head">
              <span class="section-title">— 关联申报单 —</span>
              <el-button link type="primary" size="small" @click="addApprovalDecl">
                + 新增申报单{{ approvalDialog.declarationRows.length > 0 ? '' : '（手动输入 ID）' }}
              </el-button>
            </div>
            <div v-if="approvalDialog.declarationRows.length === 0" class="section-empty">
              <el-input v-model="approvalDialog.newDeclId" placeholder="手动输入申报单 ID 后点击新增" style="width:280px" />
            </div>
            <div v-for="(item, idx) in approvalDialog.declarationRows" :key="idx" class="decl-card">
              <el-input :model-value="`#${idx + 1}`" disabled class="decl-no" />
              <el-input v-model="item.businessCode" disabled placeholder="申报编号" class="decl-code" />
              <el-input v-model="item.title" disabled placeholder="申报标题" class="decl-title" />
              <el-input
                :model-value="item.amount != null ? `¥ ${formatAmount(item.amount)}` : '—'"
                disabled
                class="decl-amount"
              />
              <el-button link type="danger" size="small" :icon="Delete" @click="removeApprovalDecl(idx)" />
            </div>
          </div>
        </el-form-item>

        <!-- 立项资料附件：卡片式 + 三列表头 -->
        <el-form-item label="立项资料附件">
          <div class="approval-section">
            <div class="section-head">
              <span class="section-title">— 立项资料附件 —</span>
              <el-button
                link type="primary" size="small" icon="Plus"
                @click="approvalDialog.form.attachments.push({ attachmentType: '', fileKey: '', fileName: '', remark: '' })"
              >+ 添加附件</el-button>
            </div>
            <div v-if="approvalDialog.form.attachments.length === 0" class="section-empty">暂无附件，点击右上角"+ 添加附件"即可</div>
            <div v-for="(row, idx) in approvalDialog.form.attachments" :key="idx" class="att-card">
              <div class="att-card-head">
                <span class="att-card-index">{{ idx + 1 }}</span>
                <span class="att-card-title">附件 #{{ idx + 1 }}</span>
                <el-button link type="danger" size="small" :icon="Delete" @click="approvalDialog.form.attachments.splice(idx, 1)">删除</el-button>
              </div>
              <div class="att-card-body">
                <el-form-item label="附件类型" label-width="72px" class="att-field att-field-type">
                  <el-select v-model="row.attachmentType" placeholder="请选择类型" style="width:100%">
                    <el-option v-for="d in approvalAttachmentTypeOpts" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
                  </el-select>
                </el-form-item>
                <el-form-item label="上传附件" label-width="72px" class="att-field att-field-upload">
                  <el-upload
                    :action="fileUploadAction"
                    :headers="fileUploadHeaders"
                    :data="{ bizModule: 'approval', bucket: 'archive-formal' }"
                    :show-file-list="false"
                    :before-upload="(file: File) => beforeApprovalAttUpload(file, idx)"
                    :on-success="(res: any, _f: any) => onApprovalAttUploadSuccess(res, idx)"
                    :on-error="onApprovalAttUploadError"
                  >
                    <el-button>
                      <el-icon><UploadFilled /></el-icon>
                      {{ row.fileName ? '重新上传' : '点击选择文件' }}
                    </el-button>
                  </el-upload>
                  <span v-if="row.fileName" class="att-fname">✓ {{ row.fileName }}</span>
                </el-form-item>
                <el-form-item label="备注" label-width="72px" class="att-field att-field-remark">
                  <el-input v-model="row.remark" placeholder="选填" maxlength="200" />
                </el-form-item>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="approvalDialog.submitting" @click="doSubmitApproval">提交立项申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Loading, Paperclip, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getDictByType } from '@/api/dict'
import { getPresignUrl } from '@/api/file'
import {
  getReceivedDeclarationPage,
  auditDeclarationDetail,
  revertDeclarationDetail,
  getDeclarationDetail,
  getAuditLogs,
  submitProjectApproval,
  type DeclarationQuery,
  type DeclarationVO,
  type DeclarationDetailFull,
  type DeclarationHouseholdDetail,
  type DeclarationFacilityDetail,
  type DeclarationDetailAuditDTO,
  type DeclarationDetailRevertDTO,
  type ProjectApprovalSubmitDTO,
  type ApprovalAttachmentItem,
  type DeclarationDetailAuditLogVO
} from '@/api/declaration'

// 上传 action / headers（立项附件用 archive-formal 桶）
const fileUploadAction = '/api/file/upload'
const fileUploadHeaders = computed(() => {
  const token = localStorage.getItem('token') || ''
  return token ? { Authorization: `Bearer ${token}` } : {}
})

// ================== 字典（页面共用） ==================
const declarationTypeOpts = ref<any[]>([])
const declarationCategoryOpts = ref<any[]>([])
const statusOpts = ref<any[]>([])
const attachmentTypeOpts = ref<any[]>([])
const houseTypeOpts = ref<any[]>([])
const householdTypeOpts = ref<any[]>([])
const facilityTypeOpts = ref<any[]>([])

async function loadDict() {
  const types = ['shen_bao_lei_xing', 'shen_bao_lei_bie', 'declaration_status', 'fu_jian_lei_xing',
    'fang_wu_jie_gou_lei_xing', 'hu_lei_xing', 'she_shi_lei_xing']
  const results = await Promise.all(types.map((t) => getDictByType(t)))
  declarationTypeOpts.value = results[0]?.data || []
  declarationCategoryOpts.value = results[1]?.data || []
  statusOpts.value = results[2]?.data || []
  attachmentTypeOpts.value = results[3]?.data || []
  houseTypeOpts.value = results[4]?.data || []
  householdTypeOpts.value = results[5]?.data || []
  facilityTypeOpts.value = results[6]?.data || []
}

function dictLabel(dictType: string, value?: string): string {
  if (!value) return '—'
  const opts = ({
    shen_bao_lei_xing: declarationTypeOpts.value,
    shen_bao_lei_bie: declarationCategoryOpts.value,
    declaration_status: statusOpts.value,
    fu_jian_lei_xing: attachmentTypeOpts.value,
    fang_wu_jie_gou_lei_xing: houseTypeOpts.value,
    hu_lei_xing: householdTypeOpts.value,
    she_shi_lei_xing: facilityTypeOpts.value
  } as any)[dictType] || []
  return opts.find((d: any) => d.dictValue === value)?.dictLabel || value
}

function statusTagType(status?: string) {
  if (status === 'approved') return 'success'
  if (status === 'submitted') return 'warning'
  if (status === 'rejected') return 'danger'
  if (status === 'established') return ''
  return 'info'
}

// ================== 列表页 ==================
const loading = ref(false)
const query = reactive<DeclarationQuery>({ pageNum: 1, pageSize: 10 })
// 状态页签（申报审核页固定顺序：待审核 → 已通过 → 已驳回 → 已立项）
const statusTabs = [
  { label: '待审核', name: 'submitted' },
  { label: '已通过', name: 'approved' },
  { label: '已驳回', name: 'rejected' },
  { label: '已立项', name: 'established' }
]
// 审核页默认落在"待审核"
const activeStatusTab = ref<string>('submitted')
const tableData = ref<DeclarationDetailFull[]>([])
const total = ref(0)

/**
 * 把后端返回的 DeclarationDetailFull[] 列表转成 Element Plus 树形结构
 * 父行：声明单本身；子行：农户/公建设施明细（按 category 分支）
 * 操作列对每一行都生效（fixed:right）
 */
const treeTableData = computed(() => {
  return tableData.value.map(parent => {
    // 拷贝一份，加树形辅助字段
    const node: any = { ...parent, _rowKey: parent.id, _isDetail: false }
    let children: any[] = []
    if (parent.declarationCategory === '1' && parent.households?.length) {
      children = parent.households.map(hh => ({
        ...hh,
        _rowKey: 'h-' + hh.id,
        _isDetail: true,
        _detailKind: 'household',
        _parentStatus: parent.status,
        _parentRef: { id: parent.id, declarationCategory: parent.declarationCategory, status: parent.status }
      }))
    } else if (parent.declarationCategory === '2' && parent.facilities?.length) {
      children = parent.facilities.map(pf => ({
        ...pf,
        _rowKey: 'f-' + pf.id,
        _isDetail: true,
        _detailKind: 'public_facility',
        _parentStatus: parent.status,
        _parentRef: { id: parent.id, declarationCategory: parent.declarationCategory, status: parent.status }
      }))
    }
    node._children = children
    node._hasChildren = children.length > 0
    return node
  })
})

async function loadData() {
  loading.value = true
  try {
    // 状态页签合并进查询参数（页签优先级高于筛选区的 status 下拉，两者互斥）
    const params = { ...query }
    if (activeStatusTab.value) {
      params.status = activeStatusTab.value
    } else {
      params.status = query.status || undefined
    }
    const res: any = await getReceivedDeclarationPage(params)
    tableData.value = (res.data?.records as any) || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}
function handleQuery() { query.pageNum = 1; loadData() }
function resetQuery() {
  Object.assign(query, { title: undefined, declarationType: undefined, declarationCategory: undefined, pageNum: 1, pageSize: 10 })
  activeStatusTab.value = 'submitted'
  loadData()
}
function onStatusTabChange(tab: string) {
  activeStatusTab.value = tab
  query.pageNum = 1
  loadData()
}

/** 手机号脱敏 138****1234 */
function maskPhone(p?: string) {
  if (!p || p.length < 7) return p || '—'
  return p.substring(0, 3) + '****' + p.substring(p.length - 4)
}
function formatAmount(v?: number | string | null) {
  if (v == null || v === '') return '0.00'
  const n = Number(v)
  if (isNaN(n)) return String(v)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 文件大小格式化：字节数 → KB/MB */
function formatFileSize(bytes?: number | null): string {
  if (bytes == null || bytes === 0) return '—'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/** 审核日志 action 常量映射（和 DeclarationSubmit.vue 对齐） */
const AUDIT_ACTION_MAP: Record<string, { type: string; label: string }> = {
  SUBMIT_FIRST:    { type: 'info',    label: '首次提交审核' },
  SUBMIT_RESUBMITTED: { type: 'info', label: '重新提交审核' },
  AUDIT_PASS:      { type: 'success', label: '审核通过' },
  AUDIT_REJECT:    { type: 'danger',  label: '审核驳回' },
  AUDIT_REVOKE:    { type: 'warning', label: '撤销审核' },
  USER_EDIT:       { type: 'primary', label: '用户修改明细' },
}
function auditLogType(action: string) { return AUDIT_ACTION_MAP[action]?.type ?? 'info' }
function auditLogLabel(action: string) { return AUDIT_ACTION_MAP[action]?.label ?? action }
function auditStatusLabel(s?: string | null) {
  const m: Record<string, string> = { unsubmitted: '未提交', pending: '待审核', approved: '已通过', rejected: '已驳回', revoked: '已撤销' }
  return s ? (m[s] ?? s) : ''
}
function auditStatusTagType(s?: string | null): string {
  if (s === 'approved') return 'success'
  if (s === 'rejected') return 'danger'
  if (s === 'pending') return 'warning'
  return 'info'
}
/** fieldDiff JSON 解析：后端写的是 [{field, old, new}] 数组字符串 */
function parseFieldDiff(raw?: string): { field: string; old: any; new: any }[] {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    if (Array.isArray(arr)) return arr
  } catch (_) { /* ignore */ }
  return []
}

/** 明细审核历史时间线 */
const detailTab = ref<'info' | 'audit-log'>('info')
const auditLogsList = ref<DeclarationDetailAuditLogVO[]>([])
const auditLogsLoading = ref(false)
let auditLogsLoadedFor: string | null = null
async function fetchAuditLogs(declarationId: string) {
  if (!declarationId) return
  auditLogsLoading.value = true
  auditLogsLoadedFor = declarationId
  try {
    const r = await getAuditLogs(declarationId)
    auditLogsList.value = (r.data || []) as DeclarationDetailAuditLogVO[]
  } catch { auditLogsList.value = [] }
  finally { auditLogsLoading.value = false }
}
function resetAuditLogs() {
  auditLogsList.value = []
  auditLogsLoadedFor = null
}

/**
 * 子明细行横向合并（列索引随 selection 列的 v-if 动态变化，不能硬编码）
 * 待审核等 tab（无 selection，共 10 列）：0=箭头列，1=申报编号(明细内容)…8，9=操作列
 * 已通过/已立项 tab（有 selection，共 11 列）：0=勾选列，1=箭头列，2=申报编号(明细内容)…9，10=操作列
 * 规则：最左侧的勾选/箭头列各自独立；申报编号列横向合并到操作列之前；操作列独立
 */
function detailMergeMethod({ row, columnIndex }: { row: any; columnIndex: number }) {
  if (!row._isDetail) return { rowspan: 1, colspan: 1 }
  // 已通过等页签多出一列 selection，总列数动态计算（加了"申报日期"列后变 11 / 12）
  const hasSelection = activeStatusTab.value === 'approved'
  const totalCols = hasSelection ? 12 : 11
  const actionColIndex = totalCols - 1              // 操作列（固定右侧）
  const firstMergedCol = hasSelection ? 2 : 1       // 申报编号列（明细内容模板所在列）
  // 勾选列 / 箭头列：独立不合并
  if (columnIndex < firstMergedCol) return { rowspan: 1, colspan: 1 }
  // 操作列：独立，保留按钮
  if (columnIndex === actionColIndex) return { rowspan: 1, colspan: 1 }
  // 申报编号列：一次性横向合并到操作列之前
  if (columnIndex === firstMergedCol) {
    return { rowspan: 1, colspan: actionColIndex - firstMergedCol }
  }
  return { rowspan: 0, colspan: 0 }  // 被合并掉的中间列
}
function isDetailRowClass({ row }: { row: any }) {
  return row._isDetail ? 'is-detail-row' : ''
}

// ================== 详情 弹窗（只读） ==================
const dialogVisible = ref(false)
const detailData = ref<any>({})

/** 申报单级附件弹窗 */
const attachmentDialogVisible = ref(false)
function openAttachmentDialog() { attachmentDialogVisible.value = true }

/** 农户/公建设施行的附件弹窗 */
const detailAttachmentDialogVisible = ref(false)
const detailAttachmentDialogTitle = ref('')
const currentDetailAttachments = ref<any[]>([])
function openDetailAttachmentDialog(row: any) {
  const name = row.snapshotHeadName || row.snapshotFacilityName || ''
  const total = row.attachments?.length || 0
  detailAttachmentDialogTitle.value = `${name} 的附件（${total}）`
  currentDetailAttachments.value = row.attachments || []
  detailAttachmentDialogVisible.value = true
}

/** 打开申报详情（防重入：先关再开防止叠层） */
function openDetail(row: DeclarationVO) {
  const open = () => {
    dialogVisible.value = true
    detailTab.value = 'info'
    resetAuditLogs()
    getDeclarationDetail(row.id).then((r) => {
      detailData.value = (r.data as any) || {}
      fetchAuditLogs(row.id)
    })
  }
  if (dialogVisible.value) {
    dialogVisible.value = false
    nextTick(open)
  } else {
    open()
  }
}

/** 附件下载：通过后端获取时效签名 URL */
async function openAttachmentFile(file: any, download = false) {
  if (!file?.fileKey) return
  const res = await getPresignUrl({ bucket: file.bucket, fileKey: file.fileKey, fileName: file.fileName, download })
  window.open(res.data as string, '_blank')
}

// ================== 明细查看弹窗（只读，展示申报数据 + 附件） ==================
const householdViewDialog = reactive({ visible: false, data: null as DeclarationHouseholdDetail | null })
const facilityViewDialog  = reactive({ visible: false, data: null as DeclarationFacilityDetail | null })

function openHouseholdDetailView(hh: DeclarationHouseholdDetail) {
  householdViewDialog.data = hh
  householdViewDialog.visible = true
}
function openFacilityDetailView(pf: DeclarationFacilityDetail) {
  facilityViewDialog.data = pf
  facilityViewDialog.visible = true
}

// ================== 明细审核弹窗（明细 pending → approved / rejected，后端自动汇总推进父状态） ==================
const detailAuditDialog = reactive({
  visible: false,
  row: null as any,
  detailKind: 'household' as 'household' | 'public_facility',
  action: 'approve' as 'approve' | 'reject',
  auditRemark: '',
  submitting: false
})

function openDetailAuditDialog(row: any, action: 'approve' | 'reject') {
  detailAuditDialog.row = row
  detailAuditDialog.detailKind = row._detailKind === 'public_facility' ? 'public_facility' : 'household'
  detailAuditDialog.action = action
  detailAuditDialog.auditRemark = ''
  detailAuditDialog.visible = true
}

async function doAuditDetailDeclaration() {
  if (!detailAuditDialog.row) return
  if (detailAuditDialog.action === 'reject' && !detailAuditDialog.auditRemark.trim()) {
    ElMessage.warning('驳回必须填写审核意见')
    return
  }
  const dto: DeclarationDetailAuditDTO = {
    detailId: String(detailAuditDialog.row.id),
    detailKind: detailAuditDialog.detailKind,
    action: detailAuditDialog.action,
    auditRemark: detailAuditDialog.auditRemark || undefined
  }
  detailAuditDialog.submitting = true
  try {
    await auditDeclarationDetail(dto)
    ElMessage.success(detailAuditDialog.action === 'approve' ? '已通过' : '已驳回')
    detailAuditDialog.visible = false
    loadData()
  } catch { /* 拦截器统一处理 */ }
  finally { detailAuditDialog.submitting = false }
}

// ================== 明细撤销弹窗（明细 → pending，父申报单 → submitted 待审核，保留 recipient 审核指派） ==================
const revertDialog = reactive({
  visible: false,
  row: null as any,
  detailKind: 'household' as 'household' | 'public_facility',
  remark: '',
  submitting: false
})

function openRevertDialog(row: any) {
  revertDialog.row = row
  revertDialog.detailKind = row._detailKind === 'public_facility' ? 'public_facility' : 'household'
  revertDialog.remark = ''
  revertDialog.visible = true
}

async function doRevertDetailDeclaration() {
  if (!revertDialog.row) return
  const dto: DeclarationDetailRevertDTO = {
    detailId: String(revertDialog.row.id),
    detailKind: revertDialog.detailKind,
    remark: revertDialog.remark.trim() || undefined
  }
  revertDialog.submitting = true
  try {
    await revertDeclarationDetail(dto)
    ElMessage.success('已撤销，该明细回到待审核，可重新审核')
    revertDialog.visible = false
    loadData()
  } catch { /* 拦截器会提示后端返回的业务错误 */ }
  finally { revertDialog.submitting = false }
}

// ================== 多选（已通过页签） ==================
const selectedRows = ref<any[]>([])
const selectedDeclarationIds = computed(() =>
  selectedRows.value
    .filter((r: any) => !r._isDetail && (r.auditStatus === 'approved' || !r.auditStatus))
    .map((r: any) => String(r.id))
)

function isSelectableRow(row: any) {
  // 只允许选父申报单行，且该行必须至少有一条 approved 明细（立项才有意义）
  if (row._isDetail) return false
  const approvedExists = Array.isArray(row._children) && row._children.some((c: any) => c.auditStatus === 'approved')
  return approvedExists
}

function onSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

// ================== 立项弹窗（已通过页签 → 新建立项） ==================
const approvalFormRef = ref<FormInstance>()
// projectTypeOpts / projectCategoryOpts / attachmentTypeOpts 与页面共用字典同名（attachmentTypeOpts 存在）
// 这里另外起名字避免冲突 —— attachmentTypeOpts 已在公共区定义过
const approvalProjectTypeOpts = ref<any[]>([])
const approvalProjectCategoryOpts = ref<any[]>([])
const approvalAttachmentTypeOpts = ref<any[]>([])

const approvalRules: FormRules = {
  approvalCode: [{ required: true, message: '立项编号必填', trigger: 'blur' }],
  projectName: [{ required: true, message: '项目名称必填', trigger: 'blur' }],
  projectType: [{ required: true, message: '项目类型必填', trigger: 'change' }],
  projectCategory: [{ required: true, message: '项目类别必填', trigger: 'change' }],
  remark: [{ required: true, message: '建设依据必填', trigger: 'blur' }]
}

interface ApprovalRowRef { id: string; businessCode: string; title: string; amount: number | null }

const approvalDialog = reactive({
  visible: false,
  submitting: false,
  declarationRows: [] as ApprovalRowRef[],
  newDeclId: '',
  form: {
    approvalCode: '',
    projectName: '',
    projectType: '',
    projectCategory: '',
    projectSource: '',
    budget: null as number | null,
    leader: '',
    applyBy: '',
    remark: '',
    townCode: '',
    townName: '',
    attachments: [] as ApprovalAttachmentItem[]
  }
})

function openApprovalDialog() {
  // 准备字典（立项弹窗独立加载，与共用字典互不干扰）
  getDictByType('xiang_mu_lei_xing').then((r: any) => { approvalProjectTypeOpts.value = r?.data || [] })
  getDictByType('xiang_mu_lei_bie').then((r: any) => { approvalProjectCategoryOpts.value = r?.data || [] })
  getDictByType('li_xiang_fu_jian_lei_xing').then((r: any) => { approvalAttachmentTypeOpts.value = r?.data || [] })

  // 从已选申报单行里抽信息做快照（只读，后端存 declaration_id 即可）
  approvalDialog.declarationRows = selectedRows.value
    .filter((r: any) => !r._isDetail)
    .map((r: any) => ({
      id: String(r.id),
      businessCode: r.businessCode || '',
      title: r.title || '',
      amount: r.amount ?? null
    }))

  // 所属乡镇：自动从第一条带 town 信息的申报单里取（所有勾的申报单本来就应该同乡镇）
  const firstWithTown = selectedRows.value.find((r: any) => r.townCode && !r._isDetail)
  if (firstWithTown) {
    approvalDialog.form.townCode = firstWithTown.townCode
    approvalDialog.form.townName = firstWithTown.townName
  }

  // 重置表单
  approvalDialog.form.approvalCode = ''
  approvalDialog.form.projectName = ''
  approvalDialog.form.projectType = ''
  approvalDialog.form.projectCategory = ''
  approvalDialog.form.projectSource = ''
  approvalDialog.form.budget = null
  approvalDialog.form.leader = ''
  approvalDialog.form.applyBy = ''
  approvalDialog.form.remark = ''
  approvalDialog.form.attachments = []
  approvalDialog.newDeclId = ''

  approvalDialog.visible = true
  nextTick(() => approvalFormRef.value?.clearValidate())
}

function removeApprovalDecl(idx: number) {
  approvalDialog.declarationRows.splice(idx, 1)
}

function addApprovalDecl() {
  // 手动追加：给一个空行让用户输入 ID（后端不校验数量，只要在已选范围外且归属同用户即可）
  if (approvalDialog.newDeclId && approvalDialog.newDeclId.trim()) {
    approvalDialog.declarationRows.push({
      id: approvalDialog.newDeclId.trim(),
      businessCode: '(手动追加)',
      title: '待后端补全',
      amount: null
    })
    approvalDialog.newDeclId = ''
  }
}

async function doSubmitApproval() {
  if (!approvalFormRef.value) return
  const valid = await approvalFormRef.value.validate().catch(() => false)
  if (!valid) return

  if (approvalDialog.declarationRows.length === 0) {
    ElMessage.warning('至少要有一条关联申报单')
    return
  }
  if (approvalDialog.form.attachments.some((a) => a.attachmentType && !a.fileKey)) {
    ElMessage.warning('附件行已选类型但未上传文件，请补传或删除该行')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认提交立项申请「${approvalDialog.form.projectName}」？\n提交后 ${approvalDialog.declarationRows.length} 条申报单下的已通过明细将变为已立项。`,
      '提交确认',
      { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '取消' }
    )
  } catch { return }

  approvalDialog.submitting = true
  try {
    const dto: ProjectApprovalSubmitDTO = {
      approvalCode: approvalDialog.form.approvalCode,
      projectName: approvalDialog.form.projectName,
      projectType: approvalDialog.form.projectType || undefined,
      projectCategory: approvalDialog.form.projectCategory || undefined,
      projectSource: approvalDialog.form.projectSource || undefined,
      budget: approvalDialog.form.budget,
      leader: approvalDialog.form.leader || undefined,
      applyBy: approvalDialog.form.applyBy || undefined,
      remark: approvalDialog.form.remark || undefined,
      townCode: approvalDialog.form.townCode || undefined,
      townName: approvalDialog.form.townName || undefined,
      declarationIds: approvalDialog.declarationRows.map((r) => r.id),
      attachments: approvalDialog.form.attachments.filter((a) => a.fileKey)
    }
    const res = await submitProjectApproval(dto)
    ElMessage.success(
      `立项提交成功！立项编号：${res.data?.approvalCode}；已处理 ${res.data?.declareCount} 条申报单 / ${res.data?.detailCount} 条明细`
    )
    approvalDialog.visible = false
    // 回清空选、重新拉列表（已立项的明细会消失，parent 状态被推进）
    selectedRows.value = []
    selectedDeclarationIds.value.length = 0
    loadData()
  } catch { /* 拦截器处理业务错误（立项编号重复、申报单归属校验等） */ }
  finally { approvalDialog.submitting = false }
}

// ========== 立项附件上传 ==========
function beforeApprovalAttUpload(file: File, _idx: number) {
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.warning('单个附件不能超过 50MB')
    return false
  }
  return true
}

function onApprovalAttUploadSuccess(res: any, idx: number) {
  if (res?.code !== 200 || !res?.data) {
    ElMessage.error(res?.msg || '上传失败')
    return
  }
  const item = approvalDialog.form.attachments[idx]
  item.fileKey = res.data.fileKey
  item.bucket = res.data.bucket || 'archive-formal'
  item.fileName = res.data.fileName || res.data.originalName || ''
  item.fileSize = res.data.fileSize
  ElMessage.success('上传成功')
}

function onApprovalAttUploadError(err: any) {
  ElMessage.error('上传失败：' + (err?.message || '请重试'))
}

// ================== 生命周期 ==================
onMounted(async () => {
  await loadDict()
  loadData()
})

// 监听页码变化：翻页时只触发一次 loadData，避免和 v-model 联动导致的重复请求
// （之前用 el-pagination @current-change + onStatusTabChange 内部设 query.pageNum=1 会触发两次 loadData，
//  第二次返回空数组把 el-table 的数据覆盖成空，导致树形子行丢失）
watch(() => query.pageNum, (val, oldVal) => {
  if (val !== oldVal) loadData()
})
</script>

<style scoped>
.declaration-page { padding: 0; }
.query-card { margin-bottom: 12px; }
.tab-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.tab-bar .status-tabs { flex: 1; }
.tab-bar :deep(.el-tabs__header) { margin: 0; margin-bottom: 0; }

.detail-mode .detail-section { margin-top: 16px; }
/* info-card-header 替代旧 detail-section-title：14px bold + 下面一条分隔线，无外框 */
.detail-mode .info-card-header {
  margin: 16px 0 8px;
  padding-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
}
.detail-mode .attachment-empty { color: #c0c4cc; font-size: 13px; }
/* 附件按钮字号对齐表格内容（size="small" → 12px），避免 el-button 默认 14px 偏大 */
.detail-mode .el-button.is-link { font-size: 12px !important; padding: 0; min-height: auto; }

/* ============ 明细审核记录时间线（与 DeclarationSubmit.vue 对齐） ============ */
.audit-log-loading { text-align: center; padding: 40px; color: #909399; }
.audit-timeline { padding: 4px 0 4px 4px; }
.audit-log-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.audit-log-head .audit-log-operator { color: #909399; font-size: 12px; margin-left: auto; }
.audit-log-head .audit-log-target { font-weight: 500; color: #303133; font-size: 13px; }
.audit-log-kind { opacity: 0.8; }
.audit-log-label { color: #909399; font-size: 12px; margin-right: 4px; }
.audit-log-remark { margin-top: 4px; color: #606266; font-size: 13px; line-height: 1.5; }
.audit-log-status { margin-top: 6px; font-size: 12px; display: flex; align-items: center; gap: 6px; }
.audit-log-arrow { color: #909399; }
.audit-log-diff { margin-top: 6px; }
.audit-log-diff-item { font-size: 12px; color: #606266; line-height: 1.7; padding-left: 4px; }
.audit-log-diff-item code { background: #f4f4f5; padding: 1px 5px; border-radius: 3px; color: #606266; }
.diff-old { color: #f56c6c; text-decoration: line-through; margin: 0 4px; }
.diff-new { color: #67c23a; margin: 0 4px; font-weight: 500; }
.audit-log-att { margin-top: 4px; font-size: 12px; color: #e6a23c; display: flex; align-items: center; gap: 4px; }

.section-block { margin-top: 10px; }
.section-divider { display: flex; justify-content: space-between; align-items: center; }
.section-divider span { font-weight: 600; font-size: 14px; }
.section-tip { color: #909399; font-size: 12px; font-weight: normal; margin-left: 10px; }
.empty-tip { color: #909399; font-size: 13px; padding: 10px 0; }

.attachment-row { border: 1px solid #ebeef5; border-radius: 6px; margin-bottom: 10px; }
.attachment-row-header { display: flex; justify-content: space-between; align-items: center; padding: 6px 12px; background: #f5f7fa; border-radius: 6px 6px 0 0; }
.attachment-row-index { font-weight: 600; color: #409eff; }
.attachment-row-body { display: flex; gap: 12px; padding: 12px; }
.attachment-field { flex: 1; min-width: 0; }
.attachment-field-wide { flex: 1.2; }
.attachment-field label { display: block; font-size: 12px; color: #606266; margin-bottom: 4px; }

.upload-trigger { display: flex; align-items: center; gap: 6px; padding: 8px 12px; border: 1px dashed #dcdfe6; border-radius: 4px; cursor: pointer; color: #606266; min-height: 34px; }
.upload-trigger:hover { border-color: #409eff; color: #409eff; }

/* 明细行横向合并后的样式 —— 字号和父申报行一致（默认 el-table cell 14px） */
.detail-line.merged {
  display: flex; align-items: center; flex-wrap: wrap; gap: 0;
  font-size: 14px; line-height: 1.8; color: #303133;
}
.detail-inline-label { color: #909399; font-size: 14px; }
.detail-money { color: #d4380d; font-weight: 600; font-size: 14px; }
.sep { color: #c0c4cc; margin: 0 6px; }

/* 明细行操作按钮字号也强制对齐父行 14px（link 按钮默认会继承） */
:deep(.el-table__body tr.is-detail-row .el-button) { font-size: 14px; }
/* el-tag small 在明细里也调大一点，避免太挤 */
:deep(.el-table__body tr.is-detail-row .el-tag) { font-size: 12px; height: 22px; line-height: 20px; }

/* 子明细行（_isDetail=true）整行浅灰底色 */
:deep(.el-table__body tr.is-detail-row > td) { background-color: #fafbfc !important; }
:deep(.el-table__body tr.is-detail-row td:first-child) { border-left: 3px solid #409eff; }

/* 明细级附件区 */
.detail-attachments { margin-top: 8px; border-top: 1px dashed #ebeef5; padding-top: 12px; }
.detail-attach-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 13px; color: #606266; font-weight: 600; }
.detail-attach-empty { color: #909399; font-size: 13px; padding: 12px 0; text-align: center; }
.detail-attach-row { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.attach-file-name { max-width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 12px; color: #606266; }
.detail-upload-trigger { padding: 4px 10px; min-height: 30px; font-size: 12px; gap: 4px; }

/* ============ 立项弹窗：关联申报单 / 立项资料附件（卡片式） ============ */
.approval-section { width: 100%; }
.approval-section .section-head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 10px; padding-bottom: 6px;
  border-bottom: 1px dashed #ebeef5;
}
.approval-section .section-title {
  font-size: 13px; font-weight: 600; color: #606266; letter-spacing: 1px;
}
.approval-section .section-empty {
  color: #909399; font-size: 12px; padding: 6px 0 10px;
}

/* 关联申报单卡片：横向一行 */
.decl-card {
  display: flex; align-items: center; gap: 10px;
  border: 1px solid #ebeef5; border-radius: 6px;
  padding: 8px 10px; margin-bottom: 8px; background: #fafbfc;
}
.decl-no { width: 48px; text-align: center; color: #409eff; font-weight: 600; }
.decl-code { width: 130px; }
.decl-title { flex: 1; min-width: 0; }
.decl-amount { width: 120px; text-align: right; color: #d4380d; font-weight: 600; }

/* 立项资料附件卡片 */
.att-card {
  border: 1px solid #ebeef5; border-radius: 6px;
  margin-bottom: 10px; background: #fff; overflow: hidden;
}
.att-card-head {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}
.att-card-index {
  display: inline-flex; align-items: center; justify-content: center;
  width: 20px; height: 20px; border-radius: 50%;
  background: #409eff; color: #fff; font-size: 12px; font-weight: 600;
}
.att-card-title { flex: 1; font-size: 13px; font-weight: 600; color: #303133; }
.att-card-body {
  padding: 14px 14px 2px;
  display: grid; grid-template-columns: 1fr 1.1fr 1fr; gap: 10px 20px;
}
.att-card-body .att-field { margin-bottom: 10px; }
.att-card-body :deep(.el-form-item__label) { font-size: 13px; }
.att-fname { margin-left: 8px; font-size: 12px; color: #67c23a; }
</style>

