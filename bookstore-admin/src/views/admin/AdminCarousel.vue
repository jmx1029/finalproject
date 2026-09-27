<template>
  <div class="admin-carousel">
    <div class="page-header">
      <h2>📸 轮播图管理</h2>
      <el-button type="primary" @click="openAddDialog">新增轮播图</el-button>
    </div>

    <el-card>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column label="排序" width="80">
          <template #default="{ row }">
            <el-input-number v-model="row.sort" size="small" :min="0" @change="updateSort(row)" />
          </template>
        </el-table-column>
        <el-table-column label="背景图" width="120">
          <template #default="{ row }">
            <el-image :src="getFullUrl(row.bgImageUrl)" style="width:80px;height:50px;border-radius:4px;" fit="cover">
              <template #error><span style="color:#999;">无图</span></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="100">
          <template #default="{ row }">{{ row.title || '无标题' }}</template>
        </el-table-column>
        <el-table-column label="关联书籍" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="id in getBookIdList(row.bookIds)" :key="id" size="small" style="margin:2px;">
              ID:{{ id }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑轮播图' : '新增轮播图'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="请输入标题" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" placeholder="请输入描述" /></el-form-item>

        <el-form-item label="背景图">
          <el-upload
            :action="uploadUrl + '?type=carousel'"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="(res) => { form.bgImageUrl = res.data; ElMessage.success('上传成功') }"
          >
            <el-image v-if="form.bgImageUrl" :src="getFullUrl(form.bgImageUrl)" style="width:120px;height:70px;border:1px solid #eee;" fit="cover" />
            <el-button v-else>上传背景图</el-button>
          </el-upload>
        </el-form-item>

        <el-form-item label="关联书籍">
          <div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:8px;">
            <el-tag
              v-for="book in selectedBooks"
              :key="book.id"
              closable
              @close="removeBook(book.id)"
              size="large"
            >
              {{ book.title }}
            </el-tag>
          </div>
          <el-select
            v-model="selectedBookId"
            placeholder="选择书籍（最多4本）"
            filterable
            style="width:100%"
            :disabled="selectedBooks.length >= 4"
            @change="addBook"
          >
            <el-option
              v-for="book in availableBooks"
              :key="book.id"
              :label="book.title + ' - ' + book.author"
              :value="book.id"
            />
          </el-select>
          <span style="color:#909399;font-size:12px;">已选 {{ selectedBooks.length }}/4 本</span>
        </el-form-item>

        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCarousel" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const allBooks = ref([])
const selectedBooks = ref([])
const selectedBookId = ref(null)

const form = reactive({
  id: null,
  title: '',
  description: '',
  bgImageUrl: '',
  bookIds: '',
  sort: 0,
  status: 1
})

const uploadUrl = 'http://localhost:8080/api/file/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const getFullUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/uploads/') ? 'http://localhost:8080' + url : url
}

const getBookIdList = (ids) => {
  if (!ids) return []
  return ids.split(',').filter(id => id.trim())
}

const availableBooks = computed(() => {
  const selectedIds = selectedBooks.value.map(b => b.id)
  return allBooks.value.filter(b => !selectedIds.includes(b.id))
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/carousel/list')
    list.value = res.data || []
  } finally { loading.value = false }
}

const loadAllBooks = async () => {
  try {
    const res = await request.get('/admin/book/page', { params: { page: 1, size: 999 } })
    allBooks.value = res.data.records || []
  } catch { /* 忽略 */ }
}

const openAddDialog = () => {
  isEdit.value = false
  Object.assign(form, { id: null, title: '', description: '', bgImageUrl: '', bookIds: '', sort: 0, status: 1 })
  selectedBooks.value = []
  selectedBookId.value = null
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  // 解析 bookIds 为 selectedBooks
  selectedBooks.value = []
  if (row.bookIds) {
    const ids = row.bookIds.split(',').filter(id => id.trim())
    ids.forEach(id => {
      const book = allBooks.value.find(b => b.id === parseInt(id))
      if (book) selectedBooks.value.push(book)
    })
  }
  selectedBookId.value = null
  dialogVisible.value = true
}

const addBook = (bookId) => {
  if (!bookId) return
  const book = allBooks.value.find(b => b.id === bookId)
  if (book) {
    selectedBooks.value.push(book)
  }
  selectedBookId.value = null
}

const removeBook = (bookId) => {
  selectedBooks.value = selectedBooks.value.filter(b => b.id !== bookId)
}

const saveCarousel = async () => {
  if (!selectedBooks.value.length) {
    ElMessage.warning('请至少选择一本关联书籍')
    return
  }
  form.bookIds = selectedBooks.value.map(b => b.id).join(',')

  saving.value = true
  try {
    if (isEdit.value) await request.put('/admin/carousel', form)
    else await request.post('/admin/carousel', form)
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    await loadData()
  } catch { /* 已由拦截器处理 */ }
  finally { saving.value = false }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  await request.put(`/admin/carousel/status/${row.id}?status=${newStatus}`)
  ElMessage.success('操作成功')
  await loadData()
}

const updateSort = async (row) => {
  await request.put('/admin/carousel', row)
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除吗？', '提示', { type: 'warning' })
    await request.delete(`/admin/carousel/${id}`)
    ElMessage.success('删除成功')
    await loadData()
  } catch { /* 取消 */ }
}

onMounted(() => {
  loadAllBooks()
  loadData()
})
</script>

<style scoped>
.admin-carousel { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
</style>
