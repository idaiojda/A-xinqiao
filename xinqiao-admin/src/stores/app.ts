import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface AppState {
  sidebar: {
    opened: boolean
    withoutAnimation: boolean
  }
  device: 'desktop' | 'mobile'
  theme: 'light' | 'dark'
  size: 'large' | 'default' | 'small'
}

export const useAppStore = defineStore('app', () => {
  const sidebar = ref({
    opened: true,
    withoutAnimation: false
  })
  const device = ref<'desktop' | 'mobile'>('desktop')
  const theme = ref<'light' | 'dark'>('light')
  const size = ref<'large' | 'default' | 'small'>('default')

  const toggleSidebar = (withoutAnimation = false) => {
    sidebar.value.opened = !sidebar.value.opened
    sidebar.value.withoutAnimation = withoutAnimation
  }

  const closeSidebar = (withoutAnimation = false) => {
    sidebar.value.opened = false
    sidebar.value.withoutAnimation = withoutAnimation
  }

  const toggleDevice = (deviceType: 'desktop' | 'mobile') => {
    device.value = deviceType
  }

  const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    document.documentElement.setAttribute('data-theme', theme.value)
  }

  const setSize = (sizeType: 'large' | 'default' | 'small') => {
    size.value = sizeType
  }

  return {
    sidebar,
    device,
    theme,
    size,
    toggleSidebar,
    closeSidebar,
    toggleDevice,
    toggleTheme,
    setSize
  }
})