<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon primary">
              <el-icon><List /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">{{ t('dashboard.jobInfoCount') }}</div>
              <div class="stat-value">{{ stats.jobInfoCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon warning">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">{{ t('dashboard.jobLogCount') }}</div>
              <div class="stat-value">{{ stats.jobLogCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon success">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">{{ t('dashboard.executorCount') }}</div>
              <div class="stat-value">{{ stats.executorCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ t('dashboard.triggerTrend') }}</span>
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="-"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                size="small"
                @change="loadChartData"
              />
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>{{ t('dashboard.triggerPie') }}</span>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import { getDashboardInfo, getChartInfo } from '@/api/auth'
import dayjs from 'dayjs'

const { t } = useI18n()

const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
const dateRange = ref<[Date, Date]>([
  dayjs().subtract(1, 'weeks').startOf('day').toDate(),
  dayjs().endOf('day').toDate()
])

const stats = reactive({
  jobInfoCount: 0,
  jobLogCount: 0,
  executorCount: 0
})

let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

async function loadDashboardInfo() {
  try {
    const res = await getDashboardInfo()
    if (res) {
      Object.assign(stats, res)
    }
  } catch (error) {
    console.error('Failed to load dashboard info:', error)
  }
}

async function loadChartData() {
  try {
    const startDate = dayjs(dateRange.value[0]).format('YYYY-MM-DD HH:mm:ss')
    const endDate = dayjs(dateRange.value[1]).format('YYYY-MM-DD HH:mm:ss')
    const res = await getChartInfo(startDate, endDate)
    
    if (res) {
      updateTrendChart(res)
      updatePieChart(res)
    }
  } catch (error) {
    console.error('Failed to load chart data:', error)
  }
}

function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  
  const option: echarts.EChartsOption = {
    title: { text: t('dashboard.triggerTrend') },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: { backgroundColor: '#6a7985' }
      }
    },
    legend: {
      data: [t('dashboard.triggerSuc'), t('dashboard.triggerFail'), t('dashboard.triggerRunning')]
    },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: [] },
    yAxis: { type: 'value' },
    series: [
      {
        name: t('dashboard.triggerSuc'),
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        data: [],
        itemStyle: { color: '#00A65A' }
      },
      {
        name: t('dashboard.triggerFail'),
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        data: [],
        itemStyle: { color: '#c23632' }
      },
      {
        name: t('dashboard.triggerRunning'),
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        data: [],
        itemStyle: { color: '#F39C12' }
      }
    ]
  }
  
  trendChart.setOption(option)
}

function initPieChart() {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  
  const option: echarts.EChartsOption = {
    title: { text: t('dashboard.triggerPie'), left: 'center' },
    tooltip: { trigger: 'item', formatter: '{b} : {c} ({d}%)' },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: [t('dashboard.triggerSuc'), t('dashboard.triggerFail'), t('dashboard.triggerRunning')]
    },
    series: [
      {
        type: 'pie',
        radius: '55%',
        center: ['50%', '60%'],
        data: [],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ],
    color: ['#00A65A', '#c23632', '#F39C12']
  }
  
  pieChart.setOption(option)
}

function updateTrendChart(data: any) {
  if (!trendChart) return
  trendChart.setOption({
    xAxis: { data: data.triggerDayList || [] },
    series: [
      { data: data.triggerDayCountSucList || [] },
      { data: data.triggerDayCountFailList || [] },
      { data: data.triggerDayCountRunningList || [] }
    ]
  })
}

function updatePieChart(data: any) {
  if (!pieChart) return
  pieChart.setOption({
    series: [{
      data: [
        { name: t('dashboard.triggerSuc'), value: data.triggerCountSucTotal || 0 },
        { name: t('dashboard.triggerFail'), value: data.triggerCountFailTotal || 0 },
        { name: t('dashboard.triggerRunning'), value: data.triggerCountRunningTotal || 0 }
      ]
    }]
  })
}

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  await nextTick()
  initTrendChart()
  initPieChart()
  await loadDashboardInfo()
  await loadChartData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  .stat-cards {
    margin-bottom: 20px;
  }
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  
  .el-icon {
    font-size: 30px;
    color: #fff;
  }
  
  &.primary { background: linear-gradient(135deg, #409eff, #66b1ff); }
  &.success { background: linear-gradient(135deg, #67c23a, #85ce61); }
  &.warning { background: linear-gradient(135deg, #e6a23c, #ebb563); }
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 350px;
}
</style>
