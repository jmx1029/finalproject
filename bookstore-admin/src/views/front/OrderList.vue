<template>
  <div class="order-list-page">
    <el-card>
      <!-- Tab 切换 -->
      <el-tabs v-model="activeStatus" @tab-change="handleTabChange">
        <el-tab-pane label="全部" :name="''" />
        <el-tab-pane label="待付款" :name="'0'" />
        <el-tab-pane label="已支付" :name="'1'" />
        <el-tab-pane label="已发货" :name="'2'" />
        <el-tab-pane label="已完成" :name="'3'" />
        <el-tab-pane label="已取消" :name="'4'" />
        <el-tab-pane label="售后中" :name="'5'" />
        <el-tab-pane label="已退款" :name="'6'" />
        <el-tab-pane label="已关闭" :name="'7'" />
      </el-tabs>

      <!-- 订单列表 -->
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="订单号" min-width="160" show-overflow-tooltip />
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
        <el-table-column label="下单时间" prop="createTime" width="170" align="center" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="viewDetail(String(row.id))">详情</el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              type="success"
              @click="openPaymentDialog(row.id)"
            >支付</el-button>
            <el-button
              v-if="row.status === 3"
              size="small"
              type="warning"
              plain
              @click="goReview(row)"
            >去评价</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="searchForm.page"
        v-model:page-size="searchForm.size"
        :total="total"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- ===== 支付方式选择弹窗 ===== -->
    <el-dialog v-model="paymentDialogVisible" title="💳 选择支付方式" width="440px" @close="resetPayment">
      <div class="payment-methods">
        <div
          v-for="method in paymentMethods"
          :key="method.value"
          class="payment-method"
          :class="{ active: selectedPayment === method.value }"
          @click="selectedPayment = method.value"
        >
          <div class="payment-icon">{{ method.icon }}</div>
          <div class="payment-info">
            <div class="payment-name">{{ method.label }}</div>
            <div class="payment-desc">{{ method.desc }}</div>
          </div>
          <div class="payment-check">
            <span v-if="selectedPayment === method.value" class="check-mark">✔</span>
            <span v-else class="check-mark empty">○</span>
          </div>
        </div>
      </div>

      <div class="payment-amount">
        <span>应付金额：</span>
        <span style="color:#F56C6C;font-size:20px;font-weight:bold;">
          ¥{{ pendingOrder ? pendingOrder.totalAmount : 0 }}
        </span>
      </div>

      <template #footer>
        <el-button @click="paymentDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmPayment"
          :loading="paymentLoading"
          :disabled="!selectedPayment || !pendingOrder"
        >
          确认支付（{{ selectedPayment ? paymentMethods.find(m => m.value === selectedPayment)?.label || '未选择' : '未选择' }}）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const router = useRouter()
const route = useRoute()

// ===== 数据 =====
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const activeStatus = ref('')

const searchForm = reactive({
  page: 1,
  size: 10,
  status: null  // null表示全部
})

// ===== 支付弹窗相关 =====
const paymentDialogVisible = ref(false)
const paymentLoading = ref(false)
const selectedPayment = ref('wechat')
const pendingOrder = ref(null)  // 当前待支付的订单

// 支付方式列表
const paymentMethods = [
  { value: 'wechat', label: '微信支付', desc: '推荐使用，扫码支付', icon: '📱' },
  { value: 'alipay', label: '支付宝', desc: '支持花呗付款', icon: '💰' },
  { value: 'bankcard', label: '银行卡支付', desc: '支持储蓄卡/信用卡', icon: '💳' }
]

// ===== 状态映射（完整支持 0-7） =====
const statusText = (status) => {
  const map = {
    0: '待付款', 1: '已支付', 2: '已发货', 3: '已完成',
    4: '已取消', 5: '售后中', 6: '已退款', 7: '已关闭'
  }
  return map[status] || '未知'
}

const statusTagType = (status) => {
  const map = {
    0: 'warning', 1: 'primary', 2: 'success', 3: 'success',
    4: 'info', 5: 'danger', 6: 'warning', 7: 'info'
  }
  return map[status] || 'info'
}

// ===== Tab 切换 =====
const handleTabChange = (val) => {
  searchForm.status = val === '' ? null : parseInt(val)
  searchForm.page = 1
  loadData()
}

// ===== 加载订单列表 =====
const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/order/list', { params: searchForm })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

// ===== 查看详情 =====
const viewDetail = (orderId) => {
  router.push({
    path: `/orders/${String(orderId)}`,
    query: { from: route.fullPath }
  })
}

// ===== 去评价 =====
const goReview = (row) => {
  // 跳到订单详情页，用户在详情页能看到商品并跳转商品详情去评价
  router.push({
    path: `/orders/${String(row.id)}`,
    query: { from: route.fullPath }
  })
}

// ===== 打开支付弹窗（替代直接支付） =====
const openPaymentDialog = (orderId) => {
  // 从列表中找出对应的订单
  const order = tableData.value.find(item => String(item.id) === String(orderId))
  if (!order) {
    ElMessage.error('订单信息不存在')
    return
  }
  pendingOrder.value = order
  selectedPayment.value = 'wechat'
  paymentDialogVisible.value = true
}

// ===== 重置支付弹窗 =====
const resetPayment = () => {
  selectedPayment.value = 'wechat'
  paymentLoading.value = false
  pendingOrder.value = null
}

// ===== 确认支付 =====
const confirmPayment = async () => {
  if (!selectedPayment.value) {
    ElMessage.warning('请选择支付方式')
    return
  }
  if (!pendingOrder.value) {
    ElMessage.error('订单信息丢失，请重新选择')
    return
  }
  const method = paymentMethods.find(m => m.value === selectedPayment.value)
  const methodName = method ? method.label : selectedPayment.value

  paymentLoading.value = true
  try {
    await request.post(`/api/order/pay/${pendingOrder.value.id}`)
    ElMessage.success(`✅ 支付成功！使用 ${methodName} 完成支付`)
    paymentDialogVisible.value = false
    // 刷新列表
    await loadData()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '支付失败，请稍后重试')
  } finally {
    paymentLoading.value = false
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.order-list-page {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* ===== 支付弹窗样式 ===== */
.payment-methods {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.payment-method {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  border: 2px solid #e4e7ed;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
  gap: 14px;
  background: #fff;
}

.payment-method:hover {
  border-color: #409EFF;
  background: #f5f9ff;
}

.payment-method.active {
  border-color: #409EFF;
  background: #ecf5ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.15);
}

.payment-method .payment-icon {
  font-size: 28px;
  line-height: 1;
  flex-shrink: 0;
}

.payment-method .payment-info {
  flex: 1;
}

.payment-method .payment-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.payment-method .payment-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.payment-method .payment-check {
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}

.payment-method .check-mark {
  font-size: 20px;
  display: inline-block;
}

.payment-method .check-mark.empty {
  color: #c0c4cc;
}

.payment-method .check-mark:not(.empty) {
  color: #409EFF;
}

.payment-amount {
  text-align: right;
  padding: 12px 0 4px;
  border-top: 1px solid #f0f0f0;
  font-size: 16px;
  color: #606266;
}
</style>
