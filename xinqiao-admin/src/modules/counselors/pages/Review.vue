<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="status" placeholder="状态" style="width: 160px" @change="load">
        <el-option label="全部" value="" />
        <el-option label="待审核" value="pending" />
        <el-option label="已通过" value="approved" />
        <el-option label="已拒绝" value="rejected" />
      </el-select>
      <el-input v-model="q" placeholder="搜索用户名/姓名" style="width: 240px; margin-left: 12px" @keyup.enter="load" />
      <el-button type="primary" style="margin-left: 12px" @click="load">查询</el-button>
    </div>
    <el-table :data="list" stripe style="width: 100%">
      <el-table-column prop="userId" label="用户ID" width="120" />
      <el-table-column prop="realName" label="姓名" width="160" />
      <el-table-column prop="phone" label="电话" width="160" />
      <el-table-column prop="qualificationType" label="资质" width="160" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button v-if="row.status==='pending'" type="success" @click="onApprove(row)">通过</el-button>
          <el-button v-if="row.status==='pending'" type="danger" @click="onReject(row)">拒绝</el-button>
          <el-button @click="onDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawer" title="申请详情" size="40%">
      <pre class="json">{{ current }}</pre>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchApplications, approveApplication, rejectApplication, type CounselorApplicationItem } from '@/modules/counselors/api'

const status = ref('pending')
const q = ref('')
const list = ref<CounselorApplicationItem[]>([])
const drawer = ref(false)
const current = ref<any>(null)

async function load() {
  try {
    list.value = await fetchApplications({ status: status.value || undefined, query: q.value || undefined })
  } catch (e) {
    list.value = []
  }
}

async function onApprove(row: CounselorApplicationItem) {
  await approveApplication(row.id)
  ElMessage.success('已通过')
  await load()
}

async function onReject(row: CounselorApplicationItem) {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', { inputPlaceholder: '原因' }).catch(() => ({ value: '' }))
  await rejectApplication(row.id, value || '')
  ElMessage.success('已拒绝')
  await load()
}

function onDetail(row: CounselorApplicationItem) {
  drawer.value = true
  current.value = row
}

onMounted(load)
</script>

<style scoped>
.page { padding: 16px }
.toolbar { margin-bottom: 12px; display: flex; align-items: center }
.json { background: #f8f9fb; padding: 12px; border-radius: 6px; white-space: pre-wrap }
</style>
