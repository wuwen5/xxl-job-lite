<template>
  <div class="jobcode-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('menu.jobcode') }} - {{ jobId }}</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleSave" :loading="saving">
              <el-icon><DocumentChecked /></el-icon>
              {{ t('common.save') }}
            </el-button>
            <el-button @click="router.back()">
              <el-icon><Back /></el-icon>
              返回
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="formData" label-width="100px" class="code-form">
        <el-form-item :label="t('jobinfo.glueRemark')">
          <el-input v-model="formData.glueRemark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <div class="code-editor" ref="editorRef"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import { java } from '@codemirror/lang-java'
import { python } from '@codemirror/lang-python'
import { javascript } from '@codemirror/lang-javascript'
import { oneDark } from '@codemirror/theme-one-dark'
import { jobcodeApi } from '@/api/jobcode'
import { jobinfoApi } from '@/api/jobinfo'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const jobId = ref(Number(route.query.jobId))
const saving = ref(false)
const editorRef = ref<HTMLElement>()
let editorView: any = null

const formData = reactive({
  glueRemark: ''
})

function getLanguageExtension(glueType: string) {
  switch (glueType) {
    case 'GLUE_GROOVY':
    case 'BEAN':
      return java()
    case 'GLUE_PYTHON':
      return python()
    case 'GLUE_NODEJS':
      return javascript()
    case 'GLUE_SHELL':
    case 'GLUE_POWERSHELL':
    default:
      return java()
  }
}

async function loadJobInfo() {
  try {
    // 获取任务信息
    const res = await jobinfoApi.getPageList({
      start: 0,
      length: 1,
      jobGroup: 0,
      triggerStatus: -1,
      jobDesc: '',
      executorHandler: '',
      author: ''
    })
    
    const job = res.data.find((j: any) => j.id === jobId.value)
    if (job && editorRef.value) {
      const langExt = getLanguageExtension(job.glueType)
      
      const state = EditorState.create({
        doc: job.glueSource || '// Write your code here...',
        extensions: [
          basicSetup,
          langExt,
          oneDark,
          EditorView.theme({
            '&': { height: '500px' },
            '.cm-scroller': { overflow: 'auto' }
          })
        ]
      })
      
      editorView = new EditorView({
        state,
        parent: editorRef.value
      })
    }
  } catch (error) {
    console.error('Failed to load job info:', error)
  }
}

async function handleSave() {
  if (!editorView) return
  
  saving.value = true
  try {
    const code = editorView.state.doc.toString()
    await jobcodeApi.save(jobId.value, code, formData.glueRemark)
    ElMessage.success(t('common.success'))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadJobInfo()
})

onUnmounted(() => {
  editorView?.destroy()
})
</script>

<style scoped lang="scss">
.jobcode-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .header-actions {
    display: flex;
    gap: 10px;
  }
}

.code-form {
  margin-bottom: 20px;
}

.code-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  
  :deep(.cm-editor) {
    min-height: 500px;
  }
}
</style>
