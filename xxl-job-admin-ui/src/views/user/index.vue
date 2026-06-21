<template>
  <div class="user-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('menu.user') }}</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            {{ t('common.add') }}
          </el-button>
        </div>
      </template>

      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('user.username')">
          <el-input v-model="searchForm.username" clearable />
        </el-form-item>
        <el-form-item :label="t('user.role')">
          <el-select v-model="searchForm.role" :placeholder="t('common.all')" style="width: 120px">
            <el-option :label="t('common.all')" :value="-1" />
            <el-option :label="t('user.roleAdmin')" :value="1" />
            <el-option :label="t('user.roleNormal')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" :label="t('user.username')" min-width="150" />
        <el-table-column :label="t('user.role')" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : 'success'">
              {{ row.role === 1 ? t('user.roleAdmin') : t('user.roleNormal') }}
            </el-tag>
          </template>
        </el-table-column>
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item :label="t('user.username')" prop="username">
          <el-input v-model="formData.username" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item :label="t('user.password')" prop="password" v-if="!editingId">
          <el-input v-model="formData.password" type="password" show-password />
        </el-form-item>
        <el-form-item :label="t('user.role')" prop="role">
          <el-radio-group v-model="formData.role">
            <el-radio :label="1">{{ t('user.roleAdmin') }}</el-radio>
            <el-radio :label="0">{{ t('user.roleNormal') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('user.permission')" v-if="formData.role === 0">
          <el-select v-model="formData.permissionIds" multiple clearable :placeholder="t('user.permissionPlaceholder')" style="width: 100%">
            <el-option
              v-for="item in jobGroups"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
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
import { userApi, type JobUser } from '@/api/user'
import { jobgroupApi, type JobGroup } from '@/api/jobgroup'

const { t } = useI18n()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<JobUser[]>([])
const jobGroups = ref<JobGroup[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const searchForm = reactive({
  username: '',
  role: -1
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const formData = reactive({
  id: undefined as number | undefined,
  username: '',
  password: '',
  role: 0,
  permission: '',
  permissionIds: [] as number[]
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const dialogTitle = computed(() => editingId.value ? t('user.editTitle') : t('user.addTitle'))

async function loadData() {
  loading.value = true
  try {
    const res = await userApi.getPageList({
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
  } catch (error) {
    console.error('Failed to load job groups:', error)
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.username = ''
  searchForm.role = -1
  handleSearch()
}

function handleAdd() {
  editingId.value = null
  Object.assign(formData, {
    id: undefined,
    username: '',
    password: '',
    role: 0,
    permission: '',
    permissionIds: []
  })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editingId.value = row.id
  Object.assign(formData, {
    id: row.id,
    username: row.username,
    password: '',
    role: row.role,
    permission: row.permission,
    permissionIds: row.permission ? row.permission.split(',').map(Number).filter(Boolean) : []
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const submitData = {
          ...formData,
          permission: formData.role === 1 ? '' : formData.permissionIds.join(',')
        }
        if (editingId.value) {
          await userApi.update(editingId.value, submitData)
        } else {
          await userApi.add(submitData)
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
  await userApi.remove(row.id)
  ElMessage.success(t('common.success'))
  loadData()
}

onMounted(() => {
  loadData()
  loadJobGroups()
})
</script>

<style scoped lang="scss">
.user-container {
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
