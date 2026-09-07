<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="exportData">导 出</t-button>
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="销售日期"
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
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      成本按商品「预计进货价」估算；利润 = 销售收入 − 成本金额。含已审核销货与退货净额。
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="productId"
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
          empty="暂无利润数据"
      />
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total">销售：{{ salesTotal }} / 成本：{{ costTotal }} / 利润：{{ profitTotal }}</span>
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
import manba from 'manba';
import SalesReport from '@js/api/sales/SalesReport';
import Customer from '@js/api/basic/Customer';
import Product from '@js/api/basic/Product';
import ProductCategory from '@js/api/basic/ProductCategory';
import { MessagePlugin } from 'tdesign-vue-next';
import * as XLSX from 'xlsx';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');

export default {
  name: 'SalesProfitReport',
  data() {
    return {
      loading: false,
      dataList: [],
      customerList: [],
      productList: [],
      productCategoryList: [],
      salesTotal: '0.00',
      costTotal: '0.00',
      profitTotal: '0.00',
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
        customerIds: [],
        productIds: [],
        productCategoryIds: []
      },
      dateRangeValue: [startTime, endTime],
      columns: [
        { colKey: 'productCode', title: '产品编码', minWidth: 110, ellipsis: true },
        { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
        { colKey: 'productCategoryName', title: '产品类别', minWidth: 110, ellipsis: true },
        { colKey: 'unitName', title: '单位', width: 80, align: 'center' },
        { colKey: 'quantity', title: '数量', width: 90, align: 'right' },
        { colKey: 'unitPrice', title: '销售单价', width: 100, align: 'right' },
        { colKey: 'subtotal', title: '销售收入', width: 110, align: 'right' },
        { colKey: 'costPrice', title: '成本单价', width: 100, align: 'right' },
        { colKey: 'costAmount', title: '成本金额', width: 110, align: 'right' },
        { colKey: 'profitAmount', title: '毛利', width: 110, align: 'right' },
        { colKey: 'profitRate', title: '毛利率%', width: 100, align: 'right' }
      ]
    };
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: start || null,
        end: end || null
      });
    },
    footData() {
      const sum = (key) => {
        const total = (this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
        return total.toFixed(2);
      };
      const subtotal = sum('subtotal');
      const costAmount = sum('costAmount');
      const profitAmount = sum('profitAmount');
      this.salesTotal = subtotal;
      this.costTotal = costAmount;
      this.profitTotal = profitAmount;
      return [{
        productCode: '合计',
        quantity: sum('quantity'),
        subtotal,
        costAmount,
        profitAmount
      }];
    }
  },
  methods: {
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadSelect() {
      Promise.all([Customer.select(), Product.select(), ProductCategory.select()]).then((results) => {
        this.customerList = results[0].data || [];
        this.productList = results[1].data || [];
        this.productCategoryList = results[2].data || [];
      });
    },
    loadList() {
      this.loading = true;
      SalesReport.profit(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    exportData() {
      if (!this.dataList.length) {
        MessagePlugin.warning('没有可导出的数据');
        return;
      }
      const exportData = this.dataList.map((item) => ({
        产品编码: item.productCode,
        产品名称: item.productName,
        产品类别: item.productCategoryName,
        数量: item.quantity,
        销售收入: item.subtotal,
        成本金额: item.costAmount,
        毛利: item.profitAmount,
        毛利率: item.profitRate
      }));
      const ws = XLSX.utils.json_to_sheet(exportData);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, '销售利润');
      XLSX.writeFile(wb, `销售利润表_${manba().format('YYYY-MM-DD')}.xlsx`);
      MessagePlugin.success('导出成功');
    }
  },
  created() {
    this.loadSelect();
    this.loadList();
  }
};
</script>

<style scoped>

.simple-page__hint {
  flex-shrink: 0;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f3f3f3;
  color: #555;
  font-size: 13px;
  line-height: 1.6;
}

</style>
