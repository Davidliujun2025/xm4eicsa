<template>
  <!-- 全局统一侧边布局 -->
  <MainLayout>
    <!-- Token额度业务页面 -->
    <TokenQuota />
  </MainLayout>
</template>

<script setup lang="ts">
import MainLayout from '@/layout/MainLayout.vue'
import TokenQuota from '@/views/TokenQuota.vue'
</script>