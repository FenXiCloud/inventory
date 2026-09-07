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
            v-if="isDateDim"
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="入库日期"
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
            v-model="supplierIds"
            :options="supplierList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择供货商"
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
          row-key="name"
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
        <template #quantity="{ row }"><span class="num">{{ fmt(row.quantity) }}</span></template>
        <template #amount="{ row }"><span class="num">¥{{ fmt(row.amount) }}</span></template>
      </t-table>
    </div>
  </div>
</template>

<script>
import manba from "manba";
import PurchaseReport from "@js/api/purchase/PurchaseReport";
import Warehouse from "@js/api/basic/Warehouse";
import Supplier from "@js/api/basic/Supplier";
import Product from "@js/api/basic/Product";
import {MessagePlugin} from "tdesign-vue-next";
import {export_json_to_excel} from "@js/excel/export2Excel";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

export default {
  name: "PurchaseStatisticsReport",
  data() {
    return {
      dataList: [],
      loading: false,
      dimension: 'day',
      warehouseIds: [],
      supplierIds: [],
      productIds: [],
      warehouseList: [],
      supplierList: [],
      productList: [],
      dateRangeValue: [startTime, endTime],
      dimensionOptions: [
        {label: '按日', value: 'day'},
        {label: '按月', value: 'month'},
        {label: '按仓库', value: 'warehouse'},
        {label: '按供货商', value: 'supplier'},
      ],
    };
  },
  computed: {
    isDateDim() {
      return this.dimension === 'day' || this.dimension === 'month';
    },
    nameTitle() {
      const map = {day: '日期', month: '月份', warehouse: '仓库', supplier: '供货商'};
      return map[this.dimension] || '维度';
    },
    columns() {
      return [
        {colKey: 'name', title: this.nameTitle, width: 160, align: 'center'},
        {colKey: 'quantity', title: '采购数量', width: 150, align: 'right'},
        {colKey: 'amount', title: '采购金额', width: 160, align: 'right'},
      ];
    },
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return {
        dimension: this.dimension,
        start: start || null,
        end: end || null,
        warehouseIds: (this.warehouseIds || []).toString(),
        supplierIds: (this.supplierIds || []).toString(),
        productIds: (this.productIds || []).toString(),
      };
    },
    footData() {
      if (!this.dataList.length) return [];
      const sum = (k) => this.dataList.reduce((a, r) => a + (Number(r[k]) || 0), 0);
      return [{
        name: '合计',
        quantity: sum('quantity').toFixed(2),
        amount: sum('amount').toFixed(2),
      }];
    },
  },
  methods: {
    fmt(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    },
    loadSelect() {
      Promise.all([Warehouse.select(), Supplier.select(), Product.select()]).then(([w, s, p]) => {
        this.warehouseList = w.data || [];
        this.supplierList = s.data || [];
        this.productList = p.data || [];
      });
    },
    loadList() {
      this.loading = true;
      PurchaseReport.statistics(this.queryParams)
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
      export_json_to_excel({header, data, filename: '采购统计表', autoWidth: true, bookType: 'xlsx'});
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
</style>
