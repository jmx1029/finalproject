<template>
  <div class="admin-order-page">
    <div class="page-header">
      <h2>📋 订单管理</h2>
      <div class="header-actions">
        <el-button type="success" @click="exportOrders" :loading="exportLoading">
          📥 导出 Excel
        </el-button>
        <el-button type="primary" plain @click="$router.push('/admin/statistics')">
          📊 统计报表
        </el-button>
      </div>
    </div>

    <!-- ===== 高级筛选 ===== -->
    <el-card class="filter-card">
      <el-form :model="searchForm" ref="searchFormRef" :inline="true" label-width="80px" size="default">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="订单号" label-width="60px">
              <el-input v-model="searchForm.orderId" placeholder="订单号" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="用户ID" label-width="60px">
              <el-input v-model="searchForm.userId" placeholder="用户ID" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="状态" label-width="60px">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width:100%">
                <el-option label="待付款" :value="0" />
                <el-option label="已支付" :value="1" />
                <el-option label="已发货" :value="2" />
                <el-option label="已完成" :value="3" />
                <el-option label="已取消" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="金额范围" label-width="60px">
              <div style="display:flex;gap:4px;align-items:center;">
                <el-input-number v-model="searchForm.minAmount" :min="0" :precision="2" placeholder="最低" style="width:100px" />
                <span>~</span>
                <el-input-number v-model="searchForm.maxAmount" :min="0" :precision="2" placeholder="最高" style="width:100px" />
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="下单时间" label-width="60px">
              <el-date-picker
                v-model="searchForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="16" :lg="18">
            <el-form-item label-width="0">
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="resetSearch">重置</el-button>
              <el-button type="info" plain @click="loadOrders">刷新</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- ===== 订单表格 ===== -->
    <el-card>
      <el-table :data="orderList" stripe v-loading="loading" border>
        <el-table-column prop="id" label="订单号" width="200" fixed />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column label="收货信息" min-width="180">
          <template #default="{ row }">
            <div>{{ row.receiverName }}</div>
            <div style="font-size:12px;color:#909399;">{{ row.receiverPhone }}</div>
            <div style="font-size:12px;color:#909399;">{{ row.receiverAddress }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="120">
          <template #default="{ row }">
            <span style="color:#F56C6C;font-weight:bold;">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="支付时间" width="170">
          <template #default="{ row }">{{ formatTime(row.payTime) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(String(row.id))">详情</el-button>
            <el-button v-if="row.status === 1" type="primary" size="small" @click="shipOrder(String(row.id))">发货</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="searchForm.page"
        v-model:page-size="searchForm.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadOrders"
        @current-change="loadOrders"
      />
    </el-card>

    <!-- ===== 订单详情弹窗 ===== -->
    <el-dialog v-model="detailVisible" title="订单详情" width="700px">
      <div v-if="currentOrder" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentOrder.id }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ currentOrder.userId }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusTagType(currentOrder.status)">
              {{ getStatusText(currentOrder.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总金额">
            <span style="color:#F56C6C;font-weight:bold;">¥{{ currentOrder.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="收货人">{{ currentOrder.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentOrder.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ currentOrder.receiverAddress }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatTime(currentOrder.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ formatTime(currentOrder.payTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ formatTime(currentOrder.shipTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatTime(currentOrder.finishTime) || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin:20px 0 12px;">商品明细</h4>
        <el-table :data="orderItems" stripe size="small">
          <el-table-column prop="bookTitle" label="商品名称" min-width="180" />
          <el-table-column prop="bookId" label="图书ID" width="100" />
          <el-table-column prop="price" label="单价" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="120">
            <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
//import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
// ===== 导出 Excel（使用原生 axios 绕过拦截器） =====
import axios from 'axios'

//const router = useRouter()

// ===== 状态 =====
const orderList = ref([])
const total = ref(0)
const loading = ref(false)
const exportLoading = ref(false)
const detailVisible = ref(false)
const currentOrder = ref(null)
const orderItems = ref([])
const searchFormRef = ref()

const searchForm = reactive({
  page: 1,
  size: 10,
  orderId: '',
  userId: '',
  status: null,
  minAmount: null,
  maxAmount: null,
  dateRange: []
})

// ===== 订单状态映射 =====
const getStatusText = (status) => {
  const map = {
    0: '待付款', 1: '已支付', 2: '已发货', 3: '已完成',
    4: '已取消', 5: '售后中', 6: '已退款', 7: '已关闭'
  }
  return map[status] || '未知状态'
}

const getStatusTagType = (status) => {
  const map = {
    0: 'warning', 1: 'primary', 2: 'success', 3: 'success',
    4: 'info', 5: 'danger', 6: 'warning', 7: 'info'
  }
  return map[status] || 'info'
}

// ===== 格式化时间 =====
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
}

// ===== 构建请求参数 =====
const buildParams = () => {
  const params = {
    page: searchForm.page,
    size: searchForm.size
  }
  if (searchForm.orderId) params.orderId = searchForm.orderId
  if (searchForm.userId) params.userId = searchForm.userId
  if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
  if (searchForm.minAmount !== null) params.minAmount = searchForm.minAmount
  if (searchForm.maxAmount !== null) params.maxAmount = searchForm.maxAmount
  if (searchForm.dateRange && searchForm.dateRange.length === 2) {
    params.startTime = searchForm.dateRange[0]
    params.endTime = searchForm.dateRange[1]
  }
  return params
}

// ===== 加载订单 =====
const loadOrders = async () => {
  loading.value = true
  try {
    const params = buildParams()
    const res = await request.get('/admin/order/page', { params })
    orderList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

// ===== 搜索 =====
const handleSearch = () => {
  searchForm.page = 1
  loadOrders()
}

// ===== 重置 =====
const resetSearch = () => {
  searchForm.orderId = ''
  searchForm.userId = ''
  searchForm.status = null
  searchForm.minAmount = null
  searchForm.maxAmount = null
  searchForm.dateRange = []
  searchForm.page = 1
  loadOrders()
}

// ===== 导出 Excel =====
const exportOrders = async () => {
  exportLoading.value = true
  try {
    const token = localStorage.getItem('token')
    const params = buildParams()
    delete params.page
    delete params.size

    // 使用原生 axios，不经过 request.js 拦截器
    const response = await axios.get('/admin/order/export', {
      params,
      headers: {
        'Authorization': `Bearer ${token}`
      },
      responseType: 'blob'
    })

    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `订单数据_${new Date().toISOString().slice(0,10)}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

// ===== 查看详情 =====
const viewDetail = async (orderId) => {
  try {
    const res = await request.get(`/api/order/${orderId}`)
    currentOrder.value = res.data
    orderItems.value = res.data.orderItems || []
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取订单详情失败')
  }
}

// ===== 发货 =====
const shipOrder = async (orderId) => {
  try {
    await ElMessageBox.confirm('确认发货该订单吗？', '发货确认', { type: 'info' })
    await request.put(`/admin/order/ship/${orderId}`)
    ElMessage.success('发货成功')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '发货失败')
    }
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.admin-order-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  color: #303133;
}
.header-actions {
  display: flex;
  gap: 12px;
}

.filter-card {
  margin-bottom: 20px;
}
.filter-card :deep(.el-form-item) {
  margin-bottom: 8px;
}
.filter-card :deep(.el-form-item__label) {
  font-size: 13px;
}
.filter-card :deep(.el-col) {
  min-width: 200px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.order-detail {
  padding: 4px 0;
}
</style>
