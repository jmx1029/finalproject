<template>
  <div class="home-page">
    <!-- ===== 轮播图 ===== -->
    <section class="carousel-section" v-if="carouselList.length > 0">
      <el-carousel height="420px" indicator-position="outside">
        <el-carousel-item v-for="item in carouselList" :key="item.id">
          <div class="carousel-slide" :style="{ backgroundImage: 'url(' + getFullUrl(item.bgImageUrl) + ')' }">
            <div class="carousel-overlay">
              <!-- 上层：标题 -->
              <div class="carousel-header">
                <h2>{{ item.title || '好书推荐' }}</h2>
                <p v-if="item.description">{{ item.description }}</p>
              </div>

              <!-- 中层：当前悬停书籍的详情 -->
              <div class="carousel-detail">
                <div class="book-detail-content" v-if="hoveredBook">
                  <h3>{{ hoveredBook.title }}</h3>
                  <p class="book-meta">作者：{{ hoveredBook.author }} | 价格：¥{{ hoveredBook.price }}</p>
                  <p class="book-desc">{{ truncateDesc(hoveredBook.description) }}</p>
                </div>
              </div>

              <!-- 下层：4本书封面 -->
              <div class="carousel-books">
                <div
                  v-for="book in (item.books || [])"
                  :key="book.id"
                  class="book-item"
                  @mouseenter="hoveredBook = book"
                  @mouseleave="hoveredBook = (item.books && item.books.length > 0) ? item.books[0] : null"
                  @click="goToDetail(book.id)"
                >
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
                  <div class="book-name">{{ book.title }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <!-- ===== 精选图书 ===== -->
    <section class="featured-books-section" v-if="featuredBooks.length > 0">
      <div class="section-header">
        <h2>📖 精选图书</h2>
        <router-link to="/category" class="more-link">查看全部 →</router-link>
      </div>
      <el-row :gutter="20">
        <el-col v-for="book in featuredBooks" :key="book.id" :xs="12" :sm="6" :md="4">
          <el-card class="book-card" shadow="hover" @click="goToDetail(book.id)">
            <div class="book-cover-wrapper">
              <el-image :src="getFullUrl(book.coverUrl)" fit="cover" class="book-cover">
                <template #error>
                  <div class="cover-placeholder">📖</div>
                </template>
              </el-image>
            </div>
            <div class="book-info">
              <div class="book-title" :title="book.title">{{ book.title }}</div>
              <div class="book-author">{{ book.author }}</div>
              <div class="book-price">¥{{ book.price }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- ===== 精选分类 ===== -->
    <section class="featured-categories-section" v-if="featuredCategories.length > 0">
      <div class="section-header">
        <h2>⭐ 精选分类</h2>
        <router-link to="/category" class="more-link">查看全部 →</router-link>
      </div>
      <el-row :gutter="20">
        <el-col v-for="(cat, index) in featuredCategories" :key="index" :xs="12" :sm="8" :md="4">
          <div class="category-item" @click="goToCategory(cat.categoryId)">
            <div class="category-image">
              <el-image :src="getFullUrl(cat.imageUrl)" fit="cover">
                <template #error>
                  <div class="cat-placeholder">📂</div>
                </template>
              </el-image>
            </div>
            <div class="category-name">{{ cat.name }}</div>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- ===== 空状态（轮播图无数据时显示） ===== -->
    <div v-if="carouselList.length === 0 && featuredBooks.length === 0 && featuredCategories.length === 0" style="text-align:center;padding:60px 0;">
      <el-empty description="暂无内容，请先配置首页数据" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'

defineOptions({
  name: 'HomePage'
})

const router = useRouter()

// ===== 数据 =====
const carouselList = ref([])
const featuredBooks = ref([])
const featuredCategories = ref([])
const hoveredBook = ref(null)

// ===== 工具函数：获取完整图片URL =====
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
  return desc.length > 60 ? desc.substring(0, 60) + '...' : desc
}

// ===== 加载轮播图 =====
const loadCarousel = async () => {
  try {
    const res = await request.get('/api/home/carousel')
    carouselList.value = res.data || []
    // 默认选中每轮播图的第一本书
    carouselList.value.forEach(item => {
      if (item.books && item.books.length > 0) {
        if (!hoveredBook.value) {
          hoveredBook.value = item.books[0]
        }
      }
    })
  } catch (error) {
  }
}

// ===== 加载精选图书 =====
const loadFeaturedBooks = async () => {
  try {
    const res = await request.get('/api/home/featured-books')
    featuredBooks.value = res.data || []
  } catch (error) {
  }
}

// ===== 加载精选分类 =====
const loadFeaturedCategories = async () => {
  try {
    const res = await request.get('/api/home/featured-categories')
    featuredCategories.value = res.data || []
  } catch (error) {
  }
}

// ===== 跳转方法 =====
const goToDetail = (id) => {
  router.push('/book/' + id)
}

const goToCategory = (categoryId) => {
  if (categoryId) {
    router.push('/category?categoryId=' + categoryId)
  } else {
    router.push('/category')
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadCarousel()
  loadFeaturedBooks()
  loadFeaturedCategories()
})
</script>

<style scoped>
.home-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 24px;
}

