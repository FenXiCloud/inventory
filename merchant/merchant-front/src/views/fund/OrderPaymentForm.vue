<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
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
        </template>
        <template #tools>
          <Stamp v-if="isAudited" />
        </template>
      </vxe-toolbar>

      <vxe-table
        ref="collectionTable"
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
        <vxe-column field="settlementAccount" title="结算账户" min-width="140" :edit-render="{}">
          <template #header>
            <span style="color: red">*</span>结算账户
          </template>
          <template #default="{ row }">
            <span>{{ row.settlementAccount }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-select
              v-model="row.settlementAccount"
              placeholder="请选择"
              :multiple="false"
              transfer
              @change="changeAccount(row.settlementAccount, row)"
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
        <vxe-column field="amount" title="付款金额" width="140" :edit-render="{}">
          <template #header>
            <span style="color: red">*</span>付款金额
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
        <vxe-column field="remarks" title="备注" min-width="120" :edit-render="{ name: 'input' }" />
        <vxe-column field="theOnlineTransactionNumber" title="在线交易单号" min-width="160" />
      </vxe-table>

      <vxe-toolbar>
        <template v-if="!isAudited" #tools>
          <t-button @click="sourceForm('OrderPayment')">选择源单</t-button>
          <t-button @click="autoMatic()">自动核销</t-button>
        </template>
      </vxe-toolbar>

      <vxe-table
        ref="table"
        size="mini"
        border
        stripe
        show-overflow
        :row-config="{ height: 40 }"
        :edit-config="isAudited ? undefined : editConfig"
        :data="tableData2"
        :show-footer="showFooter"
        :footer-method="footerMethod2"
      >
        <vxe-column type="seq" title="序号" width="60" align="center" fixed="left" />
        <vxe-column title="操作" field="seq" width="70" align="center">
          <template #default="{ rowIndex }">
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
        </vxe-column>
        <vxe-column field="businessNo" title="源单编号" min-width="140" />
        <vxe-column field="businessType" title="业务类别" width="110" align="center" />
        <vxe-column field="businessDate" title="单据日期" width="120" align="center" />
        <vxe-column field="documentAmount" title="单据金额" width="110" align="right" />
        <vxe-column field="verifiedAmount" title="已核销金额" width="110" align="right" />
        <vxe-column field="unverifiedAmount" title="未核销金额" width="110" align="right" />
        <vxe-column field="currentVerifyAmount" title="本次核销金额" width="130" :edit-render="{}">
          <template #header>
            <span style="color: red">*</span>本次核销金额
          </template>
          <template #default="{ row }">
            <span>{{ row.currentVerifyAmount }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-input
              v-model="row.currentVerifyAmount"
              type="number"
              :max="row.documentAmount"
              min="0"
              @change="changeDiscountRate"
            />
          </template>
        </vxe-column>
        <vxe-column field="remarks" title="单据备注" min-width="120" />
      </vxe-table>

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
export default {
  name: 'OrderPaymentForm',
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
        supplierName: null,
        orderStaffName: null,
        orderDate: manba().format('YYYY-MM-DD')
      },
      tableData,
      tableData2,
      SupplierDataList: [],
      orderStaffList: [],
      paymentMethodList: [],
      settlementAccount: [],
      totalTb1: 0,
      totalTb2: 0,
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
    updateFootEvent() {
      this.$refs.collectionTable && this.$refs.collectionTable.updateFooter();
      this.form.collectionAmount = this.calcCollectionAmount();
    },
    getLog() {
      let {
        createName = this.user.admin.name,
        updateName,
        createdAt,
        updateAt,
        approvedName
      } = this.form;
      const logEntries = [
        `制单人: ${createName}`,
        createdAt ? `制单时间: ${createdAt}` : null,
        updateName ? `最后修改人: ${updateName}` : null,
        updateAt ? `最后修改时间: ${updateAt}` : null,
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
      this.tableData = [{}];
      this.tableData2 = [{}];
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
          this.tableData = collectionList || [];
          this.tableData2 = itemList || [];
          this.getLog();
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
        updateBy: this.user.admin.id,
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
          .map(({ _X_ROW_KEY, salesOrderId, salesOrderNo, ...rest }) => rest)
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
      return [footerRow];
    },
    changeDiscountRate() {
      this.$refs.table && this.$refs.table.updateFooter();
      this.form.collectionAmount = this.calcCollectionAmount();
    },
    footerMethod({ columns, data }) {
      return this.footerMethodFormat({ columns, data }, ['amount'], 'totalTb1');
    },
    footerMethod2({ columns, data }) {
      return this.footerMethodFormat(
        { columns, data },
        [
          'documentAmount',
          'verifiedAmount',
          'unverifiedAmount',
          'currentVerifyAmount'
        ],
        'totalTb2'
      );
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
      this.$refs.table.updateFooter();
    },
    sourceForm(URL) {
      if (!this.form.supplierId) {
        return MessagePlugin.error('请选择供应商');
      }
      let params = {
        supplierId: this.form.supplierId,
        balance: this.form.totalAmountsOwed
      };
      let dialogId = openDialog({
        header: '选择源单',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '900px',
        body: h(SourceForm, {
          params,
          URL,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: (checkList) => {
            const rowKey = (item) => item.businessNo || item.salesOrderNo;
            const merged = new Map(
              this.tableData2.map((item) => [rowKey(item), item])
            );
            checkList.forEach((item) => {
              const key = rowKey(item);
              if (key && !merged.has(key)) {
                merged.set(key, item);
              }
            });
            this.tableData2 = Array.from(merged.values())
              .filter((item) => rowKey(item))
              .map((item) => {
                delete item._X_ROW_KEY;
                return {
                  ...item,
                  businessNo: item.businessNo || item.salesOrderNo,
                  businessId: item.businessId || item.salesOrderId,
                  currentVerifyAmount:
                    item.currentVerifyAmount ?? item.unverifiedAmount
                };
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
:deep(.vxe-select > .vxe-input) {
  width: 100%;
  height: 100%;
}

.payment-extra-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
</style>
