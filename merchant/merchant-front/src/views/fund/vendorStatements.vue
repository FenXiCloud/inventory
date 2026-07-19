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
            v-model="params.supplierId"
            :options="supplierDataList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="选择供应商"
            style="width: 200px; border-radius: 4px"
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
          :data="tableData"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
      >
        <template #empty>{{ emptyText }}</template>
      </t-table>
    </div>
    <div class="simple-page__pager">
      <span class="simple-page__total">实付合计：{{ amountTotal }}元</span>
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
import { MessagePlugin } from 'tdesign-vue-next';
import AccountFlow from '@js/api/fund/AccountFlow';
import Supplier from '@js/api/basic/Supplier';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');
const money = (v) => Number(v || 0).toFixed(2);

export default {
  name: 'vendorStatements',
  data() {
    return {
      dataList: [],
      pagination: { page: 1, pageSize: 20, total: 0 },
      loading: false,
      amountTotal: '0.00',
      searched: false,
      params: { supplierId: null },
      supplierDataList: [],
      dateRangeValue: [startTime, endTime],
      columns: [
        { colKey: 'businessDate', title: '单据日期', width: 120, align: 'center' },
        { colKey: 'businessNo', title: '单据编号', minWidth: 180, ellipsis: true },
        { colKey: 'supplierFlowType', title: '业务类型', width: 140, align: 'center' },
        { colKey: 'purchaseAmount', title: '采购金额', width: 120, align: 'right' },
        { colKey: 'preferentialAmount', title: '优惠金额', width: 110, align: 'right' },
        { colKey: 'copeWithAmount', title: '应付金额', width: 120, align: 'right' },
        { colKey: 'actualPaymentAmount', title: '实付金额', width: 120, align: 'right' },
        { colKey: 'balancePayable', title: '应付余额', width: 120, align: 'right' },
        { colKey: 'remarks', title: '备注', minWidth: 120, ellipsis: true },
      ]
    };
  },
  computed: {
    emptyText() {
      return this.searched ? '暂无数据' : '请选择供应商后查询';
    },
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
      return (this.dataList || []).map((row) => ({
        ...row,
        purchaseAmount: money(row.purchaseAmount),
        preferentialAmount: money(row.preferentialAmount),
        copeWithAmount: money(row.copeWithAmount),
        actualPaymentAmount: money(row.actualPaymentAmount),
        balancePayable: money(row.balancePayable)
      }));
    },
    footData() {
      const sum = (key) => money((this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0));
      return [{
        businessNo: '合计',
        purchaseAmount: sum('purchaseAmount'),
        preferentialAmount: sum('preferentialAmount'),
        copeWithAmount: sum('copeWithAmount'),
        actualPaymentAmount: sum('actualPaymentAmount'),
        balancePayable: sum('balancePayable'),
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
      if (!this.params.supplierId) {
        return MessagePlugin.warning('请选择供应商后再查询');
      }
      if (!this.dateRangeValue || !this.dateRangeValue[0] || !this.dateRangeValue[1]) {
        return MessagePlugin.warning('请选择单据日期');
      }
      this.pagination.page = 1;
      this.loadList();
    },
    loadSupplier() {
      Supplier.select().then(({ data }) => {
        this.supplierDataList = data || [];
      });
    },
    loadList() {
      this.loading = true;
      this.searched = true;
      AccountFlow.listBySupplier(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total || 0;
          this.amountTotal = money(this.dataList.reduce((acc, row) => acc + Number(row.actualPaymentAmount || 0), 0));
        })
        .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadSupplier();
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
