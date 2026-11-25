<template>
  <div
    ref="rootRef"
    class="screen-root"
    :class="{ 'is-standalone': standalone }"
    v-loading="loading"
    element-loading-background="rgba(3, 10, 20, 0.72)"
  >
    <div class="screen-canvas" :style="canvasStyle">
      <!-- ========== 顶部标题栏 ========== -->
      <header class="screen-header">
        <div class="header-side">
          <span class="status-chip"><i class="chip-dot"></i>系统在线</span>
          <span class="status-chip">
            <el-icon><OfficeBuilding /></el-icon>{{ kpi.villageCount }} 个行政村
          </span>
          <span class="status-chip">
            <el-icon><CircleCheck /></el-icon>数据正常
          </span>
        </div>
        <div class="header-center">
          <div class="header-plate">
            <h1 class="header-title">民生建设项目管理平台</h1>
          </div>
        </div>
        <div class="header-side header-right">
          <div class="clock">
            <span class="clock-date">{{ dateText }}</span>
            <span class="clock-time">{{ timeText }}</span>
          </div>
          <button v-if="!standalone" class="exit-btn" @click="openCast">
            <el-icon><Monitor /></el-icon>投屏
          </button>
        </div>
      </header>

      <!-- ========== 主体三栏 ========== -->
      <main class="screen-body">
        <!-- 左栏 -->
        <section class="col col-left">
          <ScreenPanel title="各村项目分布">
            <div class="village-map">
              <div class="map-caption">
                <span class="map-town">{{ regionText }}</span>
              </div>
              <div class="map-ring map-ring-outer"></div>
              <div class="map-ring map-ring-inner"></div>
              <svg class="map-links" viewBox="0 0 100 100" preserveAspectRatio="none">
                <line
                  v-for="node in mapNodes"
                  :key="`link-${node.name}`"
                  :x1="MAP_CENTER.x"
                  :y1="MAP_CENTER.y"
                  :x2="node.x"
                  :y2="node.y"
                />
              </svg>
              <div
                v-for="node in mapNodes"
                :key="node.name"
                class="map-node"
                :style="{
                  left: `${node.x}%`,
                  top: `${node.y}%`,
                  '--node-size': `${node.size}px`,
                  '--node-color': node.color
                }"
              >
                <i class="map-node-core"></i>
                <span class="map-node-label">
                  {{ node.name }}
                  <em>{{ node.projectCount }} 个项目</em>
                </span>
              </div>
              <p v-if="!mapNodes.length" class="map-empty">暂无村庄数据</p>
            </div>
          </ScreenPanel>

          <ScreenPanel title="资金完成率">
            <div class="fund-gauge">
              <div class="fund-gauge__orb">
                <div class="fund-gauge__well">
                  <div class="fund-gauge__liquid" :style="{ height: `${fundLevel}%` }">
                    <!-- 液面波纹：浅绿浪层 + 绿色浪体 + 白色浪尖，三层错峰缓慢流动 -->
                    <svg
                      class="fund-gauge__wave fund-gauge__wave--mint"
                      viewBox="0 0 600 40"
                      preserveAspectRatio="none"
                      aria-hidden="true"
                    >
                      <path :d="FUND_WAVE_PATH" />
                    </svg>
                    <svg
                      class="fund-gauge__wave fund-gauge__wave--body"
                      viewBox="0 0 600 40"
                      preserveAspectRatio="none"
                      aria-hidden="true"
                    >
                      <path :d="FUND_WAVE_PATH" />
                    </svg>
                  </div>
                  <div class="fund-gauge__value">
                    <b>{{ kpi.payRate.toFixed(1) }}<em>%</em></b>
                  </div>
                </div>
              </div>
            </div>
          </ScreenPanel>

          <ScreenPanel title="物资状态">
            <div v-if="materialStats.length" class="material-viewport">
              <div class="material-track" :class="{ scrolling: materialScrollable }" :style="materialScrollStyle">
                <ul v-for="copy in materialScrollable ? 2 : 1" :key="copy" class="material-list" :aria-hidden="copy === 2">
                  <li v-for="item in materialStats" :key="item.itemName" class="material-item">
                    <span class="material-name" :title="item.itemName">{{ item.itemName }}</span>
                    <span class="material-stock">
                      <b :class="{ danger: item.warning }">{{ item.stockQty }}</b>
                      {{ item.unit || '件' }}
                    </span>
                    <span class="material-warn">预警 {{ item.warningQty }}</span>
                    <i class="material-flag" :class="{ danger: item.warning }"></i>
                  </li>
                </ul>
              </div>
            </div>
            <p v-else class="panel-empty">暂无物资数据</p>
          </ScreenPanel>
        </section>

        <!-- 中栏 -->
        <section class="col col-center">
          <div class="kpi-row">
            <div
              v-for="(item, index) in kpiCards"
              :key="item.label"
              class="kpi-card"
              :class="[
                `kpi-tone-${item.tone}`,
                index % 2 === 0 ? 'kpi-card--back' : 'kpi-card--front'
              ]"
            >
              <div class="kpi-float">
                <!-- 底部悬浮光晕 -->
                <span class="kpi-halo" aria-hidden="true"></span>
                <!-- 椭圆全息扫描环 -->
                <span class="kpi-rings" aria-hidden="true">
                  <i></i>
                  <i></i>
                  <i></i>
                </span>
                <!-- 玻璃胶囊本体 -->
                <div class="kpi-glass">
                  <span class="kpi-value">{{ item.value }}<em>{{ item.unit }}</em></span>
                  <span class="kpi-label">{{ item.label }}</span>
                </div>
              </div>
            </div>
          </div>

          <ScreenPanel title="各村项目类型明细统计">
            <div class="detail-stage">
              <!-- 自定义图例：上排线型=项目类型，下排颜色=明细类别(单位) -->
              <div v-if="detailHasData" class="detail-legend">
                <div class="detail-legend__row">
                  <span
                    v-for="(label, i) in detailTypeLabels"
                    :key="`dtl-${label}`"
                    class="detail-legend__item"
                  >
                    <i
                      class="detail-legend__line"
                      :style="{ borderTopStyle: DETAIL_TYPE_LINE_STYLES[i % DETAIL_TYPE_LINE_STYLES.length] }"
                    ></i>
                    <em>{{ label }}</em>
                  </span>
                </div>
                <div class="detail-legend__row">
                  <span class="detail-legend__item">
                    <i
                      class="detail-legend__dot"
                      :style="{
                        background: DETAIL_CATEGORY_COLORS.household,
                        boxShadow: `0 0 6px ${DETAIL_CATEGORY_COLORS.household}`
                      }"
                    ></i>
                    <em>{{ DETAIL_CATEGORY_LABELS.household }}（{{ DETAIL_CATEGORY_UNITS.household }}）</em>
                  </span>
                  <span class="detail-legend__item">
                    <i
                      class="detail-legend__dot"
                      :style="{
                        background: DETAIL_CATEGORY_COLORS.facility,
                        boxShadow: `0 0 6px ${DETAIL_CATEGORY_COLORS.facility}`
                      }"
                    ></i>
                    <em>{{ DETAIL_CATEGORY_LABELS.facility }}（{{ DETAIL_CATEGORY_UNITS.facility }}）</em>
                  </span>
                </div>
              </div>
              <div ref="detailRef" class="chart"></div>
              <p v-if="!detailHasData" class="panel-empty detail-empty">暂无明细数据</p>
            </div>
          </ScreenPanel>

          <ScreenPanel title="各村项目类型分布">
            <div class="bar-stage">
              <div ref="barRef" class="chart"></div>
              <!-- 项目类型颜色标注：竖向排在图表右侧 -->
              <div class="type-legend">
                <span
                  v-for="(item, i) in typeSeriesNames"
                  :key="item.name"
                  class="type-legend-item"
                >
                  <i
                    :style="{
                      background: TYPE_LEGEND_COLORS[i % TYPE_LEGEND_COLORS.length],
                      boxShadow: `0 0 6px ${TYPE_LEGEND_COLORS[i % TYPE_LEGEND_COLORS.length]}`
                    }"
                  ></i>
                  <em>{{ item.name }}</em>
                </span>
              </div>
            </div>
          </ScreenPanel>
        </section>

        <!-- 右栏 -->
        <section class="col col-right">
          <ScreenPanel title="核心指标">
            <div class="metric-grid">
              <div v-for="(item, index) in metricItems" :key="item.label" class="metric-item">
                <!-- ★ 指标值 + 标签：独立定位在光束中间，z-index 最高 -->
                <div
                  class="metric-readout"
                  :style="{
                    '--metric-accent': METRIC_ACCENT_COLORS[index % METRIC_ACCENT_COLORS.length]
                  }"
                >
                  <span class="metric-value">
                    {{ formatNumber(metricDisplay[index] ?? 0) }}<em>{{ item.unit }}</em>
                  </span>
                  <span class="metric-label">{{ item.label }}</span>
                </div>

                <!-- ★ 菱形底座 + 发散光束 = 同一张 SVG（viewBox 100×80：光束短 + 菱形占比大） -->
                <span class="metric-stage" aria-hidden="true">
                  <svg viewBox="0 0 100 80" preserveAspectRatio="xMidYMid meet">
                    <defs>
                      <!-- 菱形渐变 -->
                      <linearGradient id="dp-top" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stop-color="#b4e6ff" />
                        <stop offset="50%" stop-color="#4aa8ea" />
                        <stop offset="100%" stop-color="#1e6ac0" />
                      </linearGradient>
                      <linearGradient id="dp-bottom" x1="0%" y1="0%" x2="0%" y2="100%">
                        <stop offset="0%" stop-color="#3ba3e6" />
                        <stop offset="100%" stop-color="#0d3d80" />
                      </linearGradient>
                      <linearGradient id="dp-edge" x1="0%" y1="0%" x2="0%" y2="100%">
                        <stop offset="0%" stop-color="#e8f8ff" />
                        <stop offset="100%" stop-color="#8ad3f5" />
                      </linearGradient>

                      <!-- 光束渐变：整体压低亮度，避免形成白雾遮挡文字 -->
                      <linearGradient id="mb-halo" x1="0%" y1="100%" x2="0%" y2="0%">
                        <stop offset="0%" stop-color="rgba(130,230,255,0.16)" />
                        <stop offset="100%" stop-color="rgba(130,230,255,0)" />
                      </linearGradient>
                      <linearGradient id="mb-beam" x1="0%" y1="100%" x2="0%" y2="0%">
                        <stop offset="0%" stop-color="rgba(160,245,255,0.26)" />
                        <stop offset="30%" stop-color="rgba(120,220,255,0.11)" />
                        <stop offset="100%" stop-color="rgba(80,170,255,0)" />
                      </linearGradient>
                      <linearGradient id="mb-core" x1="0%" y1="100%" x2="0%" y2="0%">
                        <stop offset="0%" stop-color="rgba(220,250,255,0.22)" />
                        <stop offset="100%" stop-color="rgba(130,210,255,0)" />
                      </linearGradient>

                      <filter id="mb-blur-halo" x="-40%" y="0" width="180%" height="100%">
                        <feGaussianBlur stdDeviation="3.5" />
                      </filter>
                      <filter id="mb-blur-beam" x="-20%" y="0" width="140%" height="100%">
                        <feGaussianBlur stdDeviation="2.5" />
                      </filter>
                      <filter id="mb-blur-core" x="-10%" y="0" width="120%" height="100%">
                        <feGaussianBlur stdDeviation="1" />
                      </filter>
                    </defs>

                    <!-- ====== 发散光束层：底边全部从菱形顶面 V 形边缘开始（不是从外边溢出） ====== -->

                    <!-- 第1层：外扩柔光，底边 V 形 = 菱形顶面（从边缘开始，不是从外面） -->
                    <g filter="url(#mb-blur-halo)">
                      <path
                        d="M 20,55
                           L 50,49
                           L 80,55
                           Q 98,22 100,0
                           Q 50,0 0,0
                           Q 2,22 20,55 Z"
                        fill="url(#mb-halo)"
                      />
                    </g>

                    <!-- 第2层：主光束，底边 V 形 = 菱形顶面 -->
                    <g filter="url(#mb-blur-beam)">
                      <path
                        d="M 20,55
                           L 50,49
                           L 80,55
                           Q 97,25 100,0
                           Q 50,0 0,0
                           Q 3,25 20,55 Z"
                        fill="url(#mb-beam)"
                      />
                    </g>

                    <!-- 第3层：中心亮柱（向外发散版！底边窄 34~66=32 → 顶部宽 28~72=44，发散率 1.38x） -->
                    <g filter="url(#mb-blur-core)">
                      <path
                        d="M 34,53
                           L 50,49
                           L 66,53
                           Q 72,25 72,0
                           Q 50,0 28,0
                           Q 28,25 34,53 Z"
                        fill="url(#mb-core)"
                      />
                    </g>

                    <!-- ====== 菱形底座层：下层大一圈（台基）、上层小（顶面），两层贴近 ====== -->

                    <!-- 下层台基菱形：比上层大一圈（宽差 12 单位），Y 轴错开 5 单位 = 贴合承托 -->
                    <polygon
                      points="50,54 88,60 50,72 12,60"
                      fill="url(#dp-bottom)"
                      style="filter: drop-shadow(0 0 4px rgba(80,180,255,0.5));"
                    />
                    <polygon
                      points="50,54 88,60 50,72 12,60 50,54"
                      fill="none"
                      stroke="rgba(150,220,255,0.55)"
                      stroke-width="0.9"
                    />

                    <!-- 上层顶面菱形：比下层小一圈（宽差 12 单位），紧贴台基 -->
                    <polygon
                      points="50,49 82,55 50,61 18,55"
                      fill="url(#dp-top)"
                      style="filter: drop-shadow(0 0 4px rgba(140,230,255,0.8)) drop-shadow(0 0 9px rgba(60,160,255,0.4));"
                    />
                    <polygon
                      points="50,49 82,55 50,61 18,55 50,49"
                      fill="none"
                      stroke="url(#dp-edge)"
                      stroke-width="1.2"
                    />
                  </svg>
                </span>
              </div>
            </div>
          </ScreenPanel>

          <ScreenPanel title="项目状态分布">
            <div class="donut-stage">
              <!-- 轨道层：纯 SVG 绘制，三层同心扁椭圆（X 轴拉伸、Y 轴压缩，模拟俯视透视） -->
              <svg class="donut-svg" viewBox="0 0 440 290" preserveAspectRatio="none" aria-hidden="true">
                <defs>
                  <!-- 中心径向蓝光光晕 -->
                  <radialGradient id="dn-halo-grad" cx="50%" cy="50%" r="50%">
                    <stop offset="0%" stop-color="#35d8ff" stop-opacity="0.07" />
                    <stop offset="45%" stop-color="#35d8ff" stop-opacity="0.02" />
                    <stop offset="100%" stop-color="#35d8ff" stop-opacity="0" />
                  </radialGradient>
                  <!-- 台面边缘渐变描边：上暗下亮再回亮，形成流光底色 -->
                  <linearGradient id="dn-inner-grad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#a8f0ff" stop-opacity="0.95" />
                    <stop offset="48%" stop-color="#35a8ff" stop-opacity="0.6" />
                    <stop offset="100%" stop-color="#a8f0ff" stop-opacity="0.95" />
                  </linearGradient>
                  <!-- 中环渐变描边：整体提亮，避免与流光弧明暗差过大 -->
                  <linearGradient id="dn-mid-grad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#6fd2ff" stop-opacity="0.78" />
                    <stop offset="50%" stop-color="#4aaef0" stop-opacity="0.58" />
                    <stop offset="100%" stop-color="#6fd2ff" stop-opacity="0.78" />
                  </linearGradient>
                  <!-- 玻璃顶面：参考柱状玻璃管，低填充高透光，靠边缘亮青描边和辉光成形 -->
                  <radialGradient id="dn-platform-top-grad" cx="40%" cy="26%" r="82%">
                    <stop offset="0%" stop-color="#7fe0ff" stop-opacity="0.14" />
                    <stop offset="40%" stop-color="#45b8f0" stop-opacity="0.18" />
                    <stop offset="78%" stop-color="#3fb0f5" stop-opacity="0.3" />
                    <stop offset="100%" stop-color="#8feaff" stop-opacity="0.42" />
                  </radialGradient>
                  <!-- 镜面高光：细窄反光条，模拟玻璃管壁的反光 -->
                  <linearGradient id="dn-platform-gloss-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="#ffffff" stop-opacity="0.32" />
                    <stop offset="16%" stop-color="#dff8ff" stop-opacity="0.05" />
                    <stop offset="100%" stop-color="#bcefff" stop-opacity="0" />
                  </linearGradient>
                  <linearGradient id="dn-platform-side-grad" x1="0%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" stop-color="#8feaff" stop-opacity="0.42" />
                    <stop offset="45%" stop-color="#3f9fd8" stop-opacity="0.2" />
                    <stop offset="100%" stop-color="#8feaff" stop-opacity="0.48" />
                  </linearGradient>
                  <radialGradient id="dn-platform-shadow-grad" cx="50%" cy="50%" r="50%">
                    <stop offset="0%" stop-color="#010913" stop-opacity="0.82" />
                    <stop offset="64%" stop-color="#010913" stop-opacity="0.44" />
                    <stop offset="100%" stop-color="#010913" stop-opacity="0" />
                  </radialGradient>
                  <filter id="dn-platform-shadow-blur" x="-30%" y="-50%" width="160%" height="200%">
                    <feGaussianBlur stdDeviation="5" />
                  </filter>
                </defs>

                <!-- 中心光晕（向上浮起，作为视觉最高点底层） -->
                <ellipse class="dn-halo" cx="220" cy="140" rx="92" ry="54" fill="url(#dn-halo-grad)" />

                <!-- 外层：最淡、虚线弱化 -->
                <ellipse class="dn-orbit dn-orbit-outer" cx="220" cy="151" rx="146" ry="82" />
                <!-- 中层：淡蓝色高亮轨道 -->
                <ellipse class="dn-orbit dn-orbit-mid" cx="220" cy="151" rx="110" ry="62" />
                <!-- 中圈流动亮弧：拖尾层先绘制，头部亮弧叠加其上 -->
                <ellipse class="dn-orbit-flow-trail" cx="220" cy="151" rx="110" ry="62" />
                <ellipse class="dn-orbit-flow" cx="220" cy="151" rx="110" ry="62" />

                <!-- 中心实心圆台：完整椭圆顶面下移 22 单位形成侧壁，项目数字置于顶面 -->
                <g class="dn-platform">
                  <ellipse
                    class="dn-platform-shadow"
                    cx="220"
                    cy="170"
                    rx="72"
                    ry="38"
                    fill="url(#dn-platform-shadow-grad)"
                    filter="url(#dn-platform-shadow-blur)"
                  />
                  <!-- 前侧外壁：仅绘制朝向观察者的下半圈，避免遮挡顶面 -->
                  <path
                    class="dn-platform-outer-wall"
                    d="M160 140 C160 158.225 186.863 173 220 173 C253.137 173 280 158.225 280 140 L280 162 C280 180.225 253.137 195 220 195 C186.863 195 160 180.225 160 162 Z"
                  />
                  <path
                    class="dn-platform-bottom-rim"
                    d="M160 162 C160 180.225 186.863 195 220 195 C253.137 195 280 180.225 280 162"
                  />
                  <!-- 顶面：完整椭圆低透明填充，上方叠加边缘清晰的玻璃反射高光 -->
                  <path
                    class="dn-platform-top"
                    d="M160 140 C160 121.775 186.863 107 220 107 C253.137 107 280 121.775 280 140 C280 158.225 253.137 173 220 173 C186.863 173 160 158.225 160 140 Z"
                  />
                  <path
                    class="dn-platform-gloss"
                    d="M174 129 C184 115 200 109 219 108 C237 107 253 112 264 120 C246 115 229 116 212 119 C196 122 184 126 174 129 Z"
                  />
                  <ellipse class="dn-platform-edge dn-platform-edge-outer" cx="220" cy="140" rx="60" ry="33" />
                </g>

                <!-- 状态节点光点：有数据亮 + 外发光，数值 0 弱化 -->
                <g v-for="item in donutNodes" :key="item.name">
                  <circle
                    class="dn-dot-halo"
                    :cx="item.x"
                    :cy="item.y"
                    r="7.5"
                    :fill="item.color"
                    :opacity="item.value ? 0.3 : 0.08"
                  />
                  <circle
                    class="dn-dot-core"
                    :cx="item.x"
                    :cy="item.y"
                    r="3.6"
                    :fill="item.color"
                    :opacity="item.value ? 1 : 0.35"
                    :style="item.value ? { filter: `drop-shadow(0 0 5px ${item.color})` } : undefined"
                  />
                </g>
              </svg>

              <!-- 中心：大号高亮数字 + 副标题 -->
              <div class="dn-center">
                <b>{{ donutTotal }}</b>
                <span>项目总数</span>
              </div>

              <!-- 状态文字标签：保持正向可读，位于光点外侧，同色系 -->
              <div
                v-for="item in donutNodes"
                :key="`label-${item.name}`"
                class="dn-label"
                :class="{ 'is-left': item.left, 'is-zero': !item.value }"
                :style="{ left: `${item.labelX}%`, top: `${item.labelY}%` }"
              >
                <span class="dn-label-name" :style="{ color: item.color }">{{ item.name }}</span>
                <b class="dn-label-value" :style="{ color: item.color }">{{ item.value }}</b>
              </div>
            </div>
          </ScreenPanel>

          <ScreenPanel title="实时事件">
            <div v-if="eventStream.length" class="event-viewport">
              <div class="event-track" :class="{ scrolling: eventScrollable }" :style="eventScrollStyle">
                <ul v-for="copy in eventScrollable ? 2 : 1" :key="copy" class="event-list" :aria-hidden="copy === 2">
                  <li v-for="item in eventStream" :key="item.id" class="event-item">
                    <i class="event-dot" :style="{ background: eventDotColor(item.category), boxShadow: `0 0 8px ${eventDotColor(item.category)}` }"></i>
                    <div class="event-main">
                      <span class="event-text" :title="item.summary || item.operation">{{ item.summary || item.operation }}</span>
                    </div>
                    <span class="event-time">{{ formatRelativeTime(item.operTime) }}</span>
                  </li>
                </ul>
              </div>
            </div>
            <p v-else class="panel-empty">暂无业务事件</p>
          </ScreenPanel>
        </section>
      </main>

      <!-- ========== 底部实时数据流 ========== -->
      <footer class="screen-ticker">
        <span class="ticker-label">综合数据流</span>
        <div class="ticker-viewport">
          <div class="ticker-track">
            <span v-for="(item, index) in tickerItems" :key="`a-${index}`" class="ticker-item">
              <em></em>{{ item.label }}<b>{{ item.value }}</b>
            </span>
            <span v-for="(item, index) in tickerItems" :key="`b-${index}`" class="ticker-item">
              <em></em>{{ item.label }}<b>{{ item.value }}</b>
            </span>
          </div>
        </div>
        <span class="ticker-update">更新 {{ overview?.updateTime?.slice(11) || '--:--:--' }}</span>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import ScreenPanel from '@/components/ScreenPanel.vue'
