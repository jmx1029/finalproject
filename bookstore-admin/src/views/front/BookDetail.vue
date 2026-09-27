<template>
  <div class="book-detail-page" v-loading="loading">
    <!-- ===== 书籍详情 ===== -->
    <el-card class="detail-card" v-if="book">
      <div class="book-detail">
        <!-- 左侧：封面 -->
        <div class="book-cover-area">
          <el-image :src="getFullUrl(book.coverUrl)" fit="contain" class="book-cover-large">
            <template #error>
              <div class="cover-placeholder-large">📖</div>
            </template>
          </el-image>
        </div>

        <!-- 右侧：书籍信息 -->
        <div class="book-info-area">
          <h1 class="book-title">{{ book.title }}</h1>
          <p class="book-subtitle" v-if="book.subtitle">{{ book.subtitle }}</p>

          <div class="book-meta-grid">
            <div class="meta-item">
              <span class="meta-label">作者</span>
              <span class="meta-value">{{ book.author || '未知' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">出版社</span>
              <span class="meta-value">{{ book.publisher || '未知' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">出品方</span>
              <span class="meta-value" style="color:#409EFF;">
                🏪 {{ book.shopName || '官方自营' }}
                <el-button
                  v-if="book.shopId && book.shopId !== 1"
                  type="primary"
                  link
                  size="small"
                  @click="goToShop(book.shopId)"
                  style="margin-left:8px;"
                >
                  查看店铺 →
                </el-button>
              </span>
            </div>

            <div class="meta-item">
              <span class="meta-label">出版日期</span>
              <span class="meta-value">{{ book.publishDate || '未知' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">ISBN</span>
              <span class="meta-value">{{ book.isbn || '暂无' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">定价</span>
              <span class="meta-value">¥{{ book.price }}</span>
            </div>
          </div>

          <div class="book-rating">
            <span class="rating-label">评分：</span>
            <span class="rating-num">{{ book.avgRating || book.rating || '暂无' }}</span>
            <span class="rating-sub"> 分（{{ book.ratingCount || 0 }} 人评价）</span>
          </div>

          <div class="book-stock">
            <span class="stock-label">库存：</span>
            <span :class="book.stock > 0 ? 'stock-available' : 'stock-empty'">
              {{ book.stock > 0 ? book.stock + ' 件' : '已售罄' }}
            </span>
          </div>

          <div class="book-price-area">
            <span class="book-price-large">¥{{ book.price }}</span>
            <span class="book-original-price"
                  v-if="book.originalPrice && book.originalPrice > book.price">
              ¥{{ book.originalPrice }}
            </span>
          </div>

          <div class="book-description">
            <h4>简介</h4>
            <p>{{ book.description || '暂无简介' }}</p>
          </div>

          <!-- 购买区域 -->
          <div class="purchase-area">
            <el-input-number v-model="quantity" :min="1" :max="book.stock > 0 ? book.stock : 1"
                             size="large"/>
            <el-button type="primary" size="large" :disabled="book.stock <= 0" @click="addToCart"
                       :loading="cartLoading">
              加入购物车
            </el-button>

            <!-- ✅ 正确 -->
            <el-button
              v-if="book.shopId && book.shopId !== 1 && book.shopName"
              type="info"
              size="large"
              plain
              @click="contactSellerHandler"
            >
              💬 联系卖家
            </el-button>

          </div>
        </div>
      </div>
    </el-card>

    <!-- ===== 书圈评论 ===== -->
    <el-card class="comment-card">
      <!-- 标题栏 -->
      <div class="comment-header">
        <div class="comment-title">
          <span class="title-icon">💬</span>
          <span class="title-text">书圈</span>
          <span class="title-count">｜ 共 {{ commentTotal }} 条</span>
        </div>
        <el-button type="primary" plain @click="openCommentDialog" v-if="isLoggedIn">
          我来说两句
        </el-button>
      </div>

      <!-- 评论列表 -->
      <div class="comment-list" v-if="commentList.length > 0">
        <div v-for="comment in commentList" :key="comment.id" class="comment-item">
          <div class="comment-avatar">
            <el-avatar :size="40" :src="getFullUrl(comment.avatar)">
              {{ comment.nickname ? comment.nickname.charAt(0) : 'U' }}
            </el-avatar>
          </div>
          <div class="comment-body">
            <div class="comment-user">
              <span class="user-name">{{
                  comment.nickname || comment.username || '匿名用户'
                }}</span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
              <el-rate
                v-if="comment.rating != null"
                :model-value="Math.floor(comment.rating / 2)"
                disabled
                size="small"
                :max="5"
              />
            </div>
            <div class="comment-content">{{ comment.content }}</div>

            <!-- 商家回复 -->
            <div v-if="comment.replyContent" class="shop-reply">
              <div class="reply-label">🏪 商家回复</div>
              <div class="reply-content">{{ comment.replyContent }}</div>
              <div v-if="comment.replyTime" class="reply-time">{{ formatTime(comment.replyTime) }}</div>
            </div>

            <!-- 用户回复列表（抖音风格） -->
            <div v-if="comment.replies && comment.replies.length > 0" class="reply-list">
              <div
                v-for="reply in comment.replies"
                :key="reply.id"
                class="reply-item"
              >
                <span class="reply-avatar">
                  <el-avatar :size="22" :src="getFullUrl(reply.avatar)">
                    {{ (reply.nickname || 'U').charAt(0) }}
                  </el-avatar>
                </span>
                <div class="reply-main">
                  <span class="reply-author">{{ reply.nickname || reply.username }}</span>
                  <template v-if="reply.replyToUsername">
                    <span class="reply-arrow">回复</span>
                    <span class="reply-target">@{{ reply.replyToUsername }}</span>
                  </template>
                  <span class="reply-content-text">：{{ reply.content }}</span>
                  <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                  <span class="reply-actions">
                    <span class="reply-like" @click="likeComment(reply.id)">👍 {{ reply.likeCount || 0 }}</span>
                    <span class="reply-delete" v-if="reply.canDelete" @click="deleteComment(reply.id)">删除</span>
                    <span class="reply-btn" @click="openReplyBox(comment.id, reply.userId, reply.nickname || reply.username)">回复</span>
                  </span>
                </div>
              </div>

              <!-- 展开更多回复 -->
              <div
                v-if="comment.replyCount && comment.replyCount > comment.replies.length"
                class="expand-reply-btn"
                @click="loadMoreReplies(comment)"
              >
                ⬇️ 展开全部 {{ comment.replyCount }} 条回复
              </div>

              <!-- 收起回复 -->
              <div
                v-if="comment._repliesExpanded && comment._repliesExpandedPage > 1"
                class="collapse-reply-btn"
                @click="collapseReplies(comment)"
              >
                ⬆️ 收起回复
              </div>
            </div>

            <!-- 操作栏 -->
            <div class="comment-actions">
              <span class="like-btn" @click="likeComment(comment.id)">
                👍 {{ comment.likeCount || 0 }}
              </span>
              <span class="reply-btn-main" @click="openReplyBox(comment.id, comment.userId, comment.nickname || comment.username)">
                💬 回复
              </span>
              <span class="delete-btn" v-if="comment.canDelete" @click="deleteComment(comment.id)">
                删除
              </span>
            </div>

            <!-- 回复输入框（点击"回复"展开） -->
            <div v-if="comment._replyBoxOpen" class="reply-box">
              <el-input
                v-model="comment._replyContent"
                type="textarea"
                :rows="2"
                :placeholder="comment._replyPlaceholder"
                maxlength="300"
                show-word-limit
              />
              <div class="reply-box-actions">
                <el-button size="small" @click="comment._replyBoxOpen = false">取消</el-button>
                <el-button size="small" type="primary" @click="submitReply(comment)">发送</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空评论 -->
      <el-empty v-else description="暂无评论，快来发表你的看法吧～" :image-size="80"/>

      <!-- 分页 -->
      <el-pagination
        class="comment-pagination"
        v-model:current-page="commentPage"
        v-model:page-size="commentSize"
        :total="commentTotal"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadComments"
        @current-change="loadComments"
      />
    </el-card>

    <!-- ===== 发表评论弹窗 ===== -->
    <el-dialog v-model="dialogVisible" title="发表评论" width="500px">
      <el-form :model="commentForm" ref="commentFormRef">
        <el-form-item label="评分">
          <el-rate v-model="commentForm._rate5" :max="5"
                   :texts="['很差', '较差', '还行', '推荐', '力荐']"
                   show-text/>
          <div class="rate-hint">💡 1 星 = 2 分（满分 10 分），你当前：{{ commentForm._rate5 * 2 }} 分</div>
        </el-form-item>
        <el-form-item label="评论">
          <el-input
            v-model="commentForm.content"
            type="textarea"
            :rows="4"
            placeholder="分享你的阅读体验..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComment" :loading="submitting">发表</el-button>
      </template>
    </el-dialog>

    <!-- ===== 相关推荐（猜你喜欢） ===== -->
    <section v-if="recommendBooks.length > 0" class="recommend-section">
      <div class="recommend-header">
        <span class="recommend-title">✨ 猜你喜欢</span>
        <span class="recommend-sub">同分类热门好书</span>
      </div>
      <div class="recommend-grid">
        <div
          v-for="bk in recommendBooks"
          :key="bk.id"
          class="recommend-card"
          @click="goDetail(bk.id)"
        >
          <el-image :src="getFullUrl(bk.coverUrl)" fit="cover" class="recommend-cover">
            <template #error>
              <div class="cover-placeholder">📖</div>
            </template>
          </el-image>
          <div class="recommend-info">
            <div class="recommend-title-line" :title="bk.title">{{ bk.title }}</div>
            <div class="recommend-author" v-if="bk.author">{{ bk.author }}</div>
            <div class="recommend-meta">
              <span class="recommend-price">¥{{ bk.price }}</span>
              <span class="recommend-sales">已售{{ bk.sales || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted, watch} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import request from '../../utils/request'
import {getFullUrl} from '../../utils/image'
import { contactSeller } from '../../utils/chat'

const router = useRouter()
const route = useRoute()

// ===== 状态 =====
const loading = ref(false)
const book = ref(null)
const quantity = ref(1)
const cartLoading = ref(false)
const recommendBooks = ref([])

// ===== 评论 =====
const commentList = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentSize = ref(10)
const dialogVisible = ref(false)
const submitting = ref(false)
const commentFormRef = ref()
const commentForm = reactive({
  content: '',
  rating: 0
})

// ===== 计算属性 =====
const isLoggedIn = computed(() => !!localStorage.getItem('token'))


// ===== 获取完整图片URL =====
/*const getFullUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  if (url.startsWith('/uploads/')) {
    return 'http://localhost:8080' + url
  }
  return 'http://localhost:8080/uploads/' + url
}*/

// ===== 联系卖家 =====
const contactSellerHandler = async () => {
  await contactSeller(book.value.shopId, book.value.shopName)
}

// ===== 跳转店铺主页（用户端） =====
const goToShop = (shopId) => {
  router.push(`/shop/${shopId}`)
}

// ===== 格式化时间 =====
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
}

// ===== 加载图书详情 =====
const loadBookDetail = async () => {
  const bookId = route.params.id
  if (!bookId) {
    ElMessage.error('图书ID不存在')
    router.push('/category')
    return
  }

  loading.value = true
  try {
    const res = await request.get('/api/books/' + bookId, {
      timeout: 10000  // 10秒超时
    })
    if (res.data) {
      book.value = res.data
      // 加载相关推荐
      loadRecommend()
    } else {
      ElMessage.error('图书不存在')
      router.push('/category')
    }
  } catch (error) {
    const msg = error.response?.data?.msg || error.message || '加载图书详情失败，请稍后重试'
    ElMessage.error(msg)
    // 不自动跳转，让用户手动刷新或返回
  } finally {
    loading.value = false
  }
}

// ===== 加载评论 =====
const loadComments = async () => {
  const bookId = route.params.id
  if (!bookId) return

  try {
    const res = await request.get('/api/comment/list', {
      params: {
        bookId: bookId,
        page: commentPage.value,
        size: commentSize.value
      }
    })
    commentList.value = res.data.records || []
    commentTotal.value = res.data.total || 0
  } catch (error) {
  }
}

// ===== 加入购物车 =====
const addToCart = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (book.value.stock <= 0) {
    ElMessage.warning('库存不足')
    return
  }

  cartLoading.value = true
  try {
    await request.post('/api/cart/add', {
      bookId: book.value.id,
      quantity: quantity.value
    })
    ElMessage.success('已加入购物车')
  } catch (error) {
    const msg = error.response?.data?.msg || '加入购物车失败'
    ElMessage.error(msg)
  } finally {
    cartLoading.value = false
  }
}

// ===== 打开评论弹窗（预检购买状态 + 是否已评价）=====
const openCommentDialog = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  // 预检：是否有资格评价
  try {
    const res = await request.get('/api/comment/check-purchased', {
      params: { bookId: book.value.id }
    })
    const check = res.data
    if (!check.purchased) {
      ElMessage.warning('您还未购买本书，暂时无法评价')
      return
    }
    if (check.alreadyCommented) {
      ElMessage.warning('您已经评价过这本书了')
      return
    }
  } catch (e) {
    // 预检接口调用失败不阻塞（可能是网络问题），让后端 addComment 兜底校验
  }

  commentForm.content = ''
  commentForm.rating = 0
  commentForm._rate5 = 0  // 5 颗星的临时值，提交时 ×2
  dialogVisible.value = true
}

// ===== 发表评论 =====
const submitComment = async () => {
  if (!commentForm.content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  submitting.value = true
  try {
    await request.post('/api/comment', {
      bookId: book.value.id,
      content: commentForm.content.trim(),
      rating: commentForm._rate5 ? commentForm._rate5 * 2 : null  // 5 星制 × 2 = 10 分制
    })
    ElMessage.success('评论发表成功')
    dialogVisible.value = false
    // 刷新评论列表
    commentPage.value = 1
    await loadComments()
    // 刷新图书详情（更新评分）
    await loadBookDetail()
  } catch (error) {
    const msg = error.response?.data?.msg || '发表评论失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

// ===== 加载相关推荐 =====
const loadRecommend = async () => {
  try {
    const params = {
      excludeBookId: book.value?.id,
      limit: 8
    }
    if (book.value?.categoryId) {
      params.categoryId = book.value.categoryId
    }
    const res = await request.get('/api/books/recommend', { params })
    recommendBooks.value = res.data || []
  } catch (e) {
    recommendBooks.value = []
  }
}

// ===== 跳转到另一本书的详情 =====
const goDetail = (targetBookId) => {
  router.push(`/book/${targetBookId}`)
}

// ===== 点赞评论 =====
const likeComment = async (commentId) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await request.put('/api/comment/like/' + commentId)
    // 本地更新点赞数
    const comment = commentList.value.find(c => c.id === commentId)
    if (comment) {
      comment.likeCount = (comment.likeCount || 0) + 1
    }
  } catch (error) {
    const msg = error.response?.data?.msg || '点赞失败'
    ElMessage.error(msg)
  }
}

// ===== 删除评论 =====
const deleteComment = async (commentId) => {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '提示', {type: 'warning'})
    await request.delete('/api/comment/' + commentId)
    ElMessage.success('删除成功')
    await loadComments()
    await loadBookDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '删除失败')
    }
  }
}

// ===== 用户回复（抖音风格）=====

// 打开/切换回复输入框
const openReplyBox = (parentCommentId, replyToUserId, replyToName) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再回复')
    return
  }
  // 先关掉其他评论的回复框
  commentList.value.forEach(c => {
    if (c.id !== parentCommentId) {
      c._replyBoxOpen = false
    }
  })
  const parent = commentList.value.find(c => c.id === parentCommentId)
  if (!parent) return

  parent._replyBoxOpen = true
  parent._replyContent = ''
  parent._replyToUserId = replyToUserId
  parent._replyPlaceholder = replyToName
    ? `回复 @${replyToName}：`
    : '输入回复内容...'
}

// 提交回复
const submitReply = async (parentComment) => {
  const content = (parentComment._replyContent || '').trim()
  if (!content) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  try {
    await request.post('/api/comment/reply', {
      parentId: parentComment.id,
      replyToUserId: parentComment._replyToUserId,
      content: content
    })
    ElMessage.success('回复成功')
    parentComment._replyBoxOpen = false
    parentComment._replyContent = ''

    // 如果已经展开，直接追加到现有 replies；否则只刷新 replyCount
    if (parentComment._repliesExpanded) {
      parentComment._repliesExpandedPage++
      await loadMoreReplies(parentComment)
    } else {
      // 刷新一下当前顶级评论的 replyCount + replies（重新 load 顶级评论列表更简单）
      await loadComments()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '回复失败')
  }
}

// 加载更多回复（分页追加）
const loadMoreReplies = async (parentComment) => {
  if (!parentComment._repliesExpandedPage) {
    parentComment._repliesExpandedPage = 1
  }
  parentComment._repliesExpandedPage++

  try {
    const res = await request.get(`/api/comment/replies/${parentComment.id}`, {
      params: {
        page: parentComment._repliesExpandedPage,
        size: 20
      }
    })
    const newReplies = res.data?.records || []
    if (newReplies.length === 0) {
      parentComment._repliesExpandedPage--  // 没数据了回退
      ElMessage.info('没有更多回复了')
      return
    }

    // 合并 replies
    if (!parentComment._repliesExpanded) {
      parentComment._repliesExpanded = true
      parentComment.replies = []
    }
    parentComment.replies = [...(parentComment.replies || []), ...newReplies]
  } catch (error) {
    parentComment._repliesExpandedPage--
    ElMessage.error(error.response?.data?.msg || '加载回复失败')
  }
}

// 收起回复（重置为只显示初始 5 条）
const collapseReplies = (parentComment) => {
  parentComment._repliesExpanded = false
  parentComment._repliesExpandedPage = 1
  // 重新 load 顶级评论列表（每条默认带 5 条回复）
  loadComments()
}

// ===== 生命周期 =====
onMounted(() => {
  loadBookDetail()
  loadComments()
})

// 监听路由参数变化，同组件跳转（如 /book/1 → /book/2）时重新加载
watch(() => route.params.id, (newId) => {
  if (newId) {
    loadBookDetail()
    loadComments()
  }
})
</script>

<style scoped>
.book-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px 40px;
}

