import { ref, computed } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

// 用户增长趋势图
export const useUserGrowthChart = () => {
  const chartRef = ref<HTMLElement>()
  let chartInstance: echarts.ECharts | null = null

  const option = computed<EChartsOption>(() => ({
    title: {
      text: '用户增长趋势',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['新增用户', '活跃用户'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月']
    },
    yAxis: {
      type: 'value',
      name: '用户数'
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: [120, 132, 101, 134, 90, 230, 210],
        itemStyle: {
          color: '#667eea'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0.1)' }
          ])
        }
      },
      {
        name: '活跃用户',
        type: 'line',
        smooth: true,
        data: [220, 182, 191, 234, 290, 330, 310],
        itemStyle: {
          color: '#10b981'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16, 185, 129, 0.3)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.1)' }
          ])
        }
      }
    ]
  }))

  const initChart = () => {
    if (chartRef.value) {
      chartInstance = echarts.init(chartRef.value)
      chartInstance.setOption(option.value)
      
      // 响应式
      window.addEventListener('resize', () => {
        chartInstance?.resize()
      })
    }
  }

  const destroyChart = () => {
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
  }

  return {
    chartRef,
    initChart,
    destroyChart
  }
}

// 咨询类型分布饼图
export const useConsultationTypeChart = () => {
  const chartRef = ref<HTMLElement>()
  let chartInstance: echarts.ECharts | null = null

  const option = computed<EChartsOption>(() => ({
    title: {
      text: '咨询类型分布',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: ['情感咨询', '职场压力', '人际关系', '学习困难', '焦虑抑郁', '其他']
    },
    series: [
      {
        name: '咨询类型',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '20',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: 335, name: '情感咨询', itemStyle: { color: '#667eea' } },
          { value: 310, name: '职场压力', itemStyle: { color: '#10b981' } },
          { value: 234, name: '人际关系', itemStyle: { color: '#f59e0b' } },
          { value: 135, name: '学习困难', itemStyle: { color: '#ef4444' } },
          { value: 148, name: '焦虑抑郁', itemStyle: { color: '#8b5cf6' } },
          { value: 89, name: '其他', itemStyle: { color: '#6b7280' } }
        ]
      }
    ]
  }))

  const initChart = () => {
    if (chartRef.value) {
      chartInstance = echarts.init(chartRef.value)
      chartInstance.setOption(option.value)
      
      window.addEventListener('resize', () => {
        chartInstance?.resize()
      })
    }
  }

  const destroyChart = () => {
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
  }

  return {
    chartRef,
    initChart,
    destroyChart
  }
}

// 收入趋势柱状图
export const useRevenueChart = () => {
  const chartRef = ref<HTMLElement>()
  let chartInstance: echarts.ECharts | null = null

  const option = computed<EChartsOption>(() => ({
    title: {
      text: '收入趋势分析',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['咨询收入', '课程收入', '测评收入'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月']
    },
    yAxis: {
      type: 'value',
      name: '收入 (万元)'
    },
    series: [
      {
        name: '咨询收入',
        type: 'bar',
        data: [2.0, 4.9, 7.0, 23.2, 25.6, 76.7],
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#764ba2' }
          ])
        }
      },
      {
        name: '课程收入',
        type: 'bar',
        data: [2.6, 5.9, 9.0, 26.4, 28.7, 70.7],
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#10b981' },
            { offset: 1, color: '#059669' }
          ])
        }
      },
      {
        name: '测评收入',
        type: 'bar',
        data: [1.6, 3.9, 5.0, 16.4, 18.7, 50.7],
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#f59e0b' },
            { offset: 1, color: '#d97706' }
          ])
        }
      }
    ]
  }))

  const initChart = () => {
    if (chartRef.value) {
      chartInstance = echarts.init(chartRef.value)
      chartInstance.setOption(option.value)
      
      window.addEventListener('resize', () => {
        chartInstance?.resize()
      })
    }
  }

  const destroyChart = () => {
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
  }

  return {
    chartRef,
    initChart,
    destroyChart
  }
}