<template>
  <div class="category-page">
    <!-- ===== 搜索栏（新增） ===== -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.keyword"
        placeholder="搜索图书、作者、ISBN..."
        size="large"
        clearable
        @input="onSearchInput"
        @keyup.enter="onSearchSubmit"
        @clear="onSearchClear"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button :icon="Search" @click="onSearchSubmit" />
        </template>
      </el-input>
      <!-- 搜索历史 -->
      <div v-if="searchHistory.length > 0 && searchForm.keyword === ''" class="search-history">
        <div class="history-header">
          <span class="history-title">最近搜索</span>
          <el-button type="danger" link size="small" @click="clearHistory">清除全部</el-button>
        </div>
        <div class="history-tags">
          <el-tag
            v-for="(word, index) in searchHistory"
            :key="index"
            size="default"
            closable
            @click="applyHistory(word)"
            @close="removeHistory(index)"
          >
            {{ word }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 分类导航 -->
    <CategoryNav
      :categories="categories"
      v-model="searchForm.categoryId"
      @change="onCategoryChange"
    />

    <!-- 排序筛选栏 -->
    <div class="sort-bar">
      <span class="sort-label">排序：</span>
      <el-radio-group v-model="orderBy" size="small" @change="onOrderChange">
        <el-radio-button value="sales">🔥 热门</el-radio-button>
        <el-radio-button value="publish_date">📅 最新</el-radio-button>
        <el-radio-button value="rating">⭐ 好评</el-radio-button>
      </el-radio-group>
      <span v-if="isSearchMode" class="search-result-info">
        找到 <strong>{{ total }}</strong> 本相关图书
        <el-button type="primary" link size="small" @click="clearSearch">查看全部</el-button>
      </span>
    </div>

    <!-- 图书列表 -->
    <el-row :gutter="20">
      <el-col v-for="book in bookList" :key="book.id" :xs="12" :sm="6" :md="4">
        <el-card class="book-card" shadow="hover" @click="goToDetail(book.id)">
          <div class="book-cover-wrapper">
            <el-image
              :src="getFullUrl(book.coverUrl)"
              referrerpolicy="no-referrer"
              fit="cover"
              class="book-cover"
            >
              <template #error>
                <div class="cover-placeholder">📖</div>
              </template>
            </el-image>
            <div v-if="book.stock <= 0" class="stock-badge">已售罄</div>
          </div>
          <div class="book-info">
            <div class="book-title" :title="book.title">{{ book.title }}</div>
            <div class="book-author">{{ book.author }}</div>
            <div class="book-price">¥{{ book.price }}</div>
            <div class="book-stock">库存：{{ book.stock }} 件</div>
            <el-button
              type="primary"
              size="small"
              style="width:100%;margin-top:8px;"
              :disabled="book.stock <= 0"
              @click.stop="addToCart(book.id)"
            >
              加入购物车
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索无结果 -->
    <el-empty
      v-if="isSearchMode && bookList.length === 0 && !loading"
      description="未找到相关图书，试试其他关键词吧"
      :image-size="120"
    >
      <el-button type="primary" @click="clearSearch">查看全部图书</el-button>
    </el-empty>

    <!-- 分页 -->
    <el-pagination
      v-if="bookList.length > 0"
      class="pagination"
      v-model:current-page="searchForm.page"
      v-model:page-size="searchForm.size"
      :total="total"
      :page-sizes="[12, 24, 48]"
      layout="total, sizes, prev, pager, next"
      @size-change="loadBooks"
      @current-change="loadBooks"
    />

    <!-- 为你推荐（仅在非搜索模式下显示） -->
    <div v-if="!isSearchMode && recommendBooks.length > 0 && isLoggedIn" class="recommend-section">
      <div class="section-title">
        <span class="title-icon">🎯</span>
        <span class="title-text">为你推荐</span>
        <span class="title-sub">基于你的阅读偏好</span>
      </div>
      <el-row :gutter="20">
        <el-col v-for="book in recommendBooks" :key="book.id" :xs="12" :md="6" :lg="4">
          <el-card class="book-card" shadow="hover" @click="goToDetail(book.id)">
            <div class="book-cover-wrapper">
              <el-image
                :src="getFullUrl(book.coverUrl)"
                referrerpolicy="no-referrer"
                fit="cover"
                class="book-cover"
              >
                <template #error>
                  <div class="cover-placeholder">📖</div>
                </template>
              </el-image>
              <div v-if="book.stock <= 0" class="stock-badge">已售罄</div>
            </div>
            <div class="book-info">
              <div class="book-title" :title="book.title">{{ book.title }}</div>
              <div class="book-author">{{ book.author }}</div>
              <div class="book-price">¥{{ book.price }}</div>
              <div class="book-stock">库存：{{ book.stock }} 件</div>
              <el-button
                type="primary"
                size="small"
                style="width:100%;margin-top:8px;"
                :disabled="book.stock <= 0"
                @click.stop="addToCart(book.id)"
              >
                加入购物车
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import CategoryNav from '../../components/CategoryNav.vue'
import { getFullUrl } from '../../utils/image'

const router = useRouter()
const route = useRoute()

// ===== 数据 =====
const bookList = ref([])
const total = ref(0)
const loading = ref(false)
const categories = ref([])
const recommendBooks = ref([])
const orderBy = ref('sales')
const searchHistory = ref([])
const HISTORY_KEY = 'search_history'

const searchForm = reactive({
  page: 1,
  size: 12,
  keyword: '',
  categoryId: null
})

// ===== 计算属性 =====
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const isSearchMode = computed(() => {
  return searchForm.keyword && searchForm.keyword.trim().length > 0
})

// ===== 搜索历史 =====
const loadSearchHistory = () => {
  try {
    const data = localStorage.getItem(HISTORY_KEY)
    searchHistory.value = data ? JSON.parse(data) : []
  } catch {
    searchHistory.value = []
  }
}

const saveSearchHistory = (keyword) => {
  if (!keyword || !keyword.trim()) return
  const trimmed = keyword.trim()
  let history = [...searchHistory.value]
  history = history.filter(item => item !== trimmed)
  history.unshift(trimmed)
  if (history.length > 10) history = history.slice(0, 10)
  searchHistory.value = history
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history))
}

