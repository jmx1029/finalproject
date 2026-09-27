<template>
  <div class="user-shop-home-page" v-loading="loading">
    <!-- ===== 预览模式下的关闭按钮 ===== -->
    <div v-if="previewMode" class="preview-close-bar">
      <el-button type="info" plain @click="$emit('close')">
        ✕ 关闭预览
      </el-button>
    </div>

    <!-- ===== 店铺信息头部 ===== -->
    <div class="shop-header" v-if="shop">
      <div class="shop-header-content">
        <div class="shop-avatar">
          <el-avatar :size="80" :src="getFullUrl(shop.logo)">
            {{ shop.name?.charAt(0) || '店' }}
          </el-avatar>
        </div>

        <div class="shop-info">
          <h1 class="shop-name">{{ shop.name }}</h1>
          <!-- ===== 店铺公告 ===== -->
          <div v-if="shop.announcement" class="shop-announcement">
            <el-alert
              :title="shop.announcement"
              type="info"
              :closable="false"
              show-icon
            />
          </div>
          <p class="shop-description">{{ shop.description || '这家店还没有简介哦~' }}</p>
          <div class="shop-contact">
            <span v-if="shop.contactPerson">👤 {{ shop.contactPerson }}</span>
            <span v-if="shop.contactPhone">📞 {{ shop.contactPhone }}</span>
            <span v-if="shop.address">📍 {{ shop.address }}</span>
          </div>
          <!-- 店铺评分 -->
          <div class="shop-rating-row">
            <el-rate :model-value="shop.avgRating || 0" disabled show-score :max="10" :step="2" size="small"/>
            <span class="shop-rating-count">（{{ shop.ratingCount || 0 }} 人评价）</span>
          </div>
        </div>
        <div class="shop-actions">
          <!-- 已登录且非店主且非自营店（自营店联系平台客服） -->
          <el-button
            v-if="isLoggedIn && !isOwner && shop.id !== 1"
            type="info"
            size="large"
            plain
            @click="contactShop"
          >
            💬 联系卖家
          </el-button>
          <!-- 自营店（shopId=1）联系平台客服 -->
          <el-button
            v-else-if="isLoggedIn && shop.id === 1"
            type="info"
            size="large"
            plain
            @click="contactShop"
          >
            💬 联系平台客服
          </el-button>
          <!-- 店主本人 -->
          <el-tag v-else-if="isLoggedIn && isOwner" type="success" size="large">
            👑 这是我的店铺
          </el-tag>
          <!-- 未登录 -->
          <el-button
            v-else
            type="primary"
            size="large"
            @click="$router.push('/login')"
          >
            登录后联系卖家
          </el-button>
        </div>
      </div>
    </div>

    <!-- ===== 商品列表 ===== -->
    <el-card class="book-list-card" shadow="hover">
      <template #header>
        <div class="book-list-header">
          <span>📚 全部商品</span>
          <span class="book-count">共 {{ total }} 件商品</span>
        </div>
      </template>

      <div v-if="books.length === 0 && !loading" class="empty-books">
        <el-empty description="该店铺暂无商品" />
      </div>

      <div v-else class="book-grid">
        <div
          v-for="book in books"
          :key="book.id"
          class="book-item"
          @click="goBookDetail(book.id)"
        >
          <el-image
            :src="getFullUrl(book.coverUrl)"
            fit="cover"
            class="book-cover"
          >
            <template #error>
              <div class="cover-placeholder">📖</div>
            </template>
          </el-image>
          <div class="book-info">
            <div class="book-title">{{ book.title }}</div>
            <div class="book-author">{{ book.author || '未知作者' }}</div>
            <div class="book-price">¥{{ book.price }}</div>
          </div>
        </div>
      </div>

      <el-pagination
        class="pagination"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[8, 16, 24]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadBooks"
        @current-change="loadBooks"
      />
    </el-card>

    <!-- ===== 店铺评价区 ===== -->
    <el-card class="shop-comment-card" shadow="hover">
      <template #header>
        <div class="comment-header">
          <span>📝 店铺评价</span>
          <el-button
            v-if="isLoggedIn && !isOwner"
            type="primary"
            plain
            @click="openCommentDialog"
          >
            我要评价
          </el-button>
        </div>
      </template>

      <!-- 评价列表 -->
      <div class="comment-list" v-if="commentList.length > 0">
        <div v-for="comment in commentList" :key="comment.id" class="comment-item">
          <div class="comment-avatar">
            <el-avatar :size="36" :src="getFullUrl(comment.avatar)">
              {{ comment.nickname ? comment.nickname.charAt(0) : 'U' }}
            </el-avatar>
          </div>
          <div class="comment-body">
            <div class="comment-user">
              <span class="user-name">{{ comment.nickname || comment.username || '匿名用户' }}</span>
              <el-rate v-model="comment.rating" disabled size="small" :max="10" :step="2"/>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div v-if="comment.replyContent" class="shop-reply">
              <div class="reply-label">商家回复</div>
              <div class="reply-content">{{ comment.replyContent }}</div>
              <div v-if="comment.replyTime" class="reply-time">{{ formatTime(comment.replyTime) }}</div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无评价，快来发表第一条吧～" :image-size="80"/>

      <el-pagination
        class="pagination"
        v-model:current-page="commentPage"
        v-model:page-size="commentSize"
        :total="commentTotal"
        :page-sizes="[5, 10]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadComments"
        @current-change="loadComments"
      />
    </el-card>

    <!-- ===== 写评价弹窗 ===== -->
    <el-dialog v-model="commentDialogVisible" title="评价店铺" width="480px">
      <el-form :model="commentForm" ref="commentFormRef">
        <el-form-item label="评分" required>
          <el-rate v-model="commentForm.rating" :max="10" :step="2"
                   :texts="['很差', '较差', '还行', '推荐', '力荐']" show-text/>
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="commentForm.content" type="textarea"
                    :rows="4" maxlength="500" show-word-limit
                    placeholder="说说你的购物体验吧～"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="commentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComment" :loading="submitting">提交评价</el-button>
      </template>
    </el-dialog>

    <!-- ===== 加载失败 ===== -->
    <el-empty v-if="!loading && !shop" description="店铺不存在或已被关闭">
      <el-button type="primary" @click="previewMode ? $emit('close') : $router.push('/')">
        {{ previewMode ? '关闭预览' : '返回首页' }}
      </el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'
