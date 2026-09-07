<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" v-auth="'salesOutbound:edit'" @click="addForm()">新 增</t-button>
        <t-button style="border-radius: 4px" v-auth="'salesOutbound:edit'" @click="showImportForm()">导 入</t-button>
        <t-button style="border-radius: 4px" @click="exportToExcel()">导 出</t-button>
        <t-button variant="outline" style="border-radius: 4px" v-auth="'salesOutbound:delete'" @click="batchDelete()">批量删除</t-button>
        <t-button variant="outline" style="border-radius: 4px" v-auth="'salesOutbound:audit'"          @click="approved()">审 核</t-button>
        <t-button variant="outline" style="border-radius: 4px" v-auth="'salesOutbound:audit'" @click="backApproved()">反审核</t-button>
        <t-select
            v-model="params.state"
            :options="stateOptions"
            clearable
            placeholder="审核状态"
            style="width: 140px; border-radius: 4px"
        />
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
          :foot-data="footData"
          @select-change="onSelectChange"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="viewDetail(row)">详情</t-link>
            <template v-if="row.orderStatus === '已保存'">
              <t-link theme="primary" v-auth="'salesOutbound:edit'" @click="addForm('edit', row.id)">编辑</t-link>
              <t-link theme="primary" v-auth="'salesOutbound:delete'" @click="doRemove(row)">删除</t-link>
            </template>
            <template v-if="row.orderStatus === '已审核'">
              <t-link theme="primary" @click="showQuickReceipt(row)">便捷收款</t-link>
              <t-link theme="primary" @click="generateInvoice(row)">生成发票</t-link>
            </template>
          </t-space>
        </template>
        <template #orderStatus="{ row }">
          <t-tag
              :theme="row.orderStatus === '已审核' ? 'success' : 'warning'"
              variant="light"
          >
            {{ row.orderStatus === '已保存' ? '未审核' : row.orderStatus }}
          </t-tag>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total">合计金额：{{ amountTotal }}元&nbsp;&nbsp;合计数量：{{ totalQuantity }}&nbsp;&nbsp;</span>
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
import {mapMutations} from "vuex";
import {h} from "vue";
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import Customer from "@js/api/basic/Customer";
import SalesOutboundImportForm from "@views/sales/SalesOutboundImportForm.vue";
import SalesOutbound from "@js/api/sales/SalesOutbound";
import QuickPaymentDialog from "@views/common/QuickPaymentDialog.vue";
import {downloadBlob} from 'download.js';

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

