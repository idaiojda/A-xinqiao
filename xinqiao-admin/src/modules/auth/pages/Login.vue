<template>
  <div class="login-container">
    <div class="login-wrapper">
      <div class="login-header">
        <div class="logo">
          <img src="/logo.svg" alt="心桥心理" />
          <h1>心桥心理</h1>
        </div>
        <p class="subtitle">管理员登录</p>
      </div>
      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <div class="form-options">
            <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
          </div>
        </el-form-item>
        <div class="form-footer">
          <a href="#" class="forgot-link">忘记密码？</a>
        </div>
        <el-form-item>
          <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer"><p>© 2025 心桥心理管理系统</p></div>
    </div>
    <div class="login-background"><div class="gradient-overlay"></div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/modules/auth/api'
import type { LoginForm } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive<LoginForm>({ username: '', password: '', rememberMe: false })

const loginRules: FormRules = {
  username: [ { required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' } ],
  password: [ { required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' } ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const authInfo = await authApi.login(loginForm)
        if (authInfo.user.role !== 'admin') {
          ElMessage.error('仅管理员可登录管理端，请使用咨询师移动端')
          authStore.clearAuth()
          return
        }
        authStore.setAuth(authInfo)
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } catch (error) {
        console.error('登录失败:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.login-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; position: relative; overflow: hidden }
.login-wrapper { width: 400px; padding: 40px; background: rgba(255,255,255,0.95); border-radius: 20px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); backdrop-filter: blur(10px); position: relative; z-index: 1 }
.login-header { text-align: center; margin-bottom: 40px }
.login-header .logo { display: flex; align-items: center; justify-content: center; margin-bottom: 10px }
.login-header .logo img { width: 40px; height: 40px; margin-right: 12px }
.login-header .logo h1 { font-size: 28px; font-weight: 600; color: #333; margin: 0 }
.login-header .subtitle { color: #666; font-size: 16px; margin: 0 }
.login-form .el-form-item { margin-bottom: 24px }
.login-form .form-options { display: flex; justify-content: flex-start; align-items: center }
.login-form .form-footer { display: flex; justify-content: flex-end; margin-bottom: 24px; padding-right: 0 }
.login-form .form-footer .forgot-link { color: var(--el-color-primary); text-decoration: none; font-size: 14px }
.login-form .login-button { width: 100%; height: 48px; font-size: 16px; font-weight: 500; background: linear-gradient(135deg, var(--el-color-primary) 0%, #764ba2 100%); border: none }
.login-background { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) }
.login-background .gradient-overlay { width: 100%; height: 100%; background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="50" cy="50" r="1" fill="white" opacity="0.1"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>') }
@media (max-width: 768px) { .login-wrapper { width: 90%; max-width: 400px; padding: 30px 20px } .login-form .form-footer { justify-content: center; margin-bottom: 20px } }
</style>
