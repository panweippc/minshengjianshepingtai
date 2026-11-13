<template>
  <div class="archive-page">
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

    <!-- 列表区 -->
    <el-card shadow="never" class="list-card">
      <el-table v-loading="loading" :data="tableData" border stripe class="archive-table">
        <el-table-column prop="archiveNo" label="档案编号" width="180" show-overflow-tooltip />
        <el-table-column prop="approvalCode" label="项目编号" width="190" show-overflow-tooltip />
        <el-table-column prop="projectName" label="项目名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="项目类别" width="130" show-overflow-tooltip>
          <template #default="{ row }">
            {{ dictLabel('xiang_mu_lei_bie', row.projectCategory) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="项目类型" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ dictLabel('xiang_mu_lei_xing', row.projectType) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="leader" label="负责人" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.leader || '-' }}</template>
        </el-table-column>
        <el-table-column prop="archiveTime" label="归档时间" width="170">
          <template #default="{ row }">{{ row.archiveTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="storageLocation" label="存储位置" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.storageLocation || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                v-if="canView"
                link
                type="primary"
                icon="View"
                @click="handleView(row)"
              >查看</el-button>
              <el-button
                v-if="canEdit"
                link
                type="primary"
                icon="Edit"
                @click="openEdit(row)"
              >编辑</el-button>
              <el-button
                v-if="canExport"
                link
                type="primary"
                icon="Download"
                :loading="exportingId === row.id"
                @click="handleExport(row)"
              >导出</el-button>
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

    <!-- 编辑弹窗：基础信息只读 + 档案位置可维护 -->
    <el-dialog v-model="editVisible" title="编辑档案" width="760px" destroy-on-close>
      <el-form :model="editForm" label-width="110px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目编号">
              <el-input :model-value="editForm.approvalCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目名称">
              <el-input :model-value="editForm.projectName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类别">
              <el-input :model-value="dictLabel('xiang_mu_lei_bie', editForm.projectCategory)" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类型">
              <el-input :model-value="dictLabel('xiang_mu_lei_xing', editForm.projectType)" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">档案位置</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="档案编号">
              <el-input :model-value="editForm.archiveNo" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归档时间">
              <el-date-picker
                v-model="editForm.archiveTime"
                type="datetime"
                placeholder="请选择归档时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="存储位置">
              <el-input v-model="editForm.storageLocation" maxlength="255" placeholder="请输入存储位置（档案室/档案柜等）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看弹窗：六个页签 -->
    <el-dialog v-model="viewVisible" title="档案详情" width="1100px" destroy-on-close>
      <div v-loading="viewLoading" class="archive-view-body">
        <el-tabs v-model="viewTab" class="archive-view-tabs">
          <!-- 1. 事项申报 -->
          <el-tab-pane label="事项申报" name="declare">
            <el-empty v-if="!detail?.declares?.length" description="暂无事项申报数据" :image-size="80" />
            <template v-else>
              <el-table :data="detail.declares" border size="small">
                <el-table-column prop="declareCode" label="申报编号" width="170" show-overflow-tooltip />
                <el-table-column prop="declareName" label="申报标题" min-width="180" show-overflow-tooltip />
                <el-table-column label="申报类型" width="120" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ dictLabel('shen_bao_lei_xing', row.declareType) || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="申报类别" width="110" show-overflow-tooltip>
                  <template #default="{ row }">{{ categoryLabel(row.declareCategory) }}</template>
                </el-table-column>
                <el-table-column label="申报金额（元）" width="130" align="right">
                  <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
                </el-table-column>
                <el-table-column prop="declarationDate" label="申报日期" width="110">
                  <template #default="{ row }">{{ row.declarationDate || '-' }}</template>
                </el-table-column>
                <el-table-column prop="townName" label="所属乡镇" width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.townName || '-' }}</template>
                </el-table-column>
                <el-table-column prop="villageName" label="所属村组" width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.villageName || '-' }}</template>
                </el-table-column>
                <el-table-column prop="remark" label="备注说明" min-width="150" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.remark || '-' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="150" fixed="right">
                  <template #default="{ row }">
                    <div class="action-buttons">
                      <el-button
                        link
                        type="primary"
                        icon="Paperclip"
                        @click="openAttachments('申报材料附件', row.attachments, 'li_xiang_fu_jian_lei_xing')"
                      >附件</el-button>
                      <el-button link type="primary" icon="Tickets" @click="openDeclareDetails(row)">明细</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </el-tab-pane>

          <!-- 2. 立项 -->
          <el-tab-pane label="立项" name="approval">
            <template v-if="detail?.approval">
              <el-descriptions :column="2" border label-width="110px">
                <el-descriptions-item label="项目编号">{{ detail.approval.approvalCode || '-' }}</el-descriptions-item>
                <el-descriptions-item label="项目名称">{{ detail.approval.projectName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="项目类别">
                  {{ dictLabel('xiang_mu_lei_bie', detail.approval.projectCategory) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="项目类型">
                  {{ dictLabel('xiang_mu_lei_xing', detail.approval.projectType) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="项目预算">{{ formatMoney(detail.approval.budget) }} 元</el-descriptions-item>
                <el-descriptions-item label="负责人">{{ detail.approval.leader || '-' }}</el-descriptions-item>
                <el-descriptions-item label="提交时间">{{ detail.approval.submitTime || '-' }}</el-descriptions-item>
                <el-descriptions-item label="所属乡镇">{{ detail.approval.regionName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{ detail.approval.remark || '-' }}</el-descriptions-item>
              </el-descriptions>

              <el-divider content-position="left">立项资料附件</el-divider>
              <el-table :data="detail.approvalAttachments || []" border size="small">
                <el-table-column label="附件类型" width="160">
                  <template #default="{ row }">
                    {{ dictLabel('li_xiang_fu_jian_lei_xing', row.attachmentType) || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="文件名称" min-width="240">
                  <template #default="{ row }">
                    <div class="attachment-file-cell">
                      <span class="attachment-file-name" :title="row.fileName" @click="openFile(row)">
                        {{ row.fileName || '-' }}
                      </span>
                      <el-icon class="attachment-file-download" title="下载" @click.stop="openFile(row, true)">
                        <Download />
                      </el-icon>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
                <el-table-column prop="createTime" label="上传时间" width="170">
                  <template #default="{ row }">{{ row.createTime || '-' }}</template>
                </el-table-column>
              </el-table>
            </template>
            <el-empty v-else description="暂无立项信息" :image-size="80" />
          </el-tab-pane>

          <!-- 3. 资金 -->
          <el-tab-pane label="资金" name="fund">
            <el-empty v-if="!fundTree.length" description="暂无资金数据" :image-size="80" />
            <el-table
              v-else
              :data="fundTree"
              row-key="id"
              :tree-props="{ children: 'children' }"
              default-expand-all
              border
              size="small"
            >
              <el-table-column label="编号" width="190" show-overflow-tooltip>
                <template #default="{ row }">{{ row.code || '-' }}</template>
              </el-table-column>
              <el-table-column label="名称" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">{{ row.name || '-' }}</template>
              </el-table-column>
              <el-table-column label="层级" width="100">
                <template #default="{ row }">{{ fundNodeTypeLabel(row.nodeType) }}</template>
              </el-table-column>
              <el-table-column label="预算/金额（元）" width="140" align="right">
                <template #default="{ row }">{{ formatMoney(row.budget) }}</template>
              </el-table-column>
              <el-table-column label="资金来源" width="130" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ dictLabel('zi_jin_lai_yuan', row.fundSource) || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="拨付金额（元）" width="140" align="right">
                <template #default="{ row }">{{ formatMoney(row.allocatedAmount) }}</template>
              </el-table-column>
              <el-table-column label="结余（元）" width="130" align="right">
                <template #default="{ row }">
                  <template v-if="row.nodeType === 'approval'">{{ formatMoney(row.balance) }}</template>
                  <template v-else>-</template>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" icon="View" @click="openFundRecords(row)">资金流水</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 4. 施工 -->
          <el-tab-pane label="施工" name="construction">
            <template v-if="detail?.construction">
              <el-descriptions :column="2" border label-width="130px">
                <el-descriptions-item label="施工单位名称">{{ detail.construction.builder || '-' }}</el-descriptions-item>
                <el-descriptions-item label="承包人姓名">{{ detail.construction.contractorName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="承包人联系电话">{{ detail.construction.contractorPhone || '-' }}</el-descriptions-item>
                <el-descriptions-item label="负责人姓名">{{ detail.construction.leaderName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="负责人联系电话">{{ detail.construction.leaderPhone || '-' }}</el-descriptions-item>
                <el-descriptions-item label="开工/完工日期">
                  {{ detail.construction.startDate || '-' }} ~ {{ detail.construction.completeDate || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{ detail.construction.remark || '-' }}</el-descriptions-item>
              </el-descriptions>

              <el-divider content-position="left">巡检记录</el-divider>
              <el-empty v-if="!inspectionRows.length" description="暂无巡检记录" :image-size="60" />
              <el-table v-else :data="inspectionRows" border size="small">
                <el-table-column prop="code" label="户号/编码" width="190" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.code || '-' }}</template>
                </el-table-column>
                <el-table-column prop="name" label="农户/设施名称" min-width="200" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.name || '-' }}</template>
                </el-table-column>
                <el-table-column prop="regionName" label="所属区划" min-width="160" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.regionName || '-' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button link type="primary" icon="View" @click="openInspectionRecords(row)">详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <el-empty v-else description="暂无施工信息" :image-size="80" />
          </el-tab-pane>

          <!-- 5. 验收 -->
          <el-tab-pane label="验收" name="acceptance">
            <el-empty v-if="!acceptanceRows.length" description="暂无验收数据" :image-size="80" />
            <el-table v-else :data="acceptanceRows" border size="small">
              <el-table-column label="层级" width="90">
                <template #default="{ row }">{{ acceptanceLevelLabel(row.nodeType) }}</template>
              </el-table-column>
              <el-table-column label="编号" width="190" show-overflow-tooltip>
                <template #default="{ row }">{{ row.code || '-' }}</template>
              </el-table-column>
              <el-table-column label="名称" min-width="190" show-overflow-tooltip>
                <template #default="{ row }">{{ row.name || '-' }}</template>
              </el-table-column>
              <el-table-column label="验收进度" width="110">
                <template #default="{ row }">
                  <template v-if="row.nodeType === 'detail'">-</template>
                  <template v-else>{{ row.acceptedCount || 0 }}/{{ row.totalCount || 0 }}</template>
                </template>
              </el-table-column>
              <el-table-column label="验收状态" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.acceptStatus === 'ACCEPTED'" type="success" size="small">已验收</el-tag>
                  <el-tag v-else type="info" size="small">待验收</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="acceptTime" label="验收时间" width="170">
                <template #default="{ row }">{{ row.acceptTime || '-' }}</template>
              </el-table-column>
              <el-table-column prop="acceptBy" label="验收人" width="110">
                <template #default="{ row }">{{ row.acceptBy || '-' }}</template>
              </el-table-column>
              <el-table-column label="验收结果" width="120" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ dictLabel('yan_shou_jie_guo', row.acceptResult) || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="110" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" icon="View" @click="openAcceptanceRecords(row)">验收记录</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 6. 档案 -->
          <el-tab-pane label="档案" name="archive">
            <template v-if="detail?.archive">
              <el-descriptions :column="2" border label-width="110px">
                <el-descriptions-item label="档案编号">{{ detail.archive.archiveNo || '-' }}</el-descriptions-item>
                <el-descriptions-item label="存储位置">{{ detail.archive.storageLocation || '-' }}</el-descriptions-item>
                <el-descriptions-item label="归档时间">{{ detail.archive.archiveTime || '-' }}</el-descriptions-item>
                <el-descriptions-item label="归档人">{{ detail.archive.archiveBy || '-' }}</el-descriptions-item>
                <el-descriptions-item label="项目编号">{{ detail.archive.approvalCode || '-' }}</el-descriptions-item>
                <el-descriptions-item label="项目名称">{{ detail.archive.projectName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="项目类别">
                  {{ dictLabel('xiang_mu_lei_bie', detail.archive.projectCategory) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="项目类型">
                  {{ dictLabel('xiang_mu_lei_xing', detail.archive.projectType) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="负责人" :span="2">{{ detail.archive.leader || '-' }}</el-descriptions-item>
              </el-descriptions>
            </template>
            <el-empty v-else description="暂无档案信息" :image-size="80" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <!-- 申报明细弹窗：农户 / 公共基础设施 -->
    <el-dialog v-model="declareDetailVisible" :title="declareDetailTitle" width="1000px" append-to-body destroy-on-close>
      <el-empty v-if="!declareDetailRow?.details?.length" description="暂无申报明细" :image-size="70" />
      <template v-else>
        <el-table v-if="isHouseholdDeclare" :data="declareDetailRow.details" border size="small">
          <el-table-column prop="code" label="户号" width="170" show-overflow-tooltip />
          <el-table-column prop="name" label="户主" width="110" show-overflow-tooltip />
          <el-table-column prop="idCard" label="身份证号" width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.idCard || '-' }}</template>
          </el-table-column>
          <el-table-column prop="phone" label="联系电话" width="130">
            <template #default="{ row }">{{ row.phone || '-' }}</template>
          </el-table-column>
          <el-table-column label="户类型" width="110">
            <template #default="{ row }">{{ dictLabel('hu_lei_xing', row.typeValue) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="regionName" label="村组" width="130" show-overflow-tooltip>
            <template #default="{ row }">{{ row.regionName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="事项说明" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
          <el-table-column label="申报金额（元）" width="130" align="right">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="附件" width="90">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                icon="Paperclip"
                @click="openAttachments('明细附件', row.attachments, 'fu_jian_lei_xing')"
              >附件</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-table v-else :data="declareDetailRow.details" border size="small">
          <el-table-column prop="code" label="设施编码" width="170" show-overflow-tooltip />
          <el-table-column prop="name" label="设施名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="设施类型" width="120">
            <template #default="{ row }">{{ dictLabel('she_shi_lei_xing', row.typeValue) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="regionName" label="村组" width="130" show-overflow-tooltip>
            <template #default="{ row }">{{ row.regionName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="事项说明" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
          <el-table-column label="申报金额（元）" width="130" align="right">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="附件" width="90">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                icon="Paperclip"
                @click="openAttachments('明细附件', row.attachments, 'fu_jian_lei_xing')"
              >附件</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <!-- 资金流水弹窗 -->
    <el-dialog v-model="fundRecordVisible" :title="fundRecordTitle" width="1000px" append-to-body destroy-on-close>
      <el-empty v-if="!fundRecords.length" description="暂无资金流水" :image-size="70" />
      <el-table v-else v-loading="fundRecordLoading" :data="fundRecords" border size="small">
        <el-table-column label="资金类型" width="140">
          <template #default="{ row }">
            {{ fundTypeLabel(row) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="资金来源" width="130">
          <template #default="{ row }">{{ dictLabel('zi_jin_lai_yuan', row.fundSource) || '-' }}</template>
        </el-table-column>
        <el-table-column label="金额（元）" width="140" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createBy" label="操作人" width="110">
          <template #default="{ row }">{{ row.createBy || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="170">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="附件" width="90">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              icon="Paperclip"
              @click="openAttachments('资金凭证附件', row.attachments, 'zi_jin_fu_jian_lei_xing')"
            >附件</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 巡检记录弹窗 -->
    <el-dialog v-model="inspectionVisible" :title="inspectionTitle" width="1000px" append-to-body destroy-on-close>
      <el-empty v-if="!inspectionRecords.length" description="暂无巡检记录" :image-size="70" />
      <el-table v-else v-loading="inspectionLoading" :data="inspectionRecords" border size="small">
        <el-table-column prop="constructionDate" label="记录日期" width="120">
          <template #default="{ row }">{{ row.constructionDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="inspector" label="巡检人" width="110">
          <template #default="{ row }">{{ row.inspector || '-' }}</template>
        </el-table-column>
        <el-table-column label="巡检状态" width="100">
          <template #default="{ row }">{{ dictLabel('xun_jian_zhuang_tai', row.inspectionStatus) || '-' }}</template>
        </el-table-column>
        <el-table-column label="施工状态" width="110">
          <template #default="{ row }">{{ dictLabel('shi_gong_zhuang_tai', row.constructionStatus) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="content" label="施工内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content || '-' }}</template>
        </el-table-column>
        <el-table-column label="附件" width="90">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              icon="Paperclip"
              @click="openAttachments('施工记录附件', row.attachments, 'shi_gong_fu_jian_lei_xing')"
            >附件</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 验收记录弹窗 -->
    <el-dialog v-model="acceptanceRecordVisible" :title="acceptanceRecordTitle" width="960px" append-to-body destroy-on-close>
      <el-empty v-if="!acceptanceRecords.length" description="暂无验收记录" :image-size="70" />
      <el-table v-else :data="acceptanceRecords" border size="small">
        <el-table-column label="验收结果" width="150">
          <template #default="{ row }">{{ dictLabel('yan_shou_jie_guo', row.acceptResult) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="acceptTime" label="验收时间" width="170">
          <template #default="{ row }">{{ row.acceptTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="acceptBy" label="验收人" width="110">
          <template #default="{ row }">{{ row.acceptBy || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="验收备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="附件" width="90">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              icon="Paperclip"
              @click="openAttachments('验收资料附件', row.attachments, 'yan_shou_fu_jian_lei_xing')"
            >附件</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 通用附件列表弹窗 -->
    <el-dialog v-model="attachVisible" :title="attachTitle" width="760px" append-to-body destroy-on-close>
      <el-empty v-if="!attachList.length" description="暂无附件" :image-size="70" />
      <el-table v-else :data="attachList" border size="small">
        <el-table-column label="附件类型" width="160">
          <template #default="{ row }">
            {{ attachDictType ? (dictLabel(attachDictType, row.attachmentType) || row.attachmentType || '-') : (row.attachmentType || '-') }}
          </template>
        </el-table-column>
        <el-table-column label="文件名称" min-width="240">
          <template #default="{ row }">
            <div class="attachment-file-cell">
              <span class="attachment-file-name" :title="row.fileName" @click="openFile(row)">
                {{ row.fileName || '-' }}
              </span>
              <el-icon class="attachment-file-download" title="下载" @click.stop="openFile(row, true)">
                <Download />
              </el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="170">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getDictByType } from '@/api/dict'
import { getPresignUrl } from '@/api/file'
import type { DictVO } from '@/types/system'
import {
  getArchivePage,
  getArchiveDetail,
  updateArchive,
  exportArchive,
  type ArchiveVO,
  type ArchiveDetailVO,
  type ArchiveDeclareVO,
  type ArchiveAttachmentVO
} from '@/api/archive'
import { getFundRecords, type FundNodeVO, type FundRecordVO } from '@/api/fund'
import { getDetailLogHistory, type ConstructionLogVO } from '@/api/construction'
import type { AcceptanceNodeVO, AcceptanceRecordVO } from '@/api/acceptance'

/**
 * 档案管理
 * 所属菜单：档案管理
 * 数据来源：验收管理归档后的项目；查看按事项申报、立项、资金、施工、验收、档案六页签展示
 */
const userStore = useUserStore()
const canView = computed(() => userStore.hasPerm('archive:view'))
const canEdit = computed(() => userStore.hasPerm('archive:edit'))
const canExport = computed(() => userStore.hasPerm('archive:export'))

/* ============ 字典 ============ */
const dictMap = ref<Record<string, DictVO[]>>({})
const DICT_TYPES = [
  'xiang_mu_lei_bie',
  'xiang_mu_lei_xing',
  'shen_bao_lei_bie',
  'shen_bao_lei_xing',
  'li_xiang_fu_jian_lei_xing',
  'fu_jian_lei_xing',
  'hu_lei_xing',
  'she_shi_lei_xing',
  'zi_jin_lei_xing',
  'xia_bo_lei_xing',
  'zi_jin_lai_yuan',
  'zi_jin_fu_jian_lei_xing',
  'shi_gong_fu_jian_lei_xing',
  'xun_jian_zhuang_tai',
  'shi_gong_zhuang_tai',
  'yan_shou_jie_guo',
  'yan_shou_fu_jian_lei_xing'
]
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

/* ============ 筛选与列表 ============ */
const queryForm = reactive({
  projectName: '',
  projectCategory: '',
  projectType: ''
})
const loading = ref(false)
const tableData = ref<ArchiveVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getArchivePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      projectName: queryForm.projectName || undefined,
      projectCategory: queryForm.projectCategory || undefined,
      projectType: queryForm.projectType || undefined
    })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
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
  queryForm.projectType = ''
  handleQuery()
}

/* ============ 编辑 ============ */
const editVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  id: '',
  archiveNo: '',
  storageLocation: '',
  archiveTime: '',
  approvalCode: '',
  projectName: '',
  projectCategory: '',
  projectType: ''
})

const openEdit = (row: ArchiveVO) => {
  editForm.id = row.id
  editForm.archiveNo = row.archiveNo || ''
  editForm.storageLocation = row.storageLocation || ''
  editForm.archiveTime = row.archiveTime || ''
  editForm.approvalCode = row.approvalCode || ''
  editForm.projectName = row.projectName || ''
  editForm.projectCategory = row.projectCategory || ''
  editForm.projectType = row.projectType || ''
  editVisible.value = true
}

const handleEditSubmit = async () => {
  if (!editForm.archiveTime) {
    ElMessage.warning('请选择归档时间')
    return
  }
  editLoading.value = true
  try {
    await updateArchive({
      id: editForm.id,
      storageLocation: editForm.storageLocation || undefined,
      archiveTime: editForm.archiveTime
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    await loadData()
  } finally {
    editLoading.value = false
  }
}

/* ============ 导出 ============ */
const exportingId = ref('')

const handleExport = async (row: ArchiveVO) => {
  exportingId.value = row.id
  try {
    const blob = await exportArchive(row.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${row.projectName || '项目档案'}-档案资料.zip`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } finally {
    exportingId.value = ''
  }
}

/* ============ 查看 ============ */
const viewVisible = ref(false)
const viewLoading = ref(false)
const viewTab = ref('declare')
const detail = ref<ArchiveDetailVO | null>(null)

const handleView = async (row: ArchiveVO) => {
  viewVisible.value = true
  viewLoading.value = true
  viewTab.value = 'declare'
  detail.value = null
  try {
    const res = await getArchiveDetail(row.id)
    detail.value = res.data
  } finally {
    viewLoading.value = false
  }
}

/* ============ 事项申报 ============ */
const declareDetailVisible = ref(false)
const declareDetailRow = ref<ArchiveDeclareVO | null>(null)
const declareDetailTitle = computed(() => {
  const row = declareDetailRow.value
  return row?.declareName ? `申报明细 - ${row.declareName}` : '申报明细'
})
const isHouseholdDeclare = computed(
  () => declareDetailRow.value?.details?.[0]?.detailType === 'HOUSEHOLD'
)

const openDeclareDetails = (row: ArchiveDeclareVO) => {
  declareDetailRow.value = row
  declareDetailVisible.value = true
}

/* ============ 资金 ============ */
const fundTree = computed<FundNodeVO[]>(() => (detail.value?.fund ? [detail.value.fund] : []))
const fundRecordVisible = ref(false)
const fundRecordLoading = ref(false)
const fundRecordTitle = ref('资金流水')
const fundRecords = ref<FundRecordVO[]>([])

const fundNodeTypeLabel = (nodeType?: string): string => {
  if (nodeType === 'approval') return '项目'
  if (nodeType === 'declare') return '申报'
  if (nodeType === 'detail') return '明细'
  return nodeType || '-'
}

const fundTypeLabel = (record: FundRecordVO): string => {
  if (record.nodeLevel === 1) {
    return dictLabel('zi_jin_lei_xing', record.fundType)
  }
  return dictLabel('xia_bo_lei_xing', record.fundType)
}

const openFundRecords = async (row: FundNodeVO) => {
  fundRecordVisible.value = true
  fundRecordLoading.value = true
  fundRecordTitle.value = `${row.name || '-'} 资金流水`
  fundRecords.value = row.records || []
  try {
    if (row.id && row.nodeType) {
      const res = await getFundRecords(row.nodeType, row.id)
      fundRecords.value = res.data || []
    }
  } finally {
    fundRecordLoading.value = false
  }
}

/* ============ 施工巡检记录 ============ */
interface InspectionRow {
  detailId: string
  code: string
  name: string
  regionName: string
}

const inspectionRows = computed<InspectionRow[]>(() => {
  const rows: InspectionRow[] = []
  const construction = detail.value?.construction
  construction?.children?.forEach((declareNode) => {
    declareNode.children?.forEach((detailNode) => {
      if (!detailNode.detailId) return
      rows.push({
        detailId: detailNode.detailId,
        code: detailNode.code || '',
        name: detailNode.name || '',
        regionName: detailNode.source || ''
      })
    })
  })
  return rows
})

const inspectionVisible = ref(false)
const inspectionLoading = ref(false)
const inspectionTitle = ref('巡检记录')
const inspectionRecords = ref<ConstructionLogVO[]>([])

const openInspectionRecords = async (row: InspectionRow) => {
  inspectionVisible.value = true
  inspectionLoading.value = true
  inspectionTitle.value = `${row.name || '-'} 巡检记录`
  inspectionRecords.value = []
  try {
    const res = await getDetailLogHistory(row.detailId)
    inspectionRecords.value = res.data || []
  } finally {
    inspectionLoading.value = false
  }
}

/* ============ 验收 ============ */
interface AcceptanceRow {
  rowKey: string
  nodeType: string
  code?: string
  name?: string
  acceptedCount?: number
  totalCount?: number
  acceptStatus?: string
  acceptTime?: string
  acceptBy?: string
  acceptResult?: string
  records: AcceptanceRecordVO[]
}

const acceptanceRows = computed<AcceptanceRow[]>(() => {
  const rows: AcceptanceRow[] = []
  const walk = (node: AcceptanceNodeVO) => {
    rows.push({
      rowKey: `${node.nodeType}_${node.id}`,
      nodeType: node.nodeType,
      code: node.code,
      name: node.name,
      acceptedCount: node.acceptedCount,
      totalCount: node.totalCount,
      acceptStatus: node.acceptStatus,
      acceptTime: node.acceptTime,
      acceptBy: node.acceptBy,
      acceptResult: node.acceptResult,
      records: node.acceptanceRecords || []
    })
    node.children?.forEach(walk)
  }
  const root = detail.value?.acceptance
  if (root) {
    walk(root)
  }
  return rows
})

const acceptanceLevelLabel = (nodeType?: string): string => {
  if (nodeType === 'project') return '项目'
  if (nodeType === 'declare') return '申报'
  if (nodeType === 'detail') return '明细'
  return '-'
}

const acceptanceRecordVisible = ref(false)
const acceptanceRecordTitle = ref('验收记录')
const acceptanceRecords = ref<AcceptanceRecordVO[]>([])

const openAcceptanceRecords = (row: AcceptanceRow) => {
  acceptanceRecordVisible.value = true
  acceptanceRecordTitle.value = `${row.name || '-'} 验收记录`
  acceptanceRecords.value = row.records
}

/* ============ 附件 ============ */
const attachVisible = ref(false)
const attachTitle = ref('附件列表')
const attachDictType = ref('')
const attachList = ref<ArchiveAttachmentVO[]>([])

const openAttachments = (title: string, list?: ArchiveAttachmentVO[], dictType?: string) => {
  attachTitle.value = title
  attachDictType.value = dictType || ''
  attachList.value = list || []
  attachVisible.value = true
}

/** 打开附件：预览或下载（MinIO 时效签名URL，禁止永久直链） */
const openFile = async (
  file: { bucket?: string; fileKey?: string; fileName?: string },
  download = false
) => {
  if (!file?.fileKey) return
  const res = await getPresignUrl({
    bucket: file.bucket || 'archive-formal',
    fileKey: file.fileKey,
    fileName: file.fileName,
    download
  })
  window.open(res.data, '_blank')
}

/* ============ 通用 ============ */
const formatMoney = (value?: number): string => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 申报类别：兼容字典值与 1/2 编码 */
const categoryLabel = (value?: string): string => {
  if (!value) return '-'
  const label = dictLabel('shen_bao_lei_bie', value)
  if (label !== value) return label
  if (value === '1') return '到户项目'
  if (value === '2') return '公建项目'
  return value
}

onMounted(async () => {
  await loadDicts()
  await loadData()
})
</script>

<style scoped>
.archive-page {
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

.archive-table {
  margin-top: 4px;
}

.action-buttons {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
}

.action-buttons :deep(.el-button) {
  margin-left: 0;
}

.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}

/* 查看弹窗：固定高度 + flex 纵向布局，避免页签内容多少导致高度跳动 */
.archive-view-body {
  height: 76vh;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.archive-view-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.archive-view-tabs :deep(.el-tabs__header) {
  flex-shrink: 0;
}

.archive-view-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

/* 立项页签附件：文件名可点击预览，右侧下载图标 */
.attachment-file-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.attachment-file-name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-color-primary, #409eff);
  cursor: pointer;
}

.attachment-file-name:hover {
  text-decoration: underline;
}

.attachment-file-download {
  flex-shrink: 0;
  color: #909399;
  cursor: pointer;
}

.attachment-file-download:hover {
  color: var(--el-color-primary, #409eff);
}
</style>
