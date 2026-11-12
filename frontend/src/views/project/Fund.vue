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
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6366f1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 14 1.45-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.55 6a2 2 0 0 1-1.94 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2"/></svg>
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
          <template #default="{ row }">{{ formatMoney(row.budget) }}</template>
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
                type="primary"
                @click="openForm(row)"
              >资金登记</el-button>
              <el-button
                v-if="row.nodeType !== 'approval' && canAllocate"
                link
                type="primary"
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
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6366f1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 14 1.45-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.55 6a2 2 0 0 1-1.94 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2"/></svg>
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
              <el-table-column label="附件类型" width="150">
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
              <el-table-column prop="createTime" label="上传时间" width="170" />
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
                <el-table-column label="附件" min-width="200">
                  <template #default="{ row }">
                    <template v-if="row.detailLogSnapshot?.attachments?.length">
                      <div
                        v-for="file in row.detailLogSnapshot.attachments"
                        :key="file.id"
                        class="record-attachment"
                      >
                        <span class="attachment-file-name" :title="file.fileName" @click="openFile(file)">{{ file.fileName }}</span>
                        <el-icon class="upload-file-download" @click="openFile(file, true)"><Download /></el-icon>
                      </div>
                    </template>
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
                <el-table-column label="附件类型" width="180">
                  <template #default="{ row }">
                    {{ dictLabel('fu_jian_lei_xing', row.attachmentType) || row.attachmentType || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="文件名" min-width="240">
                  <template #default="{ row }">
                    <div class="record-attachment">
                      <span class="attachment-file-name" :title="row.fileName" @click="openFile(row)">{{ row.fileName || '-' }}</span>
                      <el-icon class="upload-file-download" @click="openFile(row, true)"><Download /></el-icon>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="大小" width="110" align="right">
                  <template #default="{ row }">{{ row.fileSize ? formatFileSize(row.fileSize) : '-' }}</template>
                </el-table-column>
                <el-table-column label="备注" min-width="160">
                  <template #default="{ row }">{{ row.remark || '-' }}</template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <!-- 二级：事项申报资金 / 立项 / 施工 / 申报明细 四个页签 -->
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
              <!-- 申报材料附件：以字段+附件按钮展示，点击弹窗查看附件列表 -->
              <el-descriptions-item label="附件" :span="2">
                <el-button
                  v-if="viewRow.raw.attachments?.length"
                  link
                  type="primary"
                  @click="openDeclareAttachList"
                >附件 ({{ viewRow.raw.attachments.length }})</el-button>
                <span v-else>-</span>
              </el-descriptions-item>
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
              <el-table-column label="申报材料附件" min-width="190">
                <template #default="{ row }">
                  <template v-if="row.materialAttachments?.length">
                    <div v-for="file in row.materialAttachments" :key="file.id" class="record-attachment">
                      <span class="attachment-file-name" :title="file.fileName" @click="openFile(file)">{{ file.fileName }}</span>
                      <el-icon class="upload-file-download" @click="openFile(file, true)"><Download /></el-icon>
                    </div>
                  </template>
                  <span v-else class="declare-no-attachment-text">-</span>
                </template>
              </el-table-column>
              <el-table-column label="资金凭证附件" min-width="190">
                <template #default="{ row }">
                  <template v-if="row.fundAttachments?.length">
                    <div v-for="file in row.fundAttachments" :key="file.id" class="record-attachment">
                      <span class="attachment-file-name" :title="file.fileName" @click="openFile(file)">{{ file.fileName }}</span>
                      <el-icon class="upload-file-download" @click="openFile(file, true)"><Download /></el-icon>
                    </div>
                  </template>
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
              v-if="!viewRow.raw.attachments?.length"
              description="暂无申报材料附件"
              :image-size="60"
            />
            <el-table v-else :data="viewRow.raw.attachments" size="small" border>
              <el-table-column label="附件类型" width="150">
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
              <el-table-column prop="createTime" label="上传时间" width="170" />
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
                <el-table-column label="附件" min-width="200">
                  <template #default="{ row }">
                    <template v-if="row.detailLogSnapshot?.attachments?.length">
                      <div
                        v-for="file in row.detailLogSnapshot.attachments"
                        :key="file.id"
                        class="record-attachment"
                      >
                        <span class="attachment-file-name" :title="file.fileName" @click="openFile(file)">{{ file.fileName }}</span>
                        <el-icon class="upload-file-download" @click="openFile(file, true)"><Download /></el-icon>
                      </div>
                    </template>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

        </el-tabs>
      </template>

      <!-- 三级：明细资金 / 立项 / 施工 三个页签 -->
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
                <!-- 申报材料附件：以字段+附件按钮展示，点击弹窗查看附件列表 -->
                <el-descriptions-item label="附件" :span="2">
                  <el-button
                    v-if="viewRow.raw.materialAttachments?.length"
                    link
                    type="primary"
                    @click="openDetailAttachList"
                  >附件 ({{ viewRow.raw.materialAttachments.length }})</el-button>
                  <span v-else>-</span>
                </el-descriptions-item>
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
                <el-table-column label="附件类型" width="180">
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
        <el-table-column label="附件类型" width="150">
          <template #default="{ row }">
            {{ dictLabel(attachDictType, row.attachmentType) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="附件名称" min-width="240">
          <template #default="{ row }">
            <div class="record-attachment">
              <span class="attachment-file-name" :title="row.fileName" @click="openFile(row)">{{ row.fileName }}</span>
              <el-icon class="upload-file-download" @click="openFile(row, true)"><Download /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="上传时间" width="170" />
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
  type FundNodeVO,
  type FundNodeType,
  type FundRecordVO,
  type FundSaveDTO,
  type FundAttachmentVO,
  type DeclareAttachmentVO
} from '@/api/fund'
import {
  getConstructionDetail,
  getDetailLog,
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
  'shi_gong_fu_jian_lei_xing'
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

/** 已收起的节点集合（默认全部展开） */
const collapsedKeys = ref<Set<string>>(new Set())

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
 * 一级：fund 项目资金 / approval 立项 / declare 事项申报 / construction 施工
 * 二级：declareFund 事项申报资金 / approval 立项 / construction 施工 / declareDetail 申报明细
 * 三级：fund 明细资金 / approval 立项 / construction 施工
 */
const viewTab = ref<'fund' | 'approval' | 'declare' | 'construction'>('fund')
const viewTabDeclare = ref<'declareFund' | 'approval' | 'construction' | 'declareDetail'>('declareFund')
const viewTabDetail = ref<'fund' | 'approval' | 'construction'>('fund')

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

/** 文件大小格式化 */
const formatFileSize = (bytes?: number): string => {
  if (!bytes || bytes <= 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let idx = 0
  let size = bytes
  while (size >= 1024 && idx < units.length - 1) {
    size /= 1024
    idx++
  }
  return `${size.toFixed(2)} ${units[idx]}`
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
      attachments: [],
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
  records.value = []
  viewVisible.value = true
  recordLoading.value = true
  try {
    const res = await getFundRecords(row.nodeType, row.nodeId)
    records.value = res.data || []
  } finally {
    recordLoading.value = false
  }
}

/* ============ 附件列表弹窗（一级事项申报页签） ============ */
type AttachListItem = DeclareAttachmentVO | FundAttachmentVO
const attachListVisible = ref(false)
const attachListTitle = ref('')
/** 附件类型字典：一级/二级申报材料 li_xiang_fu_jian_lei_xing，三级明细材料 fu_jian_lei_xing，资金凭证 zi_jin_fu_jian_lei_xing */
const attachDictType = ref('li_xiang_fu_jian_lei_xing')
const attachListData = ref<AttachListItem[]>([])

const openAttachList = (row: DeclareTreeRow, type: 'material' | 'fund') => {
  const files = type === 'material' ? row.attachments : row.fundAttachments
  attachListData.value = files || []
  attachDictType.value = type === 'material' ? 'li_xiang_fu_jian_lei_xing' : 'zi_jin_fu_jian_lei_xing'
  attachListTitle.value = `${type === 'material' ? '申报材料附件' : '资金凭证附件'} — ${[row.code, row.title].filter(Boolean).join(' ')}`
  attachListVisible.value = true
}

/** 二级查看-事项申报资金页签：申报材料附件按钮，弹出附件列表 */
const openDeclareAttachList = () => {
  const row = viewRow.value
  if (!row) return
  attachListData.value = row.raw.attachments || []
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

/** 一级/二级资金流水：某条流水的资金凭证附件按钮，弹出附件列表 */
const openRecordAttachList = (record: FundRecordVO) => {
  attachListData.value = record.attachments || []
  attachDictType.value = 'zi_jin_fu_jian_lei_xing'
  attachListTitle.value = `资金凭证附件 — ${[record.createBy, record.createTime].filter(Boolean).join(' ')}`
  attachListVisible.value = true
}

/* ============ 资金登记 / 下拨弹窗 ============ */
const formVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const formRow = ref<DisplayRow | null>(null)
/** 可下拨上限：二级取一级结余，三级取所属申报的可下拨余额；一级不限制（展示项目预算供参考） */
const formLimit = ref<number | undefined>(undefined)

const formTitle = computed(() => (formRow.value?.nodeType === 'approval' ? '资金登记' : '下拨资金'))

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

const handleSubmit = async () => {
  if (!formRef.value || !formRow.value) return
  await formRef.value.validate()
  const row = formRow.value
  // 仅提交已上传文件的附件卡片
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
  submitLoading.value = true
  try {
    if (row.nodeType === 'approval') {
      await registerFund(payload)
    } else {
      await allocateFund(payload)
    }
    ElMessage.success(row.nodeType === 'approval' ? '资金登记成功' : '资金下拨成功')
    formVisible.value = false
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
  background: #e8f3ff;
  color: #409eff;
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
  color: var(--el-color-primary, #409eff);
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
  color: #409eff;
  background: #ecf5ff;
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
  color: var(--el-color-primary, #409eff);
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
  color: var(--el-color-primary, #409eff);
  cursor: pointer;
}

.attachment-file-name:hover {
  text-decoration: underline;
}

.upload-file-download {
  flex-shrink: 0;
  color: var(--el-color-primary, #409eff);
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
  background: var(--el-color-primary, #409eff);
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
  border-color: var(--el-color-primary, #409eff);
  color: var(--el-color-primary, #409eff);
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