import { getScreenOverview } from '@/api/dashboard'
import type {
  ScreenChartItemVO,
  ScreenEventVO,
  ScreenMaterialVO,
  ScreenOverviewVO
} from '@/api/dashboard'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

/** 是否投屏独立页（全屏展示，不显示投屏按钮） */
const standalone = computed(() => route.meta.standalone === true)

/** 设计稿宽度：整体按屏幕宽度等比缩放，高度按屏幕比例自动延展 */
const DESIGN_WIDTH = 1920
/** 设计稿基准高度 */
const DESIGN_HEIGHT = 1080
/** 数据自动刷新间隔（毫秒） */
const REFRESH_INTERVAL = 60 * 1000
/** 各村项目分布中心点（百分比坐标） */
const MAP_CENTER = { x: 48, y: 50 }
/** 各村项目分布节点固定布局（百分比坐标，避免每次渲染位置抖动） */
const MAP_POSITIONS = [
  { x: 24, y: 32 },
  { x: 62, y: 24 },
  { x: 78, y: 48 },
  { x: 60, y: 70 },
  { x: 30, y: 66 },
  { x: 44, y: 46 },
  { x: 16, y: 52 },
  { x: 74, y: 76 }
]
/** 各村光点配色（RGB 三元组，按节点顺序循环取用，保证各村颜色互不相同） */
const MAP_NODE_COLORS = [
  '38, 224, 164',
  '96, 200, 255',
  '255, 200, 92',
  '255, 128, 158',
  '180, 140, 255',
  '120, 240, 220',
  '255, 160, 100',
  '140, 240, 120'
]
/** 项目状态配色 */
const STATUS_COLORS: Record<string, string> = {
  待审核: '#35d8ff',
  待开工: '#7a8fa6',
  施工中: '#23e6a0',
  待验收: '#ffc857',
  已归档: '#a37bff',
  已驳回: '#ff6b81'
}

