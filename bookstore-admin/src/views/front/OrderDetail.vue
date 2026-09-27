<template>
  <div class="order-detail-page" v-loading="loading">
    <!-- ===== 顶部导航条 ===== -->
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
        ← 返回
      </button>
      <h2 class="page-title">订单详情</h2>
      <div class="header-right">
        <span class="order-id-label">订单号：</span>
        <span class="order-id-value">{{ order?.id }}</span>
        <button class="copy-btn" @click="copyOrderId" v-if="order?.id">复制</button>
      </div>
    </div>

    <template v-if="order">
      <!-- ===== 1. 状态进度卡 ===== -->
      <div class="status-card">
        <div class="status-banner" :style="{ background: getStatusGradient(order.status) }">
          <div class="status-info">
            <span class="status-big-icon">{{ getStatusIcon(order.status) }}</span>
            <div class="status-text-group">
              <div class="status-title">{{ getStatusText(order.status) }}</div>
              <div class="status-hint" v-if="getStatusHint(order.status)">
                {{ getStatusHint(order.status) }}
              </div>
            </div>
          </div>
          <div class="status-timer" v-if="order.status === 0">
            <div class="timer-label">剩余支付时间</div>
            <div class="timer-value countdown">{{ countdown }}</div>
          </div>
        </div>

        <!-- 步骤时间线 -->
        <el-steps :active="getStepActive(order.status)" align-center class="order-steps" finish-status="success">
          <el-step title="提交订单" :description="formatTime(order.createTime)" />
          <el-step title="付款成功" :description="formatTime(order.payTime)" />
          <el-step title="商品发货" :description="formatTime(order.shipTime)" />
          <el-step title="交易完成" :description="formatTime(order.finishTime)" />
        </el-steps>
      </div>

      <!-- ===== 2. 收货信息卡 ===== -->
      <div class="section-card address-card">
        <div class="card-header">
          <span class="card-icon">📍</span>
          <span class="card-title">收货信息</span>
          <span class="card-sub" @click="goToShop(order.shopId)" v-if="order.shopId" style="margin-left:auto; cursor:pointer;">
            🏪 {{ order.shopName || '官方自营' }}
          </span>
        </div>
        <div class="address-content">
          <div class="address-user">
            <span class="user-name">{{ order.receiverName }}</span>
            <span class="user-phone">{{ order.receiverPhone }}</span>
          </div>
          <div class="address-detail">
            <span class="address-icon">📬</span>
            <span>{{ order.receiverAddress }}</span>
          </div>
        </div>
      </div>

      <!-- ===== 3. 商品明细卡 ===== -->
      <div class="section-card">
        <div class="card-header">
          <span class="card-icon">📦</span>
          <span class="card-title">商品明细</span>
          <span class="card-sub" style="margin-left:auto;">
            共 {{ orderItems.length }} 件商品，合计 ¥{{ order.totalAmount }}
          </span>
        </div>

        <div class="items-grid">
          <div
            v-for="item in orderItems"
            :key="item.id"
            class="item-card"
            @click="goToBookDetail(item.bookId)"
          >
            <el-image
              :src="getFullUrl(item.bookCover)"
              fit="cover"
              class="item-cover"
            >
              <template #error>
                <div class="cover-fallback">📖</div>
              </template>
            </el-image>
            <div class="item-info">
              <div class="item-title">{{ item.bookTitle }}</div>
              <div class="item-bottom">
                <span class="item-price">¥{{ item.price }}</span>
                <span class="item-qty">×{{ item.quantity }}</span>
              </div>
            </div>
            <div class="item-subtotal">
              <span class="subtotal-label">小计</span>
              <span class="subtotal-value">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
            </div>
          </div>
        </div>

        <!-- 金额汇总 -->
        <div class="amount-block">
          <div class="amount-line">
            <span>商品总额</span>
            <span>¥{{ order.totalAmount }}</span>
          </div>
          <div class="amount-line">
            <span>运费</span>
            <span class="free-tag">免运费</span>
          </div>
          <div class="amount-line grand-total">
            <span>实付金额</span>
            <span class="grand-total-value">¥{{ order.totalAmount }}</span>
          </div>
        </div>
      </div>

      <!-- ===== 4. 操作栏 ===== -->
      <div class="section-card action-card">
        <div class="action-buttons">
          <!-- 待付款：支付 + 取消 -->
          <template v-if="order.status === 0">
            <el-button type="primary" size="large" class="btn-primary" @click="openPaymentDialog(String(order.id))">
              💳 立即支付
            </el-button>
            <el-button size="large" class="btn-danger-outline" @click="cancelOrder(String(order.id))">
              取消订单
            </el-button>
          </template>

          <!-- 已发货：确认收货 + 售后 -->
          <template v-else-if="order.status === 2">
            <el-button type="success" size="large" class="btn-primary" @click="confirmFinish(String(order.id))">
              ✅ 确认收货
            </el-button>
          </template>

          <!-- 联系卖家 -->
          <el-button
            v-if="order.shopId && order.status !== 4 && order.status !== 6"
            size="large"
            plain
            @click="contactSellerHandler"
          >
            💬 联系卖家
          </el-button>

          <!-- 申请售后 -->
          <el-button
            v-if="canApplyAfterSale"
            type="warning"
            size="large"
            plain
            @click="openAfterSaleDialog"
          >
            🔧 申请售后
          </el-button>

          <!-- 继续购物 -->
          <el-button
            v-if="[1, 2, 3].includes(order.status)"
            size="large"
            plain
            @click="$router.push('/home')"
          >
            🛒 继续购物
          </el-button>
        </div>
      </div>

      <!-- ===== 5. 相关推荐（猜你喜欢） ===== -->
      <section v-if="recommendBooks.length > 0" class="recommend-section">
        <div class="recommend-header">
          <span class="recommend-title">✨ 猜你喜欢</span>
          <span class="recommend-sub">根据你购买的商品推荐</span>
        </div>
        <div class="recommend-grid">
          <div
            v-for="bk in recommendBooks"
            :key="bk.id"
            class="recommend-card"
            @click="goToBookDetail(bk.id)"
          >
            <el-image :src="getFullUrl(bk.coverUrl)" fit="cover" class="recommend-cover">
              <template #error>
                <div class="cover-fallback">📖</div>
              </template>
            </el-image>
            <div class="recommend-info">
              <div class="recommend-title-line" :title="bk.title">{{ bk.title }}</div>
              <div class="recommend-author" v-if="bk.author">{{ bk.author }}</div>
              <div class="recommend-meta">
                <span class="recommend-price">¥{{ bk.price }}</span>
                <span class="recommend-sales">已售 {{ bk.sales || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>

    <!-- ===== 订单不存在 ===== -->
    <el-empty v-else-if="!loading" description="订单不存在或已被删除">
      <el-button type="primary" @click="$router.push('/orders')">返回订单列表</el-button>
    </el-empty>

    <!-- ===== 支付弹窗 ===== -->
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
        <span class="pay-amount-value">¥{{ order ? order.totalAmount : 0 }}</span>
      </div>

      <template #footer>
        <el-button @click="paymentDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmPayment"
          :loading="paymentLoading"
          :disabled="!selectedPayment"
        >
          确认支付
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 售后申请弹窗 ===== -->
    <el-dialog v-model="afterSaleDialogVisible" title="🔧 申请售后" width="500px">
      <el-form :model="afterSaleForm" ref="afterSaleFormRef" label-width="80px">
        <el-form-item label="售后类型">
          <el-radio-group v-model="afterSaleForm.type">
            <el-radio :value="1">仅退款</el-radio>
            <el-radio :value="2">退货退款</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input-number v-model="afterSaleForm.amount" :min="0.01" :max="order.totalAmount" :precision="2" />
          <span style="margin-left:10px;color:#909399;">（最多 ¥{{ order.totalAmount }}）</span>
        </el-form-item>
        <el-form-item label="申请原因">
          <el-select v-model="afterSaleForm.reason" placeholder="请选择原因" style="width:100%">
            <el-option label="质量问题" value="质量问题" />
            <el-option label="发错货" value="发错货" />
            <el-option label="商品与描述不符" value="商品与描述不符" />
            <el-option label="物流问题" value="物流问题" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input v-model="afterSaleForm.description" type="textarea" :rows="3" placeholder="请详细描述您遇到的问题" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="afterSaleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAfterSale" :loading="afterSaleLoading">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFullUrl } from '../../utils/image'
import { contactSeller } from '../../utils/chat'

const router = useRouter()
const route = useRoute()

// ===== 状态 =====
const order = ref(null)
const orderItems = ref([])
const loading = ref(false)
const recommendBooks = ref([])

// ===== 支付状态 =====
const paymentDialogVisible = ref(false)
const paymentLoading = ref(false)
const selectedPayment = ref('wechat')
const currentOrderId = ref(null)

// ===== 售后相关 =====
const afterSaleDialogVisible = ref(false)
const afterSaleLoading = ref(false)
const afterSaleFormRef = ref()
const afterSaleForm = reactive({
  type: 1,
  reason: '',
  description: '',
  amount: 0
})

// ===== 支付方式 =====
const paymentMethods = [
  { value: 'wechat', label: '微信支付', desc: '推荐使用，扫码支付', icon: '📱' },
  { value: 'alipay', label: '支付宝', desc: '支持花呗付款', icon: '💰' },
  { value: 'bankcard', label: '银行卡支付', desc: '支持储蓄卡/信用卡', icon: '💳' }
]

// ===== 倒计时（待付款，基于创建时间 + 30 分钟） =====
const countdown = ref('')
let countdownTimer = null

const startCountdown = (createTimeStr) => {
  if (countdownTimer) clearInterval(countdownTimer)
  // 基于订单创建时间 + 30 分钟，而不是当前时间 + 30 分钟
  const createTime = createTimeStr ? new Date(createTimeStr).getTime() : Date.now()
  const end = createTime + 30 * 60 * 1000
  const update = () => {
    const diff = Math.max(0, end - Date.now())
    const m = String(Math.floor(diff / 60000)).padStart(2, '0')
    const s = String(Math.floor((diff % 60000) / 1000)).padStart(2, '0')
    countdown.value = `${m}:${s}`
    if (diff === 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }
  update()
  countdownTimer = setInterval(update, 1000)
}

// ===== 联系卖家 =====
const contactSellerHandler = async () => {
  const shopName = order.value.shopName || '商家'
  await contactSeller(order.value.shopId, shopName)
}

// ===== 跳转 =====
const goToShop = (shopId) => {
  if (shopId) router.push(`/shop/${shopId}`)
}

const goToBookDetail = (bookId) => {
  if (bookId) router.push(`/book/${bookId}`)
}

const copyOrderId = () => {
  if (!order.value?.id) return
  navigator.clipboard.writeText(String(order.value.id)).then(() => {
    ElMessage.success('订单号已复制')
  })
}

// ===== 是否可申请售后 =====
const canApplyAfterSale = computed(() => {
  if (!order.value) return false
  const status = order.value.status
  if (![1, 2, 3, 7].includes(status)) return false
  if ((status === 3 || status === 7) && order.value.finishTime) {
    const finish = new Date(order.value.finishTime)
    const now = new Date()
    const diffDays = (now - finish) / (1000 * 60 * 60 * 24)
    if (diffDays > 15) return false
  }
  return true
})

// ===== 订单状态映射 =====
const getStatusText = (status) => {
  const map = {
    0: '待付款', 1: '已支付', 2: '已发货', 3: '已完成',
    4: '已取消', 5: '售后中', 6: '已退款', 7: '已关闭'
  }
  return map[status] || '未知状态'
}

const getStatusHint = (status) => {
  const map = {
    0: '请尽快完成支付，超时订单将自动取消',
    1: '商品即将为你发出',
    2: '商品已发出，请注意查收',
    3: '交易已完成，感谢您的购买 🎉',
    4: '订单已取消',
    5: '售后处理中，请耐心等待商家审核',
    6: '已退款，款项将原路返回',
    7: '售后已驳回，可重新申请'
  }
  return map[status] || ''
}

const getStatusIcon = (status) => {
  const map = {
    0: '⏳', 1: '✅', 2: '🚚', 3: '🎉',
    4: '❌', 5: '🔄', 6: '💰', 7: '🔒'
  }
  return map[status] || '📦'
}

const getStatusGradient = (status) => {
  const map = {
    0: 'linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%)',
    1: 'linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%)',
    2: 'linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%)',
    3: 'linear-gradient(135deg, #f6ffed 0%, #b7eb8f 100%)',
    4: 'linear-gradient(135deg, #f5f5f5 0%, #d9d9d9 100%)',
    5: 'linear-gradient(135deg, #fff1f0 0%, #ffa39e 100%)',
    6: 'linear-gradient(135deg, #f6ffed 0%, #95de64 100%)',
    7: 'linear-gradient(135deg, #f5f5f5 0%, #bfbfbf 100%)'
  }
  return map[status] || '#f5f5f5'
}

// el-steps active（0-based）
const getStepActive = (status) => {
  const map = {
    0: 0,  // 待付款 → 提交订单
    1: 1,  // 已支付 → 付款成功
    2: 2,  // 已发货 → 商品发货
    3: 3,  // 已完成 → 交易完成
    4: 0,  // 已取消 → 停在提交
    5: 2,  // 售后中 → 如果已发货则在发货步骤
    6: 1,  // 已退款 → 停在付款（未发货的退款）
    7: 0   // 已关闭 → 停在提交
  }
  return map[status] ?? 0
}

// ===== 格式化时间 =====
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
}

// ===== 加载订单详情 =====
const loadOrderDetail = async () => {
  const orderId = route.params.orderId
  if (!orderId) {
    ElMessage.error('订单号不存在')
    return
  }
  loading.value = true
  try {
    const res = await request.get(`/api/order/${orderId}`)
    order.value = res.data
    orderItems.value = res.data.orderItems || []

    // 待付款开启倒计时（基于创建时间）
    if (order.value.status === 0) {
      startCountdown(order.value.createTime)
    } else if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }

    // 加载相关推荐（用第一件商品排除）
    loadRecommend()
  } catch (error) {
    ElMessage.error('加载订单详情失败')
    router.push('/orders')
  } finally {
    loading.value = false
  }
}

