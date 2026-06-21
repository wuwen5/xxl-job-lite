<template>
  <div class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
    <div class="logo">
      <span class="logo-text" v-show="!appStore.sidebarCollapsed">XXL-JOB</span>
      <span class="logo-text-mini" v-show="appStore.sidebarCollapsed">X</span>
    </div>
    <el-scrollbar>
      <el-menu
        :default-active="route.path"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <template #title>{{ t('menu.dashboard') }}</template>
        </el-menu-item>
        <el-menu-item index="/jobinfo">
          <el-icon><List /></el-icon>
          <template #title>{{ t('menu.jobinfo') }}</template>
        </el-menu-item>
        <el-menu-item index="/jobgroup">
          <el-icon><Connection /></el-icon>
          <template #title>{{ t('menu.jobgroup') }}</template>
        </el-menu-item>
        <el-menu-item index="/joblog">
          <el-icon><Document /></el-icon>
          <template #title>{{ t('menu.joblog') }}</template>
        </el-menu-item>
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <template #title>{{ t('menu.user') }}</template>
        </el-menu-item>
        <el-menu-item index="/help">
          <el-icon><QuestionFilled /></el-icon>
          <template #title>{{ t('menu.help') }}</template>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const { t } = useI18n()
const appStore = useAppStore()
</script>

<style scoped lang="scss">
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: $sidebar-width;
  background-color: $sidebar-bg;
  transition: width 0.3s;
  z-index: 1001;
  overflow: hidden;

  &.collapsed {
    width: $sidebar-collapsed-width;
  }
}

.logo {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #263445;
  cursor: pointer;
}

.logo-img {
  width: 32px;
  height: 32px;
  margin-right: 10px;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

.logo-text-mini {
  color: #fff;
  font-size: 20px;
  font-weight: 600;
}

.el-menu {
  border-right: none;
}
</style>
