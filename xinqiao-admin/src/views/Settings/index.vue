<template>
  <div class="settings-container">
    <div class="page-header">
      <h1>系统设置</h1>
      <p>配置系统参数和权限管理</p>
    </div>

    <el-tabs v-model="activeTab" class="settings-tabs">
      <!-- 基础设置 -->
      <el-tab-pane label="基础设置" name="basic">
        <div class="settings-section">
          <h3>系统基础信息</h3>
          <el-form :model="basicSettings" label-width="120px" style="max-width: 600px">
            <el-form-item label="系统名称">
              <el-input v-model="basicSettings.systemName" placeholder="请输入系统名称" />
            </el-form-item>
            <el-form-item label="系统Logo">
              <el-upload
                class="logo-upload"
                action="/api/upload/logo"
                :show-file-list="false"
                :on-success="handleLogoSuccess"
                :before-upload="beforeLogoUpload"
              >
                <img v-if="basicSettings.logoUrl" :src="basicSettings.logoUrl" class="logo-image" />
                <el-icon v-else class="logo-upload-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="系统描述">
              <el-input
                v-model="basicSettings.description"
                type="textarea"
                :rows="3"
                placeholder="请输入系统描述"
              />
            </el-form-item>
            <el-form-item label="备案信息">
              <el-input v-model="basicSettings.icp" placeholder="请输入备案号" />
            </el-form-item>
            <el-form-item label="技术支持">
              <el-input v-model="basicSettings.support" placeholder="请输入技术支持信息" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBasicSettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 安全设置 -->
      <el-tab-pane label="安全设置" name="security">
        <div class="settings-section">
          <h3>密码安全策略</h3>
          <el-form :model="securitySettings" label-width="160px" style="max-width: 800px">
            <el-form-item label="密码最小长度">
              <el-input-number v-model="securitySettings.minPasswordLength" :min="6" :max="20" />
            </el-form-item>
            <el-form-item label="密码复杂度要求">
              <el-checkbox-group v-model="securitySettings.passwordComplexity">
                <el-checkbox label="uppercase">包含大写字母</el-checkbox>
                <el-checkbox label="lowercase">包含小写字母</el-checkbox>
                <el-checkbox label="number">包含数字</el-checkbox>
                <el-checkbox label="special">包含特殊字符</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="密码过期时间">
              <el-input-number v-model="securitySettings.passwordExpiryDays" :min="0" :max="365" />
              <span style="margin-left: 8px; color: #909399">天（0表示永不过期）</span>
            </el-form-item>
            <el-form-item label="登录失败锁定">
              <el-input-number v-model="securitySettings.maxLoginAttempts" :min="3" :max="10" />
              <span style="margin-left: 8px; color: #909399">次失败后锁定账户</span>
            </el-form-item>
            <el-form-item label="会话超时时间">
              <el-input-number v-model="securitySettings.sessionTimeout" :min="15" :max="480" />
              <span style="margin-left: 8px; color: #909399">分钟</span>
            </el-form-item>
            <el-form-item label="IP访问限制">
              <el-switch v-model="securitySettings.ipRestriction" />
            </el-form-item>
            <el-form-item v-if="securitySettings.ipRestriction" label="允许访问IP">
              <el-input
                v-model="securitySettings.allowedIPs"
                type="textarea"
                :rows="3"
                placeholder="请输入允许访问的IP地址，多个IP用逗号分隔"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSecuritySettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 邮件设置 -->
      <el-tab-pane label="邮件设置" name="email">
        <div class="settings-section">
          <h3>SMTP邮件配置</h3>
          <el-form :model="emailSettings" label-width="120px" style="max-width: 600px">
            <el-form-item label="SMTP服务器">
              <el-input v-model="emailSettings.smtpHost" placeholder="smtp.example.com" />
            </el-form-item>
            <el-form-item label="SMTP端口">
              <el-input-number v-model="emailSettings.smtpPort" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item label="加密方式">
              <el-select v-model="emailSettings.encryption">
                <el-option label="无" value="none" />
                <el-option label="SSL" value="ssl" />
                <el-option label="TLS" value="tls" />
              </el-select>
            </el-form-item>
            <el-form-item label="发件人邮箱">
              <el-input v-model="emailSettings.fromEmail" placeholder="noreply@example.com" />
            </el-form-item>
            <el-form-item label="发件人名称">
              <el-input v-model="emailSettings.fromName" placeholder="心桥心理" />
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="emailSettings.username" placeholder="邮箱用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="emailSettings.password"
                type="password"
                placeholder="邮箱密码或授权码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button @click="testEmailConnection">测试连接</el-button>
              <el-button type="primary" @click="saveEmailSettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 通知设置 -->
      <el-tab-pane label="通知设置" name="notification">
        <div class="settings-section">
          <h3>系统通知配置</h3>
          <el-form :model="notificationSettings" label-width="140px" style="max-width: 800px">
            <el-form-item label="系统通知">
              <el-switch v-model="notificationSettings.systemEnabled" />
            </el-form-item>
            <el-form-item label="邮件通知">
              <el-switch v-model="notificationSettings.emailEnabled" />
            </el-form-item>
            <el-form-item label="短信通知">
              <el-switch v-model="notificationSettings.smsEnabled" />
            </el-form-item>
            <el-form-item label="推送通知">
              <el-switch v-model="notificationSettings.pushEnabled" />
            </el-form-item>
            
            <h4 style="margin: 20px 0 16px 0; color: #303133;">通知事件配置</h4>
            
            <el-form-item label="用户注册">
              <el-checkbox v-model="notificationSettings.events.userRegistration">启用</el-checkbox>
              <span style="margin-left: 16px; color: #909399">新用户注册时发送通知</span>
            </el-form-item>
            <el-form-item label="咨询预约">
              <el-checkbox v-model="notificationSettings.events.consultationBooking">启用</el-checkbox>
              <span style="margin-left: 16px; color: #909399">用户预约咨询时发送通知</span>
            </el-form-item>
            <el-form-item label="咨询取消">
              <el-checkbox v-model="notificationSettings.events.consultationCancellation">启用</el-checkbox>
              <span style="margin-left: 16px; color: #909399">咨询被取消时发送通知</span>
            </el-form-item>
            <el-form-item label="支付成功">
              <el-checkbox v-model="notificationSettings.events.paymentSuccess">启用</el-checkbox>
              <span style="margin-left: 16px; color: #909399">用户支付成功时发送通知</span>
            </el-form-item>
            <el-form-item label="系统异常">
              <el-checkbox v-model="notificationSettings.events.systemError">启用</el-checkbox>
              <span style="margin-left: 16px; color: #909399">系统出现异常时发送通知</span>
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="saveNotificationSettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 权限管理 -->
      <el-tab-pane label="权限管理" name="permissions">
        <div class="settings-section">
          <div class="permission-header">
            <h3>角色权限管理</h3>
            <el-button type="primary" @click="handleAddRole">
              <el-icon><Plus /></el-icon>
              添加角色
            </el-button>
          </div>
          
          <el-table :data="roles" style="width: 100%">
            <el-table-column prop="name" label="角色名称" width="150" />
            <el-table-column prop="description" label="角色描述" />
            <el-table-column prop="permissions" label="权限数量" width="100">
              <template #default="{ row }">
                {{ row.permissions.length }}
              </template>
            </el-table-column>
            <el-table-column prop="userCount" label="用户数" width="80" />
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" text @click="handleEditRole(row)">编辑</el-button>
                <el-button type="primary" text @click="handleSetPermissions(row)">权限</el-button>
                <el-button 
                  type="danger" 
                  text 
                  @click="handleDeleteRole(row)"
                  :disabled="row.builtin"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 系统日志 -->
      <el-tab-pane label="系统日志" name="logs">
        <div class="settings-section">
          <div class="log-header">
            <h3>系统操作日志</h3>
            <div class="log-actions">
              <el-date-picker
                v-model="logDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                size="small"
                style="width: 240px; margin-right: 12px;"
              />
              <el-select v-model="logLevel" placeholder="日志级别" size="small" style="width: 120px; margin-right: 12px;">
                <el-option label="全部" value="" />
                <el-option label="信息" value="info" />
                <el-option label="警告" value="warning" />
                <el-option label="错误" value="error" />
              </el-select>
              <el-button type="primary" size="small" @click="exportLogs">
                <el-icon><Download /></el-icon>
                导出日志
              </el-button>
              <el-button type="danger" size="small" @click="clearLogs">
                <el-icon><Delete /></el-icon>
                清空日志
              </el-button>
            </div>
          </div>
          
          <el-table :data="logs" style="width: 100%" max-height="600">
            <el-table-column prop="timestamp" label="时间" width="180" />
            <el-table-column prop="level" label="级别" width="80">
              <template #default="{ row }">
                <el-tag :type="getLogLevelType(row.level)" size="small">
                  {{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="user" label="用户" width="120" />
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="action" label="操作" width="150" />
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="ip" label="IP地址" width="120" />
          </el-table>
          
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="logCurrentPage"
              v-model:page-size="logPageSize"
              :page-sizes="[20, 50, 100, 200]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="logTotal"
              @size-change="handleLogSizeChange"
              @current-change="handleLogCurrentChange"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 角色编辑对话框 -->
    <el-dialog
      v-model="roleDialog.visible"
      :title="roleDialog.isEdit ? '编辑角色' : '添加角色'"
      width="500px"
    >
      <el-form :model="roleDialog.form" label-width="80px">
        <el-form-item label="角色名称">
          <el-input v-model="roleDialog.form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input
            v-model="roleDialog.form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限设置对话框 -->
    <el-dialog
      v-model="permissionDialog.visible"
      title="权限设置"
      width="600px"
    >
      <div class="permission-tree">
        <el-tree
          ref="permissionTreeRef"
          :data="permissionTreeData"
          show-checkbox
          node-key="id"
          :default-checked-keys="permissionDialog.checkedKeys"
          :props="{ label: 'name', children: 'children' }"
        />
      </div>
      <template #footer>
        <el-button @click="permissionDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="savePermissions">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Delete } from '@element-plus/icons-vue'

// 当前激活的标签页
const activeTab = ref('basic')

// 基础设置
const basicSettings = reactive({
  systemName: '心桥心理管理系统',
  logoUrl: '',
  description: '专业的心理咨询服务平台',
  icp: '京ICP备12345678号',
  support: '技术支持：心桥科技'
})

// 安全设置
const securitySettings = reactive({
  minPasswordLength: 8,
  passwordComplexity: ['lowercase', 'number'],
  passwordExpiryDays: 90,
  maxLoginAttempts: 5,
  sessionTimeout: 120,
  ipRestriction: false,
  allowedIPs: ''
})

// 邮件设置
const emailSettings = reactive({
  smtpHost: 'smtp.example.com',
  smtpPort: 587,
  encryption: 'tls',
  fromEmail: 'noreply@xinqiao.com',
  fromName: '心桥心理',
  username: '',
  password: ''
})

// 通知设置
const notificationSettings = reactive({
  systemEnabled: true,
  emailEnabled: true,
  smsEnabled: false,
  pushEnabled: true,
  events: {
    userRegistration: true,
    consultationBooking: true,
    consultationCancellation: true,
    paymentSuccess: true,
    systemError: true
  }
})

// 角色数据
const roles = ref([
  {
    id: 1,
    name: '超级管理员',
    description: '拥有系统所有权限',
    permissions: ['*'],
    userCount: 2,
    createdAt: '2024-01-01 10:00:00',
    builtin: true
  },
  {
    id: 2,
    name: '系统管理员',
    description: '管理系统配置和用户',
    permissions: ['system.*', 'user.*', 'role.*'],
    userCount: 3,
    createdAt: '2024-01-02 09:30:00',
    builtin: true
  },
  {
    id: 3,
    name: '内容审核员',
    description: '审核平台内容',
    permissions: ['content.*'],
    userCount: 5,
    createdAt: '2024-01-03 14:20:00',
    builtin: false
  },
  {
    id: 4,
    name: '咨询师',
    description: '提供心理咨询服务',
    permissions: ['consultation.*', 'user.read'],
    userCount: 15,
    createdAt: '2024-01-04 11:15:00',
    builtin: false
  }
])

// 权限树数据
const permissionTreeData = [
  {
    id: 'system',
    name: '系统管理',
    children: [
      { id: 'system.basic', name: '基础设置' },
      { id: 'system.security', name: '安全设置' },
      { id: 'system.email', name: '邮件设置' },
      { id: 'system.notification', name: '通知设置' },
      { id: 'system.logs', name: '系统日志' }
    ]
  },
  {
    id: 'user',
    name: '用户管理',
    children: [
      { id: 'user.read', name: '查看用户' },
      { id: 'user.create', name: '创建用户' },
      { id: 'user.update', name: '修改用户' },
      { id: 'user.delete', name: '删除用户' }
    ]
  },
  {
    id: 'role',
    name: '角色权限',
    children: [
      { id: 'role.read', name: '查看角色' },
      { id: 'role.create', name: '创建角色' },
      { id: 'role.update', name: '修改角色' },
      { id: 'role.delete', name: '删除角色' }
    ]
  },
  {
    id: 'content',
    name: '内容管理',
    children: [
      { id: 'content.read', name: '查看内容' },
      { id: 'content.audit', name: '审核内容' },
      { id: 'content.delete', name: '删除内容' }
    ]
  },
  {
    id: 'consultation',
    name: '咨询管理',
    children: [
      { id: 'consultation.read', name: '查看咨询' },
      { id: 'consultation.create', name: '创建咨询' },
      { id: 'consultation.update', name: '修改咨询' },
      { id: 'consultation.cancel', name: '取消咨询' }
    ]
  }
]

// 日志数据
const logs = ref([
  {
    timestamp: '2024-01-15 14:30:25',
    level: 'info',
    user: 'admin',
    module: '系统管理',
    action: '登录',
    description: '用户登录成功',
    ip: '192.168.1.100'
  },
  {
    timestamp: '2024-01-15 14:25:10',
    level: 'warning',
    user: 'test_user',
    module: '用户管理',
    action: '修改密码',
    description: '密码修改失败：原密码错误',
    ip: '192.168.1.101'
  },
  {
    timestamp: '2024-01-15 14:20:05',
    level: 'error',
    user: 'system',
    module: '数据库',
    action: '连接失败',
    description: '数据库连接超时',
    ip: 'localhost'
  }
])

// 日志筛选条件
const logDateRange = ref('')
const logLevel = ref('')
const logCurrentPage = ref(1)
const logPageSize = ref(20)
const logTotal = ref(156)

// 对话框状态
const roleDialog = reactive({
  visible: false,
  isEdit: false,
  form: {
    id: null as number | null,
    name: '',
    description: ''
  }
})

const permissionDialog = reactive({
  visible: false,
  roleId: null as number | null,
  checkedKeys: [] as string[]
})

const permissionTreeRef = ref()

// 获取日志级别标签类型
const getLogLevelType = (level: string) => {
  switch (level) {
    case 'info': return 'info'
    case 'warning': return 'warning'
    case 'error': return 'danger'
    default: return 'info'
  }
}

// 处理Logo上传
const handleLogoSuccess = (response: any) => {
  basicSettings.logoUrl = response.url
  ElMessage.success('Logo上传成功')
}

const beforeLogoUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('Logo大小不能超过 2MB!')
    return false
  }
  return true
}

