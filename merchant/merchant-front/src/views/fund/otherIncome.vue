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
            placeholder="单据类型"
            style="width: 140px; border-radius: 4px"
        />
        <t-select
            v-model="params.orderStaffId"
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
      <span class="simple-page__total">{{ amountName }}合计：{{ amountTotal }}元</span>
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
import OrderStaff from '@js/api/basic/OrderStaff';
import AccountFlow from '@js/api/fund/AccountFlow';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');
const money = (v) => Number(v || 0).toFixed(2);

export default {
  name: 'otherIncome',
  data() {
    return {
      dataList: [],
      orderStaffList: [],
      pagination: { page: 1, pageSize: 20, total: 0 },
      loading: false,
      amountName: '收入金额',
      amountTotal: '0.00',
      params: { type: 1, orderStaffId: null },
      dateRangeValue: [startTime, endTime],
      typeOptions: [
        { label: '其他收入', value: 1 },
        { label: '其他支出', value: 2 }
      ]
    };
  },
  computed: {
    columns() {
      return [
        { colKey: 'date', title: '日期', width: 120, align: 'center' },
        { colKey: 'documentNumber', title: '单据编号', minWidth: 180, ellipsis: true },
        { colKey: 'accountType', title: '收支类别', minWidth: 120, ellipsis: true },
        { colKey: 'businessPartner', title: '往来单位', minWidth: 140, ellipsis: true },
        { colKey: 'staffName', title: '业务员', width: 110, align: 'center' },
        { colKey: 'amount', title: this.amountName, width: 130, align: 'right' },
      ];
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
      return (this.dataList || []).map((row, index) => ({
        ...row,
        rowKey: `${row.documentNumber || ''}_${index}`,
        amount: money(row.amount)
      }));
    },
    footData() {
      const total = money((this.dataList || []).reduce((acc, row) => acc + Number(row.amount || 0), 0));
      return [{ documentNumber: '合计', amount: total }];
    }
  },
  methods: {
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      if (!this.params.type) {
        return MessagePlugin.warning('请选择单据类型');
      }
      this.amountName = this.params.type == 2 ? '支出金额' : '收入金额';
      this.pagination.page = 1;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      AccountFlow.otherFundDetails(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total || 0;
          this.amountTotal = money(this.dataList.reduce((acc, row) => acc + Number(row.amount || 0), 0));
        })
        .finally(() => (this.loading = false));
    },
    loadOrderStaff() {
      OrderStaff.orderStaffList().then(({ data }) => {
        this.orderStaffList = data || [];
      });
    }
  },
  created() {
    this.loadOrderStaff();
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
