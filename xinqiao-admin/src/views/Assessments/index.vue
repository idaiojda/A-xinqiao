<template>
  <div class="assessments-container">
    <!-- 搜索和操作区域 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="测评标题">
          <el-input
            v-model="searchForm.title"
            placeholder="请输入测评标题"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        
        <el-form-item label="测评分类">
          <el-select v-model="searchForm.category" placeholder="请选择分类" clearable>
            <el-option label="心理健康" value="mental_health" />
            <el-option label="人格测试" value="personality" />
            <el-option label="职业倾向" value="career" />
            <el-option label="人际关系" value="relationship" />
            <el-option label="学习评估" value="learning" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已下架" value="archived" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="创建者">
          <el-input
            v-model="searchForm.creator"
            placeholder="请输入创建者"
            clearable
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
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建测评
        </el-button>
        <el-button type="success" @click="handleBatchPublish" :disabled="selectedIds.length === 0">
          <el-icon><Upload /></el-icon>
          批量发布
        </el-button>
        <el-button type="warning" @click="handleBatchArchive" :disabled="selectedIds.length === 0">
          <el-icon><Download /></el-icon>
          批量下架
        </el-button>
        <el-button @click="handleExport" :disabled="selectedIds.length === 0">
          <el-icon><Document /></el-icon>
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

    <!-- 测评列表 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="assessmentList"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="测评标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="getCategoryType(row.category)">
              {{ getCategoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题目数量" width="100" />
        <el-table-column prop="duration" label="测试时长" width="100">
          <template #default="{ row }">
            {{ row.duration }} 分钟
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completedCount" label="完成次数" width="100" />
        <el-table-column prop="creator" label="创建者" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handlePreview(row)">预览</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button 
              v-if="row.status === 'draft'" 
              type="success" 
              link 
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button 
              v-if="row.status === 'published'" 
              type="warning" 
              link 
              @click="handleArchive(row)"
            >
              下架
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <!-- 测评表单对话框 -->
    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新建测评' : '编辑测评'"
      width="60%"
      top="5vh"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="测评标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入测评标题" />
        </el-form-item>
        
        <el-form-item label="测评描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入测评描述"
          />
        </el-form-item>
        
        <el-form-item label="测评分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类">
            <el-option label="心理健康" value="mental_health" />
            <el-option label="人格测试" value="personality" />
            <el-option label="职业倾向" value="career" />
            <el-option label="人际关系" value="relationship" />
            <el-option label="学习评估" value="learning" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="测试时长" prop="duration">
          <el-input-number
            v-model="formData.duration"
            :min="1"
            :max="180"
            :step="5"
            controls-position="right"
          />
          <span style="margin-left: 8px">分钟</span>
        </el-form-item>
        
        <el-form-item label="题目设置">
          <div class="questions-section">
            <div class="questions-header">
              <span>题目列表 ({{ formData.questions.length }})</span>
              <el-button type="primary" size="small" @click="handleAddQuestion">
                <el-icon><Plus /></el-icon>
                添加题目
              </el-button>
            </div>
            
            <div class="questions-list">
              <div
                v-for="(question, index) in formData.questions"
                :key="index"
                class="question-item"
              >
                <div class="question-header">
                  <span class="question-number">第 {{ index + 1 }} 题</span>
                  <el-button
                    type="danger"
                    size="small"
                    link
                    @click="handleRemoveQuestion(index)"
                  >
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
                
                <el-form-item :label="'题目'" :prop="`questions.${index}.question`" required>
                  <el-input v-model="question.question" placeholder="请输入题目" />
                </el-form-item>
                
                <el-form-item label="题型" :prop="`questions.${index}.type`" required>
                  <el-radio-group v-model="question.type">
                    <el-radio label="single">单选题</el-radio>
                    <el-radio label="multiple">多选题</el-radio>
                    <el-radio label="text">文本题</el-radio>
                    <el-radio label="rating">评分题</el-radio>
                  </el-radio-group>
                </el-form-item>
                
                <el-form-item
                  v-if="question.type === 'single' || question.type === 'multiple'"
                  label="选项"
                  required
                >
                  <div class="options-list">
                    <div
                      v-for="(option, optIndex) in question.options"
                      :key="optIndex"
                      class="option-item"
                    >
                      <el-input
                        v-model="question.options[optIndex]"
                        :placeholder="`选项 ${String.fromCharCode(65 + optIndex)}`"
                        style="flex: 1"
                      />
                      <el-button
                        v-if="question.options.length > 2"
                        type="danger"
                        size="small"
                        link
                        @click="handleRemoveOption(index, optIndex)"
                      >
                        <el-icon><Minus /></el-icon>
                      </el-button>
                    </div>
                    <el-button
                      v-if="question.options.length < 6"
                      type="primary"
                      size="small"
                      link
                      @click="handleAddOption(index)"
                    >
                      <el-icon><Plus /></el-icon>
                      添加选项
                    </el-button>
                  </div>
                </el-form-item>
                
                <el-form-item label="是否必填">
                  <el-switch v-model="question.required" />
                </el-form-item>
              </div>
              
              <div v-if="formData.questions.length === 0" class="no-questions">
                <el-empty description="暂无题目，请添加题目" />
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 测评预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="测评预览"
      width="50%"
      top="5vh"
    >
      <div class="assessment-preview" v-if="currentAssessment">
        <div class="preview-header">
          <h3>{{ currentAssessment.title }}</h3>
          <p>{{ currentAssessment.description }}</p>
          <div class="preview-meta">
            <el-tag>{{ getCategoryLabel(currentAssessment.category) }}</el-tag>
            <span>题目数量: {{ currentAssessment.questions.length }}</span>
            <span>测试时长: {{ currentAssessment.duration }} 分钟</span>
          </div>
        </div>
        
        <div class="preview-questions">
          <div
            v-for="(question, index) in currentAssessment.questions"
            :key="index"
            class="preview-question"
          >
            <div class="question-title">
              <span class="question-number">{{ index + 1 }}.</span>
              <span>{{ question.question }}</span>
              <el-tag v-if="question.required" size="small" type="danger">必填</el-tag>
            </div>
            
            <div class="question-content">
              <div v-if="question.type === 'single'" class="single-choice">
                <el-radio-group>
                  <el-radio
                    v-for="(option, optIndex) in question.options"
                    :key="optIndex"
                    :label="optIndex"
                  >
                    {{ String.fromCharCode(65 + optIndex) }}. {{ option }}
                  </el-radio>
                </el-radio-group>
              </div>
              
              <div v-else-if="question.type === 'multiple'" class="multiple-choice">
                <el-checkbox-group>
                  <el-checkbox
                    v-for="(option, optIndex) in question.options"
                    :key="optIndex"
                    :label="optIndex"
                  >
                    {{ String.fromCharCode(65 + optIndex) }}. {{ option }}
                  </el-checkbox>
                </el-checkbox-group>
              </div>
              
              <div v-else-if="question.type === 'text'" class="text-input">
                <el-input
                  type="textarea"
                  :rows="3"
                  placeholder="请输入您的回答"
                  disabled
                />
              </div>
              
              <div v-else-if="question.type === 'rating'" class="rating-input">
                <el-rate disabled />
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download, Document } from '@element-plus/icons-vue'
import type { Assessment, Question } from '@/types'

// 搜索表单
const searchForm = reactive({
  title: '',
  category: '',
  status: '',
  creator: ''
})

// 表格数据
const loading = ref(false)
const assessmentList = ref<Assessment[]>([])
const selectedIds = ref<number[]>([])

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 表单
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formRef = ref()
const formData = reactive({
  id: 0,
  title: '',
  description: '',
  category: '',
  duration: 30,
  questions: [] as Question[],
  status: 'draft' as const,
  completedCount: 0,
  createdAt: '',
  updatedAt: '',
  creator: 'admin'
})

const formRules = {
  title: [{ required: true, message: '请输入测评标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入测评描述', trigger: 'blur' }],
  category: [{ required: true, message: '请选择测评分类', trigger: 'change' }],
  duration: [{ required: true, message: '请输入测试时长', trigger: 'blur' }]
}

// 预览
const previewVisible = ref(false)
const currentAssessment = ref<Assessment | null>(null)

// 获取测评列表
const fetchAssessmentList = async () => {
  loading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 模拟数据
    const mockData: Assessment[] = [
      {
        id: 1,
        title: '心理健康自评量表',
        description: '用于评估个人心理健康状况的标准化量表',
        category: 'mental_health',
        duration: 15,
        questions: [
          {
            id: 1,
            question: '最近一周，您是否经常感到焦虑或紧张？',
            type: 'single',
            options: ['从不', '偶尔', '经常', '总是'],
            required: true
          }
        ],
        status: 'published',
        completedCount: 156,
        createdAt: '2024-01-15 10:00:00',
        updatedAt: '2024-01-15 10:00:00',
        creator: 'admin'
      },
      {
        id: 2,
        title: 'MBTI职业性格测试',
        description: '基于MBTI理论的职业性格倾向测试',
        category: 'personality',
        duration: 20,
        questions: [
          {
            id: 1,
            question: '在社交场合中，您更倾向于？',
            type: 'single',
            options: ['主动与他人交流', '等待他人主动交流'],
            required: true
          }
        ],
        status: 'draft',
        completedCount: 0,
        createdAt: '2024-01-14 15:30:00',
        updatedAt: '2024-01-14 15:30:00',
        creator: 'admin'
      }
    ]
    
    assessmentList.value = mockData
    pagination.total = mockData.length
  } catch (error) {
    console.error('获取测评列表失败:', error)
    ElMessage.error('获取测评列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchAssessmentList()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    title: '',
    category: '',
    status: '',
    creator: ''
  })
  handleSearch()
}

