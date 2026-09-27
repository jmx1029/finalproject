<template>
  <div class="admin-home-manage">
    <div class="page-header">
      <h2>🏠 首页管理</h2>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="📸 轮播图管理" name="carousel">
          <AdminCarousel />
        </el-tab-pane>
        <el-tab-pane label="⭐ 精选分类" name="categories">
          <AdminHomeConfig />
        </el-tab-pane>
        <el-tab-pane label="⭐ 精选图书" name="books">
          <AdminFeaturedBooks />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AdminCarousel from './AdminCarousel.vue'
import AdminHomeConfig from './AdminHomeConfig.vue'
import AdminFeaturedBooks from './AdminFeaturedBooks.vue'

defineOptions({ name: 'AdminHomeManage' })

const activeTab = ref('carousel')
</script>

<style scoped>
.admin-home-manage { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
</style>
