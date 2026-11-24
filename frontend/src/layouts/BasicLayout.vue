<template>
  <el-container class="layout-container">
    <el-aside :width="sidebarCollapsed ? '0px' : '220px'" class="aside">
      <div class="logo">
        <el-icon><Platform /></el-icon>
        <span>民生建设项目管理平台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="menu"
        router
        background-color="transparent"
        text-color="#dbe9f2"
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
        <!-- 侧边栏收起/展开按钮：无边框图标按钮，图标方向随状态切换 -->
        <el-button
          text
          class="sidebar-toggle"
          :title="sidebarCollapsed ? '展开菜单栏' : '收起菜单栏'"
          @click="toggleSidebar"
        >
          <el-icon>
            <Fold v-if="!sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
        </el-button>
        <!-- 页签左箭头：页签溢出时显示 -->
        <el-icon
          class="tabs-arrow"
          :class="{ 'is-hidden': !tabsOverflow }"
          title="向左滚动页签"
          @click="scrollTabs(-1)"
        >
          <ArrowLeft />
        </el-icon>
        <!-- 多页签导航：记录已打开的菜单页面，页签过多可左右滑动切换 -->
        <div ref="tabsScrollRef" class="tabs-scroll" @wheel="handleTabsWheel" @scroll="updateScrollState">
          <div
            v-for="tab in tabs"
            :key="tab.path"
            class="tab-item"
            :class="{ 'is-active': activeTab === tab.path }"
            @click="handleTabClick(tab)"
          >
            <span class="tab-title">{{ tab.title }}</span>
            <el-icon class="tab-close" title="关闭页签" @click.stop="handleTabClose(tab)">
              <Close />
            </el-icon>
          </div>
        </div>
        <!-- 页签右箭头：页签溢出时显示 -->
        <el-icon
          class="tabs-arrow"
          :class="{ 'is-hidden': !tabsOverflow }"
          title="向右滚动页签"
          @click="scrollTabs(1)"
        >
          <ArrowRight />
        </el-icon>
        <div class="user-info">
          <el-dropdown
            trigger="click"
            placement="bottom-end"
            popper-class="user-dropdown-popper"
            @command="handleCommand"
          >
            <span class="user-trigger">
              <span class="user-avatar">
                <el-icon><UserFilled /></el-icon>
              </span>
              <span class="user-name">{{ userInfo?.realName || userInfo?.username || '系统管理员' }}</span>
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
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'
import { updateProfile, changePassword } from '@/api/auth'

/** 页签项 */
interface TabItem {
  path: string
  title: string
}

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const { userInfo, routeMenus } = storeToRefs(store)

/** 已打开的页签，首页同样作为可关闭页签 */
const tabs = ref<TabItem[]>([{ path: '/home', title: '首页' }])
/** 当前激活页签路径 */
const activeTab = ref('')
/** 页签容器 ref，用于滚动定位与横向滑动 */
const tabsScrollRef = ref<HTMLElement>()
/** 左侧菜单栏是否收起 */
const sidebarCollapsed = ref(false)
/** 页签区是否发生横向溢出：溢出时左右箭头同时显示 */
const tabsOverflow = ref(false)

/** 更新页签区横向溢出状态 */
const updateScrollState = () => {
  const container = tabsScrollRef.value
  if (!container) return
  tabsOverflow.value = container.scrollWidth > container.clientWidth + 1
}

/** 点击左右箭头：按可视宽度的 60% 平滑滚动页签区 */
const scrollTabs = (direction: 1 | -1) => {
  const container = tabsScrollRef.value
  if (!container || !tabsOverflow.value) return
  container.scrollBy({ left: direction * Math.max(160, container.clientWidth * 0.6), behavior: 'smooth' })
}

/** 切换左侧菜单栏收起/展开 */
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

/** 打开页签：已存在则更新标题，否则追加到末尾 */
const addTab = (path: string, title: string) => {
  const hit = tabs.value.find((tab) => tab.path === path)
  if (hit) {
    hit.title = title
  } else {
    tabs.value.push({ path, title })
  }
  activeTab.value = path
}

/** 切换页签 */
const handleTabClick = (tab: TabItem) => {
  if (activeTab.value === tab.path) return
  router.push(tab.path)
}

