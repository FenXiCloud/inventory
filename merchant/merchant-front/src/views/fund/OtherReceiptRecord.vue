<template>
  <div class="frame-page flex flex-column">
    <!-- <vxe-toolbar>
      <template #buttons>
        <Select
          v-model="params.orderStatus"
          class="w-120px"
          :datas="{ 已保存: '未审核', 已审核: '已审核' }"
          placeholder="订单状态："
        />
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">日期：</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">单据类型：</span>
          <Select
            v-model="params.orderType"
            class="w-180px"
            :datas="{ 1: '收款单', 2: '预收款单' }"
            placeholder="选择单据类型"
          />
        </div>

        <Search
          v-model.trim="params.keyword"
          show-search-button
          class="w-280px ml-8px"
          placeholder="请输入客户名称或订单编号"
          @search="doSearch"
        >
          <t-icon name="search" />
        </Search>
      </template>
    </vxe-toolbar> -->
    <vxe-toolbar>
      <template #buttons>
        <Button @click="addForm()" color="primary">新 增</Button>
        <Button @click="batchAudit('已审核')"> 审 核 </Button>

        <Button @click="batchAudit('已保存')"> 反审核</Button>
        <Button @click="doRemove()"> 删 除</Button>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table
        row-id="id"
        ref="table"
        height="auto"
        :data="dataList"
        highlight-hover-row
        show-overflow
        show-footer
        :footer-method="footerMethod"
        :row-config="{ height: 48 }"
        :column-config="{ resizable: true }"
        :sort-config="{ remote: true }"
        :loading="loading"
      >
        <vxe-column type="checkbox" width="40" align="center" />
        <vxe-column title="操作" align="center" width="120">
          <template #default="{ row }">
            <span
              v-if="row.orderStatus != '已审核'"
              class="primary-color text-hover ml-10px"
              @click="addForm('edit', row.id)"
              >编辑</span
            >
            <span
              v-if="row.orderStatus != '已审核'"
              class="primary-color text-hover ml-10px"
              @click="doRemove(row)"
              >删除</span
            >
            <span
              v-if="row.orderStatus == '已审核'"
              class="primary-color text-hover ml-10px"
              @click="addForm('edit', row.id)"
              >查看</span
            >
          </template>
        </vxe-column>
        <vxe-column title="状态" field="orderStatus" width="80" />

        <vxe-column
          title="单据日期"
          field="orderDate"
          align="center"
          width="130"
        />
        <vxe-column title="单据编号" field="orderNo" width="200" />
        <!-- <vxe-column title="订单类型" field="orderType" width="200">
          <template #default="{ row }">
            {{ row.orderType == 1 ? '收款单' : '预收款单' }}
          </template>
        </vxe-column> -->

        <vxe-column title="客户" field="customerName" min-width="120" />
        <vxe-column title="结算账户" field="settlementAccount" min-width="120">
        </vxe-column>
        <vxe-column title="欠款金额" field="amount" min-width="120"> </vxe-column>
        <vxe-column title="收款金额" field="collectionAmount" min-width="120">
        </vxe-column>

        <vxe-column title="业务员" field="orderStaffName" width="120" />
        <vxe-column title="审核人" field="approvedName" width="120" />

        <vxe-column
          title="制单人"
          field="createName"
          align="center"
          width="100"
        />
        <vxe-column
          title="制单时间"
          field="createdAt"
          align="center"
          width="100"
        />
        <vxe-column title="备注" field="remarks" align="center" width="100" />
      </vxe-table>
    </div>
    <div class="flex justify-between items-center pt-5px">
      <vxe-pager
        perfect
        @page-change="loadList(false)"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :layouts="[
          'PrevJump',
          'PrevPage',
          'Number',
          'NextPage',
          'NextJump',
          'Sizes',
          'Total'
        ]"
      >
        <template #left>
          <!-- <span class="mr-12px text-16px">总金额：{{ amountTotal }}元</span> -->
          <vxe-button
            @click="loadList(false)"
            type="text"
            size="mini"
            icon="vxe-icon-refresh"
            :loading="loading"
          ></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>
<script>
import manba from 'manba';
import SalesOrder from '@js/api/sales/SalesOrder';
import { mapMutations } from 'vuex';
import { DialogPlugin, LoadingPlugin, MessagePlugin } from 'tdesign-vue-next';
import PurchaseOrder from '@js/api/purchase/PurchaseOrder';
import Customer from '@js/api/basic/Customer';
import Warehouse from '@js/api/basic/Warehouse';
import Product from '@js/api/basic/Product';
import OtherReceipt from '@js/api/fund/OtherReceipt';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-dd');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-dd');

