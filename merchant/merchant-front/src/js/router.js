
import {defineAsyncComponent} from "vue";

const groupRoutes = [
	{name: 'DashboardMain', component: defineAsyncComponent(() => import("@views/dashboard/DashboardMain"))},

	{name: 'AccountBasic', component: defineAsyncComponent(() => import("@views/common/AccountBasic"))},

	{name: 'ProductCategoryList', component: defineAsyncComponent(() => import('@views/basic/ProductCategoryList'))},

	{name: 'CustomerList', component: defineAsyncComponent(() => import('@views/basic/CustomerList'))},

	{name: 'CustomerLevelList', component: defineAsyncComponent(() => import('@views/basic/CustomerLevelList'))},

	{name: 'CustomerCategoryList', component: defineAsyncComponent(() => import('@views/basic/CustomerCategoryList'))},

	{name: 'SupplierCategoryList', component: defineAsyncComponent(() => import('@views/basic/SupplierCategoryList'))},

	{name: 'SupplierList', component: defineAsyncComponent(() => import('@views/basic/SupplierList'))},

	{name: 'WarehouseList', component: defineAsyncComponent(() => import('@views/basic/WarehouseList'))},

	{name: 'UnitList', component: defineAsyncComponent(() => import('@views/basic/UnitList'))},

	{name: 'ProductList', component: defineAsyncComponent(() => import('@views/basic/ProductList'))},

	{name: 'AccountBookList', component: defineAsyncComponent(() => import('@views/setting/AccountBookList'))},

	{name: 'MerchantInfo', component: defineAsyncComponent(() => import('@views/setting/MerchantInfo'))},

	{name: 'AdminList', component: defineAsyncComponent(() => import('@views/setting/AdminList'))},

	{name: 'RoleList', component: defineAsyncComponent(() => import('@views/setting/RoleList'))},

	{name: 'CheckoutList', component: defineAsyncComponent(() => import('@views/setting/CheckoutList'))},

]

/**
 * 加载组件
 */
export default {
	install: (app) => {
		groupRoutes.forEach(item => app.component(item.name, item.component))
	}

}

