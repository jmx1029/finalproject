<template>
  <div class="cart-page">
    <!-- ===== 购物车列表 ===== -->
    <el-card v-if="cartList.length > 0" class="cart-card">
      <div class="cart-header">
        <el-checkbox :model-value="isAllSelected" @change="handleSelectAll">全选</el-checkbox>
        <span class="cart-count">共 {{ totalCount }} 件商品</span>
        <el-button
          type="danger"
          plain
          size="small"
          :disabled="selectedCount === 0"
          @click="deleteSelected"
        >
          删除选中（{{ selectedCount }}）
        </el-button>
        <el-button
          type="danger"
          size="small"
          plain
          @click="clearAll"
        >
          清空购物车
        </el-button>
        <span class="total-price">合计：<span>¥{{ totalAmount }}</span></span>
        <el-button type="primary" @click="goCheckout" :disabled="selectedCount === 0">去结算</el-button>
      </div>

      <el-table :data="cartList" class="cart-table">
        <el-table-column width="60">
          <template #default="{ row }">
            <el-checkbox :model-value="row.selected" @change="(val) => toggleSelect(row.bookId, val)" />
          </template>
        </el-table-column>

        <el-table-column label="商品" min-width="300">
          <template #default="{ row }">
            <div class="book-info">
              <el-image
                :src="getFullUrl(row.coverUrl)"
                referrerpolicy="no-referrer"
                class="book-cover"
                fit="cover"
              >
                <template #error>
                  <div class="cover-placeholder">📖</div>
                </template>
              </el-image>
              <div class="book-detail">
                <div class="book-title">{{ row.title }}</div>
                <div class="book-price">¥{{ row.price }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="数量" width="150" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              :max="row.stock || 999"
              size="small"
              @change="updateQuantity(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="小计" width="130" align="right">
          <template #default="{ row }">
            <span class="subtotal">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="danger" link @click="removeItem(row.bookId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ===== 空购物车 ===== -->
    <div v-else class="empty-cart">
      <el-empty description="购物车空空如也，去逛逛吧~">
        <el-button type="primary" @click="$router.push('/category')">去逛逛</el-button>
      </el-empty>
    </div>

    <!-- ===== 结算弹窗 ===== -->
    <el-dialog v-model="checkoutVisible" title="确认订单" width="540px">
      <div v-if="addressList.length > 0" class="address-select-area">
        <div class="address-select-label">选择已有地址：</div>
        <el-radio-group v-model="selectedAddressId" @change="onAddressSelect" class="address-radio-group">
          <el-radio
            v-for="addr in addressList"
            :key="addr.id"
            :value="addr.id"
            border
            class="address-radio-item"
          >
            <div class="address-item-content">
              <span class="addr-name">{{ addr.receiverName }}</span>
              <span class="addr-phone">{{ addr.receiverPhone }}</span>
              <span class="addr-detail">
                {{ addr.province || '' }}{{ addr.city || '' }}{{ addr.district || '' }}
                {{ addr.detailAddress }}
              </span>
              <el-tag v-if="addr.isDefault === 1" size="small" type="primary" class="addr-tag">默认</el-tag>
            </div>
          </el-radio>
        </el-radio-group>
        <el-divider class="address-divider">或手动填写新地址</el-divider>
      </div>

      <el-form :model="addressForm" :rules="addressRules" ref="addressFormRef" label-width="80px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="addressForm.receiverPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="收货地址" prop="receiverAddress">
          <el-input
            v-model="addressForm.receiverAddress"
            type="textarea"
            :rows="2"
            placeholder="请输入详细地址（如：xx路xx号）"
          />
        </el-form-item>
      </el-form>

      <div class="checkout-total">
        应付金额：<span>¥{{ totalAmount }}</span>
      </div>

      <template #footer>
        <el-button @click="closeCheckout">取消</el-button>
        <el-button type="primary" @click="submitOrder" :loading="submitting">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFullUrl } from '../../utils/image'

const router = useRouter()

// ---------- 数据 ----------
const cartList = ref([])
const checkoutVisible = ref(false)
const submitting = ref(false)
const addressFormRef = ref()
const addressList = ref([])
const selectedAddressId = ref(null)

const addressForm = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

