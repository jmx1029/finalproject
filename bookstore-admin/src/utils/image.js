// src/utils/image.js
const BASE_URL = 'http://localhost:8080'

export const getFullUrl = (url) => {
  if (!url) return ''
  // 1. 完整 URL（豆瓣图片）
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // 2. 已有 /uploads/ 前缀
  if (url.startsWith('/uploads/')) {
    return BASE_URL + url
  }
  // 3. 纯文件名 → 添加 /uploads/ 前缀
  return BASE_URL + '/uploads/' + url
}
