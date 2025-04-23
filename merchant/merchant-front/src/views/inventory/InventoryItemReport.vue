<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="excel" color="primary">导出</Button>
      </template>
      <template #tools>
        <div class="h-input-group h-table-checkbox-wrap">
          <span class="h-input-addon ml-8px">仓库：</span>
          <Select :multiple="true" v-model="params.warehouseIds" class="w-120px" keyName="id" titleName="name"
                  :datas="warehouseList"/>
        </div>
        <div class="h-input-group h-table-checkbox-wrap">
          <span class="h-input-addon ml-8px">商品：</span>
          <Select :multiple="true" v-model="params.productIds" class="w-120px" keyName="id" titleName="name"
                  :datas="productList"/>
        </div>
        <div class="h-input-group h-table-checkbox-wrap">
          <span class="h-input-addon ml-8px">往来单位：</span>
          <Select :multiple="true" v-model="params.supplierIds" class="w-120px" keyName="id" titleName="name"
                  :datas="supplierList"/>
        </div>
        <div class="h-input-group h-table-checkbox-wrap">
          <span class="h-input-addon ml-8px">业务类型：</span>
          <Select autosize :filterable="true" :multiple="true" v-model="params.operationTypes" class="w-120px"
                  :datas="operationTypeList"/>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">单据日期：</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-360px ml-8px"
                placeholder="请输入商品名称/单据编号" @search="doSearch">
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
        <vxe-column title="单据日期" field="createdAt" width="120">
          <template #default="{ row }">
            <div v-if="row.operationType !== '期初余额'">
              {{ row.createdAt }}
            </div>
            <div v-else>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="业务类型" field="operationType" width="120"/>
        <vxe-column title="单据编号" field="batchNumber" width="200"/>
        <vxe-column title="往来单位" field="supplierName" width="120">
          <template #default="{ row }">
            <div v-if="row.supplierName && row.supplierName !== ''">
              {{ row.supplierName }}
            </div>
            <div v-else-if="row.customerName && row.customerName !== ''">
              {{ row.customerName }}
            </div>
            <div v-else>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="仓库" field="warehouseName" align="center" width="100"/>
        <vxe-column title="单位" field="unitName" width="80"/>
        <vxe-column title="商品名称备注" field="productRemarks" width="80"/>
        <vxe-column title="入库数量" field="quantity" width="80">
          <template #default="{ row }">
            <div v-if="inboundItems.includes(row['operationType'])">
              {{ row.quantity }}
            </div>
            <div v-else>
            </div>
          </template>
        </vxe-column>
        <vxe-colgroup title="入库" align="center">
          <vxe-column title="基本单位数量" field="quantity" align="center" width="100">
            <template #default="{ row }">
              <div v-if="inboundItems.includes(row['operationType'])">
                {{ row.quantity }}
              </div>
              <div v-else>
              </div>
            </template>
          </vxe-column>
          <vxe-column title="单位成本" field="unitPrice" align="center" width="100">
            <template #default="{ row }">
              <div v-if="inboundItems.includes(row['operationType'])">
                {{ row.unitPrice }}
              </div>
              <div v-else>
              </div>
            </template>
          </vxe-column>
          <vxe-column title="成本" field="subtotal" align="center" width="100">
            <template #default="{ row }">
              <div
                  v-if="inboundItems.includes(row['operationType']) || ['成本调整'].includes(row['operationType'])">
                {{ row.subtotal }}
              </div>
              <div v-else>
              </div>
            </template>
          </vxe-column>
        </vxe-colgroup>
        <vxe-column title="出库数量" field="quantity" width="80">
          <template #default="{ row }">
            <div v-if="outboundItems.includes(row['operationType'])">
              {{ getAbsoluteValue(row.quantity) }}
            </div>
            <div v-else>
            </div>
          </template>
        </vxe-column>
        <vxe-colgroup title="出库" align="center">
          <vxe-column title="基本单位数量" field="quantity" align="center" width="100">
            <template #default="{ row }">
              <div v-if="outboundItems.includes(row['operationType'])">
                {{ getAbsoluteValue(row.quantity) }}
              </div>
              <div v-else>
              </div>
            </template>
          </vxe-column>
          <vxe-column title="单位成本" field="unitPrice" align="center" width="100">
            <template #default="{ row }">
              <div v-if="outboundItems.includes(row['operationType'])">
                {{ row.unitPrice }}
              </div>
              <div v-else>
              </div>
            </template>
          </vxe-column>
          <vxe-column title="成本" field="subtotal" align="center" width="100">
            <template #default="{ row }">
              <div v-if="outboundItems.includes(row['operationType'])">
                {{ row.subtotal }}
              </div>
              <div v-else>
              </div>
            </template>
          </vxe-column>
        </vxe-colgroup>
        <vxe-colgroup title="结存" align="center">
          <vxe-column title="基本单位数量" field="currentQuantity" align="center" width="100"/>
          <vxe-column title="单位成本" field="averageCost" align="center" width="100"/>
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
import Supplier from "@js/api/basic/Supplier";
import {exportExcel, exportExcelHeader} from "@js/excel";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "InventoryItemList",
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
        productIds: [],
        supplierIds: [],
        warehouseIds: [],
        operationTypes: [],
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
      supplierList: [],
      outboundItems: ["采购退货", "销售出库", "调拨出库", "盘亏出库", "其他出库"],
      inboundItems: ["采购入库", "销售退货", "调拨入库", "其他入库", "盘盈入库"],
      operationTypeList: {
        "采购入库": "采购入库",
        "销售退货": "销售退货",
        "调拨入库": "调拨入库",
        "其他入库": "其他入库",
        "盘盈入库": "盘盈入库",
        "采购退货": "采购退货",
        "销售出库": "销售出库",
        "调拨出库": "调拨出库",
        "盘亏出库": "盘亏出库",
        "其他出库": "其他出库",
        "成本调整": "成本调整",
      }
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
      data.forEach((row) => {
        let rd = row['quantity'];
        if (rd) {
          if (this.inboundItems.includes(row['operationType'])) {
            inQuantity += Number(rd || 0);
          }
          if (this.outboundItems.includes(row['operationType'])) {
            outQuantity += Number(this.getAbsoluteValue(rd) || 0);
          }
        }
      });
      data.forEach((row) => {
        let rd = row['subtotal'];
        if (rd) {
          if (this.inboundItems.includes(row['operationType'])) {
            inTotal += Number(rd || 0);
          }
          if (this.outboundItems.includes(row['operationType'])) {
            outTotal += Number(this.getAbsoluteValue(rd) || 0);
          }
        }
      });
      data.forEach((row) => {
        let rd = row['currentQuantity'];
        if (rd) {
          currentQuantity += Number(rd || 0);
        }
      });
      data.forEach((row) => {
        let rd = row['totalCost'];
        if (rd) {
          totalCost += Number(rd || 0);
        }
      });
      return [['合计', '', '', '', '', '', '', '', '', '', '', inQuantity, inQuantity, '', inTotal.toFixed(2), outQuantity, outQuantity, '', outTotal.toFixed(2), currentQuantity, '', totalCost.toFixed(2)]];
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadDict(callback) {
      loading("加载中....");
      Promise.all([Product.select(), Warehouse.select(), Supplier.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            this.warehouseList = results[1].data || [];
            this.supplierList = results[2].data || [];
            callback();
          })
          .finally(() => loading.close());
    },
    loadList(type = true) {
      this.loading = true;
      const params = JSON.parse(JSON.stringify(this.queryParams));
      params.warehouseIds = params.warehouseIds.join(",");
      params.productIds = params.productIds.join(",");
      params.supplierIds = params.supplierIds.join(",");
      params.operationTypes = params.operationTypes.join(",");
      params.isReport = true;
      InventoryItem.report(params).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
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
      params.warehouseIds = params.warehouseIds.join(",");
      params.productIds = params.productIds.join(",");
      params.supplierIds = params.supplierIds.join(",");
      params.operationTypes = params.operationTypes.join(",");
      params.page = 1;
      params.pageSize = 99999;
      params.isReport = true;
      InventoryItem.report(params).then(({data: {results, total}}) => {
        let dataList = results || [];
        let headList = [
          {label: "商品编号", key: "productCode"},
          {label: "商品名称", key: "productName"},
          {label: "商品类别", key: "productCategoryName"},
          {label: "规格型号", key: "productSpecification"},
          {label: "单据日期", key: "createdAt"},
          {label: "业务类型", key: "operationType"},
          {label: "单据编号", key: "batchNumber"},
          {label: "往来单位", key: "correspondent"},
          {label: "仓库", key: "warehouseName"},
          {label: "单位", key: "unitName"},
          {label: "商品名称备注", key: "productRemarks"},
          {label: "入库数量", key: "inQuantity"},
          {label: "基本单位数量", key: "inQuantity"},
          {label: "单位成本", key: "inUnitPrice"},
          {label: "成本", key: "inSubtotal"},
          {label: "出库数量", key: "outQuantity"},
          {label: "基本单位数量", key: "outQuantity"},
          {label: "单位成本", key: "outUnitPrice"},
          {label: "成本", key: "outSubtotal"},
          {label: "基本单位数量", key: "currentQuantity"},
          {label: "单位成本", key: "averageCost"},
          {label: "成本", key: "totalCost"},
        ];
        const tHeader = ['商品编号', '商品名称', '商品类别', '规格型号', '单据日期', '业务类型', '单据编号', '往来单位', '仓库', '单位', '商品名称备注', '入库数量', '入库',
          null, null, '出库数量', '出库', null, null, '结存', null, null
        ];
        const merges = [
          {s: {r: 0, c: 0}, e: {r: 1, c: 0}},
          {s: {r: 0, c: 1}, e: {r: 1, c: 1}},
          {s: {r: 0, c: 2}, e: {r: 1, c: 2}},
          {s: {r: 0, c: 3}, e: {r: 1, c: 3}},
          {s: {r: 0, c: 4}, e: {r: 1, c: 4}},
          {s: {r: 0, c: 5}, e: {r: 1, c: 5}},
          {s: {r: 0, c: 6}, e: {r: 1, c: 6}},
          {s: {r: 0, c: 7}, e: {r: 1, c: 7}},
          {s: {r: 0, c: 8}, e: {r: 1, c: 8}},
          {s: {r: 0, c: 9}, e: {r: 1, c: 9}},
          {s: {r: 0, c: 10}, e: {r: 1, c: 10}},
          {s: {r: 0, c: 11}, e: {r: 1, c: 11}},
          {s: {r: 0, c: 12}, e: {r: 0, c: 14}},
          {s: {r: 0, c: 15}, e: {r: 1, c: 15}},
          {s: {r: 0, c: 16}, e: {r: 0, c: 18}},
          {s: {r: 0, c: 19}, e: {r: 0, c: 21}},
        ];
        const list = [[null, null, null, null, null, null, null, null, null, null, null, null, '基本单位数量', '单位成本', '成本', null, '基本单位数量',
          '单位成本', '成本', '基本单位数量', '单位成本', '成本']];
        // 处理传递数据
        dataList = this.handleDataList(dataList);
        exportExcelHeader(dataList, tHeader, headList, merges, list, manba(new Date()).format("YYYYMMddHHmmss") + "_进销存明细");
      }).finally(() => this.loading = false);
    },
    handleDataList(dataList) {
      let list = [];
      dataList.forEach((item) => {
        const element = item;
        const operationType = item.operationType;
        const quantity = item.quantity;
        switch (operationType) {
          case "期初余额":
            element.createdAt = '';
            break;
          case "采购入库":
          case "销售退货":
          case "调拨入库":
          case "其他入库":
          case "盘盈入库":
            element.correspondent = item.supplierName;
            element.inQuantity = quantity;
            element.inUnitPrice = item.unitPrice;
            element.inSubtotal = item.subtotal;
            break;
          case "采购退货":
          case "销售出库":
          case "调拨出库":
          case "盘亏出库":
          case "其他出库":
            element.correspondent = item.customerName;
            element.outQuantity = this.getAbsoluteValue(quantity);
            element.outUnitPrice = item.unitPrice;
            element.outSubtotal = item.subtotal;
            break;
          case "成本调整":
            element.inSubtotal = item.subtotal;
            break;
          default:
            break;
        }
        list.push(element);
      });
      return list;
    }
  },
  created() {
    this.loadDict(() => {
    });
    this.loadList();
  }
}
</script>
