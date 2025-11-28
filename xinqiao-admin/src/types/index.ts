// 用户相关类型
export interface User {
  id: number
  username: string
  phone?: string
  avatar?: string
  role: UserRole
  status: UserStatus
  createdAt: string
  updatedAt: string
}

export enum UserRole {
  ADMIN = 'admin',
  COUNSELOR = 'counselor',
  USER = 'user'
}

export enum UserStatus {
  ACTIVE = 'active',
  INACTIVE = 'inactive',
  PENDING = 'pending',
  BANNED = 'banned'
}

// 咨询师相关类型
export interface Counselor {
  id: number
  userId: number
  name: string
  title: string
  description: string
  qualifications: string[]
  experience: number
  rating: number
  consultationCount: number
  status: CounselorStatus
  createdAt: string
  updatedAt: string
}

export enum CounselorStatus {
  PENDING = 'pending',
  APPROVED = 'approved',
  REJECTED = 'rejected',
  SUSPENDED = 'suspended'
}

// 内容相关类型
export interface Article {
  id: number
  title: string
  content: string
  author: string
  category: string
  tags: string[]
  coverImage?: string
  status: ContentStatus
  viewCount: number
  likeCount: number
  createdAt: string
  updatedAt: string
}

export interface Assessment {
  id: number
  title: string
  description: string
  questions: Question[]
  category: string
  duration: number
  status: ContentStatus
  createdAt: string
  updatedAt: string
}

export interface Question {
  id: number
  question: string
  type: QuestionType
  options?: string[]
  required: boolean
}

export enum QuestionType {
  SINGLE = 'single',
  MULTIPLE = 'multiple',
  TEXT = 'text',
  RATING = 'rating'
}

export enum ContentStatus {
  DRAFT = 'draft',
  PENDING = 'pending',
  PUBLISHED = 'published',
  REJECTED = 'rejected'
}

// 统计数据类型
export interface DashboardStats {
  totalUsers: number
  totalCounselors: number
  totalConsultations: number
  totalRevenue: number
  userGrowth: number[]
  consultationTrend: number[]
  revenueTrend: number[]
  categoryStats: CategoryStat[]
}

export interface CategoryStat {
  name: string
  value: number
  percentage: number
}

// API响应类型
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface PageResponse<T> {
  list: T[]
  total: number
  page: number
  size: number
  pages: number
}

// 用户表单类型
export interface UserForm {
  username: string
  phone?: string
  avatar?: string
  role: UserRole
  status: UserStatus
}

// 登录相关类型
export interface LoginForm {
  username: string
  password: string
  rememberMe: boolean
}

export interface AuthInfo {
  token: string
  user: User
  permissions: string[]
}

// 系统监控类型
export interface SystemStats {
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  onlineUsers: number
  activeSessions: number
  serverStatus: 'healthy' | 'warning' | 'critical'
}