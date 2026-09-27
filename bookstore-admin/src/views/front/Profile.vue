<template>
  <div class="profile-page">
    <el-card class="profile-card">
      <h2 class="page-title">👤 个人中心</h2>

      <!-- Tab 切换 -->
      <el-tabs v-model="activeTab" class="profile-tabs">
        <!-- ===== Tab 1：个人信息 ===== -->
        <el-tab-pane label="个人信息" name="info">
          <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="80px" class="profile-form">
            <!-- 头像 -->
            <el-form-item label="头像" class="avatar-item">
              <el-upload
                :action="avatarUploadUrl +' ?type=avatar'"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :on-error="handleAvatarError"
                class="avatar-uploader"
              >
                <div class="avatar-container">
                  <el-image
                    v-if="profileForm.avatar"
                    :src="getFullUrl(profileForm.avatar)"
                    class="avatar-image"
                    fit="cover"
                  >
                    <template #error>
                      <div class="avatar-placeholder">📷</div>
                    </template>
                  </el-image>
                  <div v-else class="avatar-placeholder">📷</div>
                  <div class="avatar-hover">
                    <span>点击更换</span>
                  </div>
                </div>
              </el-upload>
            </el-form-item>

            <!-- 用户名（只读） -->
            <el-form-item label="用户名">
              <span class="field-value">{{ profileForm.username || '未设置' }}</span>
            </el-form-item>

            <!-- 昵称 -->
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" />
            </el-form-item>

            <!-- 个性签名 -->
            <el-form-item label="个性签名" prop="signature">
              <el-input
                v-model="profileForm.signature"
                placeholder="一句话介绍自己..."
                maxlength="100"
                show-word-limit
              />
            </el-form-item>

            <!-- 邮箱 -->
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>

            <!-- 手机号 -->
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
            </el-form-item>

            <!-- 操作按钮 -->
            <el-form-item>
              <el-button type="primary" @click="saveProfile" :loading="saving">保存修改</el-button>
              <el-button type="warning" plain @click="openPasswordDialog">修改密码</el-button>
              <el-button type="info" plain @click="$router.push('/messages')">💬 消息中心</el-button>
              <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ===== Tab 2：我的订单 ===== -->
        <el-tab-pane label="我的订单" name="orders">
          <OrderList />
        </el-tab-pane>

        <!-- ===== Tab 3：收货地址（优化版） ===== -->
        <el-tab-pane label="收货地址" name="address">
          <div class="address-tab">
            <!-- 头部：数量提示 + 新增按钮 -->
            <div class="address-header">
              <div class="address-count">
                📮 共 <strong>{{ addressList.length }}</strong> 个地址
                <span v-if="addressList.length >= MAX_ADDRESS_COUNT" class="limit-warning">
                  （已达上限 {{ MAX_ADDRESS_COUNT }} 个）
                </span>
              </div>
              <el-button
                type="primary"
                @click="openAddressDialog"
                :disabled="addressList.length >= MAX_ADDRESS_COUNT"
              >
                新增地址
              </el-button>
            </div>

            <!-- 地址列表（卡片网格） -->
            <div v-if="addressList.length > 0" class="address-grid">
              <div
                v-for="addr in addressList"
                :key="addr.id"
                class="address-card"
                :class="{ 'is-default': addr.isDefault === 1 }"
              >
                <!-- 默认徽章 -->
                <div v-if="addr.isDefault === 1" class="default-badge">默认</div>

                <!-- 收货人信息 -->
                <div class="address-card-header">
                  <span class="addr-name">{{ addr.receiverName }}</span>
                  <span class="addr-phone">{{ addr.receiverPhone }}</span>
                </div>

                <!-- 完整地址（省市区 + 详细） -->
                <div class="address-card-body">
                  <span class="addr-full">
                    {{ addr.province || '' }}{{ addr.city || '' }}{{ addr.district || '' }}
                    {{ addr.detailAddress }}
                  </span>
                </div>

                <!-- 操作按钮 -->
                <div class="address-card-actions">
                  <el-button size="small" @click="openAddressDialog(addr)">编辑</el-button>
                  <el-button
                    v-if="addr.isDefault !== 1"
                    size="small"
                    type="primary"
                    plain
                    @click="setDefaultAddress(addr.id)"
                  >
                    设为默认
                  </el-button>
                  <el-button
                    size="small"
                    type="danger"
                    plain
                    @click="deleteAddress(addr.id)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <el-empty v-else description="暂无收货地址，点击上方「新增地址」添加">
              <el-button type="primary" @click="openAddressDialog">添加第一个地址</el-button>
            </el-empty>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ===== 修改密码弹窗 ===== -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPassword" :loading="passwordLoading">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- ===== 新增/编辑地址弹窗（优化版：省市区三级联动） ===== -->
    <el-dialog
      v-model="addressDialogVisible"
      :title="addressDialogTitle"
      width="520px"
      @close="resetAddressForm"
    >
      <el-form
        :model="addressForm"
        :rules="addressRules"
        ref="addressFormRef"
        label-width="80px"
      >
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>

        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="addressForm.receiverPhone" placeholder="请输入手机号" />
        </el-form-item>

        <!-- 省市区三级联动 -->
        <el-form-item label="所在地区" prop="region">
          <el-cascader
            v-model="addressForm.region"
            :options="regionData"
            placeholder="请选择省 / 市 / 区"
            clearable
            style="width:100%"
            :props="{ value: 'value', label: 'label', emitPath: true }"
            @change="onRegionChange"
          />
        </el-form-item>

        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="addressForm.detailAddress"
            type="textarea"
            :rows="2"
            placeholder="请输入详细地址（如：xx路xx号xx室）"
          />
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch
            v-model="addressForm.isDefault"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddress" :loading="addressLoading">
          保存地址
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// ===== 显式声明组件名，满足 multi-word 规则 =====
defineOptions({
  name: 'UserProfile'
})

