<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card class="stat-card sales" shadow="hover">
          <div class="stat-icon">💰</div>
          <div class="stat-info">
            <div class="stat-label">总销售额</div>
            <div class="stat-value">¥{{ overview.totalSales || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card orders" shadow="hover">
          <div class="stat-icon">📦</div>
          <div class="stat-info">
            <div class="stat-label">订单总数</div>
            <div class="stat-value">{{ overview.totalOrders || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card books" shadow="hover">
          <div class="stat-icon">📚</div>
          <div class="stat-info">
            <div class="stat-label">图书总数</div>
            <div class="stat-value">{{ overview.totalBooks || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card warning" shadow="hover">
          <div class="stat-icon">⚠️</div>
          <div class="stat-info">
            <div class="stat-label">库存预警</div>
            <div class="stat-value" :class="{ 'danger': overview.lowStockCount > 0 }">
              {{ overview.lowStockCount || 0 }}
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20">
      <el-col :span="14">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span class="chart-title">📈 月度销售趋势</span>
          </template>
          <v-chart class="chart" :option="trendOption" autoresize />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span class="chart-title">🧩 分类销量占比</span>
          </template>
          <v-chart class="chart" :option="pieOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- 库存预警表格 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span class="chart-title">📋 库存预警 Top10</span>
          </template>
          <el-table :data="stockWarning" stripe v-if="stockWarning.length > 0">
            <el-table-column prop="title" label="图书名称" />
            <el-table-column prop="stock" label="剩余库存" width="150" align="center">
              <template #default="{ row }">
                <el-tag :type="row.stock < 5 ? 'danger' : 'warning'" size="large">
                  {{ row.stock }} 件
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无库存预警，库存充足 ✅" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import request from '../../utils/request'

// 注册 ECharts 组件
use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

// ---------- 数据 ----------
const overview = ref({})
const trendData = ref({ months: [], values: [] })
const categoryData = ref([])
const stockWarning = ref([])

// ---------- 月度趋势折线图配置 ----------
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
    outerBounds: { left: '3%', right: '4%', bottom: '3%', top: '6%' }
  },
  xAxis: {
    type: 'category',
    data: trendData.value.months,
    axisLine: { lineStyle: { color: '#e0e0e0' } },
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
    data: trendData.value.values,
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
    itemStyle: { color: '#409EFF' },
    markPoint: {
      data: [
        { type: 'max', name: '最大值' },
        { type: 'min', name: '最小值' }
      ]
    }
  }]
}))

// ---------- 分类销量饼图配置 ----------
const pieOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}<br/>销量：{c} 件<br/>占比：{d}%'
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
    label: {
      show: false
    },
    emphasis: {
      label: {
        show: true,
        fontSize: 14,
        fontWeight: 'bold'
      }
    },
    data: categoryData.value,
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9B59B6', '#1ABC9C', '#E67E22']
  }]
}))

// ---------- 加载数据 ----------
const loadData = async () => {
  try {
    const [ov, trend, cat, stock] = await Promise.all([
      request.get('/admin/dashboard/overview'),
      request.get('/admin/dashboard/trend?year=2026'),
      request.get('/admin/dashboard/category-ratio'),
      request.get('/admin/dashboard/stock-warning')
    ])
    overview.value = ov.data || {}
    trendData.value = trend.data || { months: [], values: [] }
    categoryData.value = cat.data || []
    stockWarning.value = stock.data || []
  } catch (error) {
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
  background: #f0f2f5;
  min-height: 100vh;
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

/* 图表卡片 */
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
  height: 350px;
  width: 100%;
}
</style>
