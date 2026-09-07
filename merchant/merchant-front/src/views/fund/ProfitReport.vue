<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-radio-group v-model="viewMode" variant="default-filled" @change="loadList">
          <t-radio-button value="summary">汇总</t-radio-button>
          <t-radio-button value="monthly">按月</t-radio-button>
        </t-radio-group>
        <t-button theme="primary" style="border-radius: 4px" @click="exportData">导 出</t-button>
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="统计日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      销售收入、销售成本按已审核销货单统计；其他收支取已审核其他收入/支出单。净利润 = 毛利 + 其他收入 − 其他支出。
    </div>

    <div class="simple-page__table">
      <t-table
          v-if="viewMode === 'summary'"
          row-key="label"
          size="medium"
          bordered
          stripe
          hover
          resizable
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
          empty="暂无利润数据"
      />
      <t-table
          v-else
          row-key="month"
          size="medium"
          bordered
          stripe
          hover
          resizable
          height="100%"
          table-layout="fixed"
          :data="monthlyList"
          :columns="monthlyColumns"
          :loading="loading"
          empty="暂无利润数据"
      />
    </div>
  </div>
</template>

<script>
import manba from 'manba';
import FundReport from '@js/api/fund/FundReport';
import { export_json_to_excel } from '@js/excel/export2Excel';
import { MessagePlugin } from 'tdesign-vue-next';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');

const ROWS = [
  { key: 'salesRevenue', label: '销售收入', positive: true },
  { key: 'salesCost', label: '销售成本', positive: false },
  { key: 'grossProfit', label: '毛利', positive: true },
  { key: 'otherIncome', label: '其他收入', positive: true },
  { key: 'otherExpense', label: '其他支出', positive: false },
  { key: 'netProfit', label: '净利润', positive: true },
];

const MONTHLY_COLUMNS = [
  { colKey: 'month', title: '月份', width: 120, align: 'center', fixed: 'left' },
  { colKey: 'salesRevenue', title: '销售收入', align: 'right' },
  { colKey: 'salesCost', title: '销售成本', align: 'right' },
  { colKey: 'grossProfit', title: '毛利', align: 'right' },
  { colKey: 'otherIncome', title: '其他收入', align: 'right' },
  { colKey: 'otherExpense', title: '其他支出', align: 'right' },
  { colKey: 'netProfit', title: '净利润', align: 'right' },
];

export default {
  name: 'ProfitReport',
  data() {
    return {
      loading: false,
      viewMode: 'summary',
      dataList: [],
      monthlyList: [],
      dateRangeValue: [startTime, endTime],
      columns: [
        { colKey: 'label', title: '指标', width: 200, align: 'center' },
        { colKey: 'amount', title: '金额', align: 'right' }
      ],
      monthlyColumns: MONTHLY_COLUMNS
    };
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return {
        start: start || null,
        end: end || null
      };
    },
    footData() {
      const net = Number((this.dataList.find(i => i.key === 'netProfit') || {}).amount || 0);
      return [{ label: '净利润', amount: net.toFixed(2) }];
    }
  },
  methods: {
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      const p = this.viewMode === 'monthly'
        ? FundReport.profitMonthly(this.queryParams).then(({ data }) => {
            this.monthlyList = (data || []).map((r) => ({
              month: r.month,
              salesRevenue: Number(r.salesRevenue || 0).toFixed(2),
              salesCost: Number(r.salesCost || 0).toFixed(2),
              grossProfit: Number(r.grossProfit || 0).toFixed(2),
              otherIncome: Number(r.otherIncome || 0).toFixed(2),
              otherExpense: Number(r.otherExpense || 0).toFixed(2),
              netProfit: Number(r.netProfit || 0).toFixed(2)
            }));
          })
        : FundReport.profit(this.queryParams).then(({ data }) => {
            const d = data || {};
            this.dataList = ROWS.map((r) => ({
              key: r.key,
              label: r.label,
              amount: Number(d[r.key] || 0).toFixed(2)
            }));
          });
      p.finally(() => (this.loading = false));
    },
    exportData() {
      if (this.viewMode === 'monthly') {
        if (!this.monthlyList.length) {
          MessagePlugin.warning('没有可导出的数据');
          return;
        }
        export_json_to_excel({
          header: MONTHLY_COLUMNS.map((c) => c.title),
          data: this.monthlyList.map((i) => [i.month, i.salesRevenue, i.salesCost, i.grossProfit, i.otherIncome, i.otherExpense, i.netProfit]),
          filename: `利润表_按月_${manba().format('YYYY-MM-DD')}`,
          autoWidth: true
        });
        return;
      }
      if (!this.dataList.length) {
        MessagePlugin.warning('没有可导出的数据');
        return;
      }
      export_json_to_excel({
        header: ['指标', '金额'],
        data: this.dataList.map((i) => [i.label, i.amount]),
        filename: `利润表_${manba().format('YYYY-MM-DD')}`,
        autoWidth: true
      });
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>

.simple-page__hint {
  flex-shrink: 0;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f3f3f3;
  color: #555;
  font-size: 13px;
  line-height: 1.6;
}

</style>