const loading = ref(false)
const overview = ref<ScreenOverviewVO | null>(null)
const scale = ref(1)
/** 画布设计高度：随容器高度/缩放比动态计算，保证缩放后正好铺满容器 */
const designHeight = ref(DESIGN_HEIGHT)
const dateText = ref('')
const timeText = ref('')
/** 大屏容器：内嵌模式下按内容区尺寸等比缩放 */
const rootRef = ref<HTMLDivElement>()

const detailRef = ref<HTMLDivElement>()
const barRef = ref<HTMLDivElement>()

let detailChart: ReturnType<typeof echarts.init> | null = null
let barChart: ReturnType<typeof echarts.init> | null = null
let clockTimer: number | undefined
let refreshTimer: number | undefined
let resizeObserver: ResizeObserver | null = null

/** 区划名称（沿用登录用户所属区划） */
const regionText = computed(
  () => store.userInfo?.regionName || store.userInfo?.regionCode || '全域视图'
)

/** 核心指标（空数据兜底，避免模板取值报错） */
const kpi = computed(
  () =>
    overview.value?.kpi ?? {
      totalBudget: 0,
      paidAmount: 0,
      payRate: 0,
      projectCount: 0,
      buildingCount: 0,
      constructionCount: 0,
      villageCount: 0,
      householdCount: 0,
      facilityCount: 0,
      materialTotal: 0,
      materialWarnCount: 0,
      declarationCount: 0
    }
)

/** 项目状态分布 */
const statusStats = computed<ScreenChartItemVO[]>(() => overview.value?.statusStats ?? [])
/** 物资状态 */
const materialStats = computed<ScreenMaterialVO[]>(() => overview.value?.materialStats ?? [])
/** 各村项目类型明细统计（横轴村庄，系列为 项目类型×农户/公建；仅立项已通过） */
const villageTypeDetailStats = computed(() => overview.value?.villageTypeDetailStats)
/** 明细图是否有数据（至少一个村） */
const detailHasData = computed(() => !!villageTypeDetailStats.value?.villages?.length)
/** 明细图：项目类型线型（按字典序：常规项目→实线 / 应急抢险→虚线 / 灾后重建→点线） */
const DETAIL_TYPE_LINE_STYLES = ['solid', 'dashed', 'dotted'] as const
/** 明细图：明细类别 → 线条颜色（农户=绿，公建=青） */
const DETAIL_CATEGORY_COLORS: Record<'household' | 'facility', string> = {
  household: '#23e6a0',
  facility: '#35d8ff'
}
/** 明细图：明细类别 → 数值单位（农户=户，公建=处） */
const DETAIL_CATEGORY_UNITS: Record<'household' | 'facility', string> = {
  household: '户',
  facility: '处'
}
/** 明细图：明细类别 → 中文名 */
const DETAIL_CATEGORY_LABELS: Record<'household' | 'facility', string> = {
  household: '农户',
  facility: '公建'
}
/** 明细图：项目类型名称列表（从系列按固定顺序提取：每 2 条取农户系列「·」前半段） */
const detailTypeLabels = computed<string[]>(() => {
  const s = villageTypeDetailStats.value?.series ?? []
  const labels: string[] = []
  for (let i = 0; i < s.length; i += 2) {
    labels.push(s[i].name.split('·')[0])
  }
  return labels
})

/** 物资状态面板可视条数 */
const MATERIAL_VISIBLE_COUNT = 4
/** 物资条数超过可视条数时开启自动滚动 */
const materialScrollable = computed(() => materialStats.value.length > MATERIAL_VISIBLE_COUNT)
/** 滚动周期：按物资条数递增，保证滚动速度稳定 */
const materialScrollStyle = computed(() => ({
  '--scroll-duration': `${Math.max(materialStats.value.length * 2.4, 10)}s`
}))
/** ========== 实时事件（真实业务写操作流） ========== */
/** 事件流本地缓存上限：轮询时新事件前置、旧事件截断，保留流式观感且避免闪烁 */
const EVENT_STREAM_MAX = 12
/** 事件面板可视条数：超过则开启无缝向上滚动 */
const EVENT_VISIBLE_COUNT = 6
/** 事件类别 → 彩色圆点颜色映射（与后端 category 对齐） */
const EVENT_DOT_COLORS: Record<string, string> = {
  declaration: '#5ad8ff',
  approval: '#ffcf5a',
  construction: '#4ade80',
  acceptance: '#a78bfa',
  fund: '#f472b6',
  material: '#fbbf24',
  archive: '#c084fc',
  base: '#38bdf8',
  file: '#22d3ee',
  system: '#94a3b8'
}
const EVENT_DOT_DEFAULT = '#60b8f4'
/** 不展示的模块类别：基础信息管理 / 物资管理 / 系统管理 / 附件管理（后端已过滤，此处兜底清理历史缓存） */
const EVENT_HIDDEN_CATEGORIES = new Set(['base', 'material', 'system', 'file'])
/** 不展示的操作关键词：编辑/查看/查询/修改属于维护类操作，不产生业务推进（后端已过滤，此处兜底清理历史缓存） */
const EVENT_HIDDEN_OPERATIONS = ['编辑', '查看', '查询', '修改']
/** 事件流：跨轮询去重合并，新事件前置 */
const eventStream = ref<ScreenEventVO[]>([])
/** 判断事件是否属于不展示的噪声事件（模块或操作任一命中即隐藏） */
function isHiddenEvent(event: ScreenEventVO): boolean {
  if (EVENT_HIDDEN_CATEGORIES.has(event.category)) return true
  const operation = event.operation ?? ''
  return EVENT_HIDDEN_OPERATIONS.some((keyword) => operation.includes(keyword))
}
/** 事件流去重键：摘要相同视为同一条事件，只保留最新 */
function eventStreamKey(event: ScreenEventVO): string {
  return event.summary?.trim() || event.id
}
/** 合并新拉取的事件：按摘要去重（最新覆盖），新事件前置，按时间倒序，截断到上限 */
function mergeEvents(incoming: ScreenEventVO[]) {
  const map = new Map<string, ScreenEventVO>()
  // 先放缓存，再用最新数据覆盖同摘要条目
  for (const event of eventStream.value) {
    map.set(eventStreamKey(event), event)
  }
  for (const event of incoming ?? []) {
    if (isHiddenEvent(event)) continue
    map.set(eventStreamKey(event), event)
  }
  // 每轮都按模块与操作剪枝，确保已排除模块/操作的历史缓存能立即消失
  const merged = Array.from(map.values()).filter((e) => !isHiddenEvent(e))
  merged.sort((a, b) => (a.operTime < b.operTime ? 1 : -1))
  eventStream.value = merged.slice(0, EVENT_STREAM_MAX)
}
/** 相对时间：刚刚 / X分钟前 / X小时前 / X天前，更早回退 MM-dd HH:mm */
function formatRelativeTime(time: string): string {
  if (!time) return ''
  const t = new Date(time.replace(' ', 'T'))
  if (Number.isNaN(t.getTime())) return time
  const diff = Date.now() - t.getTime()
  if (diff < 0) return '刚刚'
  const sec = Math.floor(diff / 1000)
  if (sec < 60) return '刚刚'
  const min = Math.floor(sec / 60)
  if (min < 60) return `${min}分钟前`
  const hr = Math.floor(min / 60)
  if (hr < 24) return `${hr}小时前`
  const day = Math.floor(hr / 24)
  if (day < 30) return `${day}天前`
  return time.slice(5, 16)
}
/** 取事件圆点颜色 */
function eventDotColor(category: string): string {
  return EVENT_DOT_COLORS[category] || EVENT_DOT_DEFAULT
}
/** 事件条数超过可视条数时开启自动滚动 */
const eventScrollable = computed(() => eventStream.value.length > EVENT_VISIBLE_COUNT)
/** 滚动周期：按事件条数递增，保证滚动速度稳定 */
const eventScrollStyle = computed(() => ({
  '--event-scroll-duration': `${Math.max(eventStream.value.length * 2.2, 12)}s`
}))

/** 各村项目分布节点：位置固定、大小按项目数映射 */
const mapNodes = computed(() => {
  const villages = overview.value?.villageStats ?? []
  return villages.map((village, index) => {
    const position = MAP_POSITIONS[index % MAP_POSITIONS.length]
    return {
      ...village,
      x: position.x,
      y: position.y,
      size: 10 + Math.min(village.projectCount, 6) * 3,
      color: MAP_NODE_COLORS[index % MAP_NODE_COLORS.length]
    }
  })
})

/** 中栏四张 KPI 卡片：主数值 + 主标题 */
const kpiCards = computed(() => {
  const budget = moneyParts(kpi.value.totalBudget)
  const paid = moneyParts(kpi.value.paidAmount)
  return [
    {
      label: '项目总预算',
      value: budget.value,
      unit: budget.unit,
      tone: 'cyan'
    },
    {
      label: '已拨付资金',
      value: paid.value,
      unit: paid.unit,
      tone: 'green'
    },
    {
      label: '拨付完成率',
      value: kpi.value.payRate.toFixed(1),
      unit: '%',
      tone: 'blue'
    },
    {
      label: '在建项目',
      value: String(kpi.value.constructionCount),
      unit: '个',
      tone: 'purple'
    }
  ]
})

/** 右栏核心指标 */
const metricItems = computed(() => [
  { label: '农户档案', value: kpi.value.householdCount, unit: '户' },
  { label: '在管项目', value: kpi.value.buildingCount, unit: '个' },
  { label: '物资预警', value: kpi.value.materialWarnCount, unit: '项' },
  { label: '申报受理', value: kpi.value.declarationCount, unit: '件' }
])

/** 核心指标展示值：数据加载/刷新时递增滚动，避免数字硬切换 */
const metricDisplay = ref<number[]>([0, 0, 0, 0])
let metricRaf = 0
/** 数字滚动动画：900ms 缓出递增到目标值 */
function animateMetricNumbers() {
  const targets = metricItems.value.map((item) => item.value ?? 0)
  const from = metricDisplay.value.slice()
  const startTime = performance.now()
  const DURATION = 900
  window.cancelAnimationFrame(metricRaf)
  const step = (now: number) => {
    const progress = Math.min(1, (now - startTime) / DURATION)
    const eased = 1 - Math.pow(1 - progress, 3)
    metricDisplay.value = targets.map((target, i) => {
      const begin = from[i] ?? 0
      return Math.round(begin + (target - begin) * eased)
    })
    if (progress < 1) metricRaf = window.requestAnimationFrame(step)
  }
  metricRaf = window.requestAnimationFrame(step)
}
watch(metricItems, animateMetricNumbers, { immediate: true })

