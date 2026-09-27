/**
 * 身份/角色判断工具
 * === 阶段五 T1：解决 router/index.js 与 OrderCard.vue 对 isShopOwner 类型判断不一致问题 ===
 * - router：isShopOwner !== 1 （只认数字）
 * - OrderCard：isShopOwner === 1 || isShopOwner === true （兼容 boolean）
 * 后端 LoginVO 里 isShopOwner 是 Integer，但防御性判断应统一为兼容写法
 */

/**
 * 解析当前登录用户信息（带 try-catch，localStorage 脏数据不会崩）
 * @returns { { role: number|null, isShopOwner: number|boolean|null, userId: number|null, ... } | null }
 */
export function getUserInfo() {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || 'null')
  } catch (e) {
    return null
  }
}

/** 是否已登录（只认 token 非空） */
export function isLoggedIn() {
  return !!localStorage.getItem('token')
}

/**
 * 是否管理员（role === 1）
 */
export function isAdmin() {
  const info = getUserInfo()
  return !!(info && info.role === 1)
}

/**
 * 是否商家（isShopOwner === 1 或 === true，兼容 Integer/Boolean）
 * 原来散落在 router 和 OrderCard 里的写法不统一，现在收敛到这里
 */
export function isShopOwner() {
  const info = getUserInfo()
  if (!info) return false
  const v = info.isShopOwner
  return v === 1 || v === true
}

/**
 * 根据当前角色返回 API 前缀
 * 买家 → /api/order，商家 → /shop/order，管理员 → /admin/order
 */
export function getOrderApiPrefix() {
  if (isAdmin()) return '/admin/order'
  if (isShopOwner()) return '/shop/order'
  return '/api/order'
}

/**
 * 返回当前角色的首页路径
 * 管理员 → /books-admin，商家 → /shop/books，买家 → /home
 */
export function getHomePath() {
  if (isAdmin()) return '/books-admin'
  if (isShopOwner()) return '/shop/books'
  return '/home'
}
