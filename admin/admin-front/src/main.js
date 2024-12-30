import {createApp} from 'vue'
import App from '@/components/App.vue'
import router from '@/js/router.js'
import store from '@/js/store.js'
import {useTable} from '@/js/common/xe-table'
import heyui, {heyuiConfig} from "heyui.ext";
import './style/app.less'
import 'font-awesome/css/font-awesome.css'
import 'windi.css'
import layer from '@layui/layer-vue';
import '@layui/layer-vue/lib/index.css';

heyuiConfig.config("modal", {
	hasDivider: true
});

let app = createApp(App);
app.use(layer).use(store).use(useTable).use(heyui).use(router);
app.mount('#app')

