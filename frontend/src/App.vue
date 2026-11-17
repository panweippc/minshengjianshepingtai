<template>
  <router-view />
</template>

<script setup lang="ts">
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
.main > div:not(:has(> .tree-card)):not(:has(> .right-area)) {
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
</style>
