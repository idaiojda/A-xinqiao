<template>
  <div class="files-container">
    <div class="page-header">
      <h1>档案管理</h1>
      <p>管理系统中的各类档案文件</p>
    </div>

    <!-- 搜索和操作栏 -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件名、上传者..."
          style="width: 300px"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="fileType" placeholder="文件类型" clearable @change="handleSearch">
          <el-option label="图片" value="image" />
          <el-option label="文档" value="document" />
          <el-option label="视频" value="video" />
          <el-option label="音频" value="audio" />
          <el-option label="其他" value="other" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="handleSearch"
        />
      </div>
      <div class="filter-right">
        <el-button type="primary" @click="handleUpload">
          <el-icon><Upload /></el-icon>
          上传文件
        </el-button>
        <el-button @click="handleBatchDelete" :disabled="selectedFiles.length === 0">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>
    </div>

    <!-- 文件列表 -->
    <div class="files-grid">
      <el-card
        v-for="file in filesList"
        :key="file.id"
        class="file-card"
        :class="{ selected: selectedFiles.includes(file.id) }"
        @click="toggleFileSelection(file)"
      >
        <div class="file-preview">
          <img v-if="file.type === 'image'" :src="file.url" :alt="file.name" />
          <div v-else-if="file.type === 'document'" class="file-icon document">
            <el-icon><Document /></el-icon>
          </div>
          <div v-else-if="file.type === 'video'" class="file-icon video">
            <el-icon><VideoPlay /></el-icon>
          </div>
          <div v-else-if="file.type === 'audio'" class="file-icon audio">
            <el-icon><Headset /></el-icon>
          </div>
          <div v-else class="file-icon other">
            <el-icon><Document /></el-icon>
          </div>
        </div>
        <div class="file-info">
          <h4>{{ file.name }}</h4>
          <p class="file-size">{{ formatFileSize(file.size) }}</p>
          <p class="file-uploader">上传者: {{ file.uploader }}</p>
          <p class="file-date">{{ formatDate(file.uploadDate) }}</p>
        </div>
        <div class="file-actions">
          <el-button type="primary" text @click.stop="handlePreview(file)">
            <el-icon><View /></el-icon>
          </el-button>
          <el-button type="primary" text @click.stop="handleDownload(file)">
            <el-icon><Download /></el-icon>
          </el-button>
          <el-button type="danger" text @click.stop="handleDelete(file)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[12, 24, 48, 96]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalFiles"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 文件预览对话框 -->
    <el-dialog
      v-model="previewDialog.visible"
      :title="previewDialog.file?.name"
      width="80%"
      top="5vh"
    >
      <div class="file-preview-dialog">
        <img
          v-if="previewDialog.file?.type === 'image'"
          :src="previewDialog.file?.url"
          :alt="previewDialog.file?.name"
          class="preview-image"
        />
        <div v-else-if="previewDialog.file?.type === 'video'" class="preview-video">
          <video :src="previewDialog.file?.url" controls></video>
        </div>
        <div v-else-if="previewDialog.file?.type === 'audio'" class="preview-audio">
          <audio :src="previewDialog.file?.url" controls></audio>
        </div>
        <div v-else class="preview-other">
          <el-icon size="64"><Document /></el-icon>
          <p>该文件类型暂不支持预览</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewDialog.visible = false">关闭</el-button>
        <el-button type="primary" @click="handleDownload(previewDialog.file!)">
          <el-icon><Download /></el-icon>
          下载
        </el-button>
      </template>
    </el-dialog>

    <!-- 文件上传对话框 -->
    <el-dialog
      v-model="uploadDialog.visible"
      title="上传文件"
      width="600px"
    >
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        multiple
        :action="uploadUrl"
        :headers="uploadHeaders"
        :before-upload="beforeUpload"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :file-list="uploadDialog.fileList"
      >
        <el-icon size="64"><Upload /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持多种文件格式，单个文件不超过 100MB
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="uploadDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmUpload">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile, UploadFiles } from 'element-plus'
import {
  Search,
  Upload,
  Delete,
  View,
  Download,
  Document,
  VideoPlay,
  Headset
} from '@element-plus/icons-vue'

