<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="exportData">导 出</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="printEvent">打 印</t-button>
        <t-select
            v-model="params.salesGroup"
            :options="salesGroupOptions"
            placeholder="汇总条件"
            style="width: 200px; border-radius: 4px"
            @change="handleSalesGroupChange"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-select
            v-if="showCustomerFilter"
            v-model="params.customerIds"
            :options="customerList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择客户"
            style="width: 180px; border-radius: 4px"
        />
        <t-select
            v-model="params.warehouseIds"
            :options="warehouseList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择仓库"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.productIds"
            :options="productList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择产品"
            style="width: 180px; border-radius: 4px"
        />
        <t-select
            v-model="params.productCategoryIds"
            :options="productCategoryList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="产品类别"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-if="showCustomerFilter"
            v-model="params.customerCategoryIds"
            :options="customerCategoryList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="客户类别"
            style="width: 160px; border-radius: 4px"
        />
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          resizable
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
      >
        <template #customerCategoryId="{ row }">
          {{ customerCategoryList.find(item => item.id === row.customerCategoryId)?.name || '-' }}
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
import SalesReport from "@js/api/sales/SalesReport";
import Customer from "@js/api/basic/Customer";
import {MessagePlugin} from "tdesign-vue-next";
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";
import * as XLSX from "xlsx";
import ProductCategory from "@js/api/basic/ProductCategory";
import CustomerCategory from "@js/api/basic/CustomerCategory";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

export default {
  name: "SalesSummaryReport",
  data() {
    return {
      dataList: [],
      loading: false,
      quantityTotal: '0.00',
      subtotalTotal: '0.00',
      pagination: {
        page: 1,
        pageSize: 20,
        pageSizeOptions: [
          {label: "50条/页", value: 50},
          {label: "100条/页", value: 100},
          {label: "200条/页", value: 200},
          {label: "500条/页", value: 500},
        ],
        total: 0
      },
      params: {
        salesGroup: 'PRODUCT',
        salesGroupSearch: 'PRODUCT',
        customerIds: [],
        warehouseIds: [],
        productIds: [],
        productCategoryIds: [],
        customerCategoryIds: [],
      },
      dateRangeValue: [startTime, endTime],
      customerList: [],
      warehouseList: [],
      productList: [],
      productCategoryList: [],
      customerCategoryList: [],
      salesGroupOptions: [
        {label: '产品', value: 'PRODUCT'},
        {label: '客户', value: 'CUSTOMER'},
        {label: '产品+仓库', value: 'PRODUCT_WAREHOUSE'},
        {label: '客户+产品', value: 'CUSTOMER_PRODUCT'},
        {label: '客户+产品+仓库', value: 'CUSTOMER_PRODUCT_WAREHOUSE'},
      ],
    }
  },
  computed: {
    showCustomerFilter() {
      const g = this.params.salesGroupSearch;
      return g === 'CUSTOMER' || g === 'CUSTOMER_PRODUCT' || g === 'CUSTOMER_PRODUCT_WAREHOUSE';
    },
    showProductCols() {
      return this.params.salesGroupSearch !== 'CUSTOMER';
    },
    showWarehouseCols() {
      const g = this.params.salesGroupSearch;
      return g === 'PRODUCT_WAREHOUSE' || g === 'CUSTOMER_PRODUCT_WAREHOUSE';
    },
    columns() {
      const cols = [];
      if (this.showCustomerFilter) {
        cols.push(
            {colKey: 'customerCode', title: '客户编码', minWidth: 110, ellipsis: true},
            {colKey: 'customerName', title: '客户名称', minWidth: 120, ellipsis: true},
            {colKey: 'customerCategoryId', title: '客户类别', minWidth: 110, ellipsis: true},
        );
      }
      if (this.showProductCols) {
        cols.push(
            {colKey: 'productCode', title: '产品编码', minWidth: 110, ellipsis: true},
            {colKey: 'productName', title: '产品名称', minWidth: 120, ellipsis: true},
            {colKey: 'unitName', title: '销售单位', width: 90, align: 'center'},
            {colKey: 'specification', title: '规格型号', minWidth: 100, ellipsis: true},
            {colKey: 'productCategoryName', title: '产品类别', minWidth: 110, ellipsis: true},
        );
      }
      if (this.showWarehouseCols) {
        cols.push({colKey: 'warehouseName', title: '仓库名称', minWidth: 110, ellipsis: true});
      }
      cols.push(
          {colKey: 'unitPrice', title: '单价', width: 100, align: 'right'},
          {colKey: 'quantity', title: '数量', width: 100, align: 'right'},
          {colKey: 'subtotal', title: '销售收入', width: 110, align: 'right'},
      );
      return cols;
    },
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
      const sum = (key) => {
        const total = (this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
        return total.toFixed(2);
      };
      const quantity = sum('quantity');
      const subtotal = sum('subtotal');
      this.quantityTotal = quantity;
      this.subtotalTotal = subtotal;
      const foot = {quantity, subtotal};
      if (this.showCustomerFilter) {
        foot.customerCode = '合计';
      } else if (this.showProductCols) {
        foot.productCode = '合计';
      } else {
        foot.unitPrice = '合计';
      }
      return [foot];
    }
  },
  methods: {
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    printEvent() {
      window.print();
    },
    doSearch() {
      if (!this.params.salesGroup) {
        MessagePlugin.error("请选择汇总条件~");
        return;
      }
      this.params.salesGroupSearch = this.params.salesGroup;
      this.pagination.page = 1;
      this.loadList();
    },
    exportData() {
      if (this.dataList.length === 0) {
        MessagePlugin.warning('没有可导出的数据');
        return;
      }
      try {
        const exportData = this.dataList.map(item => ({
          '客户编码': item.customerCode,
          '客户名称': item.customerName,
          '产品编码': item.productCode,
          '产品名称': item.productName,
          '销售单位': item.unitName,
          '仓库名称': item.warehouseName,
          '单价': item.unitPrice,
          '数量': item.quantity,
          '销售收入': item.subtotal
        }));
        exportData.push({
          '单价': '合计',
          '数量': this.quantityTotal,
          '销售收入': this.subtotalTotal
        });
        const ws = XLSX.utils.json_to_sheet(exportData);
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, '销售汇总');
        XLSX.writeFile(wb, `销售汇总报表_${manba().format('YYYY-MM-DD')}.xlsx`);
        MessagePlugin.success('导出成功');
      } catch (error) {
        console.error('导出错误:', error);
        MessagePlugin.error('导出失败');
      }
    },
    loadSelect() {
      Promise.all([
        Customer.select(),
        Warehouse.select(),
        Product.select(),
        ProductCategory.select(),
        CustomerCategory.select(),
      ]).then((results) => {
        this.customerList = results[0].data || [];
        this.warehouseList = results[1].data || [];
        this.productList = results[2].data || [];
        this.productCategoryList = results[3].data || [];
        this.customerCategoryList = results[4].data || [];
      });
    },
    loadList() {
      this.loading = true;
      SalesReport.summary(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    handleSalesGroupChange() {
      this.params.customerIds = [];
      this.params.warehouseIds = [];
      this.params.productIds = [];
      this.params.productCategoryIds = [];
      this.params.customerCategoryIds = [];
    },
  },
  created() {
    this.loadSelect();
    this.loadList();
  }
}
</script>

