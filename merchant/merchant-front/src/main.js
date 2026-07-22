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
import {setupDialog} from '@common/dialog'
import widthDirective from '@/js/directives/width'

let app = createApp(App);
app.use(store).use(TDesign).directive("auth", directive).directive('width', widthDirective);
setupDialog(app);

store.dispatch('init').then(() => {
	app.use(router);
	app.mount('#app')
}).catch(() => {
	app.use(router);
	app.mount('#app')
});
