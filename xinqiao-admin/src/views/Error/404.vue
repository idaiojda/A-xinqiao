<template>
  <div class="error-container">
    <div class="error-content">
      <div class="error-icon">404</div>
      <h1 class="error-title">页面未找到</h1>
      <p class="error-desc">抱歉，您访问的页面不存在或已被删除</p>
      <div class="error-actions">
        <el-button type="primary" @click="$router.push('/dashboard')">
          返回首页
        </el-button>
        <el-button @click="$router.back()">返回上页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 404页面组件
</script>

<style scoped lang="scss">
.error-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.error-content {
  text-align: center;
  color: white;
  padding: 40px;
}

.error-icon {
  font-size: 120px;
  font-weight: bold;
  margin-bottom: 20px;
  text-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

.error-title {
  font-size: 32px;
  margin-bottom: 16px;
  font-weight: 600;
}

.error-desc {
  font-size: 16px;
  margin-bottom: 32px;
  opacity: 0.9;
}

.error-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}
</style>