<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px" style="font-size: 16px !important">单据日期：</label>
          <DatePicker
            class="w-140px"
            v-model="form.orderDate"
            :clearable="false"
            :disabled="isAudited"
          />
        </template>
        <template #tools>
          <Stamp v-if="isAudited" />
        </template>
      </vxe-toolbar>

      <vxe-table
        ref="transferTable"
        size="mini"
        border
        stripe
        show-overflow
        :row-config="{ height: 40 }"
        :edit-config="isAudited ? undefined : editConfig"
        :data="tableData"
        :show-footer="showFooter"
        :footer-method="footerMethod"
      >
        <vxe-column type="seq" title="序号" width="60" align="center" fixed="left" />
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left">
          <template #default="{ rowIndex }">
            <template v-if="!isAudited">
              <div
                class="fa fa-plus text-hover mr-5px"
                @click="adjustRows('insert', rowIndex, tableData)"
              ></div>
              <div
                class="fa fa-minus text-hover"
                v-if="canDelete(tableData)"
                @click="adjustRows('delete', rowIndex, tableData)"
              ></div>
            </template>
          </template>
        </vxe-column>
        <vxe-column field="fromAccountName" title="转出账户" min-width="140" :edit-render="{}">
          <template #header>
            <span style="color: red">*</span>转出账户
          </template>
          <template #default="{ row }">
            <span>{{ row.fromAccountName }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-select
              v-model="row.fromAccountName"
              placeholder="请选择"
              :multiple="false"
              transfer
              @change="changeAccount(row.fromAccountName, 'fromAccountId', row)"
            >
              <vxe-option
                v-for="item in settlementAccount"
                :key="item.id"
                :value="item.name"
                :label="item.name"
              />
            </vxe-select>
          </template>
        </vxe-column>
        <vxe-column field="toAccountName" title="转入账户" min-width="140" :edit-render="{}">
          <template #header>
            <span style="color: red">*</span>转入账户
          </template>
          <template #default="{ row }">
            <span>{{ row.toAccountName }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-select
              v-model="row.toAccountName"
              placeholder="请选择"
              :multiple="false"
              transfer
              @change="changeAccount(row.toAccountName, 'toAccountId', row)"
            >
              <vxe-option
                v-for="item in settlementAccount"
                :key="item.id"
                :value="item.name"
                :label="item.name"
              />
            </vxe-select>
          </template>
        </vxe-column>
        <vxe-column field="amount" title="金额" width="140" :edit-render="{}">
          <template #header>
            <span style="color: red">*</span>金额
          </template>
          <template #default="{ row }">
            <span>{{ row.amount }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-input
              v-model="row.amount"
              type="number"
              placeholder="请输入数值"
              min="0"
              @change="updateFootEvent"
            />
          </template>
        </vxe-column>
        <vxe-column field="paymentMethodName" title="结算方式" min-width="120" :edit-render="{}">
          <template #default="{ row }">
            <span>{{ row.paymentMethodName }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-select
              v-model="row.paymentMethodName"
              placeholder="请选择"
              :multiple="false"
              transfer
              @change="changePaymentMethod(row.paymentMethodName, row)"
            >
              <vxe-option
                v-for="item in paymentMethodList"
                :key="item.id"
                :value="item.name"
                :label="item.name"
              />
            </vxe-select>
          </template>
        </vxe-column>
        <vxe-column
          field="settlementNumber"
          title="结算号"
          min-width="120"
          :edit-render="{ name: 'input' }"
        />
        <vxe-column field="remarks" title="备注" min-width="120" :edit-render="{ name: 'input' }" />
      </vxe-table>

      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <Input
            placeholder="请输入备注"
            maxlength="150"
            v-model="form.remarks"
            :disabled="isAudited"
          />
          <div class="transfer-extra-actions">
            <Button @click="historyForm()">历史单据</Button>
            <Button :title="logContent">操作日志</Button>
          </div>
        </div>
      </div>
    </div>

    <div class="page-column-footer modal-column-between bg-white-color border">
      <Button :loading="loading" @click="closeWindow">取消</Button>
      <div>
        <template v-if="!isAudited">
          <Button color="primary" :loading="loading" @click="saveForm('add')">保存并新增</Button>
          <Button :loading="loading" @click="saveForm('save')">保存</Button>
          <Button
            v-if="form.orderStatus == '已保存'"
            :loading="loading"
            @click="saveForm('audit', '已审核')"
          >审核</Button>
        </template>
        <Button
          v-else
          :loading="loading"
          @click="approved('已保存')"
        >反审核</Button>
      </div>
    </div>
  </div>
</template>
<script>
import manba from 'manba';
import {LoadingPlugin, MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import { openDialog, closeDialog } from '@common/dialog';
import { h } from 'vue';
import AccountTransfer from '@js/api/fund/AccountTransfer';
import Account from '@js/api/fund/Account';
import PaymentMethod from '@js/api/basic/PaymentMethod';
import Customer from '@js/api/basic/Customer';
import OrderStaff from '@js/api/basic/OrderStaff';
import AccountType from '@js/api/basic/AccountType';
import OrderStaffForm from './OrderStaffForm';
import { mapState, mapMutations } from 'vuex';
import Stamp from '../common/Stamp.vue';
export default {
  name: 'AccountTransferForm',
  components: { Stamp },
  props: {
    orderId: [String, Number],
    type: String,
    index: Number
  },
  data() {
    const tableData = [{}];
    const tableData2 = [{}];
    const editConfig = {
      trigger: 'click',
      mode: 'cell'
    };
    const accountOptions = [];

    return {
      logContent: null,
      val1: [],
      form: {
        customerName: null,
        orderStaffName: null,
        orderDate: manba().format('YYYY-MM-DD')
      },
      tableData,
      tableData2,
      customerDataList: [],
      orderStaffList: [],
      paymentMethodList: [],
      accountTypeList: [],
      settlementAccount: [],
      totalTb1: 0,
      totalTb2: 0,
      loading: false,
      amountTotal: 0,
      pagination: {
        page: 1,
        pageSize: 100,
        total: 0
      },
      params: {
        filter: null,
        state: null,
        sortCol: null,
        sort: null
      },
      showFooter: true,
      editConfig,
      accountOptions
    };
  },
  computed: {
    ...mapState(['user']),
    isAudited() {
      return this.form.orderStatus === '已审核';
    },
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      });
    }
  },
  methods: {
    ...mapMutations(['pushTab', 'closeSelfTab']),
    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: 'AccountTransferList',
        title: '资金转账单'
      });
    },
    updateFootEvent() {
      this.$refs.transferTable && this.$refs.transferTable.updateFooter();
    },
    loadAccountType() {
      AccountType.list().then((res) => {
        this.accountTypeList = res.data;
      });
    },
    getLog() {
      let {
        createName = this.user.admin.name,
        updateName,
        createdAt,
        updateAt,
        approvedName,
        approvedAt
      } = this.form;
      const logEntries = [
        `制单人: ${createName}`,
        createdAt ? `制单时间: ${createdAt}` : null,
        updateName ? `最后修改人: ${updateName}` : null,
        updateAt ? `最后修改时间: ${updateAt}` : null,
        approvedName ? `审核人: ${approvedName}` : null,
        approvedAt ? `审核时间: ${approvedAt}` : null
      ].filter((entry) => entry);

      this.logContent = logEntries.join('\n');
    },
    calcCollectionAmount() {
      return (
        (parseFloat(this.totalTb1) || 0) -
        (parseFloat(this.totalTb2) || 0) +
        (parseFloat(this.form.discountRate) || 0)
      ).toFixed(2);
    },
    calcArrearsAmount() {
      return (
        (parseFloat(this.totalTb1) || 0) -
        (parseFloat(this.totalTb2) || 0) -
        (parseFloat(this.form.collectionAmount) || 0)
      ).toFixed(2);
    },
    approved(orderStatus) {
      const isAnti = orderStatus === '已保存';
      DialogPlugin.confirm({
        content: isAnti ? '确定反审核？' : '确定审核？',
        onConfirm: () => {
          AccountTransfer.approved(orderStatus, this.form.id)
            .then((success) => {
              if (success !== false) {
                MessagePlugin.success(isAnti ? '反审核成功' : '审核成功');
                this.loadList();
              }
            })
            .finally(() => LoadingPlugin(false));
        }
      });
    },
    clearData() {
      this.form = {
        orderDate: manba().format('YYYY-MM-DD')
      };
      this.tableData = [{}];
      this.tableData2 = [{}];
    },
    updatePage(type = 'add', orderId = null) {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: 'AccountTransferForm',
        params: { type: type, orderId: orderId },
        title: '资金转账单'
      });
    },
    loadList() {
      this.loading = true;
      AccountTransfer.load(this.orderId )
        .then(({ data: { order, itemList } }) => {
          this.form = order;
          this.tableData = itemList || [];
          this.getLog();
        })
        .finally(() => (this.loading = false));
    },
    addForm() {},
    historyForm() {
      this.pushTab({
        keepAlive: false,
        key: 'AccountTransferList',
        title: '资金转账单'
      });
    },
    saveForm(type = 'add', orderStatus = '已保存') {
      let orderReceipt = {
        createdBy: this.user.admin.id,
        updateBy: this.user.admin.id,
        orderStatus: orderStatus,
        amount: this.totalTb1,
        ...this.form
      };
      if (type === 'audit') {
        orderReceipt.orderStatus = orderStatus;
        orderReceipt.approvedBy = this.user.admin.id;
        orderReceipt.approvedName = this.user.admin.name;
      }
      const filterEmptyObjects = (arr) =>
        arr
          .map(({ _X_ROW_KEY, ...rest }) => rest)
          .filter((row) => Object.keys(row).length);

      let params = {
        order: orderReceipt,
        itemList: filterEmptyObjects(this.tableData)
      };

      if (!this.tableData.length || !this.tableData[0].fromAccountId) {
        return MessagePlugin.error('请选择转出账户');
      } else if (!this.tableData.length || !this.tableData[0].toAccountId) {
        return MessagePlugin.error('请选择转入账户');
      } else if (!this.tableData.length || !this.tableData[0].amount) {
        return MessagePlugin.error('请输入金额');
      }

      this.save(type, params);
    },
    footerMethodFormat({ columns, data }, list, totalName) {
      const footerRow = new Array(columns.length).fill('');
      footerRow[0] = '合计';
      columns.forEach((column, index) => {
        if (list.includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            const value = parseFloat(row[column.property]);
            if (!isNaN(value)) {
              total += value;
            }
          });
          footerRow[index] = total.toFixed(2);
          this[totalName] = total;
        }
      });
      this.form.collectionAmount = this.calcCollectionAmount();
      this.form.arrearsAmount = this.calcArrearsAmount();
      return [footerRow];
    },
    footerMethod({ columns, data }) {
      return this.footerMethodFormat({ columns, data }, ['amount'], 'totalTb1');
    },
    canDelete(tableData) {
      return tableData.length > 1;
    },
    adjustRows(type, index, tableData) {
      if (type === 'insert') {
        tableData.splice(index + 1, 0, {});
      } else if (type === 'delete' && this.canDelete(tableData)) {
        tableData.splice(index, 1);
      }
    },
    save(type, params) {
      this.loading = true;
      AccountTransfer.save(params)
        .then(() => {
          MessagePlugin.success('提交成功~');
          this.clearData();
          if (type == 'save') {
            this.historyForm();
          }
        })
        .finally(() => (this.loading = false));
    },
    loadCustomer() {
      this.loading = true;
      Customer.select()
        .then(({ data }) => {
          this.customerDataList = data || [];
        })
        .finally(() => (this.loading = false));
    },
    loadOrderStaff() {
      OrderStaff.select()
        .then(({ data }) => {
          this.orderStaffList = data || [];
        })
        .finally();
    },
    loadPaymentMethod() {
      this.loading = true;
      PaymentMethod.list()
        .then(({ data }) => {
          this.paymentMethodList = data;
        })
        .finally(() => (this.loading = false));
    },
    loadAccountMethod() {
      this.loading = true;
      Account.list()
        .then(({ data }) => {
          this.settlementAccount = data;
        })
        .finally(() => (this.loading = false));
    },
    selectCustomer(e) {
      this.form.customerId = e?.id || null;
      this.form = {
        ...this.form
      };
    },
    selectAccounter(e) {
      this.form.settlementAccountId = e?.id || null;
      this.form = {
        ...this.form
      };
    },
    selectOrderStaff(e) {
      this.form.orderStaffId = e?.id || null;
    },
    changeAccount(value, keyname, row) {
      const selectedItem = this.settlementAccount.find(
        (item) => item.name === value
      );
      if (selectedItem) {
        row[keyname] = selectedItem.id;
      }
    },
    changePaymentMethod(value, row) {
      const selectedItem = this.paymentMethodList.find(
        (item) => item.name === value
      );
      if (selectedItem) {
        row.paymentMethodId = selectedItem.id;
      }
    },
    addOrderStaff() {
      document.getElementsByClassName('h-dropdown')[0].style.zIndex = 1;
      this.showForm();
    },
    showForm(entity) {
      let type = 0;
      let dialogId = openDialog({
        header: '新增职员',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '600px',
        body: h(OrderStaffForm, {
          entity,
          type,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.loadOrderStaff();
            closeDialog(dialogId);
          }
        })
      });
    }
  },
  created() {
    if (this.orderId) {
      this.loadList();
    }
    this.loadCustomer();
    this.loadOrderStaff();
    this.loadPaymentMethod();
    this.loadAccountMethod();
    this.loadAccountType();
    setTimeout(() => {
      this.getLog();
    }, 500);
  }
};
</script>
<style lang="less" scoped>
:deep(.vxe-select > .vxe-input) {
  width: 100%;
  height: 100%;
}

.transfer-extra-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
</style>
