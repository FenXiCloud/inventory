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

    {name: 'PrintTemplateList', component: defineAsyncComponent(() => import('@views/setting/PrintTemplateList.vue'))},

    {name: 'SystemLogList', component: defineAsyncComponent(() => import('@views/setting/SystemLogList.vue'))},

    {name: 'ProductCategoryList', component: defineAsyncComponent(() => import('@views/basic/ProductCategoryList'))},

    {name: 'ProductPriceList', component: defineAsyncComponent(() => import('@views/basic/ProductPriceList'))},

    {name: 'CustomerList', component: defineAsyncComponent(() => import('@views/basic/CustomerList'))},

    {name: 'CustomerLevelList', component: defineAsyncComponent(() => import('@views/basic/CustomerLevelList'))},

    {name: 'CustomerCategoryList', component: defineAsyncComponent(() => import('@views/basic/CustomerCategoryList'))},

    {name: 'SupplierCategoryList', component: defineAsyncComponent(() => import('@views/basic/SupplierCategoryList'))},

    {name: 'SupplierList', component: defineAsyncComponent(() => import('@views/basic/SupplierList'))},

    {name: 'WarehouseList', component: defineAsyncComponent(() => import('@views/basic/WarehouseList'))},

    {name: 'UnitList', component: defineAsyncComponent(() => import('@views/basic/UnitList'))},

    {name: 'ProductList', component: defineAsyncComponent(() => import('@views/basic/ProductList'))},

    {name: 'AccountList', component: defineAsyncComponent(() => import('@views/fund/AccountList'))},

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

    {name: 'PriceRecordList', component: defineAsyncComponent(() => import('@views/basic/PriceRecordList.vue'))},
    {name: 'SalesOrderList', component: defineAsyncComponent(() => import('@views/sales/SalesOrderList.vue'))},
    {name: 'SalesReturnList', component: defineAsyncComponent(() => import('@views/sales/SalesReturnList.vue'))},
    {name: 'SalesOutboundList', component: defineAsyncComponent(() => import('@views/sales/SalesOutboundList.vue'))},
    {name: 'SalesItemReport', component: defineAsyncComponent(() => import('@views/sales/SalesItemReport.vue'))},
    {name: 'SalesSummaryReport', component: defineAsyncComponent(() => import('@views/sales/SalesSummaryReport.vue'))},
    {name: 'SalesProfitReport', component: defineAsyncComponent(() => import('@views/sales/SalesProfitReport.vue'))},
    {name: 'SalesRankingReport', component: defineAsyncComponent(() => import('@views/sales/SalesRankingReport.vue'))},
    {name: 'PurchaseOrderList', component: defineAsyncComponent(() => import('@views/purchase/PurchaseOrderList.vue'))},
    {name: 'PurchaseOrderForm', component: defineAsyncComponent(() => import('@views/purchase/PurchaseOrderForm.vue'))},
    {
        name: 'PurchaseInboundList',
        component: defineAsyncComponent(() => import('@views/purchase/PurchaseInboundList.vue'))
    },
    {
        name: 'PurchaseReturnList',
        component: defineAsyncComponent(() => import('@views/purchase/PurchaseReturnList.vue'))
    },
    {
        name: 'PurchaseItemReport',
        component: defineAsyncComponent(() => import('@views/purchase/PurchaseItemReport.vue'))
    },
    {
        name: 'PurchaseSummaryReport',
        component: defineAsyncComponent(() => import('@views/purchase/PurchaseSummaryReport.vue'))
    },
    {
        name: 'CostAdjustmentList',
        component: defineAsyncComponent(() => import('@views/inventory/CostAdjustmentList.vue'))
    },
    {
        name: 'InventoryItemReport',
        component: defineAsyncComponent(() => import('@views/inventory/InventoryItemReport.vue'))
    },
    {
        name: 'InventorySummaryReport',
        component: defineAsyncComponent(() => import('@views/inventory/InventorySummaryReport.vue'))
    },
    {
        name: 'InventoryReport',
        component: defineAsyncComponent(() => import('@views/inventory/InventoryReport.vue'))
    },
    {
        name: 'InventoryTransferList',
        component: defineAsyncComponent(() => import('@views/inventory/InventoryTransferList.vue'))
    },
    {
        name: 'StockTakeList',
        component: defineAsyncComponent(() => import('@views/inventory/StockTakeList.vue'))
    },
    {
        name: 'OtherInboundList',
        component: defineAsyncComponent(() => import('@views/inventory/OtherInboundList.vue'))
    }, {
        name: 'OtherOutboundList',
        component: defineAsyncComponent(() => import('@views/inventory/OtherOutboundList.vue'))
    }, {
        name: 'OtherIncomeList',
        component: defineAsyncComponent(() => import('@views/fund/OtherIncomeList.vue'))
    }, {
        name: 'OtherExpenseList',
        component: defineAsyncComponent(() => import('@views/fund/OtherExpenseList.vue'))
    }, {
        name: 'OrderReceiptList',
        component: defineAsyncComponent(() => import('@views/fund/OrderReceiptList.vue'))
    }, {
        name: 'OrderPaymentList',
        component: defineAsyncComponent(() => import('@views/fund/OrderPaymentList.vue'))
    }, {
        name: 'VerificationList',
        component: defineAsyncComponent(() => import('@views/fund/VerificationList.vue'))
    }, {
        name: 'AccountTransferList',
        component: defineAsyncComponent(() => import('@views/fund/AccountTransferList.vue'))
    }, {
        name: 'OtherIncomeExpenseReport',
        component: defineAsyncComponent(() => import('@views/fund/OtherIncomeExpenseReport.vue'))
    }, {
        name: 'CustomerFlowReport',
        component: defineAsyncComponent(() => import('@views/fund/CustomerFlowReport.vue'))
    }, {
        name: 'SupplierFlowReport',
        component: defineAsyncComponent(() => import('@views/fund/SupplierFlowReport.vue'))
    }, {
        name: 'AccountFlowList',
        component: defineAsyncComponent(() => import('@views/fund/AccountFlowList.vue'))
    },]


/**
 * 加载组件
 */
export default {
    install: (app) => {
        groupRoutes.forEach(item => app.component(item.name, item.component))
    }

}

