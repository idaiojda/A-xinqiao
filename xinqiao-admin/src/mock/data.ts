import type { User, UserRole, UserStatus, DashboardStats, CategoryStat } from '@/types'

// 模拟用户数据
export const mockUsers: User[] = [
  {
    id: 1,
    username: 'admin',
    email: 'admin@xinqiao.com',
    phone: '13800138000',
    avatar: '',
    role: 'admin' as UserRole,
    status: 'active' as UserStatus,
    createdAt: '2024-01-01 10:00:00',
    updatedAt: '2024-01-01 10:00:00'
  },
  {
    id: 2,
    username: 'counselor1',
    email: 'counselor1@xinqiao.com',
    phone: '13900139000',
    avatar: '',
    role: 'counselor' as UserRole,
    status: 'active' as UserStatus,
    createdAt: '2024-01-02 10:00:00',
    updatedAt: '2024-01-02 10:00:00'
  },
  {
    id: 3,
    username: 'user1',
    email: 'user1@xinqiao.com',
    phone: '13700137000',
    avatar: '',
    role: 'user' as UserRole,
    status: 'active' as UserStatus,
    createdAt: '2024-01-03 10:00:00',
    updatedAt: '2024-01-03 10:00:00'
  }
]

// 模拟统计数据
export const mockDashboardStats: DashboardStats = {
  totalUsers: 12847,
  totalCounselors: 342,
  totalConsultations: 8932,
  totalRevenue: 456789,
  userGrowth: [120, 132, 101, 134, 90, 230, 210],
  consultationTrend: [220, 182, 191, 234, 290, 330, 310],
  revenueTrend: [2.0, 4.9, 7.0, 23.2, 25.6, 76.7, 89.5],
  categoryStats: [
    { name: '情感咨询', value: 335, percentage: 28.5 },
    { name: '职场压力', value: 310, percentage: 26.4 },
    { name: '人际关系', value: 234, percentage: 19.9 },
    { name: '学习困难', value: 135, percentage: 11.5 },
    { name: '焦虑抑郁', value: 148, percentage: 12.6 },
    { name: '其他', value: 89, percentage: 7.6 }
  ]
}

// 模拟API响应延迟
export const delay = (ms: number = 500) => {
  return new Promise(resolve => setTimeout(resolve, ms))
}