<template>
  <div class="admin-order-detail" v-loading="loading">
    <!-- 顶部导航 -->
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="goBack">← 返回</el-button>
      <h2 class="page-title">订单详情</h2>
      <span class="order-id">订单号：{{ order?.id }}</span>
    </div>

    <template v-if="order">
      <!-- 1. 状态卡 + 进度条 + 强制关闭 -->
      <el-card class="status-card">
        <div class="status-row">
          <el-tag :type="statusTagType(order.status)" size="large" effect="dark">
            {{ statusText(order.status) }}
          </el-tag>

          <div class="action-btns">
            <el-button
              v-if="canForceClose"
              type="danger"
              size="large"
              :loading="closing"
              @click="showCloseDialog"
            >🔨 强制关闭订单</el-button>
          </div>
        </div>

        <el-steps :active="stepActive(order.status)" align-center finish-status="success" class="order-steps">
          <el-step title="下单" :description="formatTime(order.createTime)" />
          <el-step title="已支付" :description="formatTime(order.payTime)" />
          <el-step title="已发货" :description="formatTime(order.shipTime)" />
          <el-step title="已完成" :description="formatTime(order.finishTime)" />
        </el-steps>
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

      <!-- 5. 双方信息 -->
      <el-card class="section-card">
        <template #header>
          <span class="card-header">👥 相关方信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单ID">{{ order.id }}</el-descriptions-item>
          <el-descriptions-item label="店铺">{{ order.shopName || '未知' }}（ID: {{ order.shopId }}）</el-descriptions-item>
          <el-descriptions-item label="买家ID">{{ order.userId }}</el-descriptions-item>
          <el-descriptions-item label="买家昵称">—</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 6. 管理员备注（内部） -->
      <el-card class="section-card remark-card">
        <template #header>
          <span class="card-header">📝 客服备注（内部记录，买家不可见）</span>
          <el-button type="primary" size="small" style="margin-left:auto;" @click="showRemarkDialog">+ 添加备注</el-button>
        </template>

        <div class="remark-timeline" v-if="remarks.length > 0">
          <div v-for="rm in remarks" :key="rm.id" class="remark-item">
            <div class="remark-header">
              <span class="remark-admin">{{ rm.adminName || '管理员' }}</span>
              <span class="remark-time">{{ formatTime(rm.createTime) }}</span>
            </div>
            <div class="remark-content">{{ rm.content }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无备注" :image-size="60" />
      </el-card>

      <!-- 7. 时间线 -->
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

    <!-- 添加备注弹窗 -->
    <el-dialog v-model="remarkDialogVisible" title="添加客服备注" width="480px">
      <el-input
        v-model="remarkContent"
        type="textarea"
        :rows="4"
        placeholder="记录处理过程、沟通要点、特殊情况等..."
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="remarkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRemark">保存备注</el-button>
      </template>
    </el-dialog>

    <!-- 强制关闭弹窗 -->
    <el-dialog v-model="closeDialogVisible" title="强制关闭订单" width="480px">
      <el-alert type="warning" :closable="false" style="margin-bottom:12px;">
        强制关闭后订单将变为"已关闭"状态，买家可重新申请售后。此操作会自动记录备注。
      </el-alert>
      <el-input
        v-model="closeReason"
        type="textarea"
        :rows="3"
        placeholder="请填写关闭原因（必填）"
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="closeDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="closing" @click="submitClose">确认关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const closing = ref(false)
const remarkDialogVisible = ref(false)
const closeDialogVisible = ref(false)
const remarkContent = ref('')
const closeReason = ref('')

const order = ref(null)
const orderItems = ref([])
const remarks = ref([])

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

// 只有非终态可以强制关闭
const canForceClose = computed(() => {
  if (!order.value) return false
  return ![3, 4, 6].includes(order.value.status)
})

// ===== 加载详情 =====
const loadDetail = async () => {
  const orderId = route.params.orderId
  if (!orderId) { ElMessage.error('订单ID不存在'); router.push('/admin/orders'); return }

  loading.value = true
  try {
    const res = await request.get(`/admin/order/${orderId}`)
    if (res.data) {
      order.value = res.data
      orderItems.value = res.data.orderItems || []
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '加载订单详情失败')
  } finally {
    loading.value = false
  }

  // 加载备注
  try {
    const remarkRes = await request.get(`/admin/order/remark/list/${orderId}`)
    remarks.value = remarkRes.data || []
  } catch (e) { /* 静默忽略 */ }
}

// ===== 添加备注 =====
const showRemarkDialog = () => { remarkContent.value = ''; remarkDialogVisible.value = true }

const submitRemark = async () => {
  if (!remarkContent.value.trim()) { ElMessage.warning('备注内容不能为空'); return }

  submitting.value = true
  try {
    await request.post(`/admin/order/remark/${route.params.orderId}`, { content: remarkContent.value.trim() })
    ElMessage.success('备注已保存')
    remarkDialogVisible.value = false
    // 刷新备注列表
    const res = await request.get(`/admin/order/remark/list/${route.params.orderId}`)
    remarks.value = res.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '保存失败')
  } finally {
    submitting.value = false
  }
}

// ===== 强制关闭 =====
const showCloseDialog = () => { closeReason.value = ''; closeDialogVisible.value = true }

const submitClose = async () => {
  if (!closeReason.value.trim()) { ElMessage.warning('请填写关闭原因'); return }

  try {
    await ElMessageBox.confirm('确认强制关闭此订单？', '二次确认', { type: 'warning' })
  } catch { return }

  closing.value = true
  try {
    await request.put(`/admin/order/close/${route.params.orderId}`, { reason: closeReason.value.trim() })
    ElMessage.success('订单已关闭')
    closeDialogVisible.value = false
    await loadDetail()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '操作失败')
  } finally {
    closing.value = false
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
    router.push('/admin/orders')
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.admin-order-detail { padding: 20px; max-width: 1000px; margin: 0 auto; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.page-header .page-title { margin: 0; font-size: 20px; font-weight: 600; }
.order-id { margin-left: auto; color: #909399; font-size: 14px; }

.section-card { margin-bottom: 16px; }
.section-card :deep(.el-card__header) { padding: 12px 20px; }
.card-header { font-size: 15px; font-weight: 600; color: #303133; }
.status-card { margin-bottom: 16px; }
.status-row { display: flex; align-items: center; gap: 20px; padding: 8px 0 16px; }
.action-btns { margin-left: auto; }

/* 金额 */
.amount-box { padding: 0 20px; }
.amount-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px dashed #ebeef5; }
.amount-row:last-child { border-bottom: none; }
.amount-row.total { padding-top: 12px; margin-top: 8px; border-top: 2px solid #ebeef5; }
.amount-label { color: #606266; }
.amount-value { color: #303133; }
.amount-total { font-size: 22px; font-weight: bold; color: #f56c6c; }

/* 备注时间线 */
.remark-timeline { padding: 0 20px; }
.remark-item {
  padding: 12px 0;
  border-bottom: 1px dashed #ebeef5;
}
.remark-item:last-child { border-bottom: none; }
.remark-header { display: flex; gap: 12px; margin-bottom: 6px; }
.remark-admin { font-weight: 500; color: #409eff; }
.remark-time { color: #c0c4cc; font-size: 12px; }
.remark-content { color: #303133; font-size: 14px; line-height: 1.6; white-space: pre-wrap; }

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
