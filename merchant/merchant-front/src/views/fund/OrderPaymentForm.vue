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
          <label class="mr-20px ml-16px" style="font-size: 16px !important">总欠款：</label>
          <t-input v-model="form.totalAmountsOwed" class="w-120px" disabled />
          <label class="mr-20px ml-16px" style="font-size: 16px !important">付款人：</label>
          <span style="display: inline-flex; align-items: center; z-index: 1">
            <t-select
              ref="selectRef"
              v-model="form.orderStaffName"
              class="w-140px"
              :options="orderStaffList"
              :keys="{ value: 'name', label: 'name' }"
              placeholder="选择付款人"
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
        <div class="form-toolbar__right">
          <Stamp v-if="isAudited" />
        </div>
      </div>

      <t-table
        row-key="_rowKey"
        size="small"
        bordered
        stripe
        hover
        table-layout="fixed"
        :data="tableData"
        :columns="collectionColumns"
        :foot-data="footData"
        :loading="loading"
      >
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
        <template #settlementAccount="{ row }">
          <t-select
            v-if="!isAudited"
            v-model="row.settlementAccount"
            :options="settlementAccount"
            :keys="{ value: 'name', label: 'name' }"
            placeholder="请选择"
            filterable
            clearable
            style="width: 100%"
            @change="(val) => changeAccount(val, row)"
          />
          <span v-else>{{ row.settlementAccount }}</span>
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
        <template #paymentMethodName="{ row }">
          <t-select
            v-if="!isAudited"
            v-model="row.paymentMethodName"
            :options="paymentMethodList"
            :keys="{ value: 'name', label: 'name' }"
            placeholder="请选择"
            filterable
            clearable
            style="width: 100%"
            @change="(val) => changePaymentMethod(val, row)"
          />
          <span v-else>{{ row.paymentMethodName }}</span>
        </template>
        <template #remarks="{ row }">
          <t-input v-if="!isAudited" v-model="row.remarks" />
          <span v-else>{{ row.remarks }}</span>
        </template>

        <template #theOnlineTransactionNumber="{ row }">
          <t-input
            v-if="!isAudited"
            v-model="row.theOnlineTransactionNumber"
            placeholder="请输入"
          />
          <span v-else>{{ row.theOnlineTransactionNumber }}</span>
        </template>
      </t-table>

      <div v-if="!isAudited" class="form-toolbar form-toolbar--end">
        <t-button @click="sourceForm()">选择源单</t-button>
        <t-button @click="autoMatic()">自动核销</t-button>
      </div>

      <t-table
        row-key="_rowKey"
        size="small"
        bordered
        stripe
        hover
        table-layout="fixed"
        :data="tableData2"
        :columns="itemColumns"
        :foot-data="footData2"
        :loading="loading"
      >
        <template #ops="{ rowIndex }">
          <template v-if="!isAudited">
            <div
              class="fa fa-plus text-hover mr-5px"
              @click="adjustRows('insert', rowIndex, tableData2)"
            ></div>
            <div
              class="fa fa-minus text-hover"
              v-if="canDelete(tableData2)"
              @click="adjustRows('delete', rowIndex, tableData2)"
            ></div>
          </template>
        </template>
        <template #currentVerifyAmount="{ row }">
          <t-input-number
            v-if="!isAudited"
            v-model="row.currentVerifyAmount"
            theme="normal"
            :min="0"
            :max="row.documentAmount"
            :decimal-places="2"
            style="width: 100%"
            @change="changeDiscountRate"
          />
          <span v-else>{{ row.currentVerifyAmount }}</span>
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
          <label class="mr-16px w-80px">整单折扣：</label>
          <t-input
            type="number"
            v-model="form.discountRate"
            :disabled="isAudited"
            @blur="changeDiscountRate"
          />
          <label class="ml-10px mr-16px w-100px">本单预付款：</label>
          <t-input disabled v-model="form.collectionAmount" />
          <div class="payment-extra-actions">
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
          <t-button @click="doPrint" :loading="loading">打印</t-button>
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
import {openPrint} from '@common/print';
import {LoadingPlugin, MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import { h } from 'vue';
import OrderPayment from '@js/api/fund/OrderPayment';
import Account from '@js/api/fund/Account';
import PaymentMethod from '@js/api/basic/PaymentMethod';
import Supplier from '@js/api/basic/Supplier';
import OrderStaff from '@js/api/basic/OrderStaff';
import OrderStaffForm from './OrderStaffForm';
import SourceForm from './SourceForm.vue';
import { mapState, mapMutations } from 'vuex';
import Stamp from '../common/Stamp.vue';

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, ...extra };
}

