<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="addForm()">新 增</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="batchAudit('已审核')">审 核</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="batchAudit('已保存')">反审核</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="doRemove()">删 除</t-button>
        <t-select
            v-model="params.orderStatus"
            :options="stateOptions"
            clearable
            placeholder="订单状态"
            style="width: 140px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="单据日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-select
            v-model="params.orderType"
            :options="orderTypeOptions"
            clearable
            placeholder="单据类型"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.customerId"
            :options="customerList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="请选择客户"
            style="width: 180px; border-radius: 4px"
        />
        <t-input
            v-model="params.keyword"
            clearable
            placeholder="请输入客户名称或订单编号"
            style="width: 260px; background: #fff; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
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
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :selected-row-keys="selectedRowKeys"
          :foot-data="footData"
          @select-change="onSelectChange"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <template v-if="row.orderStatus != '已审核'">
              <t-link theme="primary" @click="addForm('edit', row.id)">编辑</t-link>
              <t-link theme="danger" @click="doRemove(row)">删除</t-link>
            </template>
            <template v-else>
              <t-link theme="primary" @click="addForm('edit', row.id)">查看</t-link>
            </template>
          </t-space>
        </template>
        <template #orderStatus="{ row }">
          <t-tag
              :theme="row.orderStatus === '已审核' ? 'success' : 'warning'"
              variant="light"
          >
            {{ row.orderStatus === '已保存' ? '未审核' : row.orderStatus }}
          </t-tag>
        </template>
        <template #orderType="{ row }">
          {{ row.orderType == 1 ? '收款单' : '预收款单' }}
        </template>
        <template #salesOrderNo="{ row }">
          <div v-for="item in (row.itemList || [])" :key="item.id" class="cell-multi">
            {{ item.salesOrderNo }}
          </div>
        </template>
        <template #settlementAccount="{ row }">
          <div v-for="item in (row.collectionList || [])" :key="item.id" class="cell-multi">
            {{ item.settlementAccount }}
          </div>
        </template>
        <template #amount="{ row }">
          <div v-for="item in (row.collectionList || [])" :key="item.id" class="cell-multi">
            {{ item.amount }}
          </div>
        </template>
        <template #paymentMethodName="{ row }">
          <div v-for="item in (row.collectionList || [])" :key="item.id" class="cell-multi">
            {{ item.paymentMethodName }}
          </div>
        </template>
        <template #theOnlineTransactionNumber="{ row }">
          <div v-for="item in (row.collectionList || [])" :key="item.id" class="cell-multi">
            {{ item.theOnlineTransactionNumber }}
          </div>
        </template>
        <template #entryRemarks="{ row }">
          <div v-for="item in (row.collectionList || [])" :key="item.id" class="cell-multi">
            {{ item.remarks }}
          </div>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total">合计金额：{{ amountTotal }}元</span>
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
import manba from "manba";
import {mapMutations} from "vuex";
import {DialogPlugin, MessagePlugin} from "tdesign-vue-next";
import Customer from "@js/api/basic/Customer";
import OrderReceipt from "@js/api/fund/OrderReceipt";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

