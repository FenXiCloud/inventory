<template>
  <div class="simple-page">
    <div class="quota-cards">
      <div class="quota-card">
        <div class="quota-card__label">开票总限额</div>
        <div class="quota-card__value">¥{{ fmt(totalQuota) }}</div>
      </div>
      <div class="quota-card">
        <div class="quota-card__label">已使用额度</div>
        <div class="quota-card__value quota-card__value--warning">¥{{ fmt(usedQuota) }}</div>
      </div>
      <div class="quota-card">
        <div class="quota-card__label">剩余额度</div>
        <div class="quota-card__value quota-card__value--blue">¥{{ fmt(remainingQuota) }}</div>
      </div>
      <div class="quota-card">
        <div class="quota-card__label">累计开票张数</div>
        <div class="quota-card__value">{{ invoiceCount }} 张</div>
      </div>
    </div>

    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" variant="outline" :loading="loading" @click="load">刷新</t-button>
        <t-button theme="primary" variant="outline" @click="exportData">导 出</t-button>
      </t-space>
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="month"
          size="medium"
          bordered
          hover
          height="100%"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
      />
    </div>
  </div>
</template>

<script>
import Invoice from '@js/api/invoice/Invoice';
import {MessagePlugin} from 'tdesign-vue-next';
import {export_json_to_excel} from '@js/excel/export2Excel';

export default {
  name: 'InvoiceStatistics',
  data() {
    return {
      loading: false,
      totalQuota: 0,
      usedQuota: 0,
      remainingQuota: 0,
      invoiceCount: 0,
      dataList: [],
      columns: [
        {colKey: 'month', title: '月份', width: 120, align: 'center'},
        {colKey: 'blueCount', title: '蓝字张数', width: 110, align: 'right'},
        {colKey: 'redCount', title: '红字张数', width: 110, align: 'right'},
        {colKey: 'amount', title: '金额（不含税）', width: 140, align: 'right'},
        {colKey: 'tax', title: '税额', width: 120, align: 'right'},
        {colKey: 'totalAmount', title: '价税合计', width: 140, align: 'right'}
      ]
    };
  },
  computed: {
    footData() {
      if (!this.dataList.length) return [];
      const foot = {month: '合计'};
      ['blueCount', 'redCount', 'amount', 'tax', 'totalAmount'].forEach((k) => {
        foot[k] = Number(this.dataList.reduce((acc, row) => acc + (Number(row[k]) || 0), 0).toFixed(2));
      });
      return [foot];
    }
  },
  methods: {
    load() {
      this.loading = true;
      Promise.all([Invoice.stats(), Invoice.outputAggregation()])
        .then(([statsRes, aggRes]) => {
          const s = statsRes.data || {};
          this.totalQuota = s.totalQuota || 0;
          this.usedQuota = s.usedQuota || 0;
          this.remainingQuota = s.remainingQuota || 0;
          this.invoiceCount = s.invoiceCount || 0;
          this.dataList = aggRes.data || [];
        })
        .catch(() => {})
        .finally(() => (this.loading = false));
    },
    exportData() {
      if (!this.dataList.length) {
        MessagePlugin.warning('暂无数据～');
        return;
      }
      const header = this.columns.map((c) => c.title);
      const data = this.dataList.map((row) => this.columns.map((c) => row[c.colKey] ?? ''));
      export_json_to_excel({header, data, filename: '开票统计', autoWidth: true, bookType: 'xlsx'});
    },
    fmt(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    }
  },
  created() {
    this.load();
  }
};
</script>

<style scoped>
.quota-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.quota-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 18px 22px;
  box-sizing: border-box;
}

.quota-card__label {
  font-size: 13px;
  color: #646a73;
  margin-bottom: 10px;
}

.quota-card__value {
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1;
}

.quota-card__value--warning {
  color: #ed7b2f;
}

.quota-card__value--blue {
  color: #0052d9;
}
</style>
