import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, AuthInfo } from '@/types'
import Cookies from 'js-cookie'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const user = ref<User | null>(null)
  const token = ref<string>(Cookies.get('token') || '')
  const permissions = ref<string[]>([])

  // 计算属性
  const isAuthenticated = computed(() => !!token.value && !!user.value)
  
  // 方法
  const setAuth = (authInfo: AuthInfo) => {
    token.value = authInfo.token
    user.value = authInfo.user
    permissions.value = authInfo.permissions
    Cookies.set('token', authInfo.token, { expires: 7 })
  }

  const clearAuth = () => {
    token.value = ''
    user.value = null
    permissions.value = []
    Cookies.remove('token')
  }

  const hasPermissions = (requiredPermissions: string[]): boolean => {
    if (!requiredPermissions || requiredPermissions.length === 0) return true
    if (user.value?.role === 'admin') return true
    if (permissions.value.includes('*')) return true
    if (!permissions.value.length) return false
    return requiredPermissions.some(permission => {
      if (permissions.value.includes(permission)) return true
      for (const p of permissions.value) {
        if (p.endsWith('*')) {
          const prefix = p.slice(0, -1)
          if (permission.startsWith(prefix)) return true
        }
      }
      return false
    })
  }

  const checkAuth = async () => {
    if (!token.value) return
    
    try {
      // 这里应该调用API验证token
      // const response = await api.getCurrentUser()
      // if (response.data) {
      //   user.value = response.data.user
      //   permissions.value = response.data.permissions
      // } else {
      //   clearAuth()
      // }
    } catch (error) {
      clearAuth()
    }
  }

  return {
    user,
    token,
    permissions,
    isAuthenticated,
    setAuth,
    clearAuth,
    hasPermissions,
    checkAuth
  }
})
