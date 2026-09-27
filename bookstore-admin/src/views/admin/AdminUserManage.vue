<template>
  <div class="admin-user-manage">
    <div class="page-header">
      <h2>👤 用户管理</h2>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="用户名/昵称/手机号/邮箱" clearable style="width:220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width:120px">
            <el-option label="正常" :value="1" />
            <el-option label="冻结" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="用户ID" width="100" />
        <el-table-column label="用户信息" min-width="150">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px;">
              <el-avatar :size="36" :src="getFullUrl(row.avatar)">
                {{ row.nickname?.charAt(0) || row.username?.charAt(0) || 'U' }}
              </el-avatar>
              <div>
                <div style="font-weight:500;">{{ row.nickname || row.username }}</div>
                <div style="font-size:12px;color:#909399;">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column label="商家" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isShopOwner === 1 ? 'success' : 'info'" size="small">
              {{ row.isShopOwner === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '冻结' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row.id)">详情</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '冻结' : '解封' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="searchForm.page"
        v-model:page-size="searchForm.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="500px">
      <div v-if="currentRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">{{ currentRow.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentRow.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ currentRow.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentRow.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentRow.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag :type="currentRow.role === 1 ? 'danger' : 'info'" size="small">
              {{ currentRow.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="商家">
            <el-tag :type="currentRow.isShopOwner === 1 ? 'success' : 'info'" size="small">
              {{ currentRow.isShopOwner === 1 ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentRow.status === 1 ? 'success' : 'danger'">
              {{ currentRow.status === 1 ? '正常' : '冻结' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间" :span="2">{{ currentRow.createTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'

defineOptions({ name: 'AdminUserManage' })

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const detailVisible = ref(false)
const currentRow = ref(null)

const searchForm = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: null
})

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: searchForm.page, size: searchForm.size }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    const res = await request.get('/admin/user/page', { params })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { searchForm.page = 1; loadData() }
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.status = null
  searchForm.page = 1
  loadData()
}

const viewDetail = async (id) => {
  try {
    const res = await request.get(`/admin/user/${id}`)
    currentRow.value = res.data
    detailVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '冻结' : '解封'
  try {
    await ElMessageBox.confirm(`确认${action}该用户吗？`, '提示', { type: 'warning' })
    await request.put(`/admin/user/${row.id}/status?status=${newStatus}`)
    ElMessage.success(`已${action}`)
    await loadData()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

onMounted(loadData)
</script>

<style scoped>
.admin-user-manage { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.search-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
