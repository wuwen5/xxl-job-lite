import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref<boolean>(localStorage.getItem('sidebarCollapsed') === 'true')
  const locale = ref<string>(localStorage.getItem('locale') || 'zh-CN')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem('sidebarCollapsed', String(sidebarCollapsed.value))
  }

  function setLocale(lang: string) {
    locale.value = lang
    localStorage.setItem('locale', lang)
  }

  return {
    sidebarCollapsed,
    locale,
    toggleSidebar,
    setLocale
  }
})
