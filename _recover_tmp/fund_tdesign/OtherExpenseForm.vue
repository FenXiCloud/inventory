<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div class="form-toolbar">
        <div class="form-toolbar__left">
          <label class="mr-20px" style="font-size: 16px !important">
            <span style="color: red">*</span>供应商：
          </label>
          <t-select
            v-model="form.supplierName"
            class="w-200px z-index-1"
            :options="SupplierDataList"
            :keys="{ value: 'name', label: 'name' }"
            placeholder="请选择供应商"
            filterable
            clearable
            :disabled="isAudited"
            @change="selectSupplier"
          />
          <label class="mr-20px ml-16px" style="font-size: 16px !important">业务员：</label>
          <span style="display: inline-flex; align-items: center; z-index: 1">
            <t-select
              ref="selectRef"
              v-model="form.orderStaffName"
              class="w-140px"
              :options="orderStaffList"
              :keys="{ value: 'name', label: 'name' }"
              placeholder="选择业务员"
              filterable
              clearable
              :disabled="isAudited"
              @change="selectOrderStaff"
            />
            <t-button v-if="!isAudited" variant="text" @click="addOrderStaff()">新建</t-button>
          </span>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <t-date-picker
            class="w-140px"
            v-model="form.orderDate"
            :clearable="false"
            :disabled="isAudited"
          />
        </div>
        <Stamp v-if="isAudited" />
      </div>

      <t-table
        ref="collectionTable"
        row-key="_rowKey"
        size="small"
        bordered
        stripe
        hover
        table-layout="fixed"
        :columns="columns"
        :data="tableData"
        :foot-data="footData"
        :loading="loading"
      >
        <template #title-accountTypeName>
          <span style="color: red">*</span>支出类别
        </template>
        <template #title-amount>
          <span style="color: red">*</span>金额
        </template>
        <template #ops="{ rowIndex }">
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
        <template #accountTypeName="{ row }">
          <t-select
            v-if="!isAudited"
            v-model="row.accountTypeName"
            placeholder="请选择"
            :options="accountTypeList"
            :keys="{ value: 'name', label: 'name' }"
            filterable
            clearable
            @change="changeAccount(row.accountTypeName, row)"
          />
          <span v-else>{{ row.accountTypeName }}</span>
        </template>
        <template #amount="{ row }">
          <t-input-number
            v-if="!isAudited"
            v-model="row.amount"
            theme="normal"
            :min="0"
            :decimal-places="2"
            placeholder="请输入数值"
            style="width: 100%"
            @change="updateFootEvent"
          />
          <span v-else>{{ row.amount }}</span>
        </template>
        <template #remarks="{ row }">
          <t-input
            v-if="!isAudited"
            v-model="row.remarks"
            placeholder="请输入"
          />
          <span v-else>{{ row.remarks }}</span>
        </template>
      </t-table>

      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <t-input
            placeholder="请输入备注"
            :maxlength="150"
            v-model="form.remarks"
            :disabled="isAudited"
          />
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">
            <span style="color: red">*</span>结算账户：
          </label>
          <t-select
            v-model="form.settlementAccount"
            class="w-160px"
            :options="settlementAccount"
            :keys="{ value: 'name', label: 'name' }"
            placeholder="选择结算账户"
            filterable
            clearable
            :disabled="isAudited"
            @change="selectAccounter"
          />
          <label class="ml-10px mr-16px w-90px">付款金额：</label>
          <t-input
            v-model="form.collectionAmount"
            :disabled="isAudited"
            @change="form.arrearsAmount = calcArrearsAmount()"
          />
          <label class="ml-10px mr-16px w-90px">本次欠款：</label>
          <t-input disabled v-model="form.arrearsAmount" />
          <div class="expense-extra-actions">
            <t-button @click="historyForm()">历史单据</t-button>
            <t-button :title="logContent">操作日志</t-button>
          </div>
        </div>
      </div>
    </div>

    <div class="page-column-footer modal-column-between bg-white-color border">
      <t-button :loading="loading" @click="closeWindow">取消</t-button>
      <div>
        <template v-if="!isAudited">
          <t-button theme="primary" :loading="loading" @click="saveForm('add')">保存并新增</t-button>
          <t-button :loading="loading" @click="saveForm('save')">保存</t-button>
          <t-button
            v-if="form.orderStatus == '已保存'"
            :loading="loading"
            @click="saveForm('audit', '已审核')"
          >审核</t-button>
        </template>
        <t-button
          v-else
          :loading="loading"
          @click="approved('已保存')"
        >反审核</t-button>
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
import OtherExpense from '@js/api/fund/OtherExpense';
import Account from '@js/api/fund/Account';
import PaymentMethod from '@js/api/basic/PaymentMethod';
import Supplier from '@js/api/basic/Supplier';
import OrderStaff from '@js/api/basic/OrderStaff';
import AccountType from '@js/api/basic/AccountType';
import OrderStaffForm from './OrderStaffForm';
import { mapState, mapMutations } from 'vuex';
import Stamp from '../common/Stamp.vue';

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, ...extra };
}

