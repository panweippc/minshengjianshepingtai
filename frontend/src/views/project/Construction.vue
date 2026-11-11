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
        <!-- 一级：两个页签 -->
        <template v-if="viewNode.nodeType === 'project'">
          <div v-loading="viewLoading">
            <el-tabs v-model="viewActiveTab">
              <!-- 页签 1：施工详情 -->
              <el-tab-pane label="施工详情" name="detail">
                <el-descriptions :column="2" border label-width="130px">
                  <el-descriptions-item label="项目编码">{{ projectView?.code || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="项目名称">{{ projectView?.name || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="项目类别">{{ categoryLabel(projectView) }}</el-descriptions-item>
                  <el-descriptions-item label="项目来源">{{ sourceLabel(projectView) }}</el-descriptions-item>
                  <el-descriptions-item label="状态" :span="2">
                    <span class="status-tag" :class="`status-tag--${projectView?.status}`">{{ statusLabel(projectView?.status) }}</span>
                  </el-descriptions-item>
                </el-descriptions>

                <el-divider content-position="left">明细进度</el-divider>
                <el-empty v-if="!projectDetails.length" description="暂无农户/设施明细" :image-size="60" />
                <el-table v-else :data="projectDetails" border size="small" class="construction-view-table">
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
                  <el-table-column label="巡检状态" width="100" align="center">
                    <template #default="{ row }">
                      <span
                        v-if="row.detailLogSnapshot?.inspectionStatus"
                        class="status-tag"
                        :class="`status-tag--${row.detailLogSnapshot.inspectionStatus === 'PASS' ? 'COMPLETED' : 'FAIL'}`"
                      >{{ inspectionLabel(row.detailLogSnapshot.inspectionStatus) }}</span>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="施工状态" width="110" align="center">
                    <template #default="{ row }">
                      <span
                        v-if="row.detailLogSnapshot?.constructionStatus"
                        class="status-tag"
                        :class="`status-tag--${row.detailLogSnapshot.inspectionStatus === 'FAIL' ? 'SUSPENDED' : row.detailLogSnapshot.constructionStatus}`"
                      >{{ row.detailLogSnapshot.inspectionStatus === 'FAIL' ? '暂停施工' : statusLabel(row.detailLogSnapshot.constructionStatus) }}</span>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="巡检日期" width="120">
                    <template #default="{ row }">{{ row.detailLogSnapshot?.constructionDate || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="施工内容" min-width="220" show-overflow-tooltip>
                    <template #default="{ row }">{{ row.detailLogSnapshot?.content || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="附件" min-width="200">
                    <template #default="{ row }">
                      <template v-if="row.detailLogSnapshot?.attachments?.length">
                        <div
                          v-for="file in row.detailLogSnapshot.attachments"
                          :key="file.id"
                          class="attachment-cell-line"
                        >
                          <el-link
                            type="primary"
                            :underline="false"
                            class="attachment-name"
                            @click="previewAttachment(file)"
                          >{{ file.fileName }}</el-link>
                          <el-icon class="attachment-download" @click="downloadAttachment(file)">
                            <Download />
                          </el-icon>
                        </div>
                      </template>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <!-- 页签 2：施工单位信息 -->
              <el-tab-pane label="施工单位信息" name="builder">
                <el-descriptions :column="2" border label-width="130px">
                  <el-descriptions-item label="施工单位名称">{{ projectView?.builder || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="承包人姓名">{{ projectView?.contractorName || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="承包人联系电话">{{ projectView?.contractorPhone || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="负责人姓名">{{ projectView?.leaderName || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="负责人联系电话" :span="2">{{ projectView?.leaderPhone || '-' }}</el-descriptions-item>
                </el-descriptions>

                <el-divider content-position="left">施工资料附件</el-divider>
                <el-empty v-if="!projectView?.attachments?.length" description="暂无施工资料" :image-size="60" />
                <el-table
                  v-else
                  :data="projectView.attachments"
                  border
                  size="small"
                  class="construction-view-table"
                >
                  <el-table-column label="附件类型" width="180">
                    <template #default="{ row }">
                      {{ dictLabel('fu_jian_lei_xing', row.attachmentType) || row.attachmentType || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="文件名" min-width="240">
                    <template #default="{ row }">
                      <el-link
                        type="primary"
                        :underline="false"
                        @click="previewAttachment(row)"
                      >{{ row.fileName || '-' }}</el-link>
                      <el-icon class="attachment-download" @click="downloadAttachment(row)">
                        <Download />
                      </el-icon>
                    </template>
                  </el-table-column>
                  <el-table-column label="大小" width="110" align="right">
                    <template #default="{ row }">{{ row.fileSize ? formatFileSize(row.fileSize) : '-' }}</template>
                  </el-table-column>
                  <el-table-column label="备注" min-width="160">
                    <template #default="{ row }">{{ row.remark || '-' }}</template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </div>
        </template>

        <!-- 二级：申报信息 + 明细进度 -->
        <template v-else-if="viewNode.nodeType === 'declare'">
          <div v-loading="viewLoading">
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
            <el-table-column label="巡检状态" width="100" align="center">
              <template #default="{ row }">
                <span
                  v-if="row.detailLogSnapshot?.inspectionStatus"
                  class="status-tag"
                  :class="`status-tag--${row.detailLogSnapshot.inspectionStatus === 'PASS' ? 'COMPLETED' : 'FAIL'}`"
                >{{ inspectionLabel(row.detailLogSnapshot.inspectionStatus) }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="施工状态" width="110" align="center">
              <template #default="{ row }">
                <span
                  v-if="row.detailLogSnapshot?.constructionStatus"
                  class="status-tag"
                  :class="`status-tag--${row.detailLogSnapshot.inspectionStatus === 'FAIL' ? 'SUSPENDED' : row.detailLogSnapshot.constructionStatus}`"
                >{{ row.detailLogSnapshot.inspectionStatus === 'FAIL' ? '暂停施工' : statusLabel(row.detailLogSnapshot.constructionStatus) }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="巡检日期" width="120">
              <template #default="{ row }">{{ row.detailLogSnapshot?.constructionDate || '-' }}</template>
            </el-table-column>
            <el-table-column label="施工内容" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.detailLogSnapshot?.content || '-' }}</template>
            </el-table-column>
            <el-table-column label="附件" min-width="200">
              <template #default="{ row }">
                <template v-if="row.detailLogSnapshot?.attachments?.length">
                  <div
                    v-for="file in row.detailLogSnapshot.attachments"
                    :key="file.id"
                    class="attachment-cell-line"
                  >
                    <el-link
                      type="primary"
                      :underline="false"
                      class="attachment-name"
                      @click="previewAttachment(file)"
                    >{{ file.fileName }}</el-link>
                    <el-icon class="attachment-download" @click="downloadAttachment(file)">
                      <Download />
                    </el-icon>
                  </div>
                </template>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
          </div>
        </template>

        <!-- 三级：施工记录 -->
        <template v-else>
          <div v-loading="detailLogLoading">
            <el-tabs v-model="detailActiveTab">
              <el-tab-pane label="详情" name="detail">
                <el-descriptions :column="2" border label-width="110px">
                  <el-descriptions-item label="记录日期">{{ detailLog?.constructionDate || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="巡检人">{{ detailLog?.inspector || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="巡检状态">
                    <span
                      v-if="detailLog?.inspectionStatus"
                      class="status-tag"
                      :class="`status-tag--${detailLog.inspectionStatus === 'PASS' ? 'COMPLETED' : 'FAIL'}`"
                    >{{ inspectionLabel(detailLog.inspectionStatus) }}</span>
                    <span v-else>-</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="施工状态">
                    <span
                      class="status-tag"
                      :class="detailLog?.inspectionStatus === 'FAIL' ? 'status-tag--SUSPENDED' : `status-tag--${detailStatus}`"
                    >{{ detailLog?.inspectionStatus === 'FAIL' ? '暂停施工' : statusLabel(detailStatus) }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="施工内容描述" :span="2">
                    {{ detailLog?.content || '-' }}
                  </el-descriptions-item>
                </el-descriptions>

                <div class="section-header section-header--inline attachment-header">
                  <span class="section-header__title">附件</span>
                  <span class="section-header__extra">（共 {{ detailLog?.attachments?.length ?? 0 }} 个）</span>
                </div>
                <el-empty v-if="!detailLog?.attachments?.length" description="暂无附件" :image-size="50" />
                <el-table v-else :data="detailLog.attachments" border size="small">
                  <el-table-column label="附件类型" width="180">
                    <template #default="{ row }">
                      {{ dictLabel('shi_gong_fu_jian_lei_xing', row.attachmentType) || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="附件" min-width="220">
                    <template #default="{ row }">
                      <template v-if="row.fileKey">
                        <el-link type="primary" :underline="false" @click="previewAttachment(row)">
                          {{ row.fileName || '查看附件' }}
                        </el-link>
                        <el-icon
                          class="attachment-download"
                          title="下载"
                          @click.stop="downloadAttachment(row)"
                        >
                          <Download />
                        </el-icon>
                      </template>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="备注" min-width="180">
                    <template #default="{ row }">{{ row.remark || '-' }}</template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <el-tab-pane :label="`明细（${detailLogHistory.length}）`" name="history">
                <el-empty v-if="!detailLogHistory.length" description="暂无施工记录" :image-size="50" />
                <el-table v-else :data="detailLogHistory" border size="small">
                  <el-table-column label="记录日期" width="110">
                    <template #default="{ row }">{{ row.constructionDate || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="巡检人" width="100">
                    <template #default="{ row }">{{ row.inspector || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="巡检状态" width="90" align="center">
                    <template #default="{ row }">
                      <span
                        class="status-tag"
                        :class="`status-tag--${row.inspectionStatus === 'PASS' ? 'COMPLETED' : 'FAIL'}`"
                      >{{ inspectionLabel(row.inspectionStatus) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="施工状态" width="110">
                    <template #default="{ row }">
                      <!-- 巡检状态为整改时，施工状态同步展示为暂停施工 -->
                      <span
                        class="status-tag"
                        :class="row.inspectionStatus === 'FAIL' ? 'status-tag--SUSPENDED' : `status-tag--${row.constructionStatus}`"
                      >{{ row.inspectionStatus === 'FAIL' ? '暂停施工' : statusLabel(row.constructionStatus) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="施工内容" min-width="220" show-overflow-tooltip>
                    <template #default="{ row }">{{ row.content || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="附件" min-width="200">
                    <template #default="{ row }">
                      <template v-if="row.attachments?.length">
                        <div v-for="file in row.attachments" :key="file.id" class="attachment-cell-line">
                          <el-link
                            type="primary"
                            :underline="false"
                            class="attachment-name"
                            @click="previewAttachment(file)"
                          >{{ file.fileName }}</el-link>
                          <el-icon class="attachment-download" title="下载" @click="downloadAttachment(file)">
                            <Download />
                          </el-icon>
                        </div>
                      </template>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </div>
        </template>
      </template>

      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 附件图片预览 -->
    <el-dialog
      v-model="previewVisible"
      :title="previewName || '附件预览'"
      width="820px"
      destroy-on-close
      @closed="previewFile = null"
    >
      <div class="attachment-preview-body">
        <img v-if="previewUrl" :src="previewUrl" class="attachment-preview-image" alt="附件预览" />
      </div>
      <template #footer>
        <el-button v-if="previewFile" type="primary" @click="downloadAttachment(previewFile)">下载</el-button>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editTitle" width="900px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="130px">
        <!-- 一级：施工单位信息 + 施工资料上传 -->
        <template v-if="editForm.nodeLevel === 1">
          <el-divider content-position="left">施工单位信息</el-divider>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="施工单位名称" prop="builder">
                <el-input v-model="editForm.builder" placeholder="请输入施工单位名称" maxlength="128" />
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

        </template>

        <!-- 三级：施工记录 -->
        <template v-else>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="记录日期" prop="constructionDate">
                <el-date-picker
                  v-model="editForm.constructionDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择记录日期"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="巡检人" prop="inspector">
                <el-input v-model="editForm.inspector" placeholder="请输入巡检人" maxlength="64" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="巡检状态" prop="inspectionStatus">
                <el-select v-model="editForm.inspectionStatus" placeholder="请选择巡检状态" style="width: 100%">
                  <el-option
                    v-for="item in dictOptions('xun_jian_zhuang_tai')"
                    :key="item.dictValue"
                    :label="item.dictLabel"
                    :value="item.dictValue"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
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
            </el-col>
          </el-row>
          <el-form-item label="施工内容描述" prop="content">
            <el-input
              v-model="editForm.content"
              type="textarea"
              :rows="3"
              placeholder="请输入施工内容描述"
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>
        </template>

        <!-- 施工资料附件：一级/三级共用卡片样式 -->
        <div class="attach-divider">
          <span class="attach-divider__line attach-divider__line--short" />
          <span class="attach-divider__title">附件列表</span>
          <el-button link type="primary" icon="Plus" @click="addAttachmentRow">添加附件</el-button>
          <span class="attach-divider__line" />
        </div>
        <div v-for="(item, index) in attachmentRows" :key="index" class="attach-card">
          <div class="attach-card__head">
            <span class="attach-card__index">{{ index + 1 }}</span>
            <span class="attach-card__name">附件 #{{ index + 1 }}</span>
            <el-button
              link
              type="danger"
              icon="Delete"
              class="attach-card__remove"
              @click="removeAttachmentRow(index)"
            >删除</el-button>
          </div>
          <div class="attach-card__body">
            <div class="attach-field">
              <div class="attach-field__label">附件类型</div>
              <el-select
                v-model="item.attachmentType"
                placeholder="请选择类型..."
                clearable
              >
                <el-option
                  v-for="opt in dictOptions(attachmentDictKey)"
                  :key="opt.dictValue"
                  :label="opt.dictLabel"
                  :value="opt.dictValue"
                />
              </el-select>
            </div>
            <div class="attach-field">
              <div class="attach-field__label">上传附件</div>
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                class="attach-upload"
                @change="(file: UploadFile) => handleAttachmentChange(item, file)"
              >
                <div class="attach-upload__inner">
                  <el-icon><Upload /></el-icon>
                  <span>{{ item.uploading ? '上传中...' : '点击选择文件...' }}</span>
                </div>
              </el-upload>
              <!-- 已上传文件：缩略图标 + 文件名 + 移除 -->
              <div v-if="item.fileName" class="attach-file-chip">
                <el-icon class="attach-file-chip__icon"><Document /></el-icon>
                <span
                  class="attach-file-chip__name"
                  :title="item.fileName"
                  @click="previewAttachment(item)"
                >{{ item.fileName }}</span>
                <el-icon class="attach-file-chip__download" title="下载" @click.stop="downloadAttachment(item)">
                  <Download />
                </el-icon>
                <el-icon class="attach-file-chip__close" title="移除" @click.stop="clearAttachmentFile(item)">
                  <Close />
                </el-icon>
              </div>
            </div>
            <div class="attach-field">
              <div class="attach-field__label">备注</div>
              <el-input v-model="item.remark" placeholder="选填" maxlength="128" />
            </div>
          </div>
        </div>
        <div v-if="!attachmentRows.length" class="attach-empty">暂无附件，请点击上方「添加附件」</div>
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
  getDetailLog,
  getDetailLogHistory,
  saveDetailLog,
  type ConstructionNodeVO,
  type ConstructionEditDTO,
  type ConstructionLogVO,
  type ConstructionAttachmentItem
} from '@/api/construction'
import { uploadFile, getPresignUrl } from '@/api/file'
import { applyAcceptance } from '@/api/acceptance'

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
const DICT_TYPES = ['xiang_mu_lei_bie', 'xiang_mu_lei_xing', 'shen_bao_lei_bie', 'shen_bao_lei_xing', 'shi_gong_fu_jian_lei_xing', 'fu_jian_lei_xing', 'xun_jian_zhuang_tai']
const dictOptions = (type: string): DictVO[] => dictMap.value[type] || []
const dictLabel = (type: string, value?: string): string => {
  if (!value) return ''
  const hit = dictOptions(type).find((item) => item.dictValue === value)
  return hit?.dictLabel || value
}

/** 附件类型字典 key：一级编辑用通用附件字典 fu_jian_lei_xing，三级编辑用施工附件字典 shi_gong_fu_jian_lei_xing */
const attachmentDictKey = computed(() =>
  editForm.nodeLevel === 1 ? 'fu_jian_lei_xing' : 'shi_gong_fu_jian_lei_xing'
)

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
const categoryLabel = (row?: ConstructionNodeVO | null): string => {
  if (!row) return '-'
  if (row.nodeType === 'project') return dictLabel('xiang_mu_lei_bie', row.category) || '-'
  if (row.nodeType === 'declare') return dictLabel('shen_bao_lei_bie', row.category) || '-'
  return detailTypeLabel(row.detailType || row.category)
}

/** 来源列：一级取项目类型字典，二级取申报类型字典，三级取所属区划 */
const sourceLabel = (row?: ConstructionNodeVO | null): string => {
  if (!row) return '-'
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

/** 库内 PASS/FAIL 转为字典值（xun_jian_zhuang_tai：1合格 2整改） */
const toInspectionDictValue = (status?: string): string => (status === 'FAIL' ? '2' : '1')

/** 字典值转回库内 PASS/FAIL */
const toInspectionStatus = (dictValue?: string): string => (dictValue === '2' ? 'FAIL' : 'PASS')

/** 文件大小展示：B / KB / MB / GB */
const formatFileSize = (bytes?: number): string => {
  if (!bytes || bytes <= 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let idx = 0
  let size = bytes
  while (size >= 1024 && idx < units.length - 1) { size /= 1024; idx++ }
  return `${size.toFixed(2)} ${units[idx]}`
}

/* ============ 查看弹窗 ============ */
const viewVisible = ref(false)
const viewNode = ref<ConstructionNodeVO | null>(null)
const viewDetail = ref<ConstructionNodeVO | null>(null)
const viewLoading = ref(false)
/** 三级施工记录（关联项目、巡检、施工内容与附件） */
const detailLog = ref<ConstructionLogVO | null>(null)
const detailLogLoading = ref(false)
/** 三级查看：详情/明细页签，明细为历次编辑记录 */
const detailActiveTab = ref<'detail' | 'history'>('detail')
const detailLogHistory = ref<ConstructionLogVO[]>([])

/* ============ 附件预览 ============ */
/** 图片类后缀：图片弹窗预览，其余类型直接下载 */
const IMAGE_EXTENSIONS = ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp']
const previewVisible = ref(false)
const previewUrl = ref('')
const previewName = ref('')
const previewFile = ref<{ bucket?: string; fileKey?: string; fileName?: string } | null>(null)

/** 三级查看的施工状态：优先取施工记录，其次取列表行状态 */
const detailStatus = computed(() => detailLog.value?.constructionStatus || viewNode.value?.buildStatus || '')

const viewTitle = computed(() => {
  if (viewNode.value?.nodeType === 'declare') return '申报施工详情'
  if (viewNode.value?.nodeType === 'detail') return '明细施工详情'
  return '项目施工详情'
})

/** 一级查看使用详情接口数据（含日志、巡检合格率、附件），加载完成前回退到列表行数据 */
const projectView = computed(() => viewDetail.value || viewNode.value)

/** 一级查看当前页签 */
const viewActiveTab = ref<'detail' | 'builder'>('detail')
/** 一级查看：把所有申报下的三级明细展平，一户一条 */
const projectDetails = computed(() => {
  if (!projectView.value?.children?.length) return []
  return projectView.value.children.flatMap((declare) => declare.children || [])
})

const openView = async (row: DisplayRow) => {
  viewNode.value = row.raw
  viewDetail.value = null
  detailLog.value = null
  viewActiveTab.value = 'detail'
  viewVisible.value = true
  if (row.nodeType === 'project') {
    viewLoading.value = true
    try {
      const res = await getConstructionDetail(row.approvalId)
      viewDetail.value = res.data
    } finally {
      viewLoading.value = false
    }
  } else if (row.nodeType === 'declare') {
    // 二级查看需展示各三级最新施工内容/巡检状态/施工状态/附件，重新拉取项目详情（后端一次性聚合）
    viewLoading.value = true
    try {
      const res = await getConstructionDetail(row.approvalId)
      viewDetail.value = res.data
      const fetched = res.data.children?.find((item) => item.declareId === row.declareId)
      if (fetched) {
        viewNode.value = fetched
      }
    } finally {
      viewLoading.value = false
    }
  } else if (row.nodeType === 'detail' && row.detailId) {
    detailLogLoading.value = true
    detailActiveTab.value = 'detail'
    detailLogHistory.value = []
    try {
      const [detailRes, historyRes] = await Promise.all([
        getDetailLog(row.detailId),
        getDetailLogHistory(row.detailId).catch(() => null)
      ])
      detailLog.value = detailRes.data
      detailLogHistory.value = historyRes?.data || []
    } finally {
      detailLogLoading.value = false
    }
  }
}

/* ============ 验收 ============ */
const openAcceptance = async (row: DisplayRow) => {
  try {
    await ElMessageBox.confirm(`「${row.name}」全部明细已完工，是否进入验收管理？`, '发起验收', {
      type: 'success',
      confirmButtonText: '发起验收'
    })
    // 调用后端发起验收接口，把项目标记为"验收中"
    await applyAcceptance(row.approvalId, userStore.userInfo?.username || '系统')
    ElMessage.success('已发起验收，项目进入验收管理')
    router.push({ path: '/project/acceptance' })
  } catch {
    /* 用户取消 */
  }
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
  /** 三级：记录日期 */
  constructionDate: string
  /** 三级：巡检人 */
  inspector: string
  /** 三级：巡检状态 PASS合格 FAIL整改 */
  inspectionStatus: string
  /** 三级：施工内容描述 */
  content: string
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
  constructionDate: '',
  inspector: '',
  inspectionStatus: 'PASS',
  content: ''
})

const editTitle = computed(() => (editForm.nodeLevel === 1 ? '编辑施工信息' : '编辑施工记录'))

const editRules = computed<FormRules>(() => ({
  builder: editForm.nodeLevel === 1
    ? [{ required: true, message: '请输入施工单位名称', trigger: 'blur' }]
    : [],
  contractorName: editForm.nodeLevel === 1
    ? [{ required: true, message: '请输入承包人姓名', trigger: 'blur' }]
    : [],
  contractorPhone: editForm.nodeLevel === 1
    ? [{ required: true, message: '请输入承包人联系电话', trigger: 'blur' }]
    : [],
  leaderName: editForm.nodeLevel === 1
    ? [{ required: true, message: '请输入负责人姓名', trigger: 'blur' }]
    : [],
  leaderPhone: editForm.nodeLevel === 1
    ? [{ required: true, message: '请输入负责人联系电话', trigger: 'blur' }]
    : [],
  constructionDate: editForm.nodeLevel === 3
    ? [{ required: true, message: '请选择记录日期', trigger: 'change' }]
    : [],
  inspectionStatus: editForm.nodeLevel === 3
    ? [{ required: true, message: '请选择巡检状态', trigger: 'change' }]
    : [],
  inspector: editForm.nodeLevel === 3
    ? [{ required: true, message: '请输入巡检人', trigger: 'blur' }]
    : [],
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
    constructionDate: '',
    inspector: '',
    inspectionStatus: 'PASS',
    content: ''
  })
  attachmentRows.value = []
  detailLog.value = null
  // 一级编辑回显施工队信息与已上传资料
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
  // 三级编辑回显施工记录：关联项目只读、记录日期/巡检/施工内容/附件
  if (row.nodeType === 'detail' && row.detailId) {
    try {
      const res = await getDetailLog(row.detailId)
      const log = res.data
      detailLog.value = log
      Object.assign(editForm, {
        buildStatus: log.constructionStatus || CONSTRUCTION_STATUS_NOT_STARTED,
        constructionDate: log.constructionDate || '',
        inspector: log.inspector || '',
        inspectionStatus: toInspectionDictValue(log.inspectionStatus),
        content: log.content || ''
      })
      attachmentRows.value = (log.attachments || []).map((item) => ({
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
const previewAttachment = async (item: { bucket?: string; fileKey?: string; fileName?: string }) => {
  if (!item.bucket || !item.fileKey) return
  const ext = (item.fileName || '').split('.').pop()?.toLowerCase() || ''
  const isImage = IMAGE_EXTENSIONS.includes(ext)
  // 图片弹窗预览；PDF 新窗口预览；其余类型直接下载
  const res = await getPresignUrl({
    bucket: item.bucket,
    fileKey: item.fileKey,
    fileName: item.fileName,
    download: !isImage && ext !== 'pdf'
  })
  if (isImage) {
    previewUrl.value = res.data
    previewName.value = item.fileName || '附件预览'
    previewFile.value = item
    previewVisible.value = true
    return
  }
  window.open(res.data, '_blank')
}

/** 下载附件：由后端生成强制下载的时效签名地址 */
const downloadAttachment = async (item: { bucket?: string; fileKey?: string; fileName?: string }) => {
  if (!item.bucket || !item.fileKey) return
  const res = await getPresignUrl({
    bucket: item.bucket,
    fileKey: item.fileKey,
    fileName: item.fileName,
    download: true
  })
  window.open(res.data, '_blank')
}

const handleSubmit = async () => {
  if (!editFormRef.value) return
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  const isProject = editForm.nodeLevel === 1
  const attachments: ConstructionAttachmentItem[] = attachmentRows.value
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
  submitLoading.value = true
  try {
    if (isProject) {
      const payload: ConstructionEditDTO = {
        approvalId: editForm.approvalId,
        nodeLevel: 1,
        builder: editForm.builder,
        contractorName: editForm.contractorName,
        contractorPhone: editForm.contractorPhone,
        leaderName: editForm.leaderName,
        leaderPhone: editForm.leaderPhone,
        attachments
      }
      await updateConstruction(payload)
    } else {
      await saveDetailLog({
        approvalId: editForm.approvalId,
        detailId: editForm.detailId,
        constructionDate: editForm.constructionDate,
        inspector: editForm.inspector || undefined,
        inspectionStatus: toInspectionStatus(editForm.inspectionStatus),
        constructionStatus: editForm.buildStatus,
        content: editForm.content || undefined,
        attachments
      })
    }
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

/* 三级查看附件区与基础信息之间的间距 */
.attachment-header {
  margin-top: 16px;
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

/* ============ 三级编辑：附件列表分割线 ============ */
.attach-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0 12px;
}

.attach-divider__line {
  flex: 1;
  height: 1px;
  background: #ebeef5;
}

.attach-divider__line--short {
  flex: none;
  width: 24px;
}

.attach-divider__title {
  color: #303133;
  font-size: 14px;
  line-height: 20px;
  white-space: nowrap;
}

/* 与标题保持同一水平线，去掉按钮默认高度与内边距带来的偏移 */
.attach-divider :deep(.el-button) {
  height: 20px;
  margin-left: 0;
  padding: 0;
  line-height: 20px;
}

.attach-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  border-radius: 4px;
  background: #f7f8fa;
  color: #909399;
  font-size: 13px;
}

.attach-card {
  margin-bottom: 12px;
  padding: 12px 16px 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.attach-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.attach-card__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  font-size: 12px;
  font-weight: 600;
}

.attach-card__name {
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.attach-card__remove {
  margin-left: auto;
}

.attach-card__body {
  display: grid;
  /* 附件类型列收窄，上传与备注均分剩余宽度 */
  grid-template-columns: 220px minmax(0, 1fr) minmax(0, 1fr);
  align-items: start;
  gap: 16px;
}

.attach-field {
  min-width: 0;
}

.attach-field__label {
  margin-bottom: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 20px;
}

.attach-field :deep(.el-select),
.attach-field :deep(.el-input) {
  width: 100%;
}

.attach-field :deep(.el-select__wrapper),
.attach-field :deep(.el-input__wrapper) {
  min-height: 32px;
  box-sizing: border-box;
}

.attach-upload {
  display: block;
  width: 100%;
}

.attach-upload :deep(.el-upload) {
  display: block;
  width: 100%;
}

/* 已上传文件展示：浅灰圆角条，含图标、文件名与移除按钮 */
.attach-file-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  margin-top: 8px;
  padding: 4px 8px;
  border-radius: 4px;
  background: #f5f7fa;
}

.attach-file-chip__icon {
  flex: none;
  color: #909399;
  font-size: 14px;
}

.attach-file-chip__name {
  flex: 1;
  overflow: hidden;
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
  text-overflow: ellipsis;
  cursor: pointer;
}

.attach-file-chip__name:hover {
  color: #409eff;
}

.attach-file-chip__download,
.attach-file-chip__close {
  flex: none;
  font-size: 12px;
  cursor: pointer;
}

.attach-file-chip__download {
  color: #409eff;
}

.attach-file-chip__close {
  color: #909399;
}

/* 二级查看附件列：每行一个文件，名称可预览、箭头可下载 */
.attachment-cell-line {
  display: flex;
  align-items: center;
  line-height: 20px;
}

.attachment-cell-line .attachment-download {
  margin-left: 6px;
}

/* 三级查看附件表格中的下载箭头 */
.attachment-download {
  margin-left: 8px;
  color: #409eff;
  font-size: 13px;
  vertical-align: middle;
  cursor: pointer;
}

.attachment-download:hover {
  color: #337ecc;
}

/* 附件图片预览弹窗 */
.attachment-preview-body {
  display: flex;
  align-items: center;
  justify-content: center;
  max-height: 70vh;
  overflow: auto;
}

.attachment-preview-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.attach-file-chip__download:hover {
  color: #337ecc;
}

.attach-file-chip__close:hover {
  color: #606266;
}

.attach-upload__inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 32px;
  box-sizing: border-box;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #909399;
  font-size: 13px;
  cursor: pointer;
}

.attach-upload__inner:hover {
  border-color: #409eff;
  color: #409eff;
}
</style>

<style>
/* 查看弹窗高度自适应：内容少时紧凑贴合，内容多时正文内部滚动 */
.construction-view-dialog {
  margin-top: 8vh;
  margin-bottom: 0;
}

.construction-view-dialog .el-dialog__body {
  max-height: 68vh;
  overflow-y: auto;
}
</style>
