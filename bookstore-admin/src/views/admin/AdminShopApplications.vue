<template>
  <div class="admin-shop-applications">
    <div class="page-header">
      <h2>🏪 商家入驻审核</h2>
      <div class="filter-tabs">
        <el-radio-group v-model="filterStatus" size="small" @change="loadData">
          <el-radio-button :label="null">全部</el-radio-button>
          <el-radio-button :label="0">待审核</el-radio-button>
          <el-radio-button :label="1">已通过</el-radio-button>
          <el-radio-button :label="2">已拒绝</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="申请ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="shopName" label="店铺名称" min-width="120" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column label="材料" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="viewMaterials(row)">查看材料</el-button>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button size="small" type="success" @click="review(row.id, 1)">通过</el-button>
              <el-button size="small" type="danger" @click="review(row.id, 2)">拒绝</el-button>
            </template>
            <span v-else style="color:#909399;">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 材料查看弹窗 -->
    <el-dialog v-model="materialsVisible" title="审核材料" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="店铺名称">{{ currentRow?.shopName }}</el-descriptions-item>
        <el-descriptions-item label="店铺简介">{{ currentRow?.description || '无' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentRow?.contactPerson }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentRow?.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="店铺地址">{{ currentRow?.address || '无' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ currentRow?.idCard || '无' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider>证件照片</el-divider>
      <div style="display:flex;gap:16px;flex-wrap:wrap;">
        <div v-if="currentRow?.idCardFront">
          <p style="font-size:12px;color:#909399;">身份证正面</p>
          <el-image :src="getFullUrl(currentRow.idCardFront)" style="width:150px;height:100px;border-radius:4px;border:1px solid #eee;" fit="cover" />
        </div>
        <div v-if="currentRow?.idCardBack">
          <p style="font-size:12px;color:#909399;">身份证反面</p>
          <el-image :src="getFullUrl(currentRow.idCardBack)" style="width:150px;height:100px;border-radius:4px;border:1px solid #eee;" fit="cover" />
        </div>
        <div v-if="currentRow?.businessLicense">
          <p style="font-size:12px;color:#909399;">营业执照</p>
          <el-image :src="getFullUrl(currentRow.businessLicense)" style="width:150px;height:100px;border-radius:4px;border:1px solid #eee;" fit="cover" />
        </div>
      </div>
      <template #footer>
        <el-button @click="materialsVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 审核备注弹窗 -->
    <el-dialog v-model="reviewDialogVisible" title="审核" width="400px">
      <el-form>
        <el-form-item label="备注">
          <el-input v-model="reviewRemark" type="textarea" :rows="3" placeholder="请输入审核备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReview" :loading="reviewLoading">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filterStatus = ref(0)  // 默认显示待审核

const materialsVisible = ref(false)
const currentRow = ref(null)

const reviewDialogVisible = ref(false)
const reviewLoading = ref(false)
const reviewId = ref(null)
const reviewStatus = ref(null)
const reviewRemark = ref('')

const getStatusTagType = (status) => {
  const map = ['warning', 'success', 'danger']
  return map[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/shop/applications', {
      params: {
        page: page.value,
        size: size.value,
        status: filterStatus.value
      }
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const viewMaterials = (row) => {
  currentRow.value = row
  materialsVisible.value = true
}

const review = (id, status) => {
  reviewId.value = id
  reviewStatus.value = status
  reviewRemark.value = ''
  reviewDialogVisible.value = true
}

const confirmReview = async () => {
  reviewLoading.value = true
  try {
    await request.put(`/admin/shop/applications/${reviewId.value}/review`, {
      status: reviewStatus.value,
      remark: reviewRemark.value
    })
    ElMessage.success(reviewStatus.value === 1 ? '审核通过' : '已拒绝')
    reviewDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  } finally {
    reviewLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.admin-shop-applications { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-header h2 { margin: 0; }
.filter-tabs { display: flex; gap: 8px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
