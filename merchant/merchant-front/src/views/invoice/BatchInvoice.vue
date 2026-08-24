<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" :disabled="!selectedRowKeys.length" :loading="issuing" @click="batchIssue">批量开票</t-button>
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="出库日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-select
            v-model="params.customerId"
            :options="customerList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="请选择客户"
            style="width: 180px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入订单号"
            style="width: 220px; background: #fff; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">仅已审核且未开票的销售出库单可批量开票，选中单据后点击「批量开票」。</div>

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
          :selected-row-keys="selectedRowKeys"
          @select-change="onSelectChange"
      >
        <template #invoiceStatus="{ row }">
          <t-tag :theme="row.invoiceStatus === '已开票' ? 'success' : 'warning'" variant="light">{{ row.invoiceStatus || '未开票' }}</t-tag>
        </template>
        <template #finalAmount="{ row }"><span class="num">¥{{ fmt(row.finalAmount) }}</span></template>
      </t-table>
    </div>

    <div class="simple-page__pager">
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
import manba from "manba";
import SalesOutbound from "@js/api/sales/SalesOutbound";
import Customer from "@js/api/basic/Customer";
import Invoice from "@js/api/invoice/Invoice";
import {MessagePlugin} from "tdesign-vue-next";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

export default {
  name: "BatchInvoice",
  data() {
    return {
      dataList: [],
      loading: false,
      issuing: false,
      customerList: [],
      selectedRowKeys: [],
      dateRangeValue: [startTime, endTime],
      params: {
        state: '已审核',
        customerId: null,
        filter: '',
      },
      pagination: {page: 1, pageSize: 20, total: 0},
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'outboundDate', title: '出库日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '订单编号', minWidth: 160, ellipsis: true},
        {colKey: 'customerName', title: '客户', minWidth: 120, ellipsis: true},
        {colKey: 'finalAmount', title: '折后金额', width: 120, align: 'right'},
        {colKey: 'invoiceStatus', title: '开票状态', width: 110, align: 'center'},
      ],
    };
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: start || null,
        end: end || null,
      });
    },
  },
  methods: {
    fmt(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    onSelectChange(value) {
      this.selectedRowKeys = value;
    },
    loadSelect() {
      Customer.select().then(({data}) => {
        this.customerList = data || [];
      });
    },
    loadList() {
      this.loading = true;
      SalesOutbound.list(this.queryParams)
        .then(({data}) => {
          this.dataList = data?.results || [];
          this.pagination.total = data?.total || 0;
          this.selectedRowKeys = [];
        })
        .finally(() => (this.loading = false));
    },
    batchIssue() {
      if (!this.selectedRowKeys.length) {
        MessagePlugin.warning('请先选择要开票的销售出库单');
        return;
      }
      this.issuing = true;
      Invoice.batchIssue(this.selectedRowKeys)
        .then(({data}) => {
          const errors = data?.errors || [];
          if (errors.length) {
            MessagePlugin.warning((data?.message || '部分开票失败') + '：' + errors.join('；'));
          } else {
            MessagePlugin.success(data?.message || '批量开票成功');
          }
          this.loadList();
        })
        .finally(() => (this.issuing = false));
    },
  },
  created() {
    this.loadSelect();
    this.loadList();
  }
};
</script>

<style scoped>
.num {
  font-variant-numeric: tabular-nums;
}
</style>
