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
      展示成本核算批次层。入库建批，出库按成本法扣减剩余数量。
    </div>

    <div class="simple-page__table">
      <t-table
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
        <template #closed="{ row }">
          <t-tag :theme="row.closed ? 'default' : 'success'" variant="light">
            {{ row.closed ? '已关闭' : '未关闭' }}
          </t-tag>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total">剩余成本：{{ amountTotal }}元</span>
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
      amountTotal: '0.00',
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
      columns: [
        { colKey: 'inboundDate', title: '入库日期', width: 120, align: 'center' },
        { colKey: 'batchNo', title: '批次号', minWidth: 180, ellipsis: true },
        { colKey: 'productCode', title: '产品编码', width: 120, ellipsis: true },
        { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
        { colKey: 'warehouseName', title: '仓库', width: 120, ellipsis: true },
        { colKey: 'inboundOrderType', title: '来源类型', width: 110, align: 'center' },
        { colKey: 'qtyIn', title: '入库数量', width: 100, align: 'right' },
        { colKey: 'qtyRemain', title: '剩余数量', width: 100, align: 'right' },
        { colKey: 'unitCost', title: '单位成本', width: 110, align: 'right' },
        { colKey: 'totalCostRemain', title: '剩余成本', width: 110, align: 'right' },
        { colKey: 'supplierName', title: '供应商', width: 120, ellipsis: true },
        { colKey: 'closed', title: '状态', width: 90, align: 'center', fixed: 'right' },
      ],
    };
  },
  computed: {
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
      return [{
        inboundDate: '合计',
        qtyIn: sum('qtyIn'),
        qtyRemain: sum('qtyRemain'),
        totalCostRemain: sum('totalCostRemain').toFixed(2),
      }];
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
    loadList() {
      this.loading = true;
      InventoryCostBatch.list(this.queryParams).then(({ data: { results, total } }) => {
        this.dataList = results || [];
        this.pagination.total = total || 0;
        const remain = (this.dataList || []).reduce((acc, row) => acc + Number(row.totalCostRemain || 0), 0);
        this.amountTotal = remain.toFixed(2);
      }).finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    Promise.all([Warehouse.select(), Product.select()]).then(([wh, prod]) => {
      this.warehouseList = wh.data || [];
      this.productList = prod.data || [];
    });
    this.loadList();
  },
};
</script>

<style scoped>
.simple-page__hint {
  flex-shrink: 0;
  padding: 0 4px 8px;
  color: var(--td-text-color-secondary, #888);
  font-size: 13px;
}
</style>
