<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-header">
        <h2>{{ t('login.title') }}</h2>
      </div>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="userName">
          <el-input
            v-model="loginForm.userName"
            :placeholder="t('login.usernamePlaceholder')"
            size="large"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            :placeholder="t('login.passwordPlaceholder')"
            show-password
            size="large"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="loginForm.ifRemember">{{ t('login.rememberMe') }}</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ t('login.login') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const { t } = useI18n()
const authStore = useAuthStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  userName: '',
  password: '',
  ifRemember: false
})

const loginRules: FormRules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authStore.login(loginForm.userName, loginForm.password, loginForm.ifRemember)
        ElMessage.success(t('common.success'))
        router.push('/')
      } catch {
        // 错误提示已在 request 拦截器中处理
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  border-radius: 8px;
  
  :deep(.el-card__body) {
    padding: 40px;
  }
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
  
  h2 {
    color: #303133;
    font-size: 24px;
    font-weight: 600;
  }
}

.login-form {
  :deep(.el-form-item__content) {
    justify-content: center;
  }
}

.login-btn {
  width: 100%;
}
</style>
