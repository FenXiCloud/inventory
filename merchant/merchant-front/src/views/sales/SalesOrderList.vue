<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" v-auth="'salesOrder:edit'" @click="addForm()">新 增</t-button>
        <t-button style="border-radius: 4px" v-auth="'salesOrder:edit'" @click="showImportForm()">导 入</t-button>
        <t-button variant="outline" style="border-radius: 4px" v-auth="'salesOrder:audit'" @click="approved()">审 核</t-button>
        <t-button variant="outline" style="border-radius: 4px" v-auth="'salesOrder:delete'" @click="batchDelete()">批量删除</t-button>
        <t-button variant="outline" style="border-radius: 4px" v-auth="'salesOrder:audit'" @click="backApproved()">反审核</t-button>
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
            placeholder="订单日期"
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
            <template v-if="row.orderStatus === '已保存'">
              <t-link theme="primary" v-auth="'salesOrder:edit'" @click="addForm('edit', row.id)">编辑</t-link>
              <t-link theme="primary" v-auth="'salesOrder:delete'" @click="doRemove(row)">删除</t-link>
            </template>
            <t-dropdown :min-column-width="110" @click="(item) => onMore(item, row)">
              <t-link theme="primary">更多</t-link>
              <template #dropdown>
                <t-dropdown-menu>
                  <t-dropdown-item v-if="$can('salesOrder:audit') && row.orderStatus === '已保存'" value="approve">审核</t-dropdown-item>
                  <t-dropdown-item v-if="row.orderStatus === '已保存'" value="cancel">取消</t-dropdown-item>
                  <t-dropdown-item v-if="row.orderStatus === '已审核' && row.status !== 2" value="toOutbound">转出库单</t-dropdown-item>
                  <t-dropdown-item value="detail">详情</t-dropdown-item>
                  <t-dropdown-item value="print">打印</t-dropdown-item>
                </t-dropdown-menu>
              </template>
            </t-dropdown>
          </t-space>
        </template>
        <template #status="{ row }">
          <span v-if="row.status === 0">未出库</span>
          <span v-else-if="row.status === 1">部分出库</span>
          <span v-else-if="row.status === 2">全部出库</span>
          <span v-else>{{ row.status }}</span>
        </template>
        <template #purchaseStatusText="{ row }">
          <t-tag v-if="row.purchaseStatus === 2" theme="success" variant="light" size="small">已采购</t-tag>
          <t-tag v-else-if="row.purchaseStatus === 1" theme="warning" variant="light" size="small">部分采购</t-tag>
          <span v-else class="text-gray-400">—</span>
        </template>
        <template #orderStatus="{ row }">
          <t-tag :theme="orderStatusTheme(row)" variant="light">
            {{ orderStatusLabel(row) }}
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
import SalesOrder from "@js/api/sales/SalesOrder";
import SalesOrderImportForm from "@views/sales/SalesOrderImportForm.vue";
import {mapMutations} from "vuex";
import {h} from "vue";
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import {openPrint} from '@common/print';
import Customer from "@js/api/basic/Customer";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

