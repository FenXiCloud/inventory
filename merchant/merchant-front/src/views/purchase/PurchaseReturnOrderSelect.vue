<template>
  <div class="order-select">
    <div class="order-select__toolbar">
      <span class="order-select__label">订单日期：</span>
      <DateRangePicker v-model="dateRange"/>
      <Search
          v-model.trim="params.filter"
          show-search-button
          class="order-select__search"
          placeholder="请输入订单编号"
          @search="doSearch"
      >
        <t-icon name="search"/>
      </Search>
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
        <vxe-column title="入库日期" field="inboundDate" align="center" width="130"/>
        <vxe-column title="订单编号" field="orderNo" width="200"/>
        <vxe-column title="供货商" field="supplierName" min-width="120"/>
        <vxe-column title="采购金额" field="finalAmount" width="120"/>
        <vxe-column title="折扣金额" field="discountAmount" width="120"/>
        <vxe-column title="折后金额" field="finalAmount" width="120"/>
        <vxe-column title="制单人" field="createdName" align="center" width="100"/>
        <vxe-column title="制单时间" field="createdAt" align="center" width="100"/>
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
      <Button @click="$emit('close')" :loading="loading">取消</Button>
      <Button color="primary" @click="confirm" :loading="loading">确认</Button>
    </div>
  </div>
</template>
<script>
import manba from "manba";
import {MessagePlugin} from "tdesign-vue-next";
import PurchaseInbound from "@js/api/purchase/PurchaseInbound";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "PurchaseReturnOrderSelect",
  props: {
    supplierId: {
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
        state: null,
        sortCol: null,
        sort: null,
      },
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd")
      },
    }
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params, {
        supplierId: this.supplierId,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: this.dateRange.start,
        end: this.dateRange.end,
      })
    },
  },
  methods: {
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    confirm() {
      let checkList = this.$refs.table.getCheckboxRecords();
      if (checkList.length && checkList.length > 0) {
        let ids = checkList.map(val => val.id);
        this.$emit('success', {orderIds: ids});
      } else {
        MessagePlugin.error("未选择数据~");
      }
    },
    footerMethod({columns, data}) {
      let sums = [];
      columns.forEach((column) => {
        if (column.property && ['finalAmount'].includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              total += Number(rd || 0);
            }
          });
          sums.push(total.toFixed(2));
        }
      })
      return [["", "", "", "", "", ""].concat(sums)];
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      PurchaseInbound.listToReturn(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
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
