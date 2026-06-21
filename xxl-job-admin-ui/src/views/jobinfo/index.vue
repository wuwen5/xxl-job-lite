<template>
  <div class="jobinfo-container">
    <!-- 搜索表单 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('jobinfo.jobGroup')">
          <el-select v-model="searchForm.jobGroup" :placeholder="t('common.all')" style="width: 200px">
            <el-option v-if="isAdmin" :label="t('common.all')" :value="0" />
            <el-option
              v-for="item in jobGroups"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('jobinfo.triggerStatus')">
          <el-select v-model="searchForm.triggerStatus" :placeholder="t('common.all')" style="width: 120px">
            <el-option :label="t('common.all')" :value="-1" />
            <el-option :label="t('common.enable')" :value="1" />
            <el-option :label="t('common.disable')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('jobinfo.jobDesc')">
          <el-input v-model="searchForm.jobDesc" placeholder="" clearable />
        </el-form-item>
        <el-form-item :label="t('jobinfo.executorHandler')">
          <el-input v-model="searchForm.executorHandler" placeholder="" clearable />
        </el-form-item>
        <el-form-item :label="t('jobinfo.author')">
          <el-input v-model="searchForm.author" placeholder="" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作按钮 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('menu.jobinfo') }}</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            {{ t('common.add') }}
          </el-button>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="jobDesc" :label="t('jobinfo.jobDesc')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="t('jobinfo.scheduleType')" width="150" align="center">
          <template #default="{ row }">
            {{ row.scheduleConf ? `${row.scheduleType}：${row.scheduleConf}` : row.scheduleType }}
          </template>
        </el-table-column>
        <el-table-column :label="t('jobinfo.glueType')" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.executorHandler ? `${getGlueTypeLabel(row.glueType)}：${row.executorHandler}` : getGlueTypeLabel(row.glueType) }}
          </template>
        </el-table-column>
        <el-table-column prop="author" :label="t('jobinfo.author')" width="100" align="center" />
        <el-table-column :label="t('jobinfo.triggerStatus')" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.triggerStatus"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="t('common.operation')" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> {{ t('common.edit') }}
            </el-button>
            <el-button type="primary" link size="small" @click="handleTrigger(row)">
              <el-icon><CaretRight /></el-icon> {{ t('common.trigger') }}
            </el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleDropdownCommand(cmd, row)">
              <el-button type="primary" link size="small">
                {{ t('common.more') }} <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="log">
                    <el-icon><Document /></el-icon> {{ t('menu.joblog') }}
                  </el-dropdown-item>
                  <el-dropdown-item v-if="row.scheduleType === 'CRON' || row.scheduleType === 'FIX_RATE'" command="nextTime">
                    <el-icon><Clock /></el-icon> {{ t('jobinfo.nextTriggerTime') }}
                  </el-dropdown-item>
                  <el-dropdown-item command="copy">
                    <el-icon><CopyDocument /></el-icon> {{ t('common.copy') }}
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <el-icon><Delete /></el-icon> {{ t('common.delete') }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-tabs v-model="activeTab">
          <el-tab-pane :label="t('jobinfo.jobDesc')" name="basic">
            <el-form-item :label="t('jobinfo.jobGroup')" prop="jobGroup">
              <el-select v-model="formData.jobGroup" style="width: 100%">
                <el-option
                  v-for="item in jobGroups"
                  :key="item.id"
                  :label="item.title"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('jobinfo.jobDesc')" prop="jobDesc">
              <el-input v-model="formData.jobDesc" />
            </el-form-item>
            <el-form-item :label="t('jobinfo.author')" prop="author">
              <el-input v-model="formData.author" />
            </el-form-item>
            <el-form-item :label="t('jobinfo.alarmEmail')">
              <el-input v-model="formData.alarmEmail" />
            </el-form-item>
          </el-tab-pane>
          
          <el-tab-pane :label="t('jobinfo.scheduleType')" name="schedule">
            <el-form-item :label="t('jobinfo.scheduleType')" prop="scheduleType">
              <el-radio-group v-model="formData.scheduleType" @change="handleScheduleTypeChange">
                <el-radio-button value="CRON">CRON</el-radio-button>
                <el-radio-button value="FIX_RATE">{{ t('jobinfo.fixRate') }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item :label="t('jobinfo.scheduleConf')" prop="scheduleConf">
              <cron-input
                v-if="formData.scheduleType === 'CRON'"
                v-model="formData.scheduleConf"
                placeholder="请输入 Cron 表达式"
              />
              <el-input
                v-else
                v-model="formData.scheduleConf"
                :placeholder="scheduleConfPlaceholder"
              />
            </el-form-item>
            <el-form-item :label="t('jobinfo.misfireStrategy')" prop="misfireStrategy">
              <el-radio-group v-model="formData.misfireStrategy">
                <el-radio value="DO_NOTHING">{{ t('jobinfo.misfireDoNothing') }}</el-radio>
                <el-radio value="FIRE_ONCE_NOW">{{ t('jobinfo.misfireFireOnceNow') }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-tab-pane>
          
          <el-tab-pane :label="t('jobinfo.executorHandler')" name="executor">
            <el-form-item :label="t('jobinfo.glueType')" prop="glueType">
              <el-select v-model="formData.glueType" style="width: 100%">
                <el-option label="BEAN" value="BEAN" />
                <el-option label="GLUE(Groovy)" value="GLUE_GROOVY" />
                <el-option label="GLUE(Shell)" value="GLUE_SHELL" />
                <el-option label="GLUE(Python)" value="GLUE_PYTHON" />
                <el-option label="GLUE(PHP)" value="GLUE_PHP" />
                <el-option label="GLUE(Nodejs)" value="GLUE_NODEJS" />
                <el-option label="GLUE(PowerShell)" value="GLUE_POWERSHELL" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('jobinfo.executorHandler')" prop="executorHandler" v-if="formData.glueType === 'BEAN'" required>
              <el-input v-model="formData.executorHandler" />
            </el-form-item>
            <el-form-item :label="t('jobinfo.executorParam')">
              <el-input v-model="formData.executorParam" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item :label="t('jobinfo.executorRouteStrategy')" prop="executorRouteStrategy">
              <el-select v-model="formData.executorRouteStrategy" style="width: 100%">
                <el-option :label="t('jobinfo.routeFirst')" value="FIRST" />
                <el-option :label="t('jobinfo.routeLast')" value="LAST" />
                <el-option :label="t('jobinfo.routeRound')" value="ROUND" />
                <el-option :label="t('jobinfo.routeRandom')" value="RANDOM" />
                <el-option :label="t('jobinfo.routeConsistentHash')" value="CONSISTENT_HASH" />
                <el-option :label="t('jobinfo.routeLFU')" value="LEAST_FREQUENTLY_USED" />
                <el-option :label="t('jobinfo.routeLRU')" value="LEAST_RECENTLY_USED" />
                <el-option :label="t('jobinfo.routeFailover')" value="FAILOVER" />
                <el-option :label="t('jobinfo.routeBusyover')" value="BUSYOVER" />
                <el-option :label="t('jobinfo.routeShard')" value="SHARDING_BROADCAST" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('jobinfo.executorBlockStrategy')">
              <el-select v-model="formData.executorBlockStrategy" style="width: 100%">
                <el-option :label="t('jobinfo.blockSerial')" value="SERIAL_EXECUTION" />
                <el-option :label="t('jobinfo.blockDiscardLater')" value="DISCARD_LATER" />
                <el-option :label="t('jobinfo.blockCoverEarly')" value="COVER_EARLY" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('jobinfo.executorTimeout')">
              <el-input-number v-model="formData.executorTimeout" :min="0" :max="99999" />
            </el-form-item>
            <el-form-item :label="t('jobinfo.executorFailRetryCount')">
              <el-input-number v-model="formData.executorFailRetryCount" :min="0" :max="99" />
            </el-form-item>
          </el-tab-pane>
          
          <el-tab-pane :label="t('jobinfo.childJobId')" name="advanced">
            <el-form-item :label="t('jobinfo.childJobId')">
              <el-input v-model="formData.childJobId" placeholder="多个用逗号分隔" />
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">{{ t('common.confirm') }}</el-button>
        <el-button v-if="formData.glueType !== 'BEAN'" type="success" @click="handleGLUE">
          {{ t('menu.jobcode') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 手动触发对话框 -->
    <el-dialog v-model="triggerDialogVisible" :title="t('jobinfo.triggerTitle')" width="500px">
      <el-form :model="triggerForm" label-width="100px">
        <el-form-item :label="t('jobinfo.executorParam')">
          <el-input v-model="triggerForm.executorParam" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="triggerDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleTriggerSubmit">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 下次执行时间对话框 -->
    <el-dialog v-model="nextTriggerTimeVisible" :title="t('jobinfo.nextTriggerTime')" width="500px">
      <div v-loading="nextTriggerTimeLoading">
        <div v-if="nextTriggerTimeList.length > 0" class="next-trigger-time-list">
          <div v-for="(time, index) in nextTriggerTimeList" :key="index" class="time-item">
            <el-tag>{{ time }}</el-tag>
          </div>
        </div>
        <el-empty v-else :description="t('common.noData')" />
      </div>
      <template #footer>
        <el-button @click="nextTriggerTimeVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { jobinfoApi, type JobInfo } from '@/api/jobinfo'
import { jobgroupApi, type JobGroup } from '@/api/jobgroup'
import { useAuthStore } from '@/stores/auth'
import CronInput from '@/components/CronInput/index.vue'

const router = useRouter()
const { t } = useI18n()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin())

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<JobInfo[]>([])
const jobGroups = ref<JobGroup[]>([])
const dialogVisible = ref(false)
const triggerDialogVisible = ref(false)
const nextTriggerTimeVisible = ref(false)
const nextTriggerTimeLoading = ref(false)
const nextTriggerTimeList = ref<string[]>([])
const activeTab = ref('basic')
const editingId = ref<number | null>(null)
const triggerJobId = ref<number>(0)
const formRef = ref<FormInstance>()

const searchForm = reactive({
  jobGroup: 0,
  triggerStatus: -1,
  jobDesc: '',
  executorHandler: '',
  author: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const formData = reactive<Partial<JobInfo>>({
  jobGroup: undefined,
  jobDesc: '',
  author: '',
  alarmEmail: '',
  scheduleType: 'CRON',
  scheduleConf: '',
  misfireStrategy: 'DO_NOTHING',
  executorRouteStrategy: 'FIRST',
  executorHandler: '',
  executorParam: '',
  executorBlockStrategy: 'SERIAL_EXECUTION',
  executorTimeout: 0,
  executorFailRetryCount: 0,
  glueType: 'BEAN',
  childJobId: ''
})

const triggerForm = reactive({
  executorParam: ''
})

const formRules: FormRules = {
  jobGroup: [{ required: true, message: '请选择执行器', trigger: 'change' }],
  jobDesc: [{ required: true, message: '请输入任务描述', trigger: 'blur' }],
  author: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  scheduleType: [{ required: true, message: '请选择调度类型', trigger: 'change' }],
  scheduleConf: [{ required: true, message: '请输入调度配置', trigger: 'blur' }],
  executorHandler: [{ required: true, message: '请输入JobHandler', trigger: 'blur' }],
  executorRouteStrategy: [{ required: true, message: '请选择路由策略', trigger: 'change' }]
}

const dialogTitle = computed(() => editingId.value ? t('jobinfo.editTitle') : t('jobinfo.addTitle'))

const scheduleConfPlaceholder = computed(() => {
  switch (formData.scheduleType) {
    case 'CRON': return '请输入Cron表达式'
    case 'FIX_RATE': return '请输入固定速度（秒）'
    default: return ''
  }
})

// GlueType 枚举映射
const glueTypeMap: Record<string, string> = {
  'BEAN': 'BEAN',
  'GLUE_GROOVY': 'GLUE(Java)',
  'GLUE_SHELL': 'GLUE(Shell)',
  'GLUE_PYTHON': 'GLUE(Python)',
  'GLUE_PHP': 'GLUE(PHP)',
  'GLUE_NODEJS': 'GLUE(Nodejs)',
  'GLUE_POWERSHELL': 'GLUE(PowerShell)'
}

function getGlueTypeLabel(glueType: string): string {
  return glueTypeMap[glueType] || glueType
}

async function loadData() {
  loading.value = true
  try {
    const res = await jobinfoApi.getPageList({
      start: (pagination.page - 1) * pagination.size,
      length: pagination.size,
      ...searchForm
    })
    tableData.value = res.data
    pagination.total = res.recordsFiltered
  } finally {
    loading.value = false
  }
}

async function loadJobGroups() {
  try {
    const res = await jobgroupApi.getAll()
    jobGroups.value = res || []
    if (!isAdmin.value && jobGroups.value.length > 0 && searchForm.jobGroup === 0) {
      searchForm.jobGroup = jobGroups.value[0].id
    }
  } catch (error) {
    console.error('Failed to load job groups:', error)
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.jobGroup = isAdmin.value ? 0 : (jobGroups.value[0]?.id || 0)
  searchForm.triggerStatus = -1
  searchForm.jobDesc = ''
  searchForm.executorHandler = ''
  searchForm.author = ''
  handleSearch()
}

function handleAdd() {
  editingId.value = null
  activeTab.value = 'basic'
  Object.assign(formData, {
    jobGroup: undefined,
    jobDesc: '',
    author: '',
    alarmEmail: '',
    scheduleType: 'CRON',
    scheduleConf: '',
    misfireStrategy: 'DO_NOTHING',
    executorRouteStrategy: 'FIRST',
    executorHandler: '',
    executorParam: '',
    executorBlockStrategy: 'SERIAL_EXECUTION',
    executorTimeout: 0,
    executorFailRetryCount: 0,
    glueType: 'BEAN',
    childJobId: ''
  })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editingId.value = row.id
  activeTab.value = 'basic'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// Tab 与字段的映射关系
const tabFieldMap: Record<string, string[]> = {
  basic: ['jobGroup', 'jobDesc', 'author'],
  schedule: ['scheduleType', 'scheduleConf'],
  executor: ['glueType', 'executorHandler', 'executorRouteStrategy', 'executorBlockStrategy'],
  advanced: ['childJobId']
}

function findTabByField(field: string): string | undefined {
  for (const [tab, fields] of Object.entries(tabFieldMap)) {
    if (fields.includes(field)) return tab
  }
  return undefined
}

async function handleSubmit() {
  if (!formRef.value) return
  
  formRef.value.validate((valid, errors) => {
    if (valid) {
      submitting.value = true
      const apiCall = editingId.value
        ? jobinfoApi.update(editingId.value, formData)
        : jobinfoApi.add(formData)
      
      apiCall.then(() => {
        ElMessage.success(t('common.success'))
        dialogVisible.value = false
        loadData()
      }).finally(() => {
        submitting.value = false
      })
    } else if (errors) {
      // 验证失败，自动切换到第一个有错误的 tab
      const firstErrorField = Object.keys(errors)[0]
      const targetTab = findTabByField(firstErrorField)
      if (targetTab) {
        activeTab.value = targetTab
      }
    }
  })
}

async function handleStatusChange(row: any) {
  const isStart = row.triggerStatus === 1
  const action = isStart ? t('common.enable') : t('common.disable')
  const originalStatus = isStart ? 0 : 1
  
  try {
    await ElMessageBox.confirm(
      t('common.confirmAction', { action }),
      t('common.confirm'),
      { type: 'warning' }
    )
    
    if (isStart) {
      await jobinfoApi.start(row.id)
    } else {
      await jobinfoApi.stop(row.id)
    }
    ElMessage.success(t('common.success'))
  } catch {
    row.triggerStatus = originalStatus
  }
}

function handleTrigger(row: any) {
  triggerJobId.value = row.id
  triggerForm.executorParam = row.executorParam || ''
  triggerDialogVisible.value = true
}

function handleDropdownCommand(command: string, row: any) {
  switch (command) {
    case 'log':
      handleViewLog(row)
      break
    case 'nextTime':
      handleNextTriggerTime(row)
      break
    case 'copy':
      handleCopy(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

async function handleTriggerSubmit() {
  try {
    await jobinfoApi.trigger(triggerJobId.value, triggerForm.executorParam)
    ElMessage.success(t('common.success'))
    triggerDialogVisible.value = false
  } catch (error) {
    console.error('Trigger failed:', error)
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(t('common.confirmDelete'), t('common.delete'), {
    type: 'warning'
  })
  await jobinfoApi.remove(row.id)
  ElMessage.success(t('common.success'))
  loadData()
}

function handleViewLog(row: any) {
  router.push({ path: '/joblog', query: { jobId: row.id, jobGroup: row.jobGroup } })
}

function handleGLUE() {
  if (editingId.value) {
    router.push({
      path: '/jobcode',
      query: { jobId: editingId.value },
      state: {
        glueType: formData.glueType,
        glueSource: formData.glueSource,
        glueRemark: formData.glueRemark,
        jobDesc: formData.jobDesc
      }
    })
  }
}

async function handleNextTriggerTime(row: any) {
  nextTriggerTimeVisible.value = true
  nextTriggerTimeLoading.value = true
  nextTriggerTimeList.value = []
  try {
    const res = await jobinfoApi.getNextTriggerTime(row.scheduleType, row.scheduleConf)
    nextTriggerTimeList.value = res || []
  } catch (error) {
    console.error('Failed to get next trigger time:', error)
  } finally {
    nextTriggerTimeLoading.value = false
  }
}

function handleCopy(row: any) {
  editingId.value = null
  activeTab.value = 'basic'
  Object.assign(formData, {
    jobGroup: row.jobGroup,
    jobDesc: row.jobDesc,
    author: row.author,
    alarmEmail: row.alarmEmail,
    scheduleType: row.scheduleType,
    scheduleConf: row.scheduleConf,
    misfireStrategy: row.misfireStrategy,
    executorRouteStrategy: row.executorRouteStrategy,
    executorHandler: row.executorHandler,
    executorParam: row.executorParam,
    executorBlockStrategy: row.executorBlockStrategy,
    executorTimeout: row.executorTimeout,
    executorFailRetryCount: row.executorFailRetryCount,
    glueType: row.glueType,
    childJobId: row.childJobId
  })
  dialogVisible.value = true
}

function handleScheduleTypeChange() {
  formData.scheduleConf = ''
}

onMounted(async () => {
  await loadJobGroups()
  loadData()
})
</script>

<style scoped lang="scss">
.jobinfo-container {
  .search-card {
    margin-bottom: 20px;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.next-trigger-time-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 0;
}

:deep(.el-table) {
  .el-button + .el-button,
  .el-button + .el-dropdown,
  .el-dropdown + .el-button {
    margin-left: 8px;
  }
  
  .el-dropdown {
    vertical-align: middle;
    
    .el-button {
      margin-left: 0;
    }
  }
}
</style>
