<template>
  <div v-loading="loading" class="fund-detail-dialog">
    <el-empty v-if="!nodeId" description="未选择节点" />

    <!-- 一级项目详情 -->
    <template v-else-if="nodeType === 'approval'">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="项目资金" name="fund">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="项目编号">{{ detail?.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目名称">{{ detail?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目类别">{{ dictLabel('xiang_mu_lei_bie', detail?.category) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目类型">{{ dictLabel('xiang_mu_lei_xing', detail?.type) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目预算">{{ formatMoney(detail?.budget) }} 元</el-descriptions-item>
            <el-descriptions-item label="拨付金额">{{ formatMoney(detail?.allocatedAmount) }} 元</el-descriptions-item>
            <el-descriptions-item label="结余">
              <span :class="{ 'neg': Number(detail?.balance) < 0 }">
                {{ formatMoney(detail?.balance) }} 元
              </span>
            </el-descriptions-item>
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
            <el-table-column label="附件" width="100" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.attachments?.length"
                  link
                  type="primary"
                  @click="openAttachList(row)"
                >附件 ({{ row.attachments.length }})</el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="事项申报" name="declare">
          <el-empty v-if="!detail?.children?.length" description="暂无申报内容" :image-size="60" />
          <el-table
            v-else
            :data="detail.children"
            row-key="id"
            border
            size="small"
            default-expand-all
          >
            <el-table-column prop="code" label="申报编号" width="190" show-overflow-tooltip />
            <el-table-column prop="name" label="申报标题" min-width="200" show-overflow-tooltip />
            <el-table-column label="申报金额" width="130" align="right">
              <template #default="{ row }">{{ formatMoney(row.budget) }}</template>
            </el-table-column>
            <el-table-column label="下拨金额" width="130" align="right">
              <template #default="{ row }">{{ formatMoney(row.allocatedAmount) }}</template>
            </el-table-column>
            <el-table-column prop="villageName" label="村组" width="140" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- 二级申报详情 -->
    <template v-else-if="nodeType === 'declare'">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="事项申报资金" name="declareFund">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="申报编号">{{ detail?.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申报标题">{{ detail?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申报类别">{{ detail?.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申报类型">{{ detail?.type || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申报金额">{{ formatMoney(detail?.budget) }} 元</el-descriptions-item>
            <el-descriptions-item label="下拨金额">{{ formatMoney(detail?.allocatedAmount) }} 元</el-descriptions-item>
            <el-descriptions-item label="申报内容" :span="2">{{ detail?.declareContent || '-' }}</el-descriptions-item>
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
            <el-table-column label="附件" width="100" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.attachments?.length"
                  link
                  type="primary"
                  @click="openAttachList(row)"
                >附件 ({{ row.attachments.length }})</el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- 三级明细详情 -->
    <template v-else>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="明细资金" name="fund">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="农户/设施名称">{{ detail?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="户号/编码">{{ detail?.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属区划">{{ detail?.type || '-' }}</el-descriptions-item>
            <el-descriptions-item label="事项说明">{{ detail?.detailRemark || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申报金额">{{ formatMoney(detail?.budget) }} 元</el-descriptions-item>
            <el-descriptions-item label="拨付金额">{{ formatMoney(detail?.allocatedAmount) }} 元</el-descriptions-item>
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
            <el-table-column label="附件" width="100" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.attachments?.length"
                  link
                  type="primary"
                  @click="openAttachList(row)"
                >附件 ({{ row.attachments.length }})</el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- 附件列表弹窗 -->
    <el-dialog
      v-model="attachVisible"
      :title="attachTitle"
      width="640px"
      append-to-body
      destroy-on-close
    >
      <el-table :data="attachList" size="small" border>
        <el-table-column prop="attachmentType" label="附件类型" width="150">
          <template #default="{ row }">{{ row.attachmentType || '-' }}</template>
        </el-table-column>
        <el-table-column label="附件名称" min-width="240">
          <template #default="{ row }">
            <div class="attach-name" @click="openFile(row)">
              {{ row.fileName || (row.fileKey?.split('/').pop?.()) || '点击查看' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="上传时间" width="170" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useDict } from '@/composables/useDict'
import {
  fetchFundApprovalDetail,
  fetchFundDeclareDetail,
  fetchFundDetail
} from '@/api/fund'
import type { FundNodeVO, FundRecordVO } from '@/api/fund'

const props = defineProps<{
  nodeType: 'approval' | 'declare' | 'detail'
  nodeId: string
}>()

const { dictLabel } = useDict()

const loading = ref(false)
const recordLoading = ref(false)
const detail = ref<FundNodeVO | null>(null)
const records = ref<FundRecordVO[]>([])
const activeTab = ref('fund')

const attachVisible = ref(false)
const attachList = ref<any[]>([])
const attachTitle = computed(() => `资金凭证附件（${attachList.value.length}）`)

async function loadDetail() {
  if (!props.nodeId) return
  loading.value = true
  records.value = []
  try {
    if (props.nodeType === 'approval') {
      detail.value = await fetchFundApprovalDetail(props.nodeId)
    } else if (props.nodeType === 'declare') {
      detail.value = await fetchFundDeclareDetail(props.nodeId)
    } else {
      detail.value = await fetchFundDetail(props.nodeId)
    }
    records.value = detail.value?.records || []
  } finally {
    loading.value = false
    recordLoading.value = false
  }
}

function onTabChange(_tab: string) { /* 预留扩展点 */ }

function openAttachList(row: any) {
  attachList.value = row.attachments || []
  attachVisible.value = true
}

function openFile(row: any) {
  const fileUrl = (row as any).fileUrl
  if (fileUrl) window.open(fileUrl, '_blank')
}

function formatMoney(v: number | null | undefined): string {
  if (v == null || isNaN(v)) return '0'
  return v.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

watch(
  () => [props.nodeType, props.nodeId],
  () => {
    activeTab.value = props.nodeType === 'declare' ? 'declareFund' : 'fund'
    loadDetail()
  },
  { immediate: true }
)
</script>

<style scoped>
.attach-name {
  color: #409eff;
  cursor: pointer;
}
.attach-name:hover {
  text-decoration: underline;
}
.neg {
  color: #f56c6c;
}
</style>