interface FileItem {
  id: string
  name: string
  type: 'image' | 'document' | 'video' | 'audio' | 'other'
  size: number
  url: string
  uploader: string
  uploadDate: string
  description?: string
}

const PLACEHOLDER_IMAGE = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="300" height="200"><rect width="100%" height="100%" fill="%23e5e7eb"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%236b7280" font-size="16">No Image</text></svg>'

// 搜索和筛选条件
const searchKeyword = ref('')
const fileType = ref('')
const dateRange = ref('')

// 文件列表数据
const filesList = ref<FileItem[]>([])
const selectedFiles = ref<string[]>([])

// 分页数据
const currentPage = ref(1)
const pageSize = ref(12)
const totalFiles = ref(0)

// 对话框状态
const previewDialog = reactive({
  visible: false,
  file: null as FileItem | null
})

const uploadDialog = reactive({
  visible: false,
  fileList: [] as UploadFiles
})

const uploadRef = ref()
const uploadUrl = '/api/files/upload'
const uploadHeaders = {
  Authorization: 'Bearer ' + localStorage.getItem('token')
}

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 格式化日期
const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleString('zh-CN')
}

// 获取文件列表
const fetchFiles = async () => {
  try {
    // 模拟API调用
    const mockFiles: FileItem[] = [
      {
        id: '1',
        name: '用户头像1.jpg',
        type: 'image',
        size: 102400,
        url: PLACEHOLDER_IMAGE,
        uploader: '张三',
        uploadDate: '2024-01-15T10:30:00Z'
      },
      {
        id: '2',
        name: '心理测评报告.pdf',
        type: 'document',
        size: 2048000,
        url: '/files/report.pdf',
        uploader: '李四',
        uploadDate: '2024-01-14T15:20:00Z'
      },
      {
        id: '3',
        name: '咨询录像.mp4',
        type: 'video',
        size: 52428800,
        url: '/files/consultation.mp4',
        uploader: '王五',
        uploadDate: '2024-01-13T09:15:00Z'
      },
      {
        id: '4',
        name: '背景音乐.mp3',
        type: 'audio',
        size: 4194304,
        url: '/files/music.mp3',
        uploader: '赵六',
        uploadDate: '2024-01-12T14:45:00Z'
      },
      {
        id: '5',
        name: '系统配置.json',
        type: 'other',
        size: 8192,
        url: '/files/config.json',
        uploader: '系统管理员',
        uploadDate: '2024-01-11T16:30:00Z'
      },
      {
        id: '6',
        name: '用户头像2.jpg',
        type: 'image',
        size: 98304,
        url: PLACEHOLDER_IMAGE,
        uploader: '钱七',
        uploadDate: '2024-01-10T11:20:00Z'
      }
    ]
    
    // 模拟搜索和筛选
    let filteredFiles = mockFiles
    
    if (searchKeyword.value) {
      filteredFiles = filteredFiles.filter(file => 
        file.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
        file.uploader.toLowerCase().includes(searchKeyword.value.toLowerCase())
      )
    }
    
    if (fileType.value) {
      filteredFiles = filteredFiles.filter(file => file.type === fileType.value)
    }
    
    if (dateRange.value) {
      // 简化的日期筛选逻辑
      const startDate = new Date(dateRange.value[0])
      const endDate = new Date(dateRange.value[1])
      filteredFiles = filteredFiles.filter(file => {
        const fileDate = new Date(file.uploadDate)
        return fileDate >= startDate && fileDate <= endDate
      })
    }
    
    totalFiles.value = filteredFiles.length
    filesList.value = filteredFiles.slice(
      (currentPage.value - 1) * pageSize.value,
      currentPage.value * pageSize.value
    )
  } catch (error) {
    ElMessage.error('获取文件列表失败')
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchFiles()
}

// 切换文件选择
const toggleFileSelection = (file: FileItem) => {
  const index = selectedFiles.value.indexOf(file.id)
  if (index > -1) {
    selectedFiles.value.splice(index, 1)
  } else {
    selectedFiles.value.push(file.id)
  }
}

// 文件预览
const handlePreview = (file: FileItem) => {
  previewDialog.file = file
  previewDialog.visible = true
}

// 文件下载
const handleDownload = (file: FileItem) => {
  // 模拟文件下载
  const link = document.createElement('a')
  link.href = file.url
  link.download = file.name
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  ElMessage.success(`正在下载: ${file.name}`)
}

// 删除文件
const handleDelete = async (file: FileItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 "${file.name}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 模拟删除操作
    const index = filesList.value.findIndex(f => f.id === file.id)
    if (index > -1) {
      filesList.value.splice(index, 1)
      totalFiles.value--
    }
    
    ElMessage.success('文件删除成功')
  } catch (error) {
    // 用户取消删除
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedFiles.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedFiles.value.length} 个文件吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 模拟批量删除
    filesList.value = filesList.value.filter(file => 
      !selectedFiles.value.includes(file.id)
    )
    totalFiles.value -= selectedFiles.value.length
    selectedFiles.value = []
    
    ElMessage.success('批量删除成功')
  } catch (error) {
    // 用户取消删除
  }
}

