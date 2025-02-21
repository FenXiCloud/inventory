<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #tools>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">仓库：</span>
          <Select v-model="params.warehouseId" class="w-120px" keyName="id" titleName="name" :datas="warehouseList"/>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">商品：</span>
          <Select v-model="params.productId" class="w-120px" keyName="id" titleName="name" :datas="productList"/>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">商品类别：</span>
          <Select v-model="params.productCategoryId" keyName="id" titleName="name" class="w-120px"
                  :datas="productCategoryList"/>
        </div>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-360px ml-8px"
                placeholder="请输入商品编号/名称/类别/规格" @search="doSearch">
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
        <vxe-column title="商品图片" field="productUrl" align="center" width="130"/>
        <vxe-column title="商品编码" field="productCode" width="200"/>
        <vxe-column title="商品名称" field="productName" width="200"/>
        <vxe-column title="商品类别" field="productCategoryName" min-width="120"/>
        <vxe-column title="规格型号" field="productSpecification" width="120"/>
        <vxe-column title="单位" field="productUnitName" width="120"/>
        <!--        <vxe-column title="多单位" field="finalAmount" width="120"/>-->
        <vxe-colgroup v-for="(item,index) in warehouseList" align="center" :title="item.code + '-'+ item.name"
                      :key="index">
          <vxe-column title="单位数量" :field="item.code + '_quantity'" align="center" width="130"/>
          <!--          <vxe-column title="多单位数量" field="orderDate" align="center" width="130"/>-->
          <vxe-column title="单位成本" :field="item.code + '_averageCost'" align="center" width="130"/>
          <vxe-column title="成本小计" :field="item.code + '_totalCost'" align="center" width="130"/>
        </vxe-colgroup>
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
import Inventory from "@js/api/inventory/Inventory";
import Product from "@js/api/basic/Product";
import ProductCategory from "@js/api/basic/ProductCategory";
import Warehouse from "@js/api/basic/Warehouse";
import {mapMutations} from "vuex";
import {loading} from "heyui.ext";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "InventoryReport",
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
        warehouseId: null,
        productId: null,
        productCategoryId: null,
        filter: null,
        state: null,
        sortCol: null,
        sort: null,
      },
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd")
      },
      warehouseList: [],
      productList: [],
      productCategoryList: [],
      reportInventoryList: [],
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
      let sums = ["合计", "", "", "", "", ""];
      let propertyNames = [];
      this.warehouseList.forEach((warehouse) => {
        propertyNames.push(warehouse.code + "_quantity");
        propertyNames.push(warehouse.code + "_totalCost");
      });
      let index = 1;
      console.info("propertyNames:", propertyNames);
      columns.forEach((column) => {
        if (column.property && propertyNames.includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              total += Number(rd || 0);
            }
          });
          if (index === 1) {
            sums.push(total);
          } else {
            if (column.property && column.property.indexOf('_quantity') > -1) {
              sums.push(total);
            } else {
              sums.push("");
              sums.push(total.toFixed(2));
            }

          }
          index++;
        }
      })
      console.log(sums);
      return [sums];
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadList(type = true) {
      this.loading = true;
      Promise.all([Inventory.reportInventory(this.queryParams)])
          .then(results => {
            this.reportInventoryList = results[0].data || [];
            this.amountTotal = 0;
            Inventory.report(this.queryParams).then(({data: {results, total}}) => {
              this.dataList = results || [];
              this.pagination.total = total;
              this.dataList.forEach(item => {
                this.warehouseList.forEach(warehouse => {
                  this.reportInventoryList.forEach(report => {
                    if (report.warehouseId === warehouse.id && report.productId === item.productId) {
                      item[`${warehouse.code}_quantity`] = report.currentQuantity;
                      item[`${warehouse.code}_averageCost`] = report.averageCost;
                      item[`${warehouse.code}_totalCost`] = report.totalCost;
                      this.amountTotal += parseFloat(report.totalCost);
                    }
                  });
                });
              });
              this.amountTotal = this.amountTotal.toFixed(2);
            }).finally(() => this.loading = false);
          })
    },
    loadDict(callback) {
      loading("加载中....");
      Promise.all([Product.select(), Warehouse.select(), ProductCategory.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            this.warehouseList = results[1].data || [];
            this.productCategoryList = results[2].data || [];
            callback();
          })
          .finally(() => loading.close());
    }
  },
  created() {
    this.loadDict(() => {
      this.loadList();
    });
  }
}
</script>