export default {
  name: "SalesOutboundList",
  watch: {
    '$store.state.currentTabData': {
      handler(newVal) {
        if (newVal && newVal.refresh) {
          this.loadList();
          this.$store.commit('SET_TAB_DATA', null);
        }
      },
      deep: true
    }
  },
  data() {
    return {
      dataList: [],
      customerList: [],
      selectedRowKeys: [],
      selectedRows: [],
      loading: false,
      amountTotal: 0,
      totalQuantity: 0,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        state: null,
        sortCol: null,
        sort: null,
        customerId: null
      },
      dateRangeValue: [startTime, endTime],
      stateOptions: [
        {label: '未审核', value: '已保存'},
        {label: '已审核', value: '已审核'},
      ],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'ops', title: '操作', width: 200, fixed: 'left', align: 'center'},
        {colKey: 'outboundDate', title: '出库日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '订单编号', minWidth: 160, ellipsis: true},
        {colKey: 'salesOrderNos', title: '关联销售订单', minWidth: 140, ellipsis: true},
        {colKey: 'customerName', title: '客户', minWidth: 120, ellipsis: true},
        {colKey: 'totalAmount', title: '销售金额', width: 110, align: 'right'},
        {colKey: 'discountAmount', title: '折扣金额', width: 110, align: 'right'},
        {colKey: 'finalAmount', title: '折后金额', width: 110, align: 'right'},
        {colKey: 'totalQuantity', title: '数量', width: 90, align: 'right'},
        {colKey: 'createdName', title: '制单人', width: 90, align: 'center'},
        {colKey: 'createdAt', title: '制单时间', width: 160, align: 'center', ellipsis: true},
        {colKey: 'settlementStatus', title: '结算状态', width: 100, align: 'center', fixed: 'right'},
        {colKey: 'invoiceStatus', title: '开票状态', width: 100, align: 'center', fixed: 'right'},
        {colKey: 'orderStatus', title: '状态', width: 100, align: 'center', fixed: 'right'},
      ]
    }
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: start || null,
        end: end || null,
      })
    },
    footData() {
      const sum = (key, digits = 2) => {
        const total = (this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
        return total.toFixed(digits);
      };
      const totalAmount = sum('totalAmount');
      return [{
        ops: '合计',
        totalAmount,
        discountAmount: sum('discountAmount'),
        finalAmount: sum('finalAmount'),
        totalQuantity: sum('totalQuantity'),
      }];
    },
  },
  methods: {
    ...mapMutations(['pushTab']),
    onSelectChange(keys, {selectedRowData}) {
      this.selectedRowKeys = keys;
      this.selectedRows = selectedRowData || [];
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    addForm(type = 'add', orderId = null) {
      sessionStorage.setItem('SalesOutboundList_filters', JSON.stringify({
        params: this.params, dateRangeValue: this.dateRangeValue
      }));
      this.pushTab({
        key: 'SalesOutboundForm',
        title: type === 'edit' ? '编辑销售出库' : '新增销售出库',
        params: {type, orderId},
      });
    },
    viewDetail(row) {
      this.pushTab({
        key: 'SalesOutboundForm',
        title: '销售出库详情',
        params: { type: 'edit', orderId: row.id },
      });
    },
    generateInvoice(row) {
      this.pushTab({
        key: 'InvoiceIssue',
        title: '开票 - ' + row.orderNo,
        params: { sourceType: 'SALES_OUTBOUND', sourceId: row.id },
      });
    },
    showQuickReceipt(row) {
      const dialogId = openDialog({
        header: '便捷收款',
        closeOnOverlayClick: false,
        width: '500px',
        body: h(QuickPaymentDialog, {
          type: 'receipt',
          order: row,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    },
    showImportForm() {
      const dialogId = openDialog({
        header: '导入销售出库单',
        closeOnOverlayClick: false,
        width: '50vw',
        body: h(SalesOutboundImportForm, {
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    },
    exportToExcel() {
      this.loading = true;
      SalesOutbound.exportToExcel(this.queryParams)
        .then((blob) => {
          downloadBlob('销售出库单.xlsx', blob);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    batchDelete() {
      if (!this.selectedRows.length) return MessagePlugin.warning('请先选择要删除的单据');
      const ids = this.selectedRows.map(r => r.id);
      DialogPlugin.confirm({
        header: "批量删除",
        body: `确认删除选中的 ${ids.length} 张单据？`,
        onConfirm: () => {
          LoadingPlugin(true);
          const tasks = ids.map(id => SalesOutbound.remove(id));
          Promise.all(tasks).then(() => {
            MessagePlugin.success(`成功删除 ${ids.length} 张单据`);
            this.clearSelection();
            this.loadList();
          }).finally(() => LoadingPlugin(false));
        }
      });
    },
    clearSelection() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    approved() {
      if (!this.selectedRows.length) {
        MessagePlugin.error("未选择数据~");
        return;
      }
      const ids = this.selectedRows.filter(val => val.orderStatus == '已保存').map(val => val.id);
      if (!ids.length) {
        MessagePlugin.error("所选数据无需审核~");
        return;
      }
      DialogPlugin.confirm({
        header: "批量审核提示",
        body: `本次审核${ids.length}条?`,
        onConfirm: () => {
          return SalesOutbound.approved('已审核', ids).then(() => {
            MessagePlugin.success("操作成功~");
            this.clearSelection();
            this.loadList();
          })
        }
      })
    },
    backApproved() {
      if (!this.selectedRows.length) {
        MessagePlugin.error("未选择数据~");
        return;
      }
      const ids = this.selectedRows.filter(val => val.orderStatus == '已审核').map(val => val.id);
      if (!ids.length) {
        MessagePlugin.error("所选数据无需反审核~");
        return;
      }
      DialogPlugin.confirm({
        header: "批量反审核提示",
        body: `本次反审核${ids.length}条?`,
        onConfirm: () => {
          return SalesOutbound.approved('已保存', ids).then(() => {
            MessagePlugin.success("操作成功~");
            this.clearSelection();
            this.loadList();
          })
        }
      })
    },
    doRemove(row) {
      if (row.orderStatus !== '已保存') {
        MessagePlugin.warning('已审核单据不能删除');
        return;
      }
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除：${row.orderNo}?`,
        onConfirm: () => {
          return SalesOutbound.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadList();
          })
        }
      })
    },
    doSearch() {
      this.pagination.page = 1;
      this.clearSelection();
      this.loadList();
      this.loadTotal();
    },
    loadCustomer() {
      Customer.select().then(({data}) => {
        this.customerList = data || [];
      });
    },
    loadList() {
      this.loading = true;
      SalesOutbound.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    loadTotal() {
      SalesOutbound.total(this.queryParams).then(({data}) => {
        this.amountTotal = data?.amount || 0;
        this.totalQuantity = data?.quantity || 0;
      })
    },
  },
  created() {
    const saved = sessionStorage.getItem('SalesOutboundList_filters');
    if (saved) {
      try {
        const f = JSON.parse(saved);
        if (f.params) Object.assign(this.params, f.params);
        if (f.dateRangeValue) this.dateRangeValue = f.dateRangeValue;
      } catch(e) {}
    }
    this.loadCustomer();
    this.loadList();
    this.loadTotal();
  }
}
</script>

