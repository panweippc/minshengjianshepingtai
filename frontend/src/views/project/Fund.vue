<template>
  <div class="fund-page">
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
        <el-form-item class="query-item--actions">
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 树形列表区 -->
    <el-card shadow="never" class="list-card">
      <el-table
        v-loading="loading"
        :data="visibleRows"
        row-key="rowKey"
        border
        stripe
        class="fund-table"
        :row-class-name="rowClassName"
      >
        <el-table-column label="项目编号" width="210">
          <template #default="{ row }">
            <span class="approval-code-line">
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
              <!-- 二级申报节点：靛蓝色打开文件夹小图标 -->
              <el-icon
                v-if="row.nodeType === 'declare'"
                class="detail-type-icon detail-type-icon--folder"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#22619e" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 14 1.45-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.55 6a2 2 0 0 1-1.94 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2"/></svg>
              </el-icon>
              <!-- 三级明细按类型区分图标：农户为头像图标，公共基础设施为设施图标 -->
              <el-icon
                v-if="row.nodeType === 'detail'"
                class="detail-type-icon"
                :class="row.detailType === 'FACILITY' ? 'detail-type-icon--facility' : 'detail-type-icon--household'"
              >
                <OfficeBuilding v-if="row.detailType === 'FACILITY'" />
                <UserFilled v-else />
              </el-icon>
              <el-tooltip
                :content="row.code || '-'"
                placement="top"
                :show-after="200"
              >
                <span class="tree-code" :class="{ 'tree-code--detail': row.nodeType === 'detail' }">
                  {{ row.code || '-' }}
                </span>
              </el-tooltip>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="项目名称" min-width="240" show-overflow-tooltip />
        <el-table-column prop="category" label="项目类别" width="140" show-overflow-tooltip />
        <el-table-column prop="type" label="项目类型" width="120" show-overflow-tooltip />
        <el-table-column label="项目预算（元）" width="140" align="right">
          <template #default="{ row }">
            <!-- 项目预算仅一级项目节点展示，二级申报、三级明细不取值 -->
            <template v-if="row.nodeType === 'approval'">{{ formatMoney(row.budget) }}</template>
            <template v-else>-</template>
          </template>
        </el-table-column>
        <el-table-column label="资金来源" width="140">
          <template #default="{ row }">{{ dictLabel('zi_jin_lai_yuan', row.fundSource) || '-' }}</template>
        </el-table-column>
        <el-table-column label="拨付金额（元）" width="140" align="right">
          <template #default="{ row }">{{ formatMoney(row.allocatedAmount) }}</template>
        </el-table-column>
        <el-table-column label="结余（元）" width="140" align="right">
          <template #default="{ row }">
            <!-- 结余仅一级项目节点展示：上级拨付金额 - 二级下拨金额 -->
            <template v-if="row.nodeType === 'approval'">
              <span :class="{ 'balance-negative': Number(row.balance) < 0 }">{{ formatMoney(row.balance) }}</span>
            </template>
            <template v-else>-</template>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button v-if="canView" link type="primary" @click="openView(row)">查看</el-button>
              <el-button
                v-if="row.nodeType === 'approval' && canRegister"
                link
                type="success"
                @click="openForm(row)"
              >资金登记</el-button>
              <el-button
                v-if="row.nodeType !== 'approval' && canAllocate"
                link
                type="warning"
                @click="openForm(row)"
              >下拨资金</el-button>
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

    <!-- 查看弹窗：一级按页签展示（项目资金 / 事项申报），二三级展示节点信息 -->
    <el-dialog
      v-model="viewVisible"
      :title="viewTitle"
      :width="viewRow?.nodeType === 'approval' ? '1120px' : '900px'"
      destroy-on-close
    >
      <template v-if="viewRow?.nodeType === 'approval'">
        <el-tabs v-model="viewTab" @tab-change="handleViewTabChange">
          <!-- 项目资金页签：项目信息 + 资金流水 -->
          <el-tab-pane label="项目资金" name="fund">
            <el-descriptions :column="3" border>
              <el-descriptions-item label="项目编号">{{ viewRow.code || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目名称">{{ viewRow.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目类别">{{ viewRow.category || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目类型">{{ viewRow.type || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目预算">{{ formatMoney(viewRow.budget) }} 元</el-descriptions-item>
              <el-descriptions-item label="拨付金额">{{ formatMoney(viewRow.allocatedAmount) }} 元</el-descriptions-item>
              <el-descriptions-item label="结余">{{ formatMoney(viewRow.balance) }} 元</el-descriptions-item>
            </el-descriptions>

            <el-divider content-position="left">资金流水</el-divider>
            <el-table v-loading="recordLoading" :data="records" size="small" border>
              <el-table-column label="资金类型" width="110">
                <template #default="{ row }">{{ dictLabel('zi_jin_lei_xing', row.fundType) || '-' }}</template>
              </el-table-column>
              <el-table-column label="资金来源" width="120">
                <template #default="{ row }">{{ dictLabel('zi_jin_lai_yuan', row.fundSource) || '-' }}</template>
              </el-table-column>
              <el-table-column label="金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="createBy" label="操作人" width="100" />
              <el-table-column prop="createTime" label="时间" width="165" />
              <el-table-column prop="remark" label="拨付说明" min-width="150" show-overflow-tooltip />
              <el-table-column label="附件" width="130" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.attachments?.length"
                    link
                    type="primary"
                    @click="openRecordAttachList(row)"
                  >附件 ({{ row.attachments.length }})</el-button>
                  <span v-else class="declare-no-attachment-text">-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="editRecord(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 事项申报页签：申报信息（一级）+ 明细信息（二级），与立项管理查看一致 -->
          <el-tab-pane label="事项申报" name="declare">
            <el-table
              :data="viewDeclareTree"
              row-key="rowKey"
              :tree-props="{ children: 'children' }"
              default-expand-all
              border
              class="declare-tree-table"
              :row-class-name="declareRowClassName"
            >
              <el-table-column label="申报编号" width="190">
                <template #default="{ row }">
                  <!-- declare 节点显示文件夹标签，detail 节点按农户/公共基础设施显示小图标，图标与编号强制同行 -->
                  <span class="detail-code-line" :class="{ 'detail-code-line--detail': row.nodeType === 'detail' }">
                    <el-icon
                      v-if="row.nodeType === 'declare'"
                      class="detail-type-icon detail-type-icon--folder"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#22619e" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 14 1.45-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.55 6a2 2 0 0 1-1.94 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2"/></svg>
                    </el-icon>
                    <el-icon
                      v-else
                      class="detail-type-icon"
                      :class="row.detailType === 'FACILITY' ? 'detail-type-icon--facility' : 'detail-type-icon--household'"
                    >
                      <OfficeBuilding v-if="row.detailType === 'FACILITY'" />
                      <UserFilled v-else />
                    </el-icon>
                    <el-tooltip
                      :content="row.code || '-'"
                      placement="top"
                      :show-after="200"
                    >
                      <span class="tree-code" :class="{ 'tree-code--detail': row.nodeType === 'detail' }">
                        {{ row.code || '-' }}
                      </span>
                    </el-tooltip>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="申报标题" min-width="200" show-overflow-tooltip />
              <el-table-column prop="declareType" label="申报类型" width="120" />
              <el-table-column prop="declareCategory" label="申报类别" width="120" />
              <el-table-column label="申报金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column label="拨付金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(row.allocated) }}</template>
              </el-table-column>
              <el-table-column prop="village" label="所属村组" width="160" />
              <el-table-column label="申报材料附件" width="130" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.attachments?.length"
                    link
                    type="primary"
                    @click="openAttachList(row, 'material')"
                  >附件 ({{ row.attachments.length }})</el-button>
                  <span v-else class="declare-no-attachment-text">-</span>
                </template>
              </el-table-column>
              <el-table-column label="资金凭证附件" width="130" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.fundAttachments?.length"
                    link
                    type="primary"
                    @click="openAttachList(row, 'fund')"
                  >附件 ({{ row.fundAttachments.length }})</el-button>
                  <span v-else class="declare-no-attachment-text">-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 立项页签：与立项管理一级查看“项目立项”页签一致 -->
          <el-tab-pane label="立项" name="approval">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="项目编号">{{ viewRow.code || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目名称">{{ viewRow.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目类别">{{ viewRow.category || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目类型">{{ viewRow.type || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目预算">{{ formatMoney(viewRow.budget) }} 元</el-descriptions-item>
              <el-descriptions-item label="负责人">{{ viewRow.raw.leader || '-' }}</el-descriptions-item>
              <el-descriptions-item label="提交时间">{{ viewRow.raw.submitTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ viewRow.raw.remark || '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-divider content-position="left">立项资料附件</el-divider>
            <el-empty v-if="!viewRow.raw.attachments?.length" description="暂无附件" :image-size="60" />
            <el-table v-else :data="viewRow.raw.attachments" size="small" border>
              <el-table-column label="附件类型" width="120" align="center">
                <template #default="{ row }">
                  {{ dictLabel('li_xiang_fu_jian_lei_xing', row.attachmentType) || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="文件名称" min-width="220">
                <template #default="{ row }">
                  <div class="attachment-file-cell">
                    <span class="attachment-file-name" :title="row.fileName" @click="openFile(row)">{{ row.fileName }}</span>
                    <el-icon class="attachment-file-download" title="下载" @click.stop="openFile(row, true)">
                      <Download />
                    </el-icon>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>

          <!-- 施工页签：与施工管理一级查看一致（施工详情 + 明细进度 + 施工单位信息 + 施工资料附件） -->
          <el-tab-pane label="施工" name="construction">
            <div v-loading="constructionLoading">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="项目编码">{{ constructionView?.code || '-' }}</el-descriptions-item>
                <el-descriptions-item label="项目名称">{{ constructionView?.name || '-' }}</el-descriptions-item>
                <el-descriptions-item label="项目类别">
                  {{ dictLabel('xiang_mu_lei_bie', constructionView?.category) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="项目来源">
                  {{ dictLabel('xiang_mu_lei_xing', constructionView?.source) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="状态" :span="2">
                  <span class="status-tag" :class="`status-tag--${constructionView?.status}`">
                    {{ constructionStatusLabel(constructionView?.status) }}
                  </span>
                </el-descriptions-item>
              </el-descriptions>

              <el-divider content-position="left">明细进度</el-divider>
              <el-empty v-if="!constructionDetails.length" description="暂无农户/设施明细" :image-size="60" />
              <el-table v-else :data="constructionDetails" size="small" border>
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
                    >{{ row.detailLogSnapshot.inspectionStatus === 'FAIL' ? '暂停施工' : constructionStatusLabel(row.detailLogSnapshot.constructionStatus) }}</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="巡检日期" width="120">
                  <template #default="{ row }">{{ row.detailLogSnapshot?.constructionDate || '-' }}</template>
                </el-table-column>
                <el-table-column label="施工内容" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.detailLogSnapshot?.content || '-' }}</template>
                </el-table-column>
                <el-table-column label="附件" width="100" align="center">
                  <template #default="{ row }">
                    <el-button
                      v-if="row.detailLogSnapshot?.attachments?.length"
                      link
                      type="primary"
                      @click="openConstructionDetailAttachList(row.detailLogSnapshot)"
                    >附件（{{ row.detailLogSnapshot.attachments.length }}）</el-button>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
              </el-table>

              <el-divider content-position="left">施工单位信息</el-divider>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="施工单位名称">{{ constructionView?.builder || '-' }}</el-descriptions-item>
                <el-descriptions-item label="承包人姓名">{{ constructionView?.contractorName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="承包人联系电话">{{ constructionView?.contractorPhone || '-' }}</el-descriptions-item>
                <el-descriptions-item label="负责人姓名">{{ constructionView?.leaderName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="负责人联系电话" :span="2">{{ constructionView?.leaderPhone || '-' }}</el-descriptions-item>
              </el-descriptions>

              <el-divider content-position="left">施工资料附件</el-divider>
              <el-empty v-if="!constructionView?.attachments?.length" description="暂无施工资料" :image-size="60" />
              <el-table v-else :data="constructionView.attachments" size="small" border>
                <el-table-column label="附件类型" width="120" align="center">
                  <template #default="{ row }">
                    {{ dictLabel('fu_jian_lei_xing', row.attachmentType) || row.attachmentType || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="文件名" min-width="260">
                  <template #default="{ row }">
                    <div class="record-attachment">
                      <span class="attachment-file-name" :title="row.fileName" @click="openFile(row)">{{ row.fileName || '-' }}</span>
                      <el-icon class="upload-file-download" @click="openFile(row, true)"><Download /></el-icon>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="备注" min-width="160">
                  <template #default="{ row }">{{ row.remark || '-' }}</template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <!-- 验收页签：与验收管理一级查看“验收详情”一致（项目验收信息 + 验收记录 + 验收情况） -->
          <el-tab-pane label="验收" name="acceptance">
            <div v-loading="acceptanceLoading">
              <template v-if="acceptanceProject">
                <el-descriptions :column="3" border>
                  <el-descriptions-item label="项目编号">{{ acceptanceProject.code || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="项目名称">{{ acceptanceProject.name || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="项目类别">
                    {{ dictLabel('xiang_mu_lei_bie', acceptanceProject.category) || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="项目类型">
                    {{ dictLabel('xiang_mu_lei_xing', acceptanceProject.source) || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="提交时间">{{ acceptanceProject.submitTime || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="验收进度">
                    <div class="simple-progress simple-progress--inline">
                      <div class="simple-progress__bar">
                        <div
                          class="simple-progress__fill"
                          :style="{ width: (acceptanceProject.percent || 0) + '%' }"
                        />
                      </div>
                      <span class="simple-progress__score">
                        {{ acceptanceProject.acceptedCount || 0 }}/{{ acceptanceProject.totalCount || 0 }}
                      </span>
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="项目预算">{{ formatMoney(acceptanceProject.budget) }} 元</el-descriptions-item>
                  <el-descriptions-item label="归档状态">
                    <el-tag v-if="acceptanceProject.archived" type="success" size="small">已归档</el-tag>
                    <el-tag v-else type="info" size="small">未归档</el-tag>
                  </el-descriptions-item>
                </el-descriptions>

                <el-divider content-position="left">验收记录</el-divider>
                <el-empty
                  v-if="!acceptanceProject.acceptanceRecords?.length"
                  description="暂无验收记录"
                  :image-size="60"
                />
                <el-table v-else :data="acceptanceProject.acceptanceRecords" size="small" border>
                  <el-table-column label="验收结果" width="130">
                    <template #default="{ row }">
                      {{ dictLabel('yan_shou_jie_guo', row.acceptResult) || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="acceptTime" label="验收时间" width="165" />
                  <el-table-column prop="acceptBy" label="验收人" width="110" />
                  <el-table-column prop="remark" label="验收备注" min-width="180" show-overflow-tooltip />
                  <el-table-column label="附件" width="110" align="center">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.attachments?.length"
                        link
                        type="primary"
                        @click="openAcceptanceAttachList(row)"
                      >附件 ({{ row.attachments.length }})</el-button>
                      <span v-else class="declare-no-attachment-text">-</span>
                    </template>
                  </el-table-column>
                </el-table>

                <el-divider content-position="left">验收情况（事项申报 + 明细）</el-divider>
                <el-empty
                  v-if="!acceptanceProject.children?.length"
                  description="暂无申报明细"
                  :image-size="60"
                />
                <el-table
                  v-else
                  :data="acceptanceProject.children || []"
                  :row-key="acceptanceRowKey"
                  :tree-props="{ children: 'children' }"
                  default-expand-all
                  border
                  size="small"
                  class="declare-tree-table"
                >
                  <el-table-column label="申报编号" width="180">
                    <template #default="{ row }">
                      <span class="detail-code-line" :class="{ 'detail-code-line--detail': row.nodeType === 'detail' }">
                        <el-icon
                          v-if="row.nodeType === 'declare'"
                          class="detail-type-icon detail-type-icon--folder"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#22619e" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 14 1.45-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.55 6a2 2 0 0 1-1.94 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2"/></svg>
                        </el-icon>
                        <el-icon
                          v-else
                          class="detail-type-icon"
                          :class="row.detailType === 'FACILITY' ? 'detail-type-icon--facility' : 'detail-type-icon--household'"
                        >
                          <OfficeBuilding v-if="row.detailType === 'FACILITY'" />
                          <UserFilled v-else />
                        </el-icon>
                        <el-tooltip :content="row.code || '-'" placement="top" :show-after="200">
                          <span class="tree-code" :class="{ 'tree-code--detail': row.nodeType === 'detail' }">
                            {{ row.code || '-' }}
                          </span>
                        </el-tooltip>
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column label="申报标题" min-width="150" show-overflow-tooltip>
                    <template #default="{ row }">{{ row.name || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="申报类型" width="110">
                    <template #default="{ row }">
                      <template v-if="row.nodeType === 'declare'">
                        {{ dictLabel('shen_bao_lei_xing', row.source) || '-' }}
                      </template>
                      <template v-else>-</template>
                    </template>
                  </el-table-column>
                  <el-table-column label="申报类别" width="110">
                    <template #default="{ row }">
                      <template v-if="row.nodeType === 'declare'">
                        {{ dictLabel('shen_bao_lei_bie', row.category) || '-' }}
                      </template>
                      <template v-else>{{ detailTypeLabel(row.detailType) || '-' }}</template>
                    </template>
                  </el-table-column>
                  <el-table-column label="验收进度" width="150">
                    <template #default="{ row }">
                      <div v-if="row.nodeType === 'declare'" class="simple-progress">
                        <div class="simple-progress__bar">
                          <div
                            class="simple-progress__fill"
                            :style="{ width: (row.percent || 0) + '%' }"
                          />
                        </div>
                        <span class="simple-progress__score">
                          {{ row.acceptedCount || 0 }}/{{ row.totalCount || 0 }}
                        </span>
                      </div>
                      <el-tag
                        v-else
                        :type="acceptanceTagType(row.acceptStatus)"
                        size="small"
                      >{{ acceptanceStatusLabel(row.acceptStatus) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="验收资料附件" min-width="160">
                    <template #default="{ row }">
                      <template v-if="row.nodeType === 'detail'">
                        <template v-if="row.acceptanceAttachments?.length">
                          <div
                            v-for="file in row.acceptanceAttachments"
                            :key="file.id"
                            class="record-attachment"
                          >
                            <span class="attachment-file-name" :title="file.fileName" @click="openFile(file)">{{ file.fileName }}</span>
                            <el-icon class="upload-file-download" @click="openFile(file, true)"><Download /></el-icon>
                          </div>
                        </template>
                        <span v-else class="declare-no-attachment-text">-</span>
                      </template>
                      <template v-else>-</template>
                    </template>
                  </el-table-column>
                </el-table>
              </template>
              <el-empty v-else-if="!acceptanceLoading" description="暂无验收数据" :image-size="60" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <!-- 二级：事项申报资金 / 申报明细 / 立项 / 施工 / 验收 五个页签 -->
      <template v-else-if="viewRow?.nodeType === 'declare'">
        <el-tabs v-model="viewTabDeclare" @tab-change="handleViewTabChange">
          <el-tab-pane label="事项申报资金" name="declareFund">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="申报编号">{{ viewRow.code || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申报标题">{{ viewRow.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申报类别">{{ viewRow.category || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申报类型">{{ viewRow.type || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申报金额">{{ formatMoney(viewRow.budget) }} 元</el-descriptions-item>
              <el-descriptions-item label="申报内容" :span="2">{{ viewRow.raw.declareContent || '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-divider content-position="left">资金流水</el-divider>
            <el-table v-loading="recordLoading" :data="records" size="small" border>
              <el-table-column label="下拨类型" width="110">
                <template #default="{ row }">{{ dictLabel('xia_bo_lei_xing', row.fundType) || '-' }}</template>
              </el-table-column>
              <el-table-column label="拨付金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="remark" label="拨付说明" min-width="160" show-overflow-tooltip />
              <el-table-column prop="createBy" label="操作人" width="100" />
              <el-table-column prop="createTime" label="时间" width="165" />
              <el-table-column label="附件" width="130" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.attachments?.length"
                    link
                    type="primary"
                    @click="openRecordAttachList(row)"
                  >附件 ({{ row.attachments.length }})</el-button>
                  <span v-else class="declare-no-attachment-text">-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="editRecord(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="申报明细" name="declareDetail">
            <el-table :data="viewRow.raw.children || []" border>
              <el-table-column prop="name" label="农户/设施名称" min-width="150" show-overflow-tooltip />
              <el-table-column prop="code" label="户号/编码" width="140" />
              <el-table-column prop="type" label="所属区划" width="150" />
              <el-table-column prop="detailRemark" label="事项说明" min-width="140" show-overflow-tooltip />
              <el-table-column label="申报金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(row.budget) }}</template>
              </el-table-column>
              <el-table-column label="拨付金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(row.allocatedAmount) }}</template>
              </el-table-column>
              <el-table-column prop="fundRemark" label="拨付说明" min-width="140" show-overflow-tooltip />
              <el-table-column label="申报材料附件" width="130" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.materialAttachments?.length"
                    link
                    type="primary"
                    @click="openDeclareDetailAttachList(row, 'material')"
                  >附件 ({{ row.materialAttachments.length }})</el-button>
                  <span v-else class="declare-no-attachment-text">-</span>
                </template>
              </el-table-column>
              <el-table-column label="资金凭证附件" width="130" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.fundAttachments?.length"
                    link
                    type="primary"
                    @click="openDeclareDetailAttachList(row, 'fund')"
                  >附件 ({{ row.fundAttachments.length }})</el-button>
                  <span v-else class="declare-no-attachment-text">-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 立项页签：与立项管理二级查看“事项申报”页签一致 -->
          <el-tab-pane label="立项" name="approval">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="申报编号">{{ viewRow.code || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申报标题">{{ viewRow.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申报类别">{{ viewRow.category || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申报类型">{{ viewRow.type || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申报金额">{{ formatMoney(viewRow.budget) }} 元</el-descriptions-item>
              <el-descriptions-item label="申报内容" :span="2">{{ viewRow.raw.declareContent || '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-divider content-position="left">申报材料附件</el-divider>
            <el-empty
              v-if="!((viewRow.raw.declarationAttachments ?? viewRow.raw.attachments)?.length)"
              description="暂无申报材料附件"
              :image-size="60"
            />
            <el-table
              v-else
              :data="viewRow.raw.declarationAttachments?.length ? viewRow.raw.declarationAttachments : (viewRow.raw.attachments || [])"
              size="small"
              border
            >
              <el-table-column label="附件类型" width="120" align="center">
                <template #default="{ row }">
                  {{ dictLabel('li_xiang_fu_jian_lei_xing', row.attachmentType) || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="文件名称" min-width="220">
                <template #default="{ row }">
                  <div class="record-attachment">
                    <span class="attachment-file-name" :title="row.fileName" @click="openFile(row)">{{ row.fileName }}</span>
                    <el-icon class="upload-file-download" @click="openFile(row, true)"><Download /></el-icon>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>

          <!-- 施工页签：与施工管理二级查看一致（申报信息 + 明细进度） -->
          <el-tab-pane label="施工" name="construction">
            <div v-loading="constructionLoading">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="申报编号">{{ constructionDeclare?.code || '-' }}</el-descriptions-item>
                <el-descriptions-item label="申报标题">{{ constructionDeclare?.name || '-' }}</el-descriptions-item>
                <el-descriptions-item label="申报类别">
                  {{ dictLabel('shen_bao_lei_bie', constructionDeclare?.category) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="申报类型">
                  {{ dictLabel('shen_bao_lei_xing', constructionDeclare?.source) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="施工状态">
                  <span class="status-tag" :class="`status-tag--${constructionDeclare?.status}`">
                    {{ constructionStatusLabel(constructionDeclare?.status) }}
                  </span>
                  <span class="status-fraction">{{ constructionDeclare?.numerator }}/{{ constructionDeclare?.totalCount }}</span>
                </el-descriptions-item>
              </el-descriptions>

              <el-divider content-position="left">明细进度</el-divider>
              <el-empty
                v-if="!constructionDeclare?.children?.length"
                description="暂无农户/设施明细"
                :image-size="60"
              />
              <el-table v-else :data="constructionDeclare.children" size="small" border>
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
                    >{{ row.detailLogSnapshot.inspectionStatus === 'FAIL' ? '暂停施工' : constructionStatusLabel(row.detailLogSnapshot.constructionStatus) }}</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="巡检日期" width="120">
                  <template #default="{ row }">{{ row.detailLogSnapshot?.constructionDate || '-' }}</template>
                </el-table-column>
                <el-table-column label="施工内容" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.detailLogSnapshot?.content || '-' }}</template>
                </el-table-column>
                <el-table-column label="附件" width="100" align="center">
                  <template #default="{ row }">
                    <el-button
                      v-if="row.detailLogSnapshot?.attachments?.length"
                      link
                      type="primary"
                      @click="openConstructionDetailAttachList(row.detailLogSnapshot)"
                    >附件（{{ row.detailLogSnapshot.attachments.length }}）</el-button>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <!-- 验收页签：与验收管理二级查看“验收详情”一致（申报验收信息 + 验收记录 + 明细验收情况） -->
          <el-tab-pane label="验收" name="acceptance">
            <div v-loading="acceptanceLoading">
              <template v-if="acceptanceDeclare">
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="申报编号">{{ acceptanceDeclare.code || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="申报标题">{{ acceptanceDeclare.name || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="申报类型">
                    {{ dictLabel('shen_bao_lei_xing', acceptanceDeclare.source) || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="申报类别">
                    {{ dictLabel('shen_bao_lei_bie', acceptanceDeclare.category) || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="验收进度">
                    <div class="simple-progress simple-progress--inline">
                      <div class="simple-progress__bar">
                        <div
                          class="simple-progress__fill"
                          :style="{ width: (acceptanceDeclare.percent || 0) + '%' }"
                        />
                      </div>
                      <span class="simple-progress__score">
                        {{ acceptanceDeclare.acceptedCount || 0 }}/{{ acceptanceDeclare.totalCount || 0 }}
                      </span>
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="申报内容" :span="1">
                    {{ acceptanceDeclare.declareContent || '-' }}
                  </el-descriptions-item>
                </el-descriptions>

                <el-divider content-position="left">验收记录</el-divider>
                <el-empty
                  v-if="!acceptanceDeclare.acceptanceRecords?.length"
                  description="暂无验收记录"
                  :image-size="60"
                />
                <el-table v-else :data="acceptanceDeclare.acceptanceRecords" size="small" border>
                  <el-table-column label="验收结果" width="130">
                    <template #default="{ row }">
                      {{ dictLabel('yan_shou_jie_guo', row.acceptResult) || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="acceptTime" label="验收时间" width="165" />
                  <el-table-column prop="acceptBy" label="验收人" width="110" />
                  <el-table-column prop="remark" label="验收备注" min-width="180" show-overflow-tooltip />
                  <el-table-column label="附件" width="110" align="center">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.attachments?.length"
                        link
                        type="primary"
                        @click="openAcceptanceAttachList(row)"
                      >附件 ({{ row.attachments.length }})</el-button>
                      <span v-else class="declare-no-attachment-text">-</span>
                    </template>
                  </el-table-column>
                </el-table>

                <el-divider content-position="left">申报明细验收情况</el-divider>
                <el-empty
                  v-if="!acceptanceDeclare.children?.length"
                  description="暂无农户/设施明细"
                  :image-size="60"
                />
                <el-table v-else :data="acceptanceDeclare.children" border size="small">
                  <el-table-column label="序号" width="60" type="index" />
                  <el-table-column label="农户/设施名称" min-width="130" show-overflow-tooltip>
                    <template #default="{ row }">{{ row.name || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="户号/编码" width="130">
                    <template #default="{ row }">{{ row.code || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="所属区划" width="140">
                    <template #default="{ row }">{{ row.source || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="事项说明" min-width="140" show-overflow-tooltip>
                    <template #default="{ row }">{{ row.detailRemark || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="验收状态" width="100">
                    <template #default="{ row }">
                      <el-tag :type="acceptanceTagType(row.acceptStatus)" size="small">
                        {{ acceptanceStatusLabel(row.acceptStatus) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="验收资料附件" min-width="160">
                    <template #default="{ row }">
                      <template v-if="row.acceptanceAttachments?.length">
                        <div
                          v-for="file in row.acceptanceAttachments"
                          :key="file.id"
                          class="record-attachment"
                        >
                          <span class="attachment-file-name" :title="file.fileName" @click="openFile(file)">{{ file.fileName }}</span>
                          <el-icon class="upload-file-download" @click="openFile(file, true)"><Download /></el-icon>
                        </div>
                      </template>
                      <span v-else class="declare-no-attachment-text">-</span>
                    </template>
                  </el-table-column>
                </el-table>
              </template>
              <el-empty v-else-if="!acceptanceLoading" description="暂无验收数据" :image-size="60" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <!-- 三级：明细资金 / 立项 / 施工 / 验收 四个页签 -->
      <template v-else>
        <el-tabs v-model="viewTabDetail" @tab-change="handleViewTabChange">
          <el-tab-pane label="明细资金" name="fund">
            <el-descriptions :column="2" border>
              <template v-if="viewRow">
                <el-descriptions-item label="农户/设施名称">{{ viewRow.name || '-' }}</el-descriptions-item>
                <el-descriptions-item label="户号/编码">{{ viewRow.code || '-' }}</el-descriptions-item>
                <el-descriptions-item label="所属区划">{{ viewRow.type || '-' }}</el-descriptions-item>
                <el-descriptions-item label="事项说明">{{ viewRow.raw.detailRemark || '-' }}</el-descriptions-item>
                <el-descriptions-item label="申报金额">{{ formatMoney(viewRow.budget) }} 元</el-descriptions-item>
                <el-descriptions-item label="拨付金额">{{ formatMoney(viewRow.allocatedAmount) }} 元</el-descriptions-item>
              </template>
            </el-descriptions>

            <el-divider content-position="left">资金流水</el-divider>
            <el-table v-loading="recordLoading" :data="records" size="small" border>
              <el-table-column label="下拨类型" width="110">
                <template #default="{ row }">{{ dictLabel('xia_bo_lei_xing', row.fundType) || '-' }}</template>
              </el-table-column>
              <el-table-column label="拨付金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="remark" label="拨付说明" min-width="160" show-overflow-tooltip />
              <el-table-column prop="createBy" label="操作人" width="100" />
              <el-table-column prop="createTime" label="时间" width="165" />
              <el-table-column label="附件" min-width="190">
                <template #default="{ row }">
                  <template v-if="row.attachments?.length">
                    <div v-for="file in row.attachments" :key="file.id" class="record-attachment">
                      <span class="attachment-file-name" :title="file.fileName" @click="openFile(file)">{{ file.fileName }}</span>
                      <el-icon class="upload-file-download" @click="openFile(file, true)"><Download /></el-icon>
                    </div>
                  </template>
                  <span v-else class="declare-no-attachment-text">-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="editRecord(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 立项页签：与立项管理三级查看一致 -->
          <el-tab-pane label="立项" name="approval">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="明细类型">{{ detailTypeLabel(viewRow?.raw.detailType) }}</el-descriptions-item>
              <el-descriptions-item label="农户/设施名称">{{ viewRow?.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="户号/编码">{{ viewRow?.code || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属区划">{{ viewRow?.type || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申报金额">{{ formatMoney(viewRow?.budget) }} 元</el-descriptions-item>
              <el-descriptions-item label="事项说明">{{ viewRow?.raw.detailRemark || '-' }}</el-descriptions-item>
            </el-descriptions>
            <!-- 申报材料附件：与立项管理三级查看一致，展示明细自身的附件 -->
            <el-divider content-position="left">申报材料附件</el-divider>
            <el-empty
              v-if="!viewRow?.raw.approvalAttachments?.length"
              description="暂无申报材料附件"
              :image-size="60"
            />
            <el-table v-else :data="viewRow.raw.approvalAttachments" size="small" border>
              <el-table-column label="附件类型" width="120" align="center">
                <template #default="{ row }">{{ dictLabel('fu_jian_lei_xing', row.attachmentType) || '-' }}</template>
              </el-table-column>
              <el-table-column label="文件名称" min-width="220">
                <template #default="{ row }">
                  <div class="attachment-file-cell">
                    <span class="attachment-file-name" :title="row.fileName" @click="openFile(row)">{{ row.fileName }}</span>
                    <el-icon class="upload-file-download" title="下载" @click.stop="openFile(row, true)">
                      <Download />
                    </el-icon>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>

          <!-- 施工页签：与施工管理三级查看一致（施工记录 + 附件） -->
          <el-tab-pane label="施工" name="construction">
            <div v-loading="constructionLoading">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="记录日期">{{ constructionLog?.constructionDate || '-' }}</el-descriptions-item>
                <el-descriptions-item label="巡检人">{{ constructionLog?.inspector || '-' }}</el-descriptions-item>
                <el-descriptions-item label="巡检状态">
                  <span
                    v-if="constructionLog?.inspectionStatus"
                    class="status-tag"
                    :class="`status-tag--${constructionLog.inspectionStatus === 'PASS' ? 'COMPLETED' : 'FAIL'}`"
                  >{{ inspectionLabel(constructionLog.inspectionStatus) }}</span>
                  <span v-else>-</span>
                </el-descriptions-item>
                <el-descriptions-item label="施工状态">
                  <span
                    class="status-tag"
                    :class="constructionLog?.inspectionStatus === 'FAIL' ? 'status-tag--SUSPENDED' : `status-tag--${constructionLog?.constructionStatus}`"
                  >{{ constructionLog?.inspectionStatus === 'FAIL' ? '暂停施工' : constructionStatusLabel(constructionLog?.constructionStatus) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="施工内容描述" :span="2">
                  {{ constructionLog?.content || '-' }}
                </el-descriptions-item>
              </el-descriptions>

              <el-divider content-position="left">附件</el-divider>
              <el-empty v-if="!constructionLog?.attachments?.length" description="暂无附件" :image-size="60" />
              <el-table v-else :data="constructionLog.attachments" size="small" border>
                <el-table-column label="附件类型" width="120" align="center">
                  <template #default="{ row }">
                    {{ dictLabel('shi_gong_fu_jian_lei_xing', row.attachmentType) || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="附件" min-width="220">
                  <template #default="{ row }">
                    <div class="record-attachment">
                      <span class="attachment-file-name" :title="row.fileName" @click="openFile(row)">{{ row.fileName || '查看附件' }}</span>
                      <el-icon class="upload-file-download" @click="openFile(row, true)"><Download /></el-icon>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="备注" min-width="180">
                  <template #default="{ row }">{{ row.remark || '-' }}</template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <!-- 验收页签：与验收管理三级查看“验收详情”一致（最新验收信息 + 验收记录） -->
          <el-tab-pane label="验收" name="acceptance">
            <div v-loading="acceptanceLoading">
              <template v-if="acceptanceDetail">
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="农户/设施名称">{{ acceptanceDetail.name || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="户号/编码">{{ acceptanceDetail.code || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="所属区划">{{ acceptanceDetail.source || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="事项说明">{{ acceptanceDetail.detailRemark || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="验收状态">
                    <el-tag :type="acceptanceTagType(acceptanceDetail.acceptStatus)" size="small">
                      {{ acceptanceStatusLabel(acceptanceDetail.acceptStatus) }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="验收结果">
                    {{ dictLabel('yan_shou_jie_guo', acceptanceDetail.acceptResult) || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="验收时间">{{ acceptanceDetail.acceptTime || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="验收人">{{ acceptanceDetail.acceptBy || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="验收备注" :span="2">
                    {{ acceptanceDetail.acceptRemark || '-' }}
                  </el-descriptions-item>
                </el-descriptions>

                <el-divider content-position="left">验收记录</el-divider>
                <el-empty
                  v-if="!acceptanceDetail.acceptanceRecords?.length"
                  description="暂无验收记录"
                  :image-size="60"
                />
                <el-table v-else :data="acceptanceDetail.acceptanceRecords" size="small" border>
                  <el-table-column label="验收结果" width="130">
                    <template #default="{ row }">
                      {{ dictLabel('yan_shou_jie_guo', row.acceptResult) || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="acceptTime" label="验收时间" width="165" />
                  <el-table-column prop="acceptBy" label="验收人" width="110" />
                  <el-table-column prop="remark" label="验收备注" min-width="180" show-overflow-tooltip />
                  <el-table-column label="附件" width="110" align="center">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.attachments?.length"
                        link
                        type="primary"
                        @click="openAcceptanceAttachList(row)"
                      >附件 ({{ row.attachments.length }})</el-button>
                      <span v-else class="declare-no-attachment-text">-</span>
                    </template>
                  </el-table-column>
                </el-table>
              </template>
              <el-empty v-else-if="!acceptanceLoading" description="暂无验收数据" :image-size="60" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 附件列表弹窗：一级事项申报页签点击“附件 (n)”查看 -->
    <el-dialog
      v-model="attachListVisible"
      :title="attachListTitle"
      width="720px"
      append-to-body
      destroy-on-close
    >
      <el-table :data="attachListData" size="small" border>
        <el-table-column label="附件类型" width="120" align="center">
          <template #default="{ row }">
            {{ dictLabel(attachDictType, row.attachmentType) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="文件名" min-width="260">
          <template #default="{ row }">
            <div class="record-attachment">
              <span class="attachment-file-name" :title="row.fileName" @click="openFile(row)">{{ row.fileName }}</span>
              <el-icon class="upload-file-download" @click="openFile(row, true)"><Download /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="attachListVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 资金登记 / 下拨资金弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="formTitle"
      :width="'900px'"
      destroy-on-close
      class="fund-form-dialog"
    >
      <!-- 一级项目 / 二级申报 / 三级明细：基础信息 + 资金拨付信息 + 资金凭证附件 -->
      <template v-if="formRow">
        <el-divider content-position="left">
          {{
            formRow.nodeType === 'approval'
              ? '项目基础信息'
              : formRow.nodeType === 'declare'
                ? '事项申报基础信息'
                : '申报明细基础信息'
          }}
        </el-divider>
        <el-descriptions :column="2" border size="small">
          <template v-if="formRow.nodeType === 'approval'">
            <el-descriptions-item label="项目编号">{{ formRow.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目名称">{{ formRow.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目类别">{{ formRow.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目类型">{{ formRow.type || '-' }}</el-descriptions-item>
          </template>
          <template v-else-if="formRow.nodeType === 'declare'">
            <el-descriptions-item label="申报编号">{{ formRow.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申报标题">{{ formRow.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申报类型">{{ formRow.type || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申报类别">{{ formRow.category || '-' }}</el-descriptions-item>
          </template>
          <template v-else>
            <el-descriptions-item label="农户/设施名称">{{ formRow.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="户号/编码">{{ formRow.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属区划">{{ formRow.type || '-' }}</el-descriptions-item>
            <el-descriptions-item label="事项说明">{{ formRow.raw.detailRemark || '-' }}</el-descriptions-item>
          </template>
        </el-descriptions>

        <el-divider content-position="left">资金拨付信息</el-divider>
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="96px" class="fund-form">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item
                :label="formRow.nodeType === 'approval' ? '资金类型' : '下拨类型'"
                prop="fundType"
              >
                <el-select
                  v-model="formData.fundType"
                  :placeholder="formRow.nodeType === 'approval' ? '请选择资金类型' : '请选择下拨类型'"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in dictOptions(formRow.nodeType === 'approval' ? 'zi_jin_lei_xing' : 'xia_bo_lei_xing')"
                    :key="item.dictValue"
                    :label="item.dictLabel"
                    :value="item.dictValue"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col v-if="formRow.nodeType === 'approval'" :span="12">
              <el-form-item label="资金来源" prop="fundSource">
                <el-select
                  v-model="formData.fundSource"
                  placeholder="请选择或输入资金来源"
                  filterable
                  allow-create
                  default-first-option
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in dictOptions('zi_jin_lai_yuan')"
                    :key="item.dictValue"
                    :label="item.dictLabel"
                    :value="item.dictValue"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="金额（元）" prop="amount">
                <el-input-number
                  v-model="formData.amount"
                  :min="0.01"
                  :max="formRow.nodeType === 'approval' ? undefined : formLimit"
                  :precision="2"
                  :controls="false"
                  placeholder="请输入金额"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="操作人" prop="operator">
                <el-input
                  v-model="formData.operator"
                  maxlength="32"
                  placeholder="请输入操作人"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="时间" prop="operateTime">
                <el-date-picker
                  v-model="formData.operateTime"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="请选择时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="拨付说明" prop="remark">
                <el-input
                  v-model="formData.remark"
                  type="textarea"
                  :rows="3"
                  maxlength="512"
                  show-word-limit
                  placeholder="请输入拨付说明"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <!-- 资金凭证附件：标题与“添加附件”同行，卡片式多条维护 -->
        <div class="attachment-section-header">
          <span class="attachment-section-title">资金凭证附件</span>
          <el-button link type="primary" icon="Plus" @click="addAttachment">添加附件</el-button>
        </div>
        <div v-if="!formAttachments.length" class="attachment-empty">
          暂无附件，点击上方“添加附件”上传资金凭证
        </div>
        <div v-for="(item, index) in formAttachments" :key="index" class="attachment-card">
          <div class="attachment-card-header">
            <span class="attachment-index">{{ index + 1 }}</span>
            <span class="attachment-card-title">附件 #{{ index + 1 }}</span>
            <el-button link type="danger" icon="Delete" @click="removeAttachment(index)">删除</el-button>
          </div>
          <div class="attachment-card-body">
            <div class="attachment-field">
              <span class="attachment-field-label">附件类型</span>
              <el-select v-model="item.attachmentType" placeholder="请选择附件类型" clearable style="width: 100%">
                <el-option
                  v-for="opt in dictOptions('zi_jin_fu_jian_lei_xing')"
                  :key="opt.dictValue"
                  :label="opt.dictLabel"
                  :value="opt.dictValue"
                />
              </el-select>
            </div>
            <div class="attachment-field">
              <span class="attachment-field-label">上传附件</span>
              <el-upload
                :show-file-list="false"
                :before-upload="uploadBefore(index)"
              >
                <div class="upload-trigger">
                  <el-icon><Upload /></el-icon>
                  <span>{{ uploadingIndex === index ? '上传中...' : '点击选择文件...' }}</span>
                </div>
              </el-upload>
              <div v-if="item.fileName" class="upload-file-item">
                <el-icon class="upload-file-icon"><Document /></el-icon>
                <span class="upload-file-name" :title="item.fileName">{{ item.fileName }}</span>
                <el-icon
                  class="upload-file-download"
                  title="下载"
                  @click="openFile(item, true)"
                >
                  <Download />
                </el-icon>
                <el-icon class="upload-file-remove" title="移除" @click="clearAttachmentFile(index)">
                  <Close />
                </el-icon>
              </div>
            </div>
            <div class="attachment-field">
              <span class="attachment-field-label">备注</span>
              <el-input v-model="item.remark" maxlength="256" placeholder="请输入备注" />
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getDictByType } from '@/api/dict'
import { uploadFile, getPresignUrl, type FileUploadVO } from '@/api/file'
import type { DictVO } from '@/types/system'
import {
  getFundPage,
  getFundRecords,
  registerFund,
  allocateFund,
  updateFundRecord,
  fetchFundAcceptanceDetail,
  type FundNodeVO,
  type FundNodeType,
  type FundRecordVO,
  type FundSaveDTO,
  type FundUpdateDTO,
  type FundAttachmentVO,
  type DeclareAttachmentVO
} from '@/api/fund'
import type { AcceptanceNodeVO, AcceptanceRecordVO } from '@/api/acceptance'
import {
  getConstructionDetail,
  getDetailLog,
  type ConstructionAttachmentVO,
  type ConstructionNodeVO,
  type ConstructionLogVO
} from '@/api/construction'

/**
 * 资金管理
 * 所属菜单：项目全周期管控 → 资金管理
 * 数据来源：立项管理中审核通过的项目；三级树形：项目 → 申报内容 → 农户/公共基础设施明细
 */
const userStore = useUserStore()

const canView = computed(() => userStore.hasPerm('project:fund:view'))
const canRegister = computed(() => userStore.hasPerm('project:fund:register'))
const canAllocate = computed(() => userStore.hasPerm('project:fund:allocate'))

/* ============ 字典 ============ */
const dictMap = ref<Record<string, DictVO[]>>({})
const DICT_TYPES = [
  'xiang_mu_lei_bie',
  'xiang_mu_lei_xing',
  'shen_bao_lei_bie',
  'shen_bao_lei_xing',
  'zi_jin_lai_yuan',
  'zi_jin_lei_xing',
  'zi_jin_fu_jian_lei_xing',
  'xia_bo_lei_xing',
  'li_xiang_fu_jian_lei_xing',
  'fu_jian_lei_xing',
  'shi_gong_fu_jian_lei_xing',
  'yan_shou_jie_guo',
  'yan_shou_fu_jian_lei_xing'
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
  return value || ''
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
interface DisplayRow {
  rowKey: string
  nodeType: FundNodeType
  detailType?: string
  /** 节点ID：项目ID / 申报ID / 明细ID */
  nodeId: string
  approvalId: string
  declareId?: string
  detailId?: string
  code: string
  name: string
  category: string
  type: string
  budget?: number
  fundSource: string
  allocatedAmount?: number
  balance?: number
  raw: FundNodeVO
  children?: DisplayRow[]
  /** 是否含子节点，用于渲染展开箭头（表格数据传入前会剥离 children，避免 el-table 走树形模式） */
  hasChildren?: boolean
}

const queryForm = reactive({
  projectName: '',
  projectCategory: '',
  /** 项目类型：后端字段为 projectSource，字典为 xiang_mu_lei_xing */
  projectSource: ''
})
const loading = ref(false)
const tableData = ref<DisplayRow[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

/** 已收起的节点集合（默认全部收起） */
const collapsedKeys = ref<Set<string>>(new Set())

/** 收集所有含子节点的行标识，用于默认收起全部树节点 */
const collectExpandableKeys = (rows: DisplayRow[]): Set<string> => {
  const keys = new Set<string>()
  const walk = (list: DisplayRow[]) => {
    list.forEach((row) => {
      if (row.children?.length) {
        keys.add(row.rowKey)
        walk(row.children)
      }
    })
  }
  walk(rows)
  return keys
}

/** 手写展开/收起，替代 el-table 树形模式，保证三个层级的缩进与立项管理完全一致 */
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
  row.nodeType === 'detail' ? 'fund-detail-row' : ''

const toDisplay = (node: FundNodeVO, approvalId: string, declareId?: string): DisplayRow => ({
  rowKey: `${node.nodeType[0]}_${node.id}`,
  nodeType: node.nodeType,
  detailType: node.detailType,
  nodeId: node.id,
  approvalId,
  declareId: node.nodeType === 'declare' ? node.id : declareId,
  detailId: node.nodeType === 'detail' ? node.id : undefined,
  code: node.code || '',
  name: node.name || '',
  category:
    node.nodeType === 'approval'
      ? dictLabel('xiang_mu_lei_bie', node.category)
      : node.nodeType === 'declare'
        ? dictLabel('shen_bao_lei_bie', node.category)
        : detailTypeLabel(node.category),
  type:
    node.nodeType === 'approval'
      ? dictLabel('xiang_mu_lei_xing', node.type)
      : node.nodeType === 'declare'
        ? dictLabel('shen_bao_lei_xing', node.type)
        : node.type || '',
  budget: node.budget,
  fundSource: node.fundSource || '',
  allocatedAmount: node.allocatedAmount,
  balance: node.balance,
  raw: node,
  hasChildren: (node.children || []).length > 0,
  children: (node.children || []).map((child) =>
    toDisplay(child, approvalId, node.nodeType === 'declare' ? node.id : declareId)
  )
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getFundPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      projectName: queryForm.projectName || undefined,
      projectCategory: queryForm.projectCategory || undefined,
      projectSource: queryForm.projectSource || undefined
    })
    tableData.value = (res.data.records || []).map((node) => toDisplay(node, node.id))
    // 默认全部收起：仅展示一级项目行，展开后再逐级查看申报与明细
    collapsedKeys.value = collectExpandableKeys(tableData.value)
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
  queryForm.projectName = ''
  queryForm.projectCategory = ''
  queryForm.projectSource = ''
  pageNum.value = 1
  loadData()
}

/* ============ 格式化 ============ */
const formatMoney = (value?: number): string => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/* ============ 查看弹窗 ============ */
const viewVisible = ref(false)
const viewRow = ref<DisplayRow | null>(null)
const records = ref<FundRecordVO[]>([])
const recordLoading = ref(false)
/**
 * 当前查看页签
 * 一级：fund 项目资金 / declare 事项申报 / approval 立项 / construction 施工 / acceptance 验收
 * 二级：declareFund 事项申报资金 / declareDetail 申报明细 / approval 立项 / construction 施工 / acceptance 验收
 * 三级：fund 明细资金 / approval 立项 / construction 施工 / acceptance 验收
 */
const viewTab = ref<'fund' | 'approval' | 'declare' | 'construction' | 'acceptance'>('fund')
const viewTabDeclare = ref<
  'declareFund' | 'approval' | 'construction' | 'declareDetail' | 'acceptance'
>('declareFund')
const viewTabDetail = ref<'fund' | 'approval' | 'construction' | 'acceptance'>('fund')

/* ============ 查看弹窗-施工页签数据 ============ */
const constructionLoading = ref(false)
/** 一级/二级：项目施工详情（含申报、明细进度与施工单位信息） */
const constructionView = ref<ConstructionNodeVO | null>(null)
/** 三级：明细最新施工记录（巡检、施工内容与附件） */
const constructionLog = ref<ConstructionLogVO | null>(null)

/** 一级施工页签：全部申报下的明细展平，一户一条 */
const constructionDetails = computed(() => {
  if (!constructionView.value?.children?.length) return []
  return constructionView.value.children.flatMap((declare) => declare.children || [])
})

/** 二级施工页签：取当前申报节点 */
const constructionDeclare = computed(() => {
  if (viewRow.value?.nodeType !== 'declare') return null
  return constructionView.value?.children?.find((item) => item.declareId === viewRow.value?.declareId) || null
})

/** 施工状态文案 */
const constructionStatusLabel = (status?: string): string => {
  if (status === 'COMPLETED') return '已完工'
  if (status === 'IN_PROGRESS') return '施工中'
  return '未开工'
}

/** 巡检状态文案 */
const inspectionLabel = (status?: string): string => {
  if (status === 'PASS') return '合格'
  if (status === 'FAIL') return '整改'
  return '-'
}

/** 切换到施工页签时按需加载施工数据，避免每次查看都额外请求 */
const loadConstruction = async () => {
  const row = viewRow.value
  if (!row) return
  if (row.nodeType === 'detail') {
    if (constructionLog.value) return
    constructionLoading.value = true
    try {
      const res = await getDetailLog(row.nodeId)
      constructionLog.value = res.data
    } finally {
      constructionLoading.value = false
    }
    return
  }
  if (constructionView.value) return
  constructionLoading.value = true
  try {
    const res = await getConstructionDetail(row.approvalId)
    constructionView.value = res.data
  } finally {
    constructionLoading.value = false
  }
}

const handleViewTabChange = (name: string | number) => {
  if (name === 'construction') loadConstruction()
  if (name === 'acceptance') loadAcceptance()
}

/* ============ 查看弹窗-验收页签数据 ============ */
const acceptanceLoading = ref(false)
/** 验收三级树形（项目 → 申报 → 明细，含各级验收记录），与验收管理口径一致 */
const acceptanceView = ref<AcceptanceNodeVO | null>(null)

/** 一级验收页签：项目节点 */
const acceptanceProject = computed(() =>
  viewRow.value?.nodeType === 'approval' ? acceptanceView.value : null
)

/** 二级/三级验收页签：所属申报节点 */
const acceptanceDeclare = computed(() => {
  const row = viewRow.value
  if (!row || row.nodeType === 'approval') return null
  return (
    acceptanceView.value?.children?.find(
      (item) => String(item.declareId) === String(row.declareId)
    ) || null
  )
})

/** 三级验收页签：当前明细节点 */
const acceptanceDetail = computed(() => {
  const row = viewRow.value
  if (row?.nodeType !== 'detail') return null
  return (
    acceptanceDeclare.value?.children?.find(
      (item) => String(item.detailId ?? item.id) === String(row.detailId)
    ) || null
  )
})

/** 验收树形表格行标识：申报与明细ID来自不同表，拼接节点类型避免重复 */
const acceptanceRowKey = (row: AcceptanceNodeVO): string => `${row.nodeType}_${row.id}`

/** 验收状态标签类型：已验收/未通过/待验收 */
const acceptanceTagType = (status?: string): 'success' | 'danger' | 'warning' => {
  if (status === 'ACCEPTED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

/** 验收状态文案 */
const acceptanceStatusLabel = (status?: string): string => {
  if (status === 'ACCEPTED') return '已验收'
  if (status === 'REJECTED') return '未通过'
  return '待验收'
}

/** 切换到验收页签时按需加载，避免每次查看都额外请求 */
const loadAcceptance = async () => {
  const row = viewRow.value
  if (!row || acceptanceView.value) return
  acceptanceLoading.value = true
  try {
    acceptanceView.value = await fetchFundAcceptanceDetail(row.approvalId)
  } finally {
    acceptanceLoading.value = false
  }
}

const viewTitle = computed(() => {
  if (viewRow.value?.nodeType === 'declare') return '申报资金详情'
  if (viewRow.value?.nodeType === 'detail') return '明细资金详情'
  return '项目资金详情'
})

/** 事项申报页签树形行：一级申报信息 + 二级明细信息 */
interface DeclareTreeRow {
  rowKey: string
  /** 节点类型：declare申报 detail明细 */
  nodeType: 'declare' | 'detail'
  /** 明细类型：HOUSEHOLD农户 FACILITY公共基础设施（仅明细） */
  detailType?: string
  code: string
  title: string
  declareType: string
  declareCategory: string
  amount?: number
  allocated?: number
  village: string
  attachments: DeclareAttachmentVO[]
  /** 资金凭证附件（本节点下拨记录的附件） */
  fundAttachments: FundAttachmentVO[]
  children?: DeclareTreeRow[]
}

const viewDeclareTree = computed<DeclareTreeRow[]>(() => {
  const declareNodes = viewRow.value?.raw.children || []
  return declareNodes.map((declare) => ({
    rowKey: `vd_${declare.id}`,
    nodeType: 'declare',
    code: declare.code || '',
    title: declare.name || '',
    declareType: dictLabel('shen_bao_lei_xing', declare.type),
    declareCategory: dictLabel('shen_bao_lei_bie', declare.category),
    amount: declare.budget,
    allocated: declare.allocatedAmount,
    village: [declare.townName, declare.villageName].filter(Boolean).join(' / '),
    attachments: declare.attachments || [],
    fundAttachments: declare.fundAttachments || [],
    children: (declare.children || []).map((detail) => ({
      rowKey: `vt_${detail.id}`,
      nodeType: 'detail',
      detailType: detail.detailType,
      code: detail.code || '',
      title: detail.name || '',
      declareType: detailTypeLabel(detail.detailType),
      declareCategory: detailTypeLabel(detail.category),
      amount: detail.budget,
      allocated: detail.allocatedAmount,
      village: detail.type || '',
      attachments: detail.materialAttachments || [],
      fundAttachments: detail.fundAttachments || []
    }))
  }))
})

/** 事项申报页签：明细行字号略小，与列表三级一致 */
const declareRowClassName = ({ row }: { row: DeclareTreeRow }): string =>
  row.nodeType === 'detail' ? 'declare-detail-row' : ''

/** 打开附件：预览或下载（MinIO 时效签名URL，禁止永久直链） */
const openFile = async (file: { bucket?: string; fileKey?: string; fileName?: string }, download = false) => {
  if (!file?.fileKey) return
  const res = await getPresignUrl({
    bucket: file.bucket || 'archive-formal',
    fileKey: file.fileKey,
    fileName: file.fileName,
    download
  })
  window.open(res.data, '_blank')
}

const openView = async (row: DisplayRow) => {
  viewRow.value = row
  viewTab.value = 'fund'
  viewTabDeclare.value = 'declareFund'
  viewTabDetail.value = 'fund'
  constructionView.value = null
  constructionLog.value = null
  acceptanceView.value = null
  records.value = []
  viewVisible.value = true
  await loadRecords(row.nodeType, row.nodeId)
}

/** 刷新当前查看节点的资金流水列表 */
const loadRecords = async (nodeType?: FundNodeType, nodeId?: string) => {
  const type: FundNodeType | undefined = nodeType || (viewRow.value as DisplayRow | null)?.nodeType
  const id: string = nodeId || (viewRow.value as DisplayRow | null)?.nodeId || ''
  if (!type || !id) return
  recordLoading.value = true
  try {
    const res = await getFundRecords(type, id)
    records.value = res.data || []
  } finally {
    recordLoading.value = false
  }
}

/* ============ 附件列表弹窗（一级事项申报页签） ============ */
type AttachListItem = DeclareAttachmentVO | FundAttachmentVO | ConstructionAttachmentVO
const attachListVisible = ref(false)
const attachListTitle = ref('')
/** 附件类型字典：一级/二级申报材料 li_xiang_fu_jian_lei_xing，三级明细材料 fu_jian_lei_xing，资金凭证 zi_jin_fu_jian_lei_xing */
const attachDictType = ref('li_xiang_fu_jian_lei_xing')
const attachListData = ref<AttachListItem[]>([])

const openAttachList = (row: DeclareTreeRow, type: 'material' | 'fund') => {
  const files = type === 'material' ? row.attachments : row.fundAttachments
  attachListData.value = files || []
  // 申报节点材料用 li_xiang_fu_jian_lei_xing，明细节点材料用 fu_jian_lei_xing（与立项管理一致）
  if (type === 'material') {
    attachDictType.value = row.nodeType === 'detail' ? 'fu_jian_lei_xing' : 'li_xiang_fu_jian_lei_xing'
  } else {
    attachDictType.value = 'zi_jin_fu_jian_lei_xing'
  }
  attachListTitle.value = `${type === 'material' ? '申报材料附件' : '资金凭证附件'} — ${[row.code, row.title].filter(Boolean).join(' ')}`
  attachListVisible.value = true
}

/** 二级查看-事项申报资金页签：申报单附件（Declaration.attachments 优先），弹出附件列表 */
const openDeclareAttachList = () => {
  const row = viewRow.value
  if (!row) return
  attachListData.value = row.raw.declarationAttachments?.length
    ? row.raw.declarationAttachments
    : row.raw.attachments || []
  attachDictType.value = 'li_xiang_fu_jian_lei_xing'
  attachListTitle.value = `申报材料附件 — ${[row.code, row.name].filter(Boolean).join(' ')}`
  attachListVisible.value = true
}

/** 三级查看-明细资金页签：申报材料附件按钮，弹出附件列表（明细附件字典为 fu_jian_lei_xing） */
const openDetailAttachList = () => {
  const row = viewRow.value
  if (!row) return
  attachListData.value = row.raw.materialAttachments || []
  attachDictType.value = 'fu_jian_lei_xing'
  attachListTitle.value = `申报材料附件 — ${[row.code, row.name].filter(Boolean).join(' ')}`
  attachListVisible.value = true
}

/** 二级查看-申报明细页签：明细行附件按钮，弹出附件列表（明细材料用 fu_jian_lei_xing，资金凭证用 zi_jin_fu_jian_lei_xing） */
const openDeclareDetailAttachList = (row: FundNodeVO, type: 'material' | 'fund') => {
  const files = type === 'material' ? row.materialAttachments : row.fundAttachments
  attachListData.value = files || []
  attachDictType.value = type === 'material' ? 'fu_jian_lei_xing' : 'zi_jin_fu_jian_lei_xing'
  attachListTitle.value = `${type === 'material' ? '申报材料附件' : '资金凭证附件'} — ${[row.code, row.name].filter(Boolean).join(' ')}`
  attachListVisible.value = true
}

/** 一级/二级资金流水：某条流水的资金凭证附件按钮，弹出附件列表 */
const openRecordAttachList = (record: FundRecordVO) => {
  attachListData.value = record.attachments || []
  attachDictType.value = 'zi_jin_fu_jian_lei_xing'
  attachListTitle.value = `资金凭证附件 — ${[record.createBy, record.createTime].filter(Boolean).join(' ')}`
  attachListVisible.value = true
}

/** 验收记录：验收资料附件按钮，弹出附件列表（字典 yan_shou_fu_jian_lei_xing） */
const openAcceptanceAttachList = (record: AcceptanceRecordVO) => {
  attachListData.value = record.attachments || []
  attachDictType.value = 'yan_shou_fu_jian_lei_xing'
  attachListTitle.value = `验收资料附件 — ${[record.acceptBy, record.acceptTime].filter(Boolean).join(' ')}`
  attachListVisible.value = true
}

/** 一级查看-施工页签：明细进度附件按钮，弹出附件列表（与施工管理一致的施工附件字典） */
const openConstructionDetailAttachList = (snapshot?: ConstructionLogVO | null) => {
  attachListData.value = snapshot?.attachments || []
  attachDictType.value = 'shi_gong_fu_jian_lei_xing'
  attachListTitle.value = '附件'
  attachListVisible.value = true
}

/* ============ 资金登记 / 下拨弹窗 ============ */
const formVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const formRow = ref<DisplayRow | null>(null)
/** 编辑态：非 null 时为资金流水 ID，走 PUT /record 提交 */
const editingId = ref<string | null>(null)
/** 编辑态原始金额，用于计算金额输入框 max（上级可用余额 + 原金额） */
const editingOriginalAmount = ref<number>(0)
/** 可下拨上限：二级取一级结余，三级取所属申报的可下拨余额；一级不限制（展示项目预算供参考）；编辑态时加上原金额 */
const formLimit = ref<number | undefined>(undefined)

const formTitle = computed(() => {
  if (editingId.value) return '编辑资金流水'
  return formRow.value?.nodeType === 'approval' ? '资金登记' : '下拨资金'
})

/** 资金凭证附件表单项 */
interface FormAttachment extends FundAttachmentVO {
  attachmentType?: string
  remark?: string
}
const formAttachments = ref<FormAttachment[]>([])
const uploadingIndex = ref<number | null>(null)

/** 添加一条附件卡片 */
const addAttachment = () => {
  formAttachments.value.push({ fileKey: '', fileName: '', bucket: 'archive-formal', attachmentType: '', remark: '' })
}

/** 删除附件卡片 */
const removeAttachment = (index: number) => {
  formAttachments.value.splice(index, 1)
}

/** 仅移除已上传文件，保留卡片 */
const clearAttachmentFile = (index: number) => {
  const item = formAttachments.value[index]
  item.fileKey = ''
  item.fileName = ''
  item.fileSize = undefined
}

/** 上传资金凭证到正式归档桶，返回 false 阻止组件自动上传 */
const handleAttachmentUpload = async (file: File, index: number) => {
  uploadingIndex.value = index
  try {
    const res = await uploadFile(file)
    const data: FileUploadVO = res.data
    const item = formAttachments.value[index]
    item.fileName = data.fileName
    item.fileKey = data.fileKey
    item.bucket = data.bucket
    item.fileSize = data.fileSize
  } finally {
    uploadingIndex.value = null
  }
  return false
}

/** 生成指定附件卡片的上传前置回调 */
const uploadBefore = (index: number) => (file: File) => handleAttachmentUpload(file, index)

const formData = reactive({
  fundType: '',
  fundSource: '',
  amount: undefined as number | undefined,
  /** 操作人：人工录入 */
  operator: '',
  /** 时间：人工选择 */
  operateTime: '',
  remark: ''
})

/** 资金类型（一级）/下拨类型（二级、三级）必填；资金来源仅一级登记必填 */
const formRules = computed<FormRules>(() => ({
  fundType: [
    {
      required: true,
      message: formRow.value?.nodeType === 'approval' ? '请选择资金类型' : '请选择下拨类型',
      trigger: 'change'
    }
  ],
  fundSource:
    formRow.value?.nodeType === 'approval'
      ? [{ required: true, message: '请选择资金来源', trigger: 'change' }]
      : [],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
  operateTime: [{ required: true, message: '请选择时间', trigger: 'change' }]
}))

/** 在树中查找指定 rowKey 的节点 */
const findRow = (key: string, list: DisplayRow[] = tableData.value): DisplayRow | undefined => {
  for (const item of list) {
    if (item.rowKey === key) return item
    const hit = item.children?.length ? findRow(key, item.children) : undefined
    if (hit) return hit
  }
  return undefined
}

const openForm = (row: DisplayRow) => {
  // 先计算可下拨额度：额度不足时直接提示并返回，避免金额输入框 max 小于 min(0.01)
  // 触发 Element Plus InputNumber 渲染异常，导致整页后续点击全部失效
  let limit: number | undefined
  if (row.nodeType === 'approval') {
    limit = row.budget
  } else if (row.nodeType === 'declare') {
    // 二级下拨可用额度 = 一级登记金额 - 已下拨金额（即一级结余）
    const approvalRow = findRow(`a_${row.approvalId}`)
    limit = Number(approvalRow?.balance ?? 0)
  } else {
    // 三级下拨可用额度 = 所属申报已下拨金额 - 三级已下拨合计
    const declareRow = findRow(`d_${row.declareId}`)
    const allocated = Number(declareRow?.allocatedAmount ?? 0)
    const detailAllocated = (declareRow?.children || []).reduce(
      (sum, child) => sum + Number(child.allocatedAmount || 0),
      0
    )
    limit = Math.max(allocated - detailAllocated, 0)
  }
  if (row.nodeType !== 'approval' && (!limit || limit < 0.01)) {
    ElMessage.warning('当前节点暂无可下拨余额，请先完成上级资金登记或下拨')
    return
  }
  editingId.value = null
  editingOriginalAmount.value = 0
  formRow.value = row
  formData.fundType = ''
  formData.fundSource = ''
  formData.amount = undefined
  formData.operator = ''
  formData.operateTime = ''
  formData.remark = ''
  formAttachments.value = []
  uploadingIndex.value = null
  formLimit.value = limit
  formVisible.value = true
}

/** 编辑资金流水：回填表单，金额上限 = 上级可用额度 + 原金额（允许在原金额基础上增加） */
const editRecord = (record: FundRecordVO) => {
  // 找到所属的节点 row：records 是 openDetail 时按 nodeType/nodeId 查询出来挂在 viewRow.records 上的
  const nodeRow = viewRow.value as DisplayRow | null
  if (!nodeRow) {
    ElMessage.warning('节点信息丢失，请重新查看')
    return
  }
  // 编辑态金额上限计算：一级无限制；二级 = 一级结余 + 原金额；三级 = 所属申报结余 + 原金额
  let limit: number | undefined
  const originalAmount = Number(record.amount || 0)
  if (nodeRow.nodeType === 'approval') {
    limit = nodeRow.budget
  } else if (nodeRow.nodeType === 'declare') {
    const approvalRow = findRow(`a_${nodeRow.approvalId}`)
    limit = Number(approvalRow?.balance ?? 0) + originalAmount
  } else {
    const declareRow = findRow(`d_${nodeRow.declareId}`)
    const allocated = Number(declareRow?.allocatedAmount ?? 0)
    const detailAllocated = (declareRow?.children || []).reduce(
      (sum, child) => sum + Number(child.allocatedAmount || 0),
      0
    )
    limit = Math.max(allocated - detailAllocated + originalAmount, originalAmount)
  }
  editingId.value = String(record.id)
  editingOriginalAmount.value = originalAmount
  formRow.value = nodeRow
  formData.fundType = (record.fundType as string | undefined) || ''
  formData.fundSource = (record.fundSource as string | undefined) || ''
  formData.amount = originalAmount
  formData.operator = record.createBy || ''
  formData.operateTime = record.createTime || ''
  formData.remark = record.remark || ''
  // 回填附件
  formAttachments.value = (record.attachments || []).map((att) => ({
    id: att.id,
    attachmentType: att.attachmentType,
    fileName: att.fileName,
    fileKey: att.fileKey,
    bucket: att.bucket || 'archive-formal',
    fileSize: att.fileSize,
    remark: att.remark
  }))
  uploadingIndex.value = null
  formLimit.value = limit
  formVisible.value = true
  // 校验上限至少为原金额，避免 InputNumber max < min(0.01)
  if (nodeRow.nodeType !== 'approval' && limit !== undefined && limit < originalAmount) {
    formLimit.value = originalAmount
  }
}

const handleSubmit = async () => {
  if (!formRef.value || !formRow.value) return
  await formRef.value.validate()
  const row = formRow.value
  const attachments = formAttachments.value
    .filter((item) => item.fileKey)
    .map((item) => ({
      attachmentType: item.attachmentType || undefined,
      fileName: item.fileName,
      fileKey: item.fileKey,
      bucket: item.bucket || 'archive-formal',
      fileSize: item.fileSize,
      remark: item.remark || undefined
    }))
  submitLoading.value = true
  try {
    if (editingId.value) {
      // 编辑资金流水
      const payload: FundUpdateDTO = {
        id: editingId.value,
        fundType: formData.fundType || null,
        fundSource: formData.fundSource || null,
        amount: Number(formData.amount),
        operator: formData.operator.trim() || null,
        operateTime: formData.operateTime || null,
        remark: formData.remark || null,
        attachments // 前端始终提交当前全部附件（全量替换），避免"不修改附件"时传 null 给后端
      }
      await updateFundRecord(payload)
      ElMessage.success('资金流水编辑成功')
      formVisible.value = false
      editingId.value = null
      editingOriginalAmount.value = 0
      loadData()
      // 编辑态通常还在"查看弹窗"中，刷新一下 records
      await loadRecords()
    } else {
      // 新增资金登记/下拨
      const payload: FundSaveDTO = {
        nodeType: row.nodeType,
        approvalId: row.approvalId,
        declareId: row.declareId,
        detailId: row.detailId,
        fundType: formData.fundType || undefined,
        fundSource: formData.fundSource || undefined,
        amount: Number(formData.amount),
        operator: formData.operator.trim() || undefined,
        operateTime: formData.operateTime || undefined,
        remark: formData.remark || undefined,
        attachments
      }
      if (row.nodeType === 'approval') {
        await registerFund(payload)
      } else {
        await allocateFund(payload)
      }
      ElMessage.success(row.nodeType === 'approval' ? '资金登记成功' : '资金下拨成功')
      formVisible.value = false
      loadData()
    }
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
.fund-page {
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

.fund-table {
  margin-top: 4px;
}

/* 表头首列与层级文字起点对齐（单元格 12px + 展开按钮 20px） */
.fund-table :deep(.el-table__header th:first-child .cell) {
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
  color: var(--el-color-primary, #22619e);
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
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 三级明细在二级基础上向右错位展示 */
.tree-code--detail {
  padding-left: 6px;
}

/* 项目编号行容器：图标 + 编号强制同一行，禁止换行 */
.approval-code-line {
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  white-space: nowrap;
  width: 100%;
  min-width: 0;
}
.approval-code-line .tree-toggle {
  flex-shrink: 0;
  margin-right: 8px;
}
.approval-code-line .detail-type-icon {
  flex-shrink: 0;
  margin-right: 6px;
}
.approval-code-line .tree-code {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 申报编号行容器：图标 + 编号强制同一行，不与 Element Plus 内置树形展开箭头冲突 */
.detail-code-line {
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  white-space: nowrap;
  /* 不强制撑满，让 Element Plus 内置树形展开箭头 + 缩进 与本容器共存于同一行 */
  width: auto;
  max-width: 100%;
}
/* 查看弹窗 declare-tree-table：Element Plus 自动处理层级缩进和展开箭头 */
.declare-tree-table .detail-code-line {
  flex: 1 1 auto;
  min-width: 0;
}
.detail-code-line .detail-type-icon {
  flex-shrink: 0;
  margin-right: 6px;
}
.detail-code-line .tree-code {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 三级明细行文字字号略小于一级、二级 */
.fund-table :deep(.fund-detail-row) {
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
  background: #eef4fb;
  color: #22619e;
}

.detail-type-icon--facility {
  background: #fdf6ec;
  color: #e6a23c;
}

/* 二级申报节点：纯蓝色文件夹小图标，不使用圆形背景 */
.detail-type-icon--folder {
  width: 16px;
  height: 16px;
  border-radius: 0;
  background: transparent;
  color: var(--el-color-primary, #22619e);
  font-size: 14px;
}

.balance-negative {
  color: var(--el-color-danger, #f56c6c);
}

/* 施工页签状态标签：与施工管理查看保持一致 */
.status-tag {
  display: inline-block;
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
  color: #22619e;
  background: #eef4fb;
}

.status-tag--COMPLETED {
  color: #67c23a;
  background: #f0f9eb;
}

.status-tag--FAIL {
  color: #e6a23c;
  background: #fdf6ec;
}

.status-tag--SUSPENDED {
  color: #909399;
  background: #f4f4f5;
}

/* 验收页签：蓝色填充进度条 + 右侧 N/M 分数，与验收管理保持一致 */
.simple-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.simple-progress__bar {
  flex: 1;
  height: 8px;
  min-width: 40px;
  background-color: #e9ebf0;
  border-radius: 4px;
  overflow: hidden;
}

.simple-progress__fill {
  height: 100%;
  background-color: var(--el-color-primary, #22619e);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.simple-progress__score {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  font-variant-numeric: tabular-nums;
}

.simple-progress--inline {
  gap: 6px;
}

.simple-progress--inline .simple-progress__bar {
  height: 6px;
}

/* 二级施工页签：施工进度分子/分母 */
.status-fraction {
  margin-left: 8px;
  color: #606266;
  font-weight: 600;
}

.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}

.fund-target {
  margin-bottom: 16px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.fund-target__name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.fund-target__meta {
  display: flex;
  gap: 16px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.fund-target__limit {
  color: #e6a23c;
}

.fund-form {
  margin-top: 4px;
}

/* 事项申报页签：树形表格层级缩进与立项管理查看保持一致 */
.declare-tree-table {
  margin-top: 4px;
}

/* 事项申报页签二级明细：字号略小，图标与编号之间留白 */
.declare-tree-table :deep(.declare-detail-row) {
  font-size: 12px;
}

.declare-tree-table .detail-type-icon {
  margin-right: 4px;
}

.declare-tree-table :deep(.el-table__row--level-1) .cell:first-child {
  padding-left: 20px;
}

/* 资金流水 / 申报附件：文件名可点击预览，右侧图标下载 */
.record-attachment {
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 20px;
}

.attachment-file-name {
  overflow: hidden;
  color: var(--el-color-primary, #22619e);
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.attachment-file-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.attachment-file-download {
  flex-shrink: 0;
  color: var(--el-color-primary, #22619e);
  cursor: pointer;
}

.attachment-file-name:hover {
  text-decoration: underline;
}

.upload-file-download {
  flex-shrink: 0;
  color: var(--el-color-primary, #22619e);
  cursor: pointer;
}

.declare-no-attachment-text {
  color: #c0c4cc;
}

/* 无附件提示 */
.attachment-empty {
  margin-bottom: 12px;
  padding: 18px 0;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  background: #fafafa;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

/* 附件区标题栏：标题 + 添加附件，右侧延伸分隔线 */
.attachment-section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 18px 0 12px;
}

.attachment-section-title {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.attachment-section-header::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e4e7ed;
}

/* 资金凭证附件卡片 */
.attachment-card {
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.attachment-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.attachment-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--el-color-primary, #22619e);
  color: #ffffff;
  font-size: 11px;
}

.attachment-card-title {
  flex: 1;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.attachment-card-body {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 12px;
}

.attachment-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.attachment-field-label {
  color: #606266;
  font-size: 12px;
}

/* 上传控件撑满所在列，虚线框与下拉框等宽 */
.attachment-field :deep(.el-upload) {
  display: block;
  width: 100%;
}

.upload-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 32px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #606266;
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}

.upload-trigger:hover {
  border-color: var(--el-color-primary, #22619e);
  color: var(--el-color-primary, #22619e);
}

.upload-file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #ffffff;
  font-size: 12px;
}

.upload-file-icon {
  flex-shrink: 0;
  color: #909399;
}

.upload-file-name {
  flex: 1;
  overflow: hidden;
  color: #606266;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-file-remove {
  flex-shrink: 0;
  color: #909399;
  cursor: pointer;
}

.upload-file-remove:hover {
  color: var(--el-color-danger, #f56c6c);
}
</style>

<style>
/* 资金登记 / 下拨资金弹窗：隐藏 el-row 栅格负边距在底部产生的横向滚动条 */
.fund-form-dialog .el-dialog__body {
  overflow-x: hidden !important;
}
</style>
