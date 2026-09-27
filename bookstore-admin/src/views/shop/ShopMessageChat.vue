<template>
  <div class="shop-message-chat-page">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <el-button @click="$router.push('/shop/messages')" text>
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <span class="chat-title">{{ targetName || '聊天' }}</span>
      <span></span>
    </div>

    <!-- 消息列表 -->
    <div class="chat-messages" ref="messagesContainer" v-loading="loading">
      <div v-if="messages.length === 0 && !loading" class="empty-messages">
        <el-empty description="暂无消息，发送一条吧~" :image-size="80" />
      </div>

      <div v-for="msg in messages" :key="msg.id" class="message-item" :class="{ mine: msg.isMine }">
        <el-avatar :size="36" :src="getFullUrl(msg.senderAvatar)" class="message-avatar">
          {{ msg.senderName?.charAt(0) || 'U' }}
        </el-avatar>

        <div class="message-bubble-wrapper">
          <div class="message-sender">{{ msg.senderName }}</div>
          <div class="message-bubble" :class="{ mine: msg.isMine }">
            <span v-if="msg.messageType === 0" class="message-text">{{ msg.content }}</span>

            <OrderCard v-if="msg.messageType === 1" :order-id="String(msg.orderId)" @click="goOrderDetail(msg.orderId)" />

            <div v-if="msg.messageType === 2 && msg.attachments" class="message-images">
              <el-image
                v-for="(img, index) in msg.attachments"
                :key="index"
                :src="getFullUrl(img.fileUrl)"
                :preview-src-list="msg.attachments.map(a => getFullUrl(a.fileUrl))"
                fit="cover"
                class="message-image"
              />
            </div>

            <span class="message-time">{{ formatTime(msg.createTime) }}</span>
          </div>
        </div>
      </div>

      <div v-if="hasMore" class="load-more">
        <el-button text @click="loadMore">加载更多</el-button>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <div class="input-tools">
        <el-upload
          :action="uploadUrl + '?type=message'"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleImageUpload"
          :before-upload="beforeImageUpload"
          accept="image/*"
        >
          <el-button size="small" type="primary" plain>
            <el-icon><Picture /></el-icon> 图片
          </el-button>
        </el-upload>

        <el-button size="small" type="warning" plain @click="showOrderSelector = true">
          <el-icon><Document /></el-icon> 订单卡片
        </el-button>
      </div>

      <div class="input-row">
        <el-input
          v-model="inputContent"
          type="textarea"
          :rows="2"
          placeholder="输入消息..."
          resize="none"
          @keydown.ctrl.enter="sendMessage"
          @keydown.enter.prevent="sendMessage"
        />
        <el-button type="primary" @click="sendMessage" :loading="sending">发送</el-button>
      </div>
    </div>

    <!-- 订单选择弹窗 -->
    <el-dialog v-model="showOrderSelector" title="选择订单" width="520px">
      <el-input
        v-model="orderSearchKeyword"
        placeholder="🔍 输入订单号快速搜索"
        clearable
        style="margin-bottom:12px;"
      />
      <el-table :data="filteredOrders" stripe @row-click="selectOrder" max-height="360">
        <el-table-column prop="id" label="订单号" width="180" show-overflow-tooltip />
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="orderSearchKeyword = ''; showOrderSelector = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Picture, Document } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'
import OrderCard from '../../components/OrderCard.vue'

const route = useRoute()
const router = useRouter()

// ===== 参数 =====
const sessionId = route.params.sessionId
let targetUserId = route.query.targetUserId
const targetName = route.query.targetName || '对方'

// ===== 状态 =====
const loading = ref(false)
const messages = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const hasMore = ref(false)
const inputContent = ref('')
const sending = ref(false)
const showOrderSelector = ref(false)
const orderList = ref([])
const orderSearchKeyword = ref('')
const filteredOrders = computed(() => {
  const kw = orderSearchKeyword.value.trim()
  if (!kw) return orderList.value
  return orderList.value.filter(o => String(o.id).includes(kw))
})
const messagesContainer = ref(null)

// ===== 上传配置 =====
const uploadUrl = 'http://localhost:8080/api/file/upload'
const uploadHeaders = {
  Authorization: `Bearer ${localStorage.getItem('token')}`
}

