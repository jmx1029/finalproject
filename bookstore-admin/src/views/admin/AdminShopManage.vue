<template>
  <div class="admin-shop-manage">
    <div class="page-header">
      <h2>🏪 商家管理</h2>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="店铺名称/联系人/电话" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width:120px">
            <el-option label="正常营业" :value="1" />
            <el-option label="停业整顿" :value="0" />
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
        <el-table-column prop="id" label="店铺ID" width="100" />
        <el-table-column label="店铺信息" min-width="180">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px;">
              <el-image :src="getFullUrl(row.logo)" style="width:40px;height:40px;border-radius:50%;" fit="cover">
                <template #error><span>📷</span></template>
              </el-image>
              <div>
                <div style="font-weight:500;">{{ row.name }}</div>
                <div style="font-size:12px;color:#909399;">店主：{{ row.ownerNickname || row.ownerUsername }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常营业' : '停业整顿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="入驻时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row.id)">详情</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '停业整顿' : '恢复营业' }}
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
    <el-dialog v-model="detailVisible" title="商家详情" width="600px">
      <div v-if="currentRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="店铺ID">{{ currentRow.id }}</el-descriptions-item>
          <el-descriptions-item label="店铺名称">{{ currentRow.name }}</el-descriptions-item>
          <el-descriptions-item label="店主">{{ currentRow.ownerNickname || currentRow.ownerUsername }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentRow.status === 1 ? 'success' : 'danger'">
              {{ currentRow.status === 1 ? '正常营业' : '停业整顿' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentRow.contactPerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentRow.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="店铺地址" :span="2">{{ currentRow.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="入驻时间">{{ currentRow.createTime }}</el-descriptions-item>
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

defineOptions({ name: 'AdminShopManage' })

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
    const res = await request.get('/admin/shop/page', { params })
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
    const res = await request.get(`/admin/shop/${id}`)
    currentRow.value = res.data
    detailVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '停业整顿' : '恢复营业'
  try {
    await ElMessageBox.confirm(`确认${action}该商家吗？`, '提示', { type: 'warning' })
    await request.put(`/admin/shop/${row.id}/status?status=${newStatus}`)
    ElMessage.success(`已${action}`)
    await loadData()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

onMounted(loadData)
</script>

<style scoped>
.admin-shop-manage { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.search-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
