import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/modules/auth/pages/Login.vue'),
    meta: { title: '管理员登录', requiresAuth: false }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/index.vue'),
    redirect: '/dashboard',
    meta: { title: '主控制台', requiresAuth: true },
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'Odometer' }
      },
      {
        path: '/users',
        name: 'Users',
        component: () => import('@/modules/users/pages/index.vue'),
        meta: { title: '用户管理', icon: 'User', permissions: ['user:read'] }
      },
      {
        path: '/counselors',
        name: 'Counselors',
        component: () => import('@/modules/counselors/pages/Review.vue'),
        meta: { title: '咨询师审核', icon: 'Avatar', permissions: ['counselor:read'] }
      },
      {
        path: '/content',
        name: 'Content',
        component: () => import('@/views/Content/index.vue'),
        meta: { title: '内容审核', icon: 'Document', permissions: ['content:read'] }
      },
      {
        path: '/assessments',
        name: 'Assessments',
        component: () => import('@/views/Assessments/index.vue'),
        meta: { title: '测评表管理', icon: 'Edit', permissions: ['assessment:read'] }
      },
      {
        path: '/reports',
        name: 'Reports',
        component: () => import('@/views/Reports/index.vue'),
        meta: { title: '数据报表', icon: 'TrendCharts', permissions: ['report:read'] }
      },
      {
        path: '/monitor',
        name: 'Monitor',
        component: () => import('@/views/Monitor/index.vue'),
        meta: { title: '系统监控', icon: 'Monitor', permissions: ['monitor:read'] }
      },
      {
        path: '/files',
        name: 'Files',
        component: () => import('@/views/Files/index.vue'),
        meta: { title: '档案管理', icon: 'Folder', permissions: ['file:read'] }
      },
      {
        path: '/settings',
        name: 'Settings',
        component: () => import('@/views/Settings/index.vue'),
        meta: { title: '系统设置', icon: 'Setting', permissions: ['setting:read'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/Error/404.vue'),
    meta: { title: '页面未找到', requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  const isAdmin = authStore.user?.role === 'admin'
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 心桥心理管理端` : '心桥心理管理端'
  
  // 检查是否需要认证
  if (to.meta.requiresAuth !== false) {
    if (!authStore.isAuthenticated || !isAdmin) {
      next('/login')
      return
    }
    // 管理端取消权限校验，admin 登录后允许访问所有页面
  }
  
  // 如果已登录，阻止访问登录页
  if (to.path === '/login' && authStore.isAuthenticated && isAdmin) {
    next('/dashboard')
    return
  }
  
  next()
})

export default router