/** 底部实时数据流 */
const tickerItems = computed(() => {
  const item = kpi.value
  return [
    { label: '立项项目', value: `${item.projectCount} 个` },
    { label: '项目总预算', value: formatMoney(item.totalBudget) },
    { label: '已拨付资金', value: formatMoney(item.paidAmount) },
    { label: '拨付完成率', value: `${item.payRate.toFixed(1)}%` },
    { label: '行政村', value: `${item.villageCount} 个` },
    { label: '农户档案', value: `${item.householdCount} 户` },
    { label: '公共设施', value: `${item.facilityCount} 处` },
    { label: '物资预警', value: `${item.materialWarnCount} 项` }
  ]
})

/** 画布样式：宽度固定设计稿、高度按屏幕比例延展，再整体按屏幕宽度等比缩放铺满 */
const canvasStyle = computed(() => ({
  width: `${DESIGN_WIDTH}px`,
  height: `${designHeight.value}px`,
  transform: `scale(${scale.value})`
}))

/** 项目状态颜色 */
function statusColor(name: string) {
  return STATUS_COLORS[name] ?? '#35d8ff'
}

/** 数字千分位 */
function formatNumber(value: number) {
  return (value ?? 0).toLocaleString('zh-CN')
}

/** 金额拆分：亿/万/元，返回数值与单位用于 KPI 展示 */
function moneyParts(value: number): { value: string; unit: string } {
  const amount = value ?? 0
  if (amount >= 100000000) return { value: (amount / 100000000).toFixed(2), unit: '亿' }
  if (amount >= 10000) return { value: (amount / 10000).toFixed(1), unit: '万' }
  return { value: amount.toFixed(0), unit: '元' }
}

/** 金额格式化 */
function formatMoney(value: number) {
  const parts = moneyParts(value)
  return `${parts.value}${parts.unit}`
}

/** 尺寸自适应：按容器（内嵌=内容区，投屏=窗口）宽度等比缩放，高度延展后铺满整个可视区 */
async function resizeCanvas() {
  const width = rootRef.value?.clientWidth || window.innerWidth
  const height = rootRef.value?.clientHeight || window.innerHeight
  // 以宽度为基准整体缩放，保证字号、图表、间距同步放大，不出现两侧留白
  scale.value = width > 0 ? width / DESIGN_WIDTH : 1
  // 反推设计稿高度：缩放后正好等于容器高度，铺满上下区域
  designHeight.value = height > 0 ? Math.round(height / scale.value) : DESIGN_HEIGHT
  // 画布高度变化后图表容器高度同步变化，需要重算图表尺寸
  await nextTick()
  resizeCharts()
}

/** 重算图表尺寸 */
function resizeCharts() {
  ;[detailChart, barChart].forEach((chart) => chart?.resize())
}

/** 顶部时钟 */
function updateClock() {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  dateText.value = `${now.getFullYear()}.${pad(now.getMonth() + 1)}.${pad(now.getDate())}`
  timeText.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

/** 生成液面波纹路径：等高、等距的规则波峰，可视区约 2 个波峰（间距 200 单位，更疏朗）；
 *  波形每 200 个单位重复，动画平移 200 单位即一个完整周期，循环无缝不跳动 */
function buildWavePath() {
  const halfWidth = 100
  const viewWidth = 600
  const viewHeight = 40
  const baseline = 20
  const amplitude = 15
  const halfCount = viewWidth / halfWidth
  let d = `M0 ${baseline}`
  for (let i = 0; i < halfCount; i++) {
    const start = i * halfWidth
    const end = start + halfWidth
    const y = i % 2 === 0 ? baseline - amplitude : baseline + amplitude
    d += ` C${start + halfWidth / 3} ${y} ${end - halfWidth / 3} ${y} ${end} ${baseline}`
  }
  return `${d} L${viewWidth} ${viewHeight} L0 ${viewHeight} Z`
}

const FUND_WAVE_PATH = buildWavePath()

/** 资金水位：拨付完成率（0~100，决定液面高度） */
const fundLevel = computed(() => Math.max(0, Math.min(100, kpi.value.payRate ?? 0)))

/**
 * 各村项目类型明细统计：每个项目类型拆为「农户(户)」与「公建(处)」两条折线，共 6 条。
 * 颜色按明细类别（农户绿 / 公建青），线型按项目类型（实 / 虚 / 点），
 * 图例 / 线条 / tooltip 三者共用 DETAIL_CATEGORY_* 与 DETAIL_TYPE_LINE_STYLES 常量，保证映射一致。
 * 维度：横轴=村，系列=项目类型×农户/公建，数值=明细数（仅立项已通过）。
 */
function buildDetailOption() {
  const stats = villageTypeDetailStats.value
  const villages = stats?.villages ?? []
  const typeSeries = stats?.series ?? []
  // seriesName → 单位，供 tooltip 按系列拼接正确单位（农户=户，公建=处）
  const unitMap = new Map<string, string>()
  typeSeries.forEach((s) => unitMap.set(s.name, DETAIL_CATEGORY_UNITS[s.category]))

  return {
    backgroundColor: 'transparent',
    grid: { top: 64, left: 44, right: 22, bottom: 32 },
    tooltip: {
      trigger: 'axis' as const,
      backgroundColor: 'rgba(6, 24, 44, 0.96)',
      borderColor: 'rgba(53, 216, 255, 0.45)',
      borderWidth: 1,
      textStyle: { color: '#eaf7ff', fontSize: 12 },
      formatter: (
        params: { axisValue: string; marker: string; seriesName: string; value: number }[]
      ) => {
        if (!params || !params.length) return ''
        const rows = params
          .map((p) => `${p.marker}${p.seriesName}：${p.value}${unitMap.get(p.seriesName) ?? ''}`)
          .join('<br/>')
        return `${params[0].axisValue}<br/>${rows}`
      },
      extraCssText: 'box-shadow: 0 0 20px rgba(53, 216, 255, 0.35);'
    },
    legend: { show: false },
    xAxis: {
      type: 'category' as const,
      boundaryGap: false,
      data: villages,
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#bfe9ff', fontSize: 11, fontWeight: 500 },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value' as const,
      minInterval: 1,
      axisLabel: { color: '#7fa8c6', fontSize: 10 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: {
        show: true,
        lineStyle: { color: 'rgba(84, 150, 200, 0.14)', type: 'dashed' as const }
      }
    },
    series: typeSeries.map((s, i) => {
      const color = DETAIL_CATEGORY_COLORS[s.category]
      const unit = DETAIL_CATEGORY_UNITS[s.category]
      // 系列顺序固定为 类型1农户,类型1公建,类型2农户,类型2公建... → 每 2 条同属一个项目类型
      const typeIndex = Math.floor(i / 2)
      const lineType = DETAIL_TYPE_LINE_STYLES[typeIndex % DETAIL_TYPE_LINE_STYLES.length]
      return {
        name: s.name,
        type: 'line' as const,
        smooth: true,
        showSymbol: true,
        symbolSize: 7,
        z: 4,
        lineStyle: {
          width: 2.5,
          type: lineType,
          color,
          shadowBlur: 12,
          shadowColor: hexToRgba(color, 0.6)
        },
        itemStyle: {
          color,
          borderColor: '#ffffff',
          borderWidth: 1,
          shadowBlur: 8,
          shadowColor: hexToRgba(color, 0.75)
        },
        areaStyle: {
          origin: 'start' as const,
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: hexToRgba(color, 0.18) },
              { offset: 1, color: hexToRgba(color, 0) }
            ]
          }
        },
        // 数值标注：非 0 才显示，带对应单位
        label: {
          show: true,
          position: 'top',
          color,
          fontSize: 10,
          fontWeight: 600,
          formatter: (params: { value: number }) =>
            params.value > 0 ? `${params.value}${unit}` : ''
        },
        data: s.data
      }
    })
  }
}

/** 各村项目类型分布：类型配色（右侧 HTML 图例与柱体分段共用） */
const TYPE_LEGEND_COLORS = ['#00fff7', '#ffc857', '#b07bff']

/** 核心指标：四项指标各自的强调色（数字与标签同色） */
const METRIC_ACCENT_COLORS = ['#4fd8ff', '#ffc857', '#6ef2a8', '#ff9ec4']

/** 各村项目类型分布：类型系列列表（用于渲染右侧颜色标注） */
const typeSeriesNames = computed(() => overview.value?.typeStats?.series ?? [])

/** 十六进制颜色转 rgba 字符串 */
function hexToRgba(hex: string, alpha: number): string {
  const c = hex.replace('#', '')
  const num = parseInt(
    c.length === 3 ? c.split('').map((ch) => ch + ch).join('') : c,
    16
  )
  return `rgba(${(num >> 16) & 0xff}, ${(num >> 8) & 0xff}, ${num & 0xff}, ${alpha})`
}

/** custom 系列渲染函数入参（ECharts 类型过于复杂，此处只约束本图表用到的能力） */
interface BarRenderAPI {
  /** 取当前数据项指定维度的值 */
  value: (dim: number) => number
  /** 数据坐标 → 像素坐标 */
  coord: (data: number[]) => number[]
}

