<template>
  <div class="simple-page">
    <div class="simple-page__tabs">
      <t-tabs v-model="activeTab" @change="onTabChange">
        <t-tab-panel value="ar" label="客户应收欠款" />
        <t-tab-panel value="ap" label="供应商应付欠款" />
      </t-tabs>
    </div>

    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="单据日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-select
            v-model="params.type"
            :options="typeOptions"
            placeholder="汇总依据"
            style="width: 160px; border-radius: 4px"
            @change="changeType"
        />
        <template v-if="activeTab === 'ar'">
          <t-select
              v-if="params.type == 1"
              v-model="params.customerId"
              :options="customerDataList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              clearable
              placeholder="选择客户"
              style="width: 180px; border-radius: 4px"
          />
          <t-select
              v-if="params.type == 2"
              v-model="params.customerTypeId"
              :options="customerCategoryList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              clearable
              placeholder="选择客户类型"
              style="width: 180px; border-radius: 4px"
          />
          <t-select
              v-if="params.type == 3"
              v-model="params.salesmanId"
              :options="orderStaffList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              clearable
              placeholder="选择销售员"
              style="width: 160px; border-radius: 4px"
          />
        </template>
        <template v-else>
          <t-select
              v-if="params.type == 1"
              v-model="params.supplierId"
              :options="supplierDataList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              clearable
              placeholder="选择供应商"
              style="width: 180px; border-radius: 4px"
          />
          <t-select
              v-if="params.type == 2"
              v-model="params.supplierTypeId"
              :options="supplierCategoryList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              clearable
              placeholder="选择供应商类型"
              style="width: 180px; border-radius: 4px"
          />
          <t-select
              v-if="params.type == 3"
              v-model="params.salesmanId"
              :options="orderStaffList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              clearable
              placeholder="选择业务员"
              style="width: 160px; border-radius: 4px"
          />
        </template>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="rowKey"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="fixed"
          :data="tableData"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
      />
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total">期末余额合计：{{ closingTotal }}元</span>
      <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :show-jumper="true"
          :show-page-size="true"
          :popup-props="{ attach: 'body' }"
          @change="onPageChange"
      />
    </div>
  </div>
</template>

<script>
import manba from 'manba';
import AccountFlow from '@js/api/fund/AccountFlow';
import OrderStaff from '@js/api/basic/OrderStaff';
import Customer from '@js/api/basic/Customer';
import CustomerCategory from '@js/api/basic/CustomerCategory';
import Supplier from '@js/api/basic/Supplier';
import SupplierCategory from '@js/api/basic/SupplierCategory';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');

const money = (v) => Number(v || 0).toFixed(2);

/**
 * @功能描述: 往来单位欠款表
 */
