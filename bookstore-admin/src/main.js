import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 引入 Element Plus 图标库（后续会用到）
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

// 创建 Vue 应用实例
const app = createApp(App)

// 注册 Element Plus 组件库
app.use(ElementPlus)

// 注册所有 Element Plus 图标（这样可以在模板中直接使用 <el-icon>）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册路由
app.use(router)

// 将应用挂载到 HTML 中的 #app 元素上
app.mount('#app')