/* ===== 轮播图 ===== */
.carousel-section {
  margin-bottom: 40px;
}
.carousel-slide {
  height: 420px;
  background-size: cover;
  background-position: center;
  border-radius: 12px;
  position: relative;
  overflow: hidden;
}
.carousel-slide::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 12px;
}
.carousel-overlay {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 30px 40px 20px;
  color: #fff;
}

.carousel-header h2 {
  font-size: 28px;
  margin-bottom: 6px;
  font-weight: 600;
}
.carousel-header p {
  font-size: 14px;
  opacity: 0.85;
  margin: 0;
}

.carousel-detail {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 10px 0;
}
.book-detail-content {
  max-width: 70%;
}
.book-detail-content h3 {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 6px;
}
.book-detail-content .book-meta {
  font-size: 14px;
  opacity: 0.85;
  margin-bottom: 8px;
}
.book-detail-content .book-desc {
  font-size: 14px;
  opacity: 0.8;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.carousel-books {
  display: flex;
  gap: 20px;
  padding: 10px 0;
}
.book-item {
  width: 100px;
  cursor: pointer;
  text-align: center;
  transition: transform 0.25s, box-shadow 0.25s;
}
.book-item:hover {
  transform: translateY(-6px);
}
.book-item .book-cover {
  width: 100px;
  height: 130px;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  background: rgba(255,255,255,0.1);
}
.book-item .book-name {
  font-size: 13px;
  margin-top: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  opacity: 0.9;
}
.cover-placeholder {
  width: 100px;
  height: 130px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  background: rgba(255,255,255,0.15);
  border-radius: 6px;
}

/* 精选图书 & 精选分类 公共样式 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.section-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}
.more-link {
  color: #409EFF;
  text-decoration: none;
  font-size: 14px;
}

.featured-books-section {
  margin-bottom: 40px;
}
.book-card {
  cursor: pointer;
  transition: transform 0.2s;
  margin-bottom: 16px;
}
.book-card:hover {
  transform: translateY(-4px);
}
.book-cover-wrapper {
  height: 200px;
  overflow: hidden;
  border-radius: 4px;
  background: #f5f7fa;
}
.book-cover {
  width: 100%;
  height: 100%;
}
.book-info {
  padding: 10px 0;
}
.book-title {
  font-weight: 500;
  font-size: 14px;
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
  font-weight: bold;
  font-size: 16px;
}

.featured-categories-section {
  margin-bottom: 40px;
}
.category-item {
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s;
  margin-bottom: 20px;
}
.category-item:hover {
  transform: translateY(-4px);
}
.category-image {
  width: 100%;
  padding-bottom: 100%;
  position: relative;
  overflow: hidden;
  border-radius: 50%;
  background: #f0f2f5;
}
.category-image .el-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
.cat-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  background: #f0f2f5;
}
.category-name {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}
</style>
