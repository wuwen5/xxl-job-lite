<template>
  <div class="layout-container">
    <AppSidebar />
    <div class="main-container" :class="{ collapsed: appStore.sidebarCollapsed }">
      <AppHeader />
      <AppMain />
    </div>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
import AppMain from './AppMain.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<style scoped lang="scss">
.layout-container {
  display: flex;
  height: 100%;
  width: 100%;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: $sidebar-width;
  transition: margin-left 0.3s;
  
  &.collapsed {
    margin-left: $sidebar-collapsed-width;
  }
}
</style>
