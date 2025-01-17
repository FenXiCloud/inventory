<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="addForm()" color="primary">新增</Button>
        <Button>审核</Button>
        <Button>反审核</Button>
      </template>
      <template #tools>
        <Select v-model="params.state" class="w-120px" :datas="{已保存:'未审核',已审核:'已审核'}"
                placeholder="审核状态"/>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">订单日期：</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-360px ml-8px"
                placeholder="请输入订单号/客户名称" @search="doSearch">
          <i class="h-icon-search"/>
        </Search>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 height="auto"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 show-footer
                 :footer-method="footerMethod"
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :sort-config="{remote:true}"
                 :loading="loading">
        <vxe-column type="checkbox" width="40" align="center"/>
        <vxe-column title="操作" align="center" width="120">
          <template #default="{row}">
            <span class="primary-color  text-hover ml-10px" @click="addForm('edit',row.id)">编辑</span>
            <span class="primary-color  text-hover ml-10px" @click="doRemove(row)">删除</span>
          </template>
        </vxe-column>
        <vxe-column title="订单日期" field="orderDate" align="center" width="130"/>
        <vxe-column title="订单编号" field="orderNo" width="200"/>
        <vxe-column title="关联入库单" field="purchaseInboundId" width="200"/>
        <vxe-column title="供货商" field="supplierName" min-width="120"/>
        <vxe-column title="销售金额" field="totalAmount" width="120"/>
        <vxe-column title="折扣金额" field="discountAmount" width="120"/>
        <vxe-column title="折后金额" field="finalAmount" width="120"/>
        <vxe-column title="制单人" field="createdName" align="center" width="100"/>
        <vxe-column title="制单时间" field="orderDate" align="center" width="100"/>
        <vxe-column title="审核状态" field="orderStatus" width="80"/>

      </vxe-table>
    </div>
    <div class="justify-between pt-5px">
      <vxe-pager perfect @page-change="loadList(false)"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="[ 'PrevPage', 'Number', 'NextPage',  'Sizes', 'Total']">
        <template #left>
          <span class="mr-12px text-14px">合计金额：{{ amountTotal }}元</span>
          <vxe-button @click="loadList(false)" type="text" size="mini" icon="h-icon-refresh"
                      :loading="loading"></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>
<script>
import manba from "manba";
import PurchaseOrder from "@js/api/purchase/PurchaseOrder";
import {mapMutations} from "vuex";
import {message, confirm} from "heyui.ext";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "PurchaseOrderList",
  data() {
    return {
      dataList: [],
      loading: false,
      amountTotal: 0,
      totalParams: {},
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
      },
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd")
      },
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: this.dateRange.start,
        end: this.dateRange.end,
      })
    },
  },
  methods: {
    ...mapMutations(['pushTab']),
    addForm(type = 'add', orderId = null) {
      console.log(type, orderId);
      this.pushTab({
        key: 'PurchaseOrderForm',
        title: type == 'edit' ? '编辑采购订单' : '新增采购订单',
        params: {type: type, orderId: orderId}
      });
    },
    footerMethod({columns, data}) {
      let sums = [];
      columns.forEach((column) => {
        if (column.property && ['finalAmount'].includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              total += Number(rd || 0);
            }
          });
          sums.push(total.toFixed(2));
        }
      })
      return [["", "", "", "", "", ""].concat(sums)];
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadList(type = true) {
      this.loading = true;
      PurchaseOrder.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    doRemove(row) {
      console.log(row)
      confirm({
        title: "系统提示",
        content: `确认删除：${row.orderNo}?`,
        onConfirm: () => {
          PurchaseOrder.remove(row.id).then(() => {
            message("删除成功~");
            this.loadList();
          })
        }
      })
    },
  },
  created() {
    this.loadList();
  }
}
</script>
