import { createRouter, createWebHistory } from 'vue-router'
import { isAdmin, isShopOwner, getUserInfo, isLoggedIn, getHomePath } from '../utils/auth'

// ===== 前台页面 =====
import Login from '../views/front/Login.vue'
import Home from '../views/front/Home.vue'
import Category from '../views/front/Category.vue'
import Cart from '../views/front/Cart.vue'
import OrderList from '../views/front/OrderList.vue'
import OrderDetail from '../views/front/OrderDetail.vue'
import UserShopHome from '../views/front/UserShopHome.vue'

// ===== 用户端消息页面 =====
import MessageSessions from '../views/front/MessageSessions.vue'
import MessageChat from '../views/front/MessageChat.vue'

// ===== 商家端消息页面 =====
import ShopMessageSessions from '../views/shop/ShopMessageSessions.vue'
import ShopMessageChat from '../views/shop/ShopMessageChat.vue'

// ===== 管理端消息页面 =====
import AdminMessageSessions from '../views/admin/AdminMessageSessions.vue'
import AdminMessageChat from '../views/admin/AdminMessageChat.vue'

// ===== 店铺主页 =====
import ShopHome from '../views/shop/ShopHome.vue'

// ===== 后台页面 =====
import BookList from '../views/admin/BookList.vue'
import AdminOrder from '../views/admin/AdminOrder.vue'
import Dashboard from '../views/admin/Dashboard.vue'
import AdminAfterSale from '../views/admin/AdminAfterSale.vue'

// ===== 后台管理页面（懒加载） =====
const AdminCarousel = () => import('../views/admin/AdminCarousel.vue')
const AdminHomeConfig = () => import('../views/admin/AdminHomeConfig.vue')
const AdminFeaturedBooks = () => import('../views/admin/AdminFeaturedBooks.vue')
const AdminOrderDetail = () => import('../views/admin/AdminOrderDetail.vue')
const AdminStatistics = () => import('../views/admin/AdminStatistics.vue')

// ===== 前台页面（懒加载） =====
const Ranking = () => import('../views/front/Ranking.vue')
const BookDetail = () => import('../views/front/BookDetail.vue')
const Profile = () => import('../views/front/Profile.vue')

// ===== 商家页面 =====
const ShopBookList = () => import('../views/shop/ShopBookList.vue')
const ShopOrderList = () => import('../views/shop/ShopOrderList.vue')
const ShopOrderDetail = () => import('../views/shop/ShopOrderDetail.vue')
const ShopAfterSaleList = () => import('../views/shop/ShopAfterSaleList.vue')
const ShopProfile = () => import('../views/shop/ShopProfile.vue')

// ===== 申请商家页面 =====
const ApplyShop = () => import('../views/front/ApplyShop.vue')


