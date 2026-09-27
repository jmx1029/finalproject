<template>
  <div class="ranking-page">
    <h1 class="page-title">📊 图书排行榜</h1>

    <!-- 榜单切换 -->
    <el-tabs v-model="activeTab" @tab-change="loadRanking" class="ranking-tabs">
      <el-tab-pane label="🔥 畅销榜" name="sales" />
      <el-tab-pane label="⭐ 好评榜" name="rating" />
      <el-tab-pane label="🎯 推荐榜" name="recommend" />
    </el-tabs>

    <!-- 榜单列表 -->
    <div class="ranking-list">
      <div
        v-for="(book, index) in rankingList"
        :key="book.id"
        class="ranking-item"
        @mouseenter="hoveredBook = book"
        @mouseleave="hoveredBook = null"
      >
        <div class="ranking-number" :class="getRankClass(index)">
          {{ index + 1 }}
        </div>
        <el-image
          :src="getFullUrl(book.coverUrl)"
          referrerpolicy="no-referrer"
          class="ranking-cover"
          fit="cover"
          @click="goToDetail(book.id)"
        >
          <template #error>
            <div class="cover-placeholder-small">📖</div>
          </template>
        </el-image>
        <div class="ranking-info" @click="goToDetail(book.id)">
          <div class="ranking-title">{{ book.title }}</div>
          <div class="ranking-author">{{ book.author }}</div>
          <div class="ranking-meta">
            <span v-if="activeTab === 'sales'">销量：{{ book.sales }}</span>
            <span v-if="activeTab === 'rating'">评分：{{ book.rating || '暂无' }}</span>
            <span v-if="activeTab === 'recommend'">综合得分</span>
            <span class="ranking-price">¥{{ book.price }}</span>
          </div>
        </div>
        <el-button
          type="primary"
          size="small"
          :disabled="book.stock <= 0"
          @click.stop="addToCart(book.id)"
        >
          加入购物车
        </el-button>

        <!-- 悬停详情弹窗 -->
        <div
          v-if="hoveredBook && hoveredBook.id === book.id"
          class="ranking-hover-detail"
        >
          <div class="hover-detail-content">
            <h4>{{ book.title }}</h4>
            <p class="hover-author">作者：{{ book.author }}</p>
            <p class="hover-desc">{{ truncateDesc(book.description) }}</p>
            <p class="hover-meta">
              <span>出版社：{{ book.publisher || '未知' }}</span>
              <span v-if="book.publishDate">出版日期：{{ formatDate(book.publishDate) }}</span>
            </p>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!loading && rankingList.length === 0" description="暂无图书" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { getFullUrl } from '../../utils/image'

const router = useRouter()

// ===== 数据 =====
const rankingList = ref([])
const activeTab = ref('sales')
const loading = ref(false)
const hoveredBook = ref(null)

// ===== 获取完整图片URL =====
/*const getFullUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  if (url.startsWith('/uploads/')) {
    return 'http://localhost:8080' + url
  }
  return 'http://localhost:8080' + url
}*/

// ===== 截断描述 =====
const truncateDesc = (desc) => {
  if (!desc) return '暂无简介'
  return desc.length > 80 ? desc.substring(0, 80) + '...' : desc
}

// ===== 格式化日期 =====
const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0')
}

// ===== 获取排名样式 =====
const getRankClass = (index) => {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return ''
}

// ===== 加载排行榜 =====
const loadRanking = async () => {
  loading.value = true
  try {
    const apiMap = {
      sales: '/api/ranking/sales',
      rating: '/api/ranking/rating',
      recommend: '/api/ranking/recommend'
    }
    const res = await request.get(apiMap[activeTab.value])
    rankingList.value = res.data || []
  } catch (error) {
    ElMessage.error('加载排行榜失败')
  } finally {
    loading.value = false
  }
}

// ===== 加入购物车 =====
const addToCart = async (bookId) => {
  try {
    await request.post('/api/cart/add', { bookId, quantity: 1 })
    ElMessage.success('已加入购物车')
  } catch (error) {
    const msg = error.response?.data?.msg || '加入购物车失败'
    ElMessage.error(msg)
  }
}

// ===== 跳转详情 =====
const goToDetail = (id) => {
  router.push('/book/' + id)
}

// ===== 生命周期 =====
onMounted(() => {
  loadRanking()
})
</script>

<style scoped>
.ranking-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px 24px 40px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}

.ranking-tabs {
  margin-bottom: 20px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.25s, transform 0.25s;
  position: relative;
  z-index: 1;
}
.ranking-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateX(4px);
  z-index: 10;
}

.ranking-number {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  color: #909399;
  flex-shrink: 0;
}
.rank-gold {
  color: #f7c948;
}
.rank-silver {
  color: #c0c4cc;
}
.rank-bronze {
  color: #e6a23c;
}

.ranking-cover {
  width: 50px;
  height: 70px;
  border-radius: 4px;
  cursor: pointer;
  flex-shrink: 0;
  background: #f5f7fa;
}
.cover-placeholder-small {
  width: 50px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: #f5f7fa;
  border-radius: 4px;
}

.ranking-info {
  flex: 1;
  cursor: pointer;
  min-width: 0;
}
.ranking-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ranking-author {
  font-size: 13px;
  color: #909399;
}
.ranking-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #606266;
  margin-top: 2px;
}
.ranking-price {
  color: #f56c6c;
  font-weight: 500;
}

/* 悬停详情弹窗 — 显示在卡片下方，不覆盖购物车按钮 */
.ranking-hover-detail {
  position: absolute;
  left: 0;
  right: 0;
  top: 100%;
  margin-top: 4px;
  padding: 14px 18px;
  background: #fafbfc;
  border-radius: 0 0 10px 10px;
  border-top: 1px dashed #e4e7ed;
  z-index: 5;
  animation: slideDown 0.2s ease;
}
@keyframes slideDown {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
.hover-detail-content h4 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
  color: #303133;
}
.hover-detail-content .hover-author {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}
.hover-detail-content .hover-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.hover-detail-content .hover-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  display: flex;
  gap: 16px;
}

@media (max-width: 768px) {
  .ranking-hover-detail {
    display: none;
  }
}
</style>
