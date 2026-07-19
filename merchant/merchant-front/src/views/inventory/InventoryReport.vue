<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="excel" color="primary">导出</Button>
      </template>
      <template #tools>
        <div class="h-input-group h-table-checkbox-wrap">
          <span class="h-input-addon ml-8px">仓库：</span>
          <Select v-model="params.warehouseIds" :filterable="true" :multiple="true" class="w-120px" keyName="id"
                  titleName="name" :datas="warehouseList"/>
        </div>
        <div class="h-input-group h-table-checkbox-wrap">
          <span class="h-input-addon ml-8px">产品：</span>
          <Select v-model="params.productIds" :filterable="true" :multiple="true" class="w-120px" keyName="id"
                  titleName="name" :datas="productList"/>
        </div>
        <div class="h-input-group h-table-checkbox-wrap">
          <span class="h-input-addon ml-8px">产品类别：</span>
          <Select v-model="params.productCategoryIds" :filterable="true" :multiple="true" keyName="id" titleName="name"
                  class="w-120px"
                  :datas="productCategoryList"/>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">日期：</span>
          <DatePicker v-model="dateRange.start" :clearable="false"></DatePicker>
        </div>
        <Search v-model.trim="params.filter"
                show-search-button class="w-360px ml-8px"
                placeholder="请输入产品编号/名称/类别/规格" @search="doSearch">
          <t-icon name="search" />
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
        <vxe-column title="产品图片" field="productUrl" align="center" width="130"/>
        <vxe-column title="产品编码" field="productCode" width="200"/>
        <vxe-column title="产品名称" field="productName" width="200"/>
        <vxe-column title="产品类别" field="productCategoryName" min-width="120"/>
        <vxe-column title="规格型号" field="productSpecification" width="120"/>
        <vxe-column title="单位" field="productUnitName" width="120"/>
        <vxe-colgroup align="center" title="全部仓库">
          <vxe-column title="单位数量" field="all_quantity" align="center" width="130"/>
          <!--          <vxe-column title="多单位数量" field="orderDate" align="center" width="130"/>-->
          <vxe-column title="单位成本" field="all_averageCost" align="center" width="130"/>
          <vxe-column title="成本小计" field="all_totalCost" align="center" width="130"/>
        </vxe-colgroup>
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
          <vxe-button @click="loadList(false)" type="text" size="mini" icon="vxe-icon-refresh"
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
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import {exportExcelHeader} from "@js/excel";
import InventoryItem from "../../js/api/inventory/InventoryItem";

