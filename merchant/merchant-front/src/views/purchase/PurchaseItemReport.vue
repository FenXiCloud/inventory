<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button color="primary" class="mt-5px">导出</Button>
        <Button class=" mt-5px">打 印</Button>
      </template>
      <template #tools>
        <div class="h-input-group mt-5px">
          <Select v-model="params.orderType" class="w-120px" :datas="{in:'入库单',out:'退货单',all:'全部'}"
                  placeholder="订单类型：" :deletable="false"/>
          <span class="h-input-addon ml-8px ">订单日期：</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
        <Select class="ml-8px mt-5px" required :datas="warehouseList" keyName="id" titleName="name"
                v-model="warehouseIds" placeholder="请选择仓库" :multiple="true"/>
        <Select class="ml-8px mt-5px" required :datas="supplierList" keyName="id" titleName="name"
                v-model="supplierIds" placeholder="请选择供货商" :multiple="true"/>
        <Select class="ml-8px mt-5px" :multiple="true" :datas="supplierCategoryList" keyName="id" titleName="name"
                v-model="supplierCategoryIds" placeholder="请选择供货商类别"/>
        <Select class="ml-8px mt-5px" required :datas="productList" keyName="id" titleName="name"
                v-model="productIds" placeholder="请选择商品" :multiple="true"/>
        <Select class="ml-8px mt-5px" :multiple="true" :datas="productCategoryList" keyName="id" titleName="name"
                v-model="productCategoryIds" placeholder="请选择商品类别"/>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-260px ml-8px mt-5px"
                placeholder="请输入订单号/供货商名称" @search="doSearch">
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
        <vxe-column title="商品信息" width="300">
          <template #default="{row,rowIndex}">
            <div class="flex">
              <div class="flex1 ml-8px">
                <div>{{ row.productCode }}--{{ row.productName }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="规格型号" field="spec" align="center" width="130"/>
        <vxe-column title="商品类别" field="categoryName" align="center" width="130"/>
        <vxe-column title="订单日期" field="orderDate" align="center" width="130"/>
        <vxe-column title="订单编号" field="orderNo" width="200"/>
        <vxe-column title="供货商" field="supplierName" min-width="120"/>
        <vxe-column title="供货商编码" field="supplierCode" min-width="120"/>
        <vxe-column title="供货商类别" field="supplierCategoryName" min-width="120"/>
        <vxe-column title="仓库名称" field="warehouseName" min-width="120"/>
        <vxe-column title="订单类型" field="orderType" min-width="120"/>
        <vxe-column title="采购单位" field="secondaryUnitName" width="120"/>
        <vxe-column title="采购数量" field="secondaryQuantity" width="120"/>
        <vxe-column title="采购单价" field="secondaryPrice" width="120"/>
        <vxe-column title="采购金额" field="subtotal" width="120"/>
      </vxe-table>
    </div>
    <div class=" justify-between items-center pt-5px">
      <vxe-pager perfect @page-change="loadList(false)"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'Total']">
        <template #left>
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
import PurchaseReport from "@js/api/purchase/PurchaseReport";
import Supplier from "@js/api/basic/Supplier";
import Warehouse from "@js/api/basic/Warehouse";
import {loading} from "heyui.ext";
import Product from "@js/api/basic/Product";
import ProductCategory from "@js/api/basic/ProductCategory";
import SupplierCategory from "@js/api/basic/SupplierCategory";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "PurchaseItemReport",
  data() {
    return {
      dataList: [],
      productList: [],
      warehouseList: [],
      productCategoryList: [],
      supplierCategoryList: [],
      supplierCategoryIds: [],
      productCategoryIds: [],
      supplierList: [],
      supplierIds: [],
      productIds: [],
      warehouseIds: [],
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
        supplierIds: null,
        productIds: null,
        warehouseIds: null,
        orderType: "in",
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
        productCategoryIds: this.productCategoryIds.map(item => item).toString(),
        supplierCategoryIds: this.supplierCategoryIds.map(item => item).toString(),
        supplierIds: this.supplierIds.map(item => item).toString(),
        productIds: this.productIds.map(item => item).toString(),
        warehouseIds: this.warehouseIds.map(item => item).toString(),
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
      let sums = [];
      columns.forEach((column) => {
        if (column.property && ['secondaryQuantity', 'secondaryPrice', 'subtotal'].includes(column.property)) {
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
      return [["", "", "", "", "", "", "", "", "", "", "", ""].concat(sums)];
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadSelect() {
      Promise.all([
        Supplier.select(),
        Warehouse.select(),
        Product.select(),
        ProductCategory.select(),
        SupplierCategory.select(),
      ]).then((results) => {
        this.supplierList = results[0].data || [];
        this.warehouseList = results[1].data || [];
        this.productList = results[2].data || [];
        this.productCategoryList = results[3].data || [];
        this.supplierCategoryList = results[4].data || [];
      }).finally(() => loading.close());
    },
    loadList(type = true) {
      this.loading = true;
      PurchaseReport.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
  },
  created() {
    this.loadList();
    this.loadSelect();
  }
}
</script>
