<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" :lg="6">
        <div class="stat-card">
          <div class="stat-icon users">
            <el-icon size="32"><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.totalUsers }}</div>
            <div class="stat-label">总用户数</div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon>
              <span>+12.5%</span>
            </div>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="12" :sm="6" :lg="6">
        <div class="stat-card">
          <div class="stat-icon counselors">
            <el-icon size="32"><Avatar /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.totalCounselors }}</div>
            <div class="stat-label">认证咨询师</div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon>
              <span>+8.2%</span>
            </div>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="12" :sm="6" :lg="6">
        <div class="stat-card">
          <div class="stat-icon consultations">
            <el-icon size="32"><ChatDotRound /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.totalConsultations }}</div>
            <div class="stat-label">咨询次数</div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon>
              <span>+15.3%</span>
            </div>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="12" :sm="6" :lg="6">
        <div class="stat-card">
          <div class="stat-icon revenue">
            <el-icon size="32"><Money /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">¥{{ stats.totalRevenue }}</div>
            <div class="stat-label">总收入</div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon>
              <span>+22.1%</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>用户增长趋势</span>
              <el-radio-group v-model="userGrowthPeriod" size="small">
                <el-radio-button label="week">周</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
                <el-radio-button label="year">年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="userGrowthChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>咨询类型分布</span>
              <el-button type="primary" link @click="exportChart('consultation-type')">
                <el-icon><Download /></el-icon>
                导出
              </el-button>
            </div>
          </template>
          <div ref="consultationTypeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :sm="24" :lg="24">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>收入趋势分析</span>
              <el-radio-group v-model="revenuePeriod" size="small">
                <el-radio-button label="month">月度</el-radio-button>
                <el-radio-button label="quarter">季度</el-radio-button>
                <el-radio-button label="year">年度</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="revenueChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作 -->
    <el-row :gutter="20" class="quick-actions-row">
      <el-col :xs="24" :sm="24" :lg="24">
        <el-card class="quick-actions-card">
          <template #header>
            <span>快速操作</span>
          </template>
          <div class="quick-actions">
            <el-button v-if="can('user:read')" type="primary" @click="$router.push('/users')">
              <el-icon><User /></el-icon>
              用户管理
            </el-button>
            <el-button v-if="can('counselor:read')" type="success" @click="$router.push('/counselors')">
              <el-icon><Avatar /></el-icon>
              咨询师审核
            </el-button>
            <el-button v-if="can('content:read')" type="warning" @click="$router.push('/content')">
              <el-icon><Document /></el-icon>
              内容审核
            </el-button>
            <el-button v-if="can('report:read')" type="info" @click="$router.push('/reports')">
              <el-icon><TrendCharts /></el-icon>
              数据报表
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { 
  User, Avatar, ChatDotRound, Money, Top, Download,
  TrendCharts, Document 
} from '@element-plus/icons-vue'
import { 
  useUserGrowthChart, 
  useConsultationTypeChart, 
  useRevenueChart 
} from '@/composables/useCharts'

const authStore = useAuthStore()
const can = (p: string) => authStore.hasPermissions([p])

// 统计数据
const stats = ref({
  totalUsers: 12847,
  totalCounselors: 342,
  totalConsultations: 8932,
  totalRevenue: 456789
})

// 图表周期选择
const userGrowthPeriod = ref('month')
const revenuePeriod = ref('month')

// 初始化图表
const { 
  chartRef: userGrowthChartRef, 
  initChart: initUserGrowthChart, 
  destroyChart: destroyUserGrowthChart 
} = useUserGrowthChart()

const { 
  chartRef: consultationTypeChartRef, 
  initChart: initConsultationTypeChart, 
  destroyChart: destroyConsultationTypeChart 
} = useConsultationTypeChart()

const { 
  chartRef: revenueChartRef, 
  initChart: initRevenueChart, 
  destroyChart: destroyRevenueChart 
} = useRevenueChart()

// 导出图表
const exportChart = (chartType: string) => {
  ElMessage.success('图表导出功能开发中...')
}

onMounted(() => {
  initUserGrowthChart()
  initConsultationTypeChart()
  initRevenueChart()
})

onUnmounted(() => {
  destroyUserGrowthChart()
  destroyConsultationTypeChart()
  destroyRevenueChart()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
  
  .stat-card {
    background: white;
    border-radius: 12px;
    padding: 24px;
    display: flex;
    align-items: center;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    transition: transform 0.3s, box-shadow 0.3s;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
    }
    
    .stat-icon {
      width: 64px;
      height: 64px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;
      
      &.users {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
      }
      
      &.counselors {
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        color: white;
      }
      
      &.consultations {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        color: white;
      }
      
      &.revenue {
        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
        color: white;
      }
    }
    
    .stat-content {
      flex: 1;
      
      .stat-number {
        font-size: 28px;
        font-weight: 600;
        color: #1f2937;
        margin-bottom: 4px;
      }
      
      .stat-label {
        font-size: 14px;
        color: #6b7280;
        margin-bottom: 8px;
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

.quick-actions-row {
  .quick-actions-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    
    .quick-actions {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      
      .el-button {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }
  }
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 16px;
  }
  
  .stats-row {
    .stat-card {
      padding: 16px;
      
      .stat-icon {
        width: 48px;
        height: 48px;
        
        .el-icon {
          font-size: 24px;
        }
      }
      
      .stat-content {
        .stat-number {
          font-size: 20px;
        }
      }
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
