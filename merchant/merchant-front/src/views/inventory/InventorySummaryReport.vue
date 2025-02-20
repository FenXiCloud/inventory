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
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">单据日期：</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
        <Search v-model.trim="params.summaryFilter" search-button-theme="h-btn-default"
                show-search-button class="w-360px ml-8px"
                placeholder="请输入商品编码/商品名称" @search="doSearch">
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
        <vxe-column title="商品编号" field="productCode" align="center" width="130"/>
        <vxe-column title="商品名称" field="productName" width="200"/>
        <vxe-column title="商品类别" field="productCategoryName" width="200"/>
        <vxe-column title="规格型号" field="productSpecification" min-width="120"/>
        <vxe-column title="单位" field="unitName" width="120"/>
        <vxe-column title="仓库" field="warehouseName" width="120"/>
        <vxe-colgroup title="入库" align="center">
          <vxe-column title="数量" field="inQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="inSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="出库" align="center">
          <vxe-column title="数量" field="outQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="outSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="成本调整" align="center">
          <vxe-column title="数量" field="costQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="costSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="结存" align="center">
          <vxe-column title="数量" field="currentQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="totalCost" align="center" width="100"/>
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
import InventoryItem from "@js/api/inventory/InventoryItem";
import {mapMutations} from "vuex";
import {loading} from "heyui.ext";
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";
import ProductCategory from "@js/api/basic/ProductCategory";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "InventorySummaryReport",
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
        productId: null,
        warehouseId: null,
        productCategoryId: null,
        summaryFilter: null,
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
      let inQuantity = 0;
      let outQuantity = 0;
      let inTotal = 0;
      let outTotal = 0;
      let currentQuantity = 0;
      let totalCost = 0;
      let costSubtotal = 0;
      columns.forEach((column) => {
        if (column.property && ['inQuantity'].includes(column.property)) {
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              inQuantity += Number(rd || 0);
            }
          });
        }
        if (column.property && ['outQuantity'].includes(column.property)) {
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              outQuantity += Number(rd || 0);
            }
          });
        }
        if (column.property && ['inSubtotal'].includes(column.property)) {
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              inTotal += Number(rd || 0);
            }
          });
        }
        if (column.property && ['outSubtotal'].includes(column.property)) {
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              outTotal += Number(rd || 0);
            }
          });
        }
        if (column.property && ['currentQuantity'].includes(column.property)) {
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              currentQuantity += Number(rd || 0);
            }
          });
        }
        if (column.property && ['costSubtotal'].includes(column.property)) {
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              costSubtotal += Number(rd || 0);
            }
          });
        }
        if (column.property && ['totalCost'].includes(column.property)) {
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              totalCost += Number(rd || 0);
            }
          });
        }
      })
      return [['合计', '', '', '', '', '', inQuantity, inTotal.toFixed(2), outQuantity, outTotal.toFixed(2), '', costSubtotal.toFixed(2), currentQuantity, totalCost.toFixed(2)]];
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadList(type = true) {
      this.loading = true;
      InventoryItem.summary(this.queryParams).then(({data: {results, total}}) => {
        const dataList = results || [];
        this.pagination.total = total;
        InventoryItem.summaryOperationType(this.queryParams).then(({data}) => {
          dataList.forEach((row) => {
            const productId = row.productId;
            const warehouseId = row.warehouseId;
            data.forEach((data_row) => {
              if (data_row.warehouseId === warehouseId && data_row.productId === productId) {
                const operationType = data_row.operationType;
                let inQuantity = row.inQuantity;
                let inSubtotal = row.inSubtotal;
                let outQuantity = row.outQuantity;
                let outSubtotal = row.outSubtotal;
                let costSubtotal = row.costSubtotal;
                switch (operationType) {
                  case "入库":
                    row.inQuantity = data_row.quantity + (inQuantity || 0);
                    row.inSubtotal = data_row.subtotal + (inSubtotal || 0);
                    break;
                  case "出库":
                    row.outQuantity = data_row.quantity + (outQuantity || 0);
                    row.outSubtotal = data_row.subtotal + (outSubtotal || 0);
                    break;
                  case "成本调整":
                    row.costSubtotal = data_row.subtotal + (costSubtotal || 0);
                    break;
                  default:
                    break;
                }
              }
            });
          });
          this.dataList = dataList;
        }).finally(() => this.loading = false);
      });
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

    });
    this.loadList();
  }
}
</script>