/* ===== 书籍详情 ===== */
.detail-card {
  margin-bottom: 24px;
  border-radius: 12px;
}

.detail-card :deep(.el-card__body) {
  padding: 30px;
}

.book-detail {
  display: flex;
  gap: 40px;
}

.book-cover-area {
  flex-shrink: 0;
}

.book-cover-large {
  width: 280px;
  height: 380px;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  background: #f5f7fa;
}

.cover-placeholder-large {
  width: 280px;
  height: 380px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64px;
  background: #f5f7fa;
  border-radius: 8px;
}

.book-info-area {
  flex: 1;
  min-width: 0;
}

.book-title {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.book-subtitle {
  font-size: 16px;
  color: #909399;
  margin: 0 0 16px 0;
}

.book-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 24px;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  gap: 8px;
}

.meta-label {
  color: #909399;
  font-size: 14px;
  min-width: 56px;
}

.meta-value {
  color: #303133;
  font-size: 14px;
  word-break: break-all;
}

.book-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.rating-label {
  font-size: 14px;
  color: #606266;
}

.rating-count {
  font-size: 13px;
  color: #909399;
}

.rating-num {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
}

.rating-sub {
  font-size: 13px;
  color: #909399;
}

.book-stock {
  margin-bottom: 12px;
}

.stock-label {
  font-size: 14px;
  color: #606266;
}

