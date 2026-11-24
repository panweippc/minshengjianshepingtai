<template>
  <router-view />
</template>

<script setup lang="ts">
import { watch, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

/** 数据大屏为深色全屏画布，不参与后台主题；其余页面统一套用政务蓝主题 */
const applyTheme = (path: string) => {
  document.body.classList.toggle('theme-main', !path.startsWith('/screen'))
}

watch(() => route.path, applyTheme, { immediate: true })

onBeforeUnmount(() => {
  document.body.classList.remove('theme-main')
})
</script>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
}

/* ========== 列表页全局布局优化 ========== */
/* 仅对"纯纵向列表页"（只有查询卡+表格卡，没有左右分栏）生效 */
/* 左右布局页面（.tree-card + .right-area）不受影响 */
/* 数据大屏为全屏画布，不参与列表页 flex 布局规则 */
.main > div:not(.screen-root):not(:has(> .tree-card)):not(:has(> .right-area)) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
/* 查询栏卡片不参与 flex 伸缩 */
.main .query-card,
.main > div:not(:has(> .tree-card)):not(:has(> .right-area)) > .el-card:first-child:not(:has(.el-table)) {
  flex-shrink: 0;
}
/* 包含表格的卡片占据剩余空间，内部 flex 布局（仅纵向列表页） */
.main > div:not(:has(> .tree-card)):not(:has(> .right-area)) > .el-card:has(.el-table) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.main > div:not(:has(> .tree-card)):not(:has(> .right-area)) > .el-card:has(.el-table) > .el-card__body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
/* el-table 占据剩余空间（flex:1 + min-height:0 让表格填充但不撑开父容器） */
.main > div:not(:has(> .tree-card)):not(:has(> .right-area)) .el-card:has(.el-table) .el-table {
  flex: 1;
  min-height: 0;
}
/* el-pagination 固定在底部，不被表格挤出视口 */
.main > div:not(:has(> .tree-card)):not(:has(> .right-area)) .el-card:has(.el-table) .el-pagination {
  flex-shrink: 0;
}

/* ========== 全局弹窗统一样式 ========== */
/* 所有页面弹窗统一采用「施工管理-一级查看」弹窗尺寸：宽 1000px、满屏高度、顶部对齐、内容区独立滚动 */
.el-dialog {
  width: 1000px !important;
  height: 100vh !important;
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  top: 0 !important;
  display: flex !important;
  flex-direction: column !important;
}
.el-dialog__header,
.el-dialog__footer {
  flex: none !important;
}
.el-dialog__body {
  flex: 1 !important;
  min-height: 0 !important;
  max-height: none !important;
  overflow-y: auto !important;
}

/* ========== 全局主题：与侧边栏、首页一致的政务蓝 ========== */
/* 通过 body.theme-main 挂载，数据大屏路由不添加该类，保持原有深色风格 */
body.theme-main {
  /* Element Plus 主色令牌：所有按钮、链接、选中态、开关等自动跟随 */
  --el-color-primary: #22619e;
  --el-color-primary-light-3: #5484b3;
  --el-color-primary-light-5: #8fb0cf;
  --el-color-primary-light-7: #bcd2e6;
  --el-color-primary-light-8: #d3e2f0;
  --el-color-primary-light-9: #eef4fb;
  --el-color-primary-dark-2: #17518c;
  --el-border-radius-base: 6px;
}

/* 卡片：统一圆角、浅蓝描边与柔和投影，与首页卡片同款
   排除首页已定制的指标卡/进度卡，避免覆盖其自身样式 */
body.theme-main .main .el-card:not(.stat-card):not(.progress-card) {
  border: 1px solid #eef4f8;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(31, 95, 168, 0.06);
}

body.theme-main .main .el-card:not(.stat-card):not(.progress-card) > .el-card__header {
  padding: 14px 18px;
  border-bottom: 1px solid #eef4f8;
  font-weight: 600;
  color: #123a5e;
}

body.theme-main .main .el-card.is-always-shadow:not(.stat-card):not(.progress-card),
body.theme-main .main .el-card.is-hover-shadow:hover:not(.stat-card):not(.progress-card) {
  box-shadow: 0 6px 18px rgba(31, 95, 168, 0.1);
}

/* 表格：浅蓝表头、蓝色行悬停，替换默认灰底 */
body.theme-main .main .el-table {
  --el-table-border-color: #edf2f8;
  --el-table-header-bg-color: #f2f7fc;
  --el-table-header-text-color: #3d5568;
  --el-table-row-hover-bg-color: #f5f9fd;
}

body.theme-main .main .el-table th.el-table__cell {
  font-weight: 600;
}

/* 树形控件：选中节点与悬停统一主题浅蓝 */
body.theme-main .el-tree-node__content:hover {
  background-color: #f4f8fd;
}

body.theme-main .el-tree-node.is-current > .el-tree-node__content {
  background-color: #e8f1fb;
  color: #17518c;
}

/* 主色按钮：使用浅一档的蓝渐变，避免整屏按钮颜色偏深 */
body.theme-main .el-button--primary:not(.is-plain):not(.is-text):not(.is-link):not(.is-disabled) {
  background-image: linear-gradient(135deg, #4a8ac6 0%, #2b6cab 100%);
  border-color: transparent;
}

body.theme-main .el-button--primary:not(.is-plain):not(.is-text):not(.is-link):not(.is-disabled):hover,
body.theme-main .el-button--primary:not(.is-plain):not(.is-text):not(.is-link):not(.is-disabled):focus {
  background-image: linear-gradient(135deg, #5696d0 0%, #3378b8 100%);
}

body.theme-main .el-button--primary:not(.is-plain):not(.is-text):not(.is-link):not(.is-disabled):active {
  background-image: linear-gradient(135deg, #3c7cb8 0%, #22619e 100%);
}

/* 文字/链接型主色按钮同步提亮，保持与填充按钮同一色相 */
body.theme-main .el-button--primary.is-link,
body.theme-main .el-button--primary.is-text {
  --el-button-text-color: #2b6cab;
  --el-button-hover-link-text-color: #4a8ac6;
}

/* 查询表单、分页、页签等容器统一留白与字号 */
body.theme-main .main .el-pagination {
  padding: 12px 0 2px;
  justify-content: flex-end;
}

body.theme-main .main .el-tabs__item.is-active {
  color: #17518c;
}

body.theme-main .main .el-tabs__active-bar {
  background-color: #22619e;
}
</style>
