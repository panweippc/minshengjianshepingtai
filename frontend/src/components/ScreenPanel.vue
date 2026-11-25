<template>
  <section class="screen-panel" :class="customClass">
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
  /** 自定义样式类，用于单张面板特殊微调（如"某张卡单独调实/调透"） */
  customClass?: string
}>()
</script>

<style scoped>
/* ===== 赛博玻璃面板：深蓝政务底色 + 微光发光细边线 + 玻璃通透 + 网格粒子 ===== */

/* 外层：极细的微光发光描边（4 条边各自独立渐变 + 外发光） */
.screen-panel {
  position: relative;
  display: flex;
  min-height: 0;
  padding: 1px;
  /* 4 边独立发光描边：顶/底偏青光、左/右偏紫光，营造玻璃边缘折射感 */
  border-top: 1px solid rgba(140, 220, 255, 0.35);
  border-right: 1px solid rgba(160, 200, 255, 0.22);
  border-bottom: 1px solid rgba(140, 220, 255, 0.28);
  border-left: 1px solid rgba(160, 200, 255, 0.25);
  /* 内层填充：极淡青色，是"玻璃"那层本身，不是黑框 */
  background: linear-gradient(
    135deg,
    rgba(70, 160, 240, 0.12) 0%,
    rgba(40, 90, 170, 0.06) 50%,
    rgba(70, 160, 240, 0.10) 100%
  );
  clip-path: polygon(18px 0, 100% 0, 100% calc(100% - 18px), calc(100% - 18px) 100%, 0 100%, 0 18px);
  /* 微光外发光：两层低饱和，不刺眼 */
  filter:
    drop-shadow(0 14px 22px rgba(0, 6, 14, 0.55))
    drop-shadow(0 0 5px rgba(80, 180, 255, 0.18))
    drop-shadow(0 0 18px rgba(80, 180, 255, 0.06));
}

/* 内层：毛玻璃通透 + 深蓝底色 + 柔光内发光 */
.panel-inner {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  /* 玻璃底色：半透明深蓝 + 毛玻璃 blur */
  background:
    radial-gradient(ellipse 90% 60% at 12% -12%, rgba(56, 132, 200, 0.16) 0%, rgba(10, 22, 36, 0) 62%),
    linear-gradient(180deg, rgba(8, 22, 42, 0.62) 0%, rgba(5, 14, 28, 0.66) 45%, rgba(3, 9, 18, 0.64) 100%);
  /* 毛玻璃通透：极淡 blur，让面板"透"但不糊 */
  backdrop-filter: blur(2px) saturate(110%);
  -webkit-backdrop-filter: blur(2px) saturate(110%);
  /* 柔光内发光：顶边青光 + 极淡粒子光 */
  box-shadow:
    inset 0 1px 0 rgba(180, 230, 255, 0.20),
    inset 0 -1px 0 rgba(100, 160, 220, 0.06),
    inset 0 0 30px rgba(60, 140, 220, 0.04),
    inset 0 -26px 40px rgba(0, 0, 0, 0.28);
  clip-path: polygon(17.6px 0, 100% 0, 100% calc(100% - 17.6px), calc(100% - 17.6px) 100%, 0 100%, 0 17.6px);
}