import { contactSeller } from '../../utils/chat'

// ===== Props =====
const props = defineProps({
  shopId: {
    type: [String, Number],
    default: null
  },
  previewMode: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const route = useRoute()
const router = useRouter()

// ===== 状态 =====
const loading = ref(false)
const shop = ref(null)
const books = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(8)

// ===== 获取 shopId（优先使用 props，否则从路由获取） =====
const effectiveShopId = computed(() => {
  return props.shopId || route.params.shopId
})

const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const userInfo = computed(() => {
  return JSON.parse(localStorage.getItem('userInfo') || '{}')
})

const isOwner = computed(() => {
  if (!shop.value || !userInfo.value.userId) return false
  return shop.value.userId === userInfo.value.userId
})

// ===== 店铺评价相关 =====
const commentList = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentSize = ref(5)
const commentDialogVisible = ref(false)
const commentFormRef = ref()
const submitting = ref(false)
const commentForm = ref({ rating: 0, content: '' })

const loadComments = async () => {
  if (!effectiveShopId.value) return
  try {
    const res = await request.get('/api/shop-comment/list', {
      params: { shopId: effectiveShopId.value, page: commentPage.value, size: commentSize.value }
    })
    commentList.value = res.data.records || []
    commentTotal.value = res.data.total || 0
  } catch (error) {
    commentList.value = []
  }
}

const openCommentDialog = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  commentForm.value = { rating: 0, content: '' }
  commentDialogVisible.value = true
}

const submitComment = async () => {
  if (!commentForm.value.rating) {
    ElMessage.warning('请选择评分')
    return
  }
  if (!commentForm.value.content?.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }
  submitting.value = true
  try {
    await request.post('/api/shop-comment', {
      shopId: Number(effectiveShopId.value),
      rating: commentForm.value.rating,
      content: commentForm.value.content.trim()
    })
    ElMessage.success('评价成功')
    commentDialogVisible.value = false
    commentPage.value = 1
    await loadComments()
    await loadShop()  // 刷新店铺评分
  } catch (error) {
    const msg = error.response?.data?.msg || '评价失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const t = new Date(time)
  const now = new Date()
  const diff = (now - t) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  if (diff < 7 * 86400) return Math.floor(diff / 86400) + ' 天前'
  return t.toLocaleDateString()
}

// ===== 加载店铺信息 =====
const loadShop = async () => {
  if (!effectiveShopId.value) {
    ElMessage.error('店铺ID不存在')
    if (props.previewMode) {
      emit('close')
    } else {
      router.push('/')
    }
    return
  }
  try {
    const res = await request.get(`/api/shop/public/${effectiveShopId.value}`)
    if (res.data) {
      shop.value = res.data
    } else {
      shop.value = null
      ElMessage.error('店铺不存在')
    }
  } catch (error) {
    shop.value = null
    ElMessage.error('加载店铺信息失败')
  }
}

// ===== 加载商品列表 =====
const loadBooks = async () => {
  if (!effectiveShopId.value) return
  loading.value = true
  try {
    const res = await request.get(`/api/shop/public/${effectiveShopId.value}/books`, {
      params: { page: page.value, size: size.value }
    })
    books.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

// ===== 联系卖家（或平台客服） =====
const contactShop = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await contactSeller(shop.value.id, shop.value.name)
}

// ===== 跳转商品详情 =====
const goBookDetail = (bookId) => {
  router.push(`/book/${bookId}`)
}

// ===== 监听 effectiveShopId 变化 =====
watch(effectiveShopId, () => {
  page.value = 1
  commentPage.value = 1
  loadShop()
  loadBooks()
  loadComments()
}, { immediate: true })

// ===== 生命周期 =====
onMounted(() => {
  if (effectiveShopId.value) {
    loadShop()
    loadBooks()
  }
})
</script>

<style scoped>
/* 样式保持不变... */
.user-shop-home-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px 40px;
}

/* 预览模式关闭栏 */
.preview-close-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

/* ===== 店铺头部 ===== */
.shop-header {
  background: #fff;
  border-radius: 12px;
  padding: 30px 32px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.shop-header-content {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.shop-avatar {
  flex-shrink: 0;
}

.shop-info {
  flex: 1;
  min-width: 200px;
}

.shop-name {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px 0;
}

.shop-announcement {
  margin: 8px 0 10px 0;
}

.shop-description {
  font-size: 14px;
  color: #909399;
  margin: 0 0 10px 0;
}

.shop-contact {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: #606266;
}
.shop-contact span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.shop-actions {
  flex-shrink: 0;
  margin-left: auto;
}

/* ===== 商品列表 ===== */
.book-list-card {
  border-radius: 12px;
}

.book-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.book-count {
  font-size: 14px;
  color: #909399;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
  padding: 4px 0;
}

.book-item {
  background: #fafafa;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #f0f0f0;
}
.book-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border-color: #409EFF;
}

.book-cover {
  width: 100%;
  aspect-ratio: 3 / 4;
  background: #f5f7fa;
  display: block;
}

.cover-placeholder {
  width: 100%;
  aspect-ratio: 3 / 4;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  background: #f5f7fa;
  color: #c0c4cc;
}

.book-info {
  padding: 12px 14px 14px;
}

.book-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-author {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.book-price {
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
  margin-top: 6px;
}

.empty-books {
  padding: 40px 0;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* ===== 店铺评分展示 ===== */
.shop-rating-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
.shop-rating-count {
  font-size: 13px;
  color: #909399;
}

/* ===== 店铺评价 Card ===== */
.shop-comment-card {
  margin-top: 20px;
}
.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.comment-item {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.comment-body {
  flex: 1;
}
.comment-user {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.comment-user .user-name {
  font-weight: 600;
  color: #303133;
}
.comment-time {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}
.comment-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}
.shop-reply {
  background: #f5f7fa;
  border-left: 3px solid #409EFF;
  padding: 8px 12px;
  margin: 8px 0 0;
  border-radius: 0 4px 4px 0;
}
.reply-label {
  font-size: 12px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 4px;
}
.reply-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.reply-time {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .shop-header-content {
    flex-direction: column;
    text-align: center;
  }
  .shop-actions {
    margin-left: 0;
    width: 100%;
  }
  .shop-actions .el-button {
    width: 100%;
  }
  .shop-contact {
    justify-content: center;
  }
  .book-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 12px;
  }
}
</style>
