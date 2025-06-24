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
          <span class="h-input-addon ml-8px">单据日期：</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
        <Search v-model.trim="params.summaryFilter" search-button-theme="h-btn-default"
                show-search-button class="w-360px ml-8px"
                placeholder="请输入产品编码/产品名称" @search="doSearch">
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
        <vxe-column title="产品编号" field="productCode" align="center" width="130"/>
        <vxe-column title="产品名称" field="productName" width="200"/>
        <vxe-column title="产品类别" field="productCategoryName" width="200"/>
        <vxe-column title="规格型号" field="productSpecification" min-width="120"/>
        <vxe-column title="单位" field="unitName" width="120"/>
        <vxe-column title="仓库" field="warehouseName" width="120"/>
        <vxe-colgroup title="期初" align="center">
          <vxe-column title="数量" field="initialQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="initialSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="采购入库" align="center">
          <vxe-column title="数量" field="purchaseStockQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="purchaseStockSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="销售退货" align="center">
          <vxe-column title="数量" field="salesReturnsQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="salesReturnsSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="调拨入库" align="center">
          <vxe-column title="数量" field="channelInQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="channelInSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="其他入库" align="center">
          <vxe-column title="数量" field="otherInQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="otherInSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="盘盈单" align="center">
          <vxe-column title="数量" field="takeProfitQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="takeProfitSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="入库合计" align="center">
          <vxe-column title="数量" field="inQuantityTotal" align="center" width="100"/>
          <vxe-column title="成本" field="inSubtotalTotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="采购退货" align="center">
          <vxe-column title="数量" field="purchaseReturnsQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="purchaseReturnsSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="销售出库" align="center">
          <vxe-column title="数量" field="sellOutQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="sellOutSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="调拨出库" align="center">
          <vxe-column title="数量" field="channelOutQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="channelOutSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="其他出库" align="center">
          <vxe-column title="数量" field="otherOutQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="otherOutSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="盘亏单" align="center">
          <vxe-column title="数量" field="taskDeficitQuantity" align="center" width="100"/>
          <vxe-column title="成本" field="taskDeficitSubtotal" align="center" width="100"/>
        </vxe-colgroup>
        <vxe-colgroup title="出库合计" align="center">
          <vxe-column title="数量" field="outQuantityTotal" align="center" width="100"/>
          <vxe-column title="成本" field="outSubtotalTotal" align="center" width="100"/>
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
        productCategoryIds: [],
        productIds: [],
        warehouseIds: [],
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
      const map = {
        "initialQuantity": 0,
        "initialSubtotal": 0,
        "purchaseStockQuantity": 0,
        "purchaseStockSubtotal": 0,
        "salesReturnsQuantity": 0,
        "salesReturnsSubtotal": 0,
        "channelInQuantity": 0,
        "channelInSubtotal": 0,
        "otherInQuantity": 0,
        "otherInSubtotal": 0,
        "takeProfitQuantity": 0,
        "takeProfitSubtotal": 0,
        "inQuantityTotal": 0,
        "inSubtotalTotal": 0,
        "purchaseReturnsQuantity": 0,
        "purchaseReturnsSubtotal": 0,
        "sellOutQuantity": 0,
        "sellOutSubtotal": 0,
        "channelOutQuantity": 0,
        "channelOutSubtotal": 0,
        "otherOutQuantity": 0,
        "otherOutSubtotal": 0,
        "taskDeficitQuantity": 0,
        "taskDeficitSubtotal": 0,
        "outQuantityTotal": 0,
        "outSubtotalTotal": 0,
        "costSubtotal": 0,
        "currentQuantity": 0,
        "totalCost": 0
      };
      for (let mapKey in map) {
        let value = 0;
        data.forEach((row) => {
          let rd = row[mapKey];
          if (rd) {
            value += Number(this.getAbsoluteValue(rd) || 0);
          }
        });
        map[mapKey] = value;
      }
      return [['合计', '', '', '', '', '',
        map.initialQuantity, map.initialSubtotal.toFixed(2),
        map.purchaseStockQuantity, map.purchaseStockSubtotal.toFixed(2),
        map.salesReturnsQuantity, map.salesReturnsSubtotal.toFixed(2),
        map.channelInQuantity, map.channelInSubtotal.toFixed(2),
        map.otherInQuantity, map.otherInSubtotal.toFixed(2),
        map.takeProfitQuantity, map.takeProfitQuantity.toFixed(2),
        map.inQuantityTotal, map.inSubtotalTotal.toFixed(2),
        map.purchaseReturnsQuantity, map.purchaseReturnsSubtotal.toFixed(2),
        map.sellOutQuantity, map.sellOutSubtotal.toFixed(2),
        map.channelOutQuantity, map.channelOutSubtotal.toFixed(2),
        map.otherOutQuantity, map.otherOutSubtotal.toFixed(2),
        map.taskDeficitQuantity, map.taskDeficitSubtotal.toFixed(2),
        map.outQuantityTotal, map.outSubtotalTotal.toFixed(2),
        '', map.costSubtotal.toFixed(2),
        map.currentQuantity, map.totalCost.toFixed(2)]];
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
      params.isReport = true;
      InventoryItem.summary(params).then(({data: {results, total}}) => {
        const dataList = results || [];
        this.pagination.total = total;
        const operationTypeParams = JSON.parse(JSON.stringify(params));
        operationTypeParams.isReport = false;
        InventoryItem.summaryOperationType(operationTypeParams).then(({data}) => {
          dataList?.forEach((row) => {
            const productId = row.productId;
            const warehouseId = row.warehouseId;
            data?.forEach((data_row) => {
              //todo 待优化，可处理成后台统计
              if (data_row.warehouseId === warehouseId && data_row.productId === productId) {
                const operationType = data_row.operationType;
                let purchaseStockQuantity = row.purchaseStockQuantity;
                let purchaseStockSubtotal = row.purchaseStockSubtotal;
                let salesReturnsQuantity = row.salesReturnsQuantity;
                let salesReturnsSubtotal = row.salesReturnsSubtotal;
                let channelInQuantity = row.channelInQuantity;
                let channelInSubtotal = row.channelInSubtotal;
                let otherInQuantity = row.otherInQuantity;
                let otherInSubtotal = row.otherInSubtotal;
                let takeProfitQuantity = row.takeProfitQuantity;
                let takeProfitSubtotal = row.takeProfitSubtotal;
                let taskDeficitQuantity = row.taskDeficitQuantity;
                let taskDeficitSubtotal = row.taskDeficitSubtotal;
                let purchaseReturnsQuantity = row.purchaseReturnsQuantity;
                let purchaseReturnsSubtotal = row.purchaseReturnsSubtotal;
                let sellOutQuantity = row.sellOutQuantity;
                let sellOutSubtotal = row.sellOutSubtotal;
                let channelOutQuantity = row.channelOutQuantity;
                let channelOutSubtotal = row.channelOutSubtotal;
                let otherOutQuantity = row.otherOutQuantity;
                let otherOutSubtotal = row.otherOutSubtotal;
                let costSubtotal = row.costSubtotal;
                let inQuantityTotal = row.inQuantityTotal;
                let inSubtotalTotal = row.inSubtotalTotal;
                let outQuantityTotal = row.outQuantityTotal;
                let outSubtotalTotal = row.outSubtotalTotal;
                switch (operationType) {
                  case "采购入库":
                  case "销售退货":
                  case "调拨入库":
                  case "其他入库":
                  case "盘盈入库": {
                    switch (operationType) {
                      case "采购入库":
                        row.purchaseStockQuantity = data_row.quantity + (purchaseStockQuantity || 0);
                        row.purchaseStockSubtotal = data_row.subtotal + (purchaseStockSubtotal || 0);
                        break;
                      case "销售退货":
                        row.salesReturnsQuantity = data_row.quantity + (salesReturnsQuantity || 0);
                        row.salesReturnsSubtotal = data_row.subtotal + (salesReturnsSubtotal || 0);
                        break;
                      case "调拨入库":
                        row.channelInQuantity = data_row.quantity + (channelInQuantity || 0);
                        row.channelInSubtotal = data_row.subtotal + (channelInSubtotal || 0);
                        break;
                      case "其他入库":
                        row.otherInQuantity = data_row.quantity + (otherInQuantity || 0);
                        row.otherInSubtotal = data_row.subtotal + (otherInSubtotal || 0);
                        break;
                      case "盘盈入库":
                        row.takeProfitQuantity = data_row.quantity + (takeProfitQuantity || 0);
                        row.takeProfitSubtotal = data_row.subtotal + (takeProfitSubtotal || 0);
                        break;
                      default:
                        break;
                    }
                    row.inQuantityTotal = data_row.quantity + (inQuantityTotal || 0);
                    row.inSubtotalTotal = data_row.subtotal + (inSubtotalTotal || 0);
                    break;
                  }
                  case "采购退货":
                  case "销售出库":
                  case "调拨出库":
                  case "盘亏出库":
                  case "其他出库": {
                    switch (operationType) {
                      case "采购退货":
                        row.purchaseReturnsQuantity = data_row.quantity + (purchaseReturnsQuantity || 0);
                        row.purchaseReturnsSubtotal = data_row.subtotal + (purchaseReturnsSubtotal || 0);
                        break;
                      case "销售出库":
                        row.sellOutQuantity = data_row.quantity + (sellOutQuantity || 0);
                        row.sellOutSubtotal = data_row.subtotal + (sellOutSubtotal || 0);
                        break;
                      case "调拨出库":
                        row.channelOutQuantity = this.getAbsoluteValue(data_row.quantity) + (channelOutQuantity || 0);
                        row.channelOutSubtotal = this.getAbsoluteValue(data_row.subtotal) + (channelOutSubtotal || 0);
                        break;
                      case "盘亏出库":
                        row.taskDeficitQuantity = this.getAbsoluteValue(data_row.quantity) + (taskDeficitQuantity || 0);
                        row.taskDeficitSubtotal = this.getAbsoluteValue(data_row.subtotal) + (taskDeficitSubtotal || 0);
                        break;
                      case "其他出库":
                        row.otherOutQuantity = this.getAbsoluteValue(data_row.quantity) + (otherOutQuantity || 0);
                        row.otherOutSubtotal = this.getAbsoluteValue(data_row.subtotal) + (otherOutSubtotal || 0);
                        break;
                    }
                    row.outQuantityTotal = this.getAbsoluteValue(data_row.quantity) + (outQuantityTotal || 0);
                    row.outSubtotalTotal = this.getAbsoluteValue(data_row.subtotal) + (outSubtotalTotal || 0);
                    break;
                  }
                  case "成本调整":
                    row.costSubtotal = data_row.subtotal + (costSubtotal || 0);
                    break;
                  default:
                    break;
                }
              }
            });
          });
          InventoryItem.summaryInitial(params).then(({data: initialData}) => {
            dataList.forEach((row) => {
              if (initialData) {
                initialData.forEach((data_row) => {
                  const productId = row.productId;
                  const warehouseId = row.warehouseId;
                  if (data_row.productId === productId && data_row.warehouseId === warehouseId) {
                    row.initialQuantity = data_row.summaryQuantity;
                    row.initialSubtotal = data_row.summaryCost;
                  }
                })
              } else {
                row.initialQuantity = undefined;
                row.initialSubtotal = undefined;
              }
            });
            this.dataList = dataList;
          });
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
    getAbsoluteValue(number) {
      if (number < 0) {
        return -number;
      } else {
        return number;
      }
    },
    excel() {
      this.loading = true;
      const params = JSON.parse(JSON.stringify(this.queryParams));
      params.page = 1;
      params.pageSize = 1199999;
      params.productCategoryIds = params.productCategoryIds.join(",");
      params.productIds = params.productIds.join(",");
      params.isReport = true;
      params.warehouseIds = params.warehouseIds.join(",");
      InventoryItem.summary(params).then(({data: {results, total}}) => {
        const dataList = results || [];
        const operationTypeParams = JSON.parse(JSON.stringify(params));
        operationTypeParams.isReport = false;
        InventoryItem.summaryOperationType(operationTypeParams).then(({data}) => {
          dataList?.forEach((row) => {
            const productId = row.productId;
            const warehouseId = row.warehouseId;
            data?.forEach((data_row) => {
              if (data_row.warehouseId === warehouseId && data_row.productId === productId) {
                const operationType = data_row.operationType;
                let purchaseStockQuantity = row.purchaseStockQuantity;
                let purchaseStockSubtotal = row.purchaseStockSubtotal;
                let initialQuantity = row.initialQuantity;
                let initialSubtotal = row.initialSubtotal;
                let salesReturnsQuantity = row.salesReturnsQuantity;
                let salesReturnsSubtotal = row.salesReturnsSubtotal;
                let channelInQuantity = row.channelInQuantity;
                let channelInSubtotal = row.channelInSubtotal;
                let otherInQuantity = row.otherInQuantity;
                let otherInSubtotal = row.otherInSubtotal;
                let takeProfitQuantity = row.takeProfitQuantity;
                let takeProfitSubtotal = row.takeProfitSubtotal;
                let taskDeficitQuantity = row.taskDeficitQuantity;
                let taskDeficitSubtotal = row.taskDeficitSubtotal;
                let purchaseReturnsQuantity = row.purchaseReturnsQuantity;
                let purchaseReturnsSubtotal = row.purchaseReturnsSubtotal;
                let sellOutQuantity = row.sellOutQuantity;
                let sellOutSubtotal = row.sellOutSubtotal;
                let channelOutQuantity = row.channelOutQuantity;
                let channelOutSubtotal = row.channelOutSubtotal;
                let otherOutQuantity = row.otherOutQuantity;
                let otherOutSubtotal = row.otherOutSubtotal;
                let costSubtotal = row.costSubtotal;
                let inQuantityTotal = row.inQuantityTotal;
                let inSubtotalTotal = row.inSubtotalTotal;
                let outQuantityTotal = row.outQuantityTotal;
                let outSubtotalTotal = row.outSubtotalTotal;
                switch (operationType) {
                  case "期初余额":
                  case "期初库存":
                    row.initialQuantity = data_row.quantity + (initialQuantity || 0);
                    row.initialSubtotal = data_row.subtotal + (initialSubtotal || 0);
                    break;
                  case "采购入库":
                  case "销售退货":
                  case "调拨入库":
                  case "其他入库":
                  case "盘盈入库": {
                    switch (operationType) {
                      case "采购入库":
                        row.purchaseStockQuantity = data_row.quantity + (purchaseStockQuantity || 0);
                        row.purchaseStockSubtotal = data_row.subtotal + (purchaseStockSubtotal || 0);
                        break;
                      case "销售退货":
                        row.salesReturnsQuantity = data_row.quantity + (salesReturnsQuantity || 0);
                        row.salesReturnsSubtotal = data_row.subtotal + (salesReturnsSubtotal || 0);
                        break;
                      case "调拨入库":
                        row.channelInQuantity = data_row.quantity + (channelInQuantity || 0);
                        row.channelInSubtotal = data_row.subtotal + (channelInSubtotal || 0);
                        break;
                      case "其他入库":
                        row.otherInQuantity = data_row.quantity + (otherInQuantity || 0);
                        row.otherInSubtotal = data_row.subtotal + (otherInSubtotal || 0);
                        break;
                      case "盘盈入库":
                        row.takeProfitQuantity = data_row.quantity + (takeProfitQuantity || 0);
                        row.takeProfitSubtotal = data_row.subtotal + (takeProfitSubtotal || 0);
                        break;
                      default:
                        break;
                    }
                    row.inQuantityTotal = data_row.quantity + (inQuantityTotal || 0);
                    row.inSubtotalTotal = data_row.subtotal + (inSubtotalTotal || 0);
                    break;
                  }
                  case "采购退货":
                  case "销售出库":
                  case "调拨出库":
                  case "盘亏出库":
                  case "其他出库": {
                    switch (operationType) {
                      case "采购退货":
                        row.purchaseReturnsQuantity = data_row.quantity + (purchaseReturnsQuantity || 0);
                        row.purchaseReturnsSubtotal = data_row.subtotal + (purchaseReturnsSubtotal || 0);
                        break;
                      case "销售出库":
                        row.sellOutQuantity = data_row.quantity + (sellOutQuantity || 0);
                        row.sellOutSubtotal = data_row.subtotal + (sellOutSubtotal || 0);
                        break;
                      case "调拨出库":
                        row.channelOutQuantity = this.getAbsoluteValue(data_row.quantity) + (channelOutQuantity || 0);
                        row.channelOutSubtotal = this.getAbsoluteValue(data_row.subtotal) + (channelOutSubtotal || 0);
                        break;
                      case "盘亏出库":
                        row.taskDeficitQuantity = this.getAbsoluteValue(data_row.quantity) + (taskDeficitQuantity || 0);
                        row.taskDeficitSubtotal = this.getAbsoluteValue(data_row.subtotal) + (taskDeficitSubtotal || 0);
                        break;
                      case "其他出库":
                        row.otherOutQuantity = this.getAbsoluteValue(data_row.quantity) + (otherOutQuantity || 0);
                        row.otherOutSubtotal = this.getAbsoluteValue(data_row.subtotal) + (otherOutSubtotal || 0);
                        break;
                    }
                    row.outQuantityTotal = this.getAbsoluteValue(data_row.quantity) + (outQuantityTotal || 0);
                    row.outSubtotalTotal = this.getAbsoluteValue(data_row.subtotal) + (outSubtotalTotal || 0);
                    break;
                  }
                  case "成本调整":
                    row.costSubtotal = data_row.subtotal + (costSubtotal || 0);
                    break;
                  default:
                    break;
                }
              }
            });
          });
          InventoryItem.summaryInitial(params).then(({data: initialData}) => {
            dataList.forEach((row) => {
              if (initialData) {
                initialData.forEach((data_row) => {
                  const productId = row.productId;
                  const warehouseId = row.warehouseId;
                  if (data_row.productId === productId && data_row.warehouseId === warehouseId) {
                    row.initialQuantity = data_row.summaryQuantity;
                    row.initialSubtotal = data_row.summaryCost;
                  }
                })
              } else {
                row.initialQuantity = undefined;
                row.initialSubtotal = undefined;
              }
            });
            this.callExcel(dataList);
          });
        }).finally(() => this.loading = false);
      });
    },
    callExcel(dataList) {
      if (dataList.length < 0) {
        message.warn("暂无数据～");
        return;
      }
      let headList = [
        {label: "产品编号", key: "productCode"},
        {label: "产品名称", key: "productName"},
        {label: "产品类别", key: "productCategoryName"},
        {label: "规格型号", key: "productSpecification"},
        {label: "单位", key: "unitName"},
        {label: "仓库", key: "warehouseName"},
        {label: "数量", key: "initialQuantity"},
        {label: "成本", key: "initialSubtotal"},
        {label: "数量", key: "purchaseStockQuantity"},
        {label: "成本", key: "purchaseStockSubtotal"},
        {label: "数量", key: "salesReturnsQuantity"},
        {label: "成本", key: "salesReturnsSubtotal"},
        {label: "数量", key: "channelInQuantity"},
        {label: "成本", key: "channelInSubtotal"},
        {label: "数量", key: "otherInQuantity"},
        {label: "成本", key: "otherInSubtotal"},
        {label: "数量", key: "takeProfitQuantity"},
        {label: "成本", key: "takeProfitSubtotal"},
        {label: "数量", key: "inQuantityTotal"},
        {label: "成本", key: "inSubtotalTotal"},
        {label: "数量", key: "purchaseReturnsQuantity"},
        {label: "成本", key: "purchaseReturnsSubtotal"},
        {label: "数量", key: "sellOutQuantity"},
        {label: "成本", key: "sellOutSubtotal"},
        {label: "数量", key: "channelOutQuantity"},
        {label: "成本", key: "channelOutSubtotal"},
        {label: "数量", key: "otherOutQuantity"},
        {label: "成本", key: "otherOutSubtotal"},
        {label: "数量", key: "taskDeficitQuantity"},
        {label: "成本", key: "taskDeficitSubtotal"},
        {label: "数量", key: "outQuantityTotal"},
        {label: "成本", key: "outSubtotalTotal"},
        {label: "数量", key: "costQuantity"},
        {label: "成本", key: "costSubtotal"},
        {label: "数量", key: "currentQuantity"},
        {label: "成本", key: "totalCost"},
      ];
      const tHeader = ['产品编号', '产品名称', '产品类别', '规格型号', '单位', '仓库',
        '期初', null, '采购入库', null,
        '销售退货', null, '调拨入库', null,
        '其他入库', null, '盘盈单', null,
        '入库合计', null, '采购退货', null,
        '销售出库', null, '调拨出库', null,
        '其他出库', null, '盘亏单', null,
        '出库合计', null,
        '成本调整', null, '结存', null];
      const secondHeader = [null, null, null, null, null, null,
        '数量', '成本', '数量', '成本',
        '数量', '成本', '数量', '成本',
        '数量', '成本', '数量', '成本',
        '数量', '成本', '数量', '成本',
        '数量', '成本', '数量', '成本',
        '数量', '成本', '数量', '成本',
        '数量', '成本',
        '数量', '成本', '数量', '成本'];
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
        {s: {r: 0, c: 14}, e: {r: 0, c: 15}},
        {s: {r: 0, c: 16}, e: {r: 0, c: 17}},
        {s: {r: 0, c: 18}, e: {r: 0, c: 19}},
        {s: {r: 0, c: 20}, e: {r: 0, c: 21}},
        {s: {r: 0, c: 22}, e: {r: 0, c: 23}},
        {s: {r: 0, c: 24}, e: {r: 0, c: 25}},
        {s: {r: 0, c: 26}, e: {r: 0, c: 27}},
        {s: {r: 0, c: 28}, e: {r: 0, c: 29}},
        {s: {r: 0, c: 30}, e: {r: 0, c: 31}},
        {s: {r: 0, c: 32}, e: {r: 0, c: 33}},
        {s: {r: 0, c: 34}, e: {r: 0, c: 35}},
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
