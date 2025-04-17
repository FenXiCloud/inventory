<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button color="primary">导 出</Button>
        <Button>打 印</Button>
      </template>
      <template #tools>
        <Select v-model="groupValues" class="w-240px ml-8px"
                :datas="{product:'商品',supplier:'供货商',warehouse:'仓库'}"
                placeholder="统计字段：" :multiple="true"/>
        <DateRangePicker v-model="dateRange" class="w-220px ml-8px"></DateRangePicker>
        <Select class="ml-8px"  :datas="supplierList" keyName="id" titleName="name"
                v-model="supplierIds" placeholder="请选择供货商" :multiple="true"/>
        <Select class="ml-8px"  :datas="warehouseList" keyName="id" titleName="name"
                v-model="warehouseIds" placeholder="请选择仓库" :multiple="true"/>
        <Select class="ml-8px" :multiple="true" :datas="supplierCategoryList" keyName="id" titleName="name"
                v-model="supplierCategoryIds" placeholder="请选择仓库类别"/>
        <Select class="ml-8px"  :datas="productList" keyName="id" titleName="name"
                v-model="productIds" placeholder="请选择商品" :multiple="true"/>
        <Select class="ml-8px" :multiple="true" :datas="productCategoryList" keyName="id" titleName="name"
                v-model="productCategoryIds" placeholder="请选择商品类别"/>
        <Button class="ml-8px" @click="doSearch" color="primary">查 询</Button>
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
        <vxe-column title="商品信息" width="300" v-if="isProduct">
          <template #default="{row,rowIndex}">
            <div class="flex">
              <div class="flex1 ml-8px">
                <div>{{ row.productCode }}--{{ row.productName }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="商品规格" field="spec" min-width="120" v-if="isProduct"/>
        <vxe-column title="商品类别" field="categoryName" min-width="120" v-if="isProduct"/>
        <vxe-column title="供货商" field="supplierName" min-width="120" v-if="isSupplier"/>
        <vxe-column title="供货商类别" field="supplierCategoryName" min-width="120" v-if="isSupplier"/>
        <vxe-column title="仓库名称" field="warehouseName" min-width="120" v-if="isWarehouse"/>
        <vxe-column title="基本数量" field="baseQuantitySum" min-width="200"/>
        <vxe-column title="基本单位" field="baseUnitName" min-width="200"/>
        <vxe-column title="总计" field="subtotalSum" min-width="120"/>
      </vxe-table>
    </div>
    <div class=" justify-between items-center pt-5px">
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
import PurchaseOrder from "@js/api/purchase/PurchaseOrder";
import {mapMutations} from "vuex";
import Supplier from "@js/api/basic/Supplier";
import Warehouse from "@js/api/basic/Warehouse";
import Product from "@js/api/basic/Product";
import {loading, message} from "heyui.ext";
import PurchaseReport from "@js/api/purchase/PurchaseReport";
import ProductCategory from "@js/api/basic/ProductCategory";
import SupplierCategory from "@js/api/basic/SupplierCategory";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "PurchaseSummaryReport",
  data() {
    return {
      dataList: [],
      productList: [],
      productCategoryList: [],
      supplierCategoryList: [],
      warehouseList: [],
      supplierList: [],
      supplierCategoryIds: [],
      productCategoryIds: [],
      productIds: [],
      supplierIds: [],
      warehouseIds: [],
      loading: false,
      isWarehouse: true,
      isSupplier: true,
      isProduct: true,
      amountTotal: 0,
      totalParams: {},
      groupValues: ['product', 'supplier', 'warehouse'],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {},
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd")
      },
    }
  },
  computed: {
    queryParams() {
      if (!this.groupValues || this.groupValues.length == 0) {
        message.error("请统计字段~");
      }
      if (this.groupValues) {
        this.isProduct = this.groupValues.find(item => item === 'product');
        this.isSupplier = this.groupValues.find(item => item === 'supplier');
        this.isWarehouse = this.groupValues.find(item => item === 'warehouse');
      }
      return Object.assign(this.params, {
        productCategoryIds: this.productCategoryIds.map(item => item).toString(),
        supplierCategoryIds: this.supplierCategoryIds.map(item => item).toString(),
        supplierIds: this.supplierIds.map(item => item).toString(),
        productIds: this.productIds.map(item => item).toString(),
        warehouseIds: this.warehouseIds.map(item => item).toString(),
        groupValues: this.groupValues.map(item => item).toString(),
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
      PurchaseReport.listStat(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
  },
  created() {
    this.loadSelect();
    this.loadList();
  }
}
</script>
