import type { LoginForm, AuthInfo, User, UserRole } from '@/types'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/api/index'
import { delay, mockUsers } from '@/mock/data'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

export const authApi = {
  async login(data: LoginForm): Promise<AuthInfo> {
    if (useMock) {
      await delay(500)
      const user = mockUsers.find(u => u.username === data.username)
      if (!user) throw new Error('用户不存在')
      return {
        token: 'mock-jwt-token-' + Date.now(),
        user: user,
        permissions: getPermissionsByRole(user.role)
      }
    }
    const form = new URLSearchParams({ username: data.username, password: data.password })
    const loginRes: any = await api.post('/auth/login', form, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
    const token = loginRes?.token || loginRes?.data?.token
    if (!token) throw new Error('登录失败')
    const store = useAuthStore()
    try { store.token = token } catch {}
    const me: any = await api.get('/auth/me', { headers: { Authorization: `Bearer ${token}` } })
    const username = me?.username || data.username
    const roles = me?.roles || []
    const role = (Array.isArray(roles) && roles.length > 0) ? (roles[0].toLowerCase() as UserRole) : 'user'
    return {
      token,
      user: {
        id: 0,
        username,
        role,
        status: 'active' as any,
        createdAt: new Date().toLocaleString('zh-CN'),
        updatedAt: new Date().toLocaleString('zh-CN')
      } as unknown as User,
      permissions: getPermissionsByRole(role)
    }
  },
  logout(): Promise<void> {
    if (useMock) return Promise.resolve()
    return api.post('/auth/logout')
  },
  async getCurrentUser(): Promise<AuthInfo> {
    if (useMock) {
      return Promise.resolve({
        token: 'mock-jwt-token',
        user: mockUsers[0],
        permissions: getPermissionsByRole(mockUsers[0].role)
      })
    }
    const me: any = await api.get('/auth/me')
    const username = me?.username || ''
    const roles = me?.roles || []
    const role = (Array.isArray(roles) && roles.length > 0) ? (roles[0].toLowerCase() as UserRole) : 'user'
    return {
      token: '',
      user: {
        id: 0,
        username,
        role,
        status: 'active' as any,
        createdAt: new Date().toLocaleString('zh-CN'),
        updatedAt: new Date().toLocaleString('zh-CN')
      } as unknown as User,
      permissions: getPermissionsByRole(role)
    }
  },
  refreshToken(): Promise<{ token: string }> {
    if (useMock) return Promise.resolve({ token: 'mock-jwt-token-' + Date.now() })
    return api.post('/auth/refresh')
  }
}

function getPermissionsByRole(role: UserRole): string[] {
  const permissions = {
    admin: [
      'user:read', 'user:create', 'user:update', 'user:delete',
      'counselor:read', 'counselor:approve',
      'content:read', 'content:approve',
      'assessment:read', 'assessment:manage',
      'report:read',
      'monitor:read',
      'file:read', 'file:manage',
      'setting:read', 'setting:manage'
    ],
    counselor: [ 'user:read', 'content:read', 'assessment:read' ],
    user: [ 'content:read' ]
  }
  return permissions[role] || []
}
