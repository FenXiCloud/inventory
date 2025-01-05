import {defineAsyncComponent} from "vue";

const groupRoutes = [
	{name: 'DashboardMain', component: defineAsyncComponent(() => import("@views/dashboard/DashboardMain"))},

	{name: 'AccountBasic', component: defineAsyncComponent(() => import("@views/common/AccountBasic"))},

    {name: 'AccountBookList', component: defineAsyncComponent(() => import('@views/setting/AccountBookList'))},

    {name: 'MerchantInfo', component: defineAsyncComponent(() => import('@views/setting/MerchantInfo'))},

    {name: 'AdminList', component: defineAsyncComponent(() => import('@views/setting/AdminList'))},

    {name: 'RoleList', component: defineAsyncComponent(() => import('@views/setting/RoleList'))},

    {name: 'CheckoutList', component: defineAsyncComponent(() => import('@views/setting/CheckoutList'))},

    {name: 'CodeRuleList', component: defineAsyncComponent(() => import('@views/setting/CodeRuleList.vue'))},

    {name: 'ProductCategoryList', component: defineAsyncComponent(() => import('@views/basic/ProductCategoryList'))},

    {name: 'CustomerList', component: defineAsyncComponent(() => import('@views/basic/CustomerList'))},

    {name: 'CustomerLevelList', component: defineAsyncComponent(() => import('@views/basic/CustomerLevelList'))},

    {name: 'CustomerCategoryList', component: defineAsyncComponent(() => import('@views/basic/CustomerCategoryList'))},

    {name: 'SupplierCategoryList', component: defineAsyncComponent(() => import('@views/basic/SupplierCategoryList'))},

    {name: 'SupplierList', component: defineAsyncComponent(() => import('@views/basic/SupplierList'))},

    {name: 'WarehouseList', component: defineAsyncComponent(() => import('@views/basic/WarehouseList'))},

    {name: 'UnitList', component: defineAsyncComponent(() => import('@views/basic/UnitList'))},

    {name: 'ProductList', component: defineAsyncComponent(() => import('@views/basic/ProductList'))},

    {name: 'AccountList', component: defineAsyncComponent(() => import('@views/basic/AccountList'))},

    {name: 'AccountTypeList', component: defineAsyncComponent(() => import('@views/basic/AccountTypeList'))},

    {name: 'PaymentMethodList', component: defineAsyncComponent(() => import('@views/basic/PaymentMethodList.vue'))},

    {name: 'PricingPolicyList', component: defineAsyncComponent(() => import('@views/basic/PricingPolicyList.vue'))},

    {
        name: 'CustomerInitialList',
        component: defineAsyncComponent(() => import('@views/basic/CustomerInitialList.vue'))
    },

    {
        name: 'SupplierInitialList',
        component: defineAsyncComponent(() => import('@views/basic/SupplierInitialList.vue'))
    },

    {
        name: 'InventoryInitialList',
        component: defineAsyncComponent(() => import('@views/basic/InventoryInitialList.vue'))
    },

    {name: 'PriceRecordList', component: defineAsyncComponent(() => import('@views/basic/PriceRecordList.vue'))},]

/**
 * 加载组件
 */
export default {
    install: (app) => {
        groupRoutes.forEach(item => app.component(item.name, item.component))
    }

}