export default {
  name: 'counterpartDebt',
  data() {
    return {
      activeTab: 'ar',
      dataList: [],
      pagination: { page: 1, pageSize: 20, total: 0 },
      loading: false,
      params: {
        type: 1,
        customerId: null,
        customerTypeId: null,
        supplierId: null,
        supplierTypeId: null,
        salesmanId: null
      },
      displayType: 1,
      customerCategoryList: [],
      customerDataList: [],
      supplierCategoryList: [],
      supplierDataList: [],
      orderStaffList: [],
      totalCount: {},
      dateRangeValue: [startTime, endTime]
    };
  },
  computed: {
    typeOptions() {
      return this.activeTab === 'ar'
        ? [
            { label: '客户', value: 1 },
            { label: '客户类型', value: 2 },
            { label: '销售员', value: 3 }
          ]
        : [
            { label: '供应商', value: 1 },
            { label: '供应商类型', value: 2 },
            { label: '业务员', value: 3 }
          ];
    },
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      const base = {
        type: this.params.type,
        salesmanId: this.params.salesmanId,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startDate: start || null,
        endDate: end || null
      };
      if (this.activeTab === 'ar') {
        return {
          ...base,
          customerId: this.params.customerId,
          customerTypeId: this.params.customerTypeId
        };
      }
      return {
        ...base,
        supplierId: this.params.supplierId,
        supplierTypeId: this.params.supplierTypeId
      };
    },
    columns() {
      if (this.activeTab === 'ar') {
        const amounts = [
          { colKey: 'openingBalance', title: '期初余额', minWidth: 120, align: 'right' },
          { colKey: 'currentReceivable', title: '本期应收', minWidth: 120, align: 'right' },
          { colKey: 'currentReceipt', title: '本期收款', minWidth: 120, align: 'right' },
          { colKey: 'closingBalance', title: '期末余额', minWidth: 120, align: 'right' }
        ];
        if (this.displayType == 2) {
          return [{ colKey: 'customerCategory', title: '客户分类', minWidth: 140, ellipsis: true }, ...amounts];
        }
        if (this.displayType == 3) {
          return [
            { colKey: 'customerCode', title: '销售员编号', width: 130, align: 'center' },
            { colKey: 'customerName', title: '销售员', minWidth: 120, ellipsis: true },
            ...amounts
          ];
        }
        return [
          { colKey: 'customerCategory', title: '客户分类', width: 130, ellipsis: true },
          { colKey: 'customerCode', title: '客户编码', width: 120, align: 'center' },
          { colKey: 'customerName', title: '客户名称', minWidth: 140, ellipsis: true },
          ...amounts
        ];
      }
      const amounts = [
        { colKey: 'openingBalance', title: '期初余额', minWidth: 120, align: 'right' },
        { colKey: 'currentPayable', title: '本期应付', minWidth: 120, align: 'right' },
        { colKey: 'currentPayment', title: '本期付款', minWidth: 120, align: 'right' },
        { colKey: 'closingBalance', title: '期末余额', minWidth: 120, align: 'right' }
      ];
      if (this.displayType == 2) {
        return [{ colKey: 'supplierCategory', title: '供应商分类', minWidth: 140, ellipsis: true }, ...amounts];
      }
      if (this.displayType == 3) {
        return [
          { colKey: 'supplierCode', title: '业务员编号', width: 130, align: 'center' },
          { colKey: 'supplierName', title: '业务员', minWidth: 120, ellipsis: true },
          ...amounts
        ];
      }
      return [
        { colKey: 'supplierCategory', title: '供应商分类', width: 130, ellipsis: true },
        { colKey: 'supplierCode', title: '供应商编码', width: 120, align: 'center' },
        { colKey: 'supplierName', title: '供应商名称', minWidth: 140, ellipsis: true },
        ...amounts
      ];
    },
    tableData() {
      return (this.dataList || []).map((row, index) => ({
        ...row,
        rowKey: `debt_${index}`,
        openingBalance: money(row.openingBalance),
        currentReceivable: row.currentReceivable != null ? money(row.currentReceivable) : undefined,
        currentReceipt: row.currentReceipt != null ? money(row.currentReceipt) : undefined,
        currentPayable: row.currentPayable != null ? money(row.currentPayable) : undefined,
        currentPayment: row.currentPayment != null ? money(row.currentPayment) : undefined,
        closingBalance: money(row.closingBalance)
      }));
    },
    footData() {
      const t = this.totalCount || {};
      if (this.activeTab === 'ar') {
        const row = {
          openingBalance: money(t.totalOpeningBalance),
          currentReceivable: money(t.totalCurrentReceivable),
          currentReceipt: money(t.totalCurrentReceipt),
          closingBalance: money(t.totalClosingBalance)
        };
        if (this.displayType == 3) return [{ customerCode: '合计', ...row }];
        return [{ customerCategory: '合计', customerName: '合计', ...row }];
      }
      const row = {
        openingBalance: money(t.totalOpeningBalance),
        currentPayable: money(t.totalCurrentPayable),
        currentPayment: money(t.totalCurrentPayment),
        closingBalance: money(t.totalClosingBalance)
      };
      if (this.displayType == 3) return [{ supplierCode: '合计', ...row }];
      return [{ supplierCategory: '合计', supplierName: '合计', ...row }];
    },
    closingTotal() {
      return money(this.totalCount.totalClosingBalance);
    }
  },
  methods: {
    onTabChange() {
      this.params.type = 1;
      this.changeType();
      this.pagination.page = 1;
      this.displayType = 1;
      this.loadList();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.displayType = this.params.type;
      this.loadList();
    },
    changeType() {
      this.params.customerId = null;
      this.params.customerTypeId = null;
      this.params.supplierId = null;
      this.params.supplierTypeId = null;
      this.params.salesmanId = null;
    },
    loadList() {
      this.loading = true;
      const req =
        this.activeTab === 'ar'
          ? AccountFlow.summaryReceivableDetails(this.queryParams)
          : AccountFlow.summaryPayableDetails(this.queryParams);
      req
        .then(({ data }) => {
          if (this.activeTab === 'ar') {
            this.dataList = data.receivableDetailsList || [];
            this.pagination.total = data.receivableDetailsListTotal || 0;
            this.totalCount = {
              totalOpeningBalance: data.totalOpeningBalance,
              totalCurrentReceivable: data.totalCurrentReceivable,
              totalCurrentReceipt: data.totalCurrentReceipt,
              totalClosingBalance: data.totalClosingBalance
            };
          } else {
            this.dataList = data.payableDetailsList || [];
            this.pagination.total = data.receivableDetailsListTotal || data.payableDetailsListTotal || 0;
            this.totalCount = {
              totalOpeningBalance: data.totalOpeningBalance,
              totalCurrentPayable: data.totalCurrentPayable,
              totalCurrentPayment: data.totalCurrentPayment,
              totalClosingBalance: data.totalClosingBalance
            };
          }
        })
        .finally(() => (this.loading = false));
    },
    loadOptions() {
      OrderStaff.orderStaffList().then(({ data }) => {
        this.orderStaffList = data || [];
      });
      Customer.select().then(({ data }) => {
        this.customerDataList = data || [];
      });
      CustomerCategory.select().then(({ data }) => {
        this.customerCategoryList = data || [];
      });
      Supplier.select().then(({ data }) => {
        this.supplierDataList = data || [];
      });
      SupplierCategory.select().then(({ data }) => {
        this.supplierCategoryList = data || [];
      });
    }
  },
  created() {
    this.loadOptions();
    this.loadList();
  }
};
</script>

<style scoped>
.simple-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.simple-page__tabs {
  flex-shrink: 0;
  padding-top: 4px;
}

.simple-page__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.simple-page__table {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  overflow: hidden;
}

.simple-page__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
  background: #fff;
}

.simple-page__total {
  font-size: 14px;
  color: #333639;
  flex-shrink: 0;
}
</style>
