<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="excel">导 出</t-button>
        <t-select
            v-model="params.warehouseIds"
            :options="warehouseList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="仓库"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.productIds"
            :options="productList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="产品"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.productCategoryIds"
            :options="productCategoryList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="产品类别"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.supplierIds"
            :options="supplierList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="供应商"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.customerIds"
            :options="customerList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="客户"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.operationTypes"
            :options="operationTypeOptions"
            filterable
            clearable
            multiple
            placeholder="业务类型"
            style="width: 160px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="单据日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入产品名称/单据编号"
            style="width: 260px; background: #fff; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>
    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
      >
        <template #inventoryDate="{ row }">
          <span v-if="row.operationType !== '期初余额'">{{ row.inventoryDate }}</span>
        </template>
        <template #correspondent="{ row }">
          <span v-if="row.supplierName && row.supplierName !== ''">{{ row.supplierName }}</span>
          <span v-else-if="row.customerName && row.customerName !== ''">{{ row.customerName }}</span>
        </template>
        <template #inQuantityCol="{ row }">
          <span v-if="inboundItems.includes(row.operationType)">{{ row.quantity }}</span>
        </template>
        <template #inBaseQty="{ row }">
          <span v-if="inboundItems.includes(row.operationType)">{{ row.quantity }}</span>
        </template>
        <template #inUnitPrice="{ row }">
          <span v-if="inboundItems.includes(row.operationType)">{{ row.unitPrice }}</span>
        </template>
        <template #inSubtotal="{ row }">
          <span v-if="inboundItems.includes(row.operationType) || ['成本调整'].includes(row.operationType)">{{ row.subtotal }}</span>
        </template>
        <template #outQuantityCol="{ row }">
          <span v-if="outboundItems.includes(row.operationType)">{{ getAbsoluteValue(row.quantity) }}</span>
        </template>
        <template #outBaseQty="{ row }">
          <span v-if="outboundItems.includes(row.operationType)">{{ getAbsoluteValue(row.quantity) }}</span>
        </template>
        <template #outUnitPrice="{ row }">
          <span v-if="outboundItems.includes(row.operationType)">{{ row.unitPrice }}</span>
        </template>
        <template #outSubtotal="{ row }">
          <span v-if="outboundItems.includes(row.operationType)">{{ row.subtotal }}</span>
        </template>
      </t-table>
    </div>
    <div class="simple-page__pager">
      <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-size-options="pagination.pageSizeOptions"
          :show-jumper="true"
          :show-page-size="true"
          :popup-props="{ attach: 'body' }"
          @change="onPageChange"
      />
    </div>
  </div>