// ===== 加载相关推荐 =====
const loadRecommend = async () => {
  if (orderItems.value.length === 0) return
  try {
    // 取第一件商品的 bookId 作为排除项
    const firstBookId = orderItems.value[0].bookId
    const params = { excludeBookId: firstBookId, limit: 6 }
    const res = await request.get('/api/books/recommend', { params })
    recommendBooks.value = res.data || []
  } catch (e) {
    recommendBooks.value = []
  }
}

// ===== 支付 =====
const openPaymentDialog = (orderId) => {
  if (!order.value) { ElMessage.error('订单信息不存在'); return }
  currentOrderId.value = orderId
  selectedPayment.value = 'wechat'
  paymentDialogVisible.value = true
}

const resetPayment = () => {
  selectedPayment.value = 'wechat'
  paymentLoading.value = false
  currentOrderId.value = null
}

const confirmPayment = async () => {
  if (!selectedPayment.value) { ElMessage.warning('请选择支付方式'); return }
  if (!currentOrderId.value) { ElMessage.error('订单信息异常'); return }
  paymentLoading.value = true
  try {
    await request.post('/api/order/pay/' + currentOrderId.value)
    ElMessage.success('✅ 支付成功！')
    paymentDialogVisible.value = false
    await loadOrderDetail()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '支付失败，请稍后重试')
  } finally {
    paymentLoading.value = false
  }
}

