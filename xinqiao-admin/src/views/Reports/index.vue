<template>
  <div class="reports-container">
    <!-- 报表类型选择 -->
    <el-card class="report-types-card">
      <el-radio-group v-model="currentReportType" size="large">
        <el-radio-button label="user">用户数据报表</el-radio-button>
        <el-radio-button label="consultation">咨询数据报表</el-radio-button>
        <el-radio-button label="assessment">测评数据报表</el-radio-button>
        <el-radio-button label="revenue">收入数据报表</el-radio-button>
        <el-radio-button label="system">系统运营报表</el-radio-button>
      </el-radio-group>
    </el-card>

    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
          />
        </el-form-item>
        
        <el-form-item label="数据维度" v-if="currentReportType === 'user'">
          <el-select v-model="filterForm.dimension" placeholder="请选择维度">
            <el-option label="按日" value="day" />
            <el-option label="按周" value="week" />
            <el-option label="按月" value="month" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="咨询师" v-if="currentReportType === 'consultation'">
          <el-select v-model="filterForm.counselorId" placeholder="请选择咨询师" clearable>
            <el-option label="全部咨询师" value="" />
            <el-option label="张医生" value="1" />
            <el-option label="李医生" value="2" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="测评类型" v-if="currentReportType === 'assessment'">
          <el-select v-model="filterForm.assessmentType" placeholder="请选择类型" clearable>
            <el-option label="全部类型" value="" />
            <el-option label="心理健康" value="mental_health" />
            <el-option label="人格测试" value="personality" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleGenerateReport">
            <el-icon><DataAnalysis /></el-icon>
            生成报表
          </el-button>
          <el-button @click="handleExportReport">
            <el-icon><Download /></el-icon>
            导出报表
          </el-button>
          <el-button @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据概览 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" :lg="6" v-for="stat in currentStats" :key="stat.key">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" :style="{ backgroundColor: stat.color }">
              <el-icon size="24">
                <component :is="stat.icon" />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
              <div class="stat-trend" :class="stat.trend > 0 ? 'up' : 'down'">
                <el-icon><Top v-if="stat.trend > 0" /><Bottom v-else /></el-icon>
                <span>{{ Math.abs(stat.trend) }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>{{ getChartTitle('primary') }}</span>
              <el-radio-group v-model="chartPeriod" size="small">
                <el-radio-button label="day">日</el-radio-button>
                <el-radio-button label="week">周</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="primaryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>{{ getChartTitle('secondary') }}</span>
              <el-button type="primary" link @click="exportChart('secondary')">
                <el-icon><Download /></el-icon>
                导出
              </el-button>
            </div>
          </template>
          <div ref="secondaryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细数据表格 -->
    <el-card class="table-card">
      <template #header>
        <div class="table-header">
          <span>详细数据</span>
          <div class="table-actions">
            <el-input
              v-model="tableSearch"
              placeholder="搜索数据..."
              style="width: 200px"
              clearable
              @input="handleTableSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button @click="handleTableExport">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
          </div>
        </div>
      </template>
      
      <el-table
        v-loading="tableLoading"
        :data="tableData"
        stripe
        style="width: 100%"
        :default-sort="{ prop: 'date', order: 'descending' }"
      >
        <el-table-column
          v-for="column in tableColumns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          :sortable="column.sortable"
          :formatter="column.formatter"
        />
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="tablePagination.page"
          v-model:page-size="tablePagination.size"
          :total="tablePagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleTableSizeChange"
          @current-change="handleTableCurrentChange"
        />
      </div>
    </el-card>

    <!-- 趋势分析 -->
    <el-card class="trend-card">
      <template #header>
        <div class="card-header">
          <span>趋势分析</span>
          <el-radio-group v-model="trendPeriod" size="small">
            <el-radio-button label="7d">近7天</el-radio-button>
            <el-radio-button label="30d">近30天</el-radio-button>
            <el-radio-button label="90d">近90天</el-radio-button>
            <el-radio-button label="1y">近1年</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendChartRef" class="chart-container"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  DataAnalysis, Download, Refresh, Search, Top, Bottom,
  User, TrendCharts, PieChart, Histogram
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// 报表类型
const currentReportType = ref('user')

// 筛选表单
const filterForm = reactive({
  dimension: 'day',
  counselorId: '',
  assessmentType: ''
})

const dateRange = ref([])
const chartPeriod = ref('day')
const trendPeriod = ref('30d')

// 日期快捷选项
const dateShortcuts = [
  {
    text: '今天',
    value: () => {
      const end = new Date()
      const start = new Date()
      return [start, end]
    }
  },
  {
    text: '昨天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24)
      return [start, end]
    }
  },
  {
    text: '最近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  },
  {
    text: '本月',
    value: () => {
      const end = new Date()
      const start = new Date(end.getFullYear(), end.getMonth(), 1)
      return [start, end]
    }
  },
  {
    text: '上月',
    value: () => {
      const end = new Date()
      const start = new Date(end.getFullYear(), end.getMonth() - 1, 1)
      end.setDate(0)
      return [start, end]
    }
  }
]

// 统计数据
const stats = reactive({
  user: [
    { key: 'totalUsers', label: '总用户数', value: '12,847', trend: 12.5, icon: 'User', color: '#667eea' },
    { key: 'newUsers', label: '新增用户', value: '1,234', trend: 8.2, icon: 'User', color: '#10b981' },
    { key: 'activeUsers', label: '活跃用户', value: '3,456', trend: -2.1, icon: 'User', color: '#f59e0b' },
    { key: 'retentionRate', label: '留存率', value: '68.5%', trend: 5.3, icon: 'TrendCharts', color: '#ef4444' }
  ],
  consultation: [
    { key: 'totalConsultations', label: '总咨询数', value: '8,932', trend: 15.3, icon: 'PieChart', color: '#667eea' },
    { key: 'completedConsultations', label: '已完成', value: '7,654', trend: 18.7, icon: 'PieChart', color: '#10b981' },
    { key: 'avgDuration', label: '平均时长', value: '45分钟', trend: -3.2, icon: 'Histogram', color: '#f59e0b' },
    { key: 'satisfactionRate', label: '满意度', value: '4.6/5', trend: 2.1, icon: 'TrendCharts', color: '#ef4444' }
  ],
  assessment: [
    { key: 'totalAssessments', label: '测评总数', value: '156', trend: 22.1, icon: 'Histogram', color: '#667eea' },
    { key: 'completedAssessments', label: '完成数', value: '2,341', trend: 31.5, icon: 'Histogram', color: '#10b981' },
    { key: 'completionRate', label: '完成率', value: '78.3%', trend: 8.7, icon: 'TrendCharts', color: '#f59e0b' },
    { key: 'avgScore', label: '平均分', value: '7.2/10', trend: 1.5, icon: 'TrendCharts', color: '#ef4444' }
  ],
  revenue: [
    { key: 'totalRevenue', label: '总收入', value: '¥456,789', trend: 22.1, icon: 'TrendCharts', color: '#667eea' },
    { key: 'consultationRevenue', label: '咨询收入', value: '¥234,567', trend: 18.9, icon: 'PieChart', color: '#10b981' },
    { key: 'assessmentRevenue', label: '测评收入', value: '¥89,234', trend: 45.2, icon: 'Histogram', color: '#f59e0b' },
    { key: 'avgOrderValue', label: '客单价', value: '¥156', trend: 8.3, icon: 'TrendCharts', color: '#ef4444' }
  ],
  system: [
    { key: 'systemUptime', label: '系统可用性', value: '99.9%', trend: 0.1, icon: 'TrendCharts', color: '#667eea' },
    { key: 'responseTime', label: '响应时间', value: '245ms', trend: -12.5, icon: 'Histogram', color: '#10b981' },
    { key: 'errorRate', label: '错误率', value: '0.02%', trend: -25.3, icon: 'PieChart', color: '#f59e0b' },
    { key: 'concurrentUsers', label: '并发用户', value: '1,234', trend: 15.8, icon: 'User', color: '#ef4444' }
  ]
})

const currentStats = computed(() => stats[currentReportType.value])

// 图表引用
const primaryChartRef = ref<HTMLElement>()
const secondaryChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()

let primaryChart: echarts.ECharts | null = null
let secondaryChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

// 表格数据
const tableLoading = ref(false)
const tableSearch = ref('')
const tableData = ref([])
const tablePagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 表格列配置
const tableColumns = ref([
  { prop: 'date', label: '日期', width: 120, sortable: true },
  { prop: 'value', label: '数值', width: 100, sortable: true },
  { prop: 'change', label: '变化', width: 100, sortable: true, formatter: (row: any) => `${row.change > 0 ? '+' : ''}${row.change}%` },
  { prop: 'description', label: '描述', sortable: false }
])

// 获取图表标题
const getChartTitle = (type: string) => {
  const titles = {
    user: {
      primary: '用户增长趋势',
      secondary: '用户活跃度分析'
    },
    consultation: {
      primary: '咨询量趋势',
      secondary: '咨询师工作量分布'
    },
    assessment: {
      primary: '测评完成趋势',
      secondary: '测评类型分布'
    },
    revenue: {
      primary: '收入趋势',
      secondary: '收入构成分析'
    },
    system: {
      primary: '系统性能趋势',
      secondary: '错误类型分布'
    }
  }
  return titles[currentReportType.value][type]
}

// 生成报表
const handleGenerateReport = () => {
  ElMessage.success('正在生成报表...')
  generateMockData()
  updateCharts()
}

// 导出报表
const handleExportReport = () => {
  ElMessage.success('正在导出报表...')
}

// 刷新
const handleRefresh = () => {
  generateMockData()
  updateCharts()
  ElMessage.success('数据已刷新')
}

// 生成模拟数据
const generateMockData = () => {
  // 生成表格数据
  const mockTableData = []
  for (let i = 0; i < 50; i++) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    mockTableData.push({
      date: date.toISOString().split('T')[0],
      value: Math.floor(Math.random() * 1000) + 100,
      change: (Math.random() * 20 - 10).toFixed(1),
      description: '数据描述信息'
    })
  }
  tableData.value = mockTableData
  tablePagination.total = mockTableData.length
}