export default {
  name: 'OtherReceiptRecord',
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
        customerId: null
      },
      customerList: [],
      dateRange: {
        start: manba(startTime).format('YYYY-MM-dd'),
        end: manba(endTime).format('YYYY-MM-dd')
      }
    };
  },
  computed: {
    // currentTabParams() {
    //   const current = this.$store.state.tabs.find(
    //     (tab) => tab.key === this.$store.state.currentTab
    //   );
    //   return current ? current.params : {};
    // },
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startTime: this.dateRange.start,
        endTime: this.dateRange.end
      });
    }
  },
  methods: {
    ...mapMutations(['pushTab', 'closeTabKey']),

    addForm(type = 'add', orderId = null) {
      // this.$store.commit('SET_TAB_DATA_RECEIPTRECORD', { type, orderId });
      this.closeTabKey('OtherReceiptList');
      this.pushTab({
        keepAlive: false,
        key: 'OtherReceiptList',
        params: { type: type, orderId: orderId },
        title: '其他收入单'
      });
    },
    loadList(type = true) {
      this.loading = true;
      OtherReceipt.list(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
      Promise.all([Customer.select()])
        .then((results) => {
          this.customerList = results[0].data || [];
        })
        .finally(() => LoadingPlugin(false));
    },
    getCheckboxRecordsIds() {
      return this.$refs.table
        .getCheckboxRecords()
        .map((item) => item.id)
        .join(',');
    },
    doRemove(row = null) {
      let ids = null;
      if (!row) {
        ids = this.getCheckboxRecordsIds();
      } else {
        ids = row.id;
      }
      if (!ids) {
        return MessagePlugin.error('请选择至少一个订单');
      }
      DialogPlugin.confirm({
        title: '系统提示',
        content: `确认删除?`,
        onConfirm: () => {
          OtherReceipt.remove({ id: ids }).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadList();
          });
        }
      });
    },
    batchAudit(orderStatus) {
      const selectedRows = this.getCheckboxRecordsIds();
      console.log(selectedRows, this.$store.state.user.admin.id);
      if (!selectedRows) {
        MessagePlugin.error('请选择至少一个订单');
        return;
      }
      DialogPlugin.confirm({
        content: `确定审核订单？`,
        onConfirm: () => {
          const orderIds = selectedRows;
          let params = {
            id: orderIds,
            orderStatus: orderStatus,
            approvedBy: this.$store.state.user.admin.id
          };
          OtherReceipt.batchAudit(params)
            .then((success) => {
              if (success) {
                if (orderStatus === '已审核') {
                  MessagePlugin.success('审核成功');
                } else {
                  MessagePlugin.success('反审核成功');
                }
                this.loadList(); // Refresh the list
              }
            })
            .finally(() => LoadingPlugin(false));
        }
      });
    },
    /*  //关闭窗口
      closeWindow() {
        console.log('this.$store.state.currentTab', this.$store.state.currentTab);
        //this.$store.commit('closeTabKey', this.$store.state.currentTab);
        this.$store.commit('closeTabKey', this.$store.state.currentTab);
        this.$store.commit('newTab', 'OtherReceiptList');
        // 使用 nextTick 确保在 DOM 更新后执行
        this.$nextTick(() => {
          // 通过 eventBus 或 vuex 触发刷新
          this.$store.commit('SET_TAB_DATA_OUTBOUND', { refresh: true });
        });
      },

      

      footerMethod({ columns, data }) {
        let totalAmount = 0;
        let discountAmount = 0;
        let finalAmount = 0;
        let totalQuantity = 0;
        columns.forEach((column) => {
          if (
            column.property &&
            [
              'totalAmount',
              'discountAmount',
              'finalAmount',
              'totalQuantity'
            ].includes(column.property)
          ) {
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
              } else if (column.property === 'totalQuantity') {
                if (rd) {
                  totalQuantity += Number(rd || 0);
                }
              }
            });
          }
        });
        this.amountTotal = totalAmount.toFixed(2);
        return [
          [
            '',
            '',
            '',
            '',
            '',
            '',
            totalAmount.toFixed(2),
            discountAmount.toFixed(2),
            finalAmount.toFixed(2),
            totalQuantity.toFixed(2)
          ]
        ];
      },
      */
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    }
  },
  created() {
    this.loadList();
  }
};
</script>
<style lang="less" scoped>
:deep(.vxe-button.size--mini.type--button) {
  height: var(--vxe-button-height-mini);
  height: 34px;
}
</style>