/** 各村项目类型分布：立体玻璃柱 + 发光底座 + 数据虚影 + 透视轨道 */
/** 各村项目类型分布：透明玻璃罩 + 水晶柱 + 多层光环底座 + 透视网格地面 */
function buildBarOption() {
  const stats = overview.value?.typeStats
  const villages = stats?.villages ?? []
  const series = stats?.series ?? []
  // 类型配色与右侧 HTML 图例共用同一常量
  const typeColors = TYPE_LEGEND_COLORS
  const maxVal = Math.max(1, ...series.flatMap((s) => s.data.map((v) => v)))
  const cols = Math.max(1, villages.length)
  // 玻璃罩宽度随类目数自适应
  const glassW = cols <= 4 ? 78 : cols <= 7 ? 58 : 44

  // 每个村的分段数据与总高度
  const segs = villages.map((_, vi) =>
    series.map((s, si) => ({ value: s.data[vi] ?? 0, color: typeColors[si % typeColors.length] }))
  )
  const totals = segs.map((list) => list.reduce((sum, seg) => sum + seg.value, 0))
  // 柱组配色取本柱最上面一段有值的类型
  const topColors = segs.map((list) => {
    let color = typeColors[0]
    list.forEach((seg) => {
      if (seg.value > 0) color = seg.color
    })
    return color
  })

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item' as const,
      backgroundColor: 'rgba(5, 16, 36, 0.96)',
      borderColor: 'rgba(0, 255, 247, 0.5)',
      borderWidth: 1,
      textStyle: { color: '#eaf7ff', fontSize: 12 },
      extraCssText: 'box-shadow: 0 0 22px rgba(0, 255, 247, 0.45);'
    },
    legend: {
      // 图例已改为右侧 HTML 标注，这里关闭 ECharts 内置图例
      show: false
    },
    grid: { top: 34, left: 24, right: 96, bottom: 62 },
    xAxis: {
      type: 'category' as const,
      data: villages,
      boundaryGap: true,
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: {
        color: '#bfe9ff',
        fontSize: 12,
        fontWeight: 600,
        margin: 26
      },
      splitLine: { show: false },
      axisPointer: { show: false }
    },
    yAxis: {
      type: 'value' as const,
      min: 0,
      max: maxVal * 1.18,
      axisLabel: { show: false },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { show: false }
    },
    series: [
      // ① 透视网格地面：地平线 + 纵向消失线 + 横向网格
      {
        type: 'custom' as const,
        silent: true,
        clip: false,
        z: 0,
        data: [[0, 0]],
        renderItem: (_params: unknown, api: BarRenderAPI) => {
          const left = api.coord([0, 0])
          const right = api.coord([cols - 1, 0])
          const gy = left[1]
          const xL = left[0] - glassW * 0.95
          const xR = right[0] + glassW * 0.95
          const depth = 22
          const children: Record<string, unknown>[] = [
            // 地平线：贯穿全部柱组的发光横线
            {
              type: 'line',
              shape: { x1: xL, y1: gy, x2: xR, y2: gy },
              style: {
                stroke: 'rgba(0, 255, 247, 0.8)',
                lineWidth: 2,
                shadowBlur: 18,
                shadowColor: 'rgba(0, 255, 247, 0.95)'
              }
            }
          ]
          // 纵向消失线：从地平线向外下方发散，形成透视网格
          const N = 10
          for (let i = 0; i <= N; i++) {
            const t = i / N
            const xt = xL + (xR - xL) * t
            const spread = (t - 0.5) * 2
            children.push({
              type: 'line',
              shape: { x1: xt, y1: gy, x2: xt + spread * depth * 1.15, y2: gy + depth },
              style: {
                stroke: `rgba(0, 255, 247, ${0.22 - Math.abs(spread) * 0.08})`,
                lineWidth: 1
              }
            })
          }
          // 横向透视网格：近疏远密、越远越淡
          for (let i = 1; i <= 3; i++) {
            const t = i / 3
            const yy = gy + depth * t
            const sp = depth * 1.15 * t
            children.push({
              type: 'line',
              shape: { x1: xL - sp, y1: yy, x2: xR + sp, y2: yy },
              style: { stroke: `rgba(0, 255, 247, ${0.18 - t * 0.11})`, lineWidth: 1 }
            })
          }
          return { type: 'group', children }
        }
      },

      // ② 柱组：虚影 → 多层光环底座 → 玻璃罩 → 水晶柱 → 罩体轮廓 → 数值
      {
        type: 'custom' as const,
        z: 3,
        clip: false,
        data: villages.map((_, vi) => [vi, totals[vi]]),
        tooltip: {
          formatter: (p: { dataIndex: number }) => {
            const vi = p.dataIndex
            const rows = series
              .filter((s) => (s.data[vi] ?? 0) > 0)
              .map((s) => `${s.name}：<b>${s.data[vi]}</b> 个`)
              .join('<br/>')
            return `<b>${villages[vi]}</b><br/>${rows || '暂无数据'}<br/><span style="color:#7fa8c6">合计：${totals[vi]} 个</span>`
          }
        },
        renderItem: (_params: unknown, api: BarRenderAPI) => {
          const vi = api.value(0)
          const total = api.value(1)
          const topColor = topColors[vi] ?? typeColors[0]
          const basePt = api.coord([vi, 0])
          const cx = basePt[0]
          const baseY = basePt[1]
          // 可用高度：网格顶部到 0 线
          const gridTopY = api.coord([vi, maxVal * 1.18])[1]
          const availH = Math.max(60, baseY - gridTopY)
          const ratio = maxVal > 0 ? total / maxVal : 0
          // 水晶柱高度：按占比缩放，同时保留基础视觉高度
          const crystalH = availH * 0.42 * (0.34 + 0.66 * ratio)
          const crystalTopY = baseY - crystalH
          // 玻璃罩比水晶柱高近一倍
          const glassH = crystalH * 2 + 10
          const glassTopY = baseY - glassH
          const gRx = glassW / 2
          const gRy = glassW * 0.085
          const cw = glassW * 0.46
          const cRy = cw * 0.2
          const children: Record<string, unknown>[] = []

          // 数据虚影：罩体后方更大更淡的残影
          children.push({
            type: 'rect',
            shape: { x: cx - gRx * 0.9 + 10, y: glassTopY + 6, width: glassW * 0.9, height: glassH },
            style: {
              fill: hexToRgba(topColor, 0.07),
              shadowBlur: 34,
              shadowColor: hexToRgba(topColor, 0.28)
            }
          })

          // 多层光环底座：外环 + 虚线中环 + 内环 + 中心光台
          const rings = [
            { k: 1.0, w: 1.8, a: 0.62, dash: undefined },
            { k: 0.78, w: 1.1, a: 0.4, dash: [4, 5] },
            { k: 0.56, w: 1, a: 0.26, dash: undefined }
          ]
          rings.forEach((ring) => {
            children.push({
              type: 'ellipse',
              shape: { cx, cy: baseY, rx: gRx * ring.k, ry: gRy * ring.k },
              style: {
                fill: 'transparent',
                stroke: hexToRgba('#00fff7', ring.a),
                lineWidth: ring.w,
                lineDash: ring.dash,
                shadowBlur: 14,
                shadowColor: hexToRgba('#00fff7', ring.a * 0.85)
              }
            })
          })
          children.push({
            type: 'ellipse',
            shape: { cx, cy: baseY, rx: gRx * 0.94, ry: gRy * 0.94 },
            style: {
              fill: new echarts.graphic.RadialGradient(0.5, 0.5, 0.5, [
                { offset: 0, color: hexToRgba(topColor, 0.52) },
                { offset: 0.6, color: hexToRgba(topColor, 0.16) },
                { offset: 1, color: hexToRgba(topColor, 0) }
              ])
            }
          })

          // 玻璃罩填充：顶部略亮、向下渐隐
          children.push({
            type: 'rect',
            shape: { x: cx - gRx, y: glassTopY, width: glassW, height: glassH },
            style: {
              fill: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(125, 251, 255, 0.16)' },
                { offset: 0.5, color: 'rgba(0, 255, 247, 0.05)' },
                { offset: 1, color: 'rgba(0, 255, 247, 0.02)' }
              ])
            }
          })

          // 水晶柱主体：按项目类型从下往上分段堆叠，每段用各自的类型色
          if (total > 0) {
            const filledSegs = (segs[vi] ?? []).filter((seg) => seg.value > 0)
            let accVal = 0
            filledSegs.forEach((seg, si) => {
              const yBottom = baseY - (crystalH * accVal) / total
              const yTopSeg = baseY - (crystalH * (accVal + seg.value)) / total
              accVal += seg.value
              children.push({
                type: 'rect',
                shape: {
                  x: cx - cw / 2,
                  y: yTopSeg,
                  width: cw,
                  height: Math.max(1, yBottom - yTopSeg)
                },
                style: {
                  fill: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: hexToRgba(seg.color, 0.3) },
                    { offset: 0.4, color: hexToRgba(seg.color, 0.74) },
                    { offset: 0.54, color: 'rgba(255, 255, 255, 0.85)' },
                    { offset: 0.68, color: hexToRgba(seg.color, 0.72) },
                    { offset: 1, color: hexToRgba(seg.color, 0.28) }
                  ]),
                  shadowBlur: 18,
                  shadowColor: hexToRgba(seg.color, 0.7)
                }
              })
              // 段间截面：亮色椭圆分隔，强化水晶分节质感
              if (si < filledSegs.length - 1) {
                children.push({
                  type: 'ellipse',
                  shape: { cx, cy: yTopSeg, rx: cw / 2, ry: cRy },
                  style: {
                    fill: hexToRgba(seg.color, 0.6),
                    stroke: 'rgba(255, 255, 255, 0.55)',
                    lineWidth: 0.8
                  }
                })
              }
            })
            // 水晶柱底面
            children.push({
              type: 'ellipse',
              shape: { cx, cy: baseY, rx: cw / 2, ry: cRy },
              style: { fill: hexToRgba(topColor, 0.5) }
            })
            // 水晶柱顶面：取最上段类型色的亮白高光
            children.push({
              type: 'ellipse',
              shape: { cx, cy: crystalTopY, rx: cw / 2, ry: cRy },
              style: {
                fill: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(255, 255, 255, 0.95)' },
                  { offset: 0.5, color: hexToRgba(topColor, 0.88) },
                  { offset: 1, color: hexToRgba(topColor, 0.7) }
                ]),
                shadowBlur: 16,
                shadowColor: hexToRgba(topColor, 0.95)
              }
            })
          }

          // 罩体轮廓：左右竖边 + 顶/底椭圆，营造透明圆柱
          children.push({
            type: 'line',
            shape: { x1: cx - gRx, y1: glassTopY, x2: cx - gRx, y2: baseY },
            style: { stroke: 'rgba(125, 251, 255, 0.4)', lineWidth: 1 }
          })
          children.push({
            type: 'line',
            shape: { x1: cx + gRx, y1: glassTopY, x2: cx + gRx, y2: baseY },
            style: { stroke: 'rgba(125, 251, 255, 0.4)', lineWidth: 1 }
          })
          children.push({
            type: 'ellipse',
            shape: { cx, cy: glassTopY, rx: gRx, ry: gRy },
            style: {
              fill: 'transparent',
              stroke: 'rgba(125, 251, 255, 0.55)',
              lineWidth: 1.2,
              shadowBlur: 10,
              shadowColor: 'rgba(125, 251, 255, 0.7)'
            }
          })
          children.push({
            type: 'ellipse',
            shape: { cx, cy: baseY, rx: gRx, ry: gRy },
            style: { fill: 'transparent', stroke: 'rgba(125, 251, 255, 0.2)', lineWidth: 1 }
          })

          // 数值：固定在罩体顶部上方，亮白发光
          if (total > 0) {
            children.push({
              type: 'text',
              style: {
                text: String(total),
                x: cx,
                y: glassTopY - 6,
                textAlign: 'center',
                textVerticalAlign: 'bottom',
                fill: '#ffffff',
                fontSize: 14,
                fontWeight: 'bold',
                textShadowBlur: 12,
                textShadowColor: 'rgba(0, 255, 247, 0.95)'
              }
            })
          }

          return { type: 'group', children }
        }
      }
    ]
  }
}

/** 状态节点角度：0°=正右、逆时针为正，6 个节点环绕分布并避开正上(90°)与正下(270°) */
const DONUT_NODE_ANGLES = [60, 120, 0, 180, 240, 300]
/** SVG viewBox 尺寸与中心点（必须与模板 viewBox="0 0 440 290" 保持一致） */
const DONUT_VIEW_W = 440
const DONUT_VIEW_H = 290
const DONUT_CX = 220
const DONUT_CY = 151
/** 节点所在椭圆半径：X 轴拉伸、Y 轴压缩，形成俯视透视 */
const DONUT_NODE_RX = 116
const DONUT_NODE_RY = 66
/** 标签沿径向向外偏移量，保证文字落在光点外侧 */
const DONUT_LABEL_GAP = 22

/** 项目总数：中心大号高亮数字 */
const donutTotal = computed(() =>
  statusStats.value.reduce((sum, item) => sum + item.value, 0)
)

/** 状态节点：按角度在扁椭圆上算坐标，并换算成容器百分比供 HTML 标签绝对定位 */
const donutNodes = computed(() =>
  statusStats.value.map((item, i) => {
    const rad = (DONUT_NODE_ANGLES[i % DONUT_NODE_ANGLES.length] * Math.PI) / 180
    const cos = Math.cos(rad)
    const sin = Math.sin(rad)
    // 椭圆坐标：SVG 的 Y 轴向下，纵向取负号
    const x = DONUT_CX + cos * DONUT_NODE_RX
    const y = DONUT_CY - sin * DONUT_NODE_RY
    // 标签沿「中心 → 节点」方向再外推固定距离，避免压在轨道线上
    const dx = x - DONUT_CX
    const dy = y - DONUT_CY
    const len = Math.hypot(dx, dy) || 1
    const labelX = x + (dx / len) * DONUT_LABEL_GAP
    const labelY = y + (dy / len) * DONUT_LABEL_GAP
    return {
      name: item.name,
      value: item.value,
      color: statusColor(item.name),
      x,
      y,
      labelX: (labelX / DONUT_VIEW_W) * 100,
      labelY: (labelY / DONUT_VIEW_H) * 100,
      // 左半区标签反向对齐（文字朝左延伸），右半区正向
      left: cos < 0
    }
  })
)

