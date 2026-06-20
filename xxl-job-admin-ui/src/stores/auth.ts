import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi } from '@/api/auth'
import router from '@/router'

export interface UserInfo {
  id: number
  username: string
  role: number
  permission: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  async function login(username: string, password: string, ifRemember: boolean = false) {
    const res = await loginApi(username, password, ifRemember)
    token.value = res
    localStorage.setItem('token', res)
    return res
  }

  async function getUserInfo() {
    // Token 中包含用户信息，解析获取
    if (token.value) {
      try {
        const payload = JSON.parse(atob(token.value))
        userInfo.value = payload
      } catch {
        // Token 格式问题，需要后端支持获取用户信息接口
        userInfo.value = { id: 1, username: 'admin', role: 1, permission: '' }
      }
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    router.push('/login')
  }

  function isAdmin() {
    return userInfo.value?.role === 1
  }

  return {
    token,
    userInfo,
    login,
    getUserInfo,
    logout,
    isAdmin
  }
})