.stock-available {
  color: #67c23a;
  font-weight: 500;
}

.stock-empty {
  color: #f56c6c;
  font-weight: 500;
}

.book-price-area {
  margin-bottom: 16px;
}

.book-price-large {
  font-size: 28px;
  font-weight: bold;
  color: #f56c6c;
}

.book-original-price {
  font-size: 16px;
  color: #909399;
  text-decoration: line-through;
  margin-left: 12px;
}

.book-description {
  margin-bottom: 20px;
}

.book-description h4 {
  font-size: 14px;
  color: #606266;
  margin: 0 0 4px 0;
}

.book-description p {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  margin: 0;
}

.purchase-area {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* ===== 书圈评论 ===== */
.comment-card {
  border-radius: 12px;
}

.comment-card :deep(.el-card__body) {
  padding: 20px 24px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.comment-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 20px;
}

.title-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.title-count {
  font-size: 14px;
  color: #909399;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f7fa;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.user-name {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 6px;
}

/* 商家回复 */
.shop-reply {
  background: #f5f7fa;
  border-left: 3px solid #409EFF;
  padding: 8px 12px;
  margin: 8px 0 6px;
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

.comment-actions {
  display: flex;
  gap: 16px;
  font-size: 13px;
}

.like-btn {
  color: #909399;
  cursor: pointer;
  transition: color 0.2s;
}

.like-btn:hover {
  color: #409EFF;
}

.delete-btn {
  color: #f56c6c;
  cursor: pointer;
  transition: color 0.2s;
}

.delete-btn:hover {
  color: #f56c6c;
  text-decoration: underline;
}

.comment-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* ===== 用户回复列表（抖音风格）===== */
.reply-list {
  margin-top: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 14px;
}

.reply-item {
  display: flex;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed #e4e7ed;
}
.reply-item:last-child { border-bottom: none; }

.reply-avatar { flex-shrink: 0; }

.reply-main {
  flex: 1;
  font-size: 13px;
  line-height: 1.8;
  color: #303133;
  word-break: break-all;
}

.reply-author {
  color: #409EFF;
  font-weight: 500;
}
.reply-arrow { color: #909399; margin: 0 4px; }
.reply-target { color: #409EFF; font-weight: 500; }
.reply-content-text { color: #303133; }
.reply-time {
  display: inline-block;
  margin-left: 8px;
  font-size: 12px;
  color: #c0c4cc;
}
.reply-actions {
  display: inline-block;
  margin-left: 8px;
}
.reply-actions span {
  margin-right: 12px;
  font-size: 12px;
  color: #909399;
  cursor: pointer;
  transition: color 0.2s;
}
.reply-like:hover { color: #f56c6c; }
.reply-btn:hover { color: #409EFF; }
.reply-delete:hover { color: #f56c6c; }

.expand-reply-btn,
.collapse-reply-btn {
  margin-top: 8px;
  font-size: 13px;
  color: #409EFF;
  cursor: pointer;
  transition: color 0.2s;
  padding: 4px 0;
}
.expand-reply-btn:hover,
.collapse-reply-btn:hover { color: #66b1ff; }

/* 回复按钮 */
.reply-btn-main {
  cursor: pointer;
  color: #909399;
  font-size: 13px;
  transition: color 0.2s;
}
.reply-btn-main:hover { color: #409EFF; }

/* 回复输入框 */
.reply-box {
  margin-top: 10px;
  padding: 10px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}
.reply-box-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* ===== 响应式 ===== */
.rate-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
@media (max-width: 768px) {
  .book-detail {
    flex-direction: column;
    align-items: center;
  }

  .book-cover-large {
    width: 200px;
    height: 270px;
  }

  .cover-placeholder-large {
    width: 200px;
    height: 270px;
  }

  .book-meta-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== 相关推荐 ===== */
.recommend-section {
  margin-top: 40px;
  padding-top: 32px;
  border-top: 1px solid #f0f0f0;
}
.recommend-header {
  margin-bottom: 20px;
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.recommend-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
}
.recommend-sub {
  font-size: 13px;
  color: #999;
}
.recommend-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
}
@media (max-width: 900px) {
  .recommend-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 480px) {
  .recommend-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
}
.recommend-card {
  cursor: pointer;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  transition: transform .25s ease, box-shadow .25s ease, border-color .25s ease;
}
.recommend-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(0,0,0,.08);
  border-color: #409eff;
}
/* 竖版 2:3 容器，与书籍封面比例匹配 */
.recommend-cover {
  width: 100%;
  aspect-ratio: 2 / 3;
  display: block;
  background: #f5f7fa;
}
.recommend-info {
  padding: 8px 10px 10px;
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
  color: #aaa;
}
</style>