/** 初始化图表：按缩放比提升像素密度，避免大屏放大后图表模糊 */
function initChart(el: HTMLDivElement) {
  const dpr = Math.min(3, (window.devicePixelRatio || 1) * Math.max(1, scale.value))
  return echarts.init(el, undefined, { devicePixelRatio: dpr })
}

/** 渲染全部图表（首次初始化，刷新时仅更新配置） */
function renderCharts() {
  if (detailRef.value) {
    detailChart = detailChart ?? initChart(detailRef.value)
    detailChart.setOption(buildDetailOption(), true)
  }
  if (barRef.value) {
    barChart = barChart ?? initChart(barRef.value)
    barChart.setOption(buildBarOption(), true)
  }
}

/** 加载大屏数据 */
async function loadData() {
  try {
    const res = await getScreenOverview()
    overview.value = res.data
    // 实时事件流：跨轮询去重合并，新事件前置，避免整表替换造成闪烁
    mergeEvents(res.data?.events)
    await nextTick()
    renderCharts()
  } catch {
    // 拦截器已统一提示错误信息
  }
}

/** 投屏：在新窗口打开独立全屏大屏页面 */
function openCast() {
  const { href } = router.resolve({ name: 'DataScreenCast' })
  window.open(href, '_blank', 'noopener')
}

onMounted(async () => {
  // 监听容器尺寸变化（窗口缩放、浏览器缩放），实时重算缩放比例与图表尺寸
  resizeObserver = new ResizeObserver(async () => {
    await resizeCanvas()
  })
  if (rootRef.value) resizeObserver.observe(rootRef.value)
  window.addEventListener('resize', resizeCanvas)
  resizeCanvas()
  updateClock()
  clockTimer = window.setInterval(updateClock, 1000)
  refreshTimer = window.setInterval(loadData, REFRESH_INTERVAL)
  loading.value = true
  await loadData()
  loading.value = false
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  window.removeEventListener('resize', resizeCanvas)
  if (clockTimer) window.clearInterval(clockTimer)
  if (refreshTimer) window.clearInterval(refreshTimer)
  window.cancelAnimationFrame(metricRaf)
  ;[detailChart, barChart].forEach((chart) => chart?.dispose())
  detailChart = null
  barChart = null
})
</script>

<style scoped>
/* ========== 大屏骨架 ========== */
/* 内嵌模式：绝对定位铺满主内容区（含内边距区域），不遮挡侧边栏与顶栏 */
.screen-root {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 50% -10%, rgba(38, 224, 164, 0.08), transparent 55%),
    radial-gradient(ellipse at 50% 110%, rgba(24, 110, 130, 0.1), transparent 60%),
    #030a10;
}

/* 投屏模式：独立整屏，覆盖整个浏览器窗口 */
.screen-root.is-standalone {
  position: fixed;
  inset: 0;
  z-index: 2000;
}

/* 画布：左上角对齐后整体等比缩放，宽高经缩放后正好铺满容器 */
.screen-canvas {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  transform-origin: top left;
  background:
    radial-gradient(ellipse 75% 50% at 50% -8%, rgba(32, 108, 190, 0.42) 0%, rgba(3, 10, 20, 0) 62%),
    radial-gradient(ellipse 60% 42% at 50% 106%, rgba(18, 92, 160, 0.24) 0%, rgba(3, 10, 20, 0) 62%),
    linear-gradient(180deg, #02060e 0%, #071528 46%, #030a14 100%);
  color: #cfe9ff;
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
}

/* 背景网格 + 中心冷光 */
.screen-canvas::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(ellipse 62% 46% at 50% 44%, rgba(46, 130, 210, 0.14) 0%, transparent 70%),
    linear-gradient(rgba(86, 180, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(86, 180, 255, 0.05) 1px, transparent 1px);
  background-size: 100% 100%, 44px 44px, 44px 44px;
  pointer-events: none;
}

/* 底部透视网格：营造空间纵深 */
.screen-canvas::after {
  content: '';
  position: absolute;
  left: -15%;
  right: -15%;
  bottom: -2%;
  height: 34%;
  background-image:
    linear-gradient(rgba(70, 170, 255, 0.22) 1px, transparent 1px),
    linear-gradient(90deg, rgba(70, 170, 255, 0.22) 1px, transparent 1px);
  background-size: 60px 42px;
  transform: perspective(700px) rotateX(66deg);
  transform-origin: bottom center;
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, #000 58%, #000 100%);
  mask-image: linear-gradient(180deg, transparent 0%, #000 58%, #000 100%);
  opacity: 0.35;
  pointer-events: none;
  z-index: 0;
}

/* ========== 顶部标题栏 ========== */
.screen-header {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  height: 104px;
  padding: 0 26px;
  flex-shrink: 0;
}

/* 顶部横幅底板：两端斜切的深蓝条带，参考综合校情大屏版式 */
.screen-header::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background:
    radial-gradient(ellipse 46% 130% at 50% 0%, rgba(56, 156, 245, 0.42) 0%, rgba(3, 10, 20, 0) 70%),
    linear-gradient(180deg, rgba(23, 74, 130, 0.82) 0%, rgba(9, 32, 60, 0.55) 58%, rgba(3, 10, 20, 0) 100%);
  clip-path: polygon(0 0, 100% 0, 97.5% 100%, 2.5% 100%);
  pointer-events: none;
}

/* 标题正下方的 V 形光带 */
.screen-header::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 560px;
  height: 12px;
  transform: translateX(-50%);
  background: linear-gradient(180deg, rgba(64, 172, 255, 0.85), rgba(20, 80, 145, 0));
  clip-path: polygon(0 0, 100% 0, 50% 100%);
  opacity: 0.8;
  pointer-events: none;
}

.header-side {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.header-right {
  justify-content: flex-end;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 11px;
  border: 1px solid rgba(53, 216, 255, 0.28);
  border-radius: 4px;
  background: rgba(11, 46, 80, 0.55);
  font-size: 12px;
  color: #9fd4ef;
  white-space: nowrap;
}

.chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #23e6a0;
  box-shadow: 0 0 8px #23e6a0;
  animation: chip-pulse 1.8s ease-in-out infinite;
}

/* 标题居中容器：柔光衬底 */
.header-center {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 标题区：柔光衬底 */
.header-plate {
  position: relative;
  padding: 0 40px;
}

/* 标题背后的光晕：四边自然淡出 */
.header-plate::before {
  content: '';
  position: absolute;
  inset: -20px -48px;
  background: radial-gradient(
    ellipse 52% 64% at 50% 52%,
    rgba(56, 150, 228, 0.32) 0%,
    rgba(24, 88, 152, 0.12) 52%,
    rgba(3, 10, 20, 0) 78%
  );
  pointer-events: none;
}

.header-title {
  position: relative;
  z-index: 1;
  margin: 0;
  font-size: 38px;
  font-weight: 700;
  letter-spacing: 8px;
  line-height: 1.15;
  padding-left: 8px;
  background: linear-gradient(180deg, #ffffff 8%, #a8e9ff 52%, #35b8ff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 26px rgba(53, 216, 255, 0.35);
}


.clock {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.25;
}

.clock-date {
  font-size: 12px;
  color: #8fb8d4;
  letter-spacing: 1px;
}

.clock-time {
  font-size: 22px;
  font-weight: 700;
  color: #eaf7ff;
  letter-spacing: 2px;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 12px rgba(53, 216, 255, 0.45);
}

.exit-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 14px;
  padding: 7px 14px;
  border: 1px solid rgba(38, 224, 164, 0.45);
  border-radius: 4px;
  background: rgba(10, 42, 34, 0.6);
  color: #a8f5dd;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.exit-btn:hover {
  border-color: #26e0a4;
  background: rgba(16, 78, 62, 0.75);
  color: #ffffff;
  box-shadow: 0 0 14px rgba(38, 224, 164, 0.35);
}

/* ========== 主体三栏 ========== */
.screen-body {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 25% 1fr 25%;
  gap: 12px;
  flex: 1;
  min-height: 0;
  padding: 10px 16px;
  box-sizing: border-box;
}

.col {
  display: grid;
  gap: 12px;
  min-height: 0;
}

.col-left {
  grid-template-rows: 1.16fr 1.04fr 1.3fr;
}

.col-center {
  grid-template-rows: 98px 1.32fr 1fr;
}

.col-right {
  grid-template-rows: 250px 1fr 1fr;
}

/* ========== 中栏 KPI：悬浮玻璃胶囊标签 ========== */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

/* KPI 卡片：承载悬浮胶囊、光晕、扫描环与倒影 */
.kpi-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  height: 100%;
  min-width: 0;
  --kpi-accent: 110, 235, 245;
  --kpi-fill: 12, 70, 86;
  --kpi-glow: 0.55;
  --kpi-ring-opacity: 0.6;
}

/* 四卡平行静态分布：同一基线，不做前后错落 */
.kpi-card--back {
  --kpi-glow: 0.55;
  --kpi-ring-opacity: 0.55;
}

.kpi-card--front {
  --kpi-glow: 1;
  --kpi-ring-opacity: 1;
}

.kpi-tone-cyan {
  --kpi-accent: 110, 235, 245;
  --kpi-fill: 12, 70, 86;
}

.kpi-tone-green {
  --kpi-accent: 140, 240, 120;
  --kpi-fill: 24, 74, 40;
}

.kpi-tone-blue {
  --kpi-accent: 115, 180, 255;
  --kpi-fill: 20, 56, 110;
}

.kpi-tone-purple {
  --kpi-accent: 180, 130, 255;
  --kpi-fill: 62, 38, 105;
}

/* 悬浮容器：四卡平行静态排列 */
.kpi-float {
  position: relative;
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  align-items: center;
  width: 100%;
  padding-top: 4px;
}

/* 底部悬浮光晕：宽度约卡片 1.3 倍，蓝青柔光 */
.kpi-halo {
  position: absolute;
  left: 50%;
  bottom: -8px;
  z-index: 0;
  width: 132%;
  height: 54px;
  transform: translateX(-50%);
  background: radial-gradient(
    ellipse at 50% 26%,
    rgba(var(--kpi-accent), 0.85) 0%,
    rgba(90, 190, 255, 0.42) 42%,
    rgba(0, 0, 0, 0) 74%
  );
  filter: blur(12px);
  opacity: var(--kpi-glow);
  pointer-events: none;
}

/* 椭圆全息扫描环：三圈横向椭圆，贴合底盘透视 */
.kpi-rings {
  position: absolute;
  left: 50%;
  bottom: -20px;
  z-index: 0;
  width: 126%;
  height: 40px;
  transform: translateX(-50%);
  opacity: var(--kpi-ring-opacity);
  pointer-events: none;
}

.kpi-rings i {
  position: absolute;
  left: 50%;
  top: 50%;
  border-radius: 50%;
  border: 1.5px solid rgba(var(--kpi-accent), 0.7);
  box-shadow:
    0 0 8px rgba(var(--kpi-accent), 0.45),
    inset 0 0 8px rgba(var(--kpi-accent), 0.3);
  transform: translate(-50%, -50%);
}

.kpi-rings i:nth-child(1) {
  width: 100%;
  height: 100%;
  opacity: 0.55;
}

.kpi-rings i:nth-child(2) {
  width: 78%;
  height: 72%;
  border-color: rgba(var(--kpi-accent), 0.8);
  opacity: 0.42;
}

.kpi-rings i:nth-child(3) {
  width: 56%;
  height: 46%;
  opacity: 0.3;
}

