<template>
  <section class="screen-panel">
    <div class="panel-inner">
      <header class="panel-head">
        <div class="panel-title-box">
          <span class="panel-icon" aria-hidden="true">
            <svg viewBox="0 0 14 14" width="14" height="14">
              <rect x="2" y="2" width="10" height="10" rx="2" fill="none" stroke="currentColor" stroke-width="1.2" />
              <path d="M5.2 8.8V5.2h3.6" fill="none" stroke="currentColor" stroke-width="1.2"
                stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </span>
          <h3 class="panel-title">{{ title }}</h3>
        </div>
        <span v-if="badge" class="panel-badge">{{ badge }}</span>
      </header>
      <div class="panel-body">
        <slot />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
/** 数据大屏通用面板：切角卡片 + 整条标题栏 + 内容插槽 */
defineProps<{
  title: string
  badge?: string
}>()
</script>

<style scoped>
/* 立体玻璃卡片：外层渐变描边 + 内层玻璃底 + 顶部高光 */
.screen-panel {
  position: relative;
  display: flex;
  min-height: 0;
  padding: 1px;
  background: linear-gradient(
    150deg,
    rgba(122, 212, 255, 0.7) 0%,
    rgba(48, 120, 186, 0.2) 26%,
    rgba(30, 84, 140, 0.16) 70%,
    rgba(110, 205, 255, 0.55) 100%
  );
  clip-path: polygon(18px 0, 100% 0, 100% calc(100% - 18px), calc(100% - 18px) 100%, 0 100%, 0 18px);
  filter:
    drop-shadow(0 14px 22px rgba(0, 6, 14, 0.55))
    drop-shadow(0 0 12px rgba(64, 164, 255, 0.12));
}

.panel-inner {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background:
    radial-gradient(ellipse 90% 60% at 12% -12%, rgba(56, 132, 200, 0.28) 0%, rgba(10, 22, 36, 0) 62%),
    linear-gradient(180deg, rgba(14, 34, 56, 0.97) 0%, rgba(7, 16, 28, 0.98) 45%, rgba(3, 9, 16, 0.99) 100%);
  box-shadow:
    inset 0 1px 0 rgba(160, 228, 255, 0.16),
    inset 0 -30px 46px rgba(0, 0, 0, 0.38);
  clip-path: polygon(17.6px 0, 100% 0, 100% calc(100% - 17.6px), calc(100% - 17.6px) 100%, 0 100%, 0 17.6px);
}

/* 右上角光晕 + 底部反光，增强立体层次 */
.panel-inner::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(ellipse 42% 34% at 100% 0%, rgba(96, 190, 255, 0.16) 0%, transparent 70%),
    linear-gradient(0deg, rgba(58, 150, 230, 0.08) 0%, transparent 22%);
}

/* 整条标题栏：青色渐变底 + 顶/底高光 */
.panel-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  height: 40px;
  padding: 0 14px;
  background: linear-gradient(90deg, rgba(24, 74, 118, 0.92) 0%, rgba(18, 50, 84, 0.55) 50%, rgba(10, 26, 44, 0.06) 100%);
  border-bottom: 1px solid rgba(96, 184, 244, 0.3);
  box-shadow:
    inset 0 1px 0 rgba(150, 224, 255, 0.2),
    inset 0 -10px 20px rgba(0, 10, 22, 0.28);
}

/* 标题栏左侧竖向光条 */
.panel-head::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  bottom: 9px;
  width: 3px;
  background: linear-gradient(180deg, #9be8ff, #2f9cff);
  box-shadow: 0 0 10px rgba(80, 190, 255, 0.8);
}

.panel-title-box {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex-shrink: 0;
}

.panel-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #8fdcff;
  filter: drop-shadow(0 0 6px rgba(90, 190, 255, 0.85));
}

.panel-title {
  flex-shrink: 0;
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.6px;
  white-space: nowrap;
  color: #ffffff;
  text-shadow: 0 0 12px rgba(120, 210, 255, 0.4);
}

.panel-badge {
  flex-shrink: 0;
  padding: 1px 8px;
  border: 1px solid rgba(120, 200, 240, 0.45);
  border-radius: 2px;
  font-size: 10px;
  letter-spacing: 1px;
  color: #bfeaff;
  background: rgba(20, 66, 100, 0.35);
}

.panel-body {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  padding: 10px 14px 12px;
}
</style>