import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { getFullUrl } from '../../utils/image'
import OrderList from './OrderList.vue'

// ===== 仅导入需要的 regionData，删除未使用的 CodeToText =====
import { regionData } from 'element-china-area-data'

const router = useRouter()

// ===== 常量 =====
const MAX_ADDRESS_COUNT = 10

// ===== 状态 =====
const activeTab = ref('info')
const saving = ref(false)
const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref()
const profileFormRef = ref()
const addressFormRef = ref()
const addressDialogVisible = ref(false)
const addressLoading = ref(false)
const addressList = ref([])
const editingAddressId = ref(null)

const avatarUploadUrl = 'http://localhost:8080/api/file/upload?type=avatar'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

// ===== 个人信息 =====
const profileForm = reactive({
  id: null,
  username: '',
  nickname: '',
  signature: '',
  email: '',
  phone: '',
  avatar: '',
  role: 0,
  status: 1
})

const profileRules = {
  nickname: [{ max: 20, message: '昵称不能超过20个字符', trigger: 'blur' }],
  signature: [{ max: 100, message: '个性签名不能超过100个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

// ===== 密码修改 =====
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// ===== 地址管理 =====
const addressForm = reactive({
  receiverName: '',
  receiverPhone: '',
  region: [],
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
})

const addressRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  region: [{ required: true, message: '请选择所在地区', trigger: 'change' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const addressDialogTitle = computed(() => {
  return editingAddressId.value ? '编辑地址' : '新增地址'
})

// ===== 加载地址列表 =====
const loadAddressList = async () => {
  try {
    const res = await request.get('/api/address/list')
    addressList.value = res.data || []
  } catch {
    // 未使用的 error 参数已省略
    ElMessage.error('加载地址列表失败')
  }
}

// ===== 打开地址弹窗 =====
const openAddressDialog = (addr) => {
  if (!addr && addressList.value.length >= MAX_ADDRESS_COUNT) {
    ElMessage.warning(`最多只能添加 ${MAX_ADDRESS_COUNT} 个地址`)
    return
  }

  if (addr) {
    editingAddressId.value = addr.id
    const region = []
    if (addr.province) region.push(addr.province)
    if (addr.city) region.push(addr.city)
    if (addr.district) region.push(addr.district)

    Object.assign(addressForm, {
      receiverName: addr.receiverName || '',
      receiverPhone: addr.receiverPhone || '',
      region: region,
      province: addr.province || '',
      city: addr.city || '',
      district: addr.district || '',
      detailAddress: addr.detailAddress || '',
      isDefault: addr.isDefault || 0
    })
  } else {
    editingAddressId.value = null
    resetAddressForm()
  }
  addressDialogVisible.value = true
}

// ===== 重置地址表单 =====
const resetAddressForm = () => {
  Object.assign(addressForm, {
    receiverName: '',
    receiverPhone: '',
    region: [],
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: 0
  })
}

// ===== 省市区级联选择变化 =====
const onRegionChange = (value) => {
  if (value && value.length === 3) {
    addressForm.province = value[0]
    addressForm.city = value[1]
    addressForm.district = value[2]
  } else if (value && value.length === 2) {
    addressForm.province = value[0]
    addressForm.city = value[1]
    addressForm.district = ''
  } else if (value && value.length === 1) {
    addressForm.province = value[0]
    addressForm.city = ''
    addressForm.district = ''
  } else {
    addressForm.province = ''
    addressForm.city = ''
    addressForm.district = ''
  }
}

// ===== 提交地址 =====
const submitAddress = async () => {
  try {
    await addressFormRef.value.validate()
  } catch {
    return
  }

  if (!editingAddressId.value && addressList.value.length >= MAX_ADDRESS_COUNT) {
    ElMessage.warning(`最多只能添加 ${MAX_ADDRESS_COUNT} 个地址`)
    return
  }

  addressLoading.value = true
  try {
    const data = {
      receiverName: addressForm.receiverName,
      receiverPhone: addressForm.receiverPhone,
      province: addressForm.province,
      city: addressForm.city,
      district: addressForm.district,
      detailAddress: addressForm.detailAddress,
      isDefault: addressForm.isDefault
    }

    if (editingAddressId.value) {
      data.id = editingAddressId.value
      await request.post('/api/address/save', data)
      ElMessage.success('地址更新成功')
    } else {
      await request.post('/api/address/save', data)
      ElMessage.success('地址添加成功')
    }

    addressDialogVisible.value = false
    await loadAddressList()
  } catch (error) {
    const msg = error.response?.data?.msg || '操作失败'
    ElMessage.error(msg)
  } finally {
    addressLoading.value = false
  }
}

// ===== 设置默认地址 =====
const setDefaultAddress = async (id) => {
  try {
    await request.put(`/api/address/default/${id}`)
    ElMessage.success('设置成功')
    await loadAddressList()
  } catch {
    // 未使用的 error 参数已省略
    ElMessage.error('设置失败')
  }
}

// ===== 删除地址 =====
const deleteAddress = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该地址吗？', '提示', { type: 'warning' })
    await request.delete(`/api/address/${id}`)
    ElMessage.success('删除成功')
    await loadAddressList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// ===== 获取个人信息 =====
const loadProfile = async () => {
  try {
    const res = await request.get('/api/user/profile')
    Object.assign(profileForm, res.data)
  } catch {
    // 未使用的 error 参数已省略
    ElMessage.error('加载个人信息失败')
  }
}

// ===== 保存个人信息（彻底修复 no-unused-vars） =====
const saveProfile = async () => {
  try {
    await profileFormRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    // 显式构建需要提交的字段，避免解构未使用的变量
    const updateData = {
      nickname: profileForm.nickname,
      signature: profileForm.signature,
      email: profileForm.email,
      phone: profileForm.phone,
      avatar: profileForm.avatar
    }
    await request.put('/api/user/profile', updateData)
    ElMessage.success('保存成功')
    await loadProfile()
  } catch (error) {
    const msg = error.response?.data?.msg || '保存失败'
    ElMessage.error(msg)
  } finally {
    saving.value = false
  }
}

// ===== 头像上传 =====
const handleAvatarSuccess = (res) => {
  if (res.code === 200) {
    profileForm.avatar = res.data
    ElMessage.success('头像上传成功')
    saveProfile()
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

// ===== 头像上传失败（移除未使用的 error 参数） =====
const handleAvatarError = () => {
  ElMessage.error('头像上传失败')
}

// ===== 退出登录 =====
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success('已退出')
    router.push('/login')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退出失败')
    }
  }
}

// ===== 修改密码 =====
const openPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

const submitPassword = async () => {
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }
  passwordLoading.value = true
  try {
    await request.put('/api/user/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    setTimeout(() => {
      router.push('/login')
    }, 1000)
  } catch (error) {
    const msg = error.response?.data?.msg || '修改失败'
    ElMessage.error(msg)
  } finally {
    passwordLoading.value = false
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadProfile()
  loadAddressList()
})
</script>

<style scoped>
.profile-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 24px 40px;
}

.profile-card {
  border-radius: 12px;
}
.profile-card :deep(.el-card__body) {
  padding: 24px 30px 30px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.profile-form {
  max-width: 520px;
}

.field-value {
  color: #303133;
  font-size: 14px;
  line-height: 32px;
}

/* ===== 头像 ===== */
.avatar-item :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
}

.avatar-uploader {
  cursor: pointer;
}

.avatar-container {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #dcdfe6;
  transition: border-color 0.3s;
  position: relative;
  flex-shrink: 0;
  background: #f5f7fa;
}
.avatar-container:hover {
  border-color: #409EFF;
}
.avatar-container:hover .avatar-hover {
  opacity: 1;
}

.avatar-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}
.avatar-image :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: #909399;
  background: #f5f7fa;
}