// 保存基础设置
const saveBasicSettings = () => {
  ElMessage.success('基础设置保存成功')
}

// 保存安全设置
const saveSecuritySettings = () => {
  ElMessage.success('安全设置保存成功')
}

// 测试邮件连接
const testEmailConnection = () => {
  ElMessage.success('邮件连接测试成功')
}

// 保存邮件设置
const saveEmailSettings = () => {
  ElMessage.success('邮件设置保存成功')
}

// 保存通知设置
const saveNotificationSettings = () => {
  ElMessage.success('通知设置保存成功')
}

// 添加角色
const handleAddRole = () => {
  roleDialog.isEdit = false
  roleDialog.form = {
    id: null,
    name: '',
    description: ''
  }
  roleDialog.visible = true
}

// 编辑角色
const handleEditRole = (role: any) => {
  roleDialog.isEdit = true
  roleDialog.form = {
    id: role.id,
    name: role.name,
    description: role.description
  }
  roleDialog.visible = true
}

// 保存角色
const saveRole = () => {
  if (!roleDialog.form.name) {
    ElMessage.error('请输入角色名称')
    return
  }
  
  if (roleDialog.isEdit) {
    // 编辑角色
    const role = roles.value.find(r => r.id === roleDialog.form.id)
    if (role) {
      role.name = roleDialog.form.name
      role.description = roleDialog.form.description
    }
    ElMessage.success('角色编辑成功')
  } else {
    // 添加角色
    const newRole = {
      id: Date.now(),
      name: roleDialog.form.name,
      description: roleDialog.form.description,
      permissions: [],
      userCount: 0,
      createdAt: new Date().toLocaleString('zh-CN'),
      builtin: false
    }
    roles.value.push(newRole)
    ElMessage.success('角色添加成功')
  }
  
  roleDialog.visible = false
}

