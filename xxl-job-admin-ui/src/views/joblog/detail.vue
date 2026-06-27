<template>
  <div class="log-detail-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ t('joblog.logDetail') }} - {{ logId }}</span>
          <el-button @click="router.back()">
            <el-icon><Back /></el-icon>
            返回
          </el-button>
        </div>
      </template>

      <div class="log-content" ref="logContentRef">
        <pre v-html="logContent"></pre>
      </div>

      <div class="log-status">
        <el-tag v-if="isEnd" type="success">日志加载完成</el-tag>
        <el-tag v-else type="warning">加载中...</el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { joblogApi } from '@/api/joblog'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const logId = ref(Number(route.params.id))
const logContent = ref('')
const isEnd = ref(false)
const logContentRef = ref<HTMLElement>()
let fromLineNum = 1
let timer: ReturnType<typeof setTimeout> | null = null

async function loadLog() {
  if (isEnd.value) return
  
  try {
    const res = await joblogApi.getLogDetail(logId.value, fromLineNum)
    if (res) {
      logContent.value += res.logContent
      fromLineNum = res.toLineNum + 1
      isEnd.value = res.end
      
      // 自动滚动到底部
      await nextTick()
      if (logContentRef.value) {
        logContentRef.value.scrollTop = logContentRef.value.scrollHeight
      }
      
      // 如果未结束，继续轮询
      if (!isEnd.value) {
        timer = setTimeout(loadLog, 3000)
      }
    }
  } catch (error) {
    console.error('Failed to load log:', error)
  }
}

onMounted(() => {
  loadLog()
})

onUnmounted(() => {
  if (timer) {
    clearTimeout(timer)
  }
})
</script>

<style scoped lang="scss">
.log-detail-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.log-content {
  background-color: #1e1e1e;
  color: #d4d4d4;
  padding: 15px;
  border-radius: 4px;
  max-height: 600px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  
  pre {
    margin: 0;
    white-space: pre-wrap;
    word-wrap: break-word;
  }
}

.log-status {
  margin-top: 15px;
  text-align: center;
}
</style>