/** 关闭页签：关闭激活页签时优先跳转右侧页签，其次左侧；全部关闭后回到首页 */
const handleTabClose = (tab: TabItem) => {
  const index = tabs.value.findIndex((item) => item.path === tab.path)
  if (index === -1) return
  const isActive = activeTab.value === tab.path
  tabs.value.splice(index, 1)
  if (!isActive) {
    nextTick(updateScrollState)
    return
  }
  const nextTab = tabs.value[index] ?? tabs.value[index - 1] ?? tabs.value[0]
  if (nextTab) {
    router.push(nextTab.path)
    return
  }
  // 关闭最后一个页签：重新登记首页页签并跳转首页
  addTab('/home', '首页')
  router.push('/home')
}

/** 路由变化时登记页签 */
watch(
  () => route.path,
  (path) => {
    addTab(path, (route.meta.title as string) || path)
  },
  { immediate: true }
)

/** 页签超出可视宽度时，鼠标滚轮横向滚动实现左右滑动 */
const handleTabsWheel = (event: WheelEvent) => {
  const container = tabsScrollRef.value
  if (!container || container.scrollWidth <= container.clientWidth) return
  // 已带横向位移（触控板左/右滑）时交给浏览器默认处理
  if (Math.abs(event.deltaX) > Math.abs(event.deltaY)) return
  container.scrollLeft += event.deltaY
  event.preventDefault()
}

/** 激活页签自动滚入可视区域，避免切换后页签被遮挡 */
watch(activeTab, async (path) => {
  await nextTick()
  const container = tabsScrollRef.value
  const index = tabs.value.findIndex((tab) => tab.path === path)
  const tabEl = container?.children[index] as HTMLElement | undefined
  if (!container || !tabEl) {
    updateScrollState()
    return
  }
  const left = tabEl.offsetLeft
  const right = left + tabEl.offsetWidth
  if (left < container.scrollLeft) {
    container.scrollTo({ left: left - 8, behavior: 'smooth' })
  } else if (right > container.scrollLeft + container.clientWidth) {
    container.scrollTo({ left: right - container.clientWidth + 8, behavior: 'smooth' })
  }
  updateScrollState()
})

/** 页签容器尺寸观察器：窗口缩放、菜单栏收起时重新判断是否需要箭头 */
let tabsResizeObserver: ResizeObserver | undefined

/** 首次挂载后同步箭头显隐状态，并监听页签区宽度变化 */
onMounted(() => {
  nextTick(updateScrollState)
  if (tabsScrollRef.value) {
    tabsResizeObserver = new ResizeObserver(updateScrollState)
    tabsResizeObserver.observe(tabsScrollRef.value)
  }
})

onBeforeUnmount(() => {
  tabsResizeObserver?.disconnect()
})

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
  /* 政务蓝渐变侧边栏：取色自参考图的深蓝色 */
  background: linear-gradient(180deg, #22619e 0%, #17518c 100%);
  border-right: none;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  height: 100%;
  transition: width 0.25s ease;
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
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  gap: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}
.logo .el-icon {
  color: #8fc7ff;
}
.menu {
  border-right: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  background-color: transparent;
}
/* 菜单项悬停：白色淡叠加，保持深蓝底的通透感 */
.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}
/* 子菜单项与一级菜单使用同一底色，避免展开后出现深浅不一的色块 */
.menu :deep(.el-sub-menu .el-menu-item) {
  background-color: transparent;
}
.menu :deep(.el-sub-menu .el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.1);
}
/* 激活菜单：白色渐变高亮块 + 浅蓝左侧细条 */
.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.24) 0%, rgba(255, 255, 255, 0.05) 100%);
  color: #ffffff;
  font-weight: 600;
  box-shadow: inset 3px 0 0 #9ecdfa;
}
/* 侧边栏滚动条样式 */
.menu::-webkit-scrollbar {
  width: 6px;
}
.menu::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.24);
  border-radius: 3px;
}
.menu::-webkit-scrollbar-track {
  background-color: transparent;
}

