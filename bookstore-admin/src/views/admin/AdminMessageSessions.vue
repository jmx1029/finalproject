<template>
  <div class="admin-message-sessions-page">
    <div class="page-header">
      <h2>💬 客服消息</h2>
      <el-badge :value="totalUnread" :hidden="totalUnread === 0" type="danger">
        <span style="font-size:14px;color:#909399;">未读消息</span>
      </el-badge>
    </div>

    <el-card class="session-card" shadow="hover">
      <!-- ===== Tab 切换 ===== -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我的会话" name="mine" />
        <el-tab-pane label="全部会话" name="all" />
      </el-tabs>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 空状态 -->
      <div v-else-if="sessions.length === 0" class="empty-container">
        <el-empty :description="activeTab === 'mine' ? '暂无消息，用户/商家联系您后会在这里显示~' : '暂无任何会话记录'" />
      </div>

      <!-- 会话列表 -->
      <div v-else class="session-list">
        <div
          v-for="session in sessions"
          :key="session.sessionId"
          class="session-item"
          @click="enterChat(session)"
        >
          <div class="session-avatar">
            <el-avatar :size="48" :src="getFullUrl(session.targetAvatar)">
              {{ session.targetNickname?.charAt(0) || 'U' }}
            </el-avatar>
            <el-badge
              v-if="session.unreadCount > 0"
              :value="session.unreadCount"
              class="unread-badge"
              type="danger"
            />
          </div>

          <div class="session-info">
            <div class="session-header">
              <span class="session-name">{{ session.targetNickname || session.targetUsername }}</span>
              <span class="session-time">{{ formatTime(session.lastMessageTime) }}</span>
            </div>
            <div class="session-last">
              <span class="last-message">{{ session.lastMessage || '暂无消息' }}</span>
              <!-- 全部会话Tab显示参与者角色，我的会话Tab显示对方角色 -->
              <el-tag
                v-if="activeTab === 'all'"
                size="small"
                :type="getRoleTagType(session.targetRole)"
              >
                {{ session.targetRole }}
              </el-tag>
              <el-tag
                v-else
                size="small"
                :type="session.targetRole === 'admin' ? 'danger' : session.targetRole === 'shop' ? 'success' : 'info'"
              >
                {{ session.targetRole === 'admin' ? '平台' : session.targetRole === 'shop' ? '商家' : '用户' }}
              </el-tag>
            </div>
          </div>
          <div class="session-actions" @click.stop>
            <el-popconfirm title="删除后对方仍能看到历史记录，确认删除？" @confirm="deleteSession(session)">
              <template #reference>
                <el-button link type="danger" size="small" class="delete-btn">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>

      <el-pagination
        class="pagination"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadSessions"
        @current-change="loadSessions"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'

const router = useRouter()

// ===== 状态 =====
const loading = ref(false)
const sessions = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const totalUnread = ref(0)
const activeTab = ref('mine')

// ===== 格式化时间 =====
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
}

// ===== 角色标签颜色（全部会话） =====
const getRoleTagType = (role) => {
  if (role.includes('管理员')) return 'danger'
  if (role.includes('商家')) return 'success'
  return 'info'
}

// ===== 加载会话列表 =====
const loadSessions = async () => {
  loading.value = true
  try {
    const apiPath = activeTab.value === 'mine'
      ? '/admin/message/sessions'
      : '/admin/message/all-sessions'
    const res = await request.get(apiPath, {
      params: { page: page.value, size: size.value }
    })
    sessions.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (_error) {
    ElMessage.error('加载会话列表失败')
  } finally {
    loading.value = false
  }
}

// ===== Tab切换 =====
const handleTabChange = () => {
  page.value = 1
  loadSessions()
}

// ===== 获取未读总数 =====
const loadUnreadCount = async () => {
  try {
    const res = await request.get('/admin/message/unread-count')
    totalUnread.value = res.data || 0
  } catch (_error) {
    // 忽略
  }
}

// ===== 删除会话（我的会话 Tab 正常删除；全部会话 Tab 若管理员非参与方则后端拒绝） =====
const deleteSession = async (session) => {
  try {
    await request.delete(`/admin/message/session/${session.sessionId}`)
    ElMessage.success('已删除该会话')
    loadSessions()
    loadUnreadCount()
  } catch (e) {
    // 全部会话 Tab 下管理员可能非参与方，后端会抛无权删除
    if (activeTab.value === 'all') {
      ElMessage.warning('您不是该会话的参与方，无权删除')
    } else {
      ElMessage.error('删除失败')
    }
  }
}

// ===== 进入聊天（所有会话均可进入，并可发送消息） =====
const enterChat = (session) => {
  router.push({
    name: 'AdminMessageChat',
    params: { sessionId: session.sessionId },
    query: {
      targetUserId: session.targetUserId,
      targetName: session.targetNickname || session.targetUsername
    }
  })
}

// ===== 生命周期 =====
onMounted(() => {
  loadSessions()
  loadUnreadCount()
})
</script>

<style scoped>
.admin-message-sessions-page {
  max-width: 800px;
  margin: 0 auto;
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
}

.session-card {
  border-radius: 12px;
}

.session-list {
  display: flex;
  flex-direction: column;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}
.session-item:hover {
  background: #f5f7fa;
}
.session-item:last-child {
  border-bottom: none;
}

.session-avatar {
  position: relative;
  margin-right: 14px;
  flex-shrink: 0;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
}
.unread-badge :deep(.el-badge__content) {
  font-size: 11px;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
}

.session-info {
  flex: 1;
  min-width: 0;
}
.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.session-name {
  font-weight: 500;
  color: #303133;
  font-size: 15px;
}
.session-time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  margin-left: 12px;
}

.session-last {
  display: flex;
  align-items: center;
  gap: 8px;
}
.last-message {
  font-size: 14px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.loading-container {
  padding: 20px 0;
}
.empty-container {
  padding: 40px 0;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 会话删除按钮：hover 显示 */
.session-actions {
  flex-shrink: 0;
  margin-left: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}
.session-item:hover .session-actions {
  opacity: 1;
}
.delete-btn {
  font-size: 12px;
}
</style>
