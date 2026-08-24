<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="addForm()">新 增</t-button>
        <t-button variant="outline" style="border-radius: 4px"           @click="approved()">审 核</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="backApproved()">反审核</t-button>
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
            placeholder="退单日期"
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
          <t-space v-if="row.orderStatus === '已保存'" size="small">
            <t-link theme="primary" @click="addForm('edit', row.id)">编辑</t-link>
            <t-link theme="primary" @click="doRemove(row)">删除</t-link>
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
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import Customer from "@js/api/basic/Customer";
import SalesReturn from "@js/api/sales/SalesReturn";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

export default {
  name: "SalesReturnList",
  watch: {
    '$store.state.currentTabDataReturn': {
      handler(newVal) {
        if (newVal && newVal.refresh) {
          this.loadList();
          this.$store.commit('SET_TAB_DATA_RETURN', null);
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
        {colKey: 'ops', title: '操作', width: 110, fixed: 'left', align: 'center'},
        {colKey: 'returnDate', title: '退单日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '单据编号', minWidth: 160, ellipsis: true},
        {colKey: 'salesOutboundNos', title: '关联销售出库单', minWidth: 140, ellipsis: true},
        {colKey: 'customerName', title: '客户', minWidth: 120, ellipsis: true},
        {colKey: 'totalAmount', title: '销售金额', width: 110, align: 'right'},
        {colKey: 'discountAmount', title: '折扣金额', width: 110, align: 'right'},
        {colKey: 'refundAmount', title: '退款金额', width: 110, align: 'right'},
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
        refundAmount: sum('refundAmount'),
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
      this.$store.commit('SET_TAB_DATA_RETURN', {type, orderId});
      this.pushTab({
        key: 'SalesReturnForm',
        title: type === 'edit' ? '编辑销售退货单' : '新增销售退货单',
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
          return SalesReturn.approved('已审核', ids).then(() => {
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
          return SalesReturn.approved('已保存', ids).then(() => {
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
          return SalesReturn.remove(row.id).then(() => {
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
      SalesReturn.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    loadTotal() {
      SalesReturn.total(this.queryParams).then(({data}) => {
        this.amountTotal = data?.amount || 0;
        this.totalQuantity = data?.quantity || 0;
      })
    },
  },
  created() {
    this.loadCustomer();
    this.loadList();
    this.loadTotal();
  }
}
</script>

