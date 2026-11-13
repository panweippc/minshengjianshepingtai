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
          <el-dropdown
            trigger="click"
            placement="bottom-end"
            popper-class="user-dropdown-popper"
            @command="handleCommand"
          >
            <span class="user-name">
              {{ userInfo?.realName || userInfo?.username || '系统管理员' }}
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 个人中心弹窗 -->
    <el-dialog
      v-model="profileVisible"
      title="个人中心"
      width="520px"
      class="profile-dialog"
      append-to-body
      @open="resetProfileForm"
    >
      <el-form
        ref="profileFormRef"
        :model="profileForm"
        :rules="profileRules"
        label-width="92px"
        label-position="left"
        class="profile-form"
      >
        <div class="form-section-title">基础账号信息</div>
        <el-form-item label="登录账号">
          <el-input class="readonly-input" :model-value="userInfo?.username || ''" readonly />
        </el-form-item>
        <el-form-item label="所属区划">
          <el-input
            class="readonly-input"
            :model-value="userInfo?.regionName || userInfo?.regionCode || ''"
            readonly
          />
        </el-form-item>
        <el-form-item label="当前角色">
          <el-input
            class="readonly-input"
            :model-value="userInfo?.roles?.length ? userInfo.roles.join('、') : ''"
            readonly
          />
        </el-form-item>
        <el-form-item label="账号状态">
          <el-input
            class="readonly-input"
            :model-value="userInfo?.status === 1 ? '启用' : '停用'"
            readonly
          />
        </el-form-item>
        <el-form-item label="首次创建">
          <el-input class="readonly-input" :model-value="userInfo?.createTime || ''" readonly />
        </el-form-item>

        <div class="form-section-title">可编辑信息</div>
        <el-form-item label="用户姓名" prop="realName">
          <el-input v-model="profileForm.realName" placeholder="请输入用户姓名" maxlength="32" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="profileForm.phone" placeholder="请输入联系电话" maxlength="11" />
        </el-form-item>

        <div class="form-section-title">修改登录密码</div>
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="profileForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="profileForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="profileForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取 消</el-button>
        <el-button type="primary" :loading="profileSaving" @click="handleSaveProfile">
          保存修改
        </el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'
import { updateProfile, changePassword } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const { userInfo, routeMenus } = storeToRefs(store)

/** 个人中心弹窗开关 */
const profileVisible = ref(false)
/** 个人中心表单实例 */
const profileFormRef = ref<FormInstance>()
/** 保存中状态 */
const profileSaving = ref(false)

/** 个人中心表单：基础信息只读，姓名/电话可编辑，密码按需修改 */
const profileForm = reactive({
  realName: '',
  phone: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/** 是否填写了任意密码字段，决定密码区校验是否生效 */
const passwordStarted = () =>
  !!(profileForm.oldPassword || profileForm.newPassword || profileForm.confirmPassword)

/** 原密码校验 */
const validateOldPassword: FormItemRule['validator'] = (_rule, _value, callback) => {
  if (!passwordStarted()) {
    callback()
    return
  }
  if (!profileForm.oldPassword) {
    callback(new Error('请输入原密码'))
    return
  }
  callback()
}

/** 新密码校验 */
const validateNewPassword: FormItemRule['validator'] = (_rule, _value, callback) => {
  if (!passwordStarted()) {
    callback()
    return
  }
  if (!profileForm.newPassword) {
    callback(new Error('请输入新密码'))
    return
  }
  if (profileForm.newPassword.length < 6 || profileForm.newPassword.length > 32) {
    callback(new Error('密码长度须为6-32位'))
    return
  }
  callback()
}

/** 确认密码校验 */
const validateConfirmPassword: FormItemRule['validator'] = (_rule, _value, callback) => {
  if (!passwordStarted()) {
    callback()
    return
  }
  if (!profileForm.confirmPassword) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (profileForm.confirmPassword !== profileForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

/** 电话非必填，填写时校验格式 */
const validatePhone: FormItemRule['validator'] = (_rule, value, callback) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('联系电话格式不正确'))
    return
  }
  callback()
}

const profileRules: FormRules = {
  realName: [
    { required: true, message: '请输入用户姓名', trigger: 'blur' },
    { max: 32, message: '姓名长度不能超过32位', trigger: 'blur' }
  ],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  oldPassword: [{ validator: validateOldPassword, trigger: 'blur' }],
  newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

/** 打开弹窗时回填当前用户资料 */
const resetProfileForm = () => {
  profileForm.realName = userInfo.value?.realName || ''
  profileForm.phone = userInfo.value?.phone || ''
  profileForm.oldPassword = ''
  profileForm.newPassword = ''
  profileForm.confirmPassword = ''
  profileFormRef.value?.clearValidate()
}

/** 保存个人资料与密码 */
const handleSaveProfile = async () => {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) return
  profileSaving.value = true
  try {
    await updateProfile({
      realName: profileForm.realName,
      phone: profileForm.phone || undefined
    })
    // 仅在填写了新密码时提交密码修改
    if (profileForm.newPassword) {
      await changePassword({
        oldPassword: profileForm.oldPassword,
        newPassword: profileForm.newPassword
      })
    }
    await store.fetchUserInfo()
    ElMessage.success('保存成功')
    profileVisible.value = false
  } catch {
    // 拦截器已提示错误信息
  } finally {
    profileSaving.value = false
  }
}

/** 当前激活菜单 */
const activeMenu = computed(() => route.path)

/** 用户下拉命令 */
const handleCommand = (cmd: string) => {
  if (cmd === 'profile') {
    profileVisible.value = true
    return
  }
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
  color: #409eff;
  font-size: 15px;
  outline: none;
  transition: color 0.2s ease;
}
.user-name:hover {
  color: #66b1ff;
}
.user-arrow {
  font-size: 12px;
  transition: transform 0.2s ease;
}

/* ========== 个人中心弹窗 ========== */
.profile-form :deep(.el-form-item) {
  margin-bottom: 16px;
}
.profile-form :deep(.el-form-item__label) {
  color: #303133;
}
/* 分区标题：左侧蓝色竖条 */
.form-section-title {
  margin: 4px 0 14px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}
.form-section-title:not(:first-child) {
  margin-top: 10px;
}
/* 只读字段：浅灰边框与文字，区分可编辑项 */
.readonly-input :deep(.el-input__wrapper) {
  background-color: #fafafa;
  box-shadow: 0 0 0 1px #e4e7ed inset;
}
.readonly-input :deep(.el-input__inner) {
  color: #909399;
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

<!-- 下拉面板被 teleport 到 body，scoped 样式无法命中，使用 popper-class 全局定制 -->
<style>
.user-dropdown-popper .el-dropdown-menu {
  min-width: 132px;
  padding: 2px 0;
}
.user-dropdown-popper .el-dropdown-menu__item {
  padding: 9px 16px;
  font-size: 14px;
  color: #303133;
}
.user-dropdown-popper .el-dropdown-menu__item:not(.is-disabled):hover {
  background-color: #ecf5ff;
  color: #409eff;
}
.user-dropdown-popper .el-dropdown-menu__item--divided {
  margin-top: 0;
}
/* 个人中心弹窗内容超高时内部滚动，避免超出视口 */
.profile-dialog .el-dialog__body {
  max-height: 62vh;
  overflow-y: auto;
}
</style>
