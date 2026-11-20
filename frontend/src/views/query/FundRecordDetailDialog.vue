<template>
  <div class="fund-record-detail-dialog">
    <el-table :data="rows" v-loading="loading" stripe border size="default">
      <el-table-column prop="nodeLevelLabel" label="层级" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="nodeTagType(row.nodeLevel)" size="small">{{ row.nodeLevelLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="approvalCode" label="立项编号" width="100" />
      <el-table-column prop="projectName" label="项目名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="declareCode" label="申报编号" width="100" />
      <el-table-column prop="declareName" label="申报名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="detailName" label="明细名称" min-width="140" show-overflow-tooltip />
      <el-table-column label="资金类型" width="100">
        <template #default="{ row }">{{ row.fundTypeLabel || '-' }}</template>
      </el-table-column>
      <el-table-column label="资金来源" width="110">
        <template #default="{ row }">{{ row.fundSourceLabel || '-' }}</template>
      </el-table-column>
      <el-table-column label="金额（元）" width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="townName" label="乡镇" width="100" />
      <el-table-column prop="villageName" label="村组" width="120" />
      <el-table-column prop="createBy" label="经办人" width="90" />
      <el-table-column prop="createTime" label="经办时间" width="160" />
      <el-table-column label="附件" width="80" align="center">
        <template #default="{ row }">
          <el-link v-if="row.attachCount > 0" type="primary" :underline="false" @click="$emit('showDetail', row)">
            {{ row.attachCount }} 份
          </el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="$emit('showDetail', row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 10px; justify-content: flex-end"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { getFundDetailList, type FundDetailRow } from '@/api/fundQuery'

const props = defineProps<{
  groupType: string
  groupValue: string
  villageName?: string
  label: string
  year?: string
  regionCode?: string
}>()

defineEmits<{
  (e: 'showDetail', row: FundDetailRow): void
}>()

const rows = ref<FundDetailRow[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getFundDetailList({
      year: props.year,
      regionCode: props.regionCode,
      groupType: props.groupType,
      groupValue: props.groupValue,
      villageName: props.villageName
    })
    const data = res?.data ?? (res as any)
    const all: FundDetailRow[] = data ?? []
    total.value = all.length
    // 内存分页（后端 detailList 直接全量返回，弹窗分页足够）
    const start = (pageNum.value - 1) * pageSize.value
    rows.value = all.slice(start, start + pageSize.value)
  } finally {
    loading.value = false
  }
}

function nodeTagType(lv: number | null): 'primary' | 'success' | 'warning' | 'info' {
  if (lv === 1) return 'primary'
  if (lv === 2) return 'success'
  if (lv === 3) return 'warning'
  return 'info'
}

function formatMoney(v: any): string {
  if (v == null) return '0.00'
  const n = typeof v === 'string' ? Number(v) : v
  if (Number.isNaN(n)) return '0.00'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2, minimumFractionDigits: 2 })
}

onMounted(load)
watch([() => props.groupType, () => props.groupValue, () => props.villageName], () => { pageNum.value = 1; load() })
watch([pageNum, pageSize], load)
</script>

<style scoped>
.fund-record-detail-dialog { }
</style>
