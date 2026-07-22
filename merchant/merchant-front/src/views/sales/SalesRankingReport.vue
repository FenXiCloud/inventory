<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="exportData">导 出</t-button>
        <t-select
            v-model="params.rankingType"
            :options="rankingTypeOptions"
            placeholder="排行维度"
            style="width: 140px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="销售日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-select
            v-if="params.rankingType === 'CUSTOMER'"
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
            v-if="params.rankingType === 'PRODUCT'"
            v-model="params.productIds"
            :options="productList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择产品"
            style="width: 180px; border-radius: 4px"
        />
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="rowKey"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="fixed"
          :data="displayList"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
          empty="暂无排行数据"
      />
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total">合计金额：{{ amountTotal }}</span>
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
import manba from 'manba';
import SalesReport from '@js/api/sales/SalesReport';
import Customer from '@js/api/basic/Customer';
import Product from '@js/api/basic/Product';
import { MessagePlugin } from 'tdesign-vue-next';
import * as XLSX from 'xlsx';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');

export default {
  name: 'SalesRankingReport',
  data() {
    return {
      loading: false,
      dataList: [],
      customerList: [],
      productList: [],
      amountTotal: '0.00',
      pagination: { page: 1, pageSize: 20, total: 0 },
      params: {
        rankingType: 'PRODUCT',
        customerIds: [],
        productIds: []
      },
      dateRangeValue: [startTime, endTime],
      rankingTypeOptions: [
        { label: '按产品', value: 'PRODUCT' },
        { label: '按客户', value: 'CUSTOMER' }
      ]
    };
  },
  computed: {
    isCustomerRank() {
      return this.params.rankingType === 'CUSTOMER';
    },
    columns() {
      const cols = [
        { colKey: 'rankNo', title: '排名', width: 80, align: 'center' }
      ];
      if (this.isCustomerRank) {
        cols.push(
          { colKey: 'customerCode', title: '客户编码', minWidth: 120, ellipsis: true },
          { colKey: 'customerName', title: '客户名称', minWidth: 160, ellipsis: true }
        );
      } else {
        cols.push(
          { colKey: 'productCode', title: '产品编码', minWidth: 120, ellipsis: true },
          { colKey: 'productName', title: '产品名称', minWidth: 160, ellipsis: true },
          { colKey: 'unitName', title: '单位', width: 80, align: 'center' }
        );
      }
      cols.push(
        { colKey: 'quantity', title: '数量', width: 110, align: 'right' },
        { colKey: 'unitPrice', title: '均价', width: 110, align: 'right' },
        { colKey: 'subtotal', title: '销售金额', width: 130, align: 'right' }
      );
      return cols;
    },
    displayList() {
      return (this.dataList || []).map((row, index) => ({
        ...row,
        rowKey: `${row.rankNo || index}-${row.productId || ''}-${row.customerId || ''}`
      }));
    },
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
      this.amountTotal = subtotal;
      const foot = {
        quantity: sum('quantity'),
        subtotal
      };
      if (this.isCustomerRank) {
        foot.customerCode = '合计';
      } else {
        foot.productCode = '合计';
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
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadSelect() {
      Promise.all([Customer.select(), Product.select()]).then((results) => {
        this.customerList = results[0].data || [];
        this.productList = results[1].data || [];
      });
    },
    loadList() {
      this.loading = true;
      SalesReport.ranking(this.queryParams)
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
      const exportData = this.dataList.map((item) => {
        if (this.isCustomerRank) {
          return {
            排名: item.rankNo,
            客户编码: item.customerCode,
            客户名称: item.customerName,
            数量: item.quantity,
            销售金额: item.subtotal
          };
        }
        return {
          排名: item.rankNo,
          产品编码: item.productCode,
          产品名称: item.productName,
          数量: item.quantity,
          销售金额: item.subtotal
        };
      });
      const ws = XLSX.utils.json_to_sheet(exportData);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, '销售排行');
      XLSX.writeFile(wb, `销售排行表_${manba().format('YYYY-MM-DD')}.xlsx`);
      MessagePlugin.success('导出成功');
    }
  },
  created() {
    this.loadSelect();
    this.loadList();
  }
};
</script>

