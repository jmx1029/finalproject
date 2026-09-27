<template>
  <div class="admin-featured-books">
    <div class="page-header">
      <h2>⭐ 精选图书管理</h2>
      <el-button type="primary" @click="saveConfig">保存配置</el-button>
    </div>

    <el-card>
      <div class="config-description">
        <p>精选图书：在首页「精选图书」区域展示，最多 <strong>8 本</strong>。</p>
        <p style="color:#909399;font-size:13px;">拖拽可调整顺序，点击「×」移除图书。</p>
      </div>

      <div class="selected-books">
        <div
          v-for="(book, index) in selectedBooks"
          :key="book.id"
          class="selected-book-item"
        >
          <span class="book-index">{{ index + 1 }}</span>
          <el-image
            :src="getFullUrl(book.coverUrl)"
            referrerpolicy="no-referrer"
            style="width:40px;height:55px;border-radius:4px;"
            fit="cover"
          >
            <template #error><span style="font-size:12px;">无图</span></template>
          </el-image>
          <span class="book-title">{{ book.title }}</span>
          <span class="book-author">{{ book.author }}</span>
          <el-button type="danger" size="small" circle @click="removeBook(index)">×</el-button>
        </div>
        <div v-if="selectedBooks.length === 0" style="color:#909399;padding:20px;text-align:center;">
          暂无精选图书，请从下方添加
        </div>
      </div>

      <el-divider />

      <div class="add-section">
        <el-select
          v-model="selectedBookId"
          placeholder="选择图书添加"
          filterable
          style="width:300px;"
          @change="addBook"
        >
          <el-option
            v-for="book in availableBooks"
            :key="book.id"
            :label="book.title + ' - ' + book.author"
            :value="book.id"
          />
        </el-select>
        <span style="color:#909399;font-size:13px;margin-left:12px;">
          已选 {{ selectedBooks.length }}/8 本
        </span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const selectedBooks = ref([])
const allBooks = ref([])
const selectedBookId = ref(null)

const getFullUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/uploads/') ? 'http://localhost:8080' + url : url
}

const availableBooks = computed(() => {
  const selectedIds = selectedBooks.value.map(b => b.id)
  return allBooks.value.filter(b => !selectedIds.includes(b.id))
})

const loadData = async () => {
  try {
    // 加载当前精选图书
    const res = await request.get('/admin/featured-books')
    selectedBooks.value = res.data || []

    // 加载所有可用的图书
    const allRes = await request.get('/admin/featured-books/available')
    allBooks.value = allRes.data || []
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const addBook = (bookId) => {
  if (!bookId) return
  if (selectedBooks.value.length >= 8) {
    ElMessage.warning('最多只能选择8本精选图书')
    return
  }
  const book = allBooks.value.find(b => b.id === bookId)
  if (book) {
    selectedBooks.value.push(book)
    selectedBookId.value = null
  }
}

const removeBook = (index) => {
  selectedBooks.value.splice(index, 1)
}

const saveConfig = async () => {
  if (selectedBooks.value.length === 0) {
    ElMessage.warning('请至少选择一本精选图书')
    return
  }
  try {
    const bookIds = selectedBooks.value.map(b => b.id)
    await request.put('/admin/featured-books', { bookIds })
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.admin-featured-books { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.config-description { margin-bottom: 16px; padding: 12px 16px; background: #f5f7fa; border-radius: 6px; }

.selected-books { display: flex; flex-wrap: wrap; gap: 12px; padding: 8px 0; }
.selected-book-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px 6px 6px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fafafa;
}
.book-index {
  width: 24px;
  text-align: center;
  font-weight: 500;
  color: #909399;
  font-size: 13px;
}
.selected-book-item .book-title {
  font-size: 14px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.selected-book-item .book-author {
  font-size: 12px;
  color: #909399;
}

.add-section { display: flex; align-items: center; padding: 8px 0; }
</style>
