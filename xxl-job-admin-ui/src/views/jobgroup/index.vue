<template>
  <div class="jobgroup-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('menu.jobgroup') }}</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            {{ t('common.add') }}
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('jobgroup.appname')">
          <el-input v-model="searchForm.appname" clearable />
        </el-form-item>
        <el-form-item :label="t('jobgroup.title')">
          <el-input v-model="searchForm.title" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="appname" :label="t('jobgroup.appname')" min-width="150" show-overflow-tooltip />
        <el-table-column prop="title" :label="t('jobgroup.title')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="t('jobgroup.addressType')" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.addressType === 0 ? 'success' : 'warning'">
              {{ row.addressType === 0 ? t('jobgroup.addressTypeAuto') : t('jobgroup.addressTypeManual') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="addressList" :label="t('jobgroup.addressList')" min-width="200" show-overflow-tooltip />
        <el-table-column :label="t('common.operation')" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> {{ t('common.edit') }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon> {{ t('common.delete') }}
            </el-button>
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
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item :label="t('jobgroup.appname')" prop="appname">
          <el-input v-model="formData.appname" placeholder="执行器AppName" />
        </el-form-item>
        <el-form-item :label="t('jobgroup.title')" prop="title">
          <el-input v-model="formData.title" placeholder="执行器名称" />
        </el-form-item>
        <el-form-item :label="t('jobgroup.addressType')" prop="addressType">
          <el-radio-group v-model="formData.addressType">
            <el-radio :label="0">{{ t('jobgroup.addressTypeAuto') }}</el-radio>
            <el-radio :label="1">{{ t('jobgroup.addressTypeManual') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('jobgroup.addressList')" v-if="formData.addressType === 1">
          <el-input v-model="formData.addressList" type="textarea" :rows="3" placeholder="多个地址用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { jobgroupApi, type JobGroup } from '@/api/jobgroup'

const { t } = useI18n()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<JobGroup[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const searchForm = reactive({
  appname: '',
  title: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const formData = reactive<Partial<JobGroup>>({
  appname: '',
  title: '',
  addressType: 0,
  addressList: ''
})

const formRules: FormRules = {
  appname: [{ required: true, message: '请输入AppName', trigger: 'blur' }],
  title: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  addressType: [{ required: true, message: '请选择地址类型', trigger: 'change' }]
}

const dialogTitle = computed(() => editingId.value ? t('jobgroup.editTitle') : t('jobgroup.addTitle'))

async function loadData() {
  loading.value = true
  try {
    const res = await jobgroupApi.getPageList({
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

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.appname = ''
  searchForm.title = ''
  handleSearch()
}

function handleAdd() {
  editingId.value = null
  Object.assign(formData, {
    appname: '',
    title: '',
    addressType: 0,
    addressList: ''
  })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editingId.value = row.id
  Object.assign(formData, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (editingId.value) {
          await jobgroupApi.update(editingId.value, formData)
        } else {
          await jobgroupApi.add(formData)
        }
        ElMessage.success(t('common.success'))
        dialogVisible.value = false
        loadData()
      } finally {
        submitting.value = false
      }
    }
  })
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(t('common.confirmDelete'), t('common.delete'), {
    type: 'warning'
  })
  await jobgroupApi.remove(row.id)
  ElMessage.success(t('common.success'))
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.jobgroup-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .search-form {
    margin-bottom: 20px;
  }
}
</style>