// ===== 取消订单 =====
const cancelOrder = async (orderId) => {
  try {
    await ElMessageBox.confirm('确认取消该订单吗？', '取消确认', { type: 'warning' })
    await request.put('/api/order/cancel/' + orderId)
    ElMessage.success('订单已取消')
    loadOrderDetail()
  } catch (error) {
    if (error !== 'cancel') { ElMessage.error(error.response?.data?.msg || '取消失败') }
  }
}

// ===== 确认收货 =====
const confirmFinish = async (orderId) => {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '确认收货', { type: 'info' })
    await request.put('/api/order/finish/' + orderId)
    ElMessage.success('确认收货成功')
    loadOrderDetail()
  } catch (error) {
    if (error !== 'cancel') { ElMessage.error(error.response?.data?.msg || '操作失败') }
  }
}

// ===== 售后申请 =====
const openAfterSaleDialog = () => {
  afterSaleForm.type = 1
  afterSaleForm.reason = ''
  afterSaleForm.description = ''
  afterSaleForm.amount = order.value.totalAmount
  afterSaleDialogVisible.value = true
}

const submitAfterSale = async () => {
  if (!afterSaleForm.reason.trim()) {
    ElMessage.warning('请选择或填写申请原因')
    return
  }
  afterSaleLoading.value = true
  try {
    await request.post('/api/after-sale/apply', {
      orderId: String(order.value.id),
      type: afterSaleForm.type,
      reason: afterSaleForm.reason,
      description: afterSaleForm.description,
      amount: afterSaleForm.amount
    })
    ElMessage.success('售后申请已提交，请等待商家审核')
    afterSaleDialogVisible.value = false
    await loadOrderDetail()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '申请失败')
  } finally {
    afterSaleLoading.value = false
  }
}

