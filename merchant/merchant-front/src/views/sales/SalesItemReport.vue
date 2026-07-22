<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="exportData">导 出</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="printEvent">打 印</t-button>
        <t-select
            v-model="params.salesType"
            :options="salesTypeOptions"
            placeholder="业务类别"
            style="width: 120px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="订单日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-select
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
            v-model="params.customerCategoryIds"
            :options="customerCategoryList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="客户类别"
            style="width: 160px; border-radius: 4px"
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
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入订单号"
            style="width: 200px; background: #fff; border-radius: 4px"
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
          ref="tableRef"
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
      >
        <template #salesType="{ row }">
          {{ formatSalesType(row.salesType) }}
        </template>
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
import * as XLSX from 'xlsx';
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";
import CustomerCategory from "@js/api/basic/CustomerCategory";
import ProductCategory from "@js/api/basic/ProductCategory";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

export default {
  name: "SalesItemReport",
  data() {
    return {
      dataList: [],
      loading: false,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        salesType: 'out',
        customerIds: [],
        customerCategoryIds: [],
        warehouseIds: [],
        productIds: [],
        productCategoryIds: [],
      },
      customerList: [],
      warehouseList: [],
      productList: [],
      customerCategoryList: [],
      productCategoryList: [],
      dateRangeValue: [startTime, endTime],
      salesTypeOptions: [
        {label: '全部', value: 'all'},
        {label: '销货', value: 'out'},
        {label: '退货', value: 'return'},
      ],
      columns: [
        {colKey: 'orderDate', title: '销售日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '订单编号', minWidth: 150, ellipsis: true},
        {colKey: 'salesType', title: '业务类别', width: 90, align: 'center'},
        {colKey: 'customerCode', title: '客户编码', minWidth: 110, ellipsis: true},
        {colKey: 'customerName', title: '客户', minWidth: 120, ellipsis: true},
        {colKey: 'customerCategoryId', title: '客户类别', minWidth: 110, ellipsis: true},
        {colKey: 'productCode', title: '产品编码', minWidth: 110, ellipsis: true},
        {colKey: 'productName', title: '产品名称', minWidth: 120, ellipsis: true},
        {colKey: 'productCategoryName', title: '产品类别', minWidth: 110, ellipsis: true},
        {colKey: 'specification', title: '规格型号', width: 100, ellipsis: true},
        {colKey: 'unitName', title: '销售单位', width: 90, align: 'center'},
        {colKey: 'warehouseName', title: '仓库名称', minWidth: 110, ellipsis: true},
        {colKey: 'quantity', title: '数量', width: 90, align: 'right'},
        {colKey: 'unitPrice', title: '单价', width: 90, align: 'right'},
        {colKey: 'subtotal', title: '销售收入', width: 110, align: 'right'},
      ]
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
      const sum = (key) => {
        const total = (this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
        return total.toFixed(2);
      };
      return [{
        orderDate: '合计',
        quantity: sum('quantity'),
        subtotal: sum('subtotal'),
      }];
    }
  },
  methods: {
    formatSalesType(value) {
      if (value === 'return') return '退货';
      if (value === 'out') return '销货';
      return value || '销货';
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    printEvent() {
      window.print();
    },
    exportData() {
      if (this.dataList.length === 0) {
        MessagePlugin.warning('没有可导出的数据');
        return;
      }
      try {
        const quantityTotal = this.footData[0].quantity;
        const subtotalTotal = this.footData[0].subtotal;
        const exportData = this.dataList.map(item => ({
          '销售日期': item.orderDate,
          '订单编号': item.orderNo,
          '业务类别': this.formatSalesType(item.salesType),
          '客户': item.customerName,
          '产品编码': item.productCode,
          '产品名称': item.productName,
          '销售单位': item.unitName,
          '仓库名称': item.warehouseName,
          '数量': item.quantity,
          '单价': item.unitPrice,
          '销售收入': item.subtotal
        }));
        exportData.push({
          '销售日期': '合计',
          '数量': quantityTotal,
          '销售收入': subtotalTotal
        });
        const ws = XLSX.utils.json_to_sheet(exportData);
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, '销售明细');
        XLSX.writeFile(wb, `销售明细报表_${manba().format('YYYY-MM-DD')}.xlsx`);
        MessagePlugin.success('导出成功');
      } catch (error) {
        console.error('导出错误:', error);
        MessagePlugin.error('导出失败');
      }
    },
    doSearch() {
      if (!this.params.salesType) {
        MessagePlugin.error("请选择业务类型~");
        return;
      }
      this.pagination.page = 1;
      this.loadList();
    },
    loadSelect() {
      Promise.all([
        Customer.select(),
        Warehouse.select(),
        Product.select(),
        CustomerCategory.select(),
        ProductCategory.select(),
      ]).then((results) => {
        this.customerList = results[0].data || [];
        this.warehouseList = results[1].data || [];
        this.productList = results[2].data || [];
        this.customerCategoryList = results[3].data || [];
        this.productCategoryList = results[4].data || [];
      });
    },
    loadList() {
      this.loading = true;
      SalesReport.item(this.queryParams).then(({data: {results, total}}) => {
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

