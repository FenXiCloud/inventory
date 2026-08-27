<template>
  <div class="quick-payment-dialog">
    <t-form label-width="70px" label-align="left" size="small">
      <t-form-item :label="type === 'receipt' ? '客户' : '供货商'">
        <div class="jxc-flex" style="align-items: center; gap: 8px;">
          <t-input :model-value="order.customerName || order.supplierName" disabled style="width: 200px;" />
          <span style="color: #e37318; font-size: 12px;">
            未结算金额：{{ unverifiedAmount }}
          </span>
        </div>
      </t-form-item>

      <t-row :gutter="16">
        <t-col :span="8">
          <t-form-item label="结算账户" required>
            <t-select
              v-model="form.settlementAccountId"
              :options="accountList"
              :keys="{ value: 'id', label: 'name' }"
              placeholder="请选择"
              filterable
              clearable
              size="small"
              @change="onAccountChange"
            />
          </t-form-item>
        </t-col>
        <t-col :span="8">
          <t-form-item label="结算金额" required>
            <t-input-number
              v-model="form.amount"
              theme="normal"
              :min="0"
              :decimal-places="2"
              size="small"
              style="width: 100%"
            />
          </t-form-item>
        </t-col>
        <t-col :span="8">
          <t-form-item label="优惠金额">
            <t-input-number
              v-model="form.discountAmount"
              theme="normal"
              :min="0"
              :decimal-places="2"
              size="small"
              style="width: 100%"
            />
          </t-form-item>
        </t-col>
      </t-row>

      <t-form-item label="备注">
        <t-textarea
          v-model="form.remarks"
          :maxlength="200"
          size="small"
          placeholder="请输入备注"
          style="width: 100%"
        />
      </t-form-item>
    </t-form>

    <div class="quick-payment-dialog__footer">
      <t-button theme="primary" :loading="loading" @click="handleSave">保存</t-button>
      <t-button @click="handleCancel">取消</t-button>
    </div>
  </div>
</template>

<script>
import {mapState} from 'vuex';
import {MessagePlugin} from 'tdesign-vue-next';
import Account from '@js/api/fund/Account';
import OrderReceipt from '@js/api/fund/OrderReceipt';
import OrderPayment from '@js/api/fund/OrderPayment';
import manba from 'manba';

export default {
  name: 'QuickPaymentDialog',
  props: {
    // 'receipt' = 便捷收款, 'payment' = 便捷付款
    type: {
      type: String,
      required: true,
      validator: v => ['receipt', 'payment'].includes(v)
    },
    // 当前单据对象
    order: {
      type: Object,
      required: true
    },
    // 关闭回调
    onClose: Function,
    // 成功回调
    onSuccess: Function
  },
  data() {
    return {
      loading: false,
      accountList: [],
      form: {
        settlementAccountId: null,
        settlementAccount: null,
        amount: 0,
        discountAmount: 0,
        remarks: null
      }
    };
  },
  computed: {
    ...mapState(['user']),
    isReceipt() {
      return this.type === 'receipt';
    },
    unverifiedAmount() {
      const final = this.order.finalAmount || this.order.totalAmount || 0;
      const verified = this.order.verifiedAmount || 0;
      return Math.max(0, final - verified).toFixed(2);
    }
  },
  methods: {
    onAccountChange(val) {
      const item = this.accountList.find(a => a.id === val);
      this.form.settlementAccount = item?.name || null;
    },
    loadAccounts() {
      Account.select().then(({data}) => {
        this.accountList = data || [];
      });
    },
    handleSave() {
      if (!this.form.settlementAccountId) {
        return MessagePlugin.error('请选择结算账户');
      }
      if (!this.form.amount || this.form.amount <= 0) {
        return MessagePlugin.error('请输入结算金额');
      }

      this.loading = true;

      if (this.isReceipt) {
        this.saveReceipt();
      } else {
        this.savePayment();
      }
    },
    saveReceipt() {
      const orderReceipt = {
        customerId: this.order.customerId,
        customerName: this.order.customerName,
        orderType: 1,
        orderDate: manba().format('YYYY-MM-DD'),
        orderStatus: '已审核',
        documentSource: 1,
        createdBy: this.user.admin.id,
        updatedBy: this.user.admin.id,
        approvedBy: this.user.admin.id,
        remarks: this.form.remarks
      };

      const collectionList = [{
        settlementAccountId: this.form.settlementAccountId,
        settlementAccount: this.form.settlementAccount,
        amount: this.form.amount,
        remarks: this.form.remarks
      }];

      const final = this.order.finalAmount || this.order.totalAmount || 0;
      const verified = this.order.verifiedAmount || 0;
      const unverified = Math.max(0, final - verified);

      // 添加源单核销明细（关联当前销售出库单）
      const itemList = [{
        salesOrderNo: this.order.orderNo,
        salesOrderId: this.order.id,
        businessType: 1,
        documentAmount: final,
        verifiedAmount: verified,
        unverifiedAmount: unverified,
        currentVerifyAmount: Math.min(this.form.amount, unverified)
      }];

      OrderReceipt.save({orderReceipt, collectionList, itemList}).then(() => {
        MessagePlugin.success('便捷收款成功~');
        this.onSuccess?.();
        this.onClose?.();
      }).finally(() => {
        this.loading = false;
      });
    },
    savePayment() {
      const orderPayment = {
        supplierId: this.order.supplierId,
        supplierName: this.order.supplierName,
        orderType: 1,
        orderDate: manba().format('YYYY-MM-DD'),
        orderStatus: '已审核',
        documentSource: 1,
        createdBy: this.user.admin.id,
        updatedBy: this.user.admin.id,
        approvedBy: this.user.admin.id,
        remarks: this.form.remarks
      };

      const collectionList = [{
        settlementAccountId: this.form.settlementAccountId,
        settlementAccount: this.form.settlementAccount,
        amount: this.form.amount,
        remarks: this.form.remarks
      }];

      const final = this.order.finalAmount || this.order.totalAmount || 0;
      const verified = this.order.verifiedAmount || 0;
      const unverified = Math.max(0, final - verified);

      // 添加源单核销明细（关联当前采购入库单）
      const itemList = [{
        businessId: this.order.id,
        businessNo: this.order.orderNo,
        businessType: 1,
        documentAmount: final,
        verifiedAmount: verified,
        unverifiedAmount: unverified,
        currentVerifyAmount: Math.min(this.form.amount, unverified)
      }];

      OrderPayment.save({orderPayment, collectionList, itemList}).then(() => {
        MessagePlugin.success('便捷付款成功~');
        this.onSuccess?.();
        this.onClose?.();
      }).finally(() => {
        this.loading = false;
      });
    },
    handleCancel() {
      this.onClose?.();
    }
  },
  created() {
    this.loadAccounts();
    // 默认结算金额为未结算金额
    const final = this.order.finalAmount || this.order.totalAmount || 0;
    const verified = this.order.verifiedAmount || 0;
    this.form.amount = Math.max(0, final - verified);
  }
};
</script>

<style scoped>
.quick-payment-dialog {
  padding: 16px;
}
.quick-payment-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
