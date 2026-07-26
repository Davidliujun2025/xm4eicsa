import { createApp } from 'vue'
import App from './App.vue'
import './styles/global.css'   // 导入全局样式
// 如果需要全局引入 Font Awesome，可在 index.html 中已通过 CDN 引入，无需额外操作

createApp(App).mount('#app')