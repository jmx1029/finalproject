<template>
  <div class="shop-book-list">
    <div class="page-header">
      <h2>📚 我的商品</h2>
      <el-button type="primary" @click="openAddDialog">新增商品</el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="书名/作者/ISBN" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable style="width:150px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 商品表格 -->
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="封面" width="90">
          <template #default="{ row }">
            <el-image :src="getFullUrl(row.coverUrl)" style="width:55px;height:75px;border-radius:4px;" fit="cover">
              <template #error><span style="color:#999;">无封面</span></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="90" />
        <el-table-column label="价格" width="90">
          <template #default="{ row }"><span style="color:#F56C6C;">¥{{ row.price }}</span></template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="70" />
        <el-table-column prop="sales" label="销量" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" type="danger" @click="deleteBook(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="searchForm.page"
        v-model:page-size="searchForm.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="600px">
      <el-form :model="bookForm" label-width="80px">
        <el-form-item label="ISBN"><el-input v-model="bookForm.isbn" /></el-form-item>
        <el-form-item label="书名"><el-input v-model="bookForm.title" /></el-form-item>
        <el-form-item label="作者"><el-input v-model="bookForm.author" /></el-form-item>
        <el-form-item label="出版社"><el-input v-model="bookForm.publisher" /></el-form-item>
        <el-form-item label="出版日期">
          <el-date-picker v-model="bookForm.publishDate" type="date" placeholder="选择日期" style="width:200px" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="封面">
          <el-upload :action="uploadUrl + '?type=shop_book' " :headers="uploadHeaders" :show-file-list="false" :on-success="handleUploadSuccess">
            <el-image v-if="bookForm.coverUrl" :src="getFullUrl(bookForm.coverUrl)" style="width:80px;height:100px;border:1px solid #eee;" fit="cover" />
            <el-button v-else>上传封面</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="售价"><el-input-number v-model="bookForm.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="原价"><el-input-number v-model="bookForm.originalPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="bookForm.stock" :min="0" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="bookForm.categoryId" style="width:200px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="bookForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBook" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const categories = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)

const searchForm = reactive({
  page: 1,
  size: 10,
  keyword: '',
  categoryId: null,
  status: null
})

// 字段严格对齐后端 BookCreateDTO / BookUpdateDTO，避免多传字段触发 UnrecognizedPropertyException
const bookForm = reactive({
  id: null,
  title: '',
  author: '',
  isbn: '',
  publisher: '',
  publishDate: '',
  coverUrl: '',
  price: 0,
  originalPrice: 0,
  stock: 0,
  categoryId: null,
  description: ''
})

const uploadUrl = 'http://localhost:8080/api/file/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/shop/book/page', { params: searchForm })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await request.get('/api/category/list')
    categories.value = res.data || []
  } catch (error) {
    // 忽略
  }
}

const resetSearch = () => {
  Object.assign(searchForm, { keyword: '', categoryId: null, status: null, page: 1 })
  loadData()
}

const openAddDialog = () => {
  isEdit.value = false
  Object.assign(bookForm, {
    id: null, title: '', author: '', isbn: '', publisher: '',
    publishDate: '', coverUrl: '', price: 0, originalPrice: 0,
    stock: 0, categoryId: null, description: ''
  })
  dialogVisible.value = true
}

// 关键：只拷贝 DTO 声明的字段，避免把 Entity 的敏感字段（status/sales/rating/shopId/isDeleted 等）带进请求
const openEditDialog = (row) => {
  isEdit.value = true
  bookForm.id = row.id
  bookForm.title = row.title || ''
  bookForm.author = row.author || ''
  bookForm.isbn = row.isbn || ''
  bookForm.publisher = row.publisher || ''
  bookForm.publishDate = row.publishDate || ''
  bookForm.coverUrl = row.coverUrl || ''
  bookForm.price = row.price || 0
  bookForm.originalPrice = row.originalPrice || 0
  bookForm.stock = row.stock || 0
  bookForm.categoryId = row.categoryId || null
  bookForm.description = row.description || ''
  dialogVisible.value = true
}

const handleUploadSuccess = (res) => {
  if (res.code === 200) {
    bookForm.coverUrl = res.data
    ElMessage.success('上传成功')
  }
}

const saveBook = async () => {
  saving.value = true
  try {
    if (isEdit.value) {
      await request.put('/shop/book', bookForm)
    } else {
      await request.post('/shop/book', bookForm)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    await request.put('/shop/book/status', null, { params: { id: row.id, status: newStatus } })
    ElMessage.success('操作成功')
    await loadData()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

const deleteBook = (row) => {
  ElMessageBox.confirm('确定删除这本书吗？', '提示', { type: 'warning' })
    .then(async () => {
      await request.delete(`/shop/book/${row.id}`)
      ElMessage.success('删除成功')
      await loadData()
    })
    .catch(() => {})
}

onMounted(() => {
  loadData()
  loadCategories()
})
</script>

<style scoped>
.shop-book-list { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.search-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
