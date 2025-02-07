<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="addForm()" color="primary">新 增</Button>
        <Button @click="batchAudit()" > 审 核</Button>
      </template>
      <template #tools>
        <Select v-model="params.state" class="w-120px" :datas="{已保存:'未审核',已审核:'已审核'}"
                placeholder="审核状态："/>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">订单日期：</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">客户：</span>
          <Select class="w-178px" filterable :datas="customerList" keyName="id" titleName="name"
                  v-model="params.customerId" placeholder="请选择客户"  />
        </div>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-360px ml-8px"
                placeholder="请输入订单号" @search="doSearch">
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
        <vxe-column title="退单日期" field="returnDate" align="center" width="130"/>
        <vxe-column title="单据编号" field="orderNo" width="200"/>
        <vxe-column title="关联销售退货单" field="code" width="200"/>
        <vxe-column title="客户" field="customerName" min-width="120"/>
        <vxe-column title="销售金额" field="totalAmount" width="120"/>
        <vxe-column title="折扣金额" field="discountAmount" width="120"/>
        <vxe-column title="折后金额" field="finalAmount" width="120"/>
        <vxe-column title="制单人" field="createdName" align="center" width="100"/>
        <vxe-column title="制单时间" field="createdAt" align="center" width="100"/>
        <vxe-column title="状态" field="orderStatus" width="80"/>

      </vxe-table>
    </div>
    <div class="flex justify-between items-center pt-5px">
      <vxe-pager perfect @page-change="loadList(false)"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'Total']">
        <template #left>
          <span class="mr-12px text-16px">总金额：{{ amountTotal }}元</span>
          <vxe-button @click="loadList(false)" type="text" size="mini" icon="h-icon-refresh"
                      :loading="loading"></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>
<script>
import manba from "manba";
import {mapMutations} from "vuex";
import {confirm, loading, message} from "heyui.ext";
import Customer from "@js/api/basic/Customer";
import SalesOutbound from "@js/api/sales/SalesOutbound";
import SalesReturn from "@js/api/sales/SalesReturn";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "SalesOutboundList",
  watch: {
    // 监听 store 中的 currentTabData
    '$store.state.currentTabData': {
      handler(newVal) {
        console.log('currentTabData changed:', newVal)
        if (newVal && newVal.refresh) {
          this.loadList();
          // 重置刷新标志
          this.$store.commit('SET_TAB_DATA', null);
        }
      },
      deep: true
    }
  },
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
        customerId: null
      },
      customerList: [],
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
      this.$store.commit('SET_TAB_DATA', {type, orderId});
      this.pushTab({
        key: 'SalesReturnForm',
        title: type === 'edit' ? '编辑销售退货单' : '新增销售退货单',
      });
    },

    batchAudit() {
      const selectedRows = this.$refs.table.getCheckboxRecords();
      console.log(selectedRows);
      if (selectedRows.length === 0) {
        message.error("请选择至少一个订单进行审核");
        return;
      }

      confirm({
        content: `确定批量审核订单？`,
        onConfirm: () => {
          const orderIds = selectedRows.map(row => row.id);
          let params = {
            orderIds: orderIds
          };
          SalesReturn.batchAudit(params).then((success) => {
            if (success) {
              message.success("批量审核成功");
              this.loadList(); // Refresh the list
            }
          }).finally(() =>
              loading.close()
          );
        }
      })
    },
    doRemove(row) {
      console.log(row)
      confirm({
        title: "系统提示",
        content: `确认删除：${row.orderNo}?`,
        onConfirm: () => {
          SalesOutbound.remove(row.id).then(() => {
            message("删除成功~");
            this.loadList();
          })
        }
      })
    },
    footerMethod({columns, data}) {
      let totalAmount = 0;
      let discountAmount = 0;
      let finalAmount = 0;
      columns.forEach((column) => {
        if (column.property && ['totalAmount', 'discountAmount', 'finalAmount'].includes(column.property)) {

          data.forEach((row) => {
            let rd = row[column.property];
            if (column.property === 'totalAmount') {
              if (rd) {
                totalAmount += Number(rd || 0);
              }
            } else if (column.property === 'discountAmount') {
              if (rd) {
                discountAmount += Number(rd || 0);
              }
            } else if (column.property === 'finalAmount') {
              if (rd) {
                finalAmount += Number(rd || 0);
              }
            }
          });
        }
      })
      this.amountTotal = totalAmount;
      return [["", "", "", "", "", "", totalAmount.toFixed(2), discountAmount.toFixed(2), finalAmount.toFixed(2)]];
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadList(type = true) {
      this.loading = true;
      SalesReturn.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);

      Promise.all([
        Customer.select(),
      ]).then((results) => {
        this.customerList = results[0].data || [];
      }).finally(() => loading.close());
    },
  },
  created() {
    console.log('created', "SalesOutboundList")
    this.loadList();
  }
}
</script>
