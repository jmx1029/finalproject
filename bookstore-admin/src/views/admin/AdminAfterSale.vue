<template>
  <div class="admin-after-sale">
    <div class="page-header">
      <h2>📋 售后仲裁管理</h2>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <!-- ===== 新增：订单号搜索 ===== -->
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderId" placeholder="请输入订单号" clearable style="width:180px" @clear="loadData" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width:140px" @change="loadData">
            <el-option label="待商家审核" :value="0" />
            <el-option label="商家通过" :value="1" />
            <el-option label="商家驳回" :value="2" />
            <el-option label="待仲裁" :value="3" />
            <el-option label="仲裁完成" :value="4" />
            <el-option label="已关闭" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 售后列表 -->
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="售后单号" width="100" />
        <el-table-column prop="orderId" label="订单号" width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ row.type === 1 ? '仅退款' : '退货退款' }}</template>
        </el-table-column>
        <el-table-column label="退款金额" width="120">
          <template #default="{ row }"><span style="color:#F56C6C;">¥{{ row.amount }}</span></template>
        </el-table-column>
        <el-table-column label="申请原因" prop="reason" width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" prop="applyTime" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <!-- 状态 1、2、3 可仲裁 -->
            <template v-if="[1, 2, 3].includes(row.status)">
              <el-button size="small" type="success" @click="arbitrate(row.id, 4)">通过退款</el-button>
              <el-button size="small" type="danger" @click="arbitrate(row.id, 5)">驳回关闭</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pagination"
        v-model:current-page="searchForm.page"
        v-model:page-size="searchForm.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="售后详情" width="500px">
      <div v-if="currentRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="售后单号">{{ currentRow.id }}</el-descriptions-item>
          <el-descriptions-item label="关联订单">{{ currentRow.orderId }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ currentRow.userId }}</el-descriptions-item>
          <el-descriptions-item label="店铺ID">{{ currentRow.shopId }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ currentRow.type === 1 ? '仅退款' : '退货退款' }}</el-descriptions-item>
          <el-descriptions-item label="退款金额">¥{{ currentRow.amount }}</el-descriptions-item>
          <el-descriptions-item label="原因">{{ currentRow.reason }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ currentRow.description || '无' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(currentRow.status) }}</el-descriptions-item>
          <el-descriptions-item label="商家备注">{{ currentRow.shopReviewRemark || '无' }}</el-descriptions-item>
          <el-descriptions-item label="管理员备注">{{ currentRow.adminReviewRemark || '无' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const detailVisible = ref(false)
const currentRow = ref(null)

const searchForm = reactive({
  page: 1,
  size: 10,
  orderId: '',    // 新增
  status: null
})

const statusText = (s) => {
  const map = { 0: '待商家审核', 1: '商家通过', 2: '商家驳回', 3: '待仲裁', 4: '仲裁完成', 5: '已关闭' }
  return map[s] || '未知'
}
const statusTag = (s) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'danger', 3: 'info', 4: 'success', 5: 'info' }
  return map[s] || 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    // 构建请求参数，只传非空值
    const params = {
      page: searchForm.page,
      size: searchForm.size
    }
    if (searchForm.orderId) params.orderId = searchForm.orderId
    if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status

    const res = await request.get('/admin/after-sale/list', { params })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载售后列表失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.orderId = ''   // 清空订单号
  searchForm.status = null
  searchForm.page = 1
  loadData()
}

const viewDetail = (row) => {
  currentRow.value = row
  detailVisible.value = true
}

// 仲裁：status 4通过（退款），5驳回（关闭）
const arbitrate = async (id, status) => {
  const action = status === 4 ? '通过退款' : '驳回关闭'
  try {
    await ElMessageBox.confirm(`确认${action}该售后申请吗？`, '提示', { type: 'warning' })
    await request.put(`/admin/after-sale/arbitrate/${id}?status=${status}&remark=${action}`)
    ElMessage.success(`仲裁成功：${action}`)
    await loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('仲裁操作失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.admin-after-sale { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.search-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
