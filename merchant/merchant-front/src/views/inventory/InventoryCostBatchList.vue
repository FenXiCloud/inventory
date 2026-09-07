<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-select
            v-model="params.warehouseId"
            :options="warehouseList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="仓库"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.productId"
            :options="productList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="产品"
            style="width: 200px; border-radius: 4px"
        />
        <t-select
            v-model="params.closed"
            :options="closedOptions"
            clearable
            placeholder="批次状态"
            style="width: 140px; border-radius: 4px"
        />
        <t-select
            v-model="params.inboundOrderType"
            :options="orderTypeOptions"
            clearable
            placeholder="来源类型"
            style="width: 140px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="入库日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="批次号/产品编码/名称"
            style="width: 240px; background: #fff; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      {{ hintText }}
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
        <template #qtyIn="{ row }"><span class="num">{{ fmtInt(row.qtyIn) }}</span></template>
        <template #qtyRemain="{ row }"><span class="num">{{ fmtInt(row.qtyRemain) }}</span></template>
        <template #amountIn="{ row }"><span class="num">{{ fmt(row.amountIn) }}</span></template>
        <template #unitCost="{ row }"><span class="num">{{ fmtCost(row.unitCost) }}</span></template>
        <template #totalCostRemain="{ row }"><span class="num">{{ fmt(row.totalCostRemain) }}</span></template>
        <template #closed="{ row }">
          <t-tag :theme="row.closed ? 'default' : 'success'" variant="light">
            {{ row.closed ? '已关闭' : '未关闭' }}
          </t-tag>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <span v-if="isFifo" class="simple-page__total">入库金额：{{ amountInTotal }} / 剩余成本：{{ amountTotal }}元</span>
      <span v-else class="simple-page__total">剩余数量合计：{{ remainQtyTotal }}</span>
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
import InventoryCostBatch from '@js/api/inventory/InventoryCostBatch';
import AccountBook from '@js/api/setting/AccountBook';
import Product from '@js/api/basic/Product';
import Warehouse from '@js/api/basic/Warehouse';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');

export default {
  name: 'InventoryCostBatchList',
  data() {
    return {
      dataList: [],
      loading: false,
      // 成本核算方法：1=移动平均，2=先进先出（读取账套参数 costAccounting）
      costMethod: 1,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0,
      },
      params: {
        warehouseId: null,
        productId: null,
        closed: null,
        inboundOrderType: null,
        filter: null,
      },
      dateRangeValue: [startTime, endTime],
      warehouseList: [],
      productList: [],
      closedOptions: [
        { label: '未关闭', value: false },
        { label: '已关闭', value: true },
      ],
      orderTypeOptions: [
        { label: '采购入库', value: '采购入库' },
        { label: '其他入库', value: '其他入库' },
        { label: '盘盈入库', value: '盘盈入库' },
        { label: '调拨入库', value: '调拨入库' },
        { label: '销售退货', value: '销售退货' },
        { label: '期初库存', value: '期初库存' },
      ],
    };
  },
  computed: {
    isFifo() {
      return Number(this.costMethod) === 2;
    },
    hintText() {
      if (this.isFifo) {
        return '先进先出法：每个入库批次登记入库数量、单位成本与入库金额；出库按入库先后顺序核算成本。';
      }
      return '移动平均法：成本按库内移动平均核算，不按批次标记单位成本；批次仅用于数量与来源追溯。';
    },
    columns() {
      const cols = [
        { colKey: 'inboundDate', title: '入库日期', width: 120, align: 'center' },
        { colKey: 'batchNo', title: '批次号', minWidth: 180, ellipsis: true },
        { colKey: 'productCode', title: '产品编码', width: 120, ellipsis: true },
        { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
        { colKey: 'warehouseName', title: '仓库', width: 120, ellipsis: true },
        { colKey: 'inboundOrderType', title: '来源类型', width: 110, align: 'center' },
        { colKey: 'qtyIn', title: '入库数量', width: 100, align: 'right' },
      ];
      // 先进先出法才登记/展示批次单位成本与金额
      if (this.isFifo) {
        cols.push({ colKey: 'amountIn', title: '入库金额', width: 120, align: 'right' });
      }
      cols.push({ colKey: 'qtyRemain', title: '剩余数量', width: 100, align: 'right' });
      if (this.isFifo) {
        cols.push({ colKey: 'unitCost', title: '单位成本', width: 110, align: 'right' });
        cols.push({ colKey: 'totalCostRemain', title: '剩余成本', width: 120, align: 'right' });
      }
      cols.push({ colKey: 'supplierName', title: '供应商', width: 120, ellipsis: true });
      cols.push({ colKey: 'closed', title: '状态', width: 90, align: 'center', fixed: 'right' });
      return cols;
    },
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: start || null,
        end: end || null,
      });
    },
    footData() {
      const sum = (key) => (this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
      const foot = {
        inboundDate: '合计',
        qtyIn: sum('qtyIn'),
        qtyRemain: sum('qtyRemain'),
      };
      if (this.isFifo) {
        foot.amountIn = sum('amountIn').toFixed(2);
        foot.totalCostRemain = sum('totalCostRemain').toFixed(2);
      }
      return [foot];
    },
    amountInTotal() {
      const sum = (this.dataList || []).reduce((acc, row) => acc + Number(row.amountIn || 0), 0);
      return sum.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },
    amountTotal() {
      const sum = (this.dataList || []).reduce((acc, row) => acc + Number(row.totalCostRemain || 0), 0);
      return sum.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },
    remainQtyTotal() {
      const sum = (this.dataList || []).reduce((acc, row) => acc + Number(row.qtyRemain || 0), 0);
      return sum.toLocaleString('zh-CN');
    },
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
    fmtInt(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN');
    },
    fmt(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },
    fmtCost(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
    },
    loadList() {
      this.loading = true;
      InventoryCostBatch.list(this.queryParams).then(({ data: { results, total } }) => {
        this.dataList = results || [];
        this.pagination.total = total || 0;
      }).finally(() => {
        this.loading = false;
      });
    },
    loadCostMethod() {
      AccountBook.parameters().then(({ data }) => {
        if (data && data.costAccounting != null) {
          this.costMethod = Number(data.costAccounting);
        }
      }).catch(() => {
        // 读取失败时按默认移动平均展示
      }).finally(() => {
        this.loadList();
      });
    },
  },
  created() {
    Promise.all([Warehouse.select(), Product.select()]).then(([wh, prod]) => {
      this.warehouseList = wh.data || [];
      this.productList = prod.data || [];
    });
    this.loadCostMethod();
  },
};
</script>

<style scoped>
.num {
  font-variant-numeric: tabular-nums;
}
.simple-page__hint {
  flex-shrink: 0;
  padding: 0 4px 8px;
  color: var(--td-text-color-secondary, #888);
  font-size: 13px;
}
</style>
