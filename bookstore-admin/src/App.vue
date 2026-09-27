<template>
  <div id="app">
    <AppNavBar
      ref="navBarRef"
      v-if="!isLoginPage"
      :current-identity="currentIdentity"
      :available-identities="availableIdentities"
      :on-switch-identity="switchIdentity"
    />
    <router-view />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppNavBar from './components/AppNavBar.vue'

const route = useRoute()
const router = useRouter()
const navBarRef = ref(null)

// ===== 当前身份 =====
const currentIdentity = ref('buyer')
const availableIdentities = ref(['buyer'])

// ===== 是否为登录页 =====
const isLoginPage = computed(() => route.path === '/login')

// ===== 更新可用身份 =====
const updateAvailableIdentities = () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  const identities = ['buyer']
  if (userInfo.isShopOwner === 1) identities.push('shop')
  if (userInfo.role === 1) identities.push('admin')
  availableIdentities.value = identities
}

// ===== 刷新用户信息 =====
const refreshUserInfo = () => {
  updateAvailableIdentities()
  if (navBarRef.value?.updateUserInfo) {
    navBarRef.value.updateUserInfo()
  }
  updateIdentityFromPath()
}

// ===== 切换身份 =====
const switchIdentity = (identity) => {
  if (identity === currentIdentity.value) return
  currentIdentity.value = identity
  const routes = {
    buyer: '/home',
    shop: '/shop/books',
    admin: '/books-admin'
  }
  router.push(routes[identity] || '/home')
}

// ===== 从路径推断身份 =====
const updateIdentityFromPath = () => {
  const path = route.path
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')

  updateAvailableIdentities()

  // 1. 管理员路径
  if (path.startsWith('/admin') || path.startsWith('/dashboard') || path.startsWith('/books-admin')) {
    if (userInfo.role === 1) {
      currentIdentity.value = 'admin'
    } else {
      router.push('/home')
    }
    return
  }

  // 2. 商家管理路径（需要商家权限）
  const shopManagePaths = ['/shop/books', '/shop/orders', '/shop/after-sale', '/shop/profile', '/shop/statistics', '/shop/comments', '/shop/messages']
  const isShopManageRoute = shopManagePaths.some(p => path.startsWith(p))

  if (isShopManageRoute) {
    if (userInfo.isShopOwner === 1) {
      currentIdentity.value = 'shop'
    } else {
      router.push('/home')
    }
    return
  }

  // 3. 公开店铺主页（/shop/:shopId 和 /shop/home/:shopId）
  // 只有登录用户且是商家本人时显示商家标签，否则显示买家视图
  if (path.startsWith('/shop')) {
    // 如果是商家本人访问自己的店铺，显示商家标签（但仍是买家视角）
    // 否则保持买家视图
    currentIdentity.value = 'buyer'
    return
  }

  // 4. 其他路径：买家视图
  currentIdentity.value = 'buyer'
}

// ===== 监听路由变化 =====
watch(() => route.path, (newPath, oldPath) => {
  if (oldPath === '/login' && newPath !== '/login') {
    nextTick(() => {
      refreshUserInfo()
    })
  } else {
    updateIdentityFromPath()
  }
}, { immediate: true })

// ===== 监听 localStorage 变化 =====
const handleStorageChange = (e) => {
  if (e.key === 'userInfo' || e.key === 'token') {
    refreshUserInfo()
  }
}

// ===== 挂载时初始化 =====
onMounted(() => {
  refreshUserInfo()
  window.addEventListener('storage', handleStorageChange)
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif; background-color: #f5f7fa; }
#app { min-height: 100vh; }
</style>
