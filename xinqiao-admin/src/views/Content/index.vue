<template>
  <div class="content-container">
    <!-- 搜索和操作区域 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="内容标题">
          <el-input
            v-model="searchForm.title"
            placeholder="请输入内容标题"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        
        <el-form-item label="内容类型">
          <el-select v-model="searchForm.type" placeholder="请选择内容类型" clearable>
            <el-option label="文章" value="article" />
            <el-option label="测评" value="assessment" />
            <el-option label="视频" value="video" />
            <el-option label="动态" value="post" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.status" placeholder="请选择审核状态" clearable>
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="草稿" value="draft" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="发布者">
          <el-input
            v-model="searchForm.author"
            placeholder="请输入发布者"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="提交时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作按钮区域 -->
    <el-card class="actions-card">
      <div class="actions-left">
        <el-button type="primary" @click="handleBatchApprove" :disabled="selectedIds.length === 0">
          <el-icon><Check /></el-icon>
          批量通过
        </el-button>
        <el-button type="danger" @click="handleBatchReject" :disabled="selectedIds.length === 0">
          <el-icon><Close /></el-icon>
          批量拒绝
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
      </div>
      
      <div class="actions-right">
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </el-card>

    <!-- 内容列表 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="contentList"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="内容预览" width="120">
          <template #default="{ row }">
            <div class="content-preview">
              <el-image
                v-if="row.coverImage"
                :src="row.coverImage"
                fit="cover"
                class="cover-image"
              />
              <div v-else class="cover-placeholder">
                <el-icon><Document /></el-icon>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="author" label="发布者" width="120" />
        <el-table-column label="内容类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getContentTypeType(row.type)">
              {{ getContentTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="100" />
        <el-table-column prop="likeCount" label="点赞数" width="100" />
        <el-table-column prop="createdAt" label="提交时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
            <el-button 
              v-if="row.status === 'pending'" 
              type="success" 
              link 
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button 
              v-if="row.status === 'pending'" 
              type="danger" 
              link 
              @click="handleReject(row)"
            >
              拒绝
            </el-button>
            <el-button 
              v-if="row.status === 'approved'" 
              type="warning" 
              link 
              @click="handleRevoke(row)"
            >
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 内容详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="内容详情"
      width="60%"
      top="5vh"
    >
      <div class="content-detail" v-if="currentContent">
        <div class="detail-header">
          <h3>{{ currentContent.title }}</h3>
          <div class="detail-meta">
            <span>发布者: {{ currentContent.author }}</span>
            <span>类型: {{ getContentTypeLabel(currentContent.type) }}</span>
            <span>状态: <el-tag :type="getStatusType(currentContent.status)">{{ getStatusLabel(currentContent.status) }}</el-tag></span>
            <span>提交时间: {{ currentContent.createdAt }}</span>
          </div>
        </div>
        
        <div class="detail-content">
          <div class="content-cover" v-if="currentContent.coverImage">
            <el-image :src="currentContent.coverImage" fit="contain" />
          </div>
          <div class="content-text" v-html="currentContent.content"></div>
        </div>
        
        <div class="detail-stats">
          <div class="stat-item">
            <span class="stat-label">浏览量:</span>
            <span class="stat-value">{{ currentContent.viewCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">点赞数:</span>
            <span class="stat-value">{{ currentContent.likeCount }}</span>
          </div>
          <div class="stat-item" v-if="currentContent.tags && currentContent.tags.length">
            <span class="stat-label">标签:</span>
            <span class="stat-value">
              <el-tag v-for="tag in currentContent.tags" :key="tag" size="small">{{ tag }}</el-tag>
            </span>
          </div>
        </div>
        
        <div class="detail-actions" v-if="currentContent.status === 'pending'">
          <el-button type="success" @click="handleApprove(currentContent)">
            <el-icon><Check /></el-icon>
            通过审核
          </el-button>
          <el-button type="danger" @click="handleReject(currentContent)">
            <el-icon><Close /></el-icon>
            拒绝审核
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Check, Close, Download, Document } from '@element-plus/icons-vue'
import type { ContentItem, ContentType, ContentStatus } from '@/types'

// 搜索表单
const searchForm = reactive({
  title: '',
  type: undefined as ContentType | undefined,
  status: undefined as ContentStatus | undefined,
  author: '',
  startDate: '',
  endDate: ''
})

const dateRange = ref([])

// 表格数据
const loading = ref(false)
const contentList = ref<ContentItem[]>([])
const selectedIds = ref<number[]>([])

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 详情对话框
const detailVisible = ref(false)
const currentContent = ref<ContentItem | null>(null)

// 获取内容列表
const fetchContentList = async () => {
  loading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 模拟数据
    const mockData: ContentItem[] = [
      {
        id: 1,
        title: '如何应对职场压力',
        content: '<p>职场压力是现代生活中常见的问题...</p>',
        author: '张三',
        type: 'article',
        status: 'pending',
        viewCount: 0,
        likeCount: 0,
        createdAt: '2024-01-15 10:00:00',
        updatedAt: '2024-01-15 10:00:00'
      },
      {
        id: 2,
        title: '心理健康自评量表',
        content: '<p>请根据您最近一周的感受回答以下问题...</p>',
        author: '李四',
        type: 'assessment',
        status: 'approved',
        viewCount: 156,
        likeCount: 23,
        createdAt: '2024-01-14 15:30:00',
        updatedAt: '2024-01-14 15:30:00'
      }
    ]
    
    contentList.value = mockData
    pagination.total = mockData.length
  } catch (error) {
    console.error('获取内容列表失败:', error)
    ElMessage.error('获取内容列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchContentList()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    title: '',
    type: undefined,
    status: undefined,
    author: '',
    startDate: '',
    endDate: ''
  })
  dateRange.value = []
  handleSearch()
}

// 刷新
const handleRefresh = () => {
  fetchContentList()
}

// 详情
const handleDetail = (content: ContentItem) => {
  currentContent.value = content
  detailVisible.value = true
}

// 通过审核
const handleApprove = async (content: ContentItem) => {
  await ElMessageBox.confirm(`确定要通过内容 "${content.title}" 的审核吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 300))
    content.status = 'approved'
    ElMessage.success('审核通过')
    fetchContentList()
    detailVisible.value = false
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error('审核失败')
  }
}

// 拒绝审核
const handleReject = async (content: ContentItem) => {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审核', {
    inputPlaceholder: '请输入拒绝原因',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).catch(() => ({ value: '' }))
  
  if (!value) return
  
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 300))
    content.status = 'rejected'
    ElMessage.success('审核已拒绝')
    fetchContentList()
    detailVisible.value = false
  } catch (error) {
    console.error('拒绝审核失败:', error)
    ElMessage.error('拒绝审核失败')
  }
}

// 撤销审核
const handleRevoke = async (content: ContentItem) => {
  await ElMessageBox.confirm(`确定要撤销内容 "${content.title}" 的审核吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 300))
    content.status = 'pending'
    ElMessage.success('审核已撤销')
    fetchContentList()
  } catch (error) {
    console.error('撤销审核失败:', error)
    ElMessage.error('撤销审核失败')
  }
}