// ===== 返回逻辑 =====
const goBack = () => {
  const from = route.query.from
  if (from) {
    router.replace(from)
  } else if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/orders')
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadOrderDetail()
})
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<style scoped>
/* ===== 页面容器 ===== */
.order-detail-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px 24px 40px;
}

/* ===== 顶部导航条 ===== */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 20px;
  background: #fff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.back-btn:hover {
  border-color: #409eff;
  color: #409eff;
}
.back-icon { font-size: 16px; }
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #909399;
}
.order-id-value {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  color: #606266;
  letter-spacing: 0.5px;
}
.copy-btn {
  background: none;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
  transition: all 0.2s;
}
.copy-btn:hover {
  background: #ecf5ff;
  border-color: #409eff;
}

/* ===== 通用卡片 ===== */
.section-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04), 0 2px 12px rgba(0, 0, 0, 0.04);
  padding: 20px 24px;
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #ebeef5;
}
.card-icon {
  font-size: 18px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.card-sub {
  font-size: 13px;
  color: #909399;
}

/* ===== 状态进度卡 ===== */
.status-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04), 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  margin-bottom: 16px;
}
.status-banner {
  display: flex;
  align-items: center;
  padding: 24px 28px;
  gap: 20px;
}
.status-big-icon {
  font-size: 48px;
  line-height: 1;
}
.status-text-group { flex: 1; }
.status-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}
.status-hint {
  font-size: 14px;
  color: #606266;
  opacity: 0.9;
}
.status-timer {
  text-align: center;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
}
.timer-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.timer-value {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}
.order-steps {
  padding: 24px 32px 16px;
}
:deep(.el-step__title) {
  font-size: 13px;
}
:deep(.el-step__description) {
  font-size: 12px;
  color: #c0c4cc;
}

