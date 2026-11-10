<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon><Platform /></el-icon>
        <span>民生建设项目管理平台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="menu"
        router
        background-color="#001529"
        text-color="#a6adb4"
        active-text-color="#ffffff"
      >
        <template v-for="menu in routeMenus" :key="menu.id">
          <!-- 目录 -->
          <el-sub-menu v-if="menu.menuType === 'M'" :index="menu.path || String(menu.id)">
            <template #title>
              <el-icon><component :is="menu.icon || 'Folder'" /></el-icon>
              <span>{{ menu.menuName }}</span>
            </template>
            <el-menu-item
              v-for="child in menu.children"
              :key="child.id"
              :index="child.path"
            >
              <el-icon><component :is="child.icon || 'Document'" /></el-icon>
              <span>{{ child.menuName }}</span>
            </el-menu-item>
          </el-sub-menu>
          <!-- 菜单 -->
          <el-menu-item v-else :index="menu.path">
            <el-icon><component :is="menu.icon || 'Document'" /></el-icon>
            <span>{{ menu.menuName }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="user-info">
          <el-dropdown @command="handleCommand">
            <span class="user-name">
              {{ userInfo?.realName || userInfo?.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const { userInfo, routeMenus } = storeToRefs(store)

/** 当前激活菜单 */
const activeMenu = computed(() => route.path)

/** 用户下拉命令 */
const handleCommand = (cmd: string) => {
  if (cmd === 'logout') {
    store.logout().then(() => {
      router.push('/login')
    })
  }
}
</script>

<style scoped>
/* ========== 布局骨架 ========== */
/* flex 链关键：每一层都加 min-height:0 防止子项撑开 */
.layout-container {
  height: 100vh;
  display: flex;
  overflow: hidden;
}
/* el-aside */
.aside {
  background-color: #001529;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  height: 100%;
}
/* 右侧 el-container（Element Plus 自带 flex-direction: row→column 覆盖） */
.layout-container > :deep(.el-container) {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

/* ========== 侧边栏 ========== */
.logo {
  height: 60px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  gap: 8px;
  border-bottom: 1px solid #1f2d3d;
}
.menu {
  border-right: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}
/* 侧边栏滚动条样式 */
.menu::-webkit-scrollbar {
  width: 6px;
}
.menu::-webkit-scrollbar-thumb {
  background-color: #2f4b66;
  border-radius: 3px;
}
.menu::-webkit-scrollbar-track {
  background-color: transparent;
}

/* ========== 顶部栏 ========== */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  background-color: #fff;
  padding: 0 20px;
  flex-shrink: 0;
}
.user-name {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #303133;
}

/* ========== 主内容区（关键修复） ========== */
/* 之前缺 flex:1 / min-height:0 / overflow:auto，导致内容超过视口时被截断 */
.main {
  flex: 1;
  min-height: 0;
  background-color: #f5f7fa;
  padding: 20px;
  box-sizing: border-box;
  overflow-y: auto;
  overflow-x: hidden;
}
/* 主区域滚动条美化 */
.main::-webkit-scrollbar {
  width: 8px;
}
.main::-webkit-scrollbar-thumb {
  background-color: #c0c4cc;
  border-radius: 4px;
}
.main::-webkit-scrollbar-thumb:hover {
  background-color: #909399;
}
</style>