/* 玻璃胶囊本体：长圆角胶囊、半透明磨砂玻璃、细发光边框 */
.kpi-glass {
  position: relative;
  z-index: 2;
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: fit-content;
  min-width: 200px;
  max-width: 100%;
  height: 76px;
  padding: 0 30px;
  border-radius: 6px;
  /* 玻璃本体：半透明磨砂 + 主题色内透光 */
  background:
    linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.18) 0%,
      rgba(255, 255, 255, 0.03) 42%,
      rgba(var(--kpi-accent), 0.14) 100%
    ),
    linear-gradient(
      180deg,
      rgba(var(--kpi-fill), 0.55) 0%,
      rgba(16, 48, 82, 0.35) 55%,
      rgba(8, 26, 46, 0.42) 100%
    );
  backdrop-filter: blur(10px) saturate(170%);
  -webkit-backdrop-filter: blur(10px) saturate(170%);
  border: 1px solid rgba(80, 180, 255, 0.45);
  box-shadow:
    /* 边缘外发光 */
    0 0 16px rgba(var(--kpi-accent), 0.28),
    0 6px 14px rgba(0, 10, 24, 0.55),
    /* 上沿内高光 */
    inset 0 1px 0 rgba(255, 255, 255, 0.55),
    /* 下沿内发光 */
    inset 0 -1px 0 rgba(var(--kpi-accent), 0.5);
  transition: box-shadow 0.25s ease, transform 0.25s ease;
}

/* 底部发光棱 */
.kpi-glass::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 12%;
  right: 12%;
  height: 1px;
  background: linear-gradient(
    90deg,
    rgba(var(--kpi-accent), 0) 0%,
    rgba(var(--kpi-accent), 0.85) 30%,
    rgba(255, 255, 255, 0.9) 50%,
    rgba(var(--kpi-accent), 0.85) 70%,
    rgba(var(--kpi-accent), 0) 100%
  );
  box-shadow: 0 0 6px rgba(var(--kpi-accent), 0.8);
  pointer-events: none;
}

.kpi-card:hover .kpi-glass {
  transform: translateY(-2px);
  box-shadow:
    0 0 24px rgba(var(--kpi-accent), 0.5),
    0 8px 18px rgba(0, 10, 24, 0.7),
    inset 0 1px 0 rgba(255, 255, 255, 0.7),
    inset 0 -1px 0 rgba(var(--kpi-accent), 0.7);
}

/* 数值：白色发光，水平正向 */
.kpi-value {
  font-size: 34px;
  font-weight: 600;
  color: #ffffff;
  line-height: 1;
  letter-spacing: 0.5px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  text-shadow:
    0 0 8px rgba(var(--kpi-accent), 0.95),
    0 0 20px rgba(var(--kpi-accent), 0.5);
}

.kpi-value em {
  display: inline;
  margin-left: 3px;
  font-size: 14px;
  font-style: normal;
  font-weight: 500;
  color: rgba(236, 253, 255, 0.95);
  text-shadow: 0 0 6px rgba(var(--kpi-accent), 0.55);
}

/* 指标名称：浅青白色，水平正向 */
.kpi-label {
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 1.2px;
  line-height: 1;
  color: rgba(210, 242, 255, 0.92);
  white-space: nowrap;
  text-shadow: 0 0 6px rgba(var(--kpi-accent), 0.45);
}

/* ========== 图表容器 ========== */
.chart {
  width: 100%;
  height: 100%;
}

/* ========== 各村申报明细统计 ========== */
.detail-stage {
  position: relative;
  width: 100%;
  height: 100%;
}

/* 空态覆盖在图表之上，数据为空时展示 */
.detail-empty {
  position: absolute;
  inset: 0;
  z-index: 2;
}

/* 自定义图例：覆盖在图表顶部，两行分别说明线型(项目类型)与颜色(明细类别) */
.detail-legend {
  position: absolute;
  top: 2px;
  right: 4px;
  z-index: 3;
  display: flex;
  flex-direction: column;
  gap: 4px;
  pointer-events: none;
}

.detail-legend__row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.detail-legend__item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #bfe9ff;
  white-space: nowrap;
}

.detail-legend__item em {
  font-style: normal;
}

/* 线型示意：一段横线，通过 borderTopStyle 区分实 / 虚 / 点 */
.detail-legend__line {
  display: inline-block;
  width: 18px;
  border-top: 2px solid #cfeaff;
}

/* 颜色示意：圆点（颜色由内联 style 注入） */
.detail-legend__dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* ========== 各村项目类型分布 ========== */
.bar-stage {
  position: relative;
  width: 100%;
  height: 100%;
}

/* 类型颜色标注：竖向排在图表右侧，与柱组分离 */
.type-legend {
  position: absolute;
  right: 2px;
  top: 50%;
  display: flex;
  flex-direction: column;
  gap: 11px;
  transform: translateY(-50%);
  pointer-events: none;
}

.type-legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #bfe9ff;
  white-space: nowrap;
}

.type-legend-item i {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  flex-shrink: 0;
}

.type-legend-item em {
  font-style: normal;
}

/* ========== 资金完成率：发光圆环 + 液态水位 ========== */
/* 整体纵向布局：圆环自适应撑满，说明文案固定底部 */
.fund-gauge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
  min-height: 0;
}

/* 外环容器：只负责尺寸与外发光，内部不做填充，保证玻璃内胆通透 */
.fund-gauge__orb {
  position: relative;
  flex: 1 1 0;
  aspect-ratio: 1;
  max-width: 100%;
  min-height: 0;
  padding: 4px;
  border-radius: 50%;
  box-shadow:
    0 0 7px rgba(35, 230, 160, 0.5),
    0 0 18px rgba(35, 230, 160, 0.2);
}

/* 光环本体：用遮罩只保留 4px 描边，渐变色不会透进玻璃内胆 */
.fund-gauge__orb::before {
  content: '';
  position: absolute;
  inset: 0;
  padding: 4px;
  border-radius: 50%;
  background: conic-gradient(from 200deg, #0d5c44, #2bf0aa 22%, #b6ffe6 46%, #28d99a 68%, #0d5c44);
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  mask-composite: exclude;
  pointer-events: none;
}

/* 液池：中心透明，仅贴绿环的内圈压暗，形成管壁暗边 */
.fund-gauge__well {
  position: relative;
  height: 100%;
  overflow: hidden;
  border-radius: 50%;
  background:
    radial-gradient(circle at 34% 20%, rgba(190, 235, 255, 0.14) 0%, transparent 28%),
    radial-gradient(circle at 50% 50%, rgba(10, 24, 36, 0.05) 0%, rgba(8, 20, 32, 0.28) 60%, rgba(5, 16, 18, 0.8) 86%, rgba(2, 10, 10, 0.94) 100%);
  box-shadow:
    inset 0 0 14px 4px rgba(3, 18, 14, 0.88),
    inset 0 3px 8px rgba(190, 240, 255, 0.16),
    inset 0 -10px 18px rgba(0, 0, 0, 0.4);
}

/* 液面高度 = 拨付完成率，高度变化带过渡 */
.fund-gauge__liquid {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, #35e9ad 0%, #1ecb90 42%, #0b8a61 100%);
  transition: height 0.9s cubic-bezier(0.22, 0.61, 0.36, 1);
}

/* 水中高光，让液体更通透 */
.fund-gauge__liquid::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse 66% 46% at 50% 100%, rgba(255, 255, 255, 0.26), transparent 70%);
  pointer-events: none;
}

/* 液面波纹：宽度为液池 2 倍，平移量等于一个完整波形周期，循环无缝 */
.fund-gauge__wave {
  position: absolute;
  left: 0;
  width: 200%;
  height: 30px;
  pointer-events: none;
  animation: fund-wave 8s linear infinite;
}

/* 绿色浪体：与液体本体同色并下沉 6px 衔接，避免出现横向分割线 */
.fund-gauge__wave--body {
  bottom: calc(100% - 6px);
  fill: #35e9ad;
}

/* 浅绿浪层：位于浪体之后，波峰略高于绿色浪体，与浪体同向同速、错开相位 */
.fund-gauge__wave--mint {
  bottom: calc(100% - 3px);
  height: 36px;
  fill: rgba(178, 255, 232, 0.5);
  animation-delay: -2s;
}

/* 波纹整体向右侧流动：从 -1 个周期平移回 0，首尾波形一致，循环无缝 */
@keyframes fund-wave {
  from {
    transform: translateX(-33.3333%);
  }
  to {
    transform: translateX(0);
  }
}

/* 环内数值：百分比 + 已拨付金额 */
.fund-gauge__value {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  text-align: center;
  pointer-events: none;
  text-shadow:
    0 1px 6px rgba(2, 12, 20, 0.9),
    0 0 12px rgba(4, 30, 26, 0.65);
}

.fund-gauge__value b {
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  color: #f2fffb;
}

.fund-gauge__value b em {
  margin-left: 1px;
  font-size: 14px;
  font-style: normal;
}

/* 环下说明已移除，环体独占面板高度 */

/* ========== 各村项目分布 ========== */
.village-map {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 4px;
  background:
    radial-gradient(circle at 50% 50%, rgba(24, 96, 158, 0.28) 0%, rgba(4, 18, 34, 0.05) 65%),
    linear-gradient(180deg, rgba(10, 26, 44, 0.35) 0%, rgba(3, 10, 18, 0.2) 100%);
  box-shadow: inset 0 0 30px rgba(0, 6, 16, 0.5);
  overflow: hidden;
}

.map-caption {
  position: absolute;
  top: 2px;
  left: 2px;
  right: 2px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: #7fa8c6;
  z-index: 2;
}

.map-town {
  color: #bfe9ff;
  letter-spacing: 1px;
}

.map-ring {
  position: absolute;
  border: 1px dashed rgba(96, 184, 244, 0.2);
  border-radius: 50%;
  pointer-events: none;
}

.map-ring-outer {
  width: 76%;
  height: 78%;
  left: 12%;
  top: 11%;
  animation: ring-spin 28s linear infinite;
}

.map-ring-inner {
  width: 44%;
  height: 46%;
  left: 28%;
  top: 27%;
  border-style: solid;
  border-color: rgba(53, 216, 255, 0.12);
}

.map-links {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.map-links line {
  stroke: rgba(96, 184, 244, 0.28);
  stroke-width: 0.25;
  stroke-dasharray: 1.4 1.2;
}

.map-node {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 6px;
  transform: translate(-50%, -50%);
  white-space: nowrap;
}

.map-node-core {
  width: var(--node-size);
  height: var(--node-size);
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(var(--node-color), 1) 42%,
    rgba(var(--node-color), 0.15) 100%
  );
  box-shadow:
    0 0 0 3px rgba(var(--node-color), 0.1),
    0 0 14px rgba(var(--node-color), 0.85),
    inset -2px -2px 4px rgba(0, 12, 8, 0.35);
  animation: node-pulse 2.4s ease-in-out infinite;
}

.map-node-label {
  display: flex;
  flex-direction: column;
  font-size: 11px;
  color: #bfe9ff;
  line-height: 1.25;
}

.map-node-label em {
  font-size: 10px;
  font-style: normal;
  color: #6fe4ff;
}

.map-empty {
  position: absolute;
  left: 50%;
  top: 52%;
  transform: translate(-50%, -50%);
  margin: 0;
  font-size: 12px;
  color: #6c8ba6;
}

/* ========== 物资状态 ========== */
.material-viewport {
  height: 100%;
  overflow: hidden;
}

.material-track {
  display: flex;
  flex-direction: column;
}

/* 物资超过可视条数时无缝向上滚动，鼠标悬停暂停 */
.material-track.scrolling {
  animation: material-scroll var(--scroll-duration, 14s) linear infinite;
}

.material-track.scrolling:hover {
  animation-play-state: paused;
}

@keyframes material-scroll {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(-50%);
  }
}

.material-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  padding: 0 0 6px;
  list-style: none;
  overflow: hidden;
}

.material-item {
  display: grid;
  grid-template-columns: 1fr 56px 66px 8px;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 1px solid rgba(56, 170, 255, 0.14);
  border-radius: 4px;
  background: rgba(9, 34, 60, 0.5);
  font-size: 12px;
}

.material-name {
  color: #cfe9ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-stock {
  color: #7fa8c6;
  text-align: right;
}

