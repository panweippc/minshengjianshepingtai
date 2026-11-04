<template>
  <div class="home">
    <el-card class="card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">民生建设项目管理平台</span>
        </div>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="当前用户">{{ userInfo?.realName }}（{{ userInfo?.username }}）</el-descriptions-item>
        <el-descriptions-item label="所属区划">{{ userInfo?.regionName || userInfo?.regionCode || '未分配' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ userInfo?.roles?.join('、') || '无' }}</el-descriptions-item>
        <el-descriptions-item label="后端联通">
          <el-tag v-if="loading" type="info">检测中...</el-tag>
          <el-tag v-else-if="health" type="success">已连接（{{ health.service }}）</el-tag>
          <el-tag v-else type="danger">未连接</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="health" label="后端时间">{{ health.time }}</el-descriptions-item>
      </el-descriptions>
      <div class="actions">
        <el-button type="primary" :loading="loading" @click="check">重新检测</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { healthCheck } from '@/api/test'
import type { HealthData } from '@/api/test'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'

const store = useUserStore()
const { userInfo } = storeToRefs(store)

const loading = ref(false)
const health = ref<HealthData | null>(null)

async function check() {
  loading.value = true
  try {
    const res = await healthCheck()
    health.value = res.data
  } catch {
    health.value = null
  } finally {
    loading.value = false
  }
}

onMounted(check)
</script>

<style scoped>
.home {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.card {
  width: 560px;
}

.card-header .title {
  font-size: 18px;
  font-weight: 600;
}

.actions {
  margin-top: 16px;
  text-align: right;
}
</style>
