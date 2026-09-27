<template>
  <div class="shop-profile-page">
    <el-card class="profile-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🏪 店铺设置</span>
          <div class="header-actions">
            <el-button type="info" plain @click="openPreviewDialog">
              👁️ 预览店铺
            </el-button>
            <el-button type="primary" @click="handleSave" :loading="saving">保存设置</el-button>
          </div>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="shop-form"
      >
        <!-- 店铺头像 -->
        <el-form-item label="店铺头像" prop="logo">
          <el-upload
            class="avatar-uploader"
            :action="uploadUrl + '?type=shop_logo'"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
          >
            <img v-if="form.logo" :src="getFullUrl(form.logo)" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议尺寸 200x200，支持 JPG/PNG，大小不超过 2MB</div>
        </el-form-item>

        <!-- 店铺名称 -->
        <el-form-item label="店铺名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入店铺名称" maxlength="50" />
        </el-form-item>

        <!-- 店铺公告（新增） -->
        <el-form-item label="店铺公告" prop="announcement">
          <el-input
            v-model="form.announcement"
            type="textarea"
            :rows="2"
            placeholder="输入店铺公告，展示在店铺主页顶部"
            maxlength="500"
            show-word-limit
          />
          <div class="field-tip">💡 公告将展示在店铺主页的显著位置，让买家第一时间了解店铺动态</div>
        </el-form-item>

        <!-- 店铺简介 -->
        <el-form-item label="店铺简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="介绍一下你的店铺吧"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 联系人 -->
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人姓名" />
        </el-form-item>

        <!-- 联系电话 -->
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>

        <!-- 店铺地址 -->
        <el-form-item label="店铺地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入店铺地址" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ===== 店铺预览弹窗 ===== -->
    <el-dialog
      v-model="previewDialogVisible"
      title="店铺预览"
      width="90%"
      :fullscreen="true"
      top="0"
      class="preview-dialog"
      destroy-on-close
    >
      <UserShopHome
        v-if="previewDialogVisible"
        :shop-id="shopId"
        :preview-mode="true"
        @close="previewDialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '../../utils/request.js'
import { getFullUrl } from '../../utils/image.js'
import UserShopHome from '../front/UserShopHome.vue'

defineOptions({ name: 'ShopProfilePage' })

// ===== 状态 =====
const formRef = ref()
const saving = ref(false)
const previewDialogVisible = ref(false)
const uploadUrl = 'http://localhost:8080/api/file/upload'

// ===== 当前店铺ID（用于预览） =====
const shopId = ref(null)

// ===== 表单数据 =====
const form = reactive({
  name: '',
  logo: '',
  announcement: '',   // 新增
  description: '',
  contactPerson: '',
  contactPhone: '',
  address: ''
})

// ===== 表单校验规则 =====
const rules = {
  name: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }]
}

// ===== 上传配置 =====
const uploadHeaders = {
  Authorization: 'Bearer ' + localStorage.getItem('token')
}

// ===== 头像上传成功回调 =====
const handleAvatarSuccess = (res) => {
  if (res.code === 200) {
    form.logo = res.data
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

// ===== 上传前校验 =====
const beforeAvatarUpload = (file) => {
  const isJPGorPNG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isJPGorPNG) ElMessage.error('头像只能为 JPG/PNG 格式')
  if (!isLt2M) ElMessage.error('头像大小不能超过 2MB')
  return isJPGorPNG && isLt2M
}

// ===== 加载店铺信息 =====
const loadShopInfo = async () => {
  try {
    const res = await request.get('/api/shop/info')
    if (res.data) {
      form.name = res.data.name || ''
      form.logo = res.data.logo || ''
      form.announcement = res.data.announcement || ''   // 新增
      form.description = res.data.description || ''
      form.contactPerson = res.data.contactPerson || ''
      form.contactPhone = res.data.contactPhone || ''
      form.address = res.data.address || ''
      shopId.value = res.data.id
    }
  } catch (error) {
    ElMessage.error('加载店铺信息失败')
  }
}

// ===== 保存设置 =====
const handleSave = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    await request.put('/api/shop/update', form)
    ElMessage.success('店铺信息更新成功')
    await loadShopInfo()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '更新失败')
  } finally {
    saving.value = false
  }
}

// ===== 打开预览弹窗 =====
const openPreviewDialog = () => {
  if (!shopId.value) {
    ElMessage.warning('请先保存店铺信息')
    return
  }
  previewDialogVisible.value = true
}

// ===== 生命周期 =====
onMounted(() => {
  loadShopInfo()
})
</script>

<style scoped>
.shop-profile-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.profile-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.shop-form {
  margin-top: 10px;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}
.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}
.avatar-uploader .avatar {
  width: 120px;
  height: 120px;
  display: block;
  border-radius: 8px;
  object-fit: cover;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
  line-height: 120px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.field-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.6;
}

/* ===== 预览弹窗样式 ===== */
.preview-dialog :deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
}
.preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}
</style>
