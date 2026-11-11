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
        <el-button v-if="store.hasPerm('declaration:add')" type="primary" icon="Plus" @click="openAdd">
          新建申报
        </el-button>
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
      >
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
                <span class="detail-inline-label">明细状态：</span>
                <!-- 组合判断：auditStatus + 父表 status 区分 未提交/待重提/待审核 -->
                <el-tag
                  v-if="(row.auditStatus === 'unsubmitted') || (!row.auditStatus)"
                  type="info" size="small"
                >未提交</el-tag>
                <el-tag
                  v-else-if="row.auditStatus === 'pending' && row._parentStatus === 'draft'"
                  type="warning" size="small"
                >待重提</el-tag>
                <el-tag
                  v-else-if="row.auditStatus === 'pending'"
                  type="warning" size="small"
                >待审核</el-tag>
                <el-tag v-else-if="row.auditStatus === 'approved'" type="success" size="small">已通过</el-tag>
                <el-tag v-else-if="row.auditStatus === 'rejected'" type="danger" size="small">已驳回</el-tag>
                <el-tag v-else-if="row.auditStatus === 'established'" type="primary" size="small">已立项</el-tag>
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
                <span class="detail-inline-label">明细状态：</span>
                <el-tag
                  v-if="(row.auditStatus === 'unsubmitted') || (!row.auditStatus)"
                  type="info" size="small"
                >未提交</el-tag>
                <el-tag
                  v-else-if="row.auditStatus === 'pending' && row._parentStatus === 'draft'"
                  type="warning" size="small"
                >待重提</el-tag>
                <el-tag
                  v-else-if="row.auditStatus === 'pending'"
                  type="warning" size="small"
                >待审核</el-tag>
                <el-tag v-else-if="row.auditStatus === 'approved'" type="success" size="small">已通过</el-tag>
                <el-tag v-else-if="row.auditStatus === 'rejected'" type="danger" size="small">已驳回</el-tag>
                <el-tag v-else-if="row.auditStatus === 'established'" type="primary" size="small">已立项</el-tag>
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
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <!-- ===== 父行：申报单操作（按明细 auditStatus 计算，解耦主表 status）===== -->
            <template v-if="!row._isDetail">
              <el-button link type="primary" @click="openDetail(row)">查看</el-button>
              <el-button
                v-if="store.hasPerm('declaration:edit') && canParentEdit(row)"
                link type="primary" @click="openEdit(row)"
              >编辑</el-button>
              <el-button
                v-if="canParentSubmit(row)"
                link type="success" @click="openSubmitDialog(row)"
              >{{ canParentResubmit(row) ? '重新提交' : '提交' }}</el-button>
              <el-button
                v-if="store.hasPerm('declaration:delete') && row.status === 'draft'"
                link type="danger" @click="handleDelete(row)"
              >删除</el-button>
            </template>
            <!-- ===== 子行：农户/公建设施明细操作（按明细 auditStatus 显隐）===== -->
            <template v-else>
              <el-button
                v-if="row._detailKind === 'household'"
                link type="primary" size="small" @click="openHouseholdDetailView(row)"
              >查看</el-button>
              <el-button
                v-else
                link type="primary" size="small" @click="openFacilityDetailView(row)"
              >查看</el-button>
              <el-button
                v-if="activeStatusTab !== 'approved' && activeStatusTab !== 'established' && (row._parentStatus === 'draft' || row.auditStatus === 'rejected')"
                link type="primary" size="small" @click="openDetailEdit(row._detailKind as any, row._parentRef, row)"
              >编辑</el-button>
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

    <!-- ========== 新建 / 编辑 / 详情 弹窗 ========== -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="880px"
      destroy-on-close
      append-to-body
      @open="onDialogOpen"
    >
      <!-- 详情模式：展示树形结构 + Tab 切换基本信息 / 明细审核记录 -->
      <div v-if="dialogMode === 'detail'" class="detail-mode">
        <el-tabs v-model="detailTab" class="detail-tabs">
          <!-- ============ Tab 1：基本信息 ============ -->
          <el-tab-pane label="基本信息" name="info">
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
                <el-table-column label="户号" width="160" prop="snapshotHouseholdNo" />
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
          </el-tab-pane>

          <!-- ============ Tab 2：明细审核记录 ============ -->
          <el-tab-pane label="明细审核记录" name="audit-log">
            <el-table
              :data="allDetailAuditLogs"
              border size="small"
              empty-text="暂无审核记录（未进入审核流程）"
            >
              <el-table-column label="序号" type="index" width="60" />
              <el-table-column label="明细类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="row._kind === 'household' ? 'primary' : ''" size="small">
                    {{ row._kind === 'household' ? '农户' : '公建设施' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="明细名称" min-width="160">
                <template #default="{ row }">{{ row._displayName }}</template>
              </el-table-column>
              <el-table-column label="审核状态" width="110">
                <template #default="{ row }">
                  <el-tag
                    v-if="row.auditStatus === 'unsubmitted'" type="info" size="small">未提交</el-tag>
                  <el-tag v-else-if="row.auditStatus === 'pending'" type="warning" size="small">待审核</el-tag>
                  <el-tag v-else-if="row.auditStatus === 'approved'" type="success" size="small">已通过</el-tag>
                  <el-tag v-else-if="row.auditStatus === 'rejected'" type="danger" size="small">已驳回</el-tag>
                  <el-tag v-else type="info" size="small">{{ row.auditStatus || '—' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="审核人" width="120">
                <template #default="{ row }">{{ row.auditByName || '—' }}</template>
              </el-table-column>
              <el-table-column label="审核时间" width="170">
                <template #default="{ row }">{{ row.auditTime || '—' }}</template>
              </el-table-column>
              <el-table-column label="审核意见" min-width="200">
                <template #default="{ row }">{{ row.auditRemark || '—' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 新增/编辑模式 -->
      <div v-else>
        <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="申报编号">
                <el-input v-model="form.businessCode" placeholder="手动填写（留空则系统自动生成）" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label=" " label-width="20px">
                <span style="color:#909399;font-size:12px;line-height:32px">留空由系统自动生成</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item label="申报标题" prop="title">
                <el-input v-model="form.title" placeholder="如：XX户灾后重建申请" maxlength="128" show-word-limit />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="所属乡镇" prop="townCode">
                <el-input :model-value="userTownName" disabled placeholder="系统自动回填" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="所属村组" prop="villageGroupCode">
                <el-select
                  v-model="form.villageGroupCode"
                  placeholder="请选择"
                  style="width: 100%"
                  :loading="vgLoading"
                  @change="onVillageGroupChange"
                >
                  <el-option
                    v-for="item in villageGroupOpts"
                    :key="item.code"
                    :label="item.name"
                    :value="item.code"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="申报类型" prop="declarationType">
                <el-select v-model="form.declarationType" placeholder="请选择" style="width: 100%">
                  <el-option v-for="d in declarationTypeOpts" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="申报类别" prop="declarationCategory">
                <el-select v-model="form.declarationCategory" placeholder="请选择" style="width: 100%" @change="onCategoryChange" :disabled="dialogMode === 'edit'">
                  <el-option v-for="d in declarationCategoryOpts" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="申报金额" prop="amount">
                <el-input-number
                  v-model="form.amount"
                  :min="0.01"
                  :precision="2"
                  :step="100"
                  style="width: 100%"
                  placeholder="元"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- ============ 联动区块：到户项目 → 农户多选 ============ -->
          <div v-if="form.declarationCategory === '1'" class="section-block">
            <el-divider content-position="left" class="section-divider">
              <span>👥 申请人（农户多选）</span>
              <span class="section-tip">需选择所属村组后加载农户列表，至少勾选 1 户</span>
            </el-divider>
            <el-table
              ref="hhTableRef"
              :data="householdPool"
              border
              size="small"
              empty-text="请先选择所属村组"
              :loading="hhLoading"
              @selection-change="onHouseholdSelectionChange"
            >
              <el-table-column type="selection" width="50" />
              <el-table-column prop="headName" label="户主" width="100" />
              <el-table-column prop="householdNo" label="户号" min-width="160" />
              <el-table-column prop="contactPhone" label="联系电话" width="130" />
              <el-table-column label="户类型" width="110">
                <template #default="{ row }">{{ dictLabel('hu_lei_xing', row.householdType) }}</template>
              </el-table-column>
            </el-table>
          </div>

          <!-- ============ 联动区块：公建项目 → 公建设施多选 ============ -->
          <div v-if="form.declarationCategory === '2'" class="section-block">
            <el-divider content-position="left" class="section-divider">
              <span>🏗️ 公共基础设施（多选）</span>
              <span class="section-tip">需选择所属村组后加载公建设施列表，至少勾选 1 个</span>
            </el-divider>
            <el-table
              ref="facTableRef"
              :data="facilityPool"
              border
              size="small"
              empty-text="请先选择所属村组"
              :loading="facLoading"
              @selection-change="onFacilitySelectionChange"
            >
              <el-table-column type="selection" width="50" />
              <el-table-column prop="facilityName" label="设施名称" min-width="180" />
              <el-table-column prop="facilityCode" label="设施编号" width="140" />
              <el-table-column label="设施类型" width="120">
                <template #default="{ row }">{{ dictLabel('she_shi_lei_xing', row.facilityType) }}</template>
              </el-table-column>
              <el-table-column prop="location" label="位置" min-width="180" show-overflow-tooltip />
            </el-table>
          </div>

          <!-- ============ 备注 ============ -->
          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item label="备注说明">
                <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="3"
                  placeholder="填写申报原因、情况说明"
                  maxlength="1024"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- ============ 申报材料附件（动态多行，上限30） ============ -->
          <div class="section-block">
            <el-divider content-position="left" class="section-divider">
              <span>📎 申报材料附件</span>
              <el-button
                link
                type="primary"
                :icon="Plus"
                :disabled="form.attachments.length >= 30"
                @click="addAttachmentRow"
              >
                添加一组附件（{{ form.attachments.length }}/30）
              </el-button>
            </el-divider>
            <div v-if="!form.attachments.length" class="empty-tip">
              暂无附件，点击上方按钮添加
            </div>
            <div v-for="(row, idx) in form.attachments" :key="idx" class="attachment-row">
              <div class="attachment-row-header">
                <span class="attachment-row-index">#{{ idx + 1 }}</span>
                <el-button link type="danger" icon="Delete" @click="removeAttachmentRow(idx)">删除</el-button>
              </div>
              <div class="attachment-row-body">
                <div class="attachment-field">
                  <label>附件类型</label>
                  <el-select v-model="row.attachmentType" placeholder="请选择" style="width: 100%">
                    <el-option v-for="d in attachmentTypeOpts" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
                  </el-select>
                </div>
                <div class="attachment-field">
                  <label>上传文件</label>
                  <el-upload
                    :auto-upload="false"
                    :show-file-list="false"
                    @change="(file: any) => handleAttachmentFileChange(file, row)"
                  >
                    <div class="upload-trigger">
                      <el-icon><Upload /></el-icon>
                      <span>{{ row.fileName || '点击选择文件' }}</span>
                    </div>
                  </el-upload>
                </div>
                <div class="attachment-field attachment-field-wide">
                  <label>备注</label>
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
        >确定</el-button>
      </template>
    </el-dialog>

    <!-- 明细级编辑弹窗（改金额 + 附件） -->
    <el-dialog
      v-model="detailEditDialog.visible"
      :title="detailEditDialog.kind === 'household' ? '编辑农户明细' : '编辑公建设施明细'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form label-width="120px" :model="detailEditDialog.data">
        <el-form-item label="事项说明">
          <el-input
            :model-value="(detailEditDialog.data as any)?.remark"
            @update:model-value="(v: any) => ((detailEditDialog.data as any).remark = v)"
            type="textarea"
            :rows="3"
            placeholder="填写该户/该设施的申报原因或情况说明"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="单户申报金额" v-if="detailEditDialog.kind === 'household'">
          <el-input-number
            v-model="(detailEditDialog.data as any).amount"
            :min="0.01" :precision="2" :step="100" style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="单设施申报金额" v-else>
          <el-input-number
            v-model="(detailEditDialog.data as any).amount"
            :min="0.01" :precision="2" :step="100" style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <div class="detail-attachments">
        <div class="detail-attach-header">
          <span>明细级附件（{{ detailAttachments.length }}/30）</span>
          <el-button type="primary" link :disabled="detailAttachments.length >= 30" @click="addDetailAttachmentRow">+ 添加一组附件</el-button>
        </div>
        <div v-if="!detailAttachments.length" class="detail-attach-empty">暂无附件，点击上方按钮添加</div>
        <div v-for="(_, idx) in detailAttachments" :key="idx" class="detail-attach-row">
          <el-select v-model="detailAttachments[idx].attachmentType" placeholder="附件类型" style="width: 140px">
            <el-option v-for="d in attachmentTypeOpts" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            :disabled="!!detailAttachments[idx].fileKey"
            @change="(file: any) => handleDetailAttachmentFileChange(file, idx)"
          >
            <div class="upload-trigger detail-upload-trigger">
              <el-icon><Upload /></el-icon>
              <span class="attach-file-name">{{ detailAttachments[idx].fileName || '点击选择文件' }}</span>
            </div>
          </el-upload>
          <el-input v-model="detailAttachments[idx].remark" placeholder="备注" style="flex: 1" />
          <el-button type="danger" link @click="removeDetailAttachmentRow(idx)">删除</el-button>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailEditDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="detailEditDialog.submitting" @click="saveDetailEdit">保存</el-button>
      </template>
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

    <!-- 提交弹窗：选择接收审核人 + 二次确认 -->
    <el-dialog
      v-model="submitDialog.visible"
      :title="`提交申报：${submitDialog.row?.businessCode || ''}`"
      width="520px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form label-width="110px">
        <el-form-item label="申报标题">
          <el-input :model-value="submitDialog.row?.title" disabled />
        </el-form-item>
        <el-form-item label="接收审核人" required>
          <el-select
            v-model="submitDialog.recipientUserId"
            filterable
            remote
            :remote-method="searchRecipients"
            :loading="submitDialog.recipientLoading"
            placeholder="输入姓名/账号搜索并选择审核人"
            style="width: 100%"
          >
            <el-option
              v-for="u in submitDialog.recipientList"
              :key="u.id"
              :label="`${u.realName}（${u.username}）${u.regionName ? ' · ' + u.regionName : ''}`"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <div class="confirm-tip">
          ⚠️ 提交后申报单状态变为<strong>待审核</strong>，<br/>
          将发送给 <strong>{{ selectedRecipientLabel }}</strong> 审核，<br/>
          提交后不再允许编辑、删除。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="submitDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitDialog.submitting" :disabled="!submitDialog.recipientUserId" @click="doSubmitDeclaration">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getDictByType } from '@/api/dict'
import { uploadFile as uploadFileToServer, getPresignUrl } from '@/api/file'
import {
  getDeclarationPage,
  createDeclaration,
  updateDeclaration,
  deleteDeclaration,
  submitDeclaration,
  getRecipients,
  getDeclarationDetail,
  getCurrentUserRegion,
  getVillageGroups,
  getHouseholdsByVillageGroup,
  getFacilitiesByVillageGroup,
  editHouseholdDetail,
  editPublicFacilityDetail,
  type DeclarationQuery,
  type DeclarationSaveDTO,
  type DeclarationVO,
  type UserRegionVO,
  type VillageGroupOption,
  type HouseholdOption,
  type FacilityOption,
  type DeclarationDetailFull,
  type DeclarationHouseholdDetail,
  type DeclarationFacilityDetail,
  type DeclarationDetailEditDTO,
  type DeclarationSubmitDTO,
  type RecipientOption
} from '@/api/declaration'

const store = useUserStore()
type DialogMode = 'add' | 'edit' | 'detail'

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
// 状态页签（申报提交页：待审核 → 已通过 → 已驳回 → 已立项）
const statusTabs = [
  { label: '待审核', name: 'submitted' },
  { label: '已通过', name: 'approved' },
  { label: '已驳回', name: 'rejected' },
  { label: '已立项', name: 'established' }
]
// 默认落在"待审核"
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
    const res: any = await getDeclarationPage(params)
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

/** 把父行里挂的 households + facilities 明细扁平化成一个数组 */
function flatDetails(row: any): any[] {
  return [
    ...(row?.households || []),
    ...(row?.facilities || [])
  ]
}
/**
 * 父行是否可编辑：
 *  - 待审核页签（submitted，含被"撤销"回来的单）→ 审核中一律只读，申报人不可编辑
 *  - 已通过/已立项页签 → 一律不可编辑
 *    注：后端用明细 auditStatus 匹配页签，"已通过"页签里主表 status 不一定是 approved，
 *        必须按页签名判断，不能靠 row.status === 'approved' 拦截
 *  - draft / rejected 主表 → 可编辑
 */
function canParentEdit(row: any): boolean {
  // 已通过/已立项页签一律不可编辑；其他页签（待审核、已驳回）按主表+明细状态判断
  if (activeStatusTab.value === 'approved' || activeStatusTab.value === 'established') return false
  if (row.status === 'draft' || row.status === 'rejected') return true
  return flatDetails(row).some(d => d.auditStatus === 'rejected')
}
/**
 * 父行是否可显示"提交/重新提交"按钮：
 *  - 已通过/已立项页签 → 一律不显示
 *  - draft / rejected 主表 → 可提交/重新提交
 *  - submitted 主表且存在 rejected 明细 → 可重新提交
 */
function canParentSubmit(row: any): boolean {
  if (activeStatusTab.value === 'approved' || activeStatusTab.value === 'established') return false
  if (row.status === 'draft' || row.status === 'rejected') return true
  return flatDetails(row).some(d => d.auditStatus === 'rejected')
}
/**
 * 父行按钮文案是否显示为"重新提交"：
 *  - approved 主表 → 不进这里（canParentSubmit 拦了）
 *  - rejected 主表 / submitted 主表有 rejected 明细 → "重新提交"
 *  - draft 主表下明细非全 unsubmitted → 退回场景 "重新提交"
 *  - 否则 "提交"（首次）
 */
function canParentResubmit(row: any): boolean {
  if (row.status === 'rejected') return true
  const details = flatDetails(row)
  if (row.status === 'draft') {
    return details.some(d => d.auditStatus && d.auditStatus !== 'unsubmitted')
  }
  return details.some(d => d.auditStatus === 'rejected')
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

// ================== 明细级编辑弹窗 ==================
type DetailKind = 'household' | 'public_facility'
const detailEditDialog = reactive({
  visible: false,
  kind: 'household' as DetailKind,
  parentRow: null as DeclarationVO | null,
  data: null as (DeclarationHouseholdDetail | DeclarationFacilityDetail | null),
  submitting: false
})
const detailAttachments = ref<Array<{ attachmentId?: string; attachmentType?: string; remark?: string; fileName?: string; fileKey?: string; bucket?: string; fileSize?: number }>>([])
// attachmentTypeOpts 复用上面已有的（L524）

function openDetailEdit(kind: DetailKind, parentRow: DeclarationVO, detail: DeclarationHouseholdDetail | DeclarationFacilityDetail) {
  detailEditDialog.kind = kind
  detailEditDialog.parentRow = parentRow
  detailEditDialog.data = detail
  detailEditDialog.visible = true
  // 附件列表拷贝一份到 detailAttachments（前端编辑态）
  const atts = (detail as any).attachments || []
  detailAttachments.value = atts.map((a: any) => ({
    attachmentId: a.id,
    attachmentType: a.attachmentType,
    remark: a.remark,
    fileName: a.fileName,
    fileKey: a.fileKey,
    bucket: a.bucket,
    fileSize: a.fileSize
  }))
  if (!attachmentTypeOpts.value.length) {
    getDictByType('fu_jian_lei_xing').then(r => { attachmentTypeOpts.value = r.data || [] })
  }
}
function addDetailAttachmentRow() {
  if (detailAttachments.value.length >= 30) {
    ElMessage.warning('附件上限30组')
    return
  }
  detailAttachments.value.push({})
}
function removeDetailAttachmentRow(idx: number) {
  detailAttachments.value.splice(idx, 1)
}
async function handleDetailAttachmentFileChange(file: any, idx: number) {
  if (!file?.raw) return
  const name = file.name as string
  if (!/\.(pdf|doc|docx|xls|xlsx|ppt|pptx|png|jpg|jpeg|gif|bmp|webp|zip|rar|7z|txt|csv)$/i.test(name)) {
    ElMessage.error('不支持的文件类型')
    return
  }
  try {
    const res = await uploadFileToServer(file.raw)
    const up = res.data as any
    detailAttachments.value[idx].fileName = up.fileName || name
    detailAttachments.value[idx].fileKey = up.fileKey
    detailAttachments.value[idx].bucket = up.bucket
    detailAttachments.value[idx].fileSize = up.fileSize
    ElMessage.success(`上传成功：${detailAttachments.value[idx].fileName}`)
  } catch { /* 拦截器已弹 */ }
}
async function saveDetailEdit() {
  if (!detailEditDialog.data || !detailEditDialog.parentRow) return
  const dto: DeclarationDetailEditDTO = {
    detailId: detailEditDialog.data.id,
    declarationId: detailEditDialog.parentRow.id,
    amount: (detailEditDialog.data as any).amount ?? undefined,
    remark: (detailEditDialog.data as any).remark ?? undefined,
    attachments: detailAttachments.value.length ? detailAttachments.value : undefined
  }
  detailEditDialog.submitting = true
  try {
    if (detailEditDialog.kind === 'household') {
      await editHouseholdDetail(dto)
    } else {
      await editPublicFacilityDetail(dto)
    }
    ElMessage.success('保存成功')
    detailEditDialog.visible = false
    // 保存后刷新整个列表（树形结构重新构建）
    loadData()
  } catch (e) {
    // 错误已由拦截器弹出
  } finally {
    detailEditDialog.submitting = false
  }
}

// ================== 弹窗：状态 ==================
const dialogVisible = ref(false)
const dialogMode = ref<DialogMode>('add')
const dialogTitle = computed(() => {
  if (dialogMode.value === 'add') return '新建申报'
  if (dialogMode.value === 'edit') return '编辑申报'
  return '申报详情'
})
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

// 详情用
const detailData = ref<any>({})
/** 查看弹窗的 Tab 切换：基本信息 / 明细审核记录 */
const detailTab = ref<'info' | 'audit-log'>('info')
/**
 * 明细审核记录的合并列表（detail 模式下同时展示农户 + 公建设施）
 * 每条：明细自身字段 + _kind('household'/'public_facility') + _displayName（前端展示名）
 */
const allDetailAuditLogs = computed(() => {
  const list: any[] = []
  for (const h of (detailData.value.households || [])) {
    list.push({
      ...h,
      _kind: 'household',
      _displayName: h.snapshotHeadName || '未知户主'
    })
  }
  for (const f of (detailData.value.facilities || [])) {
    list.push({
      ...f,
      _kind: 'public_facility',
      _displayName: f.snapshotFacilityName || '未知设施'
    })
  }
  return list
})

// 当前登录人乡镇（弹窗初始化时拿）
const userTown = ref<UserRegionVO>({})
const userTownName = computed(() => userTown.value.townName || userTown.value.currentRegionName || '未绑定区划')

// 弹窗表单
interface AttachmentRow {
  attachmentType?: string
  fileName?: string
  fileKey?: string
  bucket?: string
  fileSize?: number
  remark?: string
}
const form = reactive<DeclarationSaveDTO & { attachments: AttachmentRow[] }>({
  title: '',
  businessCode: '',  // 申报编号（预生成、只读）
  townCode: '',
  villageGroupCode: '',
  declarationType: '',
  declarationCategory: '',
  amount: null as unknown as number,
  remark: '',
  householdIds: [],
  facilityIds: [],
  attachments: []
})

const formRules: FormRules = {
  title: [{ required: true, message: '请输入申报标题', trigger: 'blur' }],
  townCode: [{ required: true, message: '所属乡镇不能为空', trigger: 'change' }],
  villageGroupCode: [{ required: true, message: '请选择所属村组', trigger: 'change' }],
  declarationType: [{ required: true, message: '请选择申报类型', trigger: 'change' }],
  declarationCategory: [{ required: true, message: '请选择申报类别', trigger: 'change' }],
  amount: [
    { required: true, message: '请填写申报金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '金额必须大于0', trigger: 'blur' }
  ]
}

// 下拉数据源
const villageGroupOpts = ref<VillageGroupOption[]>([])
const vgLoading = ref(false)
const householdPool = ref<HouseholdOption[]>([])
const householdSelection = ref<HouseholdOption[]>([])
const hhLoading = ref(false)
const facLoading = ref(false)
const facilityPool = ref<FacilityOption[]>([])
const facilitySelection = ref<FacilityOption[]>([])
const hhTableRef = ref()
const facTableRef = ref()

/**
 * edit 模式下缓存后端返回的已保存明细（含 amount / remark / attachments），
 * 用于 selection-change 里判断"取消勾选的那户/设施是否已有资料 → 弹二次确认"。
 * add 模式下保持空，不拦截。
 */
const savedHouseholdDetails = ref<any[]>([])
const savedFacilityDetails = ref<any[]>([])

/** 判断某条已保存明细是否"已有资料"（金额/说明/附件任一非空） */
function hasSavedDetailResources(saved: any): boolean {
  if (!saved) return false
  if (saved.amount != null) return true
  if (saved.remark) return true
  const atts = saved.attachments || []
  if (atts.length > 0) return true
  return false
}

// ================== 弹窗打开时 ==================
async function onDialogOpen() {
  if (dialogMode.value === 'detail') return
  // 拿当前登录人乡镇
  const ur = await getCurrentUserRegion()
  userTown.value = ur.data || {}
  if (!userTown.value.townCode) {
    ElMessage.error('未找到您所属的乡镇区划，请联系管理员绑定区划')
    dialogVisible.value = false
    return
  }
  form.townCode = userTown.value.townCode
  // 拉村组列表
  await loadVillageGroups()
  // 新建模式：默认选第一个村组；编辑模式：保留 openEdit 回调里将要回填的 villageGroupCode，不要强制覆盖
  if (dialogMode.value === 'add' && villageGroupOpts.value.length > 0) {
    form.villageGroupCode = villageGroupOpts.value[0].code
  }
  // 编辑模式下 detail 回填由 openEdit 里 getDeclarationDetail 的异步回调负责（回填时会按已保存的村组自动拉农户/公建池并勾选）
}

async function loadVillageGroups() {
  if (!userTown.value.townCode) return
  vgLoading.value = true
  try {
    const res = await getVillageGroups(userTown.value.townCode)
    villageGroupOpts.value = res.data || []
  } finally {
    vgLoading.value = false
  }
}

async function loadHouseholds() {
  if (!form.villageGroupCode) return
  hhLoading.value = true
  try {
    const res = await getHouseholdsByVillageGroup(form.villageGroupCode)
    householdPool.value = res.data || []
    householdSelection.value = []
  } finally {
    hhLoading.value = false
  }
}

async function loadFacilities() {
  if (!form.villageGroupCode) return
  facLoading.value = true
  try {
    const res = await getFacilitiesByVillageGroup(form.villageGroupCode)
    facilityPool.value = res.data || []
    facilitySelection.value = []
  } finally {
    facLoading.value = false
  }
}

// ================== 联动 ==================
function onVillageGroupChange() {
  // 清空当前勾选 + 重新加载新村组下的农户/公建
  householdSelection.value = []
  facilitySelection.value = []
  if (form.declarationCategory === '1') loadHouseholds()
  else loadFacilities()
}

function onCategoryChange() {
  // 切换类别：清空另一块的勾选 + 立即加载当前类别数据源（如果已选村组）
  if (form.declarationCategory === '1') {
    facilitySelection.value = []
    form.facilityIds = []
    loadHouseholds()
  } else {
    householdSelection.value = []
    form.householdIds = []
    loadFacilities()
  }
}

/**
 * 开关：openEdit 回填 toggleRowSelection 时也会触发 selection-change，
 * 用这个 flag 跳过拦截；弹框后勾回行也会触发，同样跳过。
 * 必须声明在 onHouseholdSelectionChange / onFacilitySelectionChange 之前，否则运行时 TDZ 报错。
 */
const _suppressSelectGuard = ref(false)

/**
 * 农户多选 change：edit 模式下如果用户"取消勾选"了某户，且该户在后端已保存过 amount/remark/附件 → 弹确认框，
 * 用户点取消则把该行再勾回去。add 模式 / 回填阶段直接放行。
 */
function onHouseholdSelectionChange(selection: HouseholdOption[]) {
  // 回填阶段（openEdit 里 toggleRowSelection 也会触发）直接放行，避免自己拦自己
  if (_suppressSelectGuard.value) { householdSelection.value = selection; return }

  // 只有 edit 模式下且 savedHouseholdDetails 有数据时才做拦截
  if (dialogMode.value === 'edit' && savedHouseholdDetails.value.length) {
    const newIds = new Set(selection.map((s) => String(s.id)))
    const prevSel = householdSelection.value
    // 差集：之前选中、现在不在 selection 里的那些行（被取消勾选）
    const removed = prevSel.filter((p) => !newIds.has(String(p.id)))
    if (removed.length) {
      // 找出 removed 里哪些在 savedHouseholdDetails 里已有资料
      const withResources = removed.filter((p) => {
        const saved = savedHouseholdDetails.value.find((s) => String(s.householdId) === String(p.id))
        return hasSavedDetailResources(saved)
      })
      if (withResources.length) {
        ElMessageBox.confirm(
          `已填写资料的农户（${withResources.map((p) => p.headName).join('、')}）取消勾选后，其申报金额/事项说明/附件将一并删除。\n\n确定取消勾选吗？`,
          '确认删除农户',
          { confirmButtonText: '确定取消', cancelButtonText: '保留勾选', type: 'warning', customClass: 'decl-detail-uncheck-dialog' }
        ).then(() => {
          // 用户确认 → 放行，更新 selection
          householdSelection.value = selection
        }).catch(() => {
          // 用户取消 → 把被取消的行重新勾回去
          _suppressSelectGuard.value = true
          nextTick(() => {
            withResources.forEach((p) => hhTableRef.value?.toggleRowSelection(p, true))
            _suppressSelectGuard.value = false
          })
        })
        return  // 本次 change 先拦住，等确认回调再说
      }
    }
  }
  householdSelection.value = selection
}

/** 公建设施多选 change：逻辑同农户 */
function onFacilitySelectionChange(selection: FacilityOption[]) {
  if (_suppressSelectGuard.value) { facilitySelection.value = selection; return }
  if (dialogMode.value === 'edit' && savedFacilityDetails.value.length) {
    const newIds = new Set(selection.map((s) => String(s.id)))
    const prevSel = facilitySelection.value
    const removed = prevSel.filter((p) => !newIds.has(String(p.id)))
    if (removed.length) {
      const withResources = removed.filter((p) => {
        const saved = savedFacilityDetails.value.find((s) => String(s.facilityId) === String(p.id))
        return hasSavedDetailResources(saved)
      })
      if (withResources.length) {
        ElMessageBox.confirm(
          `已填写资料的公建设施（${withResources.map((p) => p.facilityName).join('、')}）取消勾选后，其申报金额/事项说明/附件将一并删除。\n\n确定取消勾选吗？`,
          '确认删除公建设施',
          { confirmButtonText: '确定取消', cancelButtonText: '保留勾选', type: 'warning', customClass: 'decl-detail-uncheck-dialog' }
        ).then(() => {
          facilitySelection.value = selection
        }).catch(() => {
          _suppressSelectGuard.value = true
          nextTick(() => {
            withResources.forEach((p) => facTableRef.value?.toggleRowSelection(p, true))
            _suppressSelectGuard.value = false
          })
        })
        return
      }
    }
  }
  facilitySelection.value = selection
}

// ================== 附件 ==================
function addAttachmentRow() {
  if (form.attachments.length >= 30) {
    ElMessage.warning('附件最多30组')
    return
  }
  form.attachments.push({})
}
function removeAttachmentRow(idx: number) {
  form.attachments.splice(idx, 1)
}
async function handleAttachmentFileChange(file: any, row: AttachmentRow) {
  if (!file?.raw) return
  // 校验后缀白名单（后端还会再校验）
  const name = file.name as string
  if (!/\.(pdf|doc|docx|xls|xlsx|ppt|pptx|png|jpg|jpeg|gif|bmp|webp|zip|rar|7z|txt|csv)$/i.test(name)) {
    ElMessage.error('不支持的文件类型')
    return
  }
  try {
    const res = await uploadFileToServer(file.raw)
    const up = res.data as any
    row.fileName = up.fileName || name
    row.fileKey = up.fileKey
    row.bucket = up.bucket
    row.fileSize = up.fileSize
    ElMessage.success(`上传成功：${row.fileName}`)
  } catch {
    // ElMessage 已拦截
  }
}
async function openAttachmentFile(file: any, download = false) {
  if (!file?.fileKey) return
  const res = await getPresignUrl({ bucket: file.bucket, fileKey: file.fileKey, fileName: file.fileName, download })
  window.open(res.data as string, '_blank')
}

// ================== 提交 ==================
async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 明细必填校验
  if (form.declarationCategory === '1') {
    if (!householdSelection.value.length) {
      ElMessage.warning('到户项目必须至少勾选 1 户农户')
      return
    }
    form.householdIds = householdSelection.value.map((h) => h.id)
    form.facilityIds = []
  } else {
    if (!facilitySelection.value.length) {
      ElMessage.warning('公建项目必须至少勾选 1 个公共基础设施')
      return
    }
    form.facilityIds = facilitySelection.value.map((f) => f.id)
    form.householdIds = []
  }

  submitLoading.value = true
  try {
    if (dialogMode.value === 'add') {
      await createDeclaration({ ...form } as any)
      ElMessage.success('新增成功')
    } else {
      await updateDeclaration({ ...form } as any)
      ElMessage.success('编辑成功')
    }
    dialogVisible.value = false
    resetForm()
    loadData()
  } finally {
    submitLoading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    title: '',
    businessCode: '',
    townCode: '',
    villageGroupCode: '',
    declarationType: '',
    declarationCategory: '',
    amount: null as unknown as number,
    remark: '',
    householdIds: [],
    facilityIds: [],
    attachments: []
  })
  householdPool.value = []
  facilityPool.value = []
  householdSelection.value = []
  facilitySelection.value = []
  villageGroupOpts.value = []
  userTown.value = {}
  savedHouseholdDetails.value = []
  savedFacilityDetails.value = []
}

// ================== 列表按钮 ==================
function openAdd() {
  resetForm()
  dialogMode.value = 'add'
  dialogVisible.value = true
}
function openDetail(row: DeclarationVO) {
  // 防重入：如果弹窗已经在开（用户可能没关就又点了查看），先关掉再重新打开，防止叠层
  if (dialogVisible.value) {
    dialogVisible.value = false
    nextTick(() => {
      dialogMode.value = 'detail'
      dialogVisible.value = true
      detailTab.value = 'info'
      getDeclarationDetail(row.id).then((r) => { detailData.value = (r.data as any) || {} })
    })
  } else {
    dialogMode.value = 'detail'
    dialogVisible.value = true
    detailTab.value = 'info'
    getDeclarationDetail(row.id).then((r) => { detailData.value = (r.data as any) || {} })
  }
}
function openEdit(row: DeclarationVO) {
  resetForm()
  dialogMode.value = 'edit'
  dialogVisible.value = true

  // 先调 detail 接口拿完整数据（含明细快照、附件）
  getDeclarationDetail(row.id).then(async (r) => {
    const d = r.data as any
    if (!d) { dialogVisible.value = false; return }

    // 回填主表
    form.id = d.id
    form.businessCode = d.businessCode
    form.title = d.title || ''
    form.townCode = d.townCode
    form.villageGroupCode = d.villageGroupCode
    form.declarationType = d.declarationType
    form.declarationCategory = d.declarationCategory
    form.amount = d.amount
    form.remark = d.remark || ''

    // 乡镇回填（只读）
    userTown.value = { townCode: d.townCode, townName: d.townName }

    // 1) 先拉村组列表 + 自动选中
    try {
      const vgRes = await getVillageGroups(d.townCode)
      villageGroupOpts.value = vgRes.data || []
      await nextTick()
    } catch { /* 忽略，form.villageGroupCode 会默认选中匹配的 */ }

    // 2) 再拉农户/公建设施选项池（按村组），然后选中已保存的
    // 先关拦截开关，避免回填时的 toggleRowSelection 触发 selection-change 弹框
    _suppressSelectGuard.value = true
    if (d.declarationCategory === '1') {
      // 缓存已保存的明细（含 amount/remark/attachments），供取消勾选时判断
      savedHouseholdDetails.value = d.households || []
      savedFacilityDetails.value = []
      householdPool.value = []
      facilityPool.value = []
      try {
        const hhRes = await getHouseholdsByVillageGroup(d.villageGroupCode)
        householdPool.value = hhRes.data || []
      } catch {}
      // 回填已选农户
      householdSelection.value = []
      form.householdIds = []
      await nextTick()
      const savedHouseholdIds = new Set(savedHouseholdDetails.value.map((h: any) => String(h.householdId)))
      householdPool.value.forEach((poolRow) => {
        if (savedHouseholdIds.has(String(poolRow.id))) {
          hhTableRef.value?.toggleRowSelection(poolRow, true)
          if (!householdSelection.value.some((s) => String(s.id) === String(poolRow.id))) {
            householdSelection.value.push(poolRow)
          }
        }
      })
      form.householdIds = householdSelection.value.map((h) => String(h.id) as any)
    } else if (d.declarationCategory === '2') {
      savedFacilityDetails.value = d.facilities || []
      savedHouseholdDetails.value = []
      householdPool.value = []
      facilityPool.value = []
      try {
        const pfRes = await getFacilitiesByVillageGroup(d.villageGroupCode)
        facilityPool.value = pfRes.data || []
      } catch {}
      facilitySelection.value = []
      form.facilityIds = []
      await nextTick()
      const savedFacilityIds = new Set(savedFacilityDetails.value.map((p: any) => String(p.facilityId)))
      facilityPool.value.forEach((poolRow) => {
        if (savedFacilityIds.has(String(poolRow.id))) {
          facTableRef.value?.toggleRowSelection(poolRow, true)
          if (!facilitySelection.value.some((s) => String(s.id) === String(poolRow.id))) {
            facilitySelection.value.push(poolRow)
          }
        }
      })
      form.facilityIds = facilitySelection.value.map((f) => String(f.id) as any)
    }
    // 回填结束，恢复拦截
    _suppressSelectGuard.value = false

    // 3) 回填申报单级附件（owner_type=declaration 的那些）
    form.attachments = (d.attachments || []).map((a: any) => ({
      id: a.id,  // 后端 DeclarationSaveDTO.AttachmentItem.id（已有附件回传原值，新增附件不传）
      attachmentType: a.attachmentType,
      fileKey: a.fileKey,
      bucket: a.bucket,
      fileName: a.fileName,
      fileSize: a.fileSize,
      remark: a.remark
    }))
  }).catch(() => {
    ElMessage.error('加载编辑数据失败')
    dialogVisible.value = false
  })
}
// ================== 提交弹窗（选审核人） ==================
const submitDialog = reactive({
  visible: false,
  row: null as DeclarationVO | null,
  recipientUserId: '' as string,
  recipientList: [] as RecipientOption[],
  recipientLoading: false,
  submitting: false
})
const selectedRecipientLabel = computed(() => {
  if (!submitDialog.recipientUserId) return '请选择'
  const u = submitDialog.recipientList.find((x) => x.id === submitDialog.recipientUserId)
  if (!u) return '未知用户'
  return `${u.realName}（${u.username}）`
})

function openSubmitDialog(row: DeclarationVO) {
  submitDialog.row = row
  submitDialog.recipientUserId = ''
  submitDialog.recipientList = []
  submitDialog.visible = true
  // 弹窗打开时预加载前 20 个启用中的审核人
  searchRecipients('')
}
async function searchRecipients(keyword: string) {
  submitDialog.recipientLoading = true
  try {
    const res = await getRecipients(keyword)
    submitDialog.recipientList = (res.data as any) || []
  } finally {
    submitDialog.recipientLoading = false
  }
}
async function doSubmitDeclaration() {
  if (!submitDialog.row || !submitDialog.recipientUserId) return
  const dto: DeclarationSubmitDTO = {
    id: submitDialog.row.id,
    recipientUserId: submitDialog.recipientUserId
  }
  submitDialog.submitting = true
  try {
    await submitDeclaration(dto)
    ElMessage.success('提交成功，已进入待审核')
    submitDialog.visible = false
    loadData()
  } catch { /* 拦截器已弹 */ }
  finally {
    submitDialog.submitting = false
  }
}

async function handleDelete(row: DeclarationVO) {
  await ElMessageBox.confirm(`确认删除申报单「${row.title}」？此操作不可恢复。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    confirmButtonClass: 'el-button--danger'
  })
  try {
    await deleteDeclaration(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* 拦截器已弹 */ }
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
.detail-tabs { margin-top: -4px; }
.detail-tabs :deep(.el-tabs__content) { padding-top: 12px; }

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