/**
 * @功能描述: 收款单列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "OrderReceiptRecord",
  data() {
    return {
      dataList: [],
      customerList: [],
      selectedRowKeys: [],
      selectedRows: [],
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
        customerId: null,
        orderStatus: null,
        orderType: null,
        keyword: null,
      },
      dateRangeValue: [startTime, endTime],
      stateOptions: [
        {label: '未审核', value: '已保存'},
        {label: '已审核', value: '已审核'},
      ],
      orderTypeOptions: [
        {label: '收款单', value: 1},
        {label: '预收款单', value: 2},
      ],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'ops', title: '操作', width: 110, fixed: 'left', align: 'center'},
        {colKey: 'orderStatus', title: '状态', width: 100, align: 'center'},
        {colKey: 'orderDate', title: '单据日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '单据编号', minWidth: 160, ellipsis: true},
        {colKey: 'orderType', title: '订单类型', width: 110, align: 'center'},
        {colKey: 'salesOrderNo', title: '源单编号', minWidth: 140, ellipsis: true},
        {colKey: 'customerName', title: '客户', minWidth: 120, ellipsis: true},
        {colKey: 'settlementAccount', title: '结算账户', minWidth: 120, ellipsis: true},
        {colKey: 'amount', title: '收款金额', width: 110, align: 'right'},
        {colKey: 'paymentMethodName', title: '收款方式', minWidth: 110, ellipsis: true},
        {colKey: 'theOnlineTransactionNumber', title: '在线交易单号', minWidth: 140, ellipsis: true},
        {colKey: 'entryRemarks', title: '分录备注', minWidth: 100, ellipsis: true},
        {colKey: 'discountAmount', title: '整单折扣', width: 110, align: 'right'},
        {colKey: 'collectionAmount', title: '本次预收款', width: 110, align: 'right'},
        {colKey: 'orderStaffName', title: '业务员', width: 90, align: 'center'},
        {colKey: 'shouldVerificationAmount', title: '本单应核销金额', width: 130, align: 'right'},
        {colKey: 'hasVerificationAmount', title: '本单已核销金额', width: 130, align: 'right'},
        {colKey: 'notVerificationAmount', title: '本单未核销金额', width: 130, align: 'right'},
        {colKey: 'createdBy', title: '制单人', width: 90, align: 'center'},
        {colKey: 'createdAt', title: '制单时间', width: 160, align: 'center', ellipsis: true},
        {colKey: 'remarks', title: '备注', minWidth: 100, ellipsis: true},
      ]
    }
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startTime: start || null,
        endTime: end || null,
      })
    },
    footData() {
      const sum = (key, digits = 2) => {
        const total = (this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
        return total.toFixed(digits);
      };
      return [{
        ops: '合计',
        discountAmount: sum('discountAmount'),
        collectionAmount: sum('collectionAmount'),
        shouldVerificationAmount: sum('shouldVerificationAmount'),
        hasVerificationAmount: sum('hasVerificationAmount'),
        notVerificationAmount: sum('notVerificationAmount'),
      }];
    }
  },
  methods: {
    ...mapMutations(['pushTab', 'closeTabKey']),
    onSelectChange(keys, {selectedRowData}) {
      this.selectedRowKeys = keys;
      this.selectedRows = selectedRowData || [];
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    clearSelection() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    getSelectedIds() {
      return (this.selectedRowKeys || []).join(',');
    },
    addForm(type = 'add', orderId = null) {
      this.closeTabKey('OrderReceiptForm');
      this.pushTab({
        keepAlive: false,
        key: 'OrderReceiptForm',
        params: {type: type, orderId: orderId},
        title: '收款单'
      });
    },
    loadList() {
      this.loading = true;
      OrderReceipt.list(this.queryParams)
          .then(({data: {results, total}}) => {
            this.dataList = results || [];
            this.pagination.total = total;
            let amountTotal = 0;
            this.dataList.forEach(item => {
              amountTotal += Number(item.collectionAmount || 0);
            });
            this.amountTotal = amountTotal.toFixed(2);
          })
          .finally(() => this.loading = false);
    },
    loadCustomer() {
      Customer.select().then(({data}) => {
        this.customerList = data || [];
      });
    },
    doRemove(row = null) {
      let ids = null;
      if (!row) {
        ids = this.getSelectedIds();
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
          OrderReceipt.remove({id: ids}).then(() => {
            MessagePlugin.success('删除成功~');
            this.clearSelection();
            this.loadList();
          });
        }
      });
    },
    batchAudit(orderStatus) {
      const selectedRows = this.getSelectedIds();
      if (!selectedRows) {
        MessagePlugin.error('请选择至少一个订单');
        return;
      }
      DialogPlugin.confirm({
        content: `确定审核订单？`,
        onConfirm: () => {
          const params = {
            id: selectedRows,
            orderStatus: orderStatus,
            approvedBy: this.$store.state.user.admin.id
          };
          OrderReceipt.batchAudit(params)
              .then((success) => {
                if (success) {
                  if (orderStatus === '已审核') {
                    MessagePlugin.success('审核成功');
                  } else {
                    MessagePlugin.success('反审核成功');
                  }
                  this.clearSelection();
                  this.loadList();
                }
              });
        }
      });
    },
    doSearch() {
      this.pagination.page = 1;
      this.clearSelection();
      this.loadList();
    },
  },
  created() {
    this.loadCustomer();
    this.loadList();
  }
}
</script>

<style scoped>
.simple-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.simple-page__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.simple-page__table {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  overflow: hidden;
}

.simple-page__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
  background: #fff;
}

.simple-page__total {
  font-size: 14px;
  color: #333639;
  flex-shrink: 0;
}

.cell-multi {
  line-height: 1.6;
}
</style>
