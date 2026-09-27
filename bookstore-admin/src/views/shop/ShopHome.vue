<template>
  <div class="shop-home-page" v-loading="loading">
    <!-- ===== 店铺信息头部 ===== -->
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
          <p class="shop-description">{{ shop.description || '这家店还没有简介哦~' }}</p>
          <div class="shop-contact">
            <span v-if="shop.contactPerson">👤 {{ shop.contactPerson }}</span>
            <span v-if="shop.contactPhone">📞 {{ shop.contactPhone }}</span>
            <span v-if="shop.address">📍 {{ shop.address }}</span>
          </div>
        </div>
        <div class="shop-actions">
          <!-- ===== 修复：使用 shop 对象，而非 order ===== -->
          <el-button
            v-if="shop && shop.id && shop.id !== 1"
            type="info"
            size="large"
            plain
            @click="contactShop"
          >
            💬 联系卖家
          </el-button>
          <el-button
            v-else-if="!isLoggedIn"
            type="primary"
            size="large"
            @click="$router.push('/login')"
          >
            登录后联系卖家
          </el-button>
          <el-tag v-else-if="isOwner" type="success" size="large">
            👑 这是我的店铺
          </el-tag>
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

    <!-- ===== 加载失败 ===== -->
    <el-empty v-if="!loading && !shop" description="店铺不存在或已被关闭">
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request.js'
import { getFullUrl } from '../../utils/image.js'
import { contactSeller } from '../../utils/chat.js'

const route = useRoute()
const router = useRouter()

// ===== 状态 =====
const loading = ref(false)
const shop = ref(null)
const books = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(8)

// ===== 计算属性 =====
const shopId = computed(() => route.params.shopId)

const isLoggedIn = computed(() => {
  return !!localStorage.getItem('token')
})

const userInfo = computed(() => {
  return JSON.parse(localStorage.getItem('userInfo') || '{}')
})

const isOwner = computed(() => {
  if (!shop.value || !userInfo.value.userId) return false
  return shop.value.userId === userInfo.value.userId
})

// ===== 加载店铺信息 =====
const loadShop = async () => {
  if (!shopId.value) {
    ElMessage.error('店铺ID不存在')
    return
  }
  try {
    const res = await request.get(`/api/shop/public/${shopId.value}`)
    if (res.data) {
      shop.value = res.data
    } else {
      shop.value = null
    }
  } catch (error) {
    shop.value = null
    ElMessage.error('加载店铺信息失败')
  }
}

// ===== 加载商品列表 =====
const loadBooks = async () => {
  if (!shopId.value) return
  loading.value = true
  try {
    const res = await request.get(`/api/shop/public/${shopId.value}/books`, {
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

// ===== 联系卖家 =====
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

// ===== 监听 shopId 变化 =====
watch(shopId, () => {
  page.value = 1
  loadShop()
  loadBooks()
}, { immediate: true })

// ===== 生命周期 =====
onMounted(() => {
  if (shopId.value) {
    loadShop()
    loadBooks()
  }
})
</script>

<style scoped>
.shop-home-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px 40px;
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
