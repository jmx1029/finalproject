<template>
  <div class="shop-statistics-page">
    <div class="page-header">
      <h2>📊 数据看板</h2>
      <span class="update-time">更新时间：{{ updateTime }}</span>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card class="stat-card sales" shadow="hover">
          <div class="stat-icon">💰</div>
          <div class="stat-info">
            <div class="stat-label">总销售额</div>
            <div class="stat-value">¥{{ formatMoney(overview.totalSales) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card orders" shadow="hover">
          <div class="stat-icon">📦</div>
          <div class="stat-info">
            <div class="stat-label">总订单数</div>
            <div class="stat-value">{{ overview.totalOrders || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card books" shadow="hover">
          <div class="stat-icon">📚</div>
          <div class="stat-info">
            <div class="stat-label">商品总数</div>
            <div class="stat-value">{{ overview.totalBooks || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card warning" shadow="hover">
          <div class="stat-icon">⚠️</div>
          <div class="stat-info">
            <div class="stat-label">库存预警</div>
            <div class="stat-value" :class="{ danger: (overview.lowStockCount || 0) > 0 }">
              {{ overview.lowStockCount || 0 }}
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 今日成交 -->
    <el-row :gutter="20" style="margin-bottom:20px;">
      <el-col :span="24">
        <el-card class="today-card" shadow="hover">
          <template #header>
            <span class="card-title">📅 今日成交（今日 00:00 - 当前）</span>
          </template>
          <div class="today-content">
            <div class="today-item">
              <span class="today-label">今日成交金额</span>
              <span class="today-value sales-color">¥{{ formatMoney(todaySales.todayAmount) }}</span>
            </div>
            <div class="today-divider"></div>
            <div class="today-item">
              <span class="today-label">今日订单数</span>
              <span class="today-value">{{ todaySales.todayOrders || 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热销榜 + 库存预警 -->
    <el-row :gutter="20">
      <el-col :span="14">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span class="card-title">🔥 热销图书 Top10</span>
          </template>
          <el-table :data="hotBooks" stripe v-loading="loading" max-height="400">
            <el-table-column type="index" label="排名" width="60" align="center" />
            <el-table-column label="封面" width="70" align="center">
              <template #default="{ row }">
                <el-image
                  :src="getImageUrl(row.cover_url || row.coverUrl)"
                  style="width:45px;height:60px;border-radius:4px;"
                  fit="cover"
                >
                  <template #error>
                    <div class="image-placeholder">📖</div>
                  </template>
                </el-image>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="书名" min-width="140" show-overflow-tooltip />
            <el-table-column prop="author" label="作者" width="100" />
            <el-table-column prop="sales" label="总销量" width="90" align="center">
              <template #default="{ row }">
                <span style="font-weight:bold;color:#409EFF;">{{ row.sales || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="价格" width="90" align="center">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && hotBooks.length === 0" description="暂无销售数据" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span class="card-title">📋 库存预警（≤10 件）</span>
          </template>
          <el-table :data="stockWarning" stripe v-loading="loading" max-height="400">
            <el-table-column label="封面" width="60" align="center">
              <template #default="{ row }">
                <el-image
                  :src="getImageUrl(row.cover_url || row.coverUrl)"
                  style="width:40px;height:55px;border-radius:4px;"
                  fit="cover"
                >
                  <template #error>
                    <div class="image-placeholder">📖</div>
                  </template>
                </el-image>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="书名" min-width="100" show-overflow-tooltip />
            <el-table-column prop="stock" label="库存" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.stock < 5 ? 'danger' : 'warning'" size="small">
                  {{ row.stock }} 件
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && stockWarning.length === 0" description="✅ 库存充足" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'

defineOptions({ name: 'ShopStatistics' })

// ===== 状态 =====
const loading = ref(false)
const overview = ref({})
const todaySales = ref({})
const hotBooks = ref([])
const stockWarning = ref([])
const updateTime = ref('')

// ===== 获取图片 URL（增强版） =====
const getImageUrl = (url) => {
  if (!url) return ''
  // 如果已经是完整 URL 或 /uploads/ 开头，直接用 getFullUrl 处理
  return getFullUrl(url)
}

// ===== 格式化金额 =====
const formatMoney = (value) => {
  if (value === undefined || value === null) return '0.00'
  return Number(value).toFixed(2)
}

// ===== 加载数据 =====
const loadData = async () => {
  loading.value = true
  try {
    const [ovRes, todayRes, hotRes, stockRes] = await Promise.all([
      request.get('/shop/statistics/overview'),
      request.get('/shop/statistics/today-sales'),
      request.get('/shop/statistics/hot-books?limit=10'),
      request.get('/shop/statistics/stock-warning')
    ])
    overview.value = ovRes.data || {}
    todaySales.value = todayRes.data || {}
    hotBooks.value = hotRes.data || []
    stockWarning.value = stockRes.data || []
    updateTime.value = new Date().toLocaleString('zh-CN')
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.shop-statistics-page {
  padding: 20px;
  background: #f0f2f5;
  min-height: 100vh;
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
  font-weight: 600;
  color: #303133;
}
.update-time {
  font-size: 13px;
  color: #909399;
}

/* 统计卡片 */
.stat-cards {
  margin-bottom: 20px;
}
.stat-card {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-radius: 10px;
  border-left: 4px solid #409EFF;
  transition: transform 0.2s;
}
.stat-card:hover {
  transform: translateY(-4px);
}
.stat-icon {
  font-size: 36px;
  margin-right: 16px;
}
.stat-info {
  flex: 1;
}
.stat-label {
  color: #999;
  font-size: 14px;
  margin-bottom: 4px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.stat-value.danger {
  color: #F56C6C;
}
.sales { border-left-color: #67C23A; }
.sales .stat-value { color: #67C23A; }
.orders { border-left-color: #409EFF; }
.orders .stat-value { color: #409EFF; }
.books { border-left-color: #E6A23C; }
.books .stat-value { color: #E6A23C; }
.warning { border-left-color: #F56C6C; }
.warning .stat-value { color: #F56C6C; }

/* 今日成交 */
.today-card {
  border-radius: 10px;
}
.today-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 60px;
  padding: 12px 0;
}
.today-item {
  text-align: center;
}
.today-label {
  font-size: 14px;
  color: #909399;
  display: block;
  margin-bottom: 6px;
}
.today-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.today-value.sales-color {
  color: #F56C6C;
}
.today-divider {
  width: 1px;
  height: 50px;
  background: #e4e7ed;
}

/* 图表卡片 */
.chart-card {
  border-radius: 10px;
  margin-bottom: 20px;
}
.card-title {
  font-weight: 500;
  font-size: 15px;
  color: #303133;
}

/* 图片占位 */
.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: #f5f7fa;
  border-radius: 4px;
  color: #c0c4cc;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards .el-col {
    margin-bottom: 12px;
  }
  .today-content {
    flex-direction: column;
    gap: 16px;
  }
  .today-divider {
    width: 80%;
    height: 1px;
  }
}
</style>
