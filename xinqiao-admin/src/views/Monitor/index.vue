<template>
  <div class="monitor-container">
    <!-- 系统状态概览 -->
    <el-row :gutter="20" class="system-status-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="status-card" :class="{ 'status-healthy': systemStatus === 'healthy', 'status-warning': systemStatus === 'warning', 'status-critical': systemStatus === 'critical' }">
          <div class="status-content">
            <div class="status-icon">
              <el-icon size="32"><Monitor /></el-icon>
            </div>
            <div class="status-info">
              <div class="status-title">系统状态</div>
              <div class="status-value">{{ getStatusText(systemStatus) }}</div>
              <div class="status-uptime">运行时间: {{ systemUptime }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon cpu">
              <el-icon size="32"><Cpu /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-label">CPU使用率</div>
              <div class="metric-value">{{ systemMetrics.cpu }}%</div>
              <el-progress :percentage="systemMetrics.cpu" :color="getProgressColor(systemMetrics.cpu)" />
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon memory">
              <el-icon size="32"><Histogram /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-label">内存使用率</div>
              <div class="metric-value">{{ systemMetrics.memory }}%</div>
              <el-progress :percentage="systemMetrics.memory" :color="getProgressColor(systemMetrics.memory)" />
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon disk">
              <el-icon size="32"><Coin /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-label">磁盘使用率</div>
              <div class="metric-value">{{ systemMetrics.disk }}%</div>
              <el-progress :percentage="systemMetrics.disk" :color="getProgressColor(systemMetrics.disk)" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时监控图表 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>系统资源监控</span>
              <el-switch
                v-model="realtimeEnabled"
                active-text="实时"
                inactive-text="暂停"
                @change="handleRealtimeToggle"
              />
            </div>
          </template>
          <div ref="resourceChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>网络流量监控</span>
              <el-button type="primary" link @click="exportMonitorData">
                <el-icon><Download /></el-icon>
                导出
              </el-button>
            </div>
          </template>
          <div ref="networkChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 服务状态 -->
    <el-card class="services-card">
      <template #header>
        <div class="card-header">
          <span>服务状态监控</span>
          <div class="header-actions">
            <el-button size="small" @click="refreshServices">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
            <el-button size="small" type="primary" @click="checkAllServices">
              全部检查
            </el-button>
          </div>
        </div>
      </template>
      
      <el-table
        v-loading="servicesLoading"
        :data="services"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="name" label="服务名称" min-width="150" />
        <el-table-column prop="host" label="主机地址" width="150" />
        <el-table-column prop="port" label="端口" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getServiceStatusType(row.status)" effect="dark">
              {{ getServiceStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseTime" label="响应时间" width="100">
          <template #default="{ row }">
            <span :class="getResponseTimeClass(row.responseTime)">
              {{ row.responseTime }}ms
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="uptime" label="运行时间" width="120" />
        <el-table-column prop="lastCheck" label="最后检查" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="checkService(row)">检查</el-button>
            <el-button 
              v-if="row.status !== 'running'" 
              type="success" 
              link 
              @click="startService(row)"
            >
              启动
            </el-button>
            <el-button 
              v-if="row.status === 'running'" 
              type="warning" 
              link 
              @click="restartService(row)"
            >
              重启
            </el-button>
            <el-button type="danger" link @click="stopService(row)">停止</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 告警记录 -->
    <el-card class="alerts-card">
      <template #header>
        <div class="card-header">
          <span>告警记录</span>
          <div class="header-actions">
            <el-select v-model="alertLevel" placeholder="告警级别" size="small" style="width: 120px">
              <el-option label="全部" value="" />
              <el-option label="严重" value="critical" />
              <el-option label="警告" value="warning" />
              <el-option label="信息" value="info" />
            </el-select>
            <el-button size="small" @click="clearAlerts" type="danger">
              <el-icon><Delete /></el-icon>
              清空
            </el-button>
            <el-button size="small" @click="refreshAlerts">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>
      
      <el-table
        v-loading="alertsLoading"
        :data="filteredAlerts"
        stripe
        style="width: 100%"
        :default-sort="{ prop: 'timestamp', order: 'descending' }"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="alert-detail">
              <h4>告警详情</h4>
              <p><strong>告警描述:</strong> {{ row.description }}</p>
              <p><strong>建议措施:</strong> {{ row.suggestion }}</p>
              <p><strong>相关指标:</strong></p>
              <ul>
                <li v-for="(value, key) in row.metrics" :key="key">
                  {{ key }}: {{ value }}
                </li>
              </ul>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="timestamp" label="时间" width="160" sortable />
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="getAlertLevelType(row.level)" effect="dark">
              {{ getAlertLevelText(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="service" label="服务" width="120" />
        <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'resolved' ? 'success' : 'warning'">
              {{ row.status === 'resolved' ? '已解决' : '未解决' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status !== 'resolved'" 
              type="success" 
              link 
              @click="resolveAlert(row)"
            >
              标记解决
            </el-button>
            <el-button type="primary" link @click="viewAlertDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="alertPagination.page"
          v-model:page-size="alertPagination.size"
          :total="alertPagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleAlertSizeChange"
          @current-change="handleAlertCurrentChange"
        />
      </div>
    </el-card>

    <!-- 性能指标 -->
    <el-card class="performance-card">
      <template #header>
        <div class="card-header">
          <span>性能指标监控</span>
          <el-radio-group v-model="performancePeriod" size="small">
            <el-radio-button label="1h">1小时</el-radio-button>
            <el-radio-button label="6h">6小时</el-radio-button>
            <el-radio-button label="24h">24小时</el-radio-button>
            <el-radio-button label="7d">7天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :lg="8" v-for="metric in performanceMetrics" :key="metric.name">
          <div class="performance-metric">
            <div class="metric-header">
              <span class="metric-name">{{ metric.name }}</span>
              <span class="metric-value" :class="getMetricValueClass(metric.value, metric.threshold)">
                {{ metric.value }}{{ metric.unit }}
              </span>
            </div>
            <el-progress
              :percentage="getMetricPercentage(metric.value, metric.threshold)"
              :color="getMetricProgressColor(metric.value, metric.threshold)"
              :stroke-width="8"
            />
            <div class="metric-footer">
              <span class="metric-description">{{ metric.description }}</span>
              <span class="metric-threshold">阈值: {{ metric.threshold }}{{ metric.unit }}</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Monitor, Cpu, Histogram, Coin, Download, Refresh, Delete,
  Top, Bottom, Warning, CircleCheck
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// 系统状态
const systemStatus = ref('healthy')
const systemUptime = ref('99.9%')
const realtimeEnabled = ref(true)
const servicesLoading = ref(false)
const alertsLoading = ref(false)
const alertLevel = ref('')
const performancePeriod = ref('1h')

// 系统指标
const systemMetrics = reactive({
  cpu: 45,
  memory: 62,
  disk: 78
})

// 图表引用
const resourceChartRef = ref<HTMLElement>()
const networkChartRef = ref<HTMLElement>()

let resourceChart: echarts.ECharts | null = null
let networkChart: echarts.ECharts | null = null
let realtimeTimer: number | null = null

// 服务列表
const services = ref([
  {
    name: 'Web服务器',
    host: 'localhost',
    port: 8080,
    status: 'running',
    responseTime: 45,
    uptime: '30天',
    lastCheck: '2024-01-15 10:30:00'
  },
  {
    name: '数据库服务',
    host: 'localhost',
    port: 3306,
    status: 'running',
    responseTime: 12,
    uptime: '30天',
    lastCheck: '2024-01-15 10:30:00'
  },
  {
    name: 'Redis缓存',
    host: 'localhost',
    port: 6379,
    status: 'warning',
    responseTime: 234,
    uptime: '15天',
    lastCheck: '2024-01-15 10:30:00'
  },
  {
    name: '消息队列',
    host: 'localhost',
    port: 5672,
    status: 'stopped',
    responseTime: 0,
    uptime: '0天',
    lastCheck: '2024-01-15 10:25:00'
  }
])

// 告警记录
const alerts = ref([
  {
    id: 1,
    timestamp: '2024-01-15 10:25:00',
    level: 'warning',
    type: 'performance',
    service: 'Redis缓存',
    message: 'Redis响应时间超过200ms',
    status: 'unresolved',
    description: 'Redis缓存服务响应时间异常，当前响应时间为234ms',
    suggestion: '检查Redis服务器负载，考虑增加内存或优化查询',
    metrics: {
      responseTime: '234ms',
      cpuUsage: '85%',
      memoryUsage: '92%'
    }
  },
  {
    id: 2,
    timestamp: '2024-01-15 09:45:00',
    level: 'critical',
    type: 'availability',
    service: '消息队列',
    message: '消息队列服务不可用',
    status: 'unresolved',
    description: '消息队列服务停止响应，无法建立连接',
    suggestion: '检查服务状态，重启消息队列服务',
    metrics: {
      status: 'stopped',
      lastSeen: '45分钟前'
    }
  },
  {
    id: 3,
    timestamp: '2024-01-15 08:30:00',
    level: 'info',
    type: 'security',
    service: 'Web服务器',
    message: '检测到异常登录尝试',
    status: 'resolved',
    description: '系统检测到来自异常IP的登录尝试',
    suggestion: '已自动阻止该IP，建议加强安全策略',
    metrics: {
      ipAddress: '192.168.1.100',
      attempts: 5,
      action: 'blocked'
    }
  }
])

// 告警分页
const alertPagination = reactive({
  page: 1,
  size: 10,
  total: 3
})

// 性能指标
const performanceMetrics = ref([
  {
    name: '平均响应时间',
    value: 145,
    unit: 'ms',
    threshold: 200,
    description: '系统平均响应时间'
  },
  {
    name: '并发连接数',
    value: 1234,
    unit: '',
    threshold: 2000,
    description: '当前并发连接数量'
  },
  {
    name: '错误率',
    value: 0.02,
    unit: '%',
    threshold: 0.1,
    description: '请求错误率'
  },
  {
    name: '吞吐量',
    value: 2345,
    unit: 'req/s',
    threshold: 3000,
    description: '每秒处理请求数'
  },
  {
    name: '数据库连接数',
    value: 45,
    unit: '',
    threshold: 100,
    description: '数据库连接池使用情况'
  },
  {
    name: '缓存命中率',
    value: 87.5,
    unit: '%',
    threshold: 90,
    description: 'Redis缓存命中率'
  }
])

// 过滤后的告警
const filteredAlerts = computed(() => {
  if (!alertLevel.value) return alerts.value
  return alerts.value.filter(alert => alert.level === alertLevel.value)
})

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap = {
    healthy: '健康',
    warning: '警告',
    critical: '严重'
  }
  return statusMap[status] || '未知'
}

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
  if (percentage < 60) return '#10b981'
  if (percentage < 80) return '#f59e0b'
  return '#ef4444'
}

// 获取服务状态类型
const getServiceStatusType = (status: string) => {
  const statusMap = {
    running: 'success',
    warning: 'warning',
    stopped: 'danger',
    unknown: 'info'
  }
  return statusMap[status] || 'info'
}

// 获取服务状态文本
const getServiceStatusText = (status: string) => {
  const statusMap = {
    running: '运行中',
    warning: '警告',
    stopped: '已停止',
    unknown: '未知'
  }
  return statusMap[status] || '未知'
}

// 获取响应时间类名
const getResponseTimeClass = (responseTime: number) => {
  if (responseTime < 100) return 'response-good'
  if (responseTime < 300) return 'response-warning'
  return 'response-bad'
}

// 获取告警级别类型
const getAlertLevelType = (level: string) => {
  const levelMap = {
    critical: 'danger',
    warning: 'warning',
    info: 'info'
  }
  return levelMap[level] || 'info'
}

// 获取告警级别文本
const getAlertLevelText = (level: string) => {
  const levelMap = {
    critical: '严重',
    warning: '警告',
    info: '信息'
  }
  return levelMap[level] || '未知'
}

// 获取指标值类名
const getMetricValueClass = (value: number, threshold: number) => {
  const percentage = (value / threshold) * 100
  if (percentage < 60) return 'metric-good'
  if (percentage < 80) return 'metric-warning'
  return 'metric-bad'
}

// 获取指标百分比
const getMetricPercentage = (value: number, threshold: number) => {
  return Math.min((value / threshold) * 100, 100)
}

// 获取指标进度条颜色
const getMetricProgressColor = (value: number, threshold: number) => {
  const percentage = (value / threshold) * 100
  if (percentage < 60) return '#10b981'
  if (percentage < 80) return '#f59e0b'
  return '#ef4444'
}

// 初始化资源监控图表
const initResourceChart = () => {
  if (!resourceChartRef.value) return
  
  resourceChart = echarts.init(resourceChartRef.value)
  
  const option = {
    title: {
      text: '系统资源使用情况',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['CPU', '内存', '磁盘'],
      top: 30
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: {
        formatter: '{value}%'
      }
    },
    series: [
      {
        name: 'CPU',
        type: 'line',
        data: [45, 52, 48, 65, 58, 42, 45],
        smooth: true,
        areaStyle: {
          opacity: 0.3
        },
        itemStyle: {
          color: '#667eea'
        }
      },
      {
        name: '内存',
        type: 'line',
        data: [62, 68, 65, 72, 69, 58, 62],
        smooth: true,
        areaStyle: {
          opacity: 0.3
        },
        itemStyle: {
          color: '#10b981'
        }
      },
      {
        name: '磁盘',
        type: 'line',
        data: [78, 78, 79, 80, 79, 78, 78],
        smooth: true,
        areaStyle: {
          opacity: 0.3
        },
        itemStyle: {
          color: '#f59e0b'
        }
      }
    ]
  }
  
  resourceChart.setOption(option)
}

// 初始化网络监控图表
const initNetworkChart = () => {
  if (!networkChartRef.value) return
  
  networkChart = echarts.init(networkChartRef.value)
  
  const option = {
    title: {
      text: '网络流量监控',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['上行流量', '下行流量'],
      top: 30
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: '{value} MB/s'
      }
    },
    series: [
      {
        name: '上行流量',
        type: 'line',
        data: [120, 132, 101, 134, 90, 230, 210],
        smooth: true,
        areaStyle: {
          opacity: 0.3
        },
        itemStyle: {
          color: '#667eea'
        }
      },
      {
        name: '下行流量',
        type: 'line',
        data: [220, 182, 191, 234, 290, 330, 310],
        smooth: true,
        areaStyle: {
          opacity: 0.3
        },
        itemStyle: {
          color: '#10b981'
        }
      }
    ]
  }
  
  networkChart.setOption(option)
}

// 更新实时监控数据
const updateRealtimeData = () => {
  // 模拟实时数据更新
  systemMetrics.cpu = Math.floor(Math.random() * 40) + 30
  systemMetrics.memory = Math.floor(Math.random() * 30) + 50
  systemMetrics.disk = Math.floor(Math.random() * 10) + 75
  
  // 更新图表数据
  if (resourceChart && realtimeEnabled.value) {
    const option = resourceChart.getOption()
    if (option.series && option.series.length > 0) {
      option.series[0].data = option.series[0].data.map(() => Math.floor(Math.random() * 40) + 30)
      option.series[1].data = option.series[1].data.map(() => Math.floor(Math.random() * 30) + 50)
      option.series[2].data = option.series[2].data.map(() => Math.floor(Math.random() * 10) + 75)
      resourceChart.setOption(option)
    }
  }
  
  if (networkChart && realtimeEnabled.value) {
    const option = networkChart.getOption()
    if (option.series && option.series.length > 0) {
      option.series[0].data = option.series[0].data.map(() => Math.floor(Math.random() * 100) + 100)
      option.series[1].data = option.series[1].data.map(() => Math.floor(Math.random() * 100) + 200)
      networkChart.setOption(option)
    }
  }
}

// 处理实时切换
const handleRealtimeToggle = (enabled: boolean) => {
  if (enabled) {
    ElMessage.success('实时监控已开启')
    startRealtimeUpdate()
  } else {
    ElMessage.info('实时监控已暂停')
    stopRealtimeUpdate()
  }
}

// 开始实时更新
const startRealtimeUpdate = () => {
  if (realtimeTimer) return
  realtimeTimer = window.setInterval(updateRealtimeData, 2000)
}

// 停止实时更新
const stopRealtimeUpdate = () => {
  if (realtimeTimer) {
    window.clearInterval(realtimeTimer)
    realtimeTimer = null
  }
}

// 导出监控数据
const exportMonitorData = () => {
  ElMessage.success('正在导出监控数据...')
}

// 刷新服务
const refreshServices = () => {
  servicesLoading.value = true
  setTimeout(() => {
    servicesLoading.value = false
    ElMessage.success('服务列表已刷新')
  }, 1000)
}

// 检查所有服务
const checkAllServices = () => {
  ElMessage.success('正在检查所有服务...')
}

// 检查服务
const checkService = (service: any) => {
  ElMessage.success(`正在检查 ${service.name}...`)
}

// 启动服务
const startService = (service: any) => {
  ElMessageBox.confirm(`确定要启动 ${service.name} 服务吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    service.status = 'running'
    service.responseTime = Math.floor(Math.random() * 100) + 20
    ElMessage.success(`${service.name} 服务已启动`)
  })
}

// 重启服务
const restartService = (service: any) => {
  ElMessageBox.confirm(`确定要重启 ${service.name} 服务吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    service.status = 'warning'
    service.responseTime = 0
    setTimeout(() => {
      service.status = 'running'
      service.responseTime = Math.floor(Math.random() * 100) + 20
      ElMessage.success(`${service.name} 服务已重启`)
    }, 2000)
  })
}

// 停止服务
const stopService = (service: any) => {
  ElMessageBox.confirm(`确定要停止 ${service.name} 服务吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    service.status = 'stopped'
    service.responseTime = 0
    ElMessage.success(`${service.name} 服务已停止`)
  })
}

// 清空告警
const clearAlerts = () => {
  ElMessageBox.confirm('确定要清空所有告警记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    alerts.value = []
    alertPagination.total = 0
    ElMessage.success('告警记录已清空')
  })
}

// 刷新告警
const refreshAlerts = () => {
  alertsLoading.value = true
  setTimeout(() => {
    alertsLoading.value = false
    ElMessage.success('告警记录已刷新')
  }, 1000)
}

// 解决告警
const resolveAlert = (alert: any) => {
  alert.status = 'resolved'
  ElMessage.success('告警已标记为已解决')
}

// 查看告警详情
const viewAlertDetail = (alert: any) => {
  ElMessage.info(`查看告警详情: ${alert.message}`)
}

// 告警分页
const handleAlertSizeChange = (size: number) => {
  alertPagination.size = size
}

const handleAlertCurrentChange = (page: number) => {
  alertPagination.page = page
}

onMounted(() => {
  initResourceChart()
  initNetworkChart()
  startRealtimeUpdate()
  
  const handleResize = () => {
    if (resourceChart) resourceChart.resize()
    if (networkChart) networkChart.resize()
  }
  window.addEventListener('resize', handleResize)
  ;(window as any)._monitorResizeHandler = handleResize
})

onUnmounted(() => {
  stopRealtimeUpdate()
  if (resourceChart) {
    resourceChart.dispose()
    resourceChart = null
  }
  if (networkChart) {
    networkChart.dispose()
    networkChart = null
  }
  const h = (window as any)._monitorResizeHandler
  if (h) window.removeEventListener('resize', h)
})
</script>

<style scoped lang="scss">
.monitor-container {
  padding: 20px;
}

.system-status-row {
  margin-bottom: 20px;
  
  .status-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    transition: transform 0.3s, box-shadow 0.3s;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
    }
    
    &.status-healthy {
      border-left: 4px solid #10b981;
    }
    
    &.status-warning {
      border-left: 4px solid #f59e0b;
    }
    
    &.status-critical {
      border-left: 4px solid #ef4444;
    }
    
    .status-content {
      display: flex;
      align-items: center;
      
      .status-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16px;
        color: white;
      }
      
      .status-info {
        flex: 1;
        
        .status-title {
          font-size: 14px;
          color: #6b7280;
          margin-bottom: 4px;
        }
        
        .status-value {
          font-size: 20px;
          font-weight: 600;
          color: #1f2937;
          margin-bottom: 4px;
        }
        
        .status-uptime {
          font-size: 12px;
          color: #6b7280;
        }
      }
    }
  }
  
  .metric-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    transition: transform 0.3s, box-shadow 0.3s;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
    }
    
    .metric-content {
      display: flex;
      align-items: center;
      
      .metric-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16px;
        color: white;
        
        &.cpu {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }
        
        &.memory {
          background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        }
        
        &.disk {
          background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        }
      }
      
      .metric-info {
        flex: 1;
        
        .metric-label {
          font-size: 14px;
          color: #6b7280;
          margin-bottom: 4px;
        }
        
        .metric-value {
          font-size: 24px;
          font-weight: 600;
          color: #1f2937;
          margin-bottom: 8px;
        }
      }
    }
  }
}

.charts-row {
  margin-bottom: 20px;
  
  .chart-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      span {
        font-size: 16px;
        font-weight: 500;
        color: #1f2937;
      }
    }
    
    .chart-container {
      height: 300px;
    }
  }
}

.services-card,
.alerts-card,
.performance-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    span {
      font-size: 16px;
      font-weight: 500;
      color: #1f2937;
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
      align-items: center;
    }
  }
}

.response-good {
  color: #10b981;
}

.response-warning {
  color: #f59e0b;
}

.response-bad {
  color: #ef4444;
}

.alert-detail {
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 6px;
  
  h4 {
    margin: 0 0 12px 0;
    color: #303133;
  }
  
  p {
    margin: 8px 0;
    color: #606266;
  }
  
  ul {
    margin: 8px 0;
    padding-left: 20px;
    
    li {
      color: #606266;
      margin: 4px 0;
    }
  }
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.performance-metric {
  margin-bottom: 20px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
  
  .metric-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    
    .metric-name {
      font-size: 14px;
      color: #606266;
      font-weight: 500;
    }
    
    .metric-value {
      font-size: 18px;
      font-weight: 600;
      
      &.metric-good {
        color: #10b981;
      }
      
      &.metric-warning {
        color: #f59e0b;
      }
      
      &.metric-bad {
        color: #ef4444;
      }
    }
  }
  
  .metric-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 8px;
    
    .metric-description {
      font-size: 12px;
      color: #909399;
    }
    
    .metric-threshold {
      font-size: 12px;
      color: #c0c4cc;
    }
  }
}

@media (max-width: 768px) {
  .monitor-container {
    padding: 16px;
  }
  
  .card-header {
    flex-direction: column;
    gap: 12px;
    
    .header-actions {
      width: 100%;
      justify-content: center;
    }
  }
  
  .status-content,
  .metric-content {
    flex-direction: column;
    text-align: center;
    
    .status-icon,
    .metric-icon {
      margin-right: 0;
      margin-bottom: 12px;
    }
  }
  
  .charts-row {
    .chart-card {
      .chart-container {
        height: 250px;
      }
    }
  }
}
</style>
