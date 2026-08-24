<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-input
            v-model="filter"
            clearable
            placeholder="请输入订单编号"
            style="width: 220px"
            @enter="doSearch"
        >
          <template #suffixIcon><t-icon name="search" style="cursor:pointer" @click="doSearch"/></template>
        </t-input>
        <t-select
            v-model="state"
            clearable
            placeholder="订单状态"
            :options="stateOptions"
            style="width: 140px"
            @change="doSearch"
        />
        <t-button theme="primary" variant="outline" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :expanded-row-keys="expandedRowKeys"
          @expand-change="onExpandChange"
      >
        <template #orderStatus="{ row }">
          <t-tag :theme="statusTheme(row.orderStatus)" variant="light">{{ row.orderStatus }}</t-tag>
        </template>
        <template #outboundStatus="{ row }">
          <span class="track-progress">
            <t-progress :percentage="outboundPercent(row)" :label="false" style="width: 90px"/>
            <span class="track-progress__text">{{ outboundLabel(row) }}</span>
          </span>
        </template>
        <template #finalAmount="{ row }">
          <span>¥{{ fmt(row.finalAmount) }}</span>
        </template>
        <template #expandedRow="{ row }">
          <div class="track-detail">
            <t-table row-key="id" size="small" bordered :data="row.salesOrderItemList || []" :columns="detailColumns">
              <template #quantity="{ row: it }">{{ fmt(it.quantity) }}</template>
              <template #quantityOut="{ row: it }">{{ fmt(it.quantityOut) }}</template>
              <template #quantityReturn="{ row: it }">{{ fmt(it.quantityReturn) }}</template>
              <template #remain="{ row: it }">{{ fmt(remainOf(it)) }}</template>
            </t-table>
          </div>
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
import SalesOrder from '@js/api/sales/SalesOrder';

export default {
  name: 'OrderTracking',
  data() {
    return {
      loading: false,
      filter: '',
      state: null,
      dataList: [],
      expandedRowKeys: [],
      pagination: {page: 1, pageSize: 20, total: 0},
      stateOptions: [
        {label: '已保存', value: '已保存'},
        {label: '已审核', value: '已审核'},
        {label: '已平账', value: '已平账'},
        {label: '未平账', value: '未平账'},
        {label: '已取消', value: '已取消'}
      ],
      columns: [
        {colKey: 'row-expand', type: 'multiple-expand', width: 50},
        {colKey: 'orderNo', title: '订单编号', width: 180},
        {colKey: 'customerName', title: '客户', minWidth: 150, ellipsis: true},
        {colKey: 'orderDate', title: '下单日期', width: 120, align: 'center'},
        {colKey: 'orderStatus', title: '订单状态', width: 100, align: 'center'},
        {colKey: 'outboundStatus', title: '出库进度', width: 180},
        {colKey: 'finalAmount', title: '订单金额', width: 120, align: 'right'},
        {colKey: 'outOrderNo', title: '关联出库单', minWidth: 160, ellipsis: true}
      ],
      detailColumns: [
        {colKey: 'productCode', title: '编码', width: 140},
        {colKey: 'productName', title: '商品', minWidth: 160},
        {colKey: 'quantity', title: '订购数量', width: 110, align: 'right'},
        {colKey: 'quantityOut', title: '已出库', width: 110, align: 'right'},
        {colKey: 'quantityReturn', title: '已退货', width: 110, align: 'right'},
        {colKey: 'remain', title: '剩余未出', width: 110, align: 'right'}
      ]
    };
  },
  methods: {
    fmt(v) {
      const n = Number(v) || 0;
      return n.toFixed(2);
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    onExpandChange(keys) {
      this.expandedRowKeys = keys;
    },
    loadList() {
      this.loading = true;
      SalesOrder.list({
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        filter: this.filter || null,
        state: this.state || null
      })
        .then(({data}) => {
          this.dataList = data?.results || [];
          this.pagination.total = data?.total || 0;
        })
        .finally(() => (this.loading = false));
    },
    statusTheme(status) {
      const map = {
        '已保存': 'default',
        '已审核': 'primary',
        '已平账': 'success',
        '未平账': 'warning',
        '已取消': 'danger'
      };
      return map[status] || 'default';
    },
    outboundPercent(row) {
      if (row.status === 2) return 100;
      const total = (row.salesOrderItemList || []).reduce((s, i) => s + (Number(i.quantity) || 0), 0);
      const out = (row.salesOrderItemList || []).reduce((s, i) => s + (Number(i.quantityOut) || 0), 0);
      if (!total) return 0;
      return Math.min(100, Math.round((out / total) * 100));
    },
    outboundLabel(row) {
      if (row.status === 2) return '全部出库';
      if (row.status === 1) return '部分出库';
      return '未出库';
    },
    remainOf(it) {
      const q = Number(it.quantity) || 0;
      const out = Number(it.quantityOut) || 0;
      const ret = Number(it.quantityReturn) || 0;
      return q + ret - out;
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>
.track-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.track-progress__text {
  font-size: 12px;
  color: #646a73;
}

.track-detail {
  padding: 8px 16px;
  background: #fafafa;
}
</style>