// const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
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
        productCategoryIds: [],
        productIds: [],
        warehouseIds: [],
        warehouseId: null,
        productId: null,
        productCategoryId: null,
        filter: null,
        state: null,
        sortCol: null,
        sort: null,
      },
      dateRange: {
        start: manba(endTime).format("YYYY-MM-dd"),
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
      let allQuantity = 0;
      let allTotalCost = 0;
      columns.forEach((column) => {
        if (column.property && ['all_quantity'].includes(column.property)) {
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              allQuantity += Number(rd || 0);
            }
          });
        }
        if (column.property && ['all_totalCost'].includes(column.property)) {
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              allTotalCost += Number(rd || 0);
            }
          });
        }
      });
      sums.push(allQuantity);
      sums.push('');
      sums.push(allTotalCost.toFixed(2));
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
      const params = JSON.parse(JSON.stringify(this.queryParams));
      params.productCategoryIds = params.productCategoryIds.join(",");
      params.productIds = params.productIds.join(",");
      params.warehouseIds = params.warehouseIds.join(",");
      Promise.all([InventoryItem.balance(params)])
          .then(results => {
            this.reportInventoryList = results[0].data.results || [];
            this.amountTotal = 0;
            Inventory.report(params).then(({data: {results, total}}) => {
              this.dataList = results || [];
              this.pagination.total = total;
              this.dataList.forEach(item => {
                this.warehouseList.forEach(warehouse => {
                  let allQuantity = 0;
                  let allAverageCost = 0;
                  let allTotalCost = 0;
                  this.reportInventoryList.forEach(report => {
                    if (report.warehouseId === warehouse.id && report.productId === item.productId) {
                      item[`${warehouse.code}_quantity`] = report.currentQuantity;
                      item[`${warehouse.code}_averageCost`] = report.averageCost;
                      item[`${warehouse.code}_totalCost`] = report.totalCost;
                      this.amountTotal += parseFloat(report.totalCost);
                    }
                    if (report.productId === item.productId) {
                      allQuantity += Number(report.currentQuantity || 0);
                      allAverageCost += Number(report.averageCost || 0);
                      allTotalCost += Number(report.totalCost || 0);
                    }
                  });
                  item['all_quantity'] = allQuantity;
                  item['all_averageCost'] = (allTotalCost / allQuantity).toFixed(2);
                  item['all_totalCost'] = allTotalCost;
                  if (isNaN(item['all_averageCost'])) {
                    item['all_averageCost'] = 0;
                  }
                });
              });
              this.amountTotal = this.amountTotal.toFixed(2);
            }).finally(() => this.loading = false);
          })
    },
    loadDict(callback) {
      LoadingPlugin(true);
      Promise.all([Product.select(), Warehouse.select(), ProductCategory.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            this.warehouseList = results[1].data || [];
            this.productCategoryList = results[2].data || [];
            callback();
          })
          .finally(() => LoadingPlugin(false));
    },
    excel() {
      const params = JSON.parse(JSON.stringify(this.queryParams));
      params.page = 1;
      params.pageSize = 999999;
      params.productCategoryIds = params.productCategoryIds.join(",");
      params.productIds = params.productIds.join(",");
      params.warehouseIds = params.warehouseIds.join(",");
      Promise.all([Inventory.reportInventory(params)])
          .then(results => {
            let reportInventoryList = results[0].data || [];
            Inventory.report(params).then(({data: {results, total}}) => {
              let dataList = results || [];
              dataList.forEach(item => {
                this.warehouseList.forEach(warehouse => {
                  let allQuantity = 0;
                  let allAverageCost = 0;
                  let allTotalCost = 0;
                  reportInventoryList.forEach(report => {
                    if (report.warehouseId === warehouse.id && report.productId === item.productId) {
                      item[`${warehouse.code}_quantity`] = report.currentQuantity;
                      item[`${warehouse.code}_averageCost`] = report.averageCost;
                      item[`${warehouse.code}_totalCost`] = report.totalCost;
                    }
                    if (report.productId === item.productId) {
                      allQuantity += Number(report.currentQuantity || 0);
                      allAverageCost += Number(report.averageCost || 0);
                      allTotalCost += Number(report.totalCost || 0);
                    }
                  });
                  item['all_quantity'] = allQuantity;
                  item['all_averageCost'] = (allTotalCost / allQuantity).toFixed(2);
                  item['all_totalCost'] = allTotalCost;
                });
              });
              this.callExcel(dataList);
            }).finally(() => this.loading = false);
          })
    },
    callExcel(dataList) {
      if (dataList.length < 0) {
        MessagePlugin.warning("暂无数据～");
        return;
      }
      let headList = [
        {label: "产品图片", key: "productUrl"},
        {label: "产品编码", key: "productCode"},
        {label: "产品名称", key: "productName"},
        {label: "产品类别", key: "productCategoryName"},
        {label: "规格型号", key: "productSpecification"},
        {label: "单位", key: "productUnitName"},
        {label: "单位数量", key: "all_quantity"},
        {label: "单位成本", key: "all_averageCost"},
        {label: "成本小计", key: "all_totalCost"},
      ];
      const tHeader = ['产品图片', '产品编码', '产品名称', '产品类别', '规格型号', '单位', '全部仓库', null, null];
      const secondHeader = [null, null, null, null, null, null, '单位数量', '单位成本', '成本小计'];
      const merges = [
        {s: {r: 0, c: 0}, e: {r: 1, c: 0}},
        {s: {r: 0, c: 1}, e: {r: 1, c: 1}},
        {s: {r: 0, c: 2}, e: {r: 1, c: 2}},
        {s: {r: 0, c: 3}, e: {r: 1, c: 3}},
        {s: {r: 0, c: 4}, e: {r: 1, c: 4}},
        {s: {r: 0, c: 5}, e: {r: 1, c: 5}},
        {s: {r: 0, c: 6}, e: {r: 0, c: 8}},
      ];
      let start = 6;
      let end = 8;
      this.warehouseList.forEach(warehouse => {
        start += 3;
        end += 3;
        headList.push({label: "单位数量", key: `${warehouse.code}_quantity`});
        headList.push({label: "单位成本", key: `${warehouse.code}_averageCost`});
        headList.push({label: "成本小计", key: `${warehouse.code}_totalCost`});
        tHeader.push(`${warehouse.code}-${warehouse.name}`);
        tHeader.push(null);
        tHeader.push(null);
        secondHeader.push('单位数量');
        secondHeader.push('单位成本');
        secondHeader.push('成本小计');
        merges.push({s: {r: 0, c: start}, e: {r: 0, c: end}});
      });
      const list = [secondHeader];
      // 处理传递数据
      exportExcelHeader(dataList, tHeader, headList, merges, list, manba(new Date()).format("YYYYMMddHHmmss") + "_库存余额");
    }
  },
  created() {
    this.loadDict(() => {
      this.loadList();
    });
  }
}
</script>
