// src/utils/chat.js
import { ElMessage } from 'element-plus'
import request from './request'
import router from '../router'

/**
 * 联系用户（卖家/客服）
 * @param {Number|String} targetUserId - 对方用户ID
 * @param {String} targetName - 对方名称（显示在聊天标题）
 * @param {String} apiPrefix - API前缀（可选，默认使用 /api/message）
 */
export const contactUser = async (targetUserId, targetName, apiPrefix = '/api/message') => {
  if (!targetUserId) {
    ElMessage.error('无法获取对方信息')
    return
  }

  try {
    // 获取或创建会话
    const res = await request.get(`${apiPrefix}/create-or-get-session?targetUserId=${targetUserId}`)
    const sessionId = res.data

    if (!sessionId) {
      ElMessage.error('创建会话失败')
      return
    }

    router.push({
      name: 'MessageChat',
      params: { sessionId: sessionId },
      query: {
        targetUserId: targetUserId,
        targetName: targetName || '对方'
      }
    })
  } catch (error) {
    const msg = error.response?.data?.msg || '操作失败，请稍后再试'
    ElMessage.error(msg)
  }
}

/**
 * 联系卖家（通过店铺ID）
 * @param {Number|String} shopId - 店铺ID
 * @param {String} shopName - 店铺名称
 */
export const contactSeller = async (shopId, shopName) => {
  try {
    // 1. 获取店铺信息，找到店主 userId
    const res = await request.get(`/api/shop/public/${shopId}`)
    const shop = res.data
    if (!shop || !shop.userId) {
      ElMessage.error('无法获取店铺信息')
      return
    }

    // 2. 调用 contactUser
    await contactUser(shop.userId, shopName || shop.name || '卖家')
  } catch (error) {
    const msg = error.response?.data?.msg || '联系卖家失败，请稍后再试'
    ElMessage.error(msg)
  }
}

/**
 * 联系平台客服（管理员）
 * @param {String} apiPrefix - API前缀（根据当前角色自动切换）
 */
export const contactCustomerService = async (apiPrefix = '/api/message') => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (!userInfo.userId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  // 管理员的默认用户ID为1
  const targetUserId = 1
  await contactUser(targetUserId, '平台客服', apiPrefix)
}

/**
 * 根据当前角色获取API前缀
 */
export const getMessageApiPrefix = () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (userInfo.role === 1) return '/admin/message'
  if (userInfo.isShopOwner === 1) return '/shop/message'
  return '/api/message'
}