// ---------- 表单校验规则 ----------
const addressRules = {
  receiverName: [
    { required: true, message: '请输入收货人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度 2-20 位', trigger: 'blur' }
  ],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  receiverAddress: [
    { required: true, message: '请输入收货地址', trigger: 'blur' },
    { min: 5, max: 200, message: '地址长度 5-200 位', trigger: 'blur' }
  ]
}

// ---------- 计算属性 ----------
const isAllSelected = computed(() => {
  return cartList.value.length > 0 && cartList.value.every(item => item.selected === true)
})

const selectedCount = computed(() => {
  return cartList.value
    .filter(item => item.selected)
    .reduce((sum, item) => sum + item.quantity, 0)
})

const totalCount = computed(() => {
  return cartList.value.reduce((sum, item) => sum + item.quantity, 0)
})

const totalAmount = computed(() => {
  const total = cartList.value
    .filter(item => item.selected)
    .reduce((sum, item) => sum + item.price * item.quantity, 0)
  return total.toFixed(2)
})

// ---------- 加载购物车 ----------
const loadCart = async () => {
  try {
    const res = await request.get('/api/cart')
    const rawData = res.data || []
    const mergedMap = new Map()
    rawData.forEach(item => {
      if (mergedMap.has(item.bookId)) {
        const exist = mergedMap.get(item.bookId)
        exist.quantity += item.quantity
        exist.selected = exist.selected || item.selected
      } else {
        mergedMap.set(item.bookId, { ...item })
      }
    })
    cartList.value = Array.from(mergedMap.values())
  } catch (error) {
    ElMessage.error('加载购物车失败，请刷新重试')
  }
}

// ---------- 加载地址列表 ----------
const loadAddressList = async () => {
  try {
    const res = await request.get('/api/address/list')
    addressList.value = res.data || []
    const defaultAddr = addressList.value.find(a => a.isDefault === 1)
    if (defaultAddr) {
      selectedAddressId.value = defaultAddr.id
      fillAddressForm(defaultAddr)
    } else if (addressList.value.length > 0) {
      selectedAddressId.value = addressList.value[0].id
      fillAddressForm(addressList.value[0])
    }
  } catch (error) {
  }
}

const fillAddressForm = (addr) => {
  if (!addr) return
  addressForm.receiverName = addr.receiverName || ''
  addressForm.receiverPhone = addr.receiverPhone || ''
  const fullAddr = (addr.province || '') + (addr.city || '') + (addr.district || '') + (addr.detailAddress || '')
  addressForm.receiverAddress = fullAddr
}

const onAddressSelect = (addressId) => {
  const addr = addressList.value.find(a => a.id === addressId)
  if (addr) fillAddressForm(addr)
}

// ---------- 购物车操作 ----------
const updateQuantity = async (row) => {
  if (row.quantity > row.stock) {
    ElMessage.warning('库存不足，当前库存仅剩 ' + row.stock + ' 件')
    row.quantity = row.stock
    return
  }
  try {
    await request.put('/api/cart/update', {
      bookId: row.bookId,
      quantity: row.quantity
    })
  } catch (error) {
    const msg = error.response?.data?.msg || '更新数量失败'
    ElMessage.error(msg)
    await loadCart()
  }
}

const toggleSelect = async (bookId, selected) => {
  try {
    const item = cartList.value.find(i => i.bookId === bookId)
    if (item) item.selected = selected
    await request.put('/api/cart/select', { bookId, selected })
  } catch (error) {
    const msg = error.response?.data?.msg || '操作失败'
    ElMessage.error(msg)
    await loadCart()
  }
}

const handleSelectAll = async (selected) => {
  try {
    cartList.value.forEach(item => { item.selected = selected })
    await request.put('/api/cart/selectAll', { selected })
  } catch (error) {
    const msg = error.response?.data?.msg || '操作失败'
    ElMessage.error(msg)
    await loadCart()
  }
}

const removeItem = async (bookId) => {
  try {
    await request.delete('/api/cart/' + bookId)
    ElMessage.success('已移除')
    loadCart()
  } catch (error) {
    ElMessage.error('移除失败')
  }
}

