<template>
  <header class="app-navbar" :class="identityClass">
    <div class="navbar-container">
      <!-- 左侧：品牌 + 身份标识 -->
      <div class="navbar-left">
        <router-link to="/" class="brand">
          <span class="brand-name">📚 阅微书城</span>
          <span class="identity-badge" :style="{ backgroundColor: identityColor }">
            {{ identityLabel }}
          </span>
        </router-link>
      </div>

      <!-- 中间：动态导航 -->
      <nav class="navbar-center">
        <!-- ===== 买家导航 ===== -->
        <template v-if="currentIdentity === 'buyer'">
          <router-link to="/" class="nav-link" exact-active-class="active">首页</router-link>
          <router-link to="/category" class="nav-link" active-class="active">分类</router-link>
          <router-link to="/ranking" class="nav-link" active-class="active">排行</router-link>
          <router-link to="/orders" class="nav-link" active-class="active">我的订单</router-link>
          <router-link to="/cart" class="nav-link" active-class="active">购物车</router-link>
          <router-link to="/profile" class="nav-link" active-class="active">个人中心</router-link>
          <a href="javascript:void(0)" class="nav-link" @click="contactCustomerService">📞 联系客服</a>
          <!-- 申请成为商家 -->
          <router-link
            v-if="!isShopOwner"
            to="/apply-shop"
            class="nav-link"
            active-class="active"
          >🏪 申请成为商家</router-link>
          <span v-else-if="shopStatus === 1" class="status-pending">⏳ 商家审核中</span>
        </template>

        <!-- ===== 商家导航 ===== -->
        <!-- ===== 商家导航 ===== -->
        <template v-else-if="currentIdentity === 'shop'">
          <router-link to="/shop/books" class="nav-link" active-class="active">商品管理</router-link>
          <router-link to="/shop/orders" class="nav-link" active-class="active">订单管理</router-link>
          <router-link to="/shop/after-sale" class="nav-link" active-class="active">售后处理</router-link>
          <!-- ===== 新增：数据看板 ===== -->
          <router-link to="/shop/statistics" class="nav-link" active-class="active">📊 数据看板</router-link>
          <!-- ===== 新增：评价管理 ===== -->
          <router-link to="/shop/comments" class="nav-link" active-class="active">⭐ 评价管理</router-link>
          <router-link to="/shop/profile" class="nav-link" active-class="active">店铺设置</router-link>
          <!-- ===== 移除：店铺主页（已整合到店铺设置） ===== -->
          <!-- ===== 联系平台客服 ===== -->
          <a href="javascript:void(0)" class="nav-link" @click="contactPlatform">📞 联系平台客服</a>
        </template>

        <!-- ===== 管理员导航 ===== -->
        <template v-else-if="currentIdentity === 'admin'">
          <router-link to="/books-admin" class="nav-link" active-class="active">📚 图书管理</router-link>
          <router-link to="/admin/orders" class="nav-link" active-class="active">📋 订单管理</router-link>
          <router-link to="/admin/after-sale" class="nav-link" active-class="active">🔧 售后管理</router-link>
          <router-link to="/admin/home-manage" class="nav-link" active-class="active">🏠 首页管理</router-link>
          <router-link to="/admin/shops" class="nav-link" active-class="active">🏪 商家管理</router-link>
          <router-link to="/admin/users" class="nav-link" active-class="active">👤 用户管理</router-link>
          <router-link to="/dashboard" class="nav-link" active-class="active">📊 数据大屏</router-link>
        </template>
      </nav>

      <!-- 搜索框（仅买家视图显示）-->
      <div class="navbar-search" v-if="currentIdentity === 'buyer'">
        <el-autocomplete
          v-model="searchKeyword"
          :fetch-suggestions="fetchSuggestions"
          placeholder="搜索书籍/作者/ISBN"
          clearable
          size="default"
          class="nav-search-input"
          @keyup.enter="doSearch"
          @select="goBookDetail"
        >
          <template #prefix>
            <el-icon class="nav-search-icon"><Search /></el-icon>
          </template>
          <template #default="{ item }">
            <div class="suggest-item" @click="goBookDetail(item)">
              <el-image
                :src="getFullUrl(item.coverUrl)"
                fit="cover"
                class="suggest-cover"
              />
              <div class="suggest-info">
                <span class="suggest-title">{{ item.title }}</span>
                <span class="suggest-author">{{ item.author }}</span>
              </div>
            </div>
          </template>
        </el-autocomplete>
        <el-button type="primary" class="nav-search-btn" @click="doSearch">搜索</el-button>
      </div>

      <!-- 右侧：消息入口 + 身份切换 -->
      <div class="navbar-right">
        <!-- ===== 消息入口（统一指向 /messages） ===== -->
        <!-- ===== 消息入口根据角色跳转不同路径 ===== -->
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" type="danger">
          <router-link :to="messageLink" class="message-link">
            <el-icon :size="22"><Message /></el-icon>
          </router-link>
        </el-badge>

        <!-- 身份切换 -->
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="identity-switcher">
            {{ identityLabel }} <el-icon><ArrowDown/></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="buyer" :disabled="currentIdentity === 'buyer'">👤 买家视图</el-dropdown-item>
              <el-dropdown-item v-if="availableIdentities.includes('shop')" command="shop" :disabled="currentIdentity === 'shop'">🏪 商家视图</el-dropdown-item>
              <el-dropdown-item v-if="availableIdentities.includes('admin')" command="admin" :disabled="currentIdentity === 'admin'">🔧 管理后台</el-dropdown-item>
              <el-dropdown-item divided command="logout">🚪 退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Message, Search } from '@element-plus/icons-vue'
