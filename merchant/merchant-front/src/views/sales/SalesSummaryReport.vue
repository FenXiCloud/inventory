<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="exportData" color="primary">导 出</Button>
        <Button @click="printEvent">打 印</Button>
      </template>
      <template #tools>
        <Select v-model="params.salesGroup" class="w-120px" :datas="{PRODUCT:'商品',PRODUCT_WAREHOUSE:'商品+仓库'}"
                placeholder="汇总条件："/>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">日期：</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">客户：</span>
          <Select class="w-120px" filterable :datas="customerList" keyName="id" titleName="name"
                  v-model="params.customerId" placeholder="请选择客户"  />
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">仓库：</span>
          <Select v-model="params.warehouseId" class="w-100px" keyName="id" titleName="name" :datas="warehouseList" placeholder="请选择仓库"/>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">商品：</span>
          <Select v-model="params.productId" class="w-100px" keyName="id" titleName="name" :datas="productList" placeholder="请选择商品"/>
        </div>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-180px ml-8px"
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
<!--        <vxe-column title="单据日期" field="orderDate" align="center" width="130"/>-->
<!--        <vxe-column title="订单编号" field="orderNo" width="200"/>-->
<!--        <vxe-column title="业务类别" field="orderType" width="200" :formatter="formatOrderType"/>-->
<!--        <vxe-column title="客户" field="customerName" min-width="120"/>-->
        <vxe-column title="商品编码" field="productCode" />
        <vxe-column title="商品名称" field="productName" />
        <vxe-column title="销售单位" field="unitName" />
        <vxe-column title="仓库名称" field="warehouseName" />
        <vxe-column title="数量" field="quantity" />
        <vxe-column title="单价" field="unitPrice" />
        <!--        <vxe-column title="折扣金额" field="discountValue" width="120"/>-->
        <vxe-column title="金额" field="subtotal"/>

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
import SalesOutbound from "@js/api/sales/SalesOutbound";
import {mapMutations} from "vuex";
import SalesReport from "@js/api/sales/SalesReport";
import Customer from "@js/api/basic/Customer";
import {loading} from "heyui.ext";
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "SalesSummaryReport",
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
        salesGroup: 'PRODUCT'
      },
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd")
      },
      customerList: [],
      warehouseList: [],
      productList: [],
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
      this.quantityTotal = quantityTotal.toFixed(2);
      this.subtotalTotal = subtotalTotal.toFixed(2);
      return [["", "", "", "",quantityTotal.toFixed(2), "", subtotalTotal.toFixed(2)]];
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadList(type = true) {
      this.loading = true;
      SalesReport.salesSummary(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);

      Promise.all([
        Customer.select(),
        Warehouse.select(),
        Product.select(),
      ]).then((results) => {
        this.customerList = results[0].data || [];
        this.warehouseList = results[1].data || [];
        this.productList = results[2].data || [];

      }).finally(() => loading.close());
    },
  },
  created() {
    this.loadList();
  }
}
</script>