// 上传文件
const handleUpload = () => {
  uploadDialog.visible = true
  uploadDialog.fileList = []
}

// 上传前检查
const beforeUpload = (file: File) => {
  const maxSize = 100 * 1024 * 1024 // 100MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }
  return true
}

// 上传成功
const handleUploadSuccess = (response: any, file: UploadFile, fileList: UploadFiles) => {
  ElMessage.success(`${file.name} 上传成功`)
}

// 上传失败
const handleUploadError = (error: any, file: UploadFile) => {
  ElMessage.error(`${file.name} 上传失败`)
}

// 确认上传
const confirmUpload = () => {
  uploadRef.value?.submit()
  uploadDialog.visible = false
  fetchFiles()
}

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  fetchFiles()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  fetchFiles()
}

// 初始化
onMounted(() => {
  fetchFiles()
})
</script>

<style scoped lang="scss">
.files-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.page-header {
  margin-bottom: 20px;
  
  h1 {
    font-size: 24px;
    color: #303133;
    margin: 0 0 8px 0;
  }
  
  p {
    color: #909399;
    margin: 0;
  }
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  
  .filter-left {
    display: flex;
    gap: 16px;
    align-items: center;
  }
  
  .filter-right {
    display: flex;
    gap: 12px;
  }
}

.files-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.file-card {
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
  
  &.selected {
    border-color: #409eff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
  }
  
  .file-preview {
    height: 160px;
    background: #f5f7fa;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .file-icon {
      font-size: 48px;
      color: #909399;
      
      &.document {
        color: #409eff;
      }
      
      &.video {
        color: #67c23a;
      }
      
      &.audio {
        color: #e6a23c;
      }
    }
  }
  
  .file-info {
    padding: 16px;
    
    h4 {
      margin: 0 0 8px 0;
      font-size: 16px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    p {
      margin: 4px 0;
      font-size: 12px;
      color: #909399;
    }
  }
  
  .file-actions {
    position: absolute;
    top: 8px;
    right: 8px;
    display: flex;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.3s ease;
  }
  
  &:hover .file-actions {
    opacity: 1;
  }
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.file-preview-dialog {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  
  .preview-image {
    max-width: 100%;
    max-height: 600px;
    object-fit: contain;
  }
  
  .preview-video {
    width: 100%;
    
    video {
      width: 100%;
      max-height: 600px;
    }
  }
  
  .preview-audio {
    width: 100%;
    
    audio {
      width: 100%;
    }
  }
  
  .preview-other {
    text-align: center;
    color: #909399;
    
    p {
      margin-top: 16px;
    }
  }
}

.upload-area {
  width: 100%;
  
  :deep(.el-upload-dragger) {
    width: 100%;
    height: 200px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .files-container {
    padding: 12px;
  }
  
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    
    .filter-left {
      flex-direction: column;
      align-items: stretch;
      gap: 8px;
    }
    
    .filter-right {
      justify-content: flex-end;
    }
  }
  
  .files-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .files-grid {
    grid-template-columns: 1fr;
  }
}
</style>