import request from '../utils/request'
import { getFullUrl } from '../utils/image'

defineOptions({ name: 'AppNavBar' })

const router = useRouter()

// ===== 用户信息（响应式 ref，手动更新） =====
const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

// ===== 搜索关键词 =====
const searchKeyword = ref('')

// ===== 计算属性依赖 userInfo.value =====
const isShopOwner = computed(() => userInfo.value.isShopOwner === 1)
const shopStatus = computed(() => userInfo.value.shopStatus || 0)

// ===== 未读消息 =====
const unreadCount = ref(0)

// ===== Props =====
const props = defineProps({
  currentIdentity: {
    type: String,
    default: 'buyer',
    validator: (val) => ['buyer', 'shop', 'admin'].includes(val)
  },
  availableIdentities: {
    type: Array,
    default: () => ['buyer']
  },
  onSwitchIdentity: {
    type: Function,
    default: () => {}
  }
})

// ===== 身份标识 =====
const identityLabel = computed(() => {
  const map = { buyer: '买家', shop: '商家', admin: '管理员' }
  return map[props.currentIdentity] || '买家'
})
const identityClass = computed(() => `navbar-${props.currentIdentity}`)
const identityColor = computed(() => {
  const map = { buyer: '#409EFF', shop: '#67C23A', admin: '#E6A23C' }
  return map[props.currentIdentity] || '#409EFF'
})

// ===== 消息入口路径（根据当前身份） =====
const messageLink = computed(() => {
  const identity = props.currentIdentity
  if (identity === 'shop') return '/shop/messages'
  if (identity === 'admin') return '/admin/messages'  // 管理端后续实现
  return '/messages'  // 买家默认
})

// ===== 获取 API 路径（用于消息） =====
const getApiPath = (path) => {
  const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (info.role === 1) return `/admin/message${path}`
  if (info.isShopOwner === 1) return `/shop/message${path}`
  return `/api/message${path}`
}

// ===== 搜索建议（el-autocomplete 回调）=====
const fetchSuggestions = async (query, cb) => {
  if (!query || query.trim().length === 0) { cb([]); return }
  try {
    const res = await request.get('/api/books/suggest', {
      params: { keyword: query.trim(), limit: 10 }
    })
    cb(res.data || [])
  } catch (_e) {
    cb([])
  }
}

// ===== 点击搜索按钮或回车 → 跳分类列表页 =====
const doSearch = () => {
  const kw = searchKeyword.value.trim()
  if (!kw) { router.push('/category'); return }
  router.push({ path: '/category', query: { keyword: kw } })
}

// ===== 点击某条建议 → 直接跳书籍详情 =====
const goBookDetail = (item) => {
  if (item && item.id) {
    router.push(`/book/${item.id}`)
  }
}

// ===== 未读消息 =====
const loadUnreadCount = async () => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (!info.userId) return
    const apiPath = getApiPath('/unread-count')
    const res = await request.get(apiPath)
    unreadCount.value = res.data || 0
  } catch (error) {
    // 忽略
  }
}

// ===== 更新用户信息（供父组件调用，如登录后刷新） =====
const updateUserInfo = () => {
  userInfo.value = JSON.parse(localStorage.getItem('userInfo') || '{}')
}

// ===== 监听 storage 事件（当 localStorage 被其他窗口修改时） =====
const handleStorageChange = (e) => {
  if (e.key === 'userInfo') {
    userInfo.value = JSON.parse(e.newValue || '{}')
  }
}

// ===== 联系客服（用户端） =====
const contactCustomerService = async () => {
  const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (!info.userId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    const targetUserId = 1
    const res = await request.get(`/api/message/create-or-get-session?targetUserId=${targetUserId}`)
    const sessionId = res.data

    if (!sessionId) {
      ElMessage.error('创建会话失败')
      return
    }

    router.push({
      name: 'MessageChat',
      params: { sessionId: sessionId },
      query: {
        targetUserId: targetUserId,
        targetName: '平台客服'
      }
    })
  } catch (error) {
    const msg = error.response?.data?.msg || '联系客服失败，请稍后再试'
    ElMessage.error(msg)
  }
}

// ===== 联系平台客服（商家端） =====
const contactPlatform = async () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (!userInfo.userId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    // 管理员的默认用户ID为1
    const targetUserId = 1

    // 使用商家端API创建会话
    const res = await request.get(`/shop/message/create-or-get-session?targetUserId=${targetUserId}`)
    const sessionId = res.data

    if (!sessionId) {
      ElMessage.error('创建会话失败')
      return
    }

    // 跳转到商家端聊天页面
    router.push({
      name: 'ShopMessageChat',
      params: { sessionId: sessionId },
      query: {
        targetUserId: targetUserId,
        targetName: '平台客服'
      }
    })
  } catch (error) {
    const msg = error.response?.data?.msg || '联系客服失败，请稍后再试'
    ElMessage.error(msg)
  }
}

