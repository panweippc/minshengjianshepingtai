<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="900px"
    destroy-on-close
    @update:model-value="(v: boolean) => emit('update:visible', v)"
    @close="emit('close')"
  >
    <div v-loading="loading" class="fund-detail-dialog">
      <template v-if="data">
        <!-- 基本信息（FundNodeVO 是通用树节点，不同 nodeType 字段含义不同） -->
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="节点类型">{{ data.nodeType }}</el-descriptions-item>
          <el-descriptions-item label="编号">{{ data.code || '-' }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ data.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ data.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ data.type || '-' }}</el-descriptions-item>
          <el-descriptions-item label="子节点数">{{ data.children?.length ?? 0 }}</el-descriptions-item>
        </el-descriptions>

        <!-- 资金流水 -->
        <el-divider content-position="left">资金流水</el-divider>
        <el-table :data="data.records ?? []" size="small" border stripe>
          <el-table-column prop="nodeLevelLabel" label="层级" width="90" />
          <el-table-column prop="fundTypeLabel" label="资金类型" width="110" />
          <el-table-column prop="fundSourceLabel" label="资金来源" width="110" />
          <el-table-column prop="amount" label="金额（元）" width="120" align="right">
            <template #default="{ row }">{{ Number(row.amount ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</template>
          </el-table-column>
          <el-table-column prop="createBy" label="经办人" width="90" />
          <el-table-column prop="createTime" label="经办时间" width="170" />
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        </el-table>
      </template>
    </div>

    <template #footer>
      <el-button @click="emit('close')">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchFundApprovalDetail, fetchFundDeclareDetail, fetchFundDetail } from '@/api/fund'
import type { FundNodeVO } from '@/api/fund'

const props = defineProps<{
  visible: boolean
  /** approval 项目 / declare 申报 / detail 明细 */
  nodeType: 'approval' | 'declare' | 'detail'
  nodeId: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'close'): void
}>()

const loading = ref(false)
const data = ref<FundNodeVO | null>(null)

const dialogTitle = computed(() => ({
  approval: '项目节点详情',
  declare:  '申报节点详情',
  detail:   '明细节点详情'
}[props.nodeType]))

async function load() {
  if (!props.nodeId) return
  loading.value = true
  try {
    if (props.nodeType === 'approval')      data.value = await fetchFundApprovalDetail(props.nodeId)
    else if (props.nodeType === 'declare')  data.value = await fetchFundDeclareDetail(props.nodeId)
    else                                    data.value = await fetchFundDetail(props.nodeId)
  } catch (e: any) {
    ElMessage.error('加载详情失败：' + (e?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, v => { if (v) { data.value = null; load() } })
</script>

<style scoped>
.fund-detail-dialog { min-height: 200px; }
</style>
