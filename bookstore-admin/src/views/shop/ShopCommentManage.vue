<template>
  <div class="shop-comment-manage">
    <div class="page-header">
      <h2>⭐ 评价管理</h2>
      <span class="total-tip">共 {{ total }} 条评价</span>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="评分">
          <el-select v-model="searchForm.rating" placeholder="全部评分" clearable style="width:130px" @change="handleSearch">
            <el-option label="好评 (≥4星)" :value="4" />
            <el-option label="差评 (≤2星)" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索评价内容"
            clearable
            style="width:200px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 评价列表 -->
    <el-card>
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="comments.length === 0" class="empty-container">
        <el-empty description="暂无评价" />
      </div>

      <div v-else class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <!-- 用户信息 -->
          <div class="comment-header">
            <div class="user-info">
              <el-avatar :size="36" :src="getFullUrl(comment.avatar)">
                {{ comment.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="user-name">{{ comment.nickname || comment.username || '匿名用户' }}</span>
              <el-rate v-model="comment.rating" disabled size="small" :max="10" :step="2" />
              <el-tag :type="getRatingTagType(comment.rating)" size="small">
                {{ getRatingText(comment.rating) }}
              </el-tag>
            </div>
            <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
          </div>

          <!-- 评价内容 -->
          <div class="comment-body">
            <div class="book-info" @click="goBookDetail(comment.bookId)">
              <el-image
                :src="getFullUrl(comment.bookCover)"
                style="width:40px;height:55px;border-radius:4px;"
                fit="cover"
              >
                <template #error>
                  <span style="font-size:12px;color:#999;">📖</span>
                </template>
              </el-image>
              <span class="book-title">{{ comment.bookTitle || '图书已下架' }}</span>
            </div>
            <p class="comment-content">{{ comment.content }}</p>

            <!-- 商家回复 -->
            <div v-if="comment.replyContent" class="reply-area">
              <div class="reply-label">📝 商家回复：</div>
              <div class="reply-content">{{ comment.replyContent }}</div>
              <div class="reply-time">回复于 {{ formatTime(comment.replyTime) }}</div>
            </div>

            <!-- 回复输入框 -->
            <div v-else class="reply-input-area">
              <el-input
                v-model="replyForms[comment.id]"
                type="textarea"
                :rows="2"
                placeholder="输入回复内容..."
                maxlength="500"
                show-word-limit
              />
              <el-button
                type="primary"
                size="small"
                :loading="replyingIds.includes(comment.id)"
                @click="submitReply(comment.id)"
              >
                回复
              </el-button>
            </div>

            <!-- 已回复的编辑（可修改） -->
            <div v-if="comment.replyContent" class="reply-edit-area">
              <el-button type="primary" link size="small" @click="editReply(comment.id)">
                修改回复
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <el-pagination
        class="pagination"
        v-model:current-page="searchForm.page"
        v-model:page-size="searchForm.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadComments"
        @current-change="loadComments"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'

defineOptions({ name: 'ShopCommentManage' })

const router = useRouter()

// ===== 状态 =====
const loading = ref(false)
const comments = ref([])
const total = ref(0)
const replyingIds = ref([])
const replyForms = ref({})

// ===== 搜索表单 =====
const searchForm = reactive({
  page: 1,
  size: 10,
  rating: null,
  keyword: ''
})

// ===== 评分相关（10分制）=====
const getRatingText = (rating) => {
  if (rating >= 8) return '好评'   // 原 >=4 星 ×2
  if (rating >= 6) return '中评'   // 原 >=3 星 ×2
  return '差评'                    // <6 分
}

const getRatingTagType = (rating) => {
  if (rating >= 8) return 'success'
  if (rating >= 6) return 'warning'
  return 'danger'
}

// ===== 格式化时间 =====
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
}

// ===== 加载评价列表 =====
const loadComments = async () => {
  loading.value = true
  try {
    const params = {
      page: searchForm.page,
      size: searchForm.size
    }
    if (searchForm.rating !== null && searchForm.rating !== '') {
      params.rating = searchForm.rating
    }
    if (searchForm.keyword) {
      params.keyword = searchForm.keyword
    }
    const res = await request.get('/shop/comment/page', { params })
    comments.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载评价列表失败')
  } finally {
    loading.value = false
  }
}

// ===== 搜索/重置 =====
const handleSearch = () => {
  searchForm.page = 1
  loadComments()
}

const resetSearch = () => {
  searchForm.rating = null
  searchForm.keyword = ''
  searchForm.page = 1
  loadComments()
}

// ===== 提交回复 =====
const submitReply = async (commentId) => {
  const content = replyForms.value[commentId]?.trim()
  if (!content) {
    ElMessage.warning('请输入回复内容')
    return
  }

  replyingIds.value.push(commentId)
  try {
    await request.put(`/shop/comment/reply/${commentId}?content=${encodeURIComponent(content)}`)
    ElMessage.success('回复成功')
    replyForms.value[commentId] = ''
    await loadComments()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '回复失败')
  } finally {
    replyingIds.value = replyingIds.value.filter(id => id !== commentId)
  }
}

// ===== 修改回复 =====
const editReply = (commentId) => {
  const comment = comments.value.find(c => c.id === commentId)
  if (comment) {
    replyForms.value[commentId] = comment.replyContent
    // 触发展开输入框（通过修改数据重新渲染）
    comment.replyContent = null
    comment.replyTime = null
  }
}

// ===== 跳转图书详情 =====
const goBookDetail = (bookId) => {
  if (bookId) {
    router.push(`/book/${bookId}`)
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadComments()
})
</script>

<style scoped>
.shop-comment-manage {
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
  font-size: 24px;
}
.total-tip {
  font-size: 14px;
  color: #909399;
}

.search-card {
  margin-bottom: 16px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  padding: 16px 20px;
  background: #fafafa;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  transition: border-color 0.2s;
}
.comment-item:hover {
  border-color: #d0d0d0;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-name {
  font-weight: 500;
  color: #303133;
}
.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-body {
  padding-left: 46px;
}
.book-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  margin-bottom: 8px;
}
.book-title {
  font-size: 14px;
  color: #409EFF;
  transition: color 0.2s;
}
.book-info:hover .book-title {
  color: #66b1ff;
  text-decoration: underline;
}

.comment-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  margin: 0 0 10px 0;
}

.reply-area {
  background: #f0f5ff;
  border-radius: 6px;
  padding: 10px 14px;
  margin-top: 8px;
}
.reply-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}
.reply-content {
  font-size: 14px;
  color: #303133;
  padding: 4px 0;
}
.reply-time {
  font-size: 12px;
  color: #909399;
}

.reply-input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-top: 10px;
}
.reply-input-area .el-textarea {
  flex: 1;
}
.reply-input-area .el-button {
  height: 40px;
  flex-shrink: 0;
}

.reply-edit-area {
  margin-top: 6px;
}

.loading-container {
  padding: 30px 0;
}
.empty-container {
  padding: 40px 0;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