// ===== 加载消息（商家端专用API） =====
const loadMessages = async (append = false) => {
  loading.value = true
  try {
    const res = await request.get(`/shop/message/list/${sessionId}`, {
      params: { page: page.value, size: size.value }
    })
    const newMessages = res.data.records || []
    total.value = res.data.total || 0
    hasMore.value = page.value * size.value < total.value

    if (append) {
      messages.value = [...newMessages, ...messages.value]
    } else {
      messages.value = newMessages
    }

    await nextTick()
    scrollToBottom()
  } catch (_error) {
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

// ===== 加载更多 =====
const loadMore = () => {
  page.value++
  loadMessages(true)
}

// ===== 发送消息 =====
const sendMessage = async () => {
  const content = inputContent.value.trim()
  if (!content) return

  sending.value = true
  try {
    await request.post('/shop/message/send', {
      receiverId: targetUserId,
      content: content,
      messageType: 0
    })
    inputContent.value = ''
    page.value = 1
    await loadMessages(false)
  } catch (_error) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

// ===== 发送图片 =====
const handleImageUpload = async (res) => {
  if (res.code === 200) {
    try {
      await request.post('/shop/message/send', {
        receiverId: targetUserId,
        content: '',
        messageType: 2,
        imageUrls: [res.data]
      })
      ElMessage.success('图片发送成功')
      page.value = 1
      await loadMessages(false)
    } catch (_error) {
      ElMessage.error('图片发送失败')
    }
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

const beforeImageUpload = (file) => {
  const isJPGorPNG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isJPGorPNG) ElMessage.error('仅支持 JPG/PNG 格式')
  if (!isLt5M) ElMessage.error('图片大小不能超过 5MB')
  return isJPGorPNG && isLt5M
}

// ===== 发送订单卡片 =====
const selectOrder = (row) => {
  showOrderSelector.value = false
  sendOrderCard(row.id)
}

const sendOrderCard = async (orderId) => {
  try {
    await request.post('/shop/message/send', {
      receiverId: targetUserId,
      content: '',
      messageType: 1,
      orderId: String(orderId)
    })
    ElMessage.success('订单卡片已发送')
    page.value = 1
    await loadMessages(false)
  } catch (_error) {
    ElMessage.error('发送失败')
  }
}

// ===== 加载可选订单（商家可查看的订单） =====
const loadOrders = async () => {
  try {
    if (!targetUserId) return
    const res = await request.get('/shop/order/chat-orders', {
      params: { receiverId: targetUserId }
    })
    orderList.value = res.data || []
  } catch (_error) {
    console.warn('加载聊天订单失败', _error)
    // 忽略
  }
}

// ===== 跳转订单详情 =====
const goOrderDetail = (orderId) => {
  router.push({
    name: 'ShopOrderDetail',
    params: { orderId },
    query: { from: route.fullPath }
  })
}

// ===== 滚动到底部 =====
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// ===== 格式化时间 =====
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
}

// ===== 状态映射（用于订单卡片） =====
const statusText = (status) => {
  const map = ['待付款', '已支付', '已发货', '已完成', '已取消', '售后中', '已退款', '已关闭']
  return map[status] || '未知'
}

const statusTagType = (status) => {
  const map = ['warning', 'primary', 'success', 'success', 'info', 'danger', 'warning', 'info']
  return map[status] || 'info'
}

// ===== 生命周期 =====
onMounted(async () => {
  if (!targetUserId) {
    try {
      const res = await request.get(`/shop/message/session/${sessionId}/receiver`)
      targetUserId = res.data
    } catch (e) {
      ElMessage.error('无法确定聊天对象')
      router.replace('/shop/messages')
      return
    }
  }
  loadMessages()
  loadOrders()
})

// 监听消息变化，滚动到底部
watch(messages, () => {
  nextTick(scrollToBottom)
}, { deep: true })
</script>

<style scoped>
.shop-message-chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  max-width: 900px;
  margin: 0 auto;
  background: #f5f7fa;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}
.chat-title {
  font-size: 18px;
  font-weight: 500;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.message-item.mine {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-bubble-wrapper {
  max-width: 70%;
}
.message-sender {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}
.message-item.mine .message-sender {
  text-align: right;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: relative;
}
.message-bubble.mine {
  background: #409EFF;
  color: #fff;
}
.message-bubble.mine .message-time {
  color: rgba(255, 255, 255, 0.7);
}

.message-text {
  word-break: break-word;
}
.message-time {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

.message-images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.message-image {
  width: 120px;
  height: 120px;
  border-radius: 6px;
  cursor: pointer;
  object-fit: cover;
}

.empty-messages {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.load-more {
  text-align: center;
  padding: 8px 0;
}

/* 输入区域 */
.chat-input-area {
  background: #fff;
  border-top: 1px solid #e4e7ed;
  padding: 12px 20px;
  flex-shrink: 0;
}

.input-tools {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}
.input-row .el-textarea {
  flex: 1;
}
.input-row .el-button {
  height: 48px;
  padding: 0 28px;
}
</style>