/* 淡网格底 + 粒子流动层（数据流淌感） */
.panel-inner::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  /* 1) 淡网格：0.5px 极细线 + opacity 极低 */
  background-image:
    linear-gradient(rgba(100, 190, 255, 0.06) 0.5px, transparent 0.5px),
    linear-gradient(90deg, rgba(100, 190, 255, 0.06) 0.5px, transparent 0.5px);
  background-size: 32px 32px;
  /* 2) 粒子点：用 radial-gradient 一次性画 3 个点（左上 / 右下 / 中部） */
  /*    用 CSS 动画平移这个层来让 3 个点同时流动 */
  -webkit-mask-image: radial-gradient(circle at 30% 40%, #000 1.5px, transparent 2px),
                      radial-gradient(circle at 70% 65%, #000 1px, transparent 1.5px),
                      radial-gradient(circle at 55% 30%, #000 1px, transparent 1.5px);
          mask-image: radial-gradient(circle at 30% 40%, #000 1.5px, transparent 2px),
                      radial-gradient(circle at 70% 65%, #000 1px, transparent 1.5px),
                      radial-gradient(circle at 55% 30%, #000 1px, transparent 1.5px);
  -webkit-mask-size: 100% 100%, 100% 100%, 100% 100%;
          mask-size: 100% 100%, 100% 100%, 100% 100%;
  -webkit-mask-repeat: no-repeat, no-repeat, no-repeat;
          mask-repeat: no-repeat, no-repeat, no-repeat;
  animation: panel-data-flow 14s linear infinite;
  opacity: 0.55;
}

/* 右上角淡光辉 + 底部微弱反光（保留） */
.panel-inner::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(ellipse 42% 34% at 100% 0%, rgba(96, 190, 255, 0.14) 0%, transparent 70%),
    linear-gradient(0deg, rgba(58, 150, 230, 0.05) 0%, transparent 22%);
  mix-blend-mode: screen;
  z-index: 0;
}

/* 整条标题栏：霓虹细边框风格 */
.panel-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  height: 40px;
  padding: 0 14px;
  /* 低饱和蓝青渐变，保持科技感但不刺眼 */
  background: linear-gradient(90deg, rgba(24, 74, 118, 0.62) 0%, rgba(18, 50, 84, 0.38) 50%, rgba(10, 26, 44, 0.04) 100%);
  border-bottom: 1px solid rgba(96, 184, 244, 0.22);
  box-shadow:
    inset 0 1px 0 rgba(150, 224, 255, 0.14),
    inset 0 -8px 16px rgba(0, 10, 22, 0.22);
}

/* 标题栏左侧霓虹光条：细一点 + 扫光动画 */
.panel-head::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 2px;
  /* 细霓虹条：中心亮 + 两端渐暗 */
  background: linear-gradient(180deg,
    rgba(155, 232, 255, 0) 0%,
    rgba(155, 232, 255, 0.9) 40%,
    rgba(100, 200, 255, 0.95) 60%,
    rgba(155, 232, 255, 0) 100%
  );
  box-shadow: 0 0 8px rgba(80, 190, 255, 0.6), 0 0 18px rgba(80, 190, 255, 0.25);
  /* 扫光感：偶尔闪一下 */
  animation: panel-glow-flicker 4s ease-in-out infinite;
}

/* 扫光条：干脆去掉。之前的 panel-sweep 每 6s 闪一次太快太刺眼 */
.panel-head::after {
  display: none;
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
  color: #9fdfff;
  filter: drop-shadow(0 0 4px rgba(100, 200, 255, 0.55));
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
  text-shadow: 0 0 8px rgba(120, 210, 255, 0.22);
}

.panel-badge {
  flex-shrink: 0;
  padding: 1px 8px;
  border: 1px solid rgba(120, 200, 240, 0.32);
  border-radius: 2px;
  font-size: 10px;
  letter-spacing: 1px;
  color: #bfeaff;
  background: rgba(20, 66, 100, 0.28);
}

.panel-body {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  padding: 10px 14px 12px;
}

/* ===== 动画 ===== */
/* 已废弃：旧的 panel-sweep 太快 */
@keyframes panel-sweep {
  0%, 62% { left: -35%; opacity: 0; }
  68%     { left: -28%; opacity: 1; }
  82%     { left: 130%; opacity: 1; }
  100%    { left: 135%; opacity: 0; }
}

/* 左侧霓虹条偶尔闪一下，像呼吸 */
@keyframes panel-glow-flicker {
  0%, 85%, 100% { opacity: 1; }
  88%           { opacity: 0.55; }
  91%           { opacity: 1; }
  94%           { opacity: 0.7; }
}

/* 面板内数据流淌：淡网格 + 3 个粒子点 14s 轻微横向漂移 */
@keyframes panel-data-flow {
  0%   { transform: translate(0, 0); }
  50%  { transform: translate(-6px, 3px); }
  100% { transform: translate(0, 0); }
}
</style>
