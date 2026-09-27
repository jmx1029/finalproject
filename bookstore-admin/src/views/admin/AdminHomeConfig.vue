<template>
  <div class="admin-home-config">
    <div class="page-header">
      <h2>🏠 首页配置</h2>
      <el-button type="primary" @click="addCategory">添加分类</el-button>
    </div>

    <el-card>
      <div class="config-description">
        <p>精选分类：在首页展示 6 个分类入口，用户点击可跳转到对应分类。</p>
        <p style="color:#909399;font-size:13px;">拖拽可调整顺序，点击分类名称可跳转。</p>
      </div>
      <el-table :data="categories" stripe>
        <el-table-column label="序号" width="60" type="index" />
        <el-table-column label="分类名称" min-width="120">
          <template #default="{ row }">
            <el-select v-model="row.categoryId" placeholder="选择分类" filterable @change="saveConfig">
              <el-option
                v-for="c in allCategories"
                :key="c.id"
                :label="c.name"
                :value="c.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="分类图标" width="120">
          <template #default="{ row }">
            <el-upload
              :action="uploadUrl+'?type=home'"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="(res) => { row.imageUrl = res.data; saveConfig() }"
            >
              <el-image v-if="row.imageUrl" :src="getFullUrl(row.imageUrl)" style="width:50px;height:50px;border-radius:8px;" fit="cover" />
              <el-button v-else size="small">上传图标</el-button>
            </el-upload>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ $index }">
            <el-button type="danger" size="small" @click="removeCategory($index)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right;">
        <el-button type="primary" @click="saveConfig">保存配置</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const categories = ref([])
const allCategories = ref([])

const uploadUrl = 'http://localhost:8080/api/file/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const getFullUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/uploads/') ? 'http://localhost:8080' + url : url
}

const loadConfig = async () => {
  try {
    const res = await request.get('/admin/home-config/featured-categories')
    const data = res.data || '[]'
    categories.value = JSON.parse(data)
    if (!categories.value.length) {
      categories.value = [{ categoryId: null, imageUrl: '' }]
    }
  } catch { /* 忽略 */ }
}

const loadAllCategories = async () => {
  const res = await request.get('/api/category/list')
  // 只显示二级分类（parent_id != 0）
  allCategories.value = (res.data || []).filter(c => c.parentId !== 0)
}

const addCategory = () => {
  categories.value.push({ categoryId: null, imageUrl: '' })
}

const removeCategory = (index) => {
  categories.value.splice(index, 1)
}

const saveConfig = async () => {
  const valid = categories.value.filter(c => c.categoryId)
  if (!valid.length) {
    ElMessage.warning('请至少选择一个分类')
    return
  }
  try {
    await request.put('/admin/home-config/featured-categories', {
      configValue: JSON.stringify(valid)
    })
    ElMessage.success('保存成功')
  } catch { /* 已由拦截器处理 */ }
}

onMounted(() => {
  loadAllCategories()
  loadConfig()
})
</script>

<style scoped>
.admin-home-config { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.config-description { margin-bottom: 16px; padding: 12px 16px; background: #f5f7fa; border-radius: 6px; }
</style>
