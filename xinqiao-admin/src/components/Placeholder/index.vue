<template>
  <div class="placeholder-container">
    <el-result
      icon="info"
      title="功能开发中"
      sub-title="该功能正在开发中，敬请期待"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/dashboard')">
          返回首页
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
// 占位组件，用于未实现的功能模块
</script>

<style scoped lang="scss">
.placeholder-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
</style>