<template>
  <div class="apply-shop-page">
    <el-card class="apply-card">
      <h2 class="page-title">🏪 申请成为商家</h2>

      <!-- 状态展示 -->
      <div v-if="applicationStatus !== null" class="status-section">
        <el-alert
          :type="statusAlertType"
          :title="statusAlertTitle"
          :description="statusAlertDesc"
          show-icon
          :closable="false"
        />
      </div>

      <!-- 申请表单 -->
      <el-form
        v-if="applicationStatus === null || applicationStatus === 2"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="apply-form"
      >
        <el-form-item label="店铺名称" prop="shopName">
          <el-input v-model="form.shopName" placeholder="请输入店铺名称" maxlength="50" />
        </el-form-item>

        <el-form-item label="店铺简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请介绍一下你的店铺" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人姓名" />
        </el-form-item>

        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>

        <el-form-item label="店铺地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入店铺地址" />
        </el-form-item>

        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" />
        </el-form-item>

        <!-- ===== 修改：身份证正面（使用 :auto-upload="false"） ===== -->
        <el-form-item label="身份证正面" prop="idCardFrontFile">
          <el-upload
            ref="idCardFrontUpload"
            :auto-upload="false"
            :on-change="(file) => { form.idCardFrontFile = file.raw }"
            :on-remove="() => { form.idCardFrontFile = null }"
            accept="image/*"
            :limit="1"
          >
            <el-button>选择身份证正面</el-button>
            <template #tip>
              <div style="font-size:12px;color:#909399;">支持 JPG/PNG 格式</div>
            </template>
          </el-upload>
          <span v-if="form.idCardFrontFile" style="color:#67C23A;font-size:13px;margin-left:12px;">
            ✅ 已选择：{{ form.idCardFrontFile.name }}
          </span>
        </el-form-item>

        <!-- ===== 修改：身份证反面（使用 :auto-upload="false"） ===== -->
        <el-form-item label="身份证反面" prop="idCardBackFile">
          <el-upload
            ref="idCardBackUpload"
            :auto-upload="false"
            :on-change="(file) => { form.idCardBackFile = file.raw }"
            :on-remove="() => { form.idCardBackFile = null }"
            accept="image/*"
            :limit="1"
          >
            <el-button>选择身份证反面</el-button>
            <template #tip>
              <div style="font-size:12px;color:#909399;">支持 JPG/PNG 格式</div>
            </template>
          </el-upload>
          <span v-if="form.idCardBackFile" style="color:#67C23A;font-size:13px;margin-left:12px;">
            ✅ 已选择：{{ form.idCardBackFile.name }}
          </span>
        </el-form-item>

        <!-- ===== 修改：营业执照（使用 :auto-upload="false"） ===== -->
        <el-form-item label="营业执照" prop="businessLicenseFile">
          <el-upload
            ref="businessLicenseUpload"
            :auto-upload="false"
            :on-change="(file) => { form.businessLicenseFile = file.raw }"
            :on-remove="() => { form.businessLicenseFile = null }"
            accept="image/*"
            :limit="1"
          >
            <el-button>选择营业执照</el-button>
            <template #tip>
              <div style="font-size:12px;color:#909399;">支持 JPG/PNG 格式</div>
            </template>
          </el-upload>
          <span v-if="form.businessLicenseFile" style="color:#67C23A;font-size:13px;margin-left:12px;">
            ✅ 已选择：{{ form.businessLicenseFile.name }}
          </span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" @click="submitApply" :loading="submitting">提交申请</el-button>
        </el-form-item>
      </el-form>

      <!-- 审核中 -->
      <div v-else-if="applicationStatus === 0" class="status-section">
        <el-empty description="您的申请正在审核中，请耐心等待..." />
      </div>

      <!-- 已通过 -->
      <div v-else-if="applicationStatus === 1" class="status-section">
        <el-empty description="🎉 恭喜您已成为商家！" />
        <div style="text-align:center;">
          <el-button type="primary" @click="goToShopManage">进入商家管理</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'

const formRef = ref()
const submitting = ref(false)
const applicationStatus = ref(null)

// ===== 表单数据 =====
const form = reactive({
  shopName: '',
  description: '',
  contactPerson: '',
  contactPhone: '',
  address: '',
  idCard: '',
  idCardFrontFile: null,   // 身份证正面文件
  idCardBackFile: null,    // 身份证反面文件
  businessLicenseFile: null // 营业执照文件
})

const rules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }]
}

const statusAlertType = computed(() => {
  if (applicationStatus.value === 0) return 'warning'
  if (applicationStatus.value === 1) return 'success'
  if (applicationStatus.value === 2) return 'danger'
  return 'info'
})

const statusAlertTitle = computed(() => {
  if (applicationStatus.value === 0) return '审核中'
  if (applicationStatus.value === 1) return '审核通过'
  if (applicationStatus.value === 2) return '审核未通过'
  return ''
})

const statusAlertDesc = computed(() => {
  if (applicationStatus.value === 0) return '您的商家申请正在审核中，请耐心等待。'
  if (applicationStatus.value === 1) return '恭喜您！您的申请已通过审核，您现在可以管理您的店铺了。'
  if (applicationStatus.value === 2) return '您的申请未通过审核，请补充资料后重新提交。'
  return ''
})

// ===== 提交申请 =====
const submitApply = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const formData = new FormData()
    formData.append('shopName', form.shopName)
    formData.append('description', form.description || '')
    formData.append('contactPerson', form.contactPerson || '')
    formData.append('contactPhone', form.contactPhone || '')
    formData.append('address', form.address || '')
    formData.append('idCard', form.idCard || '')

    // ===== 关键修改：直接添加文件到 FormData =====
    if (form.idCardFrontFile) {
      formData.append('idCardFront', form.idCardFrontFile)
    }
    if (form.idCardBackFile) {
      formData.append('idCardBack', form.idCardBackFile)
    }
    if (form.businessLicenseFile) {
      formData.append('businessLicense', form.businessLicenseFile)
    }

    await request.post('/api/shop/apply', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('申请提交成功，请等待审核')
    applicationStatus.value = 0
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '申请提交失败')
  } finally {
    submitting.value = false
  }
}

// ===== 跳转商家管理 =====
const goToShopManage = () => {
  localStorage.removeItem('userInfo')
  window.location.href = '/shop/books'
}

// ===== 加载申请状态 =====
const loadStatus = async () => {
  try {
    const res = await request.get('/api/shop/application/status')
    if (res.data) {
      applicationStatus.value = res.data.status
    }
  } catch (error) {
    // 忽略
  }
}

onMounted(loadStatus)
</script>

<style scoped>
.apply-shop-page { max-width: 700px; margin: 0 auto; padding: 20px; }
.apply-card { border-radius: 12px; }
.page-title { font-size: 24px; margin-bottom: 20px; }
.apply-form { margin-top: 16px; }
.status-section { padding: 20px 0; }
</style>
