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
                <span class="detail-inline-label">单户申报金额：</span><span class="detail-money">¥ {{ formatAmount(row.amount) }}</span>
                <span class="sep">｜</span>
                <span class="detail-inline-label">审核状态：</span>
                <el-tag v-if="row.auditStatus === 'pending' || !row.auditStatus" type="warning" size="small">待审核</el-tag>
                <el-tag v-else-if="row.auditStatus === 'approved'" type="success" size="small">已通过</el-tag>
                <el-tag v-else-if="row.auditStatus === 'rejected'" type="danger" size="small">已驳回</el-tag>
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
                <span class="detail-inline-label">单设施申报金额：</span><span class="detail-money">¥ {{ formatAmount(row.amount) }}</span>
                <span class="sep">｜</span>
                <span class="detail-inline-label">审核状态：</span>
                <el-tag v-if="row.auditStatus === 'pending' || !row.auditStatus" type="warning" size="small">待审核</el-tag>
                <el-tag v-else-if="row.auditStatus === 'approved'" type="success" size="small">已通过</el-tag>
                <el-tag v-else-if="row.auditStatus === 'rejected'" type="danger" size="small">已驳回</el-tag>
                <el-tag v-else size="small">{{ row.auditStatus || '—' }}</el-tag>
              </span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="申报标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ !row._isDetail ? row.title : '' }}</template>
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
              <!-- 已通过且未立项 → 退回 -->
              <template v-else-if="row.auditStatus === 'approved'">
                <el-button link type="warning" size="small" @click="openRevertDialog(row)">退回</el-button>
              </template>
              <!-- 已立项 → 只读，无操作（MD 要求禁止撤销 / 再次审核） -->
              <template v-else-if="row.auditStatus === 'established'">
                <span class="muted-ops">已立项</span>
              </template>
              <!-- 已驳回 → 退回 -->
              <template v-else-if="row.auditStatus === 'rejected'">
                <el-button link type="warning" size="small" @click="openRevertDialog(row)">退回</el-button>
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
        @current-change="loadData"
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
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申报编号">{{ detailData.businessCode || '—' }}</el-descriptions-item>
          <el-descriptions-item label="申报标题">{{ detailData.title || '—' }}</el-descriptions-item>
          <el-descriptions-item label="所属乡镇">{{ detailData.townName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="所属村组">{{ detailData.villageGroupName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="申报类型">{{ dictLabel('shen_bao_lei_xing', detailData.declarationType) }}</el-descriptions-item>
          <el-descriptions-item label="申报类别">{{ dictLabel('shen_bao_lei_bie', detailData.declarationCategory) }}</el-descriptions-item>
          <el-descriptions-item label="申报金额">¥ {{ detailData.amount?.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detailData.status)">{{ dictLabel('declaration_status', detailData.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注说明" :span="2">{{ detailData.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 申报单级附件 -->
        <div class="detail-section">
          <div class="detail-section-title">📎 申报单级附件（{{ detailData.attachments?.length || 0 }}）</div>
          <el-table :data="detailData.attachments || []" border size="small" empty-text="暂无附件">
            <el-table-column label="附件类型" width="140">
              <template #default="{ row }">{{ dictLabel('fu_jian_lei_xing', row.attachmentType) }}</template>
            </el-table-column>
            <el-table-column label="文件名" min-width="200">
              <template #default="{ row }">
                <el-link v-if="row.fileKey" type="primary" @click="openAttachmentFile(row, true)">{{ row.fileName }}</el-link>
                <span v-else>{{ row.fileName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="150"><template #default="{ row }">{{ row.remark || '—' }}</template></el-table-column>
          </el-table>
        </div>

        <!-- 农户/公建明细 -->
        <div v-if="detailData.declarationCategory === '1'" class="detail-section">
          <div class="detail-section-title">👥 农户明细（{{ detailData.households?.length || 0 }}）</div>
          <el-table :data="detailData.households || []" border size="small" empty-text="暂无农户">
            <el-table-column label="户主" width="100" prop="snapshotHeadName" />
            <el-table-column label="身份证号" width="180" prop="snapshotHeadIdCard" />
            <el-table-column label="联系电话" width="130" prop="snapshotContactPhone" />
            <el-table-column label="户类型" width="110">
              <template #default="{ row }">{{ dictLabel('hu_lei_xing', row.snapshotHouseholdType) }}</template>
            </el-table-column>
            <el-table-column label="村组" min-width="130" prop="snapshotRegionName" />
          </el-table>
        </div>
        <div v-if="detailData.declarationCategory === '2'" class="detail-section">
          <div class="detail-section-title">🏗️ 公建设施明细（{{ detailData.facilities?.length || 0 }}）</div>
          <el-table :data="detailData.facilities || []" border size="small" empty-text="暂无设施">
            <el-table-column label="设施编号" width="140" prop="snapshotFacilityCode" />
            <el-table-column label="设施名称" min-width="180" prop="snapshotFacilityName" />
            <el-table-column label="类型" width="120">
              <template #default="{ row }">{{ dictLabel('she_shi_lei_xing', row.snapshotFacilityType) }}</template>
            </el-table-column>
            <el-table-column label="位置" min-width="180" prop="snapshotLocation" />
            <el-table-column label="村组" width="130" prop="snapshotRegionName" />
          </el-table>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 农户明细查看弹窗（只读） -->
    <el-dialog v-model="householdViewDialog.visible" title="农户申报详情" width="560px">
      <template v-if="householdViewDialog.data">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="户主姓名">{{ householdViewDialog.data.snapshotHeadName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ householdViewDialog.data.snapshotContactPhone }}</el-descriptions-item>
          <el-descriptions-item label="户类型">{{ dictLabel('hu_lei_xing', householdViewDialog.data.snapshotHouseholdType) }}</el-descriptions-item>
          <el-descriptions-item label="所属村组">{{ householdViewDialog.data.snapshotRegionName }}</el-descriptions-item>
          <el-descriptions-item label="身份证号" :span="2">{{ householdViewDialog.data.snapshotHeadIdCard }}</el-descriptions-item>
          <el-descriptions-item label="单户申报金额">
            <span style="color:#d4380d;font-weight:600">¥ {{ formatAmount(householdViewDialog.data.amount) }}</span>
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
            <span style="color:#d4380d;font-weight:600">¥ {{ formatAmount(facilityViewDialog.data.amount) }}</span>
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

    <!-- 明细退回弹窗：退回到未提交（父申报单回滚到 draft，让申报人可重新编辑） -->
    <el-dialog
      v-model="revertDialog.visible"
      title="退回明细（退回到未提交，可重新编辑）"
      width="480px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-alert
        type="warning" :closable="false" show-icon
        title="退回后，该申报单将回到'未提交'状态并清掉当前审核指派，申报人可重新编辑明细后再次提交。"
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
          <el-tag v-else size="small">{{ revertDialog.row?.auditStatus }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申报金额">¥ {{ formatAmount(revertDialog.row?.amount) }}</el-descriptions-item>
      </el-descriptions>
      <el-form :label-position="'top'" style="margin-top:12px">
        <el-form-item label="退回备注（给申报人看）">
          <el-input
            v-model="revertDialog.remark"
            type="textarea"
            :rows="3"
            placeholder="可写退回原因 / 修改建议，申报人重新编辑时能看到"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revertDialog.visible = false">取消</el-button>
        <el-button type="warning" :loading="revertDialog.submitting" @click="doRevertDetailDeclaration">
          确认退回（申报单变未提交）
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
              <el-input v-model="approvalDialog.form.approvalCode" placeholder="手动填写，如 LXR-20261111-001" maxlength="32" />
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
              <el-input :model-value="approvalDialog.form.townName" disabled placeholder="自动带出（选村组时联动）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="所属村组">
          <el-select v-model="approvalDialog.form.villageCodes" multiple collapse-tags collapse-tags-tooltip placeholder="选填，可多选" style="width:100%">
            <el-option v-for="v in approvalVillageGroupOpts" :key="v.value" :label="v.label" :value="v.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="建设依据" prop="remark">
          <el-input v-model="approvalDialog.form.remark" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请填写立项依据 / 必要性 / 预期效果" />
        </el-form-item>

        <!-- 被选中的申报单（自动带出 + 可增删） -->
        <el-form-item label="关联申报单">
          <div class="approval-declaration-list">
            <div v-for="(item, idx) in approvalDialog.declarationRows" :key="idx" class="decl-row">
              <span class="decl-index">#{{ idx + 1 }}</span>
              <el-input v-model="item.businessCode" placeholder="申报编号" disabled style="width:160px" />
              <el-input v-model="item.title" placeholder="申报标题" style="flex:1" disabled />
              <el-input :model-value="item.amount != null ? `¥ ${item.amount}` : ''" disabled style="width:120px;text-align:right" />
              <el-button type="danger" link size="small" @click="removeApprovalDecl(idx)">移除</el-button>
            </div>
            <el-button v-if="approvalDialog.declarationRows.length > 0" link type="primary" size="small" @click="addApprovalDecl">
              + 新增申报单（ID 手动输入）
            </el-button>
            <el-input
              v-else
              v-model="approvalDialog.newDeclId"
              placeholder="可手动追加申报单 ID"
              style="width:240px;margin-top:4px"
            />
          </div>
        </el-form-item>

        <!-- 立项资料附件：多行动态表单 -->
        <el-form-item label="立项资料附件">
          <div class="approval-attachment-list">
            <div v-for="(row, idx) in approvalDialog.form.attachments" :key="idx" class="att-row">
              <el-select v-model="row.attachmentType" placeholder="附件类型" style="width:180px">
                <el-option v-for="d in approvalAttachmentTypeOpts" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
              <el-upload
                :action="fileUploadAction"
                :headers="fileUploadHeaders"
                :data="{ bizModule: 'approval', bucket: 'archive-formal' }"
                :show-file-list="false"
                :before-upload="(file: File) => beforeApprovalAttUpload(file, idx)"
                :on-success="(res: any, _f: any) => onApprovalAttUploadSuccess(res, idx)"
                :on-error="onApprovalAttUploadError"
              >
                <el-button size="small">
                  <el-icon><UploadFilled /></el-icon>
                  {{ row.fileName ? '重新上传' : '上传文件' }}
                </el-button>
              </el-upload>
              <span v-if="row.fileName" class="att-fname">✓ {{ row.fileName }}</span>
              <el-input v-model="row.remark" placeholder="备注（选填）" style="flex:1" />
              <el-button type="danger" link size="small" @click="approvalDialog.form.attachments.splice(idx, 1)">删除</el-button>
            </div>
            <el-button
              link type="primary" size="small" icon="Plus"
              @click="approvalDialog.form.attachments.push({ attachmentType: '', fileKey: '', fileName: '', remark: '' })"
            >新增附件行</el-button>
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
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getDictByType } from '@/api/dict'
import { getPresignUrl } from '@/api/file'
import {
  getReceivedDeclarationPage,
  auditDeclarationDetail,
  revertDeclarationDetail,
  getDeclarationDetail,
  submitProjectApproval,
  type DeclarationQuery,
  type DeclarationVO,
  type DeclarationDetailFull,
  type DeclarationHouseholdDetail,
  type DeclarationFacilityDetail,
  type DeclarationDetailAuditDTO,
  type DeclarationDetailRevertDTO,
  type ProjectApprovalSubmitDTO,
  type ApprovalAttachmentItem
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

/**
 * 子明细行横向合并：
 * 总 10 列：col0=展开箭头 / col1=申报编号 / col2-8=中间7列 / col9=操作列(固定右侧)
 * 子明细行：col0 独立；col1 横向合并到 col8 共 8 列；col9 操作列独立
 * 父行：全部正常 1×1
 */
const ACTION_COL_INDEX = 9
function detailMergeMethod({ row, columnIndex }: { row: any; columnIndex: number }) {
  if (!row._isDetail) return { rowspan: 1, colspan: 1 }
  if (columnIndex === 0)           return { rowspan: 1, colspan: 1 }  // 展开箭头列独立
  if (columnIndex === ACTION_COL_INDEX) return { rowspan: 1, colspan: 1 }  // 操作列独立
  if (columnIndex === 1)           return { rowspan: 1, colspan: ACTION_COL_INDEX - 1 }  // col1→col8 合并 8 列
  return { rowspan: 0, colspan: 0 }  // 被合并掉的中间列
}
function isDetailRowClass({ row }: { row: any }) {
  return row._isDetail ? 'is-detail-row' : ''
}

// ================== 详情 弹窗（只读） ==================
const dialogVisible = ref(false)
const detailData = ref<any>({})

/** 打开申报详情（防重入：先关再开防止叠层） */
function openDetail(row: DeclarationVO) {
  if (dialogVisible.value) {
    dialogVisible.value = false
    nextTick(() => {
      dialogVisible.value = true
      getDeclarationDetail(row.id).then((r) => { detailData.value = (r.data as any) || {} })
    })
  } else {
    dialogVisible.value = true
    getDeclarationDetail(row.id).then((r) => { detailData.value = (r.data as any) || {} })
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

// ================== 明细退回弹窗（明细 → pending，父申报单 → draft，清掉 recipient） ==================
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
    ElMessage.success('已退回，申报单回到未提交状态')
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
const approvalVillageGroupOpts = ref<{ value: string; label: string }[]>([])

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
    villageCodes: [] as string[],
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
  approvalDialog.form.villageCodes = []
  approvalDialog.form.attachments = []
  approvalDialog.newDeclId = ''

  // 自动带出默认编号（LXR-YYYYMMDD-NNN）
  const today = new Date()
  const ymd = `${today.getFullYear()}${String(today.getMonth() + 1).padStart(2, '0')}${String(today.getDate()).padStart(2, '0')}`
  approvalDialog.form.approvalCode = `LXR-${ymd}-001`

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
      villageCodes: approvalDialog.form.villageCodes,
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
</script>

<style scoped>
.declaration-page { padding: 0; }
.query-card { margin-bottom: 12px; }
.tab-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.tab-bar .status-tabs { flex: 1; }
.tab-bar :deep(.el-tabs__header) { margin: 0; margin-bottom: 0; }

.detail-mode .detail-section { margin-top: 16px; }
.detail-mode .detail-section-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; color: #303133; }

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
</style>

