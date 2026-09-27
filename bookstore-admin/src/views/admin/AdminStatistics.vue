<template>
  <div class="admin-statistics">
    <div class="page-header">
      <h2>📊 销售统计报表</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <!-- ===== 统计卡片 ===== -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon">📦</div>
          <div class="stat-info">
            <div class="stat-label">总订单数</div>
            <div class="stat-value">{{ statistics.totalOrders || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card sales" shadow="hover">
          <div class="stat-icon">💰</div>
          <div class="stat-info">
            <div class="stat-label">总销售额</div>
            <div class="stat-value">¥{{ statistics.totalSales || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card users" shadow="hover">
          <div class="stat-icon">👤</div>
          <div class="stat-info">
            <div class="stat-label">总用户数</div>
            <div class="stat-value">{{ statistics.totalUsers || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card books" shadow="hover">
          <div class="stat-icon">📚</div>
          <div class="stat-info">
            <div class="stat-label">总图书数</div>
            <div class="stat-value">{{ statistics.totalBooks || 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ===== 近7日销售趋势 ===== -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span class="chart-title">📈 近7日销售趋势</span>
          </template>
          <v-chart class="chart" :option="trendOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- ===== 热门分类 + 热门商品 ===== -->
    <el-row :gutter="20" style="margin-top:20px;">
      <el-col :xs="24" :md="10">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span class="chart-title">🧩 热门分类销量</span>
          </template>
          <v-chart class="chart" :option="pieOption" autoresize />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span class="chart-title">🔥 热门商品 Top10</span>
          </template>
          <el-table :data="statistics.hotBooks || []" stripe size="default">
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="title" label="书名" min-width="140" show-overflow-tooltip />
            <el-table-column prop="author" label="作者" width="100" />
            <el-table-column prop="totalSales" label="总销量" width="100" align="center" />
            <el-table-column label="总销售额" width="120" align="center">
              <template #default="{ row }">
                <span style="color:#F56C6C;">¥{{ row.totalAmount || 0 }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

// 注册 ECharts 组件
use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

// ===== 数据 =====
const statistics = ref({
  totalOrders: 0,
  totalSales: 0,
  totalUsers: 0,
  totalBooks: 0,
  trendData: [],
  categorySales: [],
  hotBooks: []
})

// ===== 近7日趋势图 =====
const trendOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    formatter: function(params) {
      const p = params[0]
      return p.name + '<br/>销售额：¥' + p.value.toLocaleString()
    }
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    top: '6%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: statistics.value.trendData?.map(item => item.date) || [],
    axisLabel: { color: '#666' }
  },
  yAxis: {
    type: 'value',
    axisLabel: {
      formatter: '¥{value}',
      color: '#666'
    },
    splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
  },
  series: [{
    data: statistics.value.trendData?.map(item => item.amount) || [],
    type: 'line',
    smooth: true,
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(64, 158, 255, 0.4)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ]
      }
    },
    lineStyle: { color: '#409EFF', width: 3 },
    itemStyle: { color: '#409EFF' }
  }]
}))

// ===== 分类销量饼图 =====
const pieOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}<br/>销量：{c} 件'
  },
  legend: {
    orient: 'vertical',
    right: '5%',
    top: 'center',
    textStyle: { color: '#666', fontSize: 12 }
  },
  series: [{
    type: 'pie',
    radius: ['45%', '70%'],
    avoidLabelOverlap: true,
    itemStyle: {
      borderRadius: 8,
      borderColor: '#fff',
      borderWidth: 2
    },
    label: { show: false },
    emphasis: {
      label: {
        show: true,
        fontSize: 14,
        fontWeight: 'bold'
      }
    },
    data: statistics.value.categorySales?.filter(item => item.value > 0) || [],
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9B59B6', '#1ABC9C', '#E67E22']
  }]
}))

// ===== 加载数据 =====
const loadStatistics = async () => {
  try {
    const res = await request.get('/admin/order/statistics')
    statistics.value = res.data || {}
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.admin-statistics {
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
  color: #303133;
}

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
.stat-card .stat-icon {
  font-size: 36px;
  margin-right: 16px;
}
.stat-card .stat-info {
  flex: 1;
}
.stat-card .stat-label {
  color: #999;
  font-size: 14px;
  margin-bottom: 4px;
}
.stat-card .stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.stat-card.sales { border-left-color: #67C23A; }
.stat-card.sales .stat-value { color: #67C23A; }
.stat-card.users { border-left-color: #E6A23C; }
.stat-card.users .stat-value { color: #E6A23C; }
.stat-card.books { border-left-color: #9B59B6; }
.stat-card.books .stat-value { color: #9B59B6; }

.chart-card {
  border-radius: 10px;
}
.chart-card :deep(.el-card__header) {
  border-bottom: 1px solid #f0f0f0;
  padding: 14px 20px;
}
.chart-title {
  font-weight: 500;
  font-size: 15px;
  color: #303133;
}
.chart {
  height: 320px;
  width: 100%;
}
</style>