// 表格搜索
const handleTableSearch = () => {
  // 模拟搜索过滤
  if (tableSearch.value) {
    tableData.value = tableData.value.filter(item => 
      item.description.includes(tableSearch.value) || 
      item.date.includes(tableSearch.value)
    )
  } else {
    generateMockData()
  }
}

// 表格导出
const handleTableExport = () => {
  ElMessage.success('正在导出表格数据...')
}

// 表格分页
const handleTableSizeChange = (size: number) => {
  tablePagination.size = size
}

const handleTableCurrentChange = (page: number) => {
  tablePagination.page = page
}

// 导出图表
const exportChart = (chartType: string) => {
  ElMessage.success(`正在导出${chartType}图表...`)
}

// 初始化主图表
const initPrimaryChart = () => {
  if (!primaryChartRef.value) return
  
  primaryChart = echarts.init(primaryChartRef.value)
  
  const option = {
    title: {
      text: getChartTitle('primary'),
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月']
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      data: [120, 132, 101, 134, 90, 230, 210],
      type: 'line',
      smooth: true,
      areaStyle: {
        opacity: 0.3
      }
    }]
  }
  
  primaryChart.setOption(option)
}

// 初始化副图表
const initSecondaryChart = () => {
  if (!secondaryChartRef.value) return
  
  secondaryChart = echarts.init(secondaryChartRef.value)
  
  const option = {
    title: {
      text: getChartTitle('secondary'),
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [{
      name: '数据分布',
      type: 'pie',
      radius: '50%',
      data: [
        { value: 335, name: '类别A' },
        { value: 310, name: '类别B' },
        { value: 234, name: '类别C' },
        { value: 135, name: '类别D' },
        { value: 148, name: '类别E' }
      ],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
  
  secondaryChart.setOption(option)
}

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) return
  
  trendChart = echarts.init(trendChartRef.value)
  
  const option = {
    title: {
      text: '趋势分析',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['数据A', '数据B', '数据C'],
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
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '数据A',
        type: 'line',
        stack: '总量',
        data: [120, 132, 101, 134, 90, 230, 210]
      },
      {
        name: '数据B',
        type: 'line',
        stack: '总量',
        data: [220, 182, 191, 234, 290, 330, 310]
      },
      {
        name: '数据C',
        type: 'line',
        stack: '总量',
        data: [150, 232, 201, 154, 190, 330, 410]
      }
    ]
  }
  
  trendChart.setOption(option)
}

// 更新图表
const updateCharts = () => {
  if (primaryChart) {
    primaryChart.resize()
  }
  if (secondaryChart) {
    secondaryChart.resize()
  }
  if (trendChart) {
    trendChart.resize()
  }
}

// 监听报表类型变化
watch(currentReportType, () => {
  updateCharts()
})

// 监听图表周期变化
watch(chartPeriod, () => {
  updateCharts()
})

// 监听趋势周期变化
watch(trendPeriod, () => {
  updateCharts()
})

onMounted(() => {
  generateMockData()
  initPrimaryChart()
  initSecondaryChart()
  initTrendChart()
  
  window.addEventListener('resize', updateCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateCharts)
  if (primaryChart) { primaryChart.dispose(); primaryChart = null }
  if (secondaryChart) { secondaryChart.dispose(); secondaryChart = null }
  if (trendChart) { trendChart.dispose(); trendChart = null }
})
</script>

<style scoped lang="scss">
.reports-container {
  padding: 20px;
}

.report-types-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
  text-align: center;
  
  :deep(.el-radio-group) {
    display: inline-flex;
  }
}

