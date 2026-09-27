import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from './errorCodes'

// === 阶段五 T2：baseURL 从环境变量读取，不再硬编码 ===
// 默认 http://localhost:8080，生产部署时改 .env.production 即可
const baseURL = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

// 创建 axios 实例
const request = axios.create({
  baseURL,
  timeout: 10000
})

// ----- 请求拦截器：在发送请求前自动带上 Token -----
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// ----- 响应拦截器：统一处理返回结果和错误 -----
request.interceptors.response.use(
  response => {
    const res = response.data
    // 如果后端返回的 code 不是 200，说明业务逻辑出错了
    if (res.code !== 200) {
      const message = getErrorMessage(res.code, res.msg)
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return res
  },
  error => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        // === 阶段五 T3：同时清除 token 和 userInfo，避免残留身份状态 ===
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        // === 阶段五 T3：用 router 跳转（避免 window.location 整页刷新导致页面闪白）===
        // 延迟 import 避免循环依赖（router 是从 ../router 引入的）
        if (!location.pathname.includes('/login')) {
          import('../router').then(mod => {
            mod.default.push('/login')
          })
        }
      } else if (status === 403) {
        ElMessage.error('没有权限访问该资源')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status === 500) {
        ElMessage.error('服务器内部错误，请稍后重试')
      } else if (status === 400) {
        ElMessage.error(error.response.data?.msg || '参数错误')
      } else {
        ElMessage.error(error.message || '网络错误')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络后重试')
    } else {
      ElMessage.error(error.message || '网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default request
