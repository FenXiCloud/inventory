<template>
  <div class="order-select">
    <div class="order-select__toolbar">
      <t-select
          v-model="params.state"
          class="order-select__state"
          :options="[{ value: '已保存', label: '未审核' }, { value: '已审核', label: '已审核' }]"
          placeholder="审核状态："
      />
      <span class="order-select__label">订单日期：</span>
      <t-date-range-picker v-model="dateRange" clearable allow-input style="width: 260px; border-radius: 4px"/>
      <span class="order-select__label">客户：</span>
      <t-select
          class="order-select__customer"
          filterable
          :options="customerList"
          :keys="{ value: 'id', label: 'name' }"
          v-model="params.customerId"
          placeholder="请选择客户"
          readonly
          disabled
      />
      <t-input
          v-model.trim="params.filter"
          class="order-select__search"
          placeholder="请输入订单号"
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
      <vxe-table
          row-id="id"
          ref="table"
          height="auto"
          border
          show-overflow
          :data="dataList"
          highlight-hover-row
          show-footer
          :footer-method="footerMethod"
          :row-config="{height: 48}"
          :column-config="{resizable: true}"
          :sort-config="{remote:true}"
          :loading="loading"
      >
        <vxe-column type="checkbox" width="40" align="center"/>
        <vxe-column title="订单日期" field="orderDate" align="center" width="130"/>
        <vxe-column title="订单编号" field="orderNo" width="200"/>
        <vxe-column title="关联销售出库单" field="code" width="200"/>
        <vxe-column title="客户" field="customerName" min-width="120"/>
        <vxe-column title="销售金额" field="totalAmount" width="120"/>
        <vxe-column title="折扣金额" field="discountAmount" width="120"/>
        <vxe-column title="折后金额" field="finalAmount" width="120"/>
      </vxe-table>
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
      <t-button theme="primary" @click="batchSelect" :loading="loading">确认</t-button>
    </div>
  </div>
</template>
<script>
import manba from "manba";
import SalesOrder from "@js/api/sales/SalesOrder";
import {MessagePlugin} from "tdesign-vue-next";
import Customer from "@js/api/basic/Customer";

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
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        state: '已审核',
        sortCol: null,
        sort: null,
        customerId: null
      },
      customerList: [],
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd")
      },
    }
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: this.dateRange.start,
        end: this.dateRange.end,
        //查询未出库订单
        queryUnOutOrder: 1
      })
    },
  },
  methods: {
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    batchSelect() {
      const selectedRows = this.$refs.table.getCheckboxRecords();
      if (selectedRows.length === 0) {
        MessagePlugin.error("请选择至少一条订单");
        return;
      }
      let allItemList = [];
      let selectSalesOrderIdList = [];
      selectedRows.forEach(row => {
        if (row.salesOrderItemList && row.salesOrderItemList.length > 0) {
          allItemList = allItemList.concat(row.salesOrderItemList);
          allItemList.forEach(item => {
            let quantity = item.quantity + item.quantityReturn;
            let quantityOut = item.quantityOut;
            if (quantity > quantityOut) {
              item.quantity = quantity - quantityOut;
            }
          })
          selectSalesOrderIdList = selectSalesOrderIdList.concat(row.id);
        }
      });

      this.$emit('success', {
        selectSalesOrderIdList: selectSalesOrderIdList,
        itemList: allItemList
      });
    },
    footerMethod({columns, data}) {
      let totalAmount = 0;
      let discountAmount = 0;
      let finalAmount = 0;
      columns.forEach((column) => {
        if (column.property && ['totalAmount', 'discountAmount', 'finalAmount'].includes(column.property)) {
          data.forEach((row) => {
            let rd = row[column.property];
            if (column.property === 'totalAmount') {
              if (rd) {
                totalAmount += Number(rd || 0);
              }
            } else if (column.property === 'discountAmount') {
              if (rd) {
                discountAmount += Number(rd || 0);
              }
            } else if (column.property === 'finalAmount') {
              if (rd) {
                finalAmount += Number(rd || 0);
              }
            }
          });
        }
      })
      this.amountTotal = totalAmount.toFixed(2);
      return [["", "", "", "", "", "", totalAmount.toFixed(2), discountAmount.toFixed(2), finalAmount.toFixed(2)]];
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      SalesOrder.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);

      Customer.select().then(({data}) => {
        this.customerList = data || [];
      });
    },
  },
  created() {
    if (this.customerId) {
      this.params.customerId = this.customerId;
    }
    this.loadList();
  }
}
</script>

<style scoped>
.order-select {
  display: flex;
  flex-direction: column;
  height: 70vh;
  min-height: 480px;
  max-height: calc(100vh - 120px);
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

.order-select__state {
  width: 120px;
}

.order-select__customer {
  width: 180px;
}

.order-select__search {
  width: 320px;
  max-width: 100%;
}

.order-select__table {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0 16px;
  overflow: hidden;
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
