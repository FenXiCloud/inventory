<template>
  <div class="simple-page">
    <div class="simple-page__tabs">
      <t-tabs v-model="activeTab" @change="loadList">
        <t-tab-panel value="customer" label="客户预收余额" />
        <t-tab-panel value="supplier" label="供应商预付余额" />
      </t-tabs>
    </div>

    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="exportData">导 出</t-button>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="loadList">刷新</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      客户预收余额、供应商预付余额取自往来单位档案余额（余额为负即表示预收/预付），此处列出存在预收/预付的单位及其金额。
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
          empty="暂无预收/预付余额"
      />
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total">{{ activeTab === 'customer' ? '预收余额' : '预付余额' }}合计：{{ totalText }}元</span>
    </div>
  </div>
</template>

<script>
import manba from 'manba';
import {MessagePlugin} from 'tdesign-vue-next';
import AdvanceBalance from '@js/api/fund/AdvanceBalance';
import { export_json_to_excel } from '@js/excel/export2Excel';

const money = (v) => Number(v || 0).toFixed(2);

export default {
  name: 'AdvanceBalanceReport',
  data() {
    return {
      loading: false,
      activeTab: 'customer',
      customerAdvance: [],
      supplierAdvance: [],
      customerTotal: 0,
      supplierTotal: 0,
      columns: [
        { colKey: 'code', title: '编码', width: 140, align: 'center' },
        { colKey: 'name', title: '名称', minWidth: 160, ellipsis: true },
        { colKey: 'advanceBalance', title: '余额', minWidth: 140, align: 'right' }
      ]
    };
  },
  computed: {
    tableData() {
      const list = this.activeTab === 'customer' ? this.customerAdvance : this.supplierAdvance;
      return (list || []).map((r) => ({...r, advanceBalance: money(r.advanceBalance)}));
    },
    footData() {
      const total = this.activeTab === 'customer' ? this.customerTotal : this.supplierTotal;
      return [{ code: '合计', name: '合计', advanceBalance: money(total) }];
    },
    totalText() {
      return money(this.activeTab === 'customer' ? this.customerTotal : this.supplierTotal);
    }
  },
  methods: {
    loadList() {
      this.loading = true;
      AdvanceBalance.advanceBalance()
        .then(({ data }) => {
          const d = data || {};
          this.customerAdvance = d.customerAdvance || [];
          this.supplierAdvance = d.supplierAdvance || [];
          this.customerTotal = d.customerTotal || 0;
          this.supplierTotal = d.supplierTotal || 0;
        })
        .finally(() => (this.loading = false));
    },
    exportData() {
      const isCustomer = this.activeTab === 'customer';
      const list = isCustomer ? this.customerAdvance : this.supplierAdvance;
      if (!list.length) {
        MessagePlugin.warning('没有可导出的数据');
        return;
      }
      export_json_to_excel({
        header: ['编码', '名称', '余额'],
        data: list.map((i) => [i.code, i.name, money(i.advanceBalance)]),
        filename: `${isCustomer ? '客户预收余额' : '供应商预付余额'}_${manba().format('YYYY-MM-DD')}`,
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
.simple-page__tabs {
  flex-shrink: 0;
  padding-top: 4px;
}

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

.simple-page__pager {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 8px;
}

.simple-page__total {
  font-size: 13px;
  color: #555;
}
</style>
