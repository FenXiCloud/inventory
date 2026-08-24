<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="exportData">导 出</t-button>
        <t-select
            v-model="dimension"
            :options="dimensionOptions"
            placeholder="统计维度"
            style="width: 160px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-select
            v-model="warehouseIds"
            :options="warehouseList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择仓库"
            style="width: 180px; border-radius: 4px"
        />
        <t-select
            v-model="productIds"
            :options="productList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择产品"
            style="width: 180px; border-radius: 4px"
        />
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="loadList">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="date"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
      >
        <template #salesAmount="{ row }"><span class="num">¥{{ fmt(row.salesAmount) }}</span></template>
        <template #costAmount="{ row }"><span class="num">¥{{ fmt(row.costAmount) }}</span></template>
        <template #profitAmount="{ row }"><span class="num" :class="{'profit': Number(row.profitAmount) >= 0}">¥{{ fmt(row.profitAmount) }}</span></template>
      </t-table>
    </div>
  </div>
</template>

<script>
import manba from "manba";
import SalesReport from "@js/api/sales/SalesReport";
import Warehouse from "@js/api/basic/Warehouse";
import Product from "@js/api/basic/Product";
import {MessagePlugin} from "tdesign-vue-next";
import {export_json_to_excel} from "@js/excel/export2Excel";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

export default {
  name: "SalesStatisticsReport",
  data() {
    return {
      dataList: [],
      loading: false,
      dimension: 'DAY',
      warehouseIds: [],
      productIds: [],
      warehouseList: [],
      productList: [],
      dateRangeValue: [startTime, endTime],
      dimensionOptions: [
        {label: '按日', value: 'DAY'},
        {label: '按月', value: 'MONTH'},
      ],
    };
  },
  computed: {
    columns() {
      return [
        {colKey: 'date', title: this.dimension === 'MONTH' ? '月份' : '日期', width: 140, align: 'center'},
        {colKey: 'salesAmount', title: '销售额', width: 150, align: 'right'},
        {colKey: 'costAmount', title: '成本', width: 150, align: 'right'},
        {colKey: 'profitAmount', title: '毛利', width: 150, align: 'right'},
      ];
    },
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return {
        salesGroup: this.dimension,
        start: start || null,
        end: end || null,
        warehouseIds: this.warehouseIds || [],
        productIds: this.productIds || [],
      };
    },
    footData() {
      if (!this.dataList.length) return [];
      const sum = (k) => this.dataList.reduce((a, r) => a + (Number(r[k]) || 0), 0);
      return [{
        date: '合计',
        salesAmount: sum('salesAmount').toFixed(2),
        costAmount: sum('costAmount').toFixed(2),
        profitAmount: sum('profitAmount').toFixed(2),
      }];
    },
  },
  methods: {
    fmt(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    },
    loadSelect() {
      Promise.all([Warehouse.select(), Product.select()]).then(([w, p]) => {
        this.warehouseList = w.data || [];
        this.productList = p.data || [];
      });
    },
    loadList() {
      this.loading = true;
      SalesReport.statistics(this.queryParams)
        .then(({data}) => {
          this.dataList = data || [];
        })
        .finally(() => (this.loading = false));
    },
    exportData() {
      if (!this.dataList.length) {
        MessagePlugin.warning('没有可导出的数据');
        return;
      }
      const header = this.columns.map((c) => c.title);
      const data = this.dataList.map((row) => this.columns.map((c) => row[c.colKey] ?? ''));
      export_json_to_excel({header, data, filename: '销售统计表', autoWidth: true, bookType: 'xlsx'});
    },
  },
  created() {
    this.loadSelect();
    this.loadList();
  }
};
</script>

<style scoped>
.num {
  font-variant-numeric: tabular-nums;
}
.profit {
  color: #2ba471;
}
</style>
