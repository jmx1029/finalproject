<template>
  <div class="shop-after-sale">
    <div class="page-header">
      <h2>📋 售后管理</h2>
    </div>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width:140px">
            <el-option label="待审核" :value="0" />
            <el-option label="商家通过" :value="1" />
            <el-option label="已驳回" :value="2" />
            <el-option label="已退货(待仲裁)" :value="3" />
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

    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="售后单号" width="100" />
        <el-table-column prop="orderId" label="订单号" width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ row.type === 1 ? '仅退款' : '退货退款' }}</template>
        </el-table-column>
        <el-table-column label="申请金额" width="120">
          <template #default="{ row }"><span style="color:#F56C6C;">¥{{ row.amount }}</span></template>
        </el-table-column>
        <el-table-column label="原因" prop="reason" width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" prop="applyTime" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <template v-if="row.status === 0">
              <el-button size="small" type="success" @click="review(row.id, 1)">通过</el-button>
              <el-button size="small" type="danger" @click="review(row.id, 2)">驳回</el-button>
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
          <el-descriptions-item label="类型">{{ currentRow.type === 1 ? '仅退款' : '退货退款' }}</el-descriptions-item>
          <el-descriptions-item label="申请金额">¥{{ currentRow.amount }}</el-descriptions-item>
          <el-descriptions-item label="原因">{{ currentRow.reason }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ currentRow.description || '无' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(currentRow.status) }}</el-descriptions-item>
          <el-descriptions-item label="商家备注">{{ currentRow.shopReviewRemark || '无' }}</el-descriptions-item>
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
  status: null
})

const statusText = (s) => {
  const map = { 0: '待审核', 1: '商家通过', 2: '已驳回', 3: '已退货(待仲裁)', 4: '仲裁完成', 5: '已关闭' }
  return map[s] || '未知'
}
const statusTag = (s) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'danger', 3: 'info', 4: 'success', 5: 'info' }
  return map[s] || 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/shop/after-sale/page', { params: searchForm })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { ElMessage.error('加载失败') } finally { loading.value = false }
}

const resetSearch = () => {
  searchForm.status = null
  searchForm.page = 1
  loadData()
}

const viewDetail = (row) => {
  currentRow.value = row
  detailVisible.value = true
}

// 商家审核：status 1通过 2驳回
const review = async (id, status) => {
  const action = status === 1 ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(`确认${action}该售后申请吗？`, '提示', { type: 'warning' })
    await request.put(`/shop/after-sale/review/${id}?status=${status}`)
    ElMessage.success(`已${action}`)
    await loadData()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

onMounted(loadData)
</script>

<style scoped>
.shop-after-sale { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.search-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