// 批量通过
const handleBatchApprove = async () => {
  if (selectedIds.value.length === 0) return
  
  await ElMessageBox.confirm(`确定要通过选中的 ${selectedIds.value.length} 条内容吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    // 模拟批量API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('批量审核通过')
    fetchContentList()
  } catch (error) {
    console.error('批量审核失败:', error)
    ElMessage.error('批量审核失败')
  }
}

// 批量拒绝
const handleBatchReject = async () => {
  if (selectedIds.value.length === 0) return
  
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '批量拒绝审核', {
    inputPlaceholder: '请输入拒绝原因',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).catch(() => ({ value: '' }))
  
  if (!value) return
  
  try {
    // 模拟批量API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('批量审核已拒绝')
    fetchContentList()
  } catch (error) {
    console.error('批量拒绝审核失败:', error)
    ElMessage.error('批量拒绝审核失败')
  }
}

// 导出数据
const handleExport = () => {
  ElMessage.success('导出功能开发中...')
}

// 选择变化
const handleSelectionChange = (selection: ContentItem[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 分页变化
const handleSizeChange = (size: number) => {
  pagination.size = size
  fetchContentList()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  fetchContentList()
}

// 工具函数
const getContentTypeType = (type: ContentType) => {
  const types = {
    article: 'primary',
    assessment: 'success',
    video: 'warning',
    post: 'info'
  }
  return types[type] || 'info'
}

const getContentTypeLabel = (type: ContentType) => {
  const labels = {
    article: '文章',
    assessment: '测评',
    video: '视频',
    post: '动态'
  }
  return labels[type] || '未知'
}

const getStatusType = (status: ContentStatus) => {
  const types = {
    draft: 'info',
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return types[status] || 'info'
}

const getStatusLabel = (status: ContentStatus) => {
  const labels = {
    draft: '草稿',
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }
  return labels[status] || '未知'
}

onMounted(() => {
  fetchContentList()
})
</script>

<style scoped lang="scss">
.content-container {
  padding: 20px;
}

.search-card,
.actions-card,
.table-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
}

.search-form {
  .el-form-item {
    margin-bottom: 0;
  }
}

.actions-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .actions-left,
  .actions-right {
    display: flex;
    gap: 12px;
  }
}

.content-preview {
  .cover-image {
    width: 60px;
    height: 60px;
    border-radius: 6px;
  }
  
  .cover-placeholder {
    width: 60px;
    height: 60px;
    background-color: #f5f7fa;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #909399;
  }
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.content-detail {
  .detail-header {
    margin-bottom: 20px;
    
    h3 {
      margin: 0 0 10px 0;
      color: #303133;
    }
    
    .detail-meta {
      display: flex;
      gap: 20px;
      font-size: 14px;
      color: #606266;
    }
  }
  
  .detail-content {
    margin-bottom: 20px;
    
    .content-cover {
      text-align: center;
      margin-bottom: 15px;
      
      .el-image {
        max-width: 100%;
        max-height: 300px;
      }
    }
    
    .content-text {
      line-height: 1.6;
      color: #303133;
    }
  }
  
  .detail-stats {
    background-color: #f5f7fa;
    padding: 15px;
    border-radius: 6px;
    margin-bottom: 20px;
    
    .stat-item {
      display: flex;
      align-items: center;
      margin-bottom: 10px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .stat-label {
        font-weight: 500;
        margin-right: 10px;
        min-width: 60px;
      }
      
      .stat-value {
        flex: 1;
        
        .el-tag {
          margin-right: 8px;
          margin-bottom: 5px;
        }
      }
    }
  }
  
  .detail-actions {
    text-align: center;
    padding-top: 20px;
    border-top: 1px solid #e4e7ed;
  }
}

@media (max-width: 768px) {
  .content-container {
    padding: 16px;
  }
  
  .search-form {
    .el-form-item {
      width: 100%;
      margin-bottom: 12px;
    }
  }
  
  .actions-card {
    flex-direction: column;
    gap: 12px;
    
    .actions-left,
    .actions-right {
      width: 100%;
      justify-content: center;
    }
  }
  
  .detail-meta {
    flex-direction: column;
    gap: 8px !important;
  }
}
</style>