<template>
  <router-view v-slot="{ Component, route }">
    <transition name="page" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
</template>

<script setup>
/**
 * 应用根组件
 *
 * 只做一件事：渲染当前路由对应的页面，并附带一次「淡入 + 轻微上移」的过渡。
 *
 * 为什么要绑 key？
 *   默认情况下 Vue 会复用同一个组件实例。加上 :key="route.path" 后，
 *   不同路由之间切换会重建组件，过渡动画才能正常触发，
 *   页面数据也会重新初始化（否则从 A 课程跳到 B 课程可能还显示着 A 的数据）。
 */
</script>

<style scoped>
.page-enter-active {
  transition: opacity 0.32s var(--ch-ease), transform 0.32s var(--ch-ease);
}

.page-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
