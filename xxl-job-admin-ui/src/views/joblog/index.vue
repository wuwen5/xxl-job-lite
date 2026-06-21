<template>
  <div class="joblog-container">
    <el-card class="search-card">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('joblog.jobGroup')">
          <el-select v-model="searchForm.jobGroup" :placeholder="t('common.all')" style="width: 200px" @change="handleJobGroupChange">
            <el-option v-if="isAdmin" :label="t('common.all')" :value="0" />
            <el-option
              v-for="item in jobGroups"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('joblog.job')">
          <el-select v-model="searchForm.jobId" :placeholder="t('common.all')" style="width: 200px">
            <el-option :label="t('common.all')" :value="0" />
            <el-option
              v-for="item in jobList"
              :key="item.id"
              :label="item.jobDesc"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('joblog.logStatus')">
          <el-select v-model="searchForm.logStatus" :placeholder="t('common.all')" style="width: 120px">
            <el-option :label="t('common.all')" :value="-1" />
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="2" />
            <el-option label="运行中" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('menu.joblog') }}</span>
          <el-button type="danger" @click="handleClearLog">
            <el-icon><Delete /></el-icon>
            {{ t('joblog.clearLog') }}
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="jobId" :label="t('joblog.jobId')" width="80" align="center" />
        <el-table-column prop="jobDesc" :label="t('joblog.jobDesc')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="t('joblog.triggerTime')" width="170" align="center">
          <template #default="{ row }">
            {{ formatTime(row.triggerTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('joblog.triggerCode')" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.triggerCode === 200" type="success">成功</el-tag>
            <el-tag v-else-if="row.triggerCode === 500" type="danger">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('joblog.triggerMsg')" width="80" align="center">
          <template #default="{ row }">
            <el-button v-if="row.triggerMsg" type="primary" link size="small" @click="handleViewMsg(row.triggerMsg, t('joblog.triggerMsg'))">
              {{ t('common.view') }}
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('joblog.handleTime')" width="170" align="center">
          <template #default="{ row }">
            {{ formatTime(row.handleTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('joblog.handleCode')" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.handleCode === 200" type="success">成功</el-tag>
            <el-tag v-else-if="row.handleCode === 500" type="danger">失败</el-tag>
            <el-tag v-else-if="row.handleCode === 0" type="info">运行中</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('joblog.handleMsg')" width="80" align="center">
          <template #default="{ row }">
            <el-button v-if="row.handleMsg" type="primary" link size="small" @click="handleViewMsg(row.handleMsg, t('joblog.handleMsg'))">
              {{ t('common.view') }}
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.operation')" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">
              <el-icon><View /></el-icon> {{ t('joblog.logDetail') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 清理日志对话框 -->
    <el-dialog v-model="clearLogVisible" :title="t('joblog.clearLog')" width="400px">
      <el-form :model="clearLogForm" label-width="100px">
        <el-form-item :label="t('joblog.jobGroup')">
          <el-select v-model="clearLogForm.jobGroup" style="width: 100%">
            <el-option :label="t('common.all')" :value="0" />
            <el-option
              v-for="item in jobGroups"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('joblog.job')">
          <el-select v-model="clearLogForm.jobId" style="width: 100%">
            <el-option :label="t('common.all')" :value="0" />
            <el-option
              v-for="item in jobList"
              :key="item.id"
              :label="item.jobDesc"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="清理方式">
          <el-select v-model="clearLogForm.type" style="width: 100%">
            <el-option :label="t('joblog.cleanType1')" :value="1" />
            <el-option :label="t('joblog.cleanType2')" :value="2" />
            <el-option :label="t('joblog.cleanType3')" :value="3" />
            <el-option :label="t('joblog.cleanType4')" :value="4" />
            <el-option :label="t('joblog.cleanType5')" :value="5" />
            <el-option :label="t('joblog.cleanType6')" :value="6" />
            <el-option :label="t('joblog.cleanType7')" :value="7" />
            <el-option :label="t('joblog.cleanType8')" :value="8" />
            <el-option :label="t('joblog.cleanType9')" :value="9" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clearLogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleClearLogSubmit">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 消息详情对话框 -->
    <el-dialog v-model="msgDialogVisible" :title="msgDialogTitle" width="600px">
      <div class="msg-content" v-html="msgDialogContent"></div>
      <template #footer>
        <el-button @click="msgDialogVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { joblogApi, type JobLog } from '@/api/joblog'
import { jobgroupApi, type JobGroup } from '@/api/jobgroup'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin())

const loading = ref(false)
const tableData = ref<JobLog[]>([])
const jobGroups = ref<JobGroup[]>([])
const jobList = ref<any[]>([])
const clearLogVisible = ref(false)
const msgDialogVisible = ref(false)
const msgDialogTitle = ref('')
const msgDialogContent = ref('')

const searchForm = reactive({
  jobGroup: 0 as number | string,
  jobId: 0,
  logStatus: -1
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const clearLogForm = reactive({
  jobGroup: 0,
  jobId: 0,
  type: 1
})

function formatTime(time: string) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await joblogApi.getPageList({
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

async function loadJobsByGroup(groupId: number | string) {
  const id = Number(groupId) || 0
  if (id === 0) {
    jobList.value = []
    return
  }
  try {
    const res = await joblogApi.getJobsByGroup(id)
    jobList.value = res || []
  } catch (error) {
    console.error('Failed to load jobs:', error)
  }
}

function handleJobGroupChange() {
  searchForm.jobId = 0
  loadJobsByGroup(Number(searchForm.jobGroup) || 0)
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.jobGroup = isAdmin.value ? 0 : (jobGroups.value[0]?.id || 0)
  searchForm.jobId = 0
  searchForm.logStatus = -1
  handleSearch()
}

function handleViewDetail(row: any) {
  router.push(`/joblog/detail/${row.id}`)
}

function handleViewMsg(msg: string, title: string) {
  msgDialogTitle.value = title
  msgDialogContent.value = msg.replace(/\n/g, '<br>')
  msgDialogVisible.value = true
}

function handleClearLog() {
  clearLogForm.jobGroup = Number(searchForm.jobGroup) || 0
  clearLogForm.jobId = searchForm.jobId
  clearLogVisible.value = true
}

async function handleClearLogSubmit() {
  await ElMessageBox.confirm('确认清理日志？', t('common.confirm'), {
    type: 'warning'
  })
  await joblogApi.clearLog(clearLogForm.jobGroup, clearLogForm.jobId, clearLogForm.type)
  ElMessage.success(t('common.success'))
  clearLogVisible.value = false
  loadData()
}

onMounted(async () => {
  await loadJobGroups()
  
  // 从 URL 参数初始化搜索条件
  if (route.query.jobId) {
    searchForm.jobId = Number(route.query.jobId)
  }
  if (route.query.jobGroup) {
    searchForm.jobGroup = Number(route.query.jobGroup)
    loadJobsByGroup(searchForm.jobGroup)
  }
  
  loadData()
})
</script>

<style scoped lang="scss">
.joblog-container {
  .search-card {
    margin-bottom: 20px;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.msg-content {
  max-height: 400px;
  overflow-y: auto;
  line-height: 1.6;
  word-break: break-all;
  white-space: pre-wrap;
}
</style>
