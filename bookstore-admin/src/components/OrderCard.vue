<template>
  <div class="order-card" @click="handleClick">
    <div class="order-card-header">
      <span class="order-card-id">订单 #{{ orderId }}</span>
      <el-tag :type="statusTagType(order?.status)" size="small" v-if="order">
        {{ statusText(order.status) }}
      </el-tag>
    </div>

    <!-- 加载中 -->
    <div class="order-card-body" v-if="loading">
      <el-skeleton animated :rows="2" />
    </div>

    <!-- 加载成功 -->
    <div class="order-card-body" v-else-if="order">
      <div class="order-card-items">
        <span>共 {{ orderItems.length }} 件商品</span>
        <span class="order-card-total">¥{{ order.totalAmount }}</span>
      </div>
      <div class="order-card-user">
        <span>收货人：{{ order.receiverName }}</span>
      </div>
    </div>

    <!-- 加载失败 -->
    <div class="order-card-body error" v-else>
      <el-icon><WarningFilled /></el-icon>
      <span>订单加载失败</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import request from '../utils/request'
import { getOrderApiPrefix } from '../utils/auth'

const props = defineProps({
  orderId: {
    type: [String, Number],
    required: true
  }
})

const emit = defineEmits(['click'])

const order = ref(null)
const orderItems = ref([])
const loading = ref(true)

// ===== 状态映射 =====
const statusText = (status) => {
  const map = ['待付款', '已支付', '已发货', '已完成', '已取消', '售后中', '已退款', '已关闭']
  return map[status] || '未知'
}

const statusTagType = (status) => {
  const map = ['warning', 'primary', 'success', 'success', 'info', 'danger', 'warning', 'info']
  return map[status] || 'info'
}

// ===== 加载订单（根据当前角色调不同接口） =====
const loadOrder = async () => {
  loading.value = true
  try {
    const orderIdStr = String(props.orderId)
    // === 阶段五 T1：改用 auth.js 统一角色判断，不再散写 localStorage 解析 ===
    const path = `${getOrderApiPrefix()}/${orderIdStr}`

    const res = await request.get(path)
    if (res.data) {
      order.value = res.data
      orderItems.value = res.data.orderItems || []
    } else {
      order.value = null
    }
  } catch (error) {
    order.value = null
  } finally {
    loading.value = false
  }
}

// ===== 点击事件 =====
const handleClick = () => {
  if (order.value) {
    emit('click')
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadOrder()
})
</script>

<style scoped>
.order-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px 14px;
  cursor: pointer;
  transition: background 0.2s;
  border: 1px solid #e4e7ed;
  min-width: 200px;
}
.order-card:hover {
  background: #ecf5ff;
  border-color: #409EFF;
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.order-card-id {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.order-card-body {
  font-size: 13px;
  color: #606266;
}
.order-card-body.error {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f56c6c;
  font-size: 13px;
  padding: 4px 0;
}

.order-card-items {
  display: flex;
  justify-content: space-between;
}
.order-card-total {
  font-weight: 500;
  color: #f56c6c;
}
.order-card-user {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
}
</style>