export default {
  name: 'OrderPaymentForm',
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
      settlementAccount: [],
      loading: false,
      amountTotal: 0,
      pagination: {
        page: 1,
        pageSize: 1000,
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
    totalTb1() {
      return (this.tableData || []).reduce((sum, row) => {
        const value = parseFloat(row.amount);
        return sum + (isNaN(value) ? 0 : value);
      }, 0);
    },
    totalTb2() {
      return (this.tableData2 || []).reduce((sum, row) => {
        const value = parseFloat(row.currentVerifyAmount);
        return sum + (isNaN(value) ? 0 : value);
      }, 0);
    },
    footData() {
      return [{
        seq: '合计',
        amount: this.totalTb1.toFixed(2)
      }];
    },
    footData2() {
      const sum = (key) => {
        const total = (this.tableData2 || []).reduce((acc, row) => {
          const value = parseFloat(row[key]);
          return acc + (isNaN(value) ? 0 : value);
        }, 0);
        return total.toFixed(2);
      };
      return [{
        seq: '合计',
        documentAmount: sum('documentAmount'),
        verifiedAmount: sum('verifiedAmount'),
        unverifiedAmount: sum('unverifiedAmount'),
        currentVerifyAmount: sum('currentVerifyAmount')
      }];
    },
    collectionColumns() {
      return [
        {
          colKey: 'seq',
          title: '序号',
          width: 60,
          align: 'center',
          fixed: 'left',
          cell: (h, { rowIndex }) => rowIndex + 1
        },
        { colKey: 'ops', title: '操作', width: 70, align: 'center', fixed: 'left' },
        { colKey: 'settlementAccount', title: '*结算账户', minWidth: 140 },
        { colKey: 'amount', title: '*付款金额', width: 140, align: 'right' },
        { colKey: 'paymentMethodName', title: '结算方式', minWidth: 120 },
        { colKey: 'remarks', title: '备注', minWidth: 120 },
        { colKey: 'theOnlineTransactionNumber', title: '在线交易单号', minWidth: 160 }
      ];
    },
    itemColumns() {
      return [
        {
          colKey: 'seq',
          title: '序号',
          width: 60,
          align: 'center',
          fixed: 'left',
          cell: (h, { rowIndex }) => rowIndex + 1
        },
        { colKey: 'ops', title: '操作', width: 70, align: 'center' },
        { colKey: 'businessNo', title: '源单编号', minWidth: 140 },
        { colKey: 'businessType', title: '业务类别', width: 110, align: 'center' },
        { colKey: 'businessDate', title: '单据日期', width: 120, align: 'center' },
        { colKey: 'documentAmount', title: '单据金额', width: 110, align: 'right' },
        { colKey: 'verifiedAmount', title: '已核销金额', width: 110, align: 'right' },
        { colKey: 'unverifiedAmount', title: '未核销金额', width: 110, align: 'right' },
        { colKey: 'currentVerifyAmount', title: '*本次核销金额', width: 130, align: 'right' },
        { colKey: 'remarks', title: '单据备注', minWidth: 120 }
      ];
    }
  },
  watch: {
    totalTb1() {
      this.syncCollectionAmount();
    },
    totalTb2() {
      this.syncCollectionAmount();
    },
    'form.discountRate'() {
      this.syncCollectionAmount();
    }
  },
  methods: {
    doPrint() {
      const items = (this.tableData || []).filter(r => r && !r.isNew);
      openPrint('付款单', {
        header: {
          ...this.form,
          partner: this.form.supplierName || '',
          amount: this.totalTb1 ?? this.form.collectionAmount ?? this.amountTotal,
        },
        items,
      });
    },

    ...mapMutations(['pushTab', 'closeSelfTab']),
    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: 'OrderPaymentList',
        title: '付款单'
      });
    },
    syncCollectionAmount() {
      this.form.collectionAmount = this.calcCollectionAmount();
    },
    updateFootEvent() {
      this.syncCollectionAmount();
    },
    getLog() {
      let {
        createName = this.user.admin.name,
        updateName,
        createdAt,
        updatedAt,
        approvedName
      } = this.form;
      const logEntries = [
        `制单人: ${createName}`,
        createdAt ? `制单时间: ${createdAt}` : null,
        updateName ? `最后修改人: ${updateName}` : null,
        updatedAt ? `最后修改时间: ${updatedAt}` : null,
        approvedName ? `审核人: ${approvedName}` : null
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
    approved(orderStatus) {
      const isAnti = orderStatus === '已保存';
      DialogPlugin.confirm({
        body: isAnti ? '确定反审核？' : '确定审核？',
        onConfirm: () => {
          OrderPayment.approved(orderStatus, this.form.id)
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
    },
    updatePage(type = 'add', orderId = null) {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: 'OrderPaymentForm',
        params: { type: type, orderId: orderId },
        title: '付款单'
      });
    },
    loadList() {
      this.loading = true;
      OrderPayment.load(this.orderId )
        .then(({ data: { orderPayment, collectionList, itemList } }) => {
          this.form = orderPayment;
          this.tableData = (collectionList || []).map((item) => newRow(item));
          this.tableData2 = (itemList || []).map((item) => newRow(item));
          this.getLog();
          this.syncCollectionAmount();
        })
        .finally(() => (this.loading = false));
    },
    addForm() {},
    historyForm() {
      this.pushTab({
        keepAlive: false,
        key: 'OrderPaymentList',
        title: '付款单'
      });
    },
    saveForm(type = 'add', orderStatus = '已保存') {
      let orderPayment = {
        documentSource: 1,
        createdBy: this.user.admin.id,
        updatedBy: this.user.admin.id,
        orderStatus: orderStatus,
        ...this.form
      };
      if (type === 'audit') {
        orderPayment.orderStatus = orderStatus;
        orderPayment.approvedBy = this.user.admin.id;
        orderPayment.approvedName = this.user.admin.name;
      }
      const filterEmptyObjects = (arr) =>
        arr
          .map(({ _rowKey, salesOrderId, salesOrderNo, ...rest }) => rest)
          .filter((row) => Object.keys(row).length);

      let params = {
        orderPayment,
        collectionList: filterEmptyObjects(this.tableData),
        itemList: filterEmptyObjects(this.tableData2)
      };

      if (!this.form.supplierId) {
        return MessagePlugin.error('请选择供应商');
      } else if (
        !this.tableData.length ||
        !this.tableData[0].settlementAccountId
      ) {
        return MessagePlugin.error('请选择结算账户');
      } else if (!this.tableData.length || !this.tableData[0].amount) {
        return MessagePlugin.error('请输入金额');
      }

      this.save(type, params);
    },
    changeDiscountRate() {
      this.syncCollectionAmount();
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
    },
    doSearch() {
      this.pagination.page = 1;
    },
    save(type, params) {
      this.loading = true;
      OrderPayment.save(params)
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
      this.form.totalAmountsOwed = e?.balance || null;
      this.form = {
        ...this.form
      };
    },
    selectOrderStaff(value) {
      const e = this.orderStaffList.find((item) => item.name === value);
      this.form.orderStaffId = e?.id || null;
    },
    changeAccount(value, row) {
      const selectedItem = this.settlementAccount.find(
        (item) => item.name === value
      );
      if (selectedItem) {
        row.settlementAccountId = selectedItem.id;
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
    },
    autoMatic() {
      if (!this.form.supplierId) {
        return MessagePlugin.error('请选择供应商');
      }
      const firstRow = this.tableData2[0] || {};
      if (!firstRow.businessNo && !firstRow.salesOrderNo) {
        return MessagePlugin.error('请选择需要核销的单据');
      }
      this.autoSetVerifyAmount();
    },
    autoSetVerifyAmount() {
      let remainingAmount = this.totalTb1;
      this.tableData2.forEach((row) => {
        const { unverifiedAmount = 0 } = row;
        if (remainingAmount >= unverifiedAmount) {
          row.currentVerifyAmount = unverifiedAmount;
          remainingAmount -= unverifiedAmount;
        } else if (remainingAmount > 0) {
          row.currentVerifyAmount = remainingAmount;
          remainingAmount = 0;
        } else {
          row.currentVerifyAmount = 0;
        }
      });
      MessagePlugin.success('已核销');
      this.syncCollectionAmount();
    },
    sourceForm() {
      if (!this.form.supplierId) {
        return MessagePlugin.error('请选择供应商');
      }
      let params = {
        supplierId: this.form.supplierId,
        type: 2,
        balance: this.form.totalAmountsOwed
      };
      let dialogId = openDialog({
        header: '选择源单',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '900px',
        body: h(SourceForm, {
          params,
          URL: 'Settlement',
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: (checkList) => {
            const mappedList = checkList.map(item => ({
              ...item,
              businessNo: item.businessNo,
              businessId: item.businessId,
            }));
            const rowKey = (item) => item.businessNo || item.salesOrderNo;
            const merged = new Map(
              this.tableData2.map((item) => [rowKey(item), item])
            );
            mappedList.forEach((item) => {
              const key = rowKey(item);
              if (key && !merged.has(key)) {
                merged.set(key, item);
              }
            });
            this.tableData2 = Array.from(merged.values())
              .filter((item) => rowKey(item))
              .map((item) => {
                return newRow({
                  ...item,
                  businessNo: item.businessNo || item.salesOrderNo,
                  businessId: item.businessId || item.salesOrderId,
                  currentVerifyAmount:
                    item.currentVerifyAmount ?? item.unverifiedAmount
                });
              });
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
    setTimeout(() => {
      this.getLog();
    }, 500);
  }
};
</script>
<style lang="less" scoped>

.form-toolbar--end {
  justify-content: flex-end;
  margin: 8px 0;
}

.payment-extra-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
</style>