const routes = [
  // ============================================================
  //  前台路由（无需登录）
  // ============================================================
  { path: '/', redirect: '/home' },
  { path: '/home', component: Home },
  { path: '/login', component: Login },
  { path: '/category', component: Category },
  { path: '/ranking', component: Ranking },
  { path: '/book/:id', component: BookDetail },
  // ===== 店铺主页路由（公开，无需登录） =====
  { path: '/shop/home/:shopId', component: ShopHome, name: 'ShopHome' },
  { path: '/shop/:shopId', component: UserShopHome, name: 'UserShopHome' },

  // ============================================================
  //  用户端路由（需登录）
  // ============================================================
  { path: '/cart', component: Cart, meta: { requiresAuth: true } },
  { path: '/orders', component: OrderList, meta: { requiresAuth: true } },
  { path: '/orders/:orderId', component: OrderDetail, name: 'OrderDetail', meta: { requiresAuth: true } },
  { path: '/profile', component: Profile, meta: { requiresAuth: true } },
  { path: '/apply-shop', component: ApplyShop, meta: { requiresAuth: true } },

  // ===== 用户端消息路由 =====
  { path: '/messages', component: MessageSessions, meta: { requiresAuth: true } },
  { path: '/messages/chat/:sessionId', component: MessageChat, name: 'MessageChat', meta: { requiresAuth: true } },

  // ============================================================
  //  商家端路由（需登录 + 商家权限）
  // ============================================================
  { path: '/shop/books', component: ShopBookList, meta: { requiresAuth: true, requiresShop: true } },
  { path: '/shop/orders', component: ShopOrderList, meta: { requiresAuth: true, requiresShop: true } },
  { path: '/shop/orders/:orderId', component: ShopOrderDetail, name: 'ShopOrderDetail', meta: { requiresAuth: true, requiresShop: true } },
  { path: '/shop/after-sale', component: ShopAfterSaleList, meta: { requiresAuth: true, requiresShop: true } },
  { path: '/shop/profile', component: ShopProfile, meta: { requiresAuth: true, requiresShop: true } },

  // ===== 商家路由 =====
  // 数据看板（阶段三实现）
  { path: '/shop/statistics', component: () => import('../views/shop/ShopStatistics.vue'), meta: { requiresAuth: true, requiresShop: true } },
  // 评价管理（阶段四实现）
  { path: '/shop/comments', component: () => import('../views/shop/ShopCommentManage.vue'), meta: { requiresAuth: true, requiresShop: true } },

  // ===== 商家端消息路由 =====
  { path: '/shop/messages', component: ShopMessageSessions, meta: { requiresAuth: true, requiresShop: true } },
  { path: '/shop/messages/chat/:sessionId', component: ShopMessageChat, name: 'ShopMessageChat', meta: { requiresAuth: true, requiresShop: true } },

  // ============================================================
  //  管理端路由（需登录 + 管理员权限）
  // ============================================================
  { path: '/books-admin', component: BookList, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/orders', component: AdminOrder, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/orders/:orderId', component: AdminOrderDetail, name: 'AdminOrderDetail', meta: { requiresAuth: true, requiresAdmin: true } },

  { path: '/admin/shops', component: () => import('../views/admin/AdminShopManage.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/users', component: () => import('../views/admin/AdminUserManage.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/home-manage', component: () => import('../views/admin/AdminHomeManage.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  //{ path: '/admin/carousel', component: AdminCarousel, meta: { requiresAuth: true, requiresAdmin: true } },
  //{ path: '/admin/home-config', component: AdminHomeConfig, meta: { requiresAuth: true, requiresAdmin: true } },
  //{ path: '/admin/featured-books', component: AdminFeaturedBooks, meta: { requiresAuth: true, requiresAdmin: true } },

  { path: '/dashboard', component: Dashboard, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/statistics', component: AdminStatistics, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/shop-applications', component: () => import('../views/admin/AdminShopApplications.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/after-sale', component: AdminAfterSale, meta: { requiresAuth: true, requiresAdmin: true } },

  // ===== 管理端消息路由 =====
  { path: '/admin/messages', component: AdminMessageSessions, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/messages/chat/:sessionId', component: AdminMessageChat, name: 'AdminMessageChat', meta: { requiresAuth: true, requiresAdmin: true } },
]


// ============================================================
//  路由守卫
// ============================================================
const router = createRouter({
  history: createWebHistory(),
  routes,
  // 每次路由跳转自动回到页面顶部，给用户明确的导航反馈
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

router.beforeEach((to, from) => {
  // ---------- 1. 登录页特殊处理 ----------
  if (to.path === '/login') {
    if (isLoggedIn()) {
      return getHomePath()
    }
    return true
  }

  // ---------- 2. 检查是否需要登录 ----------
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return '/login'
  }

  // ---------- 3. 检查是否需要商家权限 ----------
  if (to.meta.requiresShop && !isShopOwner()) {
    return '/home'
  }

  // ---------- 4. 检查是否需要管理员权限 ----------
  if (to.meta.requiresAdmin && !isAdmin()) {
    return '/home'
  }

  // ===== 阶段五 T4：旧版路径 startsWith 冗余检查已移到 meta 上，不再重复判 =====
  // meta 配置里已经覆盖了所有 admin/shop 路由，旧逻辑只留参考注释

  return true
})

export default router
