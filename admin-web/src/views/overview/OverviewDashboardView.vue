<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { EChartsOption } from 'echarts'
import EChartCard from '@/components/EChartCard.vue'
import { fetchAdminOverview, fetchAdminRealtimeWeather } from '@/api/admin'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import type { OverviewPayload, RealtimeWeatherPayload } from '@/types'

interface Coordinates {
  longitude: number
  latitude: number
}

const loading = ref(false)
const weatherLoading = ref(false)
const weatherRefreshing = ref(false)
const weatherError = ref('')
const weather = ref<RealtimeWeatherPayload | null>(null)
const coordinates = ref<Coordinates | null>(null)
let weatherTimer: number | undefined

const overview = ref<OverviewPayload>({
  metrics: [],
  activities: [],
  notices: [],
  deviceStats: [],
  projectDistribution: [],
  progressTrend: [],
})

const isSuitableToFly = computed(() => weather.value?.suitability.result === '适宜飞行')

const weatherMetrics = computed(() => {
  if (!weather.value) {
    return []
  }

  return [
    {
      label: '温度',
      value: `${weather.value.temperature.toFixed(1)}°C`,
      note: '高德实时天气',
    },
    {
      label: '湿度',
      value: `${weather.value.humidity}%`,
      note: '高德实时天气',
    },
    {
      label: '风速',
      value: `${weather.value.windSpeed.toFixed(1)} m/s`,
      note: `${weather.value.windDirection}风 ${weather.value.windPower}级`,
    },
    {
      label: '能见度',
      value: `${weather.value.visibility.toFixed(1)} km`,
      note: '按天气现象推导',
    },
    {
      label: '降水概率',
      value: `${weather.value.precipitationProbability}%`,
      note: '按实况与当日预报推导',
    },
    {
      label: '降水强度',
      value: `${weather.value.precipitationIntensity.toFixed(1)} mm/h`,
      note: '按天气现象推导',
    },
    {
      label: '雷暴风险',
      value: weather.value.thunderstormRisk,
      note: '按天气现象识别',
    },
  ]
})

const distributionOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['46%', '72%'],
      data: overview.value.projectDistribution,
      label: { formatter: '{b}: {c}' },
    },
  ],
}))

const progressOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: overview.value.progressTrend.map((item) => item.label) },
  yAxis: { type: 'value', max: 100 },
  series: [
    {
      type: 'line',
      smooth: true,
      areaStyle: {},
      data: overview.value.progressTrend.map((item) => item.value),
    },
  ],
}))

async function loadData() {
  loading.value = true
  try {
    overview.value = await fetchAdminOverview()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载系统概览失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

function resolveLocationErrorMessage(error: GeolocationPositionError) {
  switch (error.code) {
    case error.PERMISSION_DENIED:
      return '无法获取当前位置，请允许浏览器定位权限后重试。'
    case error.POSITION_UNAVAILABLE:
      return '当前位置暂时不可用，请检查定位服务后重试。'
    case error.TIMEOUT:
      return '定位超时，请稍后重试。'
    default:
      return '定位失败，请稍后重试。'
  }
}

function locateCurrentPosition(force = false): Promise<Coordinates> {
  if (coordinates.value && !force) {
    return Promise.resolve(coordinates.value)
  }

  if (!navigator.geolocation) {
    return Promise.reject(new Error('当前浏览器不支持定位，无法同步当前位置天气。'))
  }

  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const next = {
          longitude: position.coords.longitude,
          latitude: position.coords.latitude,
        }
        coordinates.value = next
        resolve(next)
      },
      (error) => {
        reject(new Error(resolveLocationErrorMessage(error)))
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 5 * 60 * 1000,
      },
    )
  })
}

async function loadWeather(options: { relocate?: boolean; silent?: boolean } = {}) {
  const { relocate = !coordinates.value, silent = false } = options
  if (silent) {
    weatherRefreshing.value = true
  } else {
    weatherLoading.value = true
  }

  try {
    const currentCoordinates = await locateCurrentPosition(relocate)
    const payload = await fetchAdminRealtimeWeather(currentCoordinates)
    if (!payload) {
      throw new Error('天气服务未返回有效数据，请稍后重试。')
    }
    weather.value = payload
    weatherError.value = ''
  } catch (error) {
    weather.value = null
    weatherError.value = error instanceof Error ? error.message : '天气信息加载失败，请稍后重试。'
  } finally {
    weatherLoading.value = false
    weatherRefreshing.value = false
  }
}

function refreshDashboard() {
  void loadData()
}

