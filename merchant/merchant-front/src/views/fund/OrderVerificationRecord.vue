<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="addForm()" color="primary">新 增</Button>
        <Button @click="doRemove()"> 删 除</Button>
        <Button @click="batchAudit('已审核')"> 审 核</Button>
        <Button @click="batchAudit('已保存')"> 反审核</Button>
        <!-- <Button @click="doRemove()"> 删 除</Button> -->
      </template>
      <template #tools>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px"> 业务类型</span>

          <div style="position: relative">
            <Select
              v-model="params.orderType"
              class="w-120px z-index-1"
              :datas="businessTypeList"
              keyName="type"
              titleName="name"
              placeholder="选择业务类型"
            >
            </Select>
          </div>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">日期：</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
        <Search
          v-model.trim="params.orderNo"
          search-button-theme="h-btn-default"
          show-search-button
          class="w-280px ml-8px"
          placeholder="请输入单据号"
          @search="doSearch"
        >
          <i class="h-icon-search" />
        </Search>
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
        <vxe-column
          title="单据日期"
          field="orderDate"
          align="center"
          width="130"
        />
        <vxe-column title="单据编号" field="orderNo" width="200" />
        <vxe-column title="业务类型" field="type" width="200">
          <template #default="{ row }">
            {{ row.type == "1" ? "预收冲应收" : "预付冲应付" }}
          </template>
        </vxe-column>
        <vxe-column title="客户1/供应商1" field="personnelName" min-width="120" />
        <vxe-column title="业务员" field="orderStaffName" min-width="120">
        </vxe-column>
        <!-- <vxe-column title="核销金额" field="verifiedAmount" min-width="120">
        </vxe-column> -->
        <vxe-column title="备注" min-width="120" field="remarks">
        </vxe-column>
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
          'Total',
        ]"
      >
        <template #left>
          <span class="mr-12px text-16px">总金额：{{ amountTotal }}元</span>
          <vxe-button
            @click="loadList(false)"
            type="text"
            size="mini"
            icon="h-icon-refresh"
            :loading="loading"
          ></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>
<script>
import manba from "manba";
import SalesOrder from "@js/api/sales/SalesOrder";
import { mapMutations } from "vuex";
import { confirm, loading, message } from "heyui.ext";
import Verification from "@js/api/fund/Verification";
import PurchaseOrder from "@js/api/purchase/PurchaseOrder";
import Customer from "@js/api/basic/Customer";
import Warehouse from "@js/api/basic/Warehouse";
import Product from "@js/api/basic/Product";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "OrderVerificationRecord",
  data() {
    return {
      businessTypeList: [
        {
          name: "预收冲应收",
          type: "1",
        },
        {
          name: "预付冲应付",
          type: "2",
        },
      ],
      dataList: [],
      loading: false,
      amountTotal: 0,
      totalParams: {},
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0,
      },
      params: {
        orderNo: null,
        orderType: null,
      },
      customerList: [],
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd"),
      },
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
        endTime: this.dateRange.end,
      });
    },
  },
  methods: {
    ...mapMutations(["pushTab", "closeTabKey"]),

    addForm(type = "add", orderId = null) {
      // this.$store.commit('SET_TAB_DATA_RECEIPTRECORD', { type, orderId });
      this.closeTabKey("VerificationList");
      this.pushTab({
        keepAlive: false,
        key: "VerificationList",
        params: { type: type, orderId: orderId },
        title: "核销单",
      });
    },
    loadList(type = true) {
      this.loading = true;
      Verification.list(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    getCheckboxRecordsIds() {
      return this.$refs.table
        .getCheckboxRecords()
        .map((item) => item.id)
        .join(",");
    },
    doRemove(row = null) {
      let ids = null;
      if (!row) {
        ids = this.getCheckboxRecordsIds();
      } else {
        ids = row.id;
      }
      if (!ids) {
        return message.error('请选择至少一个订单');
      }
      confirm({
        title: "系统提示",
        content: `确认删除?`,
        onConfirm: () => {
          Verification.remove({ id: ids }).then(() => {
            message("删除成功~");
            this.loadList();
          });
        },
      });
    },
    batchAudit(orderStatus) {
      const selectedRows = this.getCheckboxRecordsIds();
      console.log(selectedRows, this.$store.state.user.admin.id);
      if (!selectedRows) {
        message.error("请选择至少一个单据进行审核");
        return;
      }
      confirm({
        content: `确定批量审核单据？`,
        onConfirm: () => {
          const orderIds = selectedRows;
          let params = {
            id: orderIds,
            orderStatus: orderStatus,
            approvedBy: this.$store.state.user.admin.id,
          };
          Verification.batchAudit(params)
            .then((success) => {
              if (success) {
                if (orderStatus === "已审核") {
                  message.success("批量审核成功");
                } else {
                  message.success("批量反审核成功");
                }
                this.loadList(); // Refresh the list
              }
            })
            .finally(() => loading.close());
        },
      });
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
  },
  created() {
    this.loadList();
  },
};
</script>