/* ===== 收货信息卡 ===== */
.address-card { padding: 18px 24px; }
.address-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.address-user {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.user-phone {
  font-size: 14px;
  color: #606266;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}
.address-detail {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}
.address-icon { flex-shrink: 0; }

/* ===== 商品卡片网格 ===== */
.items-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.item-card {
  display: flex;
  align-items: stretch;
  gap: 16px;
  padding: 12px;
  background: #fafbfc;
  border-radius: 10px;
  transition: all 0.2s;
  cursor: pointer;
}
.item-card:hover {
  background: #f0f7ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}
.item-cover {
  width: 72px;
  height: 96px;
  border-radius: 6px;
  flex-shrink: 0;
  background: #f0f2f5;
}
.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: #f0f2f5;
  border-radius: 6px;
}
.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}
.item-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.item-bottom {
  display: flex;
  align-items: center;
  gap: 12px;
}
.item-price {
  color: #f56c6c;
  font-weight: 600;
  font-size: 15px;
}
.item-qty {
  color: #909399;
  font-size: 13px;
}
.item-subtotal {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  padding-left: 16px;
  border-left: 1px dashed #e4e7ed;
  flex-shrink: 0;
}
.subtotal-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}
.subtotal-value {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

/* ===== 金额汇总 ===== */
.amount-block {
  margin-top: 16px;
  padding: 16px 20px;
  background: #fafbfc;
  border-radius: 8px;
}
.amount-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #606266;
  padding: 4px 0;
}
.amount-line.grand-total {
  border-top: 1px dashed #e4e7ed;
  margin-top: 8px;
  padding-top: 12px;
  font-weight: 500;
  color: #303133;
}
.free-tag {
  color: #67c23a;
  font-weight: 500;
}
.grand-total-value {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
}