export default {
  name: "SalesOrderList",
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
        {label: '待审核', value: '已保存'},
        {label: '已审核', value: '已审核'},
        {label: '已取消', value: '已取消'},
      ],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'ops', title: '操作', width: 110, fixed: 'left', align: 'center'},
        {colKey: 'orderDate', title: '订单日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '订单编号', minWidth: 160, ellipsis: true},
        {colKey: 'status', title: '出库状态', width: 100, align: 'center'},
        {colKey: 'purchaseStatusText', title: '采购状态', width: 100, align: 'center'},
        {colKey: 'purchaseInOrderNos', title: '关联采购入库单', minWidth: 140, ellipsis: true},
        {colKey: 'outOrderNo', title: '关联销售出库单', minWidth: 140, ellipsis: true},
        {colKey: 'customerName', title: '客户', minWidth: 120, ellipsis: true},
        {colKey: 'totalAmount', title: '销售金额', width: 110, align: 'right'},
        {colKey: 'discountAmount', title: '折扣金额', width: 110, align: 'right'},
        {colKey: 'finalAmount', title: '折后金额', width: 110, align: 'right'},
        {colKey: 'totalQuantity', title: '数量', width: 90, align: 'right'},
        {colKey: 'createdName', title: '制单人', width: 90, align: 'center'},
        {colKey: 'createdAt', title: '制单时间', width: 160, align: 'center', ellipsis: true},
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
      sessionStorage.setItem('SalesOrderList_filters', JSON.stringify({
        params: this.params, dateRangeValue: this.dateRangeValue
      }));
      this.$store.commit('SET_TAB_DATA', {type, orderId});
      this.pushTab({
        key: 'SalesOrderForm',
        title: type === 'edit' ? '编辑销售订单' : '新增销售订单',
      });
    },
    showImportForm() {
      const dialogId = openDialog({
        header: '导入销售订单',
        closeOnOverlayClick: false,
        width: '50vw',
        body: h(SalesOrderImportForm, {
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
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
          Promise.all(ids.map(id => SalesOrder.remove(id))).then(() => {
            MessagePlugin.success(`成功删除 ${ids.length} 张单据`);
            this.clearSelection(); this.loadList();
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
          return SalesOrder.approved('已审核', ids).then(() => {
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
      const ids = this.selectedRows
          .filter(val => val.orderStatus == '已审核' && !val.outOrderNo)
          .map(val => val.id);
      if (!ids.length) {
        MessagePlugin.error("所选数据无需反审核~");
        return;
      }
      DialogPlugin.confirm({
        header: "批量反审核提示",
        body: `本次反审核${ids.length}条?`,
        onConfirm: () => {
          return SalesOrder.approved('已保存', ids).then(() => {
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
          return SalesOrder.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadList();
          })
        }
      })
    },
    orderStatusTheme(row) {
      if (row.status === 2) return 'primary';
      if (row.orderStatus === '已审核') return 'success';
      if (row.orderStatus === '已取消') return 'danger';
      return 'warning';
    },
    orderStatusLabel(row) {
      if (row.status === 2) return '已完成';
      if (row.orderStatus === '已审核') return '已审核';
      if (row.orderStatus === '已取消') return '已取消';
      return '待审核';
    },
    onMore(item, row) {
      const value = item && item.value;
      if (value === 'approve') return this.doApprove(row);
      if (value === 'cancel') return this.doCancel(row);
      if (value === 'toOutbound') return this.doToOutbound(row);
      if (value === 'detail') return this.addForm('edit', row.id);
      if (value === 'print') return this.doPrintRow(row);
    },
    doApprove(row) {
      DialogPlugin.confirm({
        header: "审核提示",
        body: `确认审核：${row.orderNo}?`,
        onConfirm: () => {
          return SalesOrder.approved('已审核', [row.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.loadList();
          })
        }
      })
    },
    doCancel(row) {
      DialogPlugin.confirm({
        header: "取消提示",
        body: `确认取消：${row.orderNo}?`,
        onConfirm: () => {
          return SalesOrder.approved('已取消', [row.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.loadList();
          })
        }
      })
    },
    doPrintRow(row) {
      SalesOrder.load(row.id).then(({data}) => {
        const order = data || {};
        const items = (order.salesOrderItemList || []).map((i) => ({
          productName: i.productName || i.productCode || '',
          quantity: i.quantity,
          price: i.unitPrice,
          amount: i.subtotal
        }));
        openPrint('销售订单', {
          header: {
            ...order,
            partner: row.customerName || '',
            amount: order.finalAmount ?? order.totalAmount
          },
          items
        });
      });
    },
    doToOutbound(row) {
      this.$store.commit('SET_TAB_DATA', {
        type: 'add',
        fromSalesOrder: true,
        customerId: row.customerId,
        orderIds: [row.id]
      });
      this.pushTab({
        key: 'SalesOutboundForm',
        title: '新增销售出库单',
      });
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
      SalesOrder.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    loadTotal() {
      SalesOrder.total(this.queryParams).then(({data}) => {
        this.amountTotal = data?.amount || 0;
        this.totalQuantity = data?.quantity || 0;
      })
    },
  },
  created() {
    const saved = sessionStorage.getItem('SalesOrderList_filters');
    if (saved) { try { const f = JSON.parse(saved); if (f.params) Object.assign(this.params, f.params); if (f.dateRangeValue) this.dateRangeValue = f.dateRangeValue; } catch(e) {} }
    this.loadCustomer();
    this.loadList();
    this.loadTotal();
  }
}
</script>

