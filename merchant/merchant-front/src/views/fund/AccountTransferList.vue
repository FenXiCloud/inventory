<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="addForm()">新 增</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="approved()">审 核</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="backApproved()">反审核</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="doRemove()">删 除</t-button>
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="单据日期"
            style="width: 260px; border-radius: 4px"
        />
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
        <template #fromAccountName="{ row }">
          <div v-for="item in (row.itemList || [])" :key="item.id" class="cell-multi">
            {{ item.fromAccountName }}
          </div>
        </template>
        <template #toAccountName="{ row }">
          <div v-for="item in (row.itemList || [])" :key="item.id" class="cell-multi">
            {{ item.toAccountName }}
          </div>
        </template>
        <template #itemAmount="{ row }">
          <div v-for="item in (row.itemList || [])" :key="item.id" class="cell-multi">
            {{ item.amount }}
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
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import Customer from "@js/api/basic/Customer";
import AccountTransfer from "@js/api/fund/AccountTransfer";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

/**
 * @功能描述: 资金转账单列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AccountTransferList",
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
        customerId: null
      },
      dateRangeValue: [startTime, endTime],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'ops', title: '操作', width: 110, fixed: 'left', align: 'center'},
        {colKey: 'orderStatus', title: '状态', width: 100, align: 'center'},
        {colKey: 'orderDate', title: '单据日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '单据编号', minWidth: 160, ellipsis: true},
        {colKey: 'fromAccountName', title: '转出账户', minWidth: 120, ellipsis: true},
        {colKey: 'toAccountName', title: '转入账户', minWidth: 120, ellipsis: true},
        {colKey: 'itemAmount', title: '金额', width: 110, align: 'right'},
        {colKey: 'amount', title: '合计金额', width: 110, align: 'right'},
        {colKey: 'remarks', title: '备注', minWidth: 120, ellipsis: true},
        {colKey: 'approvedName', title: '审核人', width: 90, align: 'center'},
        {colKey: 'createName', title: '制单人', width: 90, align: 'center'},
        {colKey: 'createdAt', title: '制单时间', width: 160, align: 'center', ellipsis: true},
      ]
    };
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startTime: start || null,
        endTime: end || null,
      });
    },
    footData() {
      const sum = (key, digits = 2) => {
        const total = (this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
        return total.toFixed(digits);
      };
      return [{
        ops: '合计',
        amount: sum('amount'),
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
      this.closeTabKey('AccountTransferForm');
      this.pushTab({
        keepAlive: false,
        key: 'AccountTransferForm',
        params: {type: type, orderId: orderId},
        title: '资金转账单'
      });
    },
    loadTotal() {
      AccountTransfer.total(this.queryParams).then(({data}) => {
        this.amountTotal = data || 0;
      })
    },
    loadList() {
      this.loading = true;
      AccountTransfer.list(this.queryParams)
          .then(({data: {results, total}}) => {
            this.dataList = results || [];
            this.pagination.total = total;
          })
          .finally(() => (this.loading = false));
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
        header: '系统提示',
        body: `确认删除?`,
        onConfirm: () => {
          AccountTransfer.remove({id: ids}).then(() => {
            MessagePlugin.success('删除成功~');
            this.clearSelection();
            this.loadList();
          });
        }
      });
    },
    approved() {
      const selectedRows = this.getSelectedIds();
      if (!selectedRows) {
        MessagePlugin.error('请选择至少一个订单');
        return;
      }
      DialogPlugin.confirm({
        body: `确定审核订单？`,
        onConfirm: () => {
          AccountTransfer.approved('已审核', selectedRows).then((success) => {
            if (success !== false) {
              MessagePlugin.success('审核成功');
              this.clearSelection();
              this.loadList();
            }
          });
        }
      });
    },
    backApproved() {
      const selectedRows = this.getSelectedIds();
      if (!selectedRows) {
        MessagePlugin.error('请选择至少一个订单');
        return;
      }
      DialogPlugin.confirm({
        body: `确定反审核订单？`,
        onConfirm: () => {
          AccountTransfer.approved('已保存', selectedRows).then((success) => {
            if (success !== false) {
              MessagePlugin.success('反审核成功');
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
      this.loadTotal();
    },
  },
  created() {
    this.loadCustomer();
    this.loadTotal();
    this.loadList();
  }
};
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