/* ===== 操作栏 ===== */
.action-card { padding: 16px 24px; }
.action-buttons {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
}
.btn-primary {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  border: none;
}
.btn-danger-outline {
  color: #f56c6c;
  border-color: #f56c6c;
}
.btn-danger-outline:hover {
  background: #fef0f0;
  color: #f56c6c;
}

/* ===== 相关推荐（与 BookDetail.vue 保持一致） ===== */
.recommend-section {
  margin-top: 24px;
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04), 0 2px 12px rgba(0, 0, 0, 0.04);
}
.recommend-header {
  margin-bottom: 20px;
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.recommend-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}
.recommend-sub {
  font-size: 13px;
  color: #999;
}
.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.recommend-card {
  cursor: pointer;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}
.recommend-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: #409eff;
}
.recommend-cover {
  width: 100%;
  aspect-ratio: 2 / 3;
  display: block;
  background: #f5f7fa;
}
.recommend-info {
  padding: 10px 12px 12px;
}
.recommend-title-line {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  height: 36px;
  margin-bottom: 4px;
}
.recommend-author {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recommend-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.recommend-price {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}
.recommend-sales {
  font-size: 12px;
  color: #909399;
}

/* ===== 支付弹窗 ===== */
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
.payment-method:hover { border-color: #409eff; background: #f5f9ff; }
.payment-method.active {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.15);
}
.payment-icon { font-size: 28px; line-height: 1; flex-shrink: 0; }
.payment-info { flex: 1; }
.payment-name { font-size: 16px; font-weight: 500; color: #303133; }
.payment-desc { font-size: 12px; color: #909399; margin-top: 2px; }
.payment-check { flex-shrink: 0; width: 24px; text-align: center; }
.check-mark { font-size: 20px; display: inline-block; }
.check-mark.empty { color: #c0c4cc; }
.check-mark:not(.empty) { color: #409eff; }
.payment-amount {
  text-align: right;
  padding: 12px 0 4px;
  border-top: 1px solid #f0f0f0;
  font-size: 16px;
  color: #606266;
}
.pay-amount-value {
  color: #f56c6c;
  font-size: 22px;
  font-weight: 700;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .order-detail-page { padding: 12px 12px 32px; }
  .status-banner { flex-direction: column; align-items: flex-start; padding: 20px; }
  .status-timer { align-self: flex-end; }
  .recommend-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .order-steps { padding: 16px 12px 12px; }
}
@media (max-width: 480px) {
  .action-buttons { flex-direction: column-reverse; }
  .action-buttons .el-button { width: 100%; }
}
</style>
