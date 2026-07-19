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
            v-model="params.customerId"
            :options="customerList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="选择客户"
            style="width: 180px; border-radius: 4px"
        />
        <t-input
            v-model="params.keyword"
            clearable
            placeholder="客户名称/单据编号"
            style="width: 220px; background: #fff; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch" />
          </template>
        </t-input>
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
      <span class="simple-page__total">增加应收：{{ amountTotal }} / 增加预收：{{ prepayTotal }}</span>
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
import Customer from '@js/api/basic/Customer';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');
const money = (v) => Number(v || 0).toFixed(2);

export default {
  name: 'CustomerFlowReport',
  data() {
    return {
      dataList: [],
      customerList: [],
      pagination: { page: 1, pageSize: 20, total: 0 },
      loading: false,
      amountTotal: '0.00',
      prepayTotal: '0.00',
      params: { customerId: null, keyword: null },
      dateRangeValue: [startTime, endTime],
      columns: [
        { colKey: 'orderDate', title: '单据日期', width: 120, align: 'center' },
        { colKey: 'orderNo', title: '单据编号', minWidth: 180, ellipsis: true },
        { colKey: 'customerName', title: '客户', minWidth: 130, ellipsis: true },
        { colKey: 'staffName', title: '销售人员', width: 110, align: 'center' },
        { colKey: 'businessType', title: '业务类型', width: 110, align: 'center' },
        { colKey: 'receivableAmount', title: '增加应收款', width: 120, align: 'right' },
        { colKey: 'prepaymentAmount', title: '增加预收款', width: 120, align: 'right' },
        { colKey: 'balance', title: '应收款余额', width: 120, align: 'right' },
        { colKey: 'remarks', title: '备注', minWidth: 120, ellipsis: true },
      ]
    };
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return {
        ...this.params,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startTime: start || null,
        endTime: end || null
      };
    },
    tableData() {
      return (this.dataList || []).map((row, index) => ({
        ...row,
        rowKey: `${row.orderNo || ''}_${index}`,
        receivableAmount: money(row.receivableAmount),
        prepaymentAmount: money(row.prepaymentAmount),
        balance: money(row.balance)
      }));
    },
    footData() {
      const sum = (key) => money((this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0));
      return [{
        orderNo: '合计',
        receivableAmount: sum('receivableAmount'),
        prepaymentAmount: sum('prepaymentAmount'),
        balance: sum('balance'),
      }];
    }
  },
  methods: {
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadCustomer() {
      Customer.select().then(({ data }) => {
        this.customerList = data || [];
      });
    },
    loadList() {
      this.loading = true;
      AccountFlow.getReceivableDetailReport(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total || 0;
          this.amountTotal = money(this.dataList.reduce((acc, row) => acc + Number(row.receivableAmount || 0), 0));
          this.prepayTotal = money(this.dataList.reduce((acc, row) => acc + Number(row.prepaymentAmount || 0), 0));
        })
        .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadCustomer();
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
