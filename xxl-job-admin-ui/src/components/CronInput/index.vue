<template>
  <div class="cron-input-container">
    <el-input
      v-model="cronExpression"
      placeholder="请输入 Cron 表达式"
      clearable
      @change="handleInputChange"
    >
      <template #append>
        <el-button @click="showDialog = true">
          <el-icon><Setting /></el-icon>
        </el-button>
      </template>
    </el-input>

    <el-dialog
      v-model="showDialog"
      title="Cron 表达式生成器"
      width="520px"
      :close-on-click-modal="false"
      class="cron-dialog"
    >
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 秒 -->
        <el-tab-pane label="秒" name="second">
          <div class="tab-content">
            <div class="option-item">
              <el-radio v-model="second.type" :value="1" @change="generateCron">
                每秒 允许的通配符[, - * /]
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="second.type" :value="2" @change="generateCron">
                周期 从
              </el-radio>
              <el-input-number 
                v-model="second.cycleStart" 
                :min="0" :max="59" 
                size="small" 
                class="input-short"
                @change="second.type = 2; generateCron()" 
              />
              <span class="separator">-</span>
              <el-input-number 
                v-model="second.cycleEnd" 
                :min="0" :max="59" 
                size="small" 
                class="input-short"
                @change="second.type = 2; generateCron()" 
              />
              <span class="unit">秒</span>
            </div>
            <div class="option-item">
              <el-radio v-model="second.type" :value="3" @change="generateCron">
                从
              </el-radio>
              <el-input-number 
                v-model="second.startAt" 
                :min="0" :max="59" 
                size="small" 
                class="input-short"
                @change="second.type = 3; generateCron()" 
              />
              <span class="unit">秒开始,每</span>
              <el-input-number 
                v-model="second.interval" 
                :min="1" :max="59" 
                size="small" 
                class="input-short"
                @change="second.type = 3; generateCron()" 
              />
              <span class="unit">秒执行一次</span>
            </div>
            <div class="option-item">
              <el-radio v-model="second.type" :value="4" @change="generateCron">
                指定
              </el-radio>
            </div>
            <div class="checkbox-grid" v-show="second.type === 4">
              <el-checkbox-group v-model="second.appoint" @change="second.type = 4; generateCron()">
                <div v-for="row in 6" :key="row" class="checkbox-row">
                  <el-checkbox 
                    v-for="col in 10" 
                    :key="(row-1)*10 + (col-1)" 
                    :value="(row-1)*10 + (col-1)"
                  >
                    {{ padZero((row-1)*10 + (col-1)) }}
                  </el-checkbox>
                </div>
              </el-checkbox-group>
            </div>
          </div>
        </el-tab-pane>

        <!-- 分钟 -->
        <el-tab-pane label="分钟" name="minute">
          <div class="tab-content">
            <div class="option-item">
              <el-radio v-model="minute.type" :value="1" @change="generateCron">
                每分钟 允许的通配符[, - * /]
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="minute.type" :value="2" @change="generateCron">
                周期 从
              </el-radio>
              <el-input-number 
                v-model="minute.cycleStart" 
                :min="0" :max="59" 
                size="small" 
                class="input-short"
                @change="minute.type = 2; generateCron()" 
              />
              <span class="separator">-</span>
              <el-input-number 
                v-model="minute.cycleEnd" 
                :min="0" :max="59" 
                size="small" 
                class="input-short"
                @change="minute.type = 2; generateCron()" 
              />
              <span class="unit">分钟</span>
            </div>
            <div class="option-item">
              <el-radio v-model="minute.type" :value="3" @change="generateCron">
                从
              </el-radio>
              <el-input-number 
                v-model="minute.startAt" 
                :min="0" :max="59" 
                size="small" 
                class="input-short"
                @change="minute.type = 3; generateCron()" 
              />
              <span class="unit">分钟开始,每</span>
              <el-input-number 
                v-model="minute.interval" 
                :min="1" :max="59" 
                size="small" 
                class="input-short"
                @change="minute.type = 3; generateCron()" 
              />
              <span class="unit">分钟执行一次</span>
            </div>
            <div class="option-item">
              <el-radio v-model="minute.type" :value="4" @change="generateCron">
                指定
              </el-radio>
            </div>
            <div class="checkbox-grid" v-show="minute.type === 4">
              <el-checkbox-group v-model="minute.appoint" @change="minute.type = 4; generateCron()">
                <div v-for="row in 6" :key="row" class="checkbox-row">
                  <el-checkbox 
                    v-for="col in 10" 
                    :key="(row-1)*10 + (col-1)" 
                    :value="(row-1)*10 + (col-1)"
                  >
                    {{ padZero((row-1)*10 + (col-1)) }}
                  </el-checkbox>
                </div>
              </el-checkbox-group>
            </div>
          </div>
        </el-tab-pane>

        <!-- 小时 -->
        <el-tab-pane label="小时" name="hour">
          <div class="tab-content">
            <div class="option-item">
              <el-radio v-model="hour.type" :value="1" @change="generateCron">
                每小时 允许的通配符[, - * /]
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="hour.type" :value="2" @change="generateCron">
                周期 从
              </el-radio>
              <el-input-number 
                v-model="hour.cycleStart" 
                :min="0" :max="23" 
                size="small" 
                class="input-short"
                @change="hour.type = 2; generateCron()" 
              />
              <span class="separator">-</span>
              <el-input-number 
                v-model="hour.cycleEnd" 
                :min="0" :max="23" 
                size="small" 
                class="input-short"
                @change="hour.type = 2; generateCron()" 
              />
              <span class="unit">小时</span>
            </div>
            <div class="option-item">
              <el-radio v-model="hour.type" :value="3" @change="generateCron">
                从
              </el-radio>
              <el-input-number 
                v-model="hour.startAt" 
                :min="0" :max="23" 
                size="small" 
                class="input-short"
                @change="hour.type = 3; generateCron()" 
              />
              <span class="unit">小时开始,每</span>
              <el-input-number 
                v-model="hour.interval" 
                :min="1" :max="23" 
                size="small" 
                class="input-short"
                @change="hour.type = 3; generateCron()" 
              />
              <span class="unit">小时执行一次</span>
            </div>
            <div class="option-item">
              <el-radio v-model="hour.type" :value="4" @change="generateCron">
                指定
              </el-radio>
            </div>
            <div class="checkbox-grid" v-show="hour.type === 4">
              <el-checkbox-group v-model="hour.appoint" @change="hour.type = 4; generateCron()">
                <div v-for="row in 3" :key="row" class="checkbox-row">
                  <el-checkbox 
                    v-for="col in 8" 
                    :key="(row-1)*8 + (col-1)" 
                    :value="(row-1)*8 + (col-1)"
                  >
                    {{ padZero((row-1)*8 + (col-1)) }}
                  </el-checkbox>
                </div>
              </el-checkbox-group>
            </div>
          </div>
        </el-tab-pane>

        <!-- 日 -->
        <el-tab-pane label="日" name="day">
          <div class="tab-content">
            <div class="option-item">
              <el-radio v-model="day.type" :value="1" @change="generateCron">
                每天 允许的通配符[, - * / L W]
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="day.type" :value="2" @change="generateCron">
                不指定
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="day.type" :value="3" @change="generateCron">
                周期 从
              </el-radio>
              <el-input-number 
                v-model="day.cycleStart" 
                :min="1" :max="31" 
                size="small" 
                class="input-short"
                @change="day.type = 3; generateCron()" 
              />
              <span class="separator">-</span>
              <el-input-number 
                v-model="day.cycleEnd" 
                :min="1" :max="31" 
                size="small" 
                class="input-short"
                @change="day.type = 3; generateCron()" 
              />
              <span class="unit">日</span>
            </div>
            <div class="option-item">
              <el-radio v-model="day.type" :value="4" @change="generateCron">
                从
              </el-radio>
              <el-input-number 
                v-model="day.startAt" 
                :min="1" :max="31" 
                size="small" 
                class="input-short"
                @change="day.type = 4; generateCron()" 
              />
              <span class="unit">日开始,每</span>
              <el-input-number 
                v-model="day.interval" 
                :min="1" :max="31" 
                size="small" 
                class="input-short"
                @change="day.type = 4; generateCron()" 
              />
              <span class="unit">天执行一次</span>
            </div>
            <div class="option-item">
              <el-radio v-model="day.type" :value="5" @change="generateCron">
                每月
              </el-radio>
              <el-input-number 
                v-model="day.workDay" 
                :min="1" :max="31" 
                size="small" 
                class="input-short"
                @change="day.type = 5; generateCron()" 
              />
              <span class="unit">号最近的那个工作日</span>
            </div>
            <div class="option-item">
              <el-radio v-model="day.type" :value="6" @change="generateCron">
                本月最后一天
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="day.type" :value="7" @change="generateCron">
                指定
              </el-radio>
            </div>
            <div class="checkbox-grid" v-show="day.type === 7">
              <el-checkbox-group v-model="day.appoint" @change="day.type = 7; generateCron()">
                <div v-for="row in 4" :key="row" class="checkbox-row">
                  <template v-for="col in 8" :key="(row-1)*8 + col">
                    <el-checkbox 
                      v-if="(row-1)*8 + col <= 31"
                      :value="(row-1)*8 + col"
                    >
                      {{ padZero((row-1)*8 + col) }}
                    </el-checkbox>
                  </template>
                </div>
              </el-checkbox-group>
            </div>
          </div>
        </el-tab-pane>

        <!-- 月 -->
        <el-tab-pane label="月" name="month">
          <div class="tab-content">
            <div class="option-item">
              <el-radio v-model="month.type" :value="1" @change="generateCron">
                每月 允许的通配符[, - * /]
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="month.type" :value="2" @change="generateCron">
                不指定
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="month.type" :value="3" @change="generateCron">
                周期 从
              </el-radio>
              <el-input-number 
                v-model="month.cycleStart" 
                :min="1" :max="12" 
                size="small" 
                class="input-short"
                @change="month.type = 3; generateCron()" 
              />
              <span class="separator">-</span>
              <el-input-number 
                v-model="month.cycleEnd" 
                :min="1" :max="12" 
                size="small" 
                class="input-short"
                @change="month.type = 3; generateCron()" 
              />
              <span class="unit">月</span>
            </div>
            <div class="option-item">
              <el-radio v-model="month.type" :value="4" @change="generateCron">
                从
              </el-radio>
              <el-input-number 
                v-model="month.startAt" 
                :min="1" :max="12" 
                size="small" 
                class="input-short"
                @change="month.type = 4; generateCron()" 
              />
              <span class="unit">月开始,每</span>
              <el-input-number 
                v-model="month.interval" 
                :min="1" :max="12" 
                size="small" 
                class="input-short"
                @change="month.type = 4; generateCron()" 
              />
              <span class="unit">月执行一次</span>
            </div>
            <div class="option-item">
              <el-radio v-model="month.type" :value="5" @change="generateCron">
                指定
              </el-radio>
            </div>
            <div class="checkbox-grid" v-show="month.type === 5">
              <el-checkbox-group v-model="month.appoint" @change="month.type = 5; generateCron()">
                <div class="checkbox-row">
                  <el-checkbox v-for="i in 12" :key="i" :value="i">
                    {{ padZero(i) }}
                  </el-checkbox>
                </div>
              </el-checkbox-group>
            </div>
          </div>
        </el-tab-pane>

        <!-- 周 -->
        <el-tab-pane label="周" name="week">
          <div class="tab-content">
            <div class="option-item">
              <el-radio v-model="week.type" :value="1" @change="generateCron">
                每周 允许的通配符[, - * / L #]
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="week.type" :value="2" @change="generateCron">
                不指定
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="week.type" :value="3" @change="generateCron">
                周期 从星期
              </el-radio>
              <el-input-number 
                v-model="week.cycleStart" 
                :min="1" :max="7" 
                size="small" 
                class="input-short"
                @change="week.type = 3; generateCron()" 
              />
              <span class="separator">-</span>
              <el-input-number 
                v-model="week.cycleEnd" 
                :min="1" :max="7" 
                size="small" 
                class="input-short"
                @change="week.type = 3; generateCron()" 
              />
            </div>
            <div class="option-item">
              <el-radio v-model="week.type" :value="4" @change="generateCron">
                第
              </el-radio>
              <el-input-number 
                v-model="week.weekOfMonth" 
                :min="1" :max="5" 
                size="small" 
                class="input-short"
                @change="week.type = 4; generateCron()" 
              />
              <span class="unit">周的星期</span>
              <el-input-number 
                v-model="week.dayOfWeek" 
                :min="1" :max="7" 
                size="small" 
                class="input-short"
                @change="week.type = 4; generateCron()" 
              />
            </div>
            <div class="option-item">
              <el-radio v-model="week.type" :value="5" @change="generateCron">
                本月最后一个星期
              </el-radio>
              <el-input-number 
                v-model="week.lastDay" 
                :min="1" :max="7" 
                size="small" 
                class="input-short"
                @change="week.type = 5; generateCron()" 
              />
            </div>
            <div class="option-item">
              <el-radio v-model="week.type" :value="6" @change="generateCron">
                指定
              </el-radio>
            </div>
            <div class="checkbox-grid" v-show="week.type === 6">
              <el-checkbox-group v-model="week.appoint" @change="week.type = 6; generateCron()">
                <div class="checkbox-row">
                  <el-checkbox v-for="i in 7" :key="i" :value="i">
                    {{ weekLabels[i] }}
                  </el-checkbox>
                </div>
              </el-checkbox-group>
            </div>
          </div>
        </el-tab-pane>

        <!-- 年 -->
        <el-tab-pane label="年" name="year">
          <div class="tab-content">
            <div class="option-item">
              <el-radio v-model="year.type" :value="1" @change="generateCron">
                不指定 允许的通配符[, - * /] 非必填
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="year.type" :value="2" @change="generateCron">
                每年
              </el-radio>
            </div>
            <div class="option-item">
              <el-radio v-model="year.type" :value="3" @change="generateCron">
                周期从
              </el-radio>
              <el-input-number 
                v-model="year.cycleStart" 
                :min="2024" :max="2099" 
                size="small" 
                class="input-medium"
                @change="year.type = 3; generateCron()" 
              />
              <span class="separator">-</span>
              <el-input-number 
                v-model="year.cycleEnd" 
                :min="2024" :max="2099" 
                size="small" 
                class="input-medium"
                @change="year.type = 3; generateCron()" 
              />
              <span class="unit">年</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <el-divider style="margin: 8px 0" />

      <div class="cron-preview">
        <div class="preview-header">
          <span class="preview-label">生成的 Cron 表达式：</span>
          <el-tag type="primary" size="large" class="cron-tag">{{ cronExpression }}</el-tag>
        </div>
        <div v-if="nextTriggerTimes.length > 0" class="next-trigger-times">
          <div class="preview-label">最近运行时间：</div>
          <div class="time-list">
            <el-tag v-for="(time, index) in nextTriggerTimes" :key="index" class="time-item">
              {{ time }}
            </el-tag>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { jobinfoApi } from '@/api/jobinfo'

interface Props {
  modelValue?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '0 0 0 * * ? *'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const cronExpression = ref(props.modelValue)
const showDialog = ref(false)
const activeTab = ref('second')
const nextTriggerTimes = ref<string[]>([])

// 各字段配置
const second = reactive({
  type: 1,
  cycleStart: 0,
  cycleEnd: 1,
  startAt: 0,
  interval: 1,
  appoint: [] as number[]
})

const minute = reactive({
  type: 1,
  cycleStart: 0,
  cycleEnd: 1,
  startAt: 0,
  interval: 1,
  appoint: [] as number[]
})

const hour = reactive({
  type: 1,
  cycleStart: 0,
  cycleEnd: 1,
  startAt: 0,
  interval: 1,
  appoint: [] as number[]
})

const day = reactive({
  type: 1,
  cycleStart: 1,
  cycleEnd: 2,
  startAt: 1,
  interval: 1,
  workDay: 1,
  appoint: [] as number[]
})

const month = reactive({
  type: 1,
  cycleStart: 1,
  cycleEnd: 2,
  startAt: 1,
  interval: 1,
  appoint: [] as number[]
})

const week = reactive({
  type: 2,
  cycleStart: 1,
  cycleEnd: 2,
  weekOfMonth: 1,
  dayOfWeek: 1,
  lastDay: 1,
  appoint: [] as number[]
})

const year = reactive({
  type: 1,
  cycleStart: 2024,
  cycleEnd: 2025
})

const weekLabels: Record<number, string> = {
  1: '周日',
  2: '周一',
  3: '周二',
  4: '周三',
  5: '周四',
  6: '周五',
  7: '周六'
}

function padZero(num: number): string {
  return num.toString().padStart(2, '0')
}

// 解析 Cron 表达式到各字段
function parseCronExpression(expr: string) {
  if (!expr) return

  const parts = expr.split(' ')
  if (parts.length < 6) return

  const [sec, min, hr, dy, mn, wk, yr] = parts

  // 解析秒
  parseField(sec, second)
  // 解析分
  parseField(min, minute)
  // 解析时
  parseField(hr, hour)
  // 解析日
  parseDayField(dy)
  // 解析月
  parseField(mn, month)
  // 解析周
  parseWeekField(wk)
  // 解析年
  if (yr) {
    if (yr === '*') {
      year.type = 2
    } else if (yr.includes('-')) {
      year.type = 3
      const [start, end] = yr.split('-')
      year.cycleStart = parseInt(start)
      year.cycleEnd = parseInt(end)
    }
  }
}

function parseField(value: string, field: any) {
  if (value === '*') {
    field.type = 1
  } else if (value.includes('-')) {
    field.type = 2
    const [start, end] = value.split('-')
    field.cycleStart = parseInt(start)
    field.cycleEnd = parseInt(end)
  } else if (value.includes('/')) {
    field.type = 3
    const [start, interval] = value.split('/')
    field.startAt = parseInt(start)
    field.interval = parseInt(interval)
  } else if (value !== '?') {
    field.type = 4
    field.appoint = value.split(',').map(Number)
  }
}

function parseDayField(value: string) {
  if (value === '*') {
    day.type = 1
  } else if (value === '?') {
    day.type = 2
  } else if (value === 'L') {
    day.type = 6
  } else if (value.endsWith('W')) {
    day.type = 5
    day.workDay = parseInt(value)
  } else if (value.includes('-')) {
    day.type = 3
    const [start, end] = value.split('-')
    day.cycleStart = parseInt(start)
    day.cycleEnd = parseInt(end)
  } else if (value.includes('/')) {
    day.type = 4
    const [start, interval] = value.split('/')
    day.startAt = parseInt(start)
    day.interval = parseInt(interval)
  } else {
    day.type = 7
    day.appoint = value.split(',').map(Number)
  }
}

function parseWeekField(value: string) {
  if (value === '*') {
    week.type = 1
  } else if (value === '?') {
    week.type = 2
  } else if (value.endsWith('L')) {
    week.type = 5
    week.lastDay = parseInt(value)
  } else if (value.includes('#')) {
    week.type = 4
    const [day, weekNum] = value.split('#')
    week.dayOfWeek = parseInt(day)
    week.weekOfMonth = parseInt(weekNum)
  } else if (value.includes('-')) {
    week.type = 3
    const [start, end] = value.split('-')
    week.cycleStart = parseInt(start)
    week.cycleEnd = parseInt(end)
  } else {
    week.type = 6
    week.appoint = value.split(',').map(Number)
  }
}

// 生成 Cron 表达式
function generateCron() {
  const parts = [
    generateField(second),
    generateField(minute),
    generateField(hour),
    generateDayField(),
    generateField(month),
    generateWeekField(),
    generateYearField()
  ]

  cronExpression.value = parts.join(' ')
  emit('update:modelValue', cronExpression.value)
  fetchNextTriggerTimes()
}

function generateField(field: any): string {
  switch (field.type) {
    case 1:
      return '*'
    case 2:
      return `${field.cycleStart}-${field.cycleEnd}`
    case 3:
      return `${field.startAt}/${field.interval}`
    case 4:
      return field.appoint.length > 0 ? field.appoint.join(',') : '*'
    default:
      return '*'
  }
}

function generateDayField(): string {
  switch (day.type) {
    case 1:
      return '*'
    case 2:
      return '?'
    case 3:
      return `${day.cycleStart}-${day.cycleEnd}`
    case 4:
      return `${day.startAt}/${day.interval}`
    case 5:
      return `${day.workDay}W`
    case 6:
      return 'L'
    case 7:
      return day.appoint.length > 0 ? day.appoint.join(',') : '*'
    default:
      return '*'
  }
}

function generateWeekField(): string {
  switch (week.type) {
    case 1:
      return '*'
    case 2:
      return '?'
    case 3:
      return `${week.cycleStart}-${week.cycleEnd}`
    case 4:
      return `${week.dayOfWeek}#${week.weekOfMonth}`
    case 5:
      return `${week.lastDay}L`
    case 6:
      return week.appoint.length > 0 ? week.appoint.join(',') : '?'
    default:
      return '?'
  }
}

function generateYearField(): string {
  switch (year.type) {
    case 1:
      return ''
    case 2:
      return '*'
    case 3:
      return `${year.cycleStart}-${year.cycleEnd}`
    default:
      return ''
  }
}

function handleInputChange(val: string) {
  cronExpression.value = val
  emit('update:modelValue', val)
}

function handleTabChange() {
  // Tab 切换时不做特殊处理
}

async function fetchNextTriggerTimes() {
  if (!cronExpression.value) return
  try {
    const res = await jobinfoApi.getNextTriggerTime('CRON', cronExpression.value)
    nextTriggerTimes.value = res || []
  } catch (error) {
    nextTriggerTimes.value = []
  }
}

function handleConfirm() {
  emit('update:modelValue', cronExpression.value)
  showDialog.value = false
}

watch(() => props.modelValue, (val) => {
  cronExpression.value = val
})

watch(showDialog, (val) => {
  if (val) {
    parseCronExpression(cronExpression.value)
    fetchNextTriggerTimes()
  }
})
</script>

<style scoped lang="scss">
.cron-input-container {
  width: 100%;
}

.cron-dialog {
  .el-dialog__body {
    padding: 10px 15px;
  }
}

.tab-content {
  padding: 5px 0;
}

.option-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  gap: 4px;
  flex-wrap: wrap;
}

.input-short {
  width: 110px;
}

.input-medium {
  width: 120px;
}

.separator {
  margin: 0 2px;
  color: #909399;
}

.unit {
  color: #606266;
  font-size: 13px;
}

.checkbox-grid {
  margin-top: 8px;
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.checkbox-row {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 4px;
  
  &:last-child {
    margin-bottom: 0;
  }

  .el-checkbox {
    margin-right: 8px;
    margin-bottom: 2px;
    min-width: 38px;
  }
}

.cron-preview {
  .preview-header {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
  }

  .preview-label {
    font-weight: 500;
    color: #303133;
    margin-right: 8px;
    font-size: 13px;
  }

  .cron-tag {
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 14px;
  }

  .next-trigger-times {
    margin-top: 8px;

    .time-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 6px;
    }
  }
}
</style>