onMounted(() => {
  void loadData()
  void loadWeather()
  weatherTimer = window.setInterval(() => {
    void loadWeather({ silent: true })
  }, 15 * 60 * 1000)
})

onBeforeUnmount(() => {
  if (weatherTimer) {
    window.clearInterval(weatherTimer)
  }
})
</script>

<template>
  <PageContainer
    title="总览看板"
    description="查看平台关键指标、项目态势和近期操作动态。"
    back-to="/overview"
    back-label="返回总览"
  >
    <template #actions>
      <el-button type="primary" :loading="loading" @click="refreshDashboard">刷新看板</el-button>
    </template>

    <el-card class="page-card weather-card" shadow="never" v-loading="weatherLoading">
      <template #header>
        <div class="weather-card__header">
          <div>
            <span class="section-title">实时天气与飞行适宜性</span>
            <p class="weather-card__subtitle">
              首页显著展示当前位置气象条件，系统每 15 分钟自动刷新一次。
            </p>
          </div>
          <div class="weather-card__actions">
            <el-tag effect="light">{{ weather?.serviceName ?? '实时天气' }}</el-tag>
            <el-button link type="primary" :loading="weatherRefreshing" @click="loadWeather({ relocate: true })">
              刷新天气
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="weatherError"
        class="weather-card__alert"
        type="warning"
        :closable="false"
        show-icon
        :title="weatherError"
      />

      <div v-if="weather" class="weather-card__body">
        <div class="weather-hero">
          <div class="weather-hero__summary">
            <span class="weather-hero__icon">🌤</span>
            <span class="weather-hero__label">{{ weather.locationName }}</span>
            <div class="weather-hero__temperature">{{ weather.temperature.toFixed(1) }}°C</div>
            <div class="weather-hero__description">
              {{ weather.weather }} | {{ weather.windDirection }}风 {{ weather.windPower }}级
            </div>
            <p class="weather-hero__meta">
              发布时间 {{ weather.reportTime }} | 本次同步 {{ weather.fetchedAt }}
            </p>
          </div>

          <div class="flight-decision" :class="isSuitableToFly ? 'is-suitable' : 'is-unsuitable'">
            <span class="flight-decision__label">飞行判断</span>
            <strong class="flight-decision__result">{{ weather.suitability.result }}</strong>
            <p>{{ weather.suitability.summary }}</p>
            <el-tag :type="isSuitableToFly ? 'success' : 'danger'" effect="dark" round>
              {{ weather.suitability.level }}
            </el-tag>
          </div>
        </div>

        <div class="weather-metrics-grid">
          <div v-for="metric in weatherMetrics" :key="metric.label" class="weather-metric">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
            <small>{{ metric.note }}</small>
          </div>
        </div>

        <div class="flight-checks">
          <div
            v-for="check in weather.suitability.checks"
            :key="check.label"
            class="flight-check"
            :class="check.passed ? 'is-pass' : 'is-fail'"
          >
            <div>
              <span class="flight-check__label">{{ check.label }}</span>
              <strong>{{ check.currentValue }}</strong>
            </div>
            <span class="flight-check__threshold">{{ check.threshold }}</span>
            <el-tag :type="check.passed ? 'success' : 'danger'" effect="light" round>
              {{ check.passed ? '达标' : '预警' }}
            </el-tag>
          </div>
        </div>

        <div class="weather-guidance">
          <div class="weather-guidance__panel">
            <h4>气象条件说明</h4>
            <ul>
              <li v-for="note in weather.suitability.conditionNotes" :key="note">{{ note }}</li>
            </ul>
          </div>
          <div class="weather-guidance__panel">
            <h4>飞行建议</h4>
            <ul>
              <li v-for="tip in weather.suitability.recommendations" :key="tip">{{ tip }}</li>
            </ul>
          </div>
        </div>

        <p class="weather-source-note">{{ weather.sourceNote }}</p>
      </div>
    </el-card>

    <div class="metrics-grid">
      <StatCard v-for="metric in overview.metrics" :key="metric.label" :metric="metric" />
    </div>

    <div class="dashboard-grid" v-loading="loading">
      <EChartCard title="项目区域分布" :option="distributionOption" />
      <EChartCard title="项目进度趋势" :option="progressOption" />

      <el-card class="page-card" shadow="never">
        <template #header>
          <span class="section-title">设备与终端态势</span>
        </template>
        <div class="metrics-grid compact-grid">
          <StatCard v-for="metric in overview.deviceStats" :key="metric.label" :metric="metric" />
        </div>
      </el-card>

      <el-card class="page-card" shadow="never">
        <template #header>
          <span class="section-title">近期关键动作</span>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="activity in overview.activities"
            :key="`${activity.title}-${activity.time}`"
            :timestamp="activity.time"
          >
            <strong>{{ activity.title }}</strong>
            <p>{{ activity.content }}</p>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-card class="page-card" shadow="never">
        <template #header>
          <span class="section-title">系统通知</span>
        </template>
        <div class="notice-list">
          <div v-for="notice in overview.notices" :key="notice.title" class="notice-item">
            <div>
              <strong>{{ notice.title }}</strong>
              <p>{{ notice.time }}</p>
            </div>
            <el-tag :type="notice.level === '高' ? 'danger' : notice.level === '中' ? 'warning' : 'info'">
              {{ notice.level }}
            </el-tag>
          </div>
        </div>
      </el-card>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.weather-card {
  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
}