// 设置权限
const handleSetPermissions = (role: any) => {
  permissionDialog.roleId = role.id
  permissionDialog.checkedKeys = role.permissions
  permissionDialog.visible = true
}

// 保存权限
const savePermissions = () => {
  const checkedKeys = permissionTreeRef.value?.getCheckedKeys() || []
  const role = roles.value.find(r => r.id === permissionDialog.roleId)
  if (role) {
    role.permissions = checkedKeys as string[]
  }
  permissionDialog.visible = false
  ElMessage.success('权限设置成功')
}

// 删除角色
const handleDeleteRole = async (role: any) => {
  if (role.builtin) {
    ElMessage.error('内置角色不能删除')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除角色 "${role.name}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const index = roles.value.findIndex(r => r.id === role.id)
    if (index > -1) {
      roles.value.splice(index, 1)
    }
    ElMessage.success('角色删除成功')
  } catch (error) {
    // 用户取消删除
  }
}

// 导出日志
const exportLogs = () => {
  ElMessage.success('日志导出成功')
}

// 清空日志
const clearLogs = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有系统日志吗？此操作不可恢复。',
      '清空确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    logs.value = []
    logTotal.value = 0
    ElMessage.success('日志清空成功')
  } catch (error) {
    // 用户取消操作
  }
}

