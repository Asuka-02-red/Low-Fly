/** 应用入口文件：创建 Vue 实例，注册 Pinia 状态管理、Vue Router 路由、Element Plus 组件库（中文语言包）和 FontAwesome 图标组件，并挂载到 DOM */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import App from './App.vue'
import router from './router'
import './styles/index.scss'
import 'element-plus/dist/index.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})
app.component('FontAwesomeIcon', FontAwesomeIcon)

app.mount('#app')