export default {
  name: 'OtherExpenseForm',
  components: { Stamp },
  props: {
    orderId: [String, Number],
    type: String,
    index: Number
  },
  data() {
    return {
      logContent: null,
      val1: [],
      form: {
        supplierName: null,
        orderStaffName: null,
        orderDate: manba().format('YYYY-MM-DD')
      },
      tableData: [newRow()],
      tableData2: [newRow()],
      SupplierDataList: [],
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
      accountOptions: []
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
    },
    columns() {
      return [
        {
          colKey: 'seq',
          title: '序号',
          width: 60,
          align: 'center',
          fixed: 'left',
          cell: (h, { rowIndex }) => rowIndex + 1,
          foot: () => '合计'
        },
        {
          colKey: 'ops',
          title: '操作',
          width: 70,
          align: 'center',
          fixed: 'left'
        },
        { colKey: 'accountTypeName', title: '支出类别', minWidth: 140 },
        { colKey: 'amount', title: '金额', width: 140 },
        { colKey: 'remarks', title: '备注', minWidth: 120 }
      ];
    },
    footData() {
      let total = 0;
      (this.tableData || []).forEach((row) => {
        const value = parseFloat(row.amount);
        if (!isNaN(value)) {
          total += value;
        }
      });
      return [{ seq: '合计', amount: total.toFixed(2) }];
    }
  },
  methods: {
    ...mapMutations(['pushTab', 'closeSelfTab']),
    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: 'OtherExpenseList',
        title: '其他支出单'
      });
    },
    updateFootEvent() {
      let total = 0;
      (this.tableData || []).forEach((row) => {
        const value = parseFloat(row.amount);
        if (!isNaN(value)) {
          total += value;
        }
      });
      this.totalTb1 = total;
      this.form.collectionAmount = this.calcCollectionAmount();
      this.form.arrearsAmount = this.calcArrearsAmount();
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
        body: isAnti ? '确定反审核？' : '确定审核？',
        onConfirm: () => {
          OtherExpense.approved(orderStatus, this.form.id)
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
      this.form = {};
      this.tableData = [newRow()];
      this.tableData2 = [newRow()];
      this.totalTb1 = 0;
    },
    updatePage(type = 'add', orderId = null) {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: 'OtherExpenseForm',
        params: { type: type, orderId: orderId },
        title: '其他支出单'
      });
    },
    loadList() {
      this.loading = true;
      OtherExpense.load(this.orderId )
        .then(({ data: { order, itemList } }) => {
          this.form = order;
          this.tableData = (itemList || []).map((row) => newRow(row));
          this.updateFootEvent();
          this.getLog();
        })
        .finally(() => (this.loading = false));
    },
    addForm() {},
    historyForm() {
      this.pushTab({
        keepAlive: false,
        key: 'OtherExpenseList',
        title: '其他支出单'
      });
    },
    saveForm(type = 'add', orderStatus = '已保存') {
      this.updateFootEvent();
      let orderReceipt = {
        createdBy: this.user.admin.id,
        updateBy: this.user.admin.id,
        orderStatus: orderStatus,
        ...this.form
      };
      if (type === 'audit') {
        orderReceipt.orderStatus = orderStatus;
        orderReceipt.approvedBy = this.user.admin.id;
        orderReceipt.approvedName = this.user.admin.name;
      }
      const filterEmptyObjects = (arr) =>
        arr
          .map(({ _X_ROW_KEY, _rowKey, ...rest }) => rest)
          .filter((row) => Object.keys(row).length);

      let params = {
        order: orderReceipt,
        itemList: filterEmptyObjects(this.tableData)
      };

      if (!this.form.supplierId) {
        return MessagePlugin.error('请选择供应商');
      } else if (!this.form.settlementAccountId) {
        return MessagePlugin.error('请选择结算账户');
      } else if (!this.tableData.length || !this.tableData[0].accountTypeId) {
        return MessagePlugin.error('请选择支出类别');
      } else if (!this.tableData.length || !this.tableData[0].amount) {
        return MessagePlugin.error('请输入金额');
      }

      this.save(type, params);
    },
    canDelete(tableData) {
      return tableData.length > 1;
    },
    adjustRows(type, index, tableData) {
      if (type === 'insert') {
        tableData.splice(index + 1, 0, newRow());
      } else if (type === 'delete' && this.canDelete(tableData)) {
        tableData.splice(index, 1);
      }
      this.updateFootEvent();
    },
    save(type, params) {
      this.loading = true;
      OtherExpense.save(params)
        .then(() => {
          MessagePlugin.success('提交成功~');
          this.clearData();
          if (type == 'save') {
            this.historyForm();
          }
        })
        .finally(() => (this.loading = false));
    },
    loadSupplier() {
      this.loading = true;
      Supplier.select()
        .then(({ data }) => {
          this.SupplierDataList = data || [];
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
    selectSupplier(value) {
      const e = this.SupplierDataList.find((item) => item.name === value);
      this.form.supplierId = e?.id || null;
      this.form = {
        ...this.form
      };
    },
    selectAccounter(value) {
      const e = this.settlementAccount.find((item) => item.name === value);
      this.form.settlementAccountId = e?.id || null;
      this.form = {
        ...this.form
      };
    },
    selectOrderStaff(value) {
      const e = this.orderStaffList.find((item) => item.name === value);
      this.form.orderStaffId = e?.id || null;
    },
    changeAccount(value, row) {
      const selectedItem = this.accountTypeList.find(
        (item) => item.name === value
      );
      if (selectedItem) {
        row.accountTypeId = selectedItem.id;
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
    this.loadSupplier();
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
.form-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  min-height: 40px;
}

.form-toolbar__left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.expense-extra-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
</style>
