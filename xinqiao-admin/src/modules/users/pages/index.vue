<template>
  <div class="users-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="请选择角色" clearable>
            <el-option label="管理员" value="admin" />
            <el-option label="咨询师" value="counselor" />
            <el-option label="用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="活跃" value="active" />
            <el-option label="禁用" value="banned" />
            <el-option label="待审核" value="pending" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card class="actions-card">
      <div class="actions-left">
        <el-button type="primary" @click="handleCreate"><el-icon><Plus /></el-icon>新增用户</el-button>
        <el-button type="danger" @click="handleBatchDelete" :disabled="selectedIds.length === 0"><el-icon><Delete /></el-icon>批量删除</el-button>
        <el-button @click="handleExport"><el-icon><Download /></el-icon>导出数据</el-button>
        <el-button @click="handleSync"><el-icon><Refresh /></el-icon>导入数据库</el-button>
      </div>
      <div class="actions-right">
        <el-button @click="handleRefresh"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
    </el-card>
    <el-card class="table-card">
      <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange" stripe style="width: 100%">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :src="row.avatar" :size="40"><el-icon><User /></el-icon></el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">{{ getRoleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.role==='counselor'" type="success" link @click="handleViewCounselor(row)">咨询师资料</el-button>
            <el-button type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
            <el-button :type="row.status === 'banned' ? 'success' : 'danger'" link @click="handleToggleStatus(row)">
              {{ row.status === 'banned' ? '启用' : '禁用' }}
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </el-card>
    <UserFormDialog v-model="formVisible" :user="currentUser" :mode="formMode" @success="handleFormSuccess" />
    <el-drawer v-model="profileDrawer" title="咨询师资料" size="40%">
      <div class="profile-item"><span>用户名</span><span>{{ counselorProfile?.username }}</span></div>
      <div class="profile-item"><span>标题</span><span>{{ counselorProfile?.title }}</span></div>
      <div class="profile-item"><span>模式</span><span>{{ counselorProfile?.defaultMode }}</span></div>
      <div class="profile-item"><span>标签</span><span>{{ (counselorProfile?.tags || []).join(', ') }}</span></div>
      <div class="profile-item"><span>简介</span><span>{{ counselorProfile?.bio }}</span></div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete, Download, User } from '@element-plus/icons-vue'
import { userApi } from '@/modules/users/api'
import UserFormDialog from '@/components/UserFormDialog/index.vue'
import type { User as UserType, UserRole, UserStatus } from '@/types'
import { fetchCounselorProfile, type CounselorProfile } from '@/modules/counselors/api'

const searchForm = reactive({ username: '', role: undefined as UserRole | undefined, status: undefined as UserStatus | undefined, startDate: '', endDate: '' })
const dateRange = ref([])
const loading = ref(false)
const userList = ref<UserType[]>([])
const selectedIds = ref<number[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const currentUser = ref<UserType | null>(null)
const profileDrawer = ref(false)
const counselorProfile = ref<CounselorProfile | null>(null)

const fetchUsers = async () => {
  loading.value = true
  try {
    const params: any = { ...searchForm, page: pagination.page, size: pagination.size }
    if (dateRange.value && (dateRange.value as any).length === 2) { params.startDate = (dateRange.value as any)[0]; params.endDate = (dateRange.value as any)[1] }
    const result = await userApi.getUserList(params)
    userList.value = result.list
    pagination.total = result.total
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; fetchUsers() }
const handleReset = () => { Object.assign(searchForm, { username: '', role: '', status: '', startDate: '', endDate: '' }); dateRange.value = []; handleSearch() }
const handleRefresh = () => { fetchUsers() }

const handleSync = async () => {
  try { const res = await userApi.syncFromUserInfo({ role: 'user', status: 'active' }); ElMessage.success(`已导入 ${res.created} 个用户`); fetchUsers() } catch (e) { console.error(e) }
}

const handleCreate = () => { formMode.value = 'create'; currentUser.value = null; formVisible.value = true }
const handleEdit = (user: UserType) => { formMode.value = 'edit'; currentUser.value = user; formVisible.value = true }

const handleViewCounselor = async (user: UserType) => {
  try { counselorProfile.value = await fetchCounselorProfile(user.username); profileDrawer.value = true } catch (e) { counselorProfile.value = null; profileDrawer.value = false }
}

const handleDelete = async (user: UserType) => {
  await ElMessageBox.confirm(`确定要删除用户 "${user.username}" 吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
  try { await userApi.deleteUser(user.id); ElMessage.success('删除成功'); fetchUsers() } catch (error) { console.error('删除用户失败:', error) }
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return
  await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个用户吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
  try { await userApi.batchDeleteUsers(selectedIds.value); ElMessage.success('批量删除成功'); fetchUsers() } catch (error) { console.error('批量删除用户失败:', error) }
}

const handleResetPassword = async (user: UserType) => {
  await ElMessageBox.confirm(`确定要重置用户 "${user.username}" 的密码吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
  try { await userApi.resetPassword(user.id); ElMessage.success('密码重置成功') } catch (error) { console.error('重置密码失败:', error) }
}

const handleToggleStatus = async (user: UserType) => {
  const newStatus = user.status === 'banned' ? 'active' : 'banned'
  const action = user.status === 'banned' ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定要${action}用户 "${user.username}" 吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
  try { await userApi.updateUserStatus(user.id, newStatus); ElMessage.success(`${action}成功`); fetchUsers() } catch (error) { console.error(`${action}用户失败:`, error) }
}

const handleExport = () => { ElMessage.success('导出功能开发中...') }
const handleSelectionChange = (selection: UserType[]) => { selectedIds.value = selection.map(user => user.id) }
const handleSizeChange = (size: number) => { pagination.size = size; fetchUsers() }
const handleCurrentChange = (page: number) => { pagination.page = page; fetchUsers() }
const handleFormSuccess = () => { formVisible.value = false; fetchUsers() }

const getRoleType = (role: UserRole) => ({ admin: 'danger', counselor: 'warning', user: 'info' } as any)[role] || 'info'
const getRoleLabel = (role: UserRole) => ({ admin: '管理员', counselor: '咨询师', user: '用户' } as any)[role] || '用户'
const getStatusType = (status: UserStatus) => ({ active: 'success', banned: 'danger', pending: 'warning', inactive: 'info' } as any)[status] || 'info'
const getStatusLabel = (status: UserStatus) => ({ active: '活跃', banned: '禁用', pending: '待审核', inactive: '未激活' } as any)[status] || '未知'

onMounted(() => { fetchUsers() })
</script>

<style scoped lang="scss">
.users-container { padding: 20px }
.search-card,.actions-card,.table-card { border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); margin-bottom: 20px }
.search-form .el-form-item { margin-bottom: 0 }
.actions-card { display: flex; justify-content: space-between; align-items: center }
.actions-card .actions-left,.actions-card .actions-right { display: flex; gap: 12px }
.pagination-container { display: flex; justify-content: flex-end; margin-top: 20px; padding-top: 20px; border-top: 1px solid #e4e7ed }
.profile-item { display: flex; justify-content: space-between; padding: 8px 0 }
@media (max-width: 768px) { .users-container { padding: 16px } .search-form .el-form-item { width: 100%; margin-bottom: 12px } .actions-card { flex-direction: column; gap: 12px } .actions-card .actions-left,.actions-card .actions-right { width: 100%; justify-content: center } }
</style>
