<template>
  <div class="acceptance-page">
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
        class="acceptance-table"
        :row-class-name="rowClassName"
      >
        <el-table-column label="项目编号" width="145">
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
              <!-- 二级申报节点：蓝色文件夹小标签 -->
              <el-icon
                v-if="row.nodeType === 'declare'"
                class="detail-type-icon detail-type-icon--folder"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6366f1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 14 1.45-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.55 6a2 2 0 0 1-1.94 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2"/></svg>
              </el-icon>
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
        <el-table-column prop="name" label="项目名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="category" label="项目类别" width="85" show-overflow-tooltip />
        <el-table-column prop="type" label="项目类型" width="85" show-overflow-tooltip />
        <el-table-column label="验收状态" width="92">
          <template #default="{ row }">
            <el-tag
              v-if="row.acceptStatus === 'ACCEPTED'"
              type="success"
              size="small"
            >已验收</el-tag>
            <el-tag
              v-else-if="row.acceptStatus === 'REJECTED'"
              type="danger"
              size="small"
            >未通过</el-tag>
            <el-tag v-else type="warning" size="small">待验收</el-tag>
          </template>
        </el-table-column>
        <!-- 施工状态：与施工管理状态列同源（状态标签 + 进度条 + 分子/分母） -->
        <el-table-column label="施工状态" width="155">
          <template #default="{ row }">
            <div class="status-cell">
              <span class="status-tag" :class="`status-tag--${row.buildStatus}`">
                {{ buildStatusLabel(row.buildStatus) }}
              </span>
              <el-progress
                class="status-progress"
                :percentage="row.buildPercent || 0"
                :stroke-width="6"
                :show-text="false"
                :color="buildProgressColor(row.buildStatus)"
              />
              <span class="status-fraction">{{ row.buildNumerator || 0 }}/{{ row.totalCount || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" @click="openView(row)">查看</el-button>
              <!-- 一级项目 / 二级申报：每级只维护自己的验收状态 -->
              <el-button
                v-if="row.nodeType !== 'detail' && row.acceptStatus !== 'ACCEPTED'"
                link
                type="primary"
                @click="openAcceptForm(row)"
              >验收</el-button>
              <!-- 一级项目节点：归档按钮始终显示（已归档的除外），不受验收进度影响 -->
              <el-button
                v-if="row.nodeType === 'project' && !row.raw.archived"
                link
                type="primary"
                @click="handleArchive(row)"
              >归档</el-button>
              <!-- 三级明细节点：验收按钮 -->
              <el-button
                v-if="row.nodeType === 'detail' && row.acceptStatus !== 'ACCEPTED'"
                link
                type="primary"
                @click="openAcceptForm(row)"
              >验收</el-button>
              <!-- 已验收状态的三级节点不再显示验收按钮 -->
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

    <!-- 查看弹窗：三级分别展示不同内容 -->
    <el-dialog
      v-model="viewVisible"
      :title="viewTitle"
      :width="'1000px'"
      :height="'100vh'"
      top="0"
      class="accept-view-dialog"
      destroy-on-close
    >
      <!-- 一级：项目基础信息 + 验收情况 -->
      <template v-if="viewRow?.nodeType === 'project'">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="项目编号">{{ viewRow.code || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目名称">{{ viewRow.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目类别">{{ viewRow.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ viewRow.type || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ viewRow.raw.submitTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="验收进度">
            <div class="simple-progress simple-progress--inline">
              <div class="simple-progress__bar">
                <div
                  class="simple-progress__fill"
                  :style="{ width: (viewRow!.percent || 0) + '%' }"
                />
              </div>
              <span class="simple-progress__score">
                {{ (viewRow!).raw?.acceptedCount || 0 }}/{{ (viewRow!).raw?.totalCount || 0 }}
              </span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="项目预算">{{ formatMoney(viewRow.budget) }} 元</el-descriptions-item>
          <el-descriptions-item label="归档状态">
            <el-tag v-if="viewRow.raw.archived" type="success" size="small">已归档</el-tag>
            <el-tag v-else type="info" size="small">未归档</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">验收记录</el-divider>
        <el-empty
          v-if="!viewRow?.raw.acceptanceRecords?.length"
          description="暂无验收记录"
          :image-size="60"
        />
        <el-table v-else :data="viewRow?.raw.acceptanceRecords || []" size="small" border>
          <el-table-column label="验收结果" width="140">
            <template #default="{ row }">
              {{ dictLabel('yan_shou_jie_guo', row.acceptResult) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="acceptTime" label="验收时间" width="170" />
          <el-table-column prop="acceptBy" label="验收人" width="120" />
          <el-table-column prop="remark" label="验收备注" min-width="200" show-overflow-tooltip />
          <el-table-column label="附件" width="110">
            <template #default="{ row }">
              <el-button
                v-if="row.attachments?.length"
                link
                type="primary"
                @click="openRecordAttachments(row)"
              >附件 ({{ row.attachments.length }})</el-button>
              <span v-else class="declare-no-attachment-text">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRecordEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-divider content-position="left">验收情况（事项申报 + 明细）</el-divider>

        <!-- 事项申报 + 明细，可展开树形表格 -->
        <el-table
          :data="viewAcceptRows"
          row-key="vtKey"
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
          default-expand-all
          border
          size="small"
          class="accept-tree-table"
        >
          <el-table-column label="申报编号" width="180">
            <template #default="{ row }">
              <span class="accept-tree-cell">
                <el-icon
                  v-if="row.vtKind === 'declare'"
                  class="accept-tree-kind accept-tree-kind--declare"
                ><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6366f1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 14 1.45-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.55 6a2 2 0 0 1-1.94 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2"/></svg></el-icon>
                <el-icon
                  v-else-if="row.detailType === 'FACILITY'"
                  class="detail-type-icon detail-type-icon--facility"
                ><OfficeBuilding /></el-icon>
                <el-icon
                  v-else
                  class="detail-type-icon detail-type-icon--household"
                ><UserFilled /></el-icon>
                <el-tooltip
                  :content="row.code || '-'"
                  placement="top"
                  :show-after="200"
                >
                  <span class="accept-tree-cell__text">{{ row.code || '-' }}</span>
                </el-tooltip>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="申报标题" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.name || '-' }}</template>
          </el-table-column>
          <el-table-column label="申报类型" width="130">
            <template #default="{ row }">
              <template v-if="row.vtKind === 'declare'">{{ dictLabel('shen_bao_lei_xing', row.source) }}</template>
              <template v-else>-</template>
            </template>
          </el-table-column>
          <el-table-column label="申报类别" width="130">
            <template #default="{ row }">
              <template v-if="row.vtKind === 'declare'">{{ dictLabel('shen_bao_lei_bie', row.category) }}</template>
              <template v-else>{{ row.source || '-' }}</template>
            </template>
          </el-table-column>
          <el-table-column label="验收进度" width="180">
            <template #default="{ row }">
              <template v-if="row.vtKind === 'declare'">
                <div class="simple-progress">
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
              </template>
              <template v-else>
                <el-tag
                  v-if="row.acceptStatus === 'ACCEPTED'"
                  type="success"
                  size="small"
                >已验收</el-tag>
                <el-tag
                  v-else-if="row.acceptStatus === 'REJECTED'"
                  type="danger"
                  size="small"
                >未通过</el-tag>
                <el-tag v-else type="warning" size="small">待验收</el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="验收资料附件" min-width="200">
            <template #default="{ row }">
              <template v-if="row.vtKind === 'detail'">
                <template v-if="row.acceptanceAttachments?.length">
                  <div v-for="file in row.acceptanceAttachments" :key="file.id" class="record-attachment">
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

      <!-- 二级：事项申报信息 + 申报明细验收情况 -->
      <template v-else-if="viewRow?.nodeType === 'declare'">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申报编号">{{ viewRow.code || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申报标题">{{ viewRow.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申报类型">{{ viewRow.type || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申报类别">{{ viewRow.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="验收进度">
            <div class="simple-progress simple-progress--inline">
              <div class="simple-progress__bar">
                <div
                  class="simple-progress__fill"
                  :style="{ width: (viewRow!.percent || 0) + '%' }"
                />
              </div>
              <span class="simple-progress__score">
                {{ (viewRow!).acceptedCount || 0 }}/{{ (viewRow!).totalCount || 0 }}
              </span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="申报内容" :span="1">{{ viewRow.raw.declareContent || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">验收记录</el-divider>
        <el-empty
          v-if="!viewRow?.raw.acceptanceRecords?.length"
          description="暂无验收记录"
          :image-size="60"
        />
        <el-table v-else :data="viewRow?.raw.acceptanceRecords || []" size="small" border>
          <el-table-column label="验收结果" width="140">
            <template #default="{ row }">
              {{ dictLabel('yan_shou_jie_guo', row.acceptResult) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="acceptTime" label="验收时间" width="170" />
          <el-table-column prop="acceptBy" label="验收人" width="120" />
          <el-table-column prop="remark" label="验收备注" min-width="200" show-overflow-tooltip />
          <el-table-column label="附件" width="110">
            <template #default="{ row }">
              <el-button
                v-if="row.attachments?.length"
                link
                type="primary"
                @click="openRecordAttachments(row)"
              >附件 ({{ row.attachments.length }})</el-button>
              <span v-else class="declare-no-attachment-text">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRecordEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-divider content-position="left">申报明细验收情况</el-divider>
        <el-table :data="viewRow.raw.children || []" border size="small">
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
              <el-tag
                v-if="row.acceptStatus === 'ACCEPTED'"
                type="success"
                size="small"
              >已验收</el-tag>
              <el-tag
                v-else-if="row.acceptStatus === 'REJECTED'"
                type="danger"
                size="small"
              >未通过</el-tag>
              <el-tag v-else type="warning" size="small">待验收</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="验收资料附件" min-width="160">
            <template #default="{ row }">
              <template v-if="row.acceptanceAttachments?.length">
                <div v-for="file in row.acceptanceAttachments" :key="file.id" class="record-attachment">
                  <span class="attachment-file-name" :title="file.fileName" @click="openFile(file)">{{ file.fileName }}</span>
                  <el-icon class="upload-file-download" @click="openFile(file, true)"><Download /></el-icon>
                </div>
              </template>
              <span v-else class="declare-no-attachment-text">-</span>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 三级：申报明细验收情况 -->
      <template v-else>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="农户/设施名称">
            <el-icon
              v-if="viewRow?.detailType === 'FACILITY'"
              class="detail-type-icon detail-type-icon--facility"
            ><OfficeBuilding /></el-icon>
            <el-icon v-else class="detail-type-icon detail-type-icon--household"><UserFilled /></el-icon>
            <span style="margin-left: 4px">{{ viewRow?.name || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="户号/编码">{{ viewRow?.code || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属区划">{{ viewRow?.source || '-' }}</el-descriptions-item>
          <el-descriptions-item label="事项说明">{{ viewRow?.raw.detailRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="验收状态">
            <el-tag
              v-if="viewRow?.acceptStatus === 'ACCEPTED'"
              type="success"
              size="small"
            >已验收</el-tag>
            <el-tag
              v-else-if="viewRow?.acceptStatus === 'REJECTED'"
              type="danger"
              size="small"
            >未通过</el-tag>
            <el-tag v-else type="warning" size="small">待验收</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="验收结果">
            {{ dictLabel('yan_shou_jie_guo', viewRow?.raw.acceptResult) || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="验收时间">{{ viewRow?.raw.acceptTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="验收人">{{ viewRow?.raw.acceptBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="验收备注" :span="2">{{ viewRow?.raw.acceptRemark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">验收记录</el-divider>
        <el-empty
          v-if="!viewRow?.raw.acceptanceRecords?.length"
          description="暂无验收记录"
          :image-size="60"
        />
        <el-table v-else :data="viewRow?.raw.acceptanceRecords || []" size="small" border>
          <el-table-column label="验收结果" width="140">
            <template #default="{ row }">
              {{ dictLabel('yan_shou_jie_guo', row.acceptResult) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="acceptTime" label="验收时间" width="170" />
          <el-table-column prop="acceptBy" label="验收人" width="120" />
          <el-table-column prop="remark" label="验收备注" min-width="200" show-overflow-tooltip />
          <el-table-column label="附件" width="110">
            <template #default="{ row }">
              <el-button
                v-if="row.attachments?.length"
                link
                type="primary"
                @click="openRecordAttachments(row)"
              >附件 ({{ row.attachments.length }})</el-button>
              <span v-else class="declare-no-attachment-text">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRecordEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 验收记录附件列表弹窗 -->
    <el-dialog
      v-model="recordAttachVisible"
      :title="recordAttachTitle"
      width="760px"
      destroy-on-close
    >
      <el-table :data="recordAttachList" size="small" border>
        <el-table-column label="附件类型" width="150">
          <template #default="{ row }">
            {{ dictLabel('yan_shou_fu_jian_lei_xing', row.attachmentType) || '-' }}
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
      <template #footer>
        <el-button @click="recordAttachVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑验收记录弹窗 -->
    <el-dialog v-model="recordEditVisible" title="编辑验收记录" width="760px" destroy-on-close>
      <el-form ref="recordEditRef" :model="recordEditForm" :rules="recordEditRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="验收结果" prop="acceptResult">
              <el-select v-model="recordEditForm.acceptResult" placeholder="请选择验收结果" style="width: 100%">
                <el-option
                  v-for="item in dictOptions('yan_shou_jie_guo')"
                  :key="item.dictValue"
                  :label="item.dictLabel"
                  :value="item.dictValue"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="验收时间" prop="acceptTime">
              <el-date-picker
                v-model="recordEditForm.acceptTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择验收时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="验收人" prop="acceptBy">
          <el-input v-model="recordEditForm.acceptBy" placeholder="请输入验收人" maxlength="50" />
        </el-form-item>
        <el-form-item label="验收备注">
          <el-input
            v-model="recordEditForm.remark"
            type="textarea"
            placeholder="请输入验收备注（可选）"
            maxlength="500"
            show-word-limit
            rows="2"
          />
        </el-form-item>

        <!-- 验收资料附件：卡片式多条维护，右侧删除，保存后整体替换 -->
        <div class="attachment-section-header">
          <span class="attachment-section-title">验收资料附件</span>
          <el-button link type="primary" icon="Plus" @click="addRecordEditAttachment">添加附件</el-button>
        </div>
        <div v-if="!recordEditAttachments.length" class="attachment-empty">
          暂无附件，点击上方“添加附件”上传验收资料
        </div>
        <div v-for="(item, index) in recordEditAttachments" :key="index" class="attachment-card">
          <div class="attachment-card-header">
            <span class="attachment-index">{{ index + 1 }}</span>
            <span class="attachment-card-title">附件 #{{ index + 1 }}</span>
            <el-button link type="danger" icon="Delete" @click="removeRecordEditAttachment(index)">删除</el-button>
          </div>
          <div class="attachment-card-body">
            <div class="attachment-field">
              <span class="attachment-field-label">附件类型</span>
              <el-select v-model="item.attachmentType" placeholder="请选择附件类型" clearable style="width: 100%">
                <el-option
                  v-for="opt in dictOptions('yan_shou_fu_jian_lei_xing')"
                  :key="opt.dictValue"
                  :label="opt.dictLabel"
                  :value="opt.dictValue"
                />
              </el-select>
            </div>
            <div class="attachment-field">
              <span class="attachment-field-label">上传附件</span>
              <el-upload :show-file-list="false" multiple :before-upload="uploadRecordEditBefore(index)">
                <div class="upload-trigger">
                  <el-icon><Upload /></el-icon>
                  <span>{{ item.uploading ? '上传中...' : '点击选择文件...' }}</span>
                </div>
              </el-upload>
              <!-- 同一附件类型下可挂多个文件，可单独下载/删除 -->
              <div
                v-for="(f, fi) in item.files"
                :key="f.fileKey + fi"
                class="upload-file-item"
              >
                <el-icon class="upload-file-icon"><Document /></el-icon>
                <span class="upload-file-name" :title="f.fileName">{{ f.fileName }}</span>
                <el-icon class="upload-file-download" title="下载" @click="openFile(f, true)">
                  <Download />
                </el-icon>
                <el-icon class="upload-file-download" title="删除" @click="removeRecordEditAttachmentFile(index, fi)">
                  <Delete />
                </el-icon>
              </div>
            </div>
            <div class="attachment-field">
              <span class="attachment-field-label">备注</span>
              <el-input v-model="item.remark" placeholder="请输入备注（可选）" maxlength="200" />
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="recordEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="recordEditLoading" @click="handleRecordEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 验收弹窗：一级项目 / 二级申报 / 三级明细，布局一致 -->
    <el-dialog
      v-model="acceptVisible"
      :title="acceptTitle"
      width="760px"
      destroy-on-close
    >
      <el-form ref="acceptFormRef" :model="acceptForm" :rules="acceptRules" label-width="90px">
        <!-- 一级：项目基础信息 -->
        <el-descriptions
          v-if="acceptRow?.nodeType === 'project'"
          :column="2"
          border
          class="accept-target"
        >
          <el-descriptions-item label="项目编号">{{ acceptRow?.code || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目名称">{{ acceptRow?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目类别">{{ acceptRow?.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ acceptRow?.type || '-' }}</el-descriptions-item>
        </el-descriptions>
        <!-- 二级：申报基础信息 -->
        <el-descriptions
          v-else-if="acceptRow?.nodeType === 'declare'"
          :column="2"
          border
          class="accept-target"
        >
          <el-descriptions-item label="申报编号">{{ acceptRow?.code || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申报标题">{{ acceptRow?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申报类型">{{ acceptRow?.type || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申报类别">{{ acceptRow?.category || '-' }}</el-descriptions-item>
        </el-descriptions>
        <!-- 三级：明细基础信息 -->
        <el-descriptions v-else :column="2" border class="accept-target">
          <el-descriptions-item label="验收对象">
            <template v-if="acceptRow">
              <el-icon
                v-if="acceptRow.detailType === 'FACILITY'"
                class="detail-type-icon detail-type-icon--facility"
              ><OfficeBuilding /></el-icon>
              <el-icon v-else class="detail-type-icon detail-type-icon--household"><UserFilled /></el-icon>
              <span style="margin-left: 4px">{{ acceptRow.name }}</span>
            </template>
          </el-descriptions-item>
          <el-descriptions-item label="户号/编码">{{ acceptRow?.code }}</el-descriptions-item>
          <el-descriptions-item label="所属区划" :span="2">{{ acceptRow?.source }}</el-descriptions-item>
          <el-descriptions-item label="事项说明" :span="2">{{ acceptRow?.raw.detailRemark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="验收结果" prop="acceptResult">
              <el-select v-model="acceptForm.acceptResult" placeholder="请选择验收结果" style="width: 100%">
                <el-option
                  v-for="item in dictOptions('yan_shou_jie_guo')"
                  :key="item.dictValue"
                  :label="item.dictLabel"
                  :value="item.dictValue"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="验收时间" prop="acceptTime">
              <el-date-picker
                v-model="acceptForm.acceptTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择验收时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="验收人" prop="acceptBy">
          <el-input v-model="acceptForm.acceptBy" placeholder="请输入验收人" maxlength="50" />
        </el-form-item>
        <el-form-item label="验收备注">
          <el-input
            v-model="acceptForm.remark"
            type="textarea"
            placeholder="请输入验收备注（可选）"
            maxlength="500"
            show-word-limit
            rows="2"
          />
        </el-form-item>

        <!-- 验收资料附件：卡片式多条维护，右侧删除 -->
        <div class="attachment-section-header">
          <span class="attachment-section-title">验收资料附件</span>
          <el-button link type="primary" icon="Plus" @click="addAcceptAttachment">添加附件</el-button>
        </div>
        <div v-if="!acceptAttachments.length" class="attachment-empty">
          暂无附件，点击上方“添加附件”上传验收资料
        </div>
        <div v-for="(item, index) in acceptAttachments" :key="index" class="attachment-card">
          <div class="attachment-card-header">
            <span class="attachment-index">{{ index + 1 }}</span>
            <span class="attachment-card-title">附件 #{{ index + 1 }}</span>
            <el-button link type="danger" icon="Delete" @click="removeAcceptAttachment(index)">删除</el-button>
          </div>
          <div class="attachment-card-body">
            <div class="attachment-field">
              <span class="attachment-field-label">附件类型</span>
              <el-select v-model="item.attachmentType" placeholder="请选择附件类型" clearable style="width: 100%">
                <el-option
                  v-for="opt in dictOptions('yan_shou_fu_jian_lei_xing')"
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
                multiple
                :before-upload="uploadAcceptBefore(index)"
              >
                <div class="upload-trigger">
                  <el-icon><Upload /></el-icon>
                  <span>{{ item.uploading ? '上传中...' : '点击选择文件...' }}</span>
                </div>
              </el-upload>
              <!-- 同一附件类型下可挂多个文件，可单独下载/删除 -->
              <div
                v-for="(f, fi) in item.files"
                :key="f.fileKey + fi"
                class="upload-file-item"
              >
                <el-icon class="upload-file-icon"><Document /></el-icon>
                <span class="upload-file-name" :title="f.fileName">{{ f.fileName }}</span>
                <el-icon class="upload-file-download" title="下载" @click="openFile(f, true)">
                  <Download />
                </el-icon>
                <el-icon class="upload-file-download" title="删除" @click="removeAcceptAttachmentFile(index, fi)">
                  <Delete />
                </el-icon>
              </div>
            </div>
            <div class="attachment-field">
              <span class="attachment-field-label">备注</span>
              <el-input v-model="item.remark" maxlength="256" placeholder="请输入备注" />
            </div>
          </div>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="acceptVisible = false">取消</el-button>
        <el-button type="primary" :loading="acceptLoading" @click="handleAcceptSubmit">确认验收</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getDictByType } from '@/api/dict'
import { uploadFile, getPresignUrl, type FileUploadVO } from '@/api/file'
import type { DictVO } from '@/types/system'
import {
  getAcceptancePage,
  acceptDetail,
  acceptNode,
  updateRecord,
  archive,
  type AcceptanceNodeVO,
  type AcceptanceNodeType,
  type AcceptanceAttachmentVO,
  type AcceptanceRecordVO
} from '@/api/acceptance'

/**
 * 验收管理
 * 所属菜单：项目全周期管控 → 验收管理
 * 数据来源：立项管理中审核通过且含申报明细的项目；三级树形：项目 → 申报内容 → 农户/公共基础设施明细
 * 验收进度 = 已验收明细数 / 全部明细数 × 100%
 */
const userStore = useUserStore()

const canAccept = computed(() => userStore.hasPerm('project:acceptance:accept'))
const canArchive = computed(() => userStore.hasPerm('project:acceptance:archive'))

/* ============ 字典 ============ */
const dictMap = ref<Record<string, DictVO[]>>({})
const DICT_TYPES = [
  'xiang_mu_lei_bie',
  'xiang_mu_lei_xing',
  'shen_bao_lei_bie',
  'shen_bao_lei_xing',
  'li_xiang_fu_jian_lei_xing',
  'yan_shou_fu_jian_lei_xing',
  'yan_shou_jie_guo',
  'fu_jian_lei_xing'
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
  nodeType: AcceptanceNodeType
  detailType?: string
  /** 节点ID */
  nodeId: string
  approvalId: string
  declareId?: string
  detailId?: string
  code: string
  name: string
  category: string
  type: string
  source?: string
  percent: number
  acceptedCount?: number
  totalCount?: number
  acceptStatus?: string
  /** 施工状态：NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工 */
  buildStatus?: string
  /** 施工进度分子 */
  buildNumerator?: number
  /** 施工进度百分比（0-100） */
  buildPercent?: number
  acceptTime?: string
  acceptBy?: string
  budget?: number
  raw: AcceptanceNodeVO
  children?: DisplayRow[]
  /** 是否含子节点，用于渲染展开箭头 */
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

/** 手写展开/收起，保证三个层级的缩进与资金管理、立项管理完全一致 */
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
  row.nodeType === 'detail' ? 'accept-detail-row' : ''

const toDisplay = (node: AcceptanceNodeVO, approvalId: string, declareId?: string): DisplayRow => {
  const rawNode = node as AcceptanceNodeVO
  const displayRow: DisplayRow = {
    rowKey: `${node.nodeType[0]}_${node.id}`,
    nodeType: node.nodeType,
    detailType: node.detailType,
    nodeId: node.id,
    approvalId,
    declareId: node.nodeType === 'declare' ? node.id : declareId,
    detailId: node.nodeType === 'detail' ? node.id : undefined,
    code: node.code || '',
    name: node.name || '',
    category: getCategoryLabel(node),
    type: getTypeLabel(node),
    source: node.source,
    percent: node.percent || 0,
    acceptedCount: node.acceptedCount,
    totalCount: node.totalCount,
    acceptStatus: node.acceptStatus,
    buildStatus: node.buildStatus,
    buildNumerator: node.buildNumerator,
    buildPercent: node.buildPercent,
    acceptTime: node.acceptTime,
    acceptBy: node.acceptBy,
    budget: node.budget,
    raw: rawNode,
    hasChildren: (node.children || []).length > 0,
    children: (node.children || []).map((child) =>
      toDisplay(child, approvalId, node.nodeType === 'declare' ? node.id : declareId)
    )
  }
  return displayRow
}

/** 按节点层级翻译类别字典 */
const getCategoryLabel = (node: AcceptanceNodeVO): string => {
  if (node.nodeType === 'project') return dictLabel('xiang_mu_lei_bie', node.category)
  if (node.nodeType === 'declare') return dictLabel('shen_bao_lei_bie', node.category)
  return detailTypeLabel(node.detailType)
}

/** 按节点层级翻译类型字典 */
const getTypeLabel = (node: AcceptanceNodeVO): string => {
  if (node.nodeType === 'project') return dictLabel('xiang_mu_lei_xing', node.source)
  if (node.nodeType === 'declare') return dictLabel('shen_bao_lei_xing', node.source)
  return node.source || ''
}


const loadData = async () => {
  loading.value = true
  try {
    const res = await getAcceptancePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      projectName: queryForm.projectName || undefined,
      projectCategory: queryForm.projectCategory || undefined,
      projectSource: queryForm.projectSource || undefined
    })
    tableData.value = (res.data.records || []).map((node) => toDisplay(node, node.id))
    // 默认全部收起：仅展示一级项目行，展开后再逐级查看
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
/** 施工状态文案，与施工管理保持一致 */
const buildStatusLabel = (status?: string): string => {
  if (status === 'COMPLETED') return '已完工'
  if (status === 'IN_PROGRESS') return '施工中'
  return '未开工'
}

/** 施工进度条颜色，与施工管理保持一致 */
const buildProgressColor = (status?: string): string => {
  if (status === 'COMPLETED') return '#67c23a'
  if (status === 'IN_PROGRESS') return '#409eff'
  return '#c0c4cc'
}

const formatMoney = (value?: number): string => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 打开附件：预览或下载（MinIO 时效签名URL，禁止永久直链） */
const openFile = async (file: { bucket?: string; fileKey: string; fileName?: string }, download = false) => {
  if (!file?.fileKey) return
  const res = await getPresignUrl({
    bucket: file.bucket || 'archive-formal',
    fileKey: file.fileKey,
    fileName: file.fileName,
    download
  })
  window.open(res.data, '_blank')
}

/* ============ 查看弹窗 ============ */
const viewVisible = ref(false)
const viewRow = ref<DisplayRow | null>(null)

const viewTitle = computed(() => {
  if (viewRow.value?.nodeType === 'project') return '项目验收详情'
  if (viewRow.value?.nodeType === 'declare') return '申报验收详情'
  return '明细验收详情'
})

/**
 * 一级查看弹窗"验收情况"区域的数据：
 * 把 declare children 转成可展开的树形行（vtKind='declare' 一级，vtKind='detail' 二级）
 */
const viewAcceptRows = computed(() => {
  const declares = viewRow.value?.raw?.children || []
  return declares.map((declare) => ({
    vtKey: `vt_d_${declare.id}`,
    vtKind: 'declare' as const,
    id: declare.id,
    code: declare.code,
    name: declare.name,
    category: declare.category,
    source: declare.source,
    percent: declare.percent,
    acceptedCount: declare.acceptedCount,
    totalCount: declare.totalCount,
    children: (declare.children || []).map((detail) => ({
      vtKey: `vt_t_${detail.id}`,
      vtKind: 'detail' as const,
      id: detail.id,
      detailType: detail.detailType,
      code: detail.code,
      name: detail.name,
      // 明细表头"申报类别"列展示所属区划
      source: detail.source,
      category: '',
      acceptStatus: detail.acceptStatus,
      acceptanceAttachments: detail.acceptanceAttachments,
      hasChildren: false
    })),
    hasChildren: (declare.children || []).length > 0
  }))
})

const openView = async (row: DisplayRow) => {
  viewRow.value = row
  viewVisible.value = true
}

/* ============ 验收记录附件列表 ============ */
const recordAttachVisible = ref(false)
const recordAttachList = ref<AcceptanceAttachmentVO[]>([])
const recordAttachTitle = ref('')

/** 打开某条验收记录的附件列表 */
const openRecordAttachments = (record: AcceptanceRecordVO) => {
  recordAttachList.value = record.attachments || []
  recordAttachTitle.value = `验收附件 — ${[record.acceptBy, record.acceptTime].filter(Boolean).join(' ')}`
  recordAttachVisible.value = true
}

/* ============ 编辑验收记录 ============ */
const recordEditVisible = ref(false)
const recordEditLoading = ref(false)
const recordEditId = ref('')
const recordEditRef = ref<FormInstance>()
const recordEditForm = reactive({
  acceptResult: '',
  acceptTime: '',
  acceptBy: '',
  remark: ''
})
const recordEditRules: FormRules = {
  acceptResult: [{ required: true, message: '请选择验收结果', trigger: 'change' }],
  acceptTime: [{ required: true, message: '请选择验收时间', trigger: 'change' }],
  acceptBy: [{ required: true, message: '请输入验收人', trigger: 'blur' }]
}
const recordEditAttachments = ref<AcceptFormAttachment[]>([])

/** 打开编辑验收记录弹窗，回填原记录数据与附件 */
const openRecordEdit = (record: AcceptanceRecordVO) => {
  if (!canAccept.value) {
    ElMessage.warning('无验收权限')
    return
  }
  recordEditId.value = record.id || ''
  recordEditForm.acceptResult = record.acceptResult || ''
  recordEditForm.acceptTime = record.acceptTime || nowDateTime()
  recordEditForm.acceptBy = record.acceptBy || ''
  recordEditForm.remark = record.remark || ''
  // 附件按 附件类型+备注 分组为卡片，每个卡片下挂多个文件
  const groups = new Map<string, AcceptFormAttachment>()
  for (const item of record.attachments || []) {
    const key = `${item.attachmentType || ''}|${item.remark || ''}`
    let card = groups.get(key)
    if (!card) {
      card = { attachmentType: item.attachmentType || '', remark: item.remark || '', files: [] }
      groups.set(key, card)
    }
    card.files.push({
      fileName: item.fileName || '',
      fileKey: item.fileKey,
      bucket: item.bucket || 'archive-formal',
      fileSize: item.fileSize
    })
  }
  recordEditAttachments.value = Array.from(groups.values())
  recordEditVisible.value = true
}

/** 添加一条验收资料附件（编辑弹窗） */
const addRecordEditAttachment = () => {
  recordEditAttachments.value.push({
    attachmentType: '',
    remark: '',
    files: []
  })
}

/** 删除一条验收资料附件（编辑弹窗，整卡删除） */
const removeRecordEditAttachment = (index: number) => {
  recordEditAttachments.value.splice(index, 1)
}

/** 删除编辑弹窗卡片内的单个附件文件 */
const removeRecordEditAttachmentFile = (cardIndex: number, fileIndex: number) => {
  recordEditAttachments.value[cardIndex]?.files.splice(fileIndex, 1)
}

/** 上传编辑弹窗的验收资料附件，返回 false 阻止组件自动上传；同一卡片可连续选择多个文件 */
const handleRecordEditUpload = async (file: File, index: number) => {
  const list = recordEditAttachments.value
  let card = list[index]
  if (!card) {
    card = { attachmentType: '', remark: '', files: [], uploading: true }
    list.push(card)
  } else {
    card.uploading = true
  }
  try {
    const res = await uploadFile(file)
    const data: FileUploadVO = res.data
    card.files.push({
      fileName: data.fileName,
      fileKey: data.fileKey,
      bucket: data.bucket,
      fileSize: data.fileSize
    })
  } finally {
    card.uploading = false
  }
  return false
}

/** 生成编辑弹窗附件行的上传前置回调 */
const uploadRecordEditBefore = (index: number) => (file: File) => handleRecordEditUpload(file, index)

/** 编辑保存后，在新数据中定位当前查看的节点并刷新查看弹窗 */
const refreshViewRow = () => {
  const target = viewRow.value
  if (!target) return
  let found: DisplayRow | undefined
  const walk = (list: DisplayRow[]) => {
    list.forEach((row) => {
      if (!found && row.nodeType === target.nodeType && row.nodeId === target.nodeId) {
        found = row
      }
      if (row.children?.length) walk(row.children)
    })
  }
  walk(tableData.value)
  if (found) viewRow.value = found
}

/** 提交编辑验收记录，附件整体替换，成功后刷新列表与查看弹窗 */
const handleRecordEditSubmit = async () => {
  if (!recordEditRef.value || !recordEditId.value) return
  try {
    await recordEditRef.value.validate()
  } catch {
    return
  }
  // 每个附件卡片下的多个文件展开为多条附件记录，共享该卡片的附件类型与备注
  const attachments = recordEditAttachments.value.flatMap((item) =>
    item.files.map((f) => ({
      attachmentType: item.attachmentType || undefined,
      fileName: f.fileName,
      fileKey: f.fileKey,
      bucket: f.bucket || 'archive-formal',
      fileSize: f.fileSize,
      remark: item.remark || undefined
    }))
  )
  recordEditLoading.value = true
  try {
    await updateRecord({
      id: recordEditId.value,
      acceptResult: recordEditForm.acceptResult,
      acceptTime: recordEditForm.acceptTime,
      acceptBy: recordEditForm.acceptBy.trim(),
      remark: recordEditForm.remark.trim() || undefined,
      attachments
    })
    ElMessage.success('验收记录已更新')
    recordEditVisible.value = false
    await loadData()
    refreshViewRow()
  } finally {
    recordEditLoading.value = false
  }
}

/* ============ 归档 ============ */
const handleArchive = async (row: DisplayRow) => {
  if (!canArchive.value) {
    ElMessage.warning('无归档权限')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将项目「${row.name}」归档？归档后项目将进入档案管理模块。`,
      '归档确认',
      { type: 'warning', confirmButtonText: '确认归档', cancelButtonText: '取消' }
    )
    await archive({
      approvalId: row.approvalId,
      archiveBy: userStore.userInfo?.username || '系统',
      archiveName: row.name,
      remark: '验收完成归档'
    })
    ElMessage.success('归档成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      // 错误已由 ElMessageBox 处理
    }
  }
}

/* ============ 三级明细验收 ============ */
const acceptVisible = ref(false)
const acceptLoading = ref(false)
const acceptFormRef = ref<FormInstance>()
const acceptRow = ref<DisplayRow | null>(null)
const acceptForm = reactive({
  acceptResult: '',
  acceptTime: '',
  acceptBy: '',
  remark: ''
})
const acceptRules = computed<FormRules>(() => ({
  acceptResult: [{ required: true, message: '请选择验收结果', trigger: 'change' }],
  acceptTime: [{ required: true, message: '请选择验收时间', trigger: 'change' }],
  acceptBy: [{ required: true, message: '请输入验收人', trigger: 'blur' }]
}))

/** 验收弹窗标题：一级项目 / 二级申报 / 三级明细 */
const acceptTitle = computed(() => {
  if (acceptRow.value?.nodeType === 'project') return '项目验收'
  if (acceptRow.value?.nodeType === 'declare') return '申报验收'
  return '明细验收'
})

/** 附件卡片中的单个文件 */
interface AcceptFormFile {
  fileName: string
  fileKey: string
  bucket?: string
  fileSize?: number
}

/** 验收资料附件表单项：一个附件类型（卡片）下可挂多个文件 */
interface AcceptFormAttachment {
  attachmentType?: string
  remark?: string
  files: AcceptFormFile[]
  /** 是否有文件正在上传 */
  uploading?: boolean
}
const acceptAttachments = ref<AcceptFormAttachment[]>([])

/** 当前时间（yyyy-MM-dd HH:mm:ss），验收时间默认值 */
const nowDateTime = (): string => {
  const d = new Date()
  const pad = (n: number): string => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 添加一条验收资料附件 */
const addAcceptAttachment = () => {
  acceptAttachments.value.push({
    attachmentType: '',
    remark: '',
    files: []
  })
}

/** 删除一条验收资料附件（整卡删除） */
const removeAcceptAttachment = (index: number) => {
  acceptAttachments.value.splice(index, 1)
}

/** 删除卡片内的单个附件文件 */
const removeAcceptAttachmentFile = (cardIndex: number, fileIndex: number) => {
  acceptAttachments.value[cardIndex]?.files.splice(fileIndex, 1)
}

/** 上传验收资料附件，返回 false 阻止组件自动上传；同一卡片可连续选择多个文件 */
const handleAcceptAttachmentUpload = async (file: File, index: number) => {
  const list = acceptAttachments.value
  let card = list[index]
  if (!card) {
    card = { attachmentType: '', remark: '', files: [], uploading: true }
    list.push(card)
  } else {
    card.uploading = true
  }
  try {
    const res = await uploadFile(file)
    const data: FileUploadVO = res.data
    card.files.push({
      fileName: data.fileName,
      fileKey: data.fileKey,
      bucket: data.bucket,
      fileSize: data.fileSize
    })
  } finally {
    card.uploading = false
  }
  return false
}

/** 生成指定附件卡片的上传前置回调 */
const uploadAcceptBefore = (index: number) => (file: File) => handleAcceptAttachmentUpload(file, index)

const openAcceptForm = (row: DisplayRow) => {
  if (!canAccept.value) {
    ElMessage.warning('无验收权限')
    return
  }
  // 业务约束：施工进度达到 100%（全部明细已完工）才允许验收
  if ((row.buildPercent ?? 0) !== 100) {
    ElMessage.warning('存在施工中或未开工的项目，不能验收')
    return
  }
  acceptRow.value = row
  acceptForm.acceptResult = ''
  acceptForm.acceptTime = nowDateTime()
  acceptForm.acceptBy = ''
  acceptForm.remark = ''
  acceptAttachments.value = []
  acceptVisible.value = true
}

const handleAcceptSubmit = async () => {
  if (!acceptFormRef.value || !acceptRow.value) return
  try {
    await acceptFormRef.value.validate()
  } catch {
    return
  }
  // 每个附件卡片下的多个文件展开为多条附件记录，共享该卡片的附件类型与备注
  const attachments = acceptAttachments.value.flatMap((item) =>
    item.files.map((f) => ({
      attachmentType: item.attachmentType || undefined,
      fileName: f.fileName,
      fileKey: f.fileKey,
      bucket: f.bucket || 'archive-formal',
      fileSize: f.fileSize,
      remark: item.remark || undefined
    }))
  )
  acceptLoading.value = true
  try {
    if (acceptRow.value.nodeType === 'detail') {
      await acceptDetail({
        approvalId: acceptRow.value.approvalId,
        detailId: acceptRow.value.detailId!,
        acceptBy: acceptForm.acceptBy.trim(),
        acceptResult: acceptForm.acceptResult,
        acceptTime: acceptForm.acceptTime,
        remark: acceptForm.remark.trim() || undefined,
        attachments
      })
    } else {
      // 一级项目 / 二级申报：批量验收该节点下全部明细
      await acceptNode({
        approvalId: acceptRow.value.approvalId,
        nodeType: acceptRow.value.nodeType,
        declareId: acceptRow.value.nodeType === 'declare' ? acceptRow.value.declareId : undefined,
        acceptBy: acceptForm.acceptBy.trim(),
        acceptResult: acceptForm.acceptResult,
        acceptTime: acceptForm.acceptTime,
        remark: acceptForm.remark.trim() || undefined,
        attachments
      })
    }
    ElMessage.success('验收成功')
    acceptVisible.value = false
    loadData()
  } finally {
    acceptLoading.value = false
  }
}

onMounted(async () => {
  await loadDicts()
  await loadData()
})
</script>

<style scoped>
.acceptance-page {
  padding: 0 16px 16px;
}

.query-card {
  margin-bottom: 12px;
}

/* 筛选项与查询/刷新按钮强制同一行 */
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

.query-form :deep(.query-item--actions) {
  flex: 0 0 auto;
  margin-right: 0;
}

.acceptance-table {
  margin-top: 4px;
}

/* 收紧单元格左右内边距，窄列下尽量完整展示标题与内容 */
.acceptance-table :deep(.el-table .cell) {
  padding-left: 8px;
  padding-right: 8px;
}

/* 操作按钮紧凑排列，避免窄列换行 */
.acceptance-table :deep(.action-buttons) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
}

.acceptance-table :deep(.action-buttons .el-button) {
  margin-left: 0;
}

.acceptance-table :deep(.el-table__header th:first-child .cell) {
  padding-left: 32px;
}

/* 冻结的操作列：补左侧分割线与悬浮阴影，横向滚动时与左侧列区分 */
.acceptance-table :deep(.el-table-fixed-column--right) {
  border-left: 1px solid #ebeef5;
  box-shadow: -6px 0 8px -8px rgba(0, 0, 0, 0.25);
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

.acceptance-table :deep(.accept-detail-row) {
  font-size: 12px;
}

/* 施工状态列：状态标签 + 进度条 + 分子/分母（与施工管理一致） */
.status-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-tag {
  flex: none;
  padding: 0 4px;
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
  min-width: 36px;
}

.status-fraction {
  flex: none;
  min-width: 28px;
  text-align: right;
  color: #606266;
  font-weight: 600;
}

/* 三级明细类型图标 */
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

.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}

/* 附件文件名样式 */
.record-attachment {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 2px;
}

.attachment-file-name {
  color: var(--el-color-primary, #409eff);
  cursor: pointer;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-file-name:hover {
  text-decoration: underline;
}

.upload-file-download {
  color: #909399;
  cursor: pointer;
}

.upload-file-download:hover {
  color: var(--el-color-primary, #409eff);
}

.attachment-file-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.attachment-file-download {
  color: #909399;
  cursor: pointer;
}

.attachment-file-download:hover {
  color: var(--el-color-primary, #409eff);
}

.declare-no-attachment-text {
  color: #c0c4cc;
}

/* 验收情况树形表格单元格：图标 + 编号强制同一行 */
.accept-tree-cell {
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  white-space: nowrap;
  gap: 4px;
  width: 100%;
  min-width: 0;
}

.accept-tree-kind--declare {
  color: var(--el-color-primary, #409eff);
  font-size: 14px;
  flex-shrink: 0;
}

.accept-tree-cell .detail-type-icon {
  flex-shrink: 0;
}

.accept-tree-cell__text {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

/* 验收弹窗中的目标信息 */
.accept-target {
  margin-bottom: 16px;
}

.accept-target :deep(.el-descriptions__label),
.accept-target :deep(.el-descriptions__content) {
  padding: 8px 12px;
}

/* 验收资料附件：标题 + 添加附件 */
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

.attachment-empty {
  margin-bottom: 12px;
  padding: 12px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #909399;
  font-size: 12px;
  text-align: center;
}

/* 验收资料附件卡片 */
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

/* ============ 简洁进度条（图1样式：蓝色填充条 + 右侧 N/M 分数） ============ */
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
  background-color: var(--el-color-primary, #409eff);
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

/* 弹窗内 descriptions 场景：间距紧一点 */
.simple-progress--inline {
  gap: 6px;
}

.simple-progress--inline .simple-progress__bar {
  height: 6px;
}
</style>

<!-- 验收管理查看弹窗全局样式（el-dialog 挂 body，需单独 style 无 scoped） -->
<!-- 固定高度方案：弹窗整体占满视口，内容区独立滚动 -->
<style>
.accept-view-dialog {
  height: 100vh !important;
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  top: 0 !important;
  display: flex !important;
  flex-direction: column !important;
}

.accept-view-dialog .el-dialog__header,
.accept-view-dialog .el-dialog__footer {
  flex: none !important;
}

.accept-view-dialog .el-dialog__body {
  flex: 1 !important;
  min-height: 0 !important;
  overflow-y: auto !important;
}
</style>
