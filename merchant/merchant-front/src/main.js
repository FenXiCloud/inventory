import {createApp} from 'vue'
import App from './App.vue'
import router from './js/router'
import store from './js/store'
import {useTable} from '@common/xe-table'
import TDesign, * as tdesign from 'tdesign-vue-next'

import './style/index.css'
import 'tdesign-vue-next/es/style/index.css'
import 'font-awesome/css/font-awesome.css'
import 'windi.css'
import directive from '@/js/directive'
import {setupDialog} from '@common/dialog'
import CompatSelect from '@/components/compat/Select.vue'
import CompatSearch from '@/components/compat/Search.vue'
import CompatButton from '@/components/compat/Button.vue'
import CompatTag from '@/components/compat/Tag.vue'
import CompatDropdownCustom from '@/components/compat/DropdownCustom.vue'
import CompatRow from '@/components/compat/Row.vue'
import CompatCell from '@/components/compat/Cell.vue'
import CompatNumberInput from '@/components/compat/NumberInput.vue'
import widthDirective from '@/js/directives/width'

let app = createApp(App);
app.use(store).use(useTable).use(TDesign).directive("auth", directive).directive('width', widthDirective);
setupDialog(app);

// 无 T 前缀别名（业务模板仍使用 Form/Input/DatePicker 等短名）
Object.entries(tdesign).forEach(([key, comp]) => {
  if (comp && typeof comp === 'object' && comp.name && comp.name.startsWith('T') && comp.name.length > 1 && !comp.name.startsWith('Td')) {
    const shortName = comp.name.slice(1);
    // Select / Button / Tag 由兼容组件接管
    if (shortName === 'Select' || shortName === 'Button' || shortName === 'Tag') return;
    if (shortName && !app._context.components[shortName]) {
      app.component(shortName, comp);
    }
  }
});

app.component('Select', CompatSelect);
app.component('Search', CompatSearch);
app.component('Button', CompatButton);
app.component('Tag', CompatTag);
app.component('DropdownCustom', CompatDropdownCustom);
app.component('Row', CompatRow);
app.component('Cell', CompatCell);
app.component('NumberInput', CompatNumberInput);

store.dispatch('init').then(() => {
	app.use(router);
	app.mount('#app')
}).catch(() => {
	app.use(router);
	app.mount('#app')
});
