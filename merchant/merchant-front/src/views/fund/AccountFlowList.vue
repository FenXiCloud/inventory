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
            v-model="params.accountId"
            :options="accountList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="选择账户"
            style="width: 180px; border-radius: 4px"
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
      <span class="simple-page__total">收入：{{ incomeTotal }} / 支出：{{ spendingTotal }}</span>
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
import Account from '@js/api/fund/Account';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');

export default {
  name: 'cashBankStatements',
  data() {
    return {
      dataList: [],
      accountList: [],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      loading: false,
      incomeTotal: '0.00',
      spendingTotal: '0.00',
      params: {
        accountId: null
      },
      dateRangeValue: [startTime, endTime],
      columns: [
        { colKey: 'accountName', title: '账户', width: 130, align: 'center', ellipsis: true },
        { colKey: 'accountFlowType', title: '操作类型', width: 130, align: 'center' },
        { colKey: 'amount', title: '金额', width: 110, align: 'right' },
        { colKey: 'income', title: '收入', width: 110, align: 'right' },
        { colKey: 'spending', title: '支出', width: 110, align: 'right' },
        { colKey: 'correspondentsName', title: '交易对方名称', minWidth: 120, ellipsis: true },
        { colKey: 'amountOperatorName', title: '收付款人名称', minWidth: 120, ellipsis: true },
        { colKey: 'balanceBefore', title: '交易前余额', minWidth: 120, align: 'right' },
        { colKey: 'balanceAfter', title: '交易后余额', minWidth: 120, align: 'right' },
        { colKey: 'createdAt', title: '创建时间', minWidth: 160, ellipsis: true },
        { colKey: 'remarks', title: '备注', width: 120, ellipsis: true },
      ]
    };
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startTime: start || null,
        endTime: end || null
      });
    },
    footData() {
      const sum = (key) => {
        const total = (this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
        return total.toFixed(2);
      };
      return [{
        accountName: '合计',
        amount: sum('amount'),
        income: sum('income'),
        spending: sum('spending'),
      }];
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
      this.loadList();
    },
    loadAccount() {
      Account.select().then(({ data }) => {
        this.accountList = data || [];
      });
    },
    loadList() {
      this.loading = true;
      AccountFlow.list(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
          const incomeTotal = this.dataList.reduce((acc, row) => acc + Number(row.income || 0), 0);
          const spendingTotal = this.dataList.reduce((acc, row) => acc + Number(row.spending || 0), 0);
          this.incomeTotal = incomeTotal.toFixed(2);
          this.spendingTotal = spendingTotal.toFixed(2);
        })
        .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadAccount();
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