.material-stock b {
  margin-right: 2px;
  font-size: 14px;
  color: #6fe4ff;
}

.material-stock b.danger {
  color: #ff6b81;
}

.material-warn {
  font-size: 11px;
  color: #6c8ba6;
  text-align: right;
}

.material-flag {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #23e6a0;
}

.material-flag.danger {
  background: #ff6b81;
  box-shadow: 0 0 8px rgba(255, 107, 129, 0.9);
}

/* ========== 核心指标：全息科技风环形发光底座 ========== */
.metric-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-auto-rows: 1fr;
  gap: 8px;
  height: 100%;
  /* 允许 grid 行被压缩到内容高度，避免 min-height 撑开 */
  grid-auto-flow: row;
  align-content: stretch;
}

/* 卡片：暗深蓝毛玻璃底，切角与外层面板呼应，整体低亮度 */
.metric-item {
  position: relative;
  min-width: 0;
  /* 去掉强制最小高度，让 grid 自动分配 1/2 高度 */
  padding: 6px 6px 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px);
  background:
    radial-gradient(ellipse 68% 48% at 50% 100%, rgba(56, 168, 255, 0.16) 0%, rgba(8, 24, 44, 0) 72%),
    linear-gradient(180deg, rgba(12, 32, 56, 0.6) 0%, rgba(5, 15, 28, 0.42) 100%);
  backdrop-filter: blur(8px) saturate(140%);
  -webkit-backdrop-filter: blur(8px) saturate(140%);
  box-shadow:
    inset 0 1px 0 rgba(150, 220, 255, 0.14),
    inset 0 0 24px rgba(40, 130, 210, 0.08);
}

/* 内侧细描边：clip-path 会裁掉 border，用内阴影绘制 1px 发光轮廓 */
.metric-item::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  box-shadow: inset 0 0 0 1px rgba(96, 184, 244, 0.18);
}

/* ★ 新方案：菱形底座 + 发散光束 = 同一张 SVG，stage 容器覆盖整张卡片 */
.metric-stage {
  position: absolute;
  left: 50%;
  bottom: 0;
  z-index: 1;
  width: 100%;
  height: 100%;
  transform: translateX(-50%);
  pointer-events: none;
}

.metric-stage > svg {
  display: block;
  width: 100%;
  height: 100%;
  overflow: visible;
}

/* 动态数字：整体再上移，贴近卡片顶部，远离光束亮部 */
.metric-readout {
  position: absolute;
  left: 0;
  right: 0;
  top: 8%;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  pointer-events: none;
}

.metric-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.5px;
  /* 每张卡片由父级注入 --metric-accent，实现四色区分 */
  color: var(--metric-accent, #f4f9fd);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  -webkit-font-smoothing: antialiased;
  /* 贴边深色描影保证锐利，外加一层同色微光融入卡片 */
  text-shadow:
    0 0 1px rgba(2, 10, 20, 0.95),
    0 1px 1px rgba(2, 10, 20, 0.95),
    0 1px 2px rgba(2, 10, 20, 0.9),
    0 0 7px var(--metric-accent, rgba(88, 178, 255, 0.35));
}

.metric-value em {
  margin-left: 3px;
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
  color: var(--metric-accent, #cfe2ef);
  opacity: 0.88;
  text-shadow: 0 0 1px rgba(2, 10, 20, 0.95), 0 1px 1px rgba(2, 10, 20, 0.95);
}

.metric-label {
  /* 与数字同色、略降不透明度做层级，仍保持清晰 */
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.6px;
  color: var(--metric-accent, #c9dceb);
  opacity: 0.9;
  text-shadow:
    0 0 1px rgba(2, 10, 20, 0.95),
    0 1px 1px rgba(2, 10, 20, 0.95),
    0 1px 2px rgba(2, 10, 20, 0.9);
}



/* ========== 项目状态分布：悬浮轨道圆盘 ========== */
.donut-stage {
  position: relative;
  width: 100%;
  height: 100%;
}

/* 圆盘底衬：一层柔和椭圆光垫，让整组轨道悬浮在卡片底之上 */
.donut-stage::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 78%;
  height: 74%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: radial-gradient(
    ellipse at 50% 50%,
    rgba(28, 104, 172, 0.16) 0%,
    rgba(12, 44, 76, 0.07) 52%,
    rgba(0, 0, 0, 0) 78%
  );
  pointer-events: none;
  z-index: 0;
}

/* SVG 轨道层：铺满容器（不保持比例，保证光点与 HTML 标签百分比严格对齐） */
.donut-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
  z-index: 1;
}

/* 中心径向光晕：周期性明暗呼吸，营造全息悬浮感 */
.dn-halo {
  transform-box: view-box;
  transform-origin: 220px 140px;
  animation: dn-breathe 4.6s ease-in-out infinite;
}

@keyframes dn-breathe {
  0%,
  100% {
    opacity: 0.62;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.07);
  }
}

.dn-orbit {
  fill: none;
}

/* 外层：最淡、虚线弱化，拉开纵深 */
.dn-orbit-outer {
  stroke: rgba(94, 190, 240, 0.56);
  stroke-width: 1.2;
  stroke-dasharray: 3 6;
  filter: drop-shadow(0 0 3px rgba(74, 178, 234, 0.28));
}

/* 中层：最初的淡蓝色渐变轨道 */
.dn-orbit-mid {
  stroke: url(#dn-mid-grad);
  stroke-width: 1.4;
  filter: drop-shadow(0 0 6px rgba(53, 180, 255, 0.32));
}

/* 中圈流光：拖尾层在前，亮度低、长度长，形成自然渐隐尾迹 */
.dn-orbit-flow-trail {
  fill: none;
  stroke: rgba(110, 205, 255, 0.3);
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-dasharray: 190 361;
  animation: dn-orbit-flow-run 4.8s linear infinite;
}

/* 中圈流光：头部亮弧，仅比提亮后的轨道稍亮，避免突兀 */
.dn-orbit-flow {
  fill: none;
  stroke: rgba(150, 224, 255, 0.9);
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-dasharray: 70 481;
  filter: drop-shadow(0 0 4px rgba(90, 200, 255, 0.35));
  animation: dn-orbit-flow-run 4.8s linear infinite;
}

@keyframes dn-orbit-flow-run {
  from {
    stroke-dashoffset: 551;
  }
  to {
    stroke-dashoffset: 0;
  }
}

/* 中心实心圆台：外侧壁与完整椭圆顶面共同形成约 22px 的厚度 */
.dn-platform-outer-wall {
  fill: url(#dn-platform-side-grad);
  stroke: rgba(140, 235, 255, 0.8);
  stroke-width: 1;
  filter: drop-shadow(0 4px 6px rgba(0, 7, 18, 0.45)) drop-shadow(0 0 3px rgba(90, 215, 255, 0.3));
}

.dn-platform-bottom-rim {
  fill: none;
  stroke: rgba(150, 240, 255, 0.9);
  stroke-width: 1.2;
  filter: drop-shadow(0 0 4px rgba(90, 220, 255, 0.4));
}

.dn-platform-top {
  fill: url(#dn-platform-top-grad);
  stroke: rgba(150, 240, 255, 0.95);
  stroke-width: 1.2;
}

/* 玻璃反射使用清晰轮廓，不添加模糊或雾化滤镜 */
.dn-platform-gloss {
  fill: url(#dn-platform-gloss-grad);
  pointer-events: none;
}

.dn-platform-edge {
  fill: none;
}

.dn-platform-edge-outer {
  stroke: url(#dn-inner-grad);
  stroke-width: 1.4;
  filter: drop-shadow(0 0 4px rgba(53, 216, 255, 0.65));
}

/* 中心：大号白色高亮数字 + 副标题 */
.dn-center {
  position: absolute;
  left: 50%;
  top: 37%;
  display: flex;
  flex-direction: column;
  align-items: center;
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 3;
}

.dn-center b {
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
  color: #ffffff;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 8px rgba(53, 216, 255, 0.7);
}

.dn-center span {
  margin-top: 5px;
  font-size: 11px;
  letter-spacing: 2px;
  color: #8fb8d4;
}

/* 状态文字标签：正向可读，落在光点外侧 */
.dn-label {
  position: absolute;
  display: flex;
  align-items: baseline;
  gap: 5px;
  white-space: nowrap;
  transform: translate(2px, -50%);
  pointer-events: none;
  z-index: 2;
}

/* 左半区标签反向排列，文字朝左延伸，避免压到圆盘中心 */
.dn-label.is-left {
  flex-direction: row-reverse;
  transform: translate(calc(-100% - 2px), -50%);
}

.dn-label-name {
  font-size: 11px;
  font-weight: 500;
}

.dn-label-value {
  font-size: 17px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 8px currentColor;
}

/* 数值为 0 的节点整体弱化 */
.dn-label.is-zero {
  opacity: 0.42;
}

/* ========== 实时事件 ========== */
.event-viewport {
  height: 100%;
  overflow: hidden;
}

.event-track {
  display: flex;
  flex-direction: column;
}

/* 事件超过可视条数时无缝向上滚动，鼠标悬停暂停 */
.event-track.scrolling {
  animation: event-scroll var(--event-scroll-duration, 14s) linear infinite;
}

.event-track.scrolling:hover {
  animation-play-state: paused;
}

@keyframes event-scroll {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(-50%);
  }
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  padding: 0 0 6px;
  list-style: none;
  overflow: hidden;
}

.event-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 6px 8px;
  border-left: 2px solid rgba(96, 184, 244, 0.28);
  background: linear-gradient(90deg, rgba(14, 42, 70, 0.5) 0%, rgba(8, 22, 38, 0.14) 100%);
  box-shadow: inset 0 1px 0 rgba(130, 200, 255, 0.06);
}

/* 左侧彩色圆点：颜色按事件类别映射，带辉光 */
.event-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 50%;
}

.event-main {
  flex: 1;
  min-width: 0;
}

.event-text {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  font-size: 12px;
  line-height: 1.45;
  color: #e4f3ff;
  overflow: hidden;
  word-break: break-all;
}

.event-time {
  flex-shrink: 0;
  font-size: 11px;
  color: #6fe4ff;
  font-variant-numeric: tabular-nums;
}

/* ========== 空状态 ========== */
.panel-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  margin: 0;
  font-size: 12px;
  color: #6c8ba6;
}

/* ========== 底部数据流 ========== */
.screen-ticker {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  height: 58px;
  margin: 0 16px 0;
  padding: 0 14px;
  border-top: 1px solid rgba(38, 224, 164, 0.2);
  background: linear-gradient(90deg, rgba(10, 42, 34, 0.55) 0%, rgba(5, 22, 18, 0.2) 100%);
  overflow: hidden;
}

.ticker-label {
  flex-shrink: 0;
  font-size: 12px;
  letter-spacing: 1px;
  color: #26e0a4;
  text-shadow: 0 0 10px rgba(38, 224, 164, 0.5);
}

.ticker-viewport {
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

.ticker-track {
  display: inline-flex;
  align-items: center;
  gap: 44px;
  white-space: nowrap;
  animation: ticker-move 38s linear infinite;
}

.ticker-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8fb8d4;
}

.ticker-item em {
  width: 5px;
  height: 5px;
  transform: rotate(45deg);
  background: #35d8ff;
  box-shadow: 0 0 6px rgba(53, 216, 255, 0.8);
}

.ticker-item b {
  font-weight: 600;
  color: #eaf7ff;
}

.ticker-update {
  flex-shrink: 0;
  font-size: 11px;
  color: #6c8ba6;
}

/* ========== 动效 ========== */
@keyframes chip-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.35;
  }
}

@keyframes node-pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.95;
  }
  50% {
    transform: scale(1.18);
    opacity: 0.7;
  }
}

@keyframes ring-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes ticker-move {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .chip-dot,
  .map-node-core,
  .map-ring-outer,
  .ticker-track {
    animation: none;
  }
}
</style>
