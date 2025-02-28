<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="excel" color="primary">导出</Button>
      </template>
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
import {loading, message} from "heyui.ext";
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";
import ProductCategory from "@js/api/basic/ProductCategory";
import {exportExcelHeader} from "@js/excel";

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
    },
    excel() {
      this.loading = true;
      const params = JSON.parse(JSON.stringify(this.queryParams));
      params.page = 1;
      params.pageSize = 1199999;
      InventoryItem.summary(params).then(({data: {results, total}}) => {
        const dataList = results || [];
        InventoryItem.summaryOperationType(params).then(({data}) => {
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
          this.callExcel(dataList);
        }).finally(() => this.loading = false);
      });
    },
    callExcel(dataList) {
      if (dataList.length < 0) {
        message.warn("暂无数据～");
        return;
      }
      let headList = [
        {label: "商品编号", key: "productCode"},
        {label: "商品名称", key: "productName"},
        {label: "商品类别", key: "productCategoryName"},
        {label: "规格型号", key: "productSpecification"},
        {label: "单位", key: "unitName"},
        {label: "仓库", key: "warehouseName"},
        {label: "数量", key: "inQuantity"},
        {label: "成本", key: "inSubtotal"},
        {label: "数量", key: "outQuantity"},
        {label: "成本", key: "outSubtotal"},
        {label: "数量", key: "costQuantity"},
        {label: "成本", key: "costSubtotal"},
        {label: "数量", key: "currentQuantity"},
        {label: "成本", key: "totalCost"},
      ];
      const tHeader = ['商品编号', '商品名称', '商品类别', '规格型号', '单位', '仓库', '入库', null, '出库', null, '成本调整', null, '结存', null];
      const secondHeader = [null, null, null, null, null, null, '数量', '成本', '数量', '成本', '数量', '成本', '数量', '成本'];
      const merges = [
        {s: {r: 0, c: 0}, e: {r: 1, c: 0}},
        {s: {r: 0, c: 1}, e: {r: 1, c: 1}},
        {s: {r: 0, c: 2}, e: {r: 1, c: 2}},
        {s: {r: 0, c: 3}, e: {r: 1, c: 3}},
        {s: {r: 0, c: 4}, e: {r: 1, c: 4}},
        {s: {r: 0, c: 5}, e: {r: 1, c: 5}},
        {s: {r: 0, c: 6}, e: {r: 0, c: 7}},
        {s: {r: 0, c: 8}, e: {r: 0, c: 9}},
        {s: {r: 0, c: 10}, e: {r: 0, c: 11}},
        {s: {r: 0, c: 12}, e: {r: 0, c: 13}},
      ];
      const list = [secondHeader];
      // 处理传递数据
      exportExcelHeader(dataList, tHeader, headList, merges, list, manba(new Date()).format("YYYYMMddHHmmss") + "_进销存汇总表");
    }
  },
  created() {
    this.loadDict(() => {

    });
    this.loadList();
  }
}
</script>
