<template>
  <div class="shop-order-list">
    <div class="page-header">
      <h2>📦 订单管理</h2>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width:140px">
            <el-option label="待付款" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已发货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
            <!-- ===== 新增：售后相关状态 ===== -->
            <el-option label="售后中" :value="5" />
            <el-option label="已退款" :value="6" />
            <el-option label="已关闭" :value="7" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="订单号/收货人/手机号" clearable style="width:220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单列表 -->
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <el-table-column label="订单号" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="viewDetail(String(row.id))">{{ row.id }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="买家" min-width="100">
          <template #default="{ row }">
            <div>{{ row.receiverName }}</div>
            <div style="font-size:12px;color:#909399;">{{ row.receiverPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="收货地址" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.receiverAddress }}</template>
        </el-table-column>
        <el-table-column label="总金额" width="120" align="center">
          <template #default="{ row }">
            <span style="color:#F56C6C;font-weight:bold;">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170" align="center">
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <!-- ===== 关键修改：String(row.id) ===== -->
            <el-button size="small" type="primary" plain @click="viewDetail(String(row.id))">详情</el-button>
            <el-button v-if="row.status === 1" size="small" type="success" @click="handleShip(String(row.id))">发货</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import request from '../../utils/request'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const searchForm = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: null
})

// ===== 状态映射（完整支持 0-7） =====
const statusText = (s) => {
  const map = {
    0: '待付款', 1: '已支付', 2: '已发货', 3: '已完成',
    4: '已取消', 5: '售后中', 6: '已退款', 7: '已关闭'
  }
  return map[s] || '未知'
}

const statusTagType = (s) => {
  const map = {
    0: 'warning', 1: 'primary', 2: 'success', 3: 'success',
    4: 'info', 5: 'danger', 6: 'warning', 7: 'info'
  }
  return map[s] || 'info'
}

// ===== 加载订单列表 =====
const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/shop/order/page', { params: searchForm })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

// ===== 重置搜索 =====
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.status = null
  searchForm.page = 1
  loadData()
}

// ===== 查看订单详情（跳商家端详情页） =====
const viewDetail = (orderId) => {
  router.push({
    name: 'ShopOrderDetail',
    params: { orderId },
    query: { from: route.fullPath }
  })
}

// ===== 发货操作 =====
const handleShip = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定已发货吗？', '提示', { type: 'info' })
    await request.put(`/shop/order/ship/${orderId}`)
    ElMessage.success('发货成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '发货失败')
    }
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.shop-order-list { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.search-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
