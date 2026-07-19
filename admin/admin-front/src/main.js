import {createApp} from 'vue'
import App from '@/views/App.vue'
import router from '@/js/router.js'
import store from '@/js/store.js'
import {useTable} from '@/js/common/xe-table'
import TDesign from 'tdesign-vue-next';
import 'tdesign-vue-next/es/style/index.css';
import './style/index.css'
import 'windi.css'

let app = createApp(App);
app.use(TDesign).use(store).use(useTable).use(router);
app.mount('#app')

