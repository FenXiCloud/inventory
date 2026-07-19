<template>
  <div class="modal-column">
    <div class="modal-column-full-body flex flex-column">
      <vxe-toolbar>
        <template #buttons>
        </template>
        <template #tools>
          <div class="h-input-group">
            <span class="h-input-addon ml-8px">订单日期：</span>
            <DateRangePicker v-model="dateRange"></DateRangePicker>
          </div>
          <Search v-model.trim="params.filter"
                  show-search-button class="w-360px ml-8px"
                  placeholder="请输入订单号" @search="doSearch">
            <t-icon name="search" />
          </Search>
        </template>
      </vxe-toolbar>
      <div class="flex1">
        <vxe-table row-id="id"
                   ref="table"
                   height="auto"
                   :data="dataList"
                   highlight-hover-row
                   show-overflow
                   show-footer
                   :footer-method="footerMethod"
                   :row-config="{height: 48}"
                   :column-config="{resizable: true}"
                   :sort-config="{remote:true}"
                   :loading="loading">
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
      <vxe-pager perfect @page-change="loadList(false)"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="[ 'PrevPage', 'Number', 'NextPage',  'Sizes', 'Total']">
        <template #left>
          <span class="mr-12px text-14px">合计金额：{{ amountTotal }}元</span>
          <vxe-button @click="loadList(false)" type="text" size="mini" icon="vxe-icon-refresh"
                      :loading="loading"></vxe-button>
        </template>
      </vxe-pager>
    </div>
    <div class="modal-column-between">
      <Button @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button color="primary" @click="confirm" :loading="loading">
        确认
      </Button>
    </div>
  </div>
</template>
<script>
import manba from "manba";
import PurchaseOrder from "@js/api/purchase/PurchaseOrder";
import {DialogPlugin, MessagePlugin} from "tdesign-vue-next";
import PurchaseInbound from "@js/api/purchase/PurchaseInbound";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "PurchaseOrderSelect",
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
      totalParams: {},
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
      return Object.assign(this.params, {
        supplierId: this.supplierId,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: this.dateRange.start,
        end: this.dateRange.end,
      })
    },
  },
  methods: {
    confirm() {
      let checkList = this.$refs.table.getCheckboxRecords();
      if (checkList.length && checkList.length > 0) {
        let ids = checkList.map(val => val.id);
        let params = {
          orderIds: ids
        };
        // 这里可以触发成功事件并传递数据
        this.$emit('success', params);
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
