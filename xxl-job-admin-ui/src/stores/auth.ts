import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, getUserInfoApi } from '@/api/auth'
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
    if (!token.value) return
    try {
      const data = await getUserInfoApi()
      userInfo.value = data
    } catch {
      logout()
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
