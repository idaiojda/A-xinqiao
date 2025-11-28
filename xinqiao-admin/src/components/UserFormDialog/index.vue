<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="500px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="80px"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="formData.username"
          placeholder="请输入用户名"
          :disabled="mode === 'edit'"
        />
      </el-form-item>
      
      
      
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="formData.phone"
          placeholder="请输入手机号"
        />
      </el-form-item>
      
      <el-form-item label="角色" prop="role">
        <el-select v-model="formData.role" placeholder="请选择角色" style="width: 100%">
          <el-option label="管理员" value="admin" />
          <el-option label="咨询师" value="counselor" />
          <el-option label="用户" value="user" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio label="active">活跃</el-radio>
          <el-radio label="inactive">未激活</el-radio>
          <el-radio label="banned">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="头像" prop="avatar">
        <el-upload
          class="avatar-uploader"
          action="/api/upload/avatar"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :before-upload="beforeAvatarUpload"
        >
          <img v-if="formData.avatar" :src="formData.avatar" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { userApi } from '@/modules/users/api'
import type { User, UserForm } from '@/types'
import { UserRole, UserStatus } from '@/types'

interface Props {
  modelValue: boolean
  user?: User | null
  mode: 'create' | 'edit'
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

// 表单数据
const formData = ref<UserForm>({
  username: '',
  phone: '',
  role: UserRole.USER,
  status: UserStatus.ACTIVE,
  avatar: ''
})

// 表单验证规则
const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 对话框标题
const title = computed(() => props.mode === 'create' ? '新增用户' : '编辑用户')

// 对话框显示状态
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 监听用户数据变化
watch(() => props.user, (newUser) => {
  if (newUser && props.mode === 'edit') {
    formData.value = {
      username: newUser.username,
      phone: newUser.phone || '',
      role: newUser.role,
      status: newUser.status,
      avatar: newUser.avatar || ''
    }
  } else if (props.mode === 'create') {
    // 重置表单
    formData.value = {
      username: '',
      phone: '',
      role: UserRole.USER,
      status: UserStatus.ACTIVE,
      avatar: ''
    }
  }
}, { immediate: true })

// 头像上传成功
const handleAvatarSuccess = (response: any) => {
  formData.value.avatar = response.url
}

// 头像上传前验证
const beforeAvatarUpload = (file: File) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG) {
    ElMessage.error('头像只能是 JPG/PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
  }
  return isJPG && isLt2M
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (props.mode === 'create') {
          await userApi.createUser(formData.value)
          ElMessage.success('用户创建成功')
        } else if (props.user) {
          await userApi.updateUser(props.user.id, formData.value)
          ElMessage.success('用户更新成功')
        }
        
        visible.value = false
        emit('success')
      } catch (error) {
        console.error('保存用户失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
  formRef.value?.resetFields()
}
</script>

<style scoped lang="scss">
.avatar-uploader {
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
  
  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 100px;
    height: 100px;
    text-align: center;
    line-height: 100px;
  }
  
  .avatar {
    width: 100px;
    height: 100px;
    display: block;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