.filter-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 20px;
  
  .stat-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    transition: transform 0.3s, box-shadow 0.3s;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
    }
    
    .stat-content {
      display: flex;
      align-items: center;
      
      .stat-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16px;
        color: white;
      }
      
      .stat-info {
        flex: 1;
        
        .stat-number {
          font-size: 24px;
          font-weight: 600;
          color: #1f2937;
          margin-bottom: 4px;
        }
        
        .stat-label {
          font-size: 14px;
          color: #6b7280;
          margin-bottom: 4px;
        }
        
        .stat-trend {
          display: flex;
          align-items: center;
          font-size: 12px;
          
          &.up {
            color: #10b981;
          }
          
          &.down {
            color: #ef4444;
          }
          
          span {
            margin-left: 4px;
          }
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

.table-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
  
  .table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    span {
      font-size: 16px;
      font-weight: 500;
      color: #1f2937;
    }
    
    .table-actions {
      display: flex;
      gap: 12px;
      align-items: center;
    }
  }
  
  .pagination-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid #e4e7ed;
  }
}

.trend-card {
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
    height: 400px;
  }
}

@media (max-width: 768px) {
  .reports-container {
    padding: 16px;
  }
  
  .filter-form {
    .el-form-item {
      width: 100%;
      margin-bottom: 12px;
    }
  }
  
  .stats-row {
    .stat-card {
      .stat-content {
        flex-direction: column;
        text-align: center;
        
        .stat-icon {
          margin-right: 0;
          margin-bottom: 12px;
        }
      }
    }
  }
  
  .table-header {
    flex-direction: column;
    gap: 12px;
    
    .table-actions {
      width: 100%;
      justify-content: space-between;
    }
  }
  
  .charts-row {
    .chart-card {
      .chart-container {
        height: 250px;
      }
    }
  }
  
  .trend-card {
    .chart-container {
      height: 300px;
    }
  }
}
</style>