const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem(HISTORY_KEY)
}

const removeHistory = (index) => {
  searchHistory.value.splice(index, 1)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(searchHistory.value))
}

const applyHistory = (keyword) => {
  searchForm.keyword = keyword
  searchForm.page = 1
  loadBooks()
}

// ===== 搜索方法 =====
const onSearchInput = () => {
  // 不立即搜索，等待用户按回车或点击搜索按钮
}

const onSearchSubmit = () => {
  const keyword = searchForm.keyword.trim()
  if (keyword) {
    saveSearchHistory(keyword)
    // 更新 URL
    router.replace({
      path: '/category',
      query: { keyword: keyword }
    })
  }
  searchForm.page = 1
  loadBooks()
}

const onSearchClear = () => {
  searchForm.keyword = ''
  searchForm.page = 1
  // 清除 URL 中的 keyword
  router.replace({
    path: '/category',
    query: { categoryId: searchForm.categoryId || undefined }
  })
  loadBooks()
}

const clearSearch = () => {
  searchForm.keyword = ''
  searchForm.page = 1
  router.replace({
    path: '/category',
    query: { categoryId: searchForm.categoryId || undefined }
  })
  loadBooks()
}

// ===== 加载图书 =====
const loadBooks = async () => {
  loading.value = true
  try {
    const params = {
      page: searchForm.page,
      size: searchForm.size,
      keyword: searchForm.keyword,
      cid: searchForm.categoryId,
      orderBy: orderBy.value
    }
    const res = await request.get('/api/books/page', { params })
    bookList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
  } finally {
    loading.value = false
  }
}

// ===== 加载分类 =====
const loadCategories = async () => {
  try {
    const res = await request.get('/api/category/list')
    categories.value = res.data || []
  } catch (error) {
  }
}

