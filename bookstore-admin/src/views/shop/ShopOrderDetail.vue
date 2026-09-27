<template>
  <div class="shop-order-detail" v-loading="loading">
    <!-- 顶部导航 -->
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="goBack">← 返回</el-button>
      <h2 class="page-title">订单详情</h2>
      <span class="order-id">订单号：{{ order?.id }}</span>
    </div>

    <template v-if="order">
      <!-- 1. 状态卡 + 进度条 -->
      <el-card class="status-card">
        <div class="status-row">
          <el-tag :type="statusTagType(order.status)" size="large" effect="dark">
            {{ statusText(order.status) }}
          </el-tag>

          <!-- 操作区：发货 / 处理退款 -->
          <div class="action-btns">
            <el-button
              v-if="order.status === 1"
              type="success"
              size="large"
              :loading="shipping"
              @click="handleShip"
            >🚚 确认发货</el-button>

            <el-button
              v-if="order.status === 5 && refundInfo"
              type="danger"
              size="large"
              :loading="reviewing"
              @click="showRefundDialog"
            >🔧 处理退款</el-button>
          </div>
        </div>

        <el-steps :active="stepActive(order.status)" align-center finish-status="success" class="order-steps">
          <el-step title="下单" :description="formatTime(order.createTime)" />
          <el-step title="已支付" :description="formatTime(order.payTime)" />
          <el-step title="已发货" :description="formatTime(order.shipTime)" />
          <el-step title="已完成" :description="formatTime(order.finishTime)" />
        </el-steps>
      </el-card>

      <!-- 售后信息（如果有） -->
      <el-card v-if="order.status === 5 || order.status === 6 || order.status === 7" class="section-card aftersale-card">
        <template #header>
          <span class="card-header">🔔 售后信息</span>
        </template>
        <div v-if="refundInfo" class="aftersale-info">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="售后类型">{{ refundTypeText(refundInfo.type) }}</el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ formatTime(refundInfo.applyTime) }}</el-descriptions-item>
            <el-descriptions-item label="申请原因" :span="2">{{ refundInfo.reason || '—' }}</el-descriptions-item>
            <el-descriptions-item label="详细描述" :span="2">{{ refundInfo.description || '—' }}</el-descriptions-item>
            <el-descriptions-item label="退款金额">
              <span style="color:#f56c6c;font-weight:bold;">¥{{ refundInfo.amount }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="售后状态">{{ afterSaleStatusText(refundInfo.status) }}</el-descriptions-item>
            <el-descriptions-item label="商家备注" v-if="refundInfo.shopReviewRemark" :span="2">{{ refundInfo.shopReviewRemark }}</el-descriptions-item>
            <el-descriptions-item label="仲裁备注" v-if="refundInfo.adminReviewRemark" :span="2">{{ refundInfo.adminReviewRemark }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>

      <!-- 2. 收货信息 -->
      <el-card class="section-card">
        <template #header>
          <span class="card-header">📍 收货信息</span>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="收货人">{{ order.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ order.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="1">{{ order.receiverAddress }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 3. 商品明细 -->
      <el-card class="section-card">
        <template #header>
          <span class="card-header">📦 商品明细（共 {{ orderItems.length }} 件）</span>
        </template>
        <div class="items-grid">
          <div
            v-for="item in orderItems"
            :key="item.id"
            class="item-card"
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
      </el-card>

      <!-- 4. 金额明细 -->
      <el-card class="section-card amount-card">
        <template #header>
          <span class="card-header">💰 金额明细</span>
        </template>
        <div class="amount-box">
          <div class="amount-row">
            <span class="amount-label">商品总额</span>
            <span class="amount-value">¥{{ order.totalAmount }}</span>
          </div>
          <div class="amount-row">
            <span class="amount-label">运费</span>
            <span class="amount-value">¥0.00</span>
          </div>
          <div class="amount-row total">
            <span class="amount-label">实付金额</span>
            <span class="amount-total">¥{{ order.totalAmount }}</span>
          </div>
        </div>
      </el-card>

      <!-- 5. 时间线 -->
      <el-card class="section-card">
        <template #header>
          <span class="card-header">🕐 时间记录</span>
        </template>
        <el-timeline>
          <el-timeline-item :timestamp="formatTime(order.createTime)" type="primary">
            买家创建订单
          </el-timeline-item>
          <el-timeline-item v-if="order.payTime" :timestamp="formatTime(order.payTime)" type="success">
            买家完成支付
          </el-timeline-item>
          <el-timeline-item v-if="order.shipTime" :timestamp="formatTime(order.shipTime)" type="warning">
            商家完成发货
          </el-timeline-item>
          <el-timeline-item v-if="order.finishTime" :timestamp="formatTime(order.finishTime)" type="success">
            订单已完成
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </template>

    <!-- 退款处理弹窗 -->
    <el-dialog v-model="refundDialogVisible" title="处理售后申请" width="480px">
      <div v-if="refundInfo" class="refund-info">
        <p><strong>售后类型：</strong>{{ refundTypeText(refundInfo.type) }}</p>
        <p><strong>申请金额：</strong><span style="color:#f56c6c;font-weight:bold;">¥{{ refundInfo.amount }}</span></p>
        <p><strong>申请原因：</strong>{{ refundInfo.reason }}</p>
        <p v-if="refundInfo.description"><strong>详细描述：</strong>{{ refundInfo.description }}</p>
      </div>
      <el-divider />
      <el-form :model="refundForm" ref="refundFormRef">
        <el-form-item label="处理结果">
          <el-radio-group v-model="refundForm.status">
            <el-radio :value="1">✅ 同意退款</el-radio>
            <el-radio :value="2">❌ 拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input v-model="refundForm.remark" type="textarea" :rows="3"
                    placeholder="请填写处理备注（拒绝时必填）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="submitRefund">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const shipping = ref(false)
const reviewing = ref(false)
const refundDialogVisible = ref(false)
const refundFormRef = ref()

const order = ref(null)
const orderItems = ref([])
const refundInfo = ref(null)

const refundForm = reactive({
  status: 1,
  remark: ''
})

// ===== 状态映射 =====
const statusText = (s) => ({
  0: '待付款', 1: '已支付', 2: '已发货', 3: '已完成',
  4: '已取消', 5: '售后中', 6: '已退款', 7: '已关闭'
}[s] || '未知')

const statusTagType = (s) => ({
  0: 'warning', 1: 'primary', 2: 'success', 3: 'success',
  4: 'info', 5: 'danger', 6: 'warning', 7: 'info'
}[s] || 'info')

const stepActive = (s) => {
  if (s <= 0) return 0
  if (s === 1 || s === 4) return 1
  if (s === 2 || s === 5) return 2
  return 3
}

const formatTime = (t) => t || '—'

const refundTypeText = (t) => ({ 1: '仅退款', 2: '退货退款' }[t] || '未知')
const afterSaleStatusText = (s) => ({
  0: '待商家审核', 1: '商家已同意', 2: '商家已驳回',
  3: '待仲裁', 4: '仲裁完成', 5: '已关闭'
}[s] || '未知')

// ===== 加载详情 =====
const loadDetail = async () => {
  const orderId = route.params.orderId
  if (!orderId) { ElMessage.error('订单ID不存在'); router.push('/shop/orders'); return }

  loading.value = true
  try {
    const res = await request.get(`/shop/order/${orderId}`)
    if (res.data) {
      order.value = res.data
      orderItems.value = res.data.orderItems || []
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '加载订单详情失败')
  } finally {
    loading.value = false
  }

  // 尝试查售后单（如果订单处于售后相关状态）
  if (order.value && [5, 6, 7].includes(order.value.status)) {
    try {
      const orderIdStr = String(order.value.id)
      // 查商家售后列表，再按 orderId 过滤出当前订单对应的售后单
      const afterRes = await request.get('/shop/after-sale/page', {
        params: { page: 1, size: 50 }
      })
      const records = afterRes.data?.records || []
      refundInfo.value = records.find(a => String(a.orderId) === orderIdStr) || null
    } catch (e) {
      // 静默忽略，主流程不受影响
    }
  }
}

// ===== 发货 =====
const handleShip = async () => {
  try {
    await ElMessageBox.confirm(
      '确认已发货？发货后订单状态将变为"已发货"，买家可确认收货。',
      '发货确认', { type: 'warning' }
    )
  } catch { return }

  shipping.value = true
  try {
    await request.put(`/shop/order/ship/${route.params.orderId}`)
    ElMessage.success('发货成功')
    await loadDetail()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '发货失败')
  } finally {
    shipping.value = false
  }
}

// ===== 处理退款 =====
const showRefundDialog = () => {
  if (!refundInfo.value) return
  refundForm.status = 1
  refundForm.remark = ''
  refundDialogVisible.value = true
}

const submitRefund = async () => {
  if (refundForm.status === 2 && !refundForm.remark.trim()) {
    ElMessage.warning('拒绝退款需要填写原因')
    return
  }

  reviewing.value = true
  try {
    await request.put(`/shop/order/refund-handle/${route.params.orderId}`, refundForm)
    ElMessage.success('处理成功')
    refundDialogVisible.value = false
    await loadDetail()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '操作失败')
  } finally {
    reviewing.value = false
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
    router.push('/shop/orders')
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.shop-order-detail {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.page-header .page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.order-id {
  margin-left: auto;
  color: #909399;
  font-size: 14px;
}

.section-card { margin-bottom: 16px; }
.section-card :deep(.el-card__header) { padding: 12px 20px; }
.card-header { font-size: 15px; font-weight: 600; color: #303133; }

/* 状态卡 */
.status-card { margin-bottom: 16px; }
.status-row {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 8px 0 16px;
}
.action-btns { margin-left: auto; }
.order-steps { margin-top: 16px; }

/* 金额卡 */
.amount-box { padding: 0 20px; }
.amount-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
.amount-row:last-child { border-bottom: none; }
.amount-row.total { padding-top: 12px; margin-top: 8px; border-top: 2px solid #ebeef5; }
.amount-label { color: #606266; }
.amount-value { color: #303133; }
.amount-total {
  font-size: 22px;
  font-weight: bold;
  color: #f56c6c;
}

/* ===== 商品明细卡片 ===== */
.items-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0 20px;
}
.item-card {
  display: flex;
  align-items: stretch;
  gap: 16px;
  padding: 12px;
  background: #fafbfc;
  border-radius: 10px;
  transition: all 0.2s;
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
  aspect-ratio: 2 / 3;
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
</style>
