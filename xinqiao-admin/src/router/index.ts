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
      // ① 数据看板
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'Odometer' }
      },
      // ② 用户管理
      {
        path: '/users',
        name: 'Users',
        component: () => import('@/modules/users/pages/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      // ③ 咨询师审核
      {
        path: '/counselors',
        name: 'Counselors',
        component: () => import('@/modules/counselors/pages/Review.vue'),
        meta: { title: '咨询师审核', icon: 'Avatar' }
      },
      // ④ 内容管理（含三个子页面）
      {
        path: '/content',
        name: 'Content',
        redirect: '/content/articles',
        meta: { title: '内容管理', icon: 'Document' },
        children: [
          {
            path: '/content/articles',
            name: 'ContentArticles',
            component: () => import('@/views/Content/ArticleReview.vue'),
            meta: { title: '文章审核', icon: 'Reading' }
          },
          {
            path: '/content/assessments',
            name: 'ContentAssessments',
            component: () => import('@/views/Content/AssessmentReview.vue'),
            meta: { title: '测评审核', icon: 'EditPen' }
          },
          {
            path: '/content/courses',
            name: 'ContentCourses',
            component: () => import('@/views/Content/CourseReview.vue'),
            meta: { title: '课程审核', icon: 'VideoPlay' }
          },
          {
            path: '/content/posts',
            name: 'ContentPosts',
            component: () => import('@/views/Content/PostReview.vue'),
            meta: { title: '帖子审核', icon: 'ChatDotRound' }
          }
        ]
      },
      // ⑤ 诊疗档案管理
      {
        path: '/medical-records',
        name: 'MedicalRecords',
        component: () => import('@/views/MedicalRecords/index.vue'),
        meta: { title: '档案管理', icon: 'FolderOpened' }
      },
      // ⑥ 个人信息
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/Profile/index.vue'),
        meta: { title: '个人信息', icon: 'UserFilled' }
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
