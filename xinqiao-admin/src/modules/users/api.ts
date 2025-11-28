import type { User, PageResponse, UserRole, UserStatus } from '@/types'
import { api } from '@/api/index'
import { delay, mockUsers } from '@/mock/data'

export interface UserQuery {
  page?: number
  size?: number
  username?: string
  role?: UserRole
  status?: UserStatus
  startDate?: string
  endDate?: string
}

export interface UserForm {
  username: string
  phone?: string
  role: UserRole
  status: UserStatus
  avatar?: string
}

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

export const userApi = {
  async getUserList(params: UserQuery): Promise<PageResponse<User>> {
    if (useMock) {
      await delay(300)
      let filteredUsers = [...mockUsers]
      if (params.username) {
        filteredUsers = filteredUsers.filter(user => user.username.toLowerCase().includes(params.username!.toLowerCase()))
      }
      if (params.role) {
        filteredUsers = filteredUsers.filter(user => user.role === params.role)
      }
      if (params.status) {
        filteredUsers = filteredUsers.filter(user => user.status === params.status)
      }
      const page = params.page || 1
      const size = params.size || 10
      const start = (page - 1) * size
      const end = start + size
      return { list: filteredUsers.slice(start, end), total: filteredUsers.length, page, size, pages: Math.ceil(filteredUsers.length / size) }
    }
    return api.get('/users', { params })
  },
  getUserById(id: number): Promise<User> {
    if (useMock) {
      const user = mockUsers.find(u => u.id === id)
      if (!user) throw new Error('用户不存在')
      return Promise.resolve(user)
    }
    return api.get(`/users/${id}`)
  },
  async createUser(data: UserForm): Promise<User> {
    if (useMock) {
      await delay(300)
      const newUser: User = { id: Math.max(...mockUsers.map(u => u.id)) + 1, ...data, createdAt: new Date().toLocaleString('zh-CN'), updatedAt: new Date().toLocaleString('zh-CN') }
      mockUsers.push(newUser)
      return newUser
    }
    return api.post('/users', data)
  },
  async updateUser(id: number, data: Partial<UserForm>): Promise<User> {
    if (useMock) {
      await delay(300)
      const index = mockUsers.findIndex(u => u.id === id)
      if (index === -1) throw new Error('用户不存在')
      mockUsers[index] = { ...mockUsers[index], ...data, updatedAt: new Date().toLocaleString('zh-CN') }
      return mockUsers[index]
    }
    return api.put(`/users/${id}`, data)
  },
  async deleteUser(id: number): Promise<void> {
    if (useMock) {
      await delay(300)
      const index = mockUsers.findIndex(u => u.id === id)
      if (index === -1) throw new Error('用户不存在')
      mockUsers.splice(index, 1)
      return
    }
    return api.delete(`/users/${id}`)
  },
  async batchDeleteUsers(ids: number[]): Promise<void> {
    if (useMock) {
      await delay(500)
      for (const id of ids) {
        const index = mockUsers.findIndex(u => u.id === id)
        if (index !== -1) mockUsers.splice(index, 1)
      }
      return
    }
    return api.delete('/users/batch', { data: { ids } })
  },
  async resetPassword(id: number): Promise<void> {
    if (useMock) { await delay(300); return }
    return api.post(`/users/${id}/reset-password`)
  },
  async updateUserStatus(id: number, status: string): Promise<User> {
    if (useMock) {
      await delay(300)
      const index = mockUsers.findIndex(u => u.id === id)
      if (index === -1) throw new Error('用户不存在')
      mockUsers[index].status = status as any
      mockUsers[index].updatedAt = new Date().toLocaleString('zh-CN')
      return mockUsers[index]
    }
    return api.put(`/users/${id}/status`, { status })
  },
  async syncFromUserInfo(params?: { role?: UserRole, status?: UserStatus }): Promise<{ created: number }> {
    return api.post('/users/sync', null, { params })
  }
}
