<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="addForm()" color="primary">导 出</Button>
        <Button>打 印</Button>
      </template>
      <template #tools>
        <!--        <Select v-model="params.salesType" class="w-120px" :datas="{all:'全部',out:'销货',return:'退货'}"-->
        <Select v-model="params.salesType" class="w-120px" :datas="{out:'销货',return:'退货'}"
                placeholder="业务类别："/>
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
                show-search-button class="w-280px ml-8px"
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
<!--        <vxe-column type="checkbox" width="40" align="center"/>-->
        <vxe-column title="销售日期" field="orderDate" align="center" width="130"/>
        <vxe-column title="订单编号" field="orderNo" width="200"/>
        <vxe-column title="业务类别" field="orderType" width="200" :formatter="formatOrderType"/>
        <vxe-column title="客户" field="customerName" min-width="120"/>
        <vxe-column title="商品编码" field="productCode" width="100"/>
        <vxe-column title="商品名称" field="productName" width="100"/>
        <vxe-column title="销售单位" field="unitName" width="100"/>
        <vxe-column title="仓库名称" field="warehouseName" width="100"/>
        <vxe-column title="数量" field="quantity" width="100"/>
        <vxe-column title="单价" field="unitPrice" width="100"/>
        <!--        <vxe-column title="折扣金额" field="discountValue" width="120"/>-->
        <vxe-column title="销售收入" field="subtotal" width="120"/>
      </vxe-table>
    </div>
    <div class="flex justify-between items-center pt-5px">
      <vxe-pager perfect @page-change="loadList(false)"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'Total']">
        <template #left>
<!--          <span class="mr-12px text-16px">总金额：{{ amountTotal }}元</span>-->
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
import SalesReport from "@js/api/sales/SalesReport";
import Customer from "@js/api/basic/Customer";
import {loading, message} from "heyui.ext";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "SalesItemReport",
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
        salesType: 'out'
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
    formatOrderType({ cellValue }) {
      if (this.params.salesType === 'return') {
        return '退货';
      }
      if (this.params.salesType === 'out') {
        return '销货';
      }
      return cellValue || '销货'; // 如果 cellValue 为空，则返回默认值 '销货'
    },
    footerMethod({columns, data}) {
      let quantityTotal = 0;
      let subtotalTotal = 0;
      columns.forEach((column) => {
        if (column.property && ['quantity', 'subtotal'].includes(column.property)) {

          data.forEach((row) => {
            let rd = row[column.property];
            if (column.property === 'quantity') {
              if (rd) {
                quantityTotal += Number(rd || 0);
              }
            } else if (column.property === 'subtotal') {
              if (rd) {
                subtotalTotal += Number(rd || 0);
              }
            }
          });
        }
      })
      return [["", "", "", "", "", "", "", "", "", quantityTotal, "", subtotalTotal]];
    },
    doSearch() {
      this.pagination.page = 1;
      if(!this.params.salesType){
        message.error("请选择业务类型~");
        return
      }
      this.loadList();
    },
    loadList(type = true) {
      this.loading = true;
      SalesReport.salesItem(this.queryParams).then(({data: {results, total}}) => {
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
    this.loadList();
  }
}
</script>