// ---------- 批量删除 & 清空 ----------
const deleteSelected = async () => {
  const ids = cartList.value.filter(i => i.selected).map(i => i.bookId)
  if (ids.length === 0) return
  try {
    await request.delete('/api/cart/batch', { data: { bookIds: ids } })
    ElMessage.success(`已删除 ${ids.length} 件商品`)
    loadCart()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗？此操作不可恢复', '提示', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete('/api/cart/clear')
    ElMessage.success('购物车已清空')
    loadCart()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('清空失败')
  }
}

// ---------- 结算 ----------
const goCheckout = () => {
  checkoutVisible.value = true
  loadAddressList()
}

const closeCheckout = () => {
  checkoutVisible.value = false
  selectedAddressId.value = null
  addressForm.receiverName = ''
  addressForm.receiverPhone = ''
  addressForm.receiverAddress = ''
}

// ---------- 提交订单 ----------
const submitOrder = async () => {
  try {
    await addressFormRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = {
      receiverName: addressForm.receiverName,
      receiverPhone: addressForm.receiverPhone,
      receiverAddress: addressForm.receiverAddress
    }

    const res = await request.post('/api/order/submit', payload)
    const orderIds = res.data

    ElMessage.success(`下单成功！共生成 ${orderIds.length} 笔订单`)

    checkoutVisible.value = false
    addressForm.receiverName = ''
    addressForm.receiverPhone = ''
    addressForm.receiverAddress = ''
    selectedAddressId.value = null

    await loadCart()
    router.push('/orders')
  } catch (error) {
    const msg = error.response?.data?.msg || error.message || '提交订单失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  loadCart()
})
</script>

<style scoped>
.cart-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px;
}

.cart-card {
  border-radius: 12px;
  overflow: hidden;
}

.cart-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
}

.cart-count {
  color: #606266;
  font-size: 14px;
}

.total-price {
  margin-left: auto;
  font-size: 18px;
  color: #303133;
}
.total-price span {
  color: #f56c6c;
  font-weight: bold;
  font-size: 20px;
}

/* ===== 商品信息 ===== */
.book-info {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 4px 0;
}

.book-cover {
  width: 64px;
  height: 84px;
  border-radius: 6px;
  flex-shrink: 0;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
}

.cover-placeholder {
  width: 64px;
  height: 84px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.book-detail {
  flex: 1;
  min-width: 0;
}

.book-title {
  font-weight: 500;
  color: #303133;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-price {
  color: #f56c6c;
  font-size: 16px;
  font-weight: 500;
  margin-top: 4px;
}

/* ===== 表格样式调整 ===== */
.cart-table :deep(.el-table__cell) {
  padding: 12px 0;
}
.cart-table :deep(.el-table__cell:last-child) {
  padding-right: 16px;
}

.subtotal {
  font-weight: 500;
  color: #303133;
}

/* ===== 空购物车 ===== */
.empty-cart {
  padding: 60px 0;
  background: #fff;
  border-radius: 12px;
}

/* ===== 结算弹窗 ===== */
.address-select-area {
  margin-bottom: 12px;
}

.address-select-label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 10px;
}

.address-radio-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.address-radio-item {
  width: 100%;
  margin: 0 !important;
  padding: 10px 14px !important;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
}

.address-radio-item.is-checked {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.address-item-content {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 16px;
  width: 100%;
}

.addr-name {
  font-weight: 500;
  color: #303133;
}
.addr-phone {
  color: #606266;
  font-size: 13px;
}
.addr-detail {
  color: #909399;
  font-size: 13px;
  flex: 1;
}
.addr-tag {
  flex-shrink: 0;
}

.address-divider {
  margin: 14px 0 10px 0;
}

.checkout-total {
  text-align: right;
  font-size: 18px;
  color: #303133;
  padding: 12px 0 4px;
  border-top: 1px solid #f0f0f0;
}
.checkout-total span {
  color: #f56c6c;
  font-weight: bold;
  font-size: 22px;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .cart-header {
    gap: 12px;
  }
  .total-price {
    margin-left: 0;
    width: 100%;
  }
  .book-info {
    gap: 10px;
  }
  .book-cover {
    width: 48px;
    height: 64px;
  }
  .book-title {
    font-size: 13px;
  }
}
</style>
