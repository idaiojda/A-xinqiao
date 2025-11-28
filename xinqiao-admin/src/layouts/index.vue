<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside 
      :width="appStore.sidebar.opened ? '260px' : '64px'" 
      class="sidebar"
      :class="{ 'sidebar-collapse': !appStore.sidebar.opened }"
    >
      <div class="sidebar-header">
        <div class="logo">
          <img src="/logo.svg" alt="心桥心理" />
          <span v-show="appStore.sidebar.opened" class="logo-text">心桥心理</span>
        </div>
      </div>
      
      <el-menu
        :default-active="$route.path"
        :collapse="!appStore.sidebar.opened"
        :collapse-transition="false"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        unique-opened
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-menu-item 
            v-if="!route.children || route.children.length === 0"
            :index="route.path"
            :route="route"
          >
            <el-icon>
              <component :is="route.meta?.icon" />
            </el-icon>
            <template #title>
              <span>{{ route.meta?.title }}</span>
            </template>
          </el-menu-item>
          
          <el-sub-menu v-else :index="route.path">
            <template #title>
              <el-icon>
                <component :is="route.meta?.icon" />
              </el-icon>
              <span>{{ route.meta?.title }}</span>
            </template>
            <el-menu-item
              v-for="child in route.children"
              :key="child.path"
              :index="child.path"
              :route="child"
            >
              <el-icon>
                <component :is="child.meta?.icon" />
              </el-icon>
              <span>{{ child.meta?.title }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-button
            text
            @click="toggleSidebar"
            class="sidebar-toggle"
          >
            <el-icon size="20">
              <Menu v-if="!appStore.sidebar.opened" />
              <Close v-else />
            </el-icon>
          </el-button>
          
          <el-breadcrumb separator="/">
            <el-breadcrumb-item 
              v-for="item in breadcrumbs" 
              :key="item.path"
              :to="item.path"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-button text @click="toggleTheme" class="theme-toggle">
            <el-icon size="18">
              <Sunny v-if="appStore.theme === 'dark'" />
              <Moon v-else />
            </el-icon>
          </el-button>
          
          <el-dropdown @command="handleUserAction">
            <div class="user-info">
              <el-avatar :size="32" :src="authStore.user?.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="username">{{ authStore.user?.username }}</span>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { Menu, Close, Sunny, Moon, User, ArrowDown, Setting, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

// 菜单路由（取消权限过滤，admin 登录显示全部）
const menuRoutes = computed(() => {
  const layoutChildren = router.getRoutes().find(r => r.name === 'Layout')?.children || []
  return layoutChildren.filter(route => route.meta?.title)
})

// 面包屑
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta?.title)
  return matched.map(item => ({
    path: item.path,
    title: item.meta?.title
  }))
})

// 切换侧边栏
const toggleSidebar = () => {
  appStore.toggleSidebar()
}

// 切换主题
const toggleTheme = () => {
  appStore.toggleTheme()
}

// 用户操作
const handleUserAction = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/settings')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    authStore.clearAuth()
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  
  &.sidebar-collapse {
    .logo-text {
      display: none;
    }
  }
  
  .sidebar-header {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: 1px solid #434a50;
    
    .logo {
      display: flex;
      align-items: center;
      color: white;
      
      img {
        width: 32px;
        height: 32px;
        margin-right: 8px;
      }
      
      .logo-text {
        font-size: 18px;
        font-weight: 600;
      }
    }
  }
  
  .el-menu {
    border-right: none;
  }
}

.header {
  background-color: white;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  
  .header-left {
    display: flex;
    align-items: center;
    
    .sidebar-toggle {
      margin-right: 20px;
    }
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .theme-toggle {
      color: #606266;
    }
    
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 6px;
      transition: background-color 0.3s;
      
      &:hover {
        background-color: #f5f7fa;
      }
      
      .username {
        margin: 0 8px;
        font-size: 14px;
        color: #606266;
      }
      
      .dropdown-icon {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}

// 过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s;
    
    &.sidebar-open {
      transform: translateX(0);
    }
  }
  
  .header {
    padding: 0 16px;
  }
}
</style>
