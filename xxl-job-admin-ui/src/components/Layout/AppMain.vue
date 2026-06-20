<template>
  <div class="main">
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="cachedViews">
          <component :is="Component" />
        </keep-alive>
      </transition>
    </router-view>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const cachedViews = computed(() => {
  // 可以根据需要缓存的视图
  return []
})
</script>

<style scoped lang="scss">
.main {
  flex: 1;
  overflow: auto;
  background-color: $bg-color;
  padding: 20px;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