/* ========== 顶部栏 ========== */
.header {
  display: flex;
  align-items: center;
  height: 44px;
  border-bottom: 1px solid #e6eef7;
  background-color: #fff;
  padding: 0 16px;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(31, 95, 168, 0.05);
  position: relative;
  z-index: 1;
}
/* 用户信息：头像 + 姓名 + 下拉箭头，左侧用细分割线与页签区分隔 */
.user-info {
  flex-shrink: 0;
  padding-left: 14px;
  margin-left: 4px;
  border-left: 1px solid #eef4f8;
}
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  outline: none;
  transition: background-color 0.2s ease;
}
.user-trigger:hover {
  background-color: #eef3f7;
}
.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #e8eff5;
  color: #17518c;
  font-size: 15px;
}
.user-name {
  color: #14384a;
  font-size: 14px;
  white-space: nowrap;
  transition: color 0.2s ease;
}
.user-trigger:hover .user-name {
  color: #17518c;
}
.user-arrow {
  font-size: 12px;
  color: #909399;
  transition: transform 0.2s ease;
}

/* ========== 侧边栏收起按钮 ========== */
/* 轻量无边框图标按钮，避免顶栏出现多余线框 */
.sidebar-toggle {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  margin-right: 6px;
  padding: 0;
  border: none;
  border-radius: 4px;
  background-color: transparent;
  color: #4a5c68;
}
.sidebar-toggle:hover {
  background-color: #eef3f7;
  color: #17518c;
}
.sidebar-toggle .el-icon {
  font-size: 18px;
}

/* ========== 页签左右滚动箭头 ========== */
.tabs-arrow {
  flex: 0 0 auto;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  font-size: 14px;
  color: #909399;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease;
}
.tabs-arrow:not(.is-hidden):hover {
  background-color: #eef3f7;
  color: #17518c;
}
/* 无被遮挡页签时隐藏箭头（保留占位，避免页签区宽度抖动） */
.tabs-arrow.is-hidden {
  visibility: hidden;
  pointer-events: none;
}

/* ========== 多页签导航（嵌入顶栏） ========== */
/* 参考样式：未激活页签为纯文字，激活页签为蓝色圆角胶囊 + 白色关闭图标 */
.tabs-scroll {
  flex: 1;
  min-width: 0;
  margin: 0 12px;
  display: flex;
  position: relative;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
  overflow-y: hidden;
  overscroll-behavior-x: contain;
  scrollbar-width: none;
}
.tabs-scroll::-webkit-scrollbar {
  width: 0;
  height: 0;
}
.tab-item {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  color: #606266;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  user-select: none;
  transition: color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease,
    box-shadow 0.2s ease;
}
.tab-item:hover {
  border-color: #cbd9e4;
  background-color: #f2f6fa;
  color: #17518c;
}
.tab-item.is-active {
  padding: 0 12px;
  border-color: #bcd6ee;
  background-color: #e8f1fb;
  box-shadow: 0 2px 6px rgba(23, 81, 140, 0.08);
  color: #17518c;
  font-weight: 500;
}
.tab-title {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* 关闭图标：激活页签常显，其余页签仅在鼠标悬停时显示 */
.tab-close {
  flex: 0 0 auto;
  font-size: 12px;
  color: #c0c4cc;
  border-radius: 50%;
  visibility: hidden;
}
.tab-item:hover .tab-close,
.tab-item.is-active .tab-close {
  visibility: visible;
}
.tab-close:hover {
  background-color: rgba(0, 0, 0, 0.08);
  color: #909399;
}
.tab-item.is-active .tab-close {
  color: #6f9cc9;
}
.tab-item.is-active .tab-close:hover {
  background-color: rgba(23, 81, 140, 0.12);
  color: #17518c;
}

/* ========== 个人中心弹窗 ========== */
.profile-form :deep(.el-form-item) {
  margin-bottom: 16px;
}
.profile-form :deep(.el-form-item__label) {
  color: #303133;
}
/* 分区标题：左侧墨蓝竖条 */
.form-section-title {
  margin: 4px 0 14px;
  padding-left: 8px;
  border-left: 3px solid #17518c;
  font-size: 14px;
  font-weight: 600;
  color: #14384a;
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
  position: relative;
  flex: 1;
  min-height: 0;
  /* 内容区用更浅的蓝白，与侧边栏形成层次 */
  background-color: #f7fafd;
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
  background-color: #eef3f7;
  color: #17518c;
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
