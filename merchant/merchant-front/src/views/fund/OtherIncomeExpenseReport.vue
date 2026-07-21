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
            v-model="params.orderStaffId"
            :options="orderStaffList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="选择业务员"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.type"
            :options="typeOptions"
            placeholder="选择单据类型"
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
import * as XLSX from 'xlsx';
import {MessagePlugin} from 'tdesign-vue-next';
import { mapMutations } from 'vuex';
import OrderStaff from '@js/api/basic/OrderStaff';
import AccountFlow from '@js/api/fund/AccountFlow';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');

/**
 * @功能描述: 其他收支明细报表
 */
export default {
  name: 'OtherIncomeExpenseReport',
  data() {
    return {
      dataList: [],
      orderStaffList: [],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      loading: false,
      amountName: '收入金额',
      amountTotal: '0.00',
      params: {
        type: 1,
        orderStaffId: null
      },
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
        { colKey: 'documentNumber', title: '单据编号', align: 'center', ellipsis: true },
        { colKey: 'date', title: '日期', align: 'center', width: 130 },
        { colKey: 'amount', title: this.amountName, align: 'right', width: 130 },
        { colKey: 'accountType', title: '收支类别', align: 'center', minWidth: 120 },
        { colKey: 'businessPartner', title: '往来单位', align: 'center', minWidth: 120, ellipsis: true },
        { colKey: 'staffName', title: '业务员', align: 'center', minWidth: 120 },
      ];
    },
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startTime: start || null,
        endTime: end || null
      });
    },
    tableData() {
      return (this.dataList || []).map((row, index) => ({
        ...row,
        rowKey: `${row.documentNumber || ''}_${row.date || ''}_${index}`
      }));
    },
    footData() {
      const total = (this.dataList || []).reduce((acc, row) => acc + Number(row.amount || 0), 0);
      return [{
        documentNumber: '合计',
        amount: total.toFixed(2),
      }];
    }
  },
  methods: {
    exportData() {
      if (!this.dataList || this.dataList.length === 0) {
        MessagePlugin.warning('没有可导出的数据');
        return;
      }
      try {
        const exportData = this.dataList.map(item => ({
          '单据编号': item.documentNumber,
          '日期': item.date,
          '金额': item.amount,
          '收支类别': item.accountType,
          '往来单位': item.businessPartner,
          '业务员': item.staffName,
        }));
        const ws = XLSX.utils.json_to_sheet(exportData);
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, '其他收支');
        XLSX.writeFile(wb, `其他收支报表_${manba().format('YYYY-MM-DD')}.xlsx`);
        MessagePlugin.success('导出成功');
      } catch (error) {
        console.error('导出错误:', error);
        MessagePlugin.error('导出失败');
      }
    },

    ...mapMutations(['pushTab']),
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.amountName = this.params.type == 2 ? '支出金额' : '收入金额';
      this.pagination.page = 1;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      AccountFlow.otherFund(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
          const amountTotal = this.dataList.reduce((acc, row) => acc + Number(row.amount || 0), 0);
          this.amountTotal = amountTotal.toFixed(2);
        })
        .finally(() => (this.loading = false));
    },
    loadOrderStaff() {
      OrderStaff.select().then(({ data }) => {
        this.orderStaffList = data || [];
      });
    }
  },
  created() {
    this.loadList();
    this.loadOrderStaff();
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