.avatar-hover {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 50%;
  pointer-events: none;
}
.avatar-hover span {
  pointer-events: none;
}

/* ===== 地址管理（优化版） ===== */
.address-tab {
  padding: 4px 0;
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  padding: 0 4px;
}

.address-count {
  font-size: 14px;
  color: #606266;
}
.address-count strong {
  color: #409EFF;
  font-size: 16px;
}
.limit-warning {
  color: #f56c6c;
  font-size: 13px;
  margin-left: 8px;
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.address-card {
  position: relative;
  padding: 18px 20px 14px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fafafa;
  transition: all 0.25s;
  cursor: default;
}

.address-card:hover {
  border-color: #409EFF;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.12);
  transform: translateY(-2px);
}

.address-card.is-default {
  border-color: #409EFF;
  background: #ecf5ff;
}

.default-badge {
  position: absolute;
  top: 10px;
  right: 12px;
  background: #409EFF;
  color: #fff;
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 12px;
  letter-spacing: 1px;
}

.address-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  padding-right: 50px;
}

.addr-name {
  font-weight: 600;
  color: #303133;
  font-size: 16px;
}

.addr-phone {
  color: #606266;
  font-size: 14px;
}

.address-card-body {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
  min-height: 24px;
}

.addr-full {
  color: #909399;
  font-size: 13px;
}

.address-card-actions {
  display: flex;
  gap: 4px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  flex-wrap: wrap;
}

@media (max-width: 600px) {
  .address-grid {
    grid-template-columns: 1fr;
  }
  .address-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