// ===== 加载推荐 =====
const loadRecommend = async () => {
  if (!isLoggedIn.value) return
  try {
    const res = await request.get('/api/recommend')
    recommendBooks.value = res.data || []
  } catch (error) {
  }
}

// ===== 排序变化 =====
const onOrderChange = () => {
  searchForm.page = 1
  loadBooks()
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

// ===== 监听路由变化（处理浏览器前进后退 + 首页分类卡片跳转） =====
watch(
  () => route.query.keyword,
  (newKeyword) => {
    if (newKeyword !== undefined && newKeyword !== searchForm.keyword) {
      searchForm.keyword = newKeyword || ''
      searchForm.page = 1
      loadBooks()
    }
  }
)

// ===== 安全解析 ID（解决 parseInt(undefined) = NaN 的坑）=====
const normalizeId = (val) => {
  if (val === null || val === undefined || val === '') return null
  const n = Number(val)
  return isNaN(n) ? null : n
}

// 监听 categoryId 路由变化
watch(
  () => route.query.categoryId,
  (newCatId) => {
    const parsedId = normalizeId(newCatId)
    if (parsedId !== searchForm.categoryId) {
      searchForm.categoryId = parsedId
      searchForm.page = 1
      loadBooks()
    }
  }
)

// ===== 分类变化 =====
const onCategoryChange = (categoryId) => {
  searchForm.categoryId = categoryId
  searchForm.page = 1
  loadBooks()
  // 同步更新 URL
  router.replace({
    path: '/category',
    query: {
      categoryId: categoryId || undefined,
      keyword: searchForm.keyword || undefined
    }
  })
}

// ===== 生命周期 =====
onMounted(async () => {
  loadSearchHistory()

  // 关键：先加载分类数据，再设置 categoryId
  // 确保 CategoryNav 首次 render 时 categories 和 selectedCategoryId 都正确
  await loadCategories()

  if (route.query.categoryId) {
    searchForm.categoryId = normalizeId(route.query.categoryId)
  }
  if (route.query.keyword) {
    searchForm.keyword = route.query.keyword
  }

  loadBooks()
  loadRecommend()
})
</script>

<style scoped>
.category-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 24px;
}

/* ===== 搜索栏 ===== */
.search-bar {
  margin-bottom: 16px;
  position: relative;
}

.search-history {
  margin-top: 8px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.history-title {
  font-size: 13px;
  color: #909399;
}
.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.history-tags .el-tag {
  cursor: pointer;
  transition: all 0.2s;
}
.history-tags .el-tag:hover {
  color: #409EFF;
  border-color: #409EFF;
}

/* 排序栏 */
.sort-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
}
.sort-label {
  font-size: 14px;
  color: #606266;
}
.search-result-info {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
}
.search-result-info strong {
  color: #409EFF;
}

/* 图书卡片 */
.book-card {
  cursor: pointer;
  transition: transform 0.2s;
  margin-bottom: 16px;
}
.book-card:hover {
  transform: translateY(-4px);
}
.book-cover-wrapper {
  position: relative;
  height: 200px;
  overflow: hidden;
  border-radius: 4px;
  background: #f5f7fa;
}
.book-cover {
  width: 100%;
  height: 100%;
}
.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #909399;
  background: #f5f7fa;
}
.stock-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #f56c6c;
  color: #fff;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
}
.book-info {
  padding-top: 10px;
}
.book-title {
  font-weight: 500;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.book-author {
  color: #909399;
  font-size: 13px;
  margin: 4px 0;
}
.book-price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
}
.book-stock {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

/* 分页 */
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 推荐区域 */
.recommend-section {
  margin-top: 40px;
  padding-top: 30px;
  border-top: 2px solid #f0f0f0;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}
.title-icon {
  font-size: 24px;
}
.title-text {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.title-sub {
  font-size: 14px;
  color: #909399;
  margin-left: 8px;
}
</style>
