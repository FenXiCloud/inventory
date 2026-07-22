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
            :options="supplierList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="选择供应商"
            style="width: 180px; border-radius: 4px"
        />
        <t-input
            v-model="params.keyword"
            clearable
            placeholder="供应商名称/单据编号"
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
      <span class="simple-page__total">增加应付：{{ amountTotal }} / 增加预付：{{ prepayTotal }}</span>
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
import AccountFlow from '@js/api/fund/AccountFlow';
import Supplier from '@js/api/basic/Supplier';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');
const money = (v) => Number(v || 0).toFixed(2);

export default {
  name: 'SupplierFlowReport',
  data() {
    return {
      dataList: [],
      supplierList: [],
      pagination: { page: 1, pageSize: 20, total: 0 },
      loading: false,
      amountTotal: '0.00',
      prepayTotal: '0.00',
      params: { supplierId: null, keyword: null },
      dateRangeValue: [startTime, endTime],
      columns: [
        { colKey: 'orderDate', title: '单据日期', width: 120, align: 'center' },
        { colKey: 'orderNo', title: '单据编号', minWidth: 180, ellipsis: true },
        { colKey: 'supplierName', title: '供应商', minWidth: 130, ellipsis: true },
        { colKey: 'staffName', title: '业务员', width: 110, align: 'center' },
        { colKey: 'businessType', title: '业务类型', width: 110, align: 'center' },
        { colKey: 'payableAmount', title: '增加应付款', width: 120, align: 'right' },
        { colKey: 'prepaymentAmount', title: '增加预付款', width: 120, align: 'right' },
        { colKey: 'balance', title: '应付款余额', width: 120, align: 'right' },
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
        payableAmount: money(row.payableAmount),
        prepaymentAmount: money(row.prepaymentAmount),
        balance: money(row.balance)
      }));
    },
    footData() {
      const sum = (key) => money((this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0));
      return [{
        orderNo: '合计',
        payableAmount: sum('payableAmount'),
        prepaymentAmount: sum('prepaymentAmount'),
        balance: sum('balance'),
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
          '业务日期': item.orderDate,
          '单据编号': item.orderNo,
          '供应商': item.supplierName,
          '业务员': item.staffName,
          '业务类型': item.businessType,
          '增加应付账款': item.payableAmount,
          '增加预付款': item.prepaymentAmount,
          '应付款余额': item.balance,
          '备注': item.remarks,
        }));
        const ws = XLSX.utils.json_to_sheet(exportData);
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, '供应商流水');
        XLSX.writeFile(wb, `供应商流水报表_${manba().format('YYYY-MM-DD')}.xlsx`);
        MessagePlugin.success('导出成功');
      } catch (error) {
        console.error('导出错误:', error);
        MessagePlugin.error('导出失败');
      }
    },

    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadSupplier() {
      Supplier.select().then(({ data }) => {
        this.supplierList = data || [];
      });
    },
    loadList() {
      this.loading = true;
      AccountFlow.payableDetail(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total || 0;
          this.amountTotal = money(this.dataList.reduce((acc, row) => acc + Number(row.payableAmount || 0), 0));
          this.prepayTotal = money(this.dataList.reduce((acc, row) => acc + Number(row.prepaymentAmount || 0), 0));
        })
        .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadSupplier();
    this.loadList();
  }
};
</script>

