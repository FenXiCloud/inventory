import {createApp} from 'vue'
import App from './App.vue'
import router from './js/router'
import store from './js/store'
import TDesign from 'tdesign-vue-next'

import './style/index.css'
import 'tdesign-vue-next/es/style/index.css'
import 'font-awesome/css/font-awesome.css'
import 'windi.css'
import directive from '@/js/directive'
import { hasPerm } from '@common/perm'
import { qtyDp, priceDp } from '@common/number'
import {setupDialog} from '@common/dialog'
import widthDirective from '@/js/directives/width'

let app = createApp(App);
app.use(store).use(TDesign).directive("auth", directive).directive('width', widthDirective);
// 供 v-if 场景使用（t-dropdown-item 等 portal 渲染元素指令时机不可靠）
app.config.globalProperties.$can = hasPerm;
// 账套小数位全局化：任意组件模板可直接绑定 :decimal-places="qtyDp" / "priceDp"（响应式跟随当前账套）
app.mixin({
    computed: {
        qtyDp() { return qtyDp(); },
        priceDp() { return priceDp(); }
    }
});
setupDialog(app);

store.dispatch('init').then(() => {
	app.use(router);
	app.mount('#app')
}).catch(() => {
	app.use(router);
	app.mount('#app')
});
