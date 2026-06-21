<template>
  <div class="jobcode-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('menu.jobcode') }} - {{ jobDesc || jobId }}</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleSave" :loading="saving">
              <el-icon><DocumentChecked /></el-icon>
              {{ t('common.save') }}
            </el-button>
            <el-button @click="router.back()">
              <el-icon><Back /></el-icon>
              {{ t('common.back') }}
            </el-button>
          </div>
        </div>
      </template>

      <div class="editor-layout">
        <div class="editor-main">
          <el-form :model="formData" label-width="100px" class="code-form">
            <el-form-item :label="t('jobinfo.glueRemark')">
              <el-input v-model="formData.glueRemark" placeholder="请输入备注" />
            </el-form-item>
          </el-form>

          <div class="code-editor" ref="editorRef"></div>
        </div>

        <div class="history-panel">
          <div class="history-header">
            <span>{{ t('jobinfo.glueHistory') }}</span>
          </div>
          <el-scrollbar height="560px">
            <div
              v-for="item in historyList"
              :key="item.id"
              class="history-item"
              :class="{ active: activeHistoryId === item.id }"
              @click="handleLoadHistory(item)"
            >
              <div class="history-remark">{{ item.glueRemark }}</div>
              <div class="history-time">{{ formatTime(item.updateTime) }}</div>
            </div>
            <el-empty v-if="historyList.length === 0" :description="t('common.noData')" :image-size="60" />
          </el-scrollbar>
        </div>
      </div>
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
import { StreamLanguage } from '@codemirror/language'
import { java } from '@codemirror/lang-java'
import { python } from '@codemirror/lang-python'
import { javascript } from '@codemirror/lang-javascript'
import { php } from '@codemirror/lang-php'
import { shell } from '@codemirror/legacy-modes/mode/shell'
import { powerShell } from '@codemirror/legacy-modes/mode/powershell'
import { oneDark } from '@codemirror/theme-one-dark'
import { jobcodeApi, type JobCode } from '@/api/jobcode'
import { jobinfoApi } from '@/api/jobinfo'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const jobId = ref(Number(route.query.jobId))
const jobDesc = ref(history.state?.jobDesc || '')
const saving = ref(false)
const editorRef = ref<HTMLElement>()
let editorView: any = null

const formData = reactive({
  glueRemark: ''
})

const historyList = ref<JobCode[]>([])
const activeHistoryId = ref<number | null>(null)

function formatTime(time: string | null | undefined): string {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return time
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function getLanguageExtension(glueType: string) {
  switch (glueType) {
    case 'GLUE_GROOVY':
    case 'BEAN':
      return java()
    case 'GLUE_PYTHON':
      return python()
    case 'GLUE_NODEJS':
      return javascript()
    case 'GLUE_PHP':
      return php()
    case 'GLUE_SHELL':
      return StreamLanguage.define(shell)
    case 'GLUE_POWERSHELL':
      return StreamLanguage.define(powerShell)
    default:
      return java()
  }
}

function updateEditorContent(code: string) {
  if (!editorView) return
  const transaction = editorView.state.update({
    changes: { from: 0, to: editorView.state.doc.length, insert: code }
  })
  editorView.dispatch(transaction)
}

function handleLoadHistory(item: JobCode) {
  activeHistoryId.value = item.id
  formData.glueRemark = item.glueRemark
  updateEditorContent(item.glueSource)
}

async function loadHistory() {
  try {
    const res = await jobcodeApi.getHistory(jobId.value)
    historyList.value = res || []
  } catch (error) {
    console.error('Failed to load history:', error)
  }
}

async function loadJobInfo() {
  try {
    let glueType = ''
    let glueSource = ''
    let glueRemark = ''

    const state = history.state
    if (state && state.glueType) {
      glueType = state.glueType
      glueSource = state.glueSource || ''
      glueRemark = state.glueRemark || ''
    } else {
      const res = await jobinfoApi.getPageList({
        start: 0,
        length: 10,
        jobGroup: 0,
        triggerStatus: -1,
        jobDesc: '',
        executorHandler: '',
        author: ''
      })

      const job = res.data.find((j: any) => j.id === jobId.value)
      if (job) {
        glueType = job.glueType
        glueSource = job.glueSource || ''
        glueRemark = job.glueRemark || ''
      }
    }

    formData.glueRemark = glueRemark

    if (editorRef.value) {
      const langExt = getLanguageExtension(glueType)

      const state = EditorState.create({
        doc: glueSource || '// Write your code here...',
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

    await loadHistory()
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
    await loadHistory()
    activeHistoryId.value = null
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

.editor-layout {
  display: flex;
  gap: 16px;
}

.editor-main {
  flex: 1;
  min-width: 0;
}

.code-form {
  margin-bottom: 16px;
}

.code-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  
  :deep(.cm-editor) {
    min-height: 500px;
  }
}

.history-panel {
  width: 280px;
  flex-shrink: 0;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;

  .history-header {
    padding: 10px 16px;
    background: #f5f7fa;
    border-bottom: 1px solid #dcdfe6;
    font-weight: 500;
    font-size: 14px;
  }

  .history-item {
    padding: 10px 16px;
    cursor: pointer;
    border-bottom: 1px solid #ebeef5;
    transition: background-color 0.2s;

    &:hover {
      background: #f5f7fa;
    }

    &.active {
      background: #ecf5ff;
      border-left: 3px solid #409eff;
    }

    .history-remark {
      font-size: 13px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .history-time {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }
  }
}
</style>
