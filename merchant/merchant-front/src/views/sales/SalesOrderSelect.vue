<template>
  <div class="order-select">
    <div class="order-select__toolbar">
      <span class="order-select__label">订单日期：</span>
      <t-date-range-picker v-model="dateRange" clearable allow-input style="width: 260px; border-radius: 4px"/>
      <t-input
          v-model.trim="params.filter"
          class="order-select__search"
          placeholder="请输入订单编号"
          clearable
          style="width: 220px; border-radius: 4px"
          @enter="doSearch"
      >
        <template #suffixIcon>
          <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
        </template>
      </t-input>
      <t-button theme="primary" variant="outline" style="border-radius: 4px" @click="doSearch">搜索</t-button>
    </div>
    <div class="order-select__table">
      <t-table
          row-key="id"
          ref="table"
          size="medium"
          bordered
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :selected-row-keys="selectedRowKeys"
          @select-change="onSelectChange"
      >
        <template #remainQuantity="{ row }">
          <span :class="{ 'qty-zero': Number(row.remainQuantity) <= 0 }">{{ fmtQty(row.remainQuantity) }}</span>
        </template>
      </t-table>
    </div>
    <div class="order-select__pager">
      <span class="order-select__total">合计金额：{{ amountTotal }}元</span>
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
    <div class="order-select__footer">
      <t-button @click="$emit('close')" :loading="loading">取消</t-button>
      <t-button theme="primary" @click="confirm" :loading="loading">确认</t-button>
    </div>
  </div>
</template>
<script>
import manba from "manba";
import SalesOrder from "@js/api/sales/SalesOrder";
import {MessagePlugin} from "tdesign-vue-next";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "SalesOrderSelect",
  props: {
    customerId: {
      type: [String, Number],
      default: null
    }
  },
  data() {
    return {
      dataList: [],
      loading: false,
      amountTotal: 0,
      selectedRowKeys: [],
      selectedRows: [],
      columns: [
        { colKey: 'row-select', type: 'multiple', width: 46 },
        { colKey: 'orderDate', title: '订单日期', width: 130, align: 'center' },
        { colKey: 'orderNo', title: '订单编号', width: 200 },
        { colKey: 'outOrderNo', title: '关联销售出库单', width: 200 },
        { colKey: 'customerName', title: '客户', minWidth: 120 },
        { colKey: 'orderQuantity', title: '商品数量', width: 100, align: 'right' },
        { colKey: 'outQuantity', title: '已出库', width: 90, align: 'right' },
        { colKey: 'returnQuantity', title: '已退货', width: 90, align: 'right' },
        { colKey: 'remainQuantity', title: '可出库', width: 90, align: 'right' },
        { colKey: 'totalAmount', title: '销售金额', width: 120 },
        { colKey: 'discountAmount', title: '折扣金额', width: 120 },
        { colKey: 'finalAmount', title: '折后金额', width: 120 },
        { colKey: 'createdName', title: '制单人', width: 100, align: 'center' },
        { colKey: 'createdAt', title: '制单时间', width: 160, align: 'center' },
      ],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        state: null,
        sortCol: null,
        sort: null,
      },
      dateRange: [startTime, endTime],
    }
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRange || [];
      return Object.assign({}, this.params, {
        customerId: this.customerId,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start,
        end,
        state: '已审核',
      })
    },
  },
  methods: {
    onSelectChange(keys, { selectedRowData }) {
      this.selectedRowKeys = keys;
      this.selectedRows = selectedRowData || [];
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    confirm() {
      let checkList = this.selectedRows;
      if (checkList.length && checkList.length > 0) {
        let ids = checkList.map(val => val.id);
        this.$emit('success', {orderIds: ids});
      } else {
        MessagePlugin.error("未选择数据~");
      }
    },
    updateAmountTotal() {
      let total = 0;
      (this.dataList || []).forEach((row) => {
        if (row.finalAmount) {
          total += Number(row.finalAmount || 0);
        }
      });
      this.amountTotal = total.toFixed(2);
    },
    fmtQty(v) {
      const n = Number(v || 0);
      if (!isFinite(n)) return '0';
      return n.toFixed(2).replace(/\.?0+$/, '');
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      this.selectedRowKeys = [];
      this.selectedRows = [];
      SalesOrder.listToOutBound(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
        this.updateAmountTotal();
      }).finally(() => this.loading = false);
    },
  },
  created() {
    this.loadList();
  }
}
</script>

<style scoped>
.order-select {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 480px;
  overflow: hidden;
  background: #fff;
}

.order-select__toolbar {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--td-component-border, #e7e7e7);
}

.order-select__label {
  color: #333;
  white-space: nowrap;
}

.order-select__search {
  width: 320px;
  max-width: 100%;
}

.order-select__table {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0 16px;
  overflow: auto;
}

.order-select__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  border-top: 1px solid var(--td-component-border, #e7e7e7);
}

.order-select__total {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
}

.qty-zero {
  color: #d54941;
  font-weight: 500;
}

.order-select__footer {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #f5f5f5;
  border-top: 1px solid var(--td-component-border, #e7e7e7);
}
</style>