// ===== 身份切换命令 =====
const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    // 更新本地 userInfo
    userInfo.value = {}
    ElMessage.success('已退出')
    router.push('/login')
    return
  }
  props.onSwitchIdentity(command)
}

// ===== 暴露方法给父组件 =====
defineExpose({
  loadUnreadCount,
  updateUserInfo
})

// ===== 定时器（刷新未读） =====
let timer = null

// ===== 生命周期 =====
onMounted(() => {
  // 1. 初始加载用户信息
  updateUserInfo()

  // 2. 加载未读消息
  loadUnreadCount()

  // 3. 定时刷新未读（30秒）
  timer = setInterval(loadUnreadCount, 30000)

  // 4. 监听 storage 事件（跨标签页）
  window.addEventListener('storage', handleStorageChange)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('storage', handleStorageChange)
})
</script>

<style scoped>
/* 样式保持不变，与原有相同 */
.app-navbar {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 1000;
  height: 64px;
  display: flex;
  align-items: center;
}

.navbar-shop {
  background: linear-gradient(135deg, #1a1a2e, #2d2d44);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}
.navbar-shop .brand-name,
.navbar-shop .nav-link,
.navbar-shop .identity-switcher {
  color: rgba(255, 255, 255, 0.85) !important;
}
.navbar-shop .nav-link:hover {
  color: #fff !important;
}
.navbar-shop .nav-link.active {
  color: #fff !important;
  border-bottom-color: #67C23A !important;
}
/* 商家/管理员视图隐藏搜索 */
.navbar-shop .navbar-search,
.navbar-admin .navbar-search {
  display: none;
}

.navbar-admin {
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}
.navbar-admin .brand-name,
.navbar-admin .nav-link,
.navbar-admin .identity-switcher {
  color: rgba(255, 255, 255, 0.85) !important;
}
.navbar-admin .nav-link:hover {
  color: #fff !important;
}
.navbar-admin .nav-link.active {
  color: #fff !important;
  border-bottom-color: #E6A23C !important;
}

.navbar-container {
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

/* ===== 搜索框样式 ===== */
.navbar-search {
  display: flex;
  align-items: center;
  gap: 6px;
}
.nav-search-input {
  width: 280px;
}
.nav-search-input :deep(.el-input__wrapper) {
  height: 34px;
  border-radius: 17px;
  padding: 0 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
.nav-search-input :deep(.el-input__inner) {
  font-size: 13px;
}
.nav-search-icon {
  font-size: 16px;
  color: #909399;
}
.nav-search-btn {
  height: 34px;
  border-radius: 17px;
  padding: 0 14px;
  font-size: 13px;
}

/* 搜索建议下拉 */
.suggest-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  cursor: pointer;
}
.suggest-item:hover {
  background: #ecf5ff;
}
.suggest-cover {
  width: 32px;
  height: 40px;
  border-radius: 3px;
  flex-shrink: 0;
}
.suggest-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.suggest-title {
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.suggest-author {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

/* ===== 分类下拉 ===== */
.navbar-category-dropdown {
  cursor: pointer;
}
.nav-category-link {
  font-size: 14px;
  color: #606266;
  padding: 6px 10px;
  border-radius: 6px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}
.nav-category-link:hover {
  color: #409EFF;
  background: #ecf5ff;
}

.navbar-left .brand {
  display: flex;
  align-items: center;
  text-decoration: none;
  gap: 10px;
}
.brand-name {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  letter-spacing: 1px;
}
.identity-badge {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 12px;
  color: #fff;
  font-weight: 500;
  background-color: #409EFF;
}

.navbar-center {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
  justify-content: center;
}
.nav-link {
  font-size: 15px;
  color: #606266;
  text-decoration: none;
  padding: 4px 0;
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s;
  white-space: nowrap;
}
.nav-link:hover {
  color: #409EFF;
}
.nav-link.active {
  color: #409EFF;
  border-bottom-color: #409EFF;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.identity-switcher {
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}
.identity-switcher:hover {
  border-color: #409EFF;
  background: #ecf5ff;
}

.message-link {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  text-decoration: none;
  transition: color 0.2s;
  padding: 4px;
}
.message-link:hover {
  color: #409EFF;
}

.status-pending {
  font-size: 14px;
  color: #E6A23C;
  cursor: default;
}

@media (max-width: 1024px) {
  .navbar-center { gap: 12px; }
  .nav-link { font-size: 13px; }
}
@media (max-width: 768px) {
  .navbar-center {
    gap: 8px;
    flex-wrap: nowrap;
    overflow-x: auto;
    justify-content: flex-start;
  }
  .nav-link { font-size: 12px; white-space: nowrap; }
  .brand-name { font-size: 16px; }
  .identity-badge { font-size: 10px; padding: 1px 8px; }
}
</style>
