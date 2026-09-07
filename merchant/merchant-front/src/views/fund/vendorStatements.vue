<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="exportData">导 出</t-button>
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
      >
        <template #empty>{{ emptyText }}</template>
      </t-table>
    </div>
  </div>
</template>
<script>
import manba from 'manba';
import * as XLSX from 'xlsx';
import { MessagePlugin } from 'tdesign-vue-next';
import AccountFlow from '@js/api/fund/AccountFlow';
import Supplier from '@js/api/basic/Supplier';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');
const money = (v) => Number(v || 0).toFixed(2);

export default {
  name: 'VendorStatements',
  data() {
    return {
      summaryData: null,
      loading: false,
      searched: false,
      params: { supplierId: null },
      supplierDataList: [],
      dateRangeValue: [startTime, endTime],
      columns: [
        { colKey: 'item', title: '项目', width: 200, align: 'left' },
        { colKey: 'amount', title: '金额', width: 180, align: 'right' },
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
        startTime: start || null,
        endTime: end || null
      };
    },
    tableData() {
      if (!this.summaryData) return [];
      // 期末余额 = 期初余额 + 本期采购 - 本期优惠 - 本期付款
      const openingBalance = Number(this.summaryData.openingBalance || 0);
      const totalPurchase = Number(this.summaryData.totalPurchaseAmount || 0);
      const totalPaid = Number(this.summaryData.totalActualPaymentAmount || 0);
      const totalDiscount = Number(this.summaryData.totalPreferentialAmount || 0);
      const closingBalance = openingBalance + totalPurchase - totalDiscount - totalPaid;
      return [
        { item: '期初余额', amount: money(openingBalance) },
        { item: '本期采购金额', amount: money(totalPurchase) },
        { item: '本期优惠金额', amount: money(totalDiscount) },
        { item: '本期付款金额', amount: money(totalPaid) },
        { item: '期末余额', amount: money(closingBalance) },
      ];
    }
  },
  methods: {
    exportData() {
      if (!this.summaryData) {
        MessagePlugin.warning('没有可导出的数据');
        return;
      }
      try {
        const exportData = this.tableData.map(item => ({
          '项目': item.item,
          '金额': item.amount,
        }));
        const ws = XLSX.utils.json_to_sheet(exportData);
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, '供应商对账');
        XLSX.writeFile(wb, `供应商对账单_${manba().format('YYYY-MM-DD')}.xlsx`);
        MessagePlugin.success('导出成功');
      } catch (error) {
        console.error('导出错误:', error);
        MessagePlugin.error('导出失败');
      }
    },

    doSearch() {
      if (!this.params.supplierId) {
        return MessagePlugin.warning('请选择供应商后再查询');
      }
      if (!this.dateRangeValue || !this.dateRangeValue[0] || !this.dateRangeValue[1]) {
        return MessagePlugin.warning('请选择单据日期');
      }
      this.loadSummary();
    },
    loadSupplier() {
      Supplier.select().then(({ data }) => {
        this.supplierDataList = data || [];
      });
    },
    loadSummary() {
      this.loading = true;
      this.searched = true;
      AccountFlow.supplierStatementSummary(this.queryParams)
        .then(({ data }) => {
          this.summaryData = data || null;
        })
        .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadSupplier();
  }
};
</script>

