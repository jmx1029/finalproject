<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="title">图书商城管理系统</h2>
      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- ===== 登录 Tab ===== -->
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                prefix-icon="Lock"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ===== 注册 Tab ===== -->
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" size="large" show-password />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="昵称（选填）" prefix-icon="Avatar" size="large" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" size="large" style="width:100%" :loading="loading" @click="handleRegister">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

// 组件名称定义（解决 multi-word 报错）
defineOptions({
  name: 'LoginPage'
})

const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

// ---------- 登录表单 ----------
const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// ---------- 注册表单 ----------
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  nickname: [
    { max: 20, message: '昵称不能超过 20 个字符', trigger: 'blur' }
  ]
}

// ---------- 登录方法 ----------
const handleLogin = async () => {
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await request.post('/api/user/login', loginForm)

    // 存储 token
    localStorage.setItem('token', res.data.token)

// 存储 userInfo
    const userInfoStr = JSON.stringify(res.data)
    localStorage.setItem('userInfo', userInfoStr)

// 立即验证是否存储成功
    const verifyToken = localStorage.getItem('token')
    const verifyUserInfo = localStorage.getItem('userInfo')

// 如果 userInfo 存储失败，尝试直接存储为对象
    if (!verifyUserInfo) {
      localStorage.setItem('userInfo', JSON.stringify({
        token: res.data.token,
        userId: res.data.userId,
        username: res.data.username,
        nickname: res.data.nickname,
        avatar: res.data.avatar,
        role: res.data.role,
        isShopOwner: res.data.isShopOwner
      }))
    }

    ElMessage.success('登录成功')
    if (res.data.role === 1) {
      router.push('/books-admin')
    } else {
      router.push('/home')
    }
  } catch (error) {
  } finally {
    loading.value = false
  }
}

// ---------- 注册方法 ----------
const handleRegister = async () => {
  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    // 使用 delete 方式去除 confirmPassword 字段，无 ESLint 警告
    const submitData = { ...registerForm }
    delete submitData.confirmPassword
    await request.post('/api/user/register', submitData)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = ''
  } catch {
    // 错误已由 request.js 统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px;
  padding: 30px 30px 20px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}
.title {
  text-align: center;
  color: #303133;
  margin-bottom: 10px;
  font-size: 22px;
  font-weight: 500;
}
.login-tabs {
  margin-top: 10px;
}
</style>