// 日志分页处理
const handleLogSizeChange = (size: number) => {
  logPageSize.value = size
  // 重新加载日志数据
}

const handleLogCurrentChange = (page: number) => {
  logCurrentPage.value = page
  // 重新加载日志数据
}

// 初始化
onMounted(() => {
  // 加载设置数据
})
</script>

<style scoped lang="scss">
.settings-container {
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

.settings-tabs {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  
  :deep(.el-tabs__header) {
    margin-bottom: 0;
    background: #f5f7fa;
    border-radius: 8px 8px 0 0;
  }
  
  :deep(.el-tabs__nav-wrap) {
    padding: 0 20px;
  }
}

.settings-section {
  padding: 30px;
  
  h3 {
    margin: 0 0 24px 0;
    font-size: 18px;
    color: #303133;
  }
  
  h4 {
    margin: 20px 0 16px 0;
    font-size: 16px;
    color: #303133;
  }
}

.logo-upload {
  :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: border-color 0.3s;
    
    &:hover {
      border-color: #409eff;
    }
  }
  
  .logo-image {
    width: 120px;
    height: 120px;
    display: block;
    object-fit: contain;
  }
  
  .logo-upload-icon {
    font-size: 28px;
    color: #8c939d;
    width: 120px;
    height: 120px;
    text-align: center;
    line-height: 120px;
  }
}

.permission-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  h3 {
    margin: 0;
  }
}

.permission-tree {
  max-height: 400px;
  overflow-y: auto;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  h3 {
    margin: 0;
  }
}

.log-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

// 响应式设计
@media (max-width: 768px) {
  .settings-container {
    padding: 12px;
  }
  
  .settings-section {
    padding: 20px;
  }
  
  .log-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .log-actions {
    flex-wrap: wrap;
  }
}

@media (max-width: 480px) {
  .settings-tabs {
    :deep(.el-tabs__nav-wrap) {
      padding: 0 12px;
    }
  }
  
  .settings-section {
    padding: 16px;
  }
}
</style>