// 刷新
const handleRefresh = () => {
  fetchAssessmentList()
}

// 新建测评
const handleCreate = () => {
  formMode.value = 'create'
  Object.assign(formData, {
    id: 0,
    title: '',
    description: '',
    category: '',
    duration: 30,
    questions: [],
    status: 'draft',
    completedCount: 0,
    createdAt: '',
    updatedAt: '',
    creator: 'admin'
  })
  formVisible.value = true
}

// 编辑测评
const handleEdit = (assessment: Assessment) => {
  formMode.value = 'edit'
  Object.assign(formData, {
    ...assessment,
    questions: [...assessment.questions]
  })
  formVisible.value = true
}

// 预览测评
const handlePreview = (assessment: Assessment) => {
  currentAssessment.value = assessment
  previewVisible.value = true
}

// 发布测评
const handlePublish = async (assessment: Assessment) => {
  await ElMessageBox.confirm(`确定要发布测评 "${assessment.title}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 300))
    assessment.status = 'published'
    ElMessage.success('测评已发布')
    fetchAssessmentList()
  } catch (error) {
    console.error('发布测评失败:', error)
    ElMessage.error('发布测评失败')
  }
}

// 下架测评
const handleArchive = async (assessment: Assessment) => {
  await ElMessageBox.confirm(`确定要下架测评 "${assessment.title}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 300))
    assessment.status = 'archived'
    ElMessage.success('测评已下架')
    fetchAssessmentList()
  } catch (error) {
    console.error('下架测评失败:', error)
    ElMessage.error('下架测评失败')
  }
}