</template>
<script>
import manba from "manba";
import InventoryItem from "@js/api/inventory/InventoryItem";
import {mapMutations} from "vuex";
import {LoadingPlugin} from "tdesign-vue-next";
import Product from "@js/api/basic/Product";
import ProductCategory from "@js/api/basic/ProductCategory";
import Warehouse from "@js/api/basic/Warehouse";
import Supplier from "@js/api/basic/Supplier";
import Customer from "@js/api/basic/Customer";
import {exportExcelHeader} from "@js/excel";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "InventoryItemReport",
  data() {
    return {
      dataList: [],
      loading: false,
      amountTotal: 0,
      totalParams: {},
      pagination: {
        page: 1,
        pageSize: 1000,
        pageSizeOptions: [
          {label: "300条/页", value: 300},
          {label: "500条/页", value: 500},
          {label: "1000条/页", value: 1000},
          {label: "2000条/页", value: 2000},
        ],
        total: 0
      },
      params: {
        filter: null,
        productIds: [],
        supplierIds: [],
        customerIds: [],
        warehouseIds: [],
        operationTypes: [],
        productCategoryIds: [],
        state: null,
        sortCol: null,
        sort: null,
      },
      dateRangeValue: [startTime, endTime],
      warehouseList: [],
      productList: [],
      productCategoryList: [],
      supplierList: [],
      customerList: [],
      outboundItems: ["采购退货", "销售出库", "调拨出库", "盘亏出库", "其他出库"],
      inboundItems: ["采购入库", "销售退货", "调拨入库", "其他入库", "盘盈入库"],
      operationTypeOptions: [
        {label: "采购入库", value: "采购入库"},
        {label: "销售退货", value: "销售退货"},
        {label: "调拨入库", value: "调拨入库"},
        {label: "其他入库", value: "其他入库"},
        {label: "盘盈入库", value: "盘盈入库"},
        {label: "采购退货", value: "采购退货"},
        {label: "销售出库", value: "销售出库"},
        {label: "调拨出库", value: "调拨出库"},
        {label: "盘亏出库", value: "盘亏出库"},
        {label: "其他出库", value: "其他出库"},
        {label: "成本调整", value: "成本调整"},
      ],
      summaryQuantity: 0,
      summaryCost: 0,
      columns: [
        {colKey: 'productCode', title: '产品编码', width: 130, align: 'center', ellipsis: true},
        {colKey: 'productName', title: '产品名称', width: 200, ellipsis: true},
        {colKey: 'productCategoryName', title: '产品类别', width: 200, ellipsis: true},
        {colKey: 'productSpecification', title: '规格型号', minWidth: 120, ellipsis: true},
        {colKey: 'inventoryDate', title: '单据日期', width: 120, ellipsis: true},
        {colKey: 'operationType', title: '业务类型', width: 120, ellipsis: true},
        {colKey: 'batchNumber', title: '单据编号', width: 200, ellipsis: true},
        {colKey: 'correspondent', title: '往来单位', width: 120, ellipsis: true},
        {colKey: 'warehouseName', title: '仓库', width: 100, align: 'center', ellipsis: true},
        {colKey: 'unitName', title: '单位', width: 80, ellipsis: true},
        {colKey: 'productRemarks', title: '产品名称备注', width: 80, ellipsis: true},
        {colKey: 'inQuantityCol', title: '入库数量', width: 80, align: 'center'},
        {
          colKey: 'inbound',
          title: '入库',
          align: 'center',
          children: [
            {colKey: 'inBaseQty', title: '基本单位数量', width: 100, align: 'center'},
            {colKey: 'inUnitPrice', title: '单位成本', width: 100, align: 'center'},
            {colKey: 'inSubtotal', title: '成本', width: 100, align: 'center'},
          ],
        },
        {colKey: 'outQuantityCol', title: '出库数量', width: 80, align: 'center'},
        {
          colKey: 'outbound',
          title: '出库',
          align: 'center',
          children: [
            {colKey: 'outBaseQty', title: '基本单位数量', width: 100, align: 'center'},
            {colKey: 'outUnitPrice', title: '单位成本', width: 100, align: 'center'},
            {colKey: 'outSubtotal', title: '成本', width: 100, align: 'center'},
          ],
        },
        {
          colKey: 'balance',
          title: '结存',
          align: 'center',
          children: [
            {colKey: 'summaryQuantity', title: '基本单位数量', width: 100, align: 'center'},
            {colKey: 'summaryAverage', title: '单位成本', width: 100, align: 'center'},
            {colKey: 'summaryCost', title: '成本', width: 100, align: 'center'},
          ],
        },
      ],
    }
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: start || null,
        end: end || null,
      })
    },
    footData() {
      let inQuantity = 0;
      let outQuantity = 0;
      let inTotal = 0;
      let outTotal = 0;
      (this.dataList || []).forEach((row) => {
        const rd = row.quantity;
        if (rd) {
          if (this.inboundItems.includes(row.operationType)) {
            inQuantity += Number(rd || 0);
          }
          if (this.outboundItems.includes(row.operationType)) {
            outQuantity += Number(this.getAbsoluteValue(rd) || 0);
          }
        }
        const sub = row.subtotal;
        if (sub) {
          if (this.inboundItems.includes(row.operationType)) {
            inTotal += Number(sub || 0);
          }
          if (this.outboundItems.includes(row.operationType)) {
            outTotal += Number(this.getAbsoluteValue(sub) || 0);
          }
        }
      });
      return [{
        productCode: '合计',
        inQuantityCol: inQuantity,
        inBaseQty: inQuantity,
        inSubtotal: inTotal.toFixed(2),
        outQuantityCol: outQuantity,
        outBaseQty: outQuantity,
        outSubtotal: outTotal.toFixed(2),
        summaryQuantity: this.summaryQuantity,
        summaryCost: Number(this.summaryCost || 0).toFixed(2),
      }];
    },
  },
  methods: {
    ...mapMutations(['pushTab']),
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadDict(callback) {
      LoadingPlugin(true);
      Promise.all([Product.select(), Warehouse.select(), Supplier.select(), ProductCategory.select(), Customer.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            this.warehouseList = results[1].data || [];
            this.supplierList = results[2].data || [];
            this.productCategoryList = results[3].data || [];
            this.customerList = results[4].data || [];
            callback();
          })
          .finally(() => LoadingPlugin(false));
    },
    loadList(type = true) {
      this.loading = true;
      const params = this.buildQueryParams();
      Promise.all([InventoryItem.itemTotal(params)]).then((promiseResults) => {
        const data = promiseResults[0].data;
        if (data) {
          this.summaryQuantity = data.summaryQuantity || 0;
          this.summaryCost = data.summaryCost || 0;
        }
        InventoryItem.item(params).then(({data: {results, total}}) => {
          this.dataList = results || [];
          this.pagination.total = total;
        }).finally(() => this.loading = false);
      });
    },
    buildQueryParams(extra = {}) {
      const params = Object.assign(JSON.parse(JSON.stringify(this.queryParams)), extra);
      const joinIds = (arr) => (Array.isArray(arr) && arr.length ? arr.join(",") : undefined);
      params.warehouseIds = joinIds(params.warehouseIds);
      params.productIds = joinIds(params.productIds);
      params.supplierIds = joinIds(params.supplierIds);
      params.customerIds = joinIds(params.customerIds);
      params.operationTypes = joinIds(params.operationTypes);
      params.productCategoryIds = joinIds(params.productCategoryIds);
      Object.keys(params).forEach((key) => {
        if (params[key] === undefined || params[key] === null || params[key] === "") {
          delete params[key];
        }
      });
      return params;
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
      const params = this.buildQueryParams({page: 1, pageSize: 99999});
      InventoryItem.item(params).then(({data: {results, total}}) => {
        let dataList = results || [];
        let headList = [
          {label: "产品编码", key: "productCode"},
          {label: "产品名称", key: "productName"},
          {label: "产品类别", key: "productCategoryName"},
          {label: "规格型号", key: "productSpecification"},
          {label: "单据日期", key: "createdAt"},
          {label: "业务类型", key: "operationType"},
          {label: "单据编号", key: "batchNumber"},
          {label: "往来单位", key: "correspondent"},
          {label: "仓库", key: "warehouseName"},
          {label: "单位", key: "unitName"},
          {label: "产品名称备注", key: "productRemarks"},
          {label: "入库数量", key: "inQuantity"},
          {label: "基本单位数量", key: "inQuantity"},
          {label: "单位成本", key: "inUnitPrice"},
          {label: "成本", key: "inSubtotal"},
          {label: "出库数量", key: "outQuantity"},
          {label: "基本单位数量", key: "outQuantity"},
          {label: "单位成本", key: "outUnitPrice"},
          {label: "成本", key: "outSubtotal"},
          {label: "基本单位数量", key: "summaryQuantity"},
          {label: "单位成本", key: "summaryAverage"},
          {label: "成本", key: "summaryCost"},
        ];
        const tHeader = ['产品编码', '产品名称', '产品类别', '规格型号', '单据日期', '业务类型', '单据编号', '往来单位', '仓库', '单位', '产品名称备注', '入库数量', '入库',
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

