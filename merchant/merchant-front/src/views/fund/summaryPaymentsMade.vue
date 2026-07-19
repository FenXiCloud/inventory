<template>
  <div class="simple-page">
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
        <t-select
            v-if="params.type == 1"
            v-model="params.supplierId"
            :options="SupplierDataList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="选择供应商"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-if="params.type == 2"
            v-model="params.supplierTypeId"
            :options="SupplierCategoryList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="选择供应商类型"
            style="width: 160px; border-radius: 4px"
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
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>
    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
      />
    </div>
    <div class="simple-page__pager">
      <span class="simple-page__total"></span>
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
import { mapMutations } from 'vuex';
import AccountFlow from '@js/api/fund/AccountFlow';
import OrderStaff from '@js/api/basic/OrderStaff';
import Supplier from '@js/api/basic/Supplier';
import SupplierCategory from '@js/api/basic/SupplierCategory';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');

const amountColumns = [
  { colKey: 'openingBalance', title: '期初余额', minWidth: 120, align: 'center' },
  { colKey: 'currentPayable', title: '本期应付', minWidth: 120, align: 'center' },
  { colKey: 'currentPayment', title: '本期付款', minWidth: 120, align: 'center' },
  { colKey: 'closingBalance', title: '期末余额', minWidth: 120, align: 'center' }
];

export default {
  name: 'summaryPaymentsMade',
  data() {
    return {
      dataList: [],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      loading: false,
      params: {
        type: 1,
        supplierId: null,
        supplierTypeId: null,
        salesmanId: null
      },
      SupplierCategoryList: [],
      SupplierDataList: [],
      orderStaffList: [],
      totalCount: {},
      dateRangeValue: [startTime, endTime],
      type: 1,
      typeOptions: [
        { label: '供应商', value: 1 },
        { label: '供应商类型', value: 2 },
        { label: '业务员', value: 3 }
      ]
    };
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startDate: start || null,
        endDate: end || null
      });
    },
    columns() {
      if (this.type == 2) {
        return [
          { colKey: 'supplierCategory', title: '供应商分类', width: 130, align: 'center' },
          ...amountColumns
        ];
      }
      if (this.type == 3) {
        return [
          { colKey: 'supplierCode', title: '业务员编号', width: 130, align: 'center' },
          { colKey: 'supplierName', title: '业务员', width: 130, align: 'center' },
          ...amountColumns
        ];
      }
      return [
        { colKey: 'supplierCategory', title: '供应商分类', width: 130, align: 'center' },
        { colKey: 'supplierCode', title: '供应商编码', minWidth: 120, align: 'center' },
        { colKey: 'supplierName', title: '供应商名称', width: 130, align: 'center' },
        ...amountColumns
      ];
    },
    footData() {
      const totals = {
        openingBalance: this.totalCount.totalOpeningBalance,
        currentPayable: this.totalCount.totalCurrentPayable,
        currentPayment: this.totalCount.totalCurrentPayment,
        closingBalance: this.totalCount.totalClosingBalance
      };
      if (this.type == 3) {
        return [{ supplierCode: '合计', ...totals }];
      }
      return [{ supplierCategory: '合计', ...totals }];
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.type = this.params.type;
      this.loadList();
    },
    changeType() {
      this.params.supplierTypeId = null;
      this.params.salesmanId = null;
      this.params.supplierId = null;
    },
    loadOrderStaff() {
      OrderStaff.orderStaffList()
        .then(({ data }) => {
          this.orderStaffList = data || [];
        })
        .finally();
    },
    loadSupplierCategory() {
      SupplierCategory.select()
        .then(({ data }) => {
          this.SupplierCategoryList = data || [];
        })
        .finally();
    },
    loadSupplier() {
      this.loading = true;
      Supplier.select()
        .then(({ data }) => {
          this.SupplierDataList = data || [];
        })
        .finally(() => (this.loading = false));
    },
    loadList() {
      this.loading = true;
      AccountFlow.summaryPayableDetails(this.queryParams)
        .then(
          ({
            data: {
              payableDetailsList,
              receivableDetailsListTotal,
              totalOpeningBalance,
              totalCurrentPayable,
              totalCurrentPayment,
              totalClosingBalance
            }
          }) => {
            this.dataList = payableDetailsList || [];
            this.pagination.total = receivableDetailsListTotal;
            this.totalCount = {
              totalOpeningBalance,
              totalCurrentPayable,
              totalCurrentPayment,
              totalClosingBalance
            };
          }
        )
        .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadList();
    this.loadOrderStaff();
    this.loadSupplier();
    this.loadSupplierCategory();
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