// 删除测评
const handleDelete = async (assessment: Assessment) => {
  await ElMessageBox.confirm(`确定要删除测评 "${assessment.title}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 300))
    const index = assessmentList.value.findIndex(a => a.id === assessment.id)
    if (index !== -1) {
      assessmentList.value.splice(index, 1)
    }
    ElMessage.success('删除成功')
  } catch (error) {
    console.error('删除测评失败:', error)
    ElMessage.error('删除测评失败')
  }
}

// 批量发布
const handleBatchPublish = async () => {
  if (selectedIds.value.length === 0) return
  
  await ElMessageBox.confirm(`确定要发布选中的 ${selectedIds.value.length} 个测评吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    // 模拟批量API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('批量发布成功')
    fetchAssessmentList()
  } catch (error) {
    console.error('批量发布失败:', error)
    ElMessage.error('批量发布失败')
  }
}

// 批量下架
const handleBatchArchive = async () => {
  if (selectedIds.value.length === 0) return
  
  await ElMessageBox.confirm(`确定要下架选中的 ${selectedIds.value.length} 个测评吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    // 模拟批量API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('批量下架成功')
    fetchAssessmentList()
  } catch (error) {
    console.error('批量下架失败:', error)
    ElMessage.error('批量下架失败')
  }
}

// 导出数据
const handleExport = () => {
  ElMessage.success('导出功能开发中...')
}

// 添加题目
const handleAddQuestion = () => {
  formData.questions.push({
    id: Date.now(),
    question: '',
    type: 'single',
    options: ['选项 A', '选项 B'],
    required: true
  })
}

// 删除题目
const handleRemoveQuestion = (index: number) => {
  ElMessageBox.confirm('确定要删除这道题目吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    formData.questions.splice(index, 1)
  })
}

// 添加选项
const handleAddOption = (questionIndex: number) => {
  const question = formData.questions[questionIndex]
  if (question.options && question.options.length < 6) {
    question.options.push(`选项 ${String.fromCharCode(65 + question.options.length)}`)
  }
}

// 删除选项
const handleRemoveOption = (questionIndex: number, optionIndex: number) => {
  const question = formData.questions[questionIndex]
  if (question.options && question.options.length > 2) {
    question.options.splice(optionIndex, 1)
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    if (formData.questions.length === 0) {
      ElMessage.error('请至少添加一道题目')
      return
    }
    
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    if (formMode.value === 'create') {
      const newAssessment: Assessment = {
        ...formData,
        id: Date.now(),
        createdAt: new Date().toLocaleString('zh-CN'),
        updatedAt: new Date().toLocaleString('zh-CN')
      }
      assessmentList.value.unshift(newAssessment)
      ElMessage.success('测评创建成功')
    } else {
      const index = assessmentList.value.findIndex(a => a.id === formData.id)
      if (index !== -1) {
        assessmentList.value[index] = {
          ...formData,
          updatedAt: new Date().toLocaleString('zh-CN')
        }
      }
      ElMessage.success('测评更新成功')
    }
    
    formVisible.value = false
    fetchAssessmentList()
  } catch (error) {
    console.error('表单提交失败:', error)
  }
}

// 选择变化
const handleSelectionChange = (selection: Assessment[]) => {
  selectedIds.value = selection.map(assessment => assessment.id)
}

// 分页变化
const handleSizeChange = (size: number) => {
  pagination.size = size
  fetchAssessmentList()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  fetchAssessmentList()
}

// 工具函数
const getCategoryType = (category: string) => {
  const types = {
    mental_health: 'primary',
    personality: 'success',
    career: 'warning',
    relationship: 'danger',
    learning: 'info'
  }
  return types[category] || 'info'
}

const getCategoryLabel = (category: string) => {
  const labels = {
    mental_health: '心理健康',
    personality: '人格测试',
    career: '职业倾向',
    relationship: '人际关系',
    learning: '学习评估'
  }
  return labels[category] || '其他'
}

const getStatusType = (status: string) => {
  const types = {
    draft: 'info',
    published: 'success',
    archived: 'warning'
  }
  return types[status] || 'info'
}

const getStatusLabel = (status: string) => {
  const labels = {
    draft: '草稿',
    published: '已发布',
    archived: '已下架'
  }
  return labels[status] || '未知'
}

onMounted(() => {
  fetchAssessmentList()
})
</script>

<style scoped lang="scss">
.assessments-container {
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

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.questions-section {
  .questions-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 12px;
    background-color: #f5f7fa;
    border-radius: 6px;
  }
  
  .questions-list {
    max-height: 400px;
    overflow-y: auto;
    
    .question-item {
      margin-bottom: 20px;
      padding: 16px;
      border: 1px solid #e4e7ed;
      border-radius: 6px;
      background-color: #fafafa;
      
      .question-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        
        .question-number {
          font-weight: 500;
          color: #303133;
        }
      }
      
      .options-list {
        .option-item {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 8px;
        }
      }
    }
    
    .no-questions {
      text-align: center;
      padding: 40px 0;
    }
  }
}

.assessment-preview {
  .preview-header {
    text-align: center;
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e4e7ed;
    
    h3 {
      margin: 0 0 8px 0;
      color: #303133;
    }
    
    p {
      color: #606266;
      margin-bottom: 12px;
    }
    
    .preview-meta {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 16px;
      
      span {
        color: #909399;
        font-size: 14px;
      }
    }
  }
  
  .preview-questions {
    max-height: 400px;
    overflow-y: auto;
    
    .preview-question {
      margin-bottom: 20px;
      padding: 16px;
      background-color: #f5f7fa;
      border-radius: 6px;
      
      .question-title {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
        
        .question-number {
          font-weight: 500;
          color: #303133;
        }
      }
      
      .question-content {
        .single-choice,
        .multiple-choice {
          .el-radio,
          .el-checkbox {
            display: block;
            margin-bottom: 8px;
          }
        }
        
        .text-input {
          .el-textarea__inner {
            background-color: #fafafa;
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .assessments-container {
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
  
  .preview-meta {
    flex-direction: column;
    gap: 8px !important;
  }
}
</style>