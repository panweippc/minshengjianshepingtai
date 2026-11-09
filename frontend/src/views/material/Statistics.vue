<template>
  <div>
    <!-- ==================== 主列表筛选区 ==================== -->
    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="报表名称">
          <el-input
            v-model="query.reportName"
            placeholder="请输入报表名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="生成时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item label="报表类型">
          <el-select
            v-model="query.reportType"
            placeholder="请选择报表类型"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in reportTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ==================== 主列表区 ==================== -->
    <el-card shadow="never" class="list-card">
      <template #header>
        <div class="card-header">
          <span>统计表列表</span>
          <div class="header-actions">
            <el-button
              type="primary"
              icon="Plus"
              @click="openAdd('INVENTORY')"
            >
              新增盘点表
            </el-button>
            <el-button
              type="success"
              icon="Plus"
              @click="openAdd('HANDOVER')"
            >
              新增交接表
            </el-button>
            <el-button
              type="warning"
              icon="Plus"
              @click="openAdd('DAMAGE')"
            >
              新增损毁表
            </el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
        <el-table-column prop="reportName" label="报表名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="时间" width="180" align="center">
          <template #default="{ row }">{{ row.reportDate || row.createTime?.slice(0, 10) || '—' }}</template>
        </el-table-column>
        <el-table-column label="报表类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="reportTypeTag(row.reportType)" effect="light">
              {{ reportTypeLabel(row.reportType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" icon="View" @click="openDetail(row)">查看</el-button>
            <el-button link type="primary" icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" icon="Printer" @click="handlePrint(row)">打印</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <!-- ==================== 新增/编辑弹窗 ==================== -->
    <el-dialog
      v-model="editDialogVisible"
      :title="editDialogTitle"
      width="1100px"
      top="5vh"
      destroy-on-close
      class="report-dialog"
    >
      <!-- 弹窗头部：报表基本信息 -->
      <el-form ref="headerFormRef" :model="headerForm" :rules="headerFormRules" label-width="90px" class="header-form">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="报表名称" prop="reportName">
              <el-input v-model="headerForm.reportName" placeholder="请输入报表名称" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时间" prop="reportDate">
              <el-date-picker
                v-model="headerForm.reportDate"
                type="date"
                placeholder="请选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="报表类型">
              <el-select v-model="headerForm.reportType" disabled style="width: 100%">
                <el-option label="盘点表" value="INVENTORY" />
                <el-option label="交接表" value="HANDOVER" />
                <el-option label="损毁表" value="DAMAGE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 明细筛选区 -->
      <el-card shadow="never" class="detail-query-card">
        <el-form :inline="true" :model="detailQuery" @submit.prevent>
          <el-form-item v-if="editType !== 'DAMAGE'" label="物品名称">
            <el-input
              v-model="detailQuery.itemName"
              placeholder="请输入物品名称"
              clearable
              @keyup.enter="handleDetailQuery"
            />
          </el-form-item>
          <el-form-item v-if="editType !== 'DAMAGE'" label="物品分类">
            <el-select
              v-model="detailQuery.itemCategory"
              placeholder="请选择物品分类"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in categoryOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="仓库名称">
            <el-select
              v-model="detailQuery.warehouseName"
              placeholder="请选择仓库"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="w in warehouseOptions"
                :key="w"
                :label="w"
                :value="w"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="editType === 'DAMAGE'" label="时间">
            <el-date-picker
              v-model="detailQuery.damageDate"
              type="date"
              placeholder="请选择日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleDetailQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetDetailQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 明细表格区：盘点表 -->
      <div v-if="editType === 'INVENTORY'" class="detail-table-wrapper">
        <el-table
          v-loading="detailLoading"
          :data="inventoryItems"
          border
          stripe
          height="320"
        >
          <el-table-column type="index" label="序号" width="70" align="center" />
          <el-table-column label="物品名称" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.itemName }}</template>
          </el-table-column>
          <el-table-column label="库房" width="140" align="center">
            <template #default="{ row }">{{ row.warehouseName || '—' }}</template>
          </el-table-column>
          <el-table-column label="单位" width="80" align="center">
            <template #default="{ row }">{{ row.unit || '—' }}</template>
          </el-table-column>
          <el-table-column label="账面数量" width="110" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.bookQty"
                :min="0"
                size="small"
                controls-position="right"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="盘点数量" width="110" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.actualQty"
                :min="0"
                size="small"
                controls-position="right"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="盘盈数量" width="110" align="center">
            <template #default="{ row }">
              <span :class="{ 'profit': calcProfit(row) > 0, 'loss': calcProfit(row) < 0 }">
                {{ calcProfit(row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="请输入备注" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" icon="Delete" @click="removeInventoryItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <el-button type="primary" icon="Plus" size="small" @click="addInventoryItem">添加行</el-button>
        </div>
      </div>

      <!-- 明细表格区：交接表 -->
      <div v-else-if="editType === 'HANDOVER'" class="detail-table-wrapper">
        <el-table
          v-loading="detailLoading"
          :data="handoverItems"
          border
          stripe
          height="320"
        >
          <el-table-column type="index" label="序号" width="70" align="center" />
          <el-table-column label="物品名称" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.itemName }}</template>
          </el-table-column>
          <el-table-column label="库房" width="140" align="center">
            <template #default="{ row }">{{ row.warehouseName || '—' }}</template>
          </el-table-column>
          <el-table-column label="单位" width="80" align="center">
            <template #default="{ row }">{{ row.unit || '—' }}</template>
          </el-table-column>
          <el-table-column label="账面数量" width="110" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.bookQty"
                :min="0"
                size="small"
                controls-position="right"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="盘点数量" width="110" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.actualQty"
                :min="0"
                size="small"
                controls-position="right"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="盘盈数量" width="110" align="center">
            <template #default="{ row }">
              <span :class="{ 'profit': calcProfit(row) > 0, 'loss': calcProfit(row) < 0 }">
                {{ calcProfit(row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="请输入备注" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" icon="Delete" @click="removeHandoverItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <el-button type="primary" icon="Plus" size="small" @click="addHandoverItem">添加行</el-button>
        </div>
      </div>

      <!-- 明细表格区：损毁表 -->
      <div v-else-if="editType === 'DAMAGE'" class="detail-table-wrapper">
        <el-table
          v-loading="detailLoading"
          :data="damageItems"
          border
          stripe
          height="320"
        >
          <el-table-column type="index" label="序号" width="70" align="center" />
          <el-table-column label="物品名称" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.itemName }}</template>
          </el-table-column>
          <el-table-column label="库房" width="140" align="center">
            <template #default="{ row }">{{ row.warehouseName || '—' }}</template>
          </el-table-column>
          <el-table-column label="单位" width="80" align="center">
            <template #default="{ row }">{{ row.unit || '—' }}</template>
          </el-table-column>
          <el-table-column label="账面数量" width="110" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.bookQty"
                :min="0"
                size="small"
                controls-position="right"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="经办人" width="120" align="center">
            <template #default="{ row }">
              <el-input v-model="row.handler" placeholder="请输入经办人" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="请输入备注" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" icon="Delete" @click="removeDamageItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <el-button type="primary" icon="Plus" size="small" @click="addDamageItem">添加行</el-button>
        </div>
      </div>

      <!-- 底部人员信息区（盘点表、交接表、损毁表通用） -->
      <el-form ref="footerFormRef" :model="footerForm" label-width="90px" class="footer-form">
        <el-row :gutter="16">
          <template v-if="editType === 'INVENTORY'">
            <el-col :span="8">
              <el-form-item label="部门">
                <el-input v-model="footerForm.department" placeholder="请输入部门" maxlength="64" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="经办人">
                <el-input v-model="footerForm.handler" placeholder="请输入经办人" maxlength="32" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="审核人">
                <el-input v-model="footerForm.auditor" placeholder="请输入审核人" maxlength="32" />
              </el-form-item>
            </el-col>
          </template>
          <template v-else-if="editType === 'HANDOVER'">
            <el-col :span="6">
              <el-form-item label="部门">
                <el-input v-model="footerForm.department" placeholder="请输入部门" maxlength="64" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="经办人">
                <el-input v-model="footerForm.handler" placeholder="请输入经办人" maxlength="32" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="交接人">
                <el-input v-model="footerForm.handoverPerson" placeholder="请输入交接人" maxlength="32" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="审核人">
                <el-input v-model="footerForm.auditor" placeholder="请输入审核人" maxlength="32" />
              </el-form-item>
            </el-col>
          </template>
          <template v-else-if="editType === 'DAMAGE'">
            <el-col :span="8">
              <el-form-item label="部门">
                <el-input v-model="footerForm.department" placeholder="请输入部门" maxlength="64" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="经办人">
                <el-input v-model="footerForm.handler" placeholder="请输入经办人" maxlength="32" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="审核人">
                <el-input v-model="footerForm.auditor" placeholder="请输入审核人" maxlength="32" />
              </el-form-item>
            </el-col>
          </template>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 查看弹窗 ==================== -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="'查看 - ' + (detail?.reportName || '')"
      width="1100px"
      top="5vh"
      destroy-on-close
      class="report-dialog"
    >
      <!-- 基本信息 -->
      <el-descriptions v-if="detail" :column="3" border class="detail-desc">
        <el-descriptions-item label="报表名称">{{ detail.reportName }}</el-descriptions-item>
        <el-descriptions-item label="报表类型">
          <el-tag :type="reportTypeTag(detail.reportType)" effect="light">
            {{ reportTypeLabel(detail.reportType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.reportDate || detail.createTime?.slice(0, 10) || '—' }}</el-descriptions-item>
        <el-descriptions-item label="经办人">{{ detail.handler || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.reportType === 'HANDOVER'" label="交接人">{{ detail.handoverPerson || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detail.auditor || '—' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 盘点表明细 -->
      <template v-if="detail?.reportType === 'INVENTORY'">
        <h4 class="section-title">盘点明细</h4>
        <el-table :data="detail.inventoryItems || []" border stripe height="320">
          <el-table-column type="index" label="序号" width="70" align="center" />
          <el-table-column prop="itemName" label="物品名称" min-width="160" />
          <el-table-column prop="warehouseName" label="库房" width="140" align="center">
            <template #default="{ row }">{{ row.warehouseName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="80" align="center">
            <template #default="{ row }">{{ row.unit || '—' }}</template>
          </el-table-column>
          <el-table-column prop="bookQty" label="账面数量" width="110" align="center" />
          <el-table-column prop="actualQty" label="盘点数量" width="110" align="center" />
          <el-table-column label="盘盈数量" width="110" align="center">
            <template #default="{ row }">
              <span :class="{ 'profit': row.profitQty > 0, 'loss': row.profitQty < 0 }">
                {{ row.profitQty }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160">
            <template #default="{ row }">{{ row.remark || '—' }}</template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 交接表明细 -->
      <template v-else-if="detail?.reportType === 'HANDOVER'">
        <h4 class="section-title">交接明细</h4>
        <el-table :data="detail.handoverItems || []" border stripe height="320">
          <el-table-column type="index" label="序号" width="70" align="center" />
          <el-table-column prop="itemName" label="物品名称" min-width="160" />
          <el-table-column prop="warehouseName" label="库房" width="140" align="center">
            <template #default="{ row }">{{ row.warehouseName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="80" align="center">
            <template #default="{ row }">{{ row.unit || '—' }}</template>
          </el-table-column>
          <el-table-column prop="bookQty" label="账面数量" width="110" align="center" />
          <el-table-column prop="actualQty" label="盘点数量" width="110" align="center" />
          <el-table-column label="盘盈数量" width="110" align="center">
            <template #default="{ row }">
              <span :class="{ 'profit': row.profitQty > 0, 'loss': row.profitQty < 0 }">
                {{ row.profitQty }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160">
            <template #default="{ row }">{{ row.remark || '—' }}</template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 损毁表明细 -->
      <template v-else-if="detail?.reportType === 'DAMAGE'">
        <h4 class="section-title">损毁明细</h4>
        <el-table :data="detail.damageItems || []" border stripe height="320">
          <el-table-column type="index" label="序号" width="70" align="center" />
          <el-table-column prop="itemName" label="物品名称" min-width="160" />
          <el-table-column prop="warehouseName" label="库房" width="140" align="center">
            <template #default="{ row }">{{ row.warehouseName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="80" align="center">
            <template #default="{ row }">{{ row.unit || '—' }}</template>
          </el-table-column>
          <el-table-column prop="bookQty" label="账面数量" width="110" align="center" />
          <el-table-column prop="handler" label="经办人" width="140" align="center">
            <template #default="{ row }">{{ row.handler || '—' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="200">
            <template #default="{ row }">{{ row.remark || '—' }}</template>
          </el-table-column>
        </el-table>
      </template>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getDictByType } from '@/api/dict'
import {
  getMaterialReportPage,
  getMaterialReportDetail,
  createMaterialInventory,
  updateMaterialInventory,
  createMaterialHandover,
  updateMaterialHandover,
  createMaterialDamage,
  updateMaterialDamage,
  deleteMaterialReport
} from '@/api/material'
import type { MaterialReportQuery } from '@/api/material'
import type {
  MaterialReportVO,
  MaterialReportDetailVO,
  MaterialReportType,
  MaterialInventoryItemSaveDTO,
  MaterialHandoverItemSaveDTO,
  MaterialDamageItemSaveDTO
} from '@/types/system'

// ==================== 报表类型辅助 ====================
const reportTypeLabel = (type?: string) => {
  const hit = reportTypeOptions.value.find((o) => o.value === type)
  return hit ? hit.label : type || '—'
}

const reportTypeTag = (type?: string) => {
  const map: Record<string, string> = {
    INVENTORY: 'primary',
    HANDOVER: 'success',
    DAMAGE: 'warning'
  }
  return map[type || ''] || 'info'
}

// ==================== 主列表 ====================
const query = reactive<MaterialReportQuery>({
  pageNum: 1,
  pageSize: 10
})
const dateRange = ref<string[]>([])
const total = ref(0)
const tableData = ref<MaterialReportVO[]>([])
const loading = ref(false)
const indexMethod = (index: number) => (query.pageNum - 1) * query.pageSize + index + 1

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMaterialReportPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleDateChange = (val: string[]) => {
  if (val && val.length === 2) {
    query.startTime = val[0]
    query.endTime = val[1]
  } else {
    query.startTime = undefined
    query.endTime = undefined
  }
}

const handleQuery = () => {
  query.pageNum = 1
  loadData()
}

const resetQuery = () => {
  query.reportName = undefined
  query.reportType = undefined
  query.startTime = undefined
  query.endTime = undefined
  dateRange.value = []
  handleQuery()
}

// ==================== 字典和仓库选项 ====================
/** 报表类型字典（baobiao_leixing）—— 字典缺失时兜底选项 */
const DEFAULT_REPORT_TYPES = [
  { label: '盘点表', value: 'INVENTORY' },
  { label: '交接表', value: 'HANDOVER' },
  { label: '损毁表', value: 'DAMAGE' }
]
const reportTypeDictItems = ref<{ dictLabel: string; dictValue: string }[]>([])
const reportTypeOptions = computed(() =>
  reportTypeDictItems.value.length
    ? reportTypeDictItems.value.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    : DEFAULT_REPORT_TYPES
)

const DEFAULT_CATEGORIES = [
  { label: '办公物品', value: '1' },
  { label: '食堂用品', value: '2' },
  { label: '办公家具', value: '3' },
  { label: '办公设备', value: '4' }
]
const categoryDictItems = ref<{ dictLabel: string; dictValue: string }[]>([])
const categoryOptions = computed(() =>
  categoryDictItems.value.length
    ? categoryDictItems.value.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
    : DEFAULT_CATEGORIES
)
const warehouseOptions = ref<string[]>([])

const loadDict = async () => {
  try {
    const [categoryRes, reportTypeRes] = await Promise.all([
      getDictByType('wu_pin_fen_lei'),
      getDictByType('baobiao_leixing')
    ])
    categoryDictItems.value = (categoryRes.data as any[]) || []
    reportTypeDictItems.value = (reportTypeRes.data as any[]) || []
  } catch {
    // 忽略错误，使用默认值
  }
}

const loadWarehouseOptions = async () => {
  try {
    // 尝试从仓库接口加载，失败则使用默认
    const res = await fetch('/api/material/warehouse/simple')
    if (res.ok) {
      const data = await res.json()
      if (data.data) {
        warehouseOptions.value = data.data.map((w: any) => w.warehouseName)
        return
      }
    }
  } catch {
    // 忽略错误
  }
  // 默认仓库选项
  warehouseOptions.value = ['主仓库', '一号库房', '二号库房', '三号库房']
}

// ==================== 新增/编辑弹窗 ====================
const editDialogVisible = ref(false)
const editType = ref<MaterialReportType>('INVENTORY')
const editMode = ref<'add' | 'edit'>('add')
const editDialogTitle = computed(() => {
  const typeLabel = reportTypeLabel(editType.value)
  return `${editMode.value === 'edit' ? '编辑' : '新增'}${typeLabel}`
})

const submitLoading = ref(false)
const headerFormRef = ref<FormInstance>()
const footerFormRef = ref<FormInstance>()

// 头部表单
const headerForm = reactive({
  reportName: '',
  reportDate: '',
  reportType: 'INVENTORY' as MaterialReportType
})

const headerFormRules: FormRules = {
  reportName: [{ required: true, message: '请输入报表名称', trigger: 'blur' }],
  reportDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

// 底部表单（三种类型共用不同字段）
const footerForm = reactive({
  handler: '',
  auditor: '',
  handoverPerson: '',
  department: ''
})

// 明细筛选
const detailQuery = reactive({
  itemName: '',
  itemCategory: '',
  warehouseName: '',
  damageDate: ''
})

const handleDetailQuery = () => {
  // 前端筛选明细数据
}

const resetDetailQuery = () => {
  detailQuery.itemName = ''
  detailQuery.itemCategory = ''
  detailQuery.warehouseName = ''
  detailQuery.damageDate = ''
}

// 盘点表明细
interface InventoryRow extends MaterialInventoryItemSaveDTO {
  _tempId: number
}
const inventoryItems = ref<InventoryRow[]>([])

const addInventoryItem = () => {
  inventoryItems.value.push({
    _tempId: Date.now() + Math.random(),
    warehouseName: '',
    itemName: '',
    unit: '',
    bookQty: 0,
    actualQty: 0,
    remark: ''
  })
}

const removeInventoryItem = (index: number) => {
  inventoryItems.value.splice(index, 1)
}

const calcProfit = (row: { bookQty: number; actualQty: number }) => {
  return (row.actualQty || 0) - (row.bookQty || 0)
}

// 交接表明细
interface HandoverRow extends MaterialHandoverItemSaveDTO {
  _tempId: number
}
const handoverItems = ref<HandoverRow[]>([])

const addHandoverItem = () => {
  handoverItems.value.push({
    _tempId: Date.now() + Math.random(),
    warehouseName: '',
    itemName: '',
    unit: '',
    bookQty: 0,
    actualQty: 0,
    remark: ''
  })
}

const removeHandoverItem = (index: number) => {
  handoverItems.value.splice(index, 1)
}

// 损毁表明细
interface DamageRow extends MaterialDamageItemSaveDTO {
  _tempId: number
}
const damageItems = ref<DamageRow[]>([])

const addDamageItem = () => {
  damageItems.value.push({
    _tempId: Date.now() + Math.random(),
    warehouseName: '',
    itemName: '',
    unit: '',
    bookQty: 0,
    handler: '',
    remark: ''
  })
}

const removeDamageItem = (index: number) => {
  damageItems.value.splice(index, 1)
}

const detailLoading = ref(false)

// 打开新增
const openAdd = (type: MaterialReportType) => {
  editMode.value = 'add'
  editType.value = type
  Object.assign(headerForm, {
    reportName: '',
    reportDate: new Date().toISOString().slice(0, 10),
    reportType: type
  })
  Object.assign(footerForm, {
    handler: '',
    auditor: '',
    handoverPerson: '',
    department: ''
  })
  inventoryItems.value = []
  handoverItems.value = []
  damageItems.value = []
  resetDetailQuery()
  editDialogVisible.value = true
}

// 打开编辑
const openEdit = async (row: MaterialReportVO) => {
  editMode.value = 'edit'
  editType.value = row.reportType
  detailLoading.value = true
  try {
    const res = await getMaterialReportDetail(row.id)
    const d = res.data
    Object.assign(headerForm, {
      reportName: d.reportName,
      reportDate: d.reportDate || '',
      reportType: d.reportType
    })
    Object.assign(footerForm, {
      handler: d.handler || '',
      auditor: d.auditor || '',
      handoverPerson: (d as any).handoverPerson || '',
      department: (d as any).department || ''
    })

    if (d.reportType === 'INVENTORY' && d.inventoryItems) {
      inventoryItems.value = d.inventoryItems.map((it: any) => ({
        _tempId: it.id || Date.now() + Math.random(),
        warehouseName: it.warehouseName || '',
        itemName: it.itemName,
        unit: it.unit || '',
        bookQty: it.bookQty,
        actualQty: it.actualQty,
        remark: it.remark || ''
      }))
    } else if (d.reportType === 'HANDOVER' && d.handoverItems) {
      handoverItems.value = d.handoverItems.map((it: any) => ({
        _tempId: it.id || Date.now() + Math.random(),
        warehouseName: it.warehouseName || '',
        itemName: it.itemName,
        unit: it.unit || '',
        bookQty: it.bookQty,
        actualQty: it.actualQty,
        remark: it.remark || ''
      }))
    } else if (d.reportType === 'DAMAGE' && d.damageItems) {
      damageItems.value = d.damageItems.map((it: any) => ({
        _tempId: it.id || Date.now() + Math.random(),
        warehouseName: it.warehouseName || '',
        itemName: it.itemName,
        unit: it.unit || '',
        bookQty: it.bookQty,
        handler: it.handler || '',
        remark: it.remark || ''
      }))
    }
    editDialogVisible.value = true
  } finally {
    detailLoading.value = false
  }
}

// 提交保存
const handleSubmit = async () => {
  const valid = await headerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  // 校验明细：至少有一行且物品名称不为空
  let items: any[] = []
  if (editType.value === 'INVENTORY') {
    items = inventoryItems.value.filter((it) => it.itemName?.trim())
  } else if (editType.value === 'HANDOVER') {
    items = handoverItems.value.filter((it) => it.itemName?.trim())
  } else if (editType.value === 'DAMAGE') {
    items = damageItems.value.filter((it) => it.itemName?.trim())
  }
  if (items.length === 0) {
    ElMessage.warning('请至少添加一条明细')
    return
  }

  submitLoading.value = true
  try {
    if (editType.value === 'INVENTORY') {
      const payload = {
        id: editMode.value === 'edit' ? (headerForm as any).id : undefined,
        reportName: headerForm.reportName,
        reportDate: headerForm.reportDate,
        handler: footerForm.handler || undefined,
        auditor: footerForm.auditor || undefined,
        department: footerForm.department || undefined,
        items: items.map(({ _tempId, ...rest }) => rest)
      }
      if (editMode.value === 'edit') {
        await updateMaterialInventory(payload)
      } else {
        await createMaterialInventory(payload)
      }
    } else if (editType.value === 'HANDOVER') {
      const payload = {
        id: editMode.value === 'edit' ? (headerForm as any).id : undefined,
        reportName: headerForm.reportName,
        reportDate: headerForm.reportDate,
        handler: footerForm.handler || undefined,
        handoverPerson: footerForm.handoverPerson || undefined,
        auditor: footerForm.auditor || undefined,
        department: footerForm.department || undefined,
        items: items.map(({ _tempId, ...rest }) => rest)
      }
      if (editMode.value === 'edit') {
        await updateMaterialHandover(payload)
      } else {
        await createMaterialHandover(payload)
      }
    } else if (editType.value === 'DAMAGE') {
      const payload = {
        id: editMode.value === 'edit' ? (headerForm as any).id : undefined,
        reportName: headerForm.reportName,
        reportDate: headerForm.reportDate,
        handler: footerForm.handler || undefined,
        auditor: footerForm.auditor || undefined,
        department: footerForm.department || undefined,
        items: items.map(({ _tempId, ...rest }) => rest)
      }
      if (editMode.value === 'edit') {
        await updateMaterialDamage(payload)
      } else {
        await createMaterialDamage(payload)
      }
    }
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

// ==================== 查看弹窗 ====================
const detailDialogVisible = ref(false)
const detail = ref<MaterialReportDetailVO | null>(null)

const openDetail = async (row: MaterialReportVO) => {
  const res = await getMaterialReportDetail(row.id)
  detail.value = res.data
  detailDialogVisible.value = true
}

// ==================== 打印 ====================
const handlePrint = async (row: MaterialReportVO) => {
  try {
    const res = await getMaterialReportDetail(row.id)
    const d = res.data

    const win = window.open('', '_blank')
    if (!win) {
      ElMessage.error('无法打开打印窗口，请允许弹窗')
      return
    }

    const typeLabel = reportTypeLabel(d.reportType)
    let tableHtml = ''
    let detailFields = ''

    if (d.reportType === 'INVENTORY' && d.inventoryItems) {
      detailFields = `
        <tr><th>序号</th><th>物品名称</th><th>库房</th><th>单位</th><th>账面数量</th><th>盘点数量</th><th>盘盈数量</th><th>备注</th></tr>
      `
      d.inventoryItems.forEach((it, i) => {
        const profit = (it.actualQty || 0) - (it.bookQty || 0)
        detailFields += `
          <tr>
            <td>${i + 1}</td>
            <td>${it.itemName}</td>
            <td>${it.warehouseName || ''}</td>
            <td>${it.unit || ''}</td>
            <td>${it.bookQty}</td>
            <td>${it.actualQty}</td>
            <td style="color:${profit > 0 ? '#67c23a' : profit < 0 ? '#f56c6c' : ''}">${profit}</td>
            <td>${it.remark || ''}</td>
          </tr>
        `
      })
      tableHtml = `<table>${detailFields}</table>`
    } else if (d.reportType === 'HANDOVER' && d.handoverItems) {
      detailFields = `
        <tr><th>序号</th><th>物品名称</th><th>库房</th><th>单位</th><th>账面数量</th><th>盘点数量</th><th>盘盈数量</th><th>备注</th></tr>
      `
      d.handoverItems.forEach((it, i) => {
        const profit = (it.actualQty || 0) - (it.bookQty || 0)
        detailFields += `
          <tr>
            <td>${i + 1}</td>
            <td>${it.itemName}</td>
            <td>${it.warehouseName || ''}</td>
            <td>${it.unit || ''}</td>
            <td>${it.bookQty}</td>
            <td>${it.actualQty}</td>
            <td style="color:${profit > 0 ? '#67c23a' : profit < 0 ? '#f56c6c' : ''}">${profit}</td>
            <td>${it.remark || ''}</td>
          </tr>
        `
      })
      tableHtml = `<table>${detailFields}</table>`
    } else if (d.reportType === 'DAMAGE' && d.damageItems) {
      detailFields = `
        <tr><th>序号</th><th>物品名称</th><th>库房</th><th>单位</th><th>账面数量</th><th>经办人</th><th>备注</th></tr>
      `
      d.damageItems.forEach((it, i) => {
        detailFields += `
          <tr>
            <td>${i + 1}</td>
            <td>${it.itemName}</td>
            <td>${it.warehouseName || ''}</td>
            <td>${it.unit || ''}</td>
            <td>${it.bookQty}</td>
            <td>${it.handler || ''}</td>
            <td>${it.remark || ''}</td>
          </tr>
        `
      })
      tableHtml = `<table>${detailFields}</table>`
    }

    const scriptTag = '<scr' + 'ipt>window.onload = function() { window.print(); window.close(); }</scr' + 'ipt>'
    const extraSign = d.reportType === 'HANDOVER'
      ? '<div class="sign-item"><div class="label">交接人</div><div>________________</div></div>'
      : ''

    win.document.write(
      '<html>' +
      '<head>' +
      '<title>' + d.reportName + ' - ' + typeLabel + '</title>' +
      '<style>' +
      'body { font-family: SimSun, sans-serif; padding: 30px; }' +
      'h1 { text-align: center; margin-bottom: 10px; }' +
      '.meta { text-align: center; color: #666; margin-bottom: 20px; }' +
      'table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }' +
      'th, td { border: 1px solid #000; padding: 8px; text-align: center; font-size: 14px; }' +
      'th { background: #f5f5f5; }' +
      '.sign-area { display: flex; justify-content: space-between; margin-top: 40px; font-size: 14px; }' +
      '.sign-item { text-align: center; }' +
      '.sign-item .label { margin-bottom: 30px; }' +
      '</style>' +
      '</head>' +
      '<body>' +
      '<h1>' + d.reportName + '</h1>' +
      '<div class="meta">报表类型：' + typeLabel + '　　日期：' + (d.reportDate || d.createTime?.slice(0, 10) || '') + '</div>' +
      tableHtml +
      '<div class="sign-area">' +
      '<div class="sign-item"><div class="label">部门</div><div>________________</div></div>' +
      '<div class="sign-item"><div class="label">经办人</div><div>________________</div></div>' +
      extraSign +
      '<div class="sign-item"><div class="label">审核人</div><div>________________</div></div>' +
      '<div class="sign-item"><div class="label">审核日期</div><div>________________</div></div>' +
      '</div>' +
      scriptTag +
      '</body>' +
      '</html>'
    )
    win.document.close()
  } catch (e) {
    console.error(e)
    ElMessage.error('打印预览失败')
  }
}

// ==================== 删除 ====================
const handleDelete = (row: MaterialReportVO) => {
  ElMessageBox.confirm(`确认删除报表「${row.reportName}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteMaterialReport(row.id)
      ElMessage.success('删除成功')
      await loadData()
    })
    .catch((err) => {
      if (err !== 'cancel' && err !== 'close') {
        console.error(err)
      }
    })
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadDict()
  loadWarehouseOptions()
  loadData()
})
</script>

<style scoped>
.query-card {
  margin-bottom: 0;
}
.query-card :deep(.el-form--inline) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}
.query-card :deep(.el-form-item) {
  flex: 1 1 auto;
  min-width: 0;
  margin-right: 16px;
  margin-bottom: 0;
}
.query-card :deep(.el-form-item__content) {
  flex: 1;
  min-width: 0;
}
.query-card :deep(.el-input),
.query-card :deep(.el-select),
.query-card :deep(.el-date-editor) {
  width: 100%;
}
.query-card :deep(.el-form-item:last-child) {
  flex: 0 0 auto;
  margin-right: 0;
}

.list-card {
  margin-top: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  gap: 8px;
}

/* 弹窗样式 */
.report-dialog :deep(.el-dialog__body) {
  max-height: 75vh;
  overflow-y: auto;
}

.header-form {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.detail-query-card {
  margin-bottom: 12px;
}
.detail-query-card :deep(.el-form--inline) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}
.detail-query-card :deep(.el-form-item) {
  margin-bottom: 8px;
}

.detail-table-wrapper {
  margin-bottom: 12px;
}
.table-footer {
  margin-top: 8px;
  text-align: left;
}

.footer-form {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

/* 盘盈盘亏样式 */
.profit {
  color: #67c23a;
  font-weight: 600;
}
.loss {
  color: #f56c6c;
  font-weight: 600;
}

/* 查看弹窗 */
.detail-desc {
  margin-bottom: 16px;
}
.detail-desc :deep(.el-descriptions__label) {
  width: 90px;
}
.section-title {
  margin: 16px 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
</style>