.weather-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.weather-card__subtitle {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-medium);
}

.weather-card__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.weather-card__alert {
  margin-bottom: -4px;
}

.weather-card__body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.weather-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(320px, 1fr);
  gap: 20px;
  align-items: stretch;
}

.weather-hero__summary,
.flight-decision {
  border-radius: 18px;
  padding: 24px;
}

.weather-hero__summary {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  display: flex;
  flex-direction: column;
}

.weather-hero__icon {
  font-size: calc(var(--font-large) * 2);
  line-height: 1;
  margin-bottom: 8px;
}

.weather-hero__label {
  display: inline-flex;
  margin-bottom: 12px;
  color: #1d4ed8;
  font-size: var(--font-medium);
  font-weight: 600;
}

.weather-hero__temperature {
  font-size: calc(var(--font-large) * 2.2);
  line-height: 1;
  font-weight: 700;
  color: var(--color-text-primary);
}

.weather-hero__description {
  margin-top: 12px;
  font-size: var(--font-large);
  color: var(--color-text-secondary);
}

.weather-hero__meta {
  margin: 12px 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-medium);
}

.flight-decision {
  display: flex;
  flex-direction: column;
  gap: 12px;

  p {
    margin: 0;
    line-height: 1.7;
  }
}

.flight-decision.is-suitable {
  background: var(--color-bg-success);
  color: #166534;
}

.flight-decision.is-unsuitable {
  background: var(--color-bg-danger);
  color: #991b1b;
}

.flight-decision__label {
  font-size: var(--font-medium);
  letter-spacing: 0.08em;
  opacity: 0.88;
}

.flight-decision__result {
  font-size: calc(var(--font-large) * 1.7);
  line-height: 1.1;
}

.weather-metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.weather-metric {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 14px;
  background: var(--color-bg-section);
  border: 1px solid var(--color-border-light);

  span {
    color: var(--color-text-primary);
    font-size: var(--font-large);
    font-weight: 600;
  }

  strong {
    font-size: var(--font-medium);
    color: var(--color-text-secondary);
  }

  small {
    color: var(--color-text-caption);
    font-size: var(--font-small);
  }
}

.flight-checks {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.flight-check {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid transparent;
  background: var(--color-bg-card);

  strong {
    display: block;
    margin-top: 6px;
    font-size: var(--font-large);
    color: var(--color-text-primary);
  }
}

.flight-check.is-pass {
  border-color: rgba(22, 163, 74, 0.2);
  background: var(--color-bg-success);
}

.flight-check.is-fail {
  border-color: rgba(220, 38, 38, 0.18);
  background: var(--color-bg-danger);
}

.flight-check__label,
.flight-check__threshold {
  color: var(--color-text-muted);
  font-size: var(--font-medium);
}

.weather-guidance {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.weather-guidance__panel {
  padding: 20px;
  border-radius: 16px;
  background: var(--color-bg-section);

  h4 {
    margin: 0 0 12px;
    font-size: var(--font-large);
    color: var(--color-text-primary);
  }

  ul {
    margin: 0;
    padding-left: 18px;
    color: var(--color-text-secondary);
    line-height: 1.8;
  }
}

.weather-source-note {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-small);
  line-height: 1.7;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.compact-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);

  &:last-child {
    padding-bottom: 0;
    border-bottom: none;
  }

  p {
    margin: 6px 0 0;
    color: var(--color-text-muted);
    font-size: var(--font-medium);
  }
}

@media (max-width: 1080px) {
  .weather-hero,
  .weather-guidance,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .flight-check {
    align-items: flex-start;
    flex-direction: column;
  }

  .weather-hero__temperature {
    font-size: calc(var(--font-large) * 1.8);
  }
}
</style>
