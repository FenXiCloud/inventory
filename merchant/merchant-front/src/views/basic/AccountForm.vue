<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <t-form
          ref="form"
          :data="model"
          :rules="rules"
          layout="vertical"
          label-align="top"
      >
        <t-form-item label="账户类别" name="accountType">
          <t-select
              v-model="model.accountType"
              :options="accountTypeOptions"
              :keys="{ value: 'key', label: 'title' }"
              placeholder="请选择账户类别"
          />
        </t-form-item>
        <t-form-item label="账户类别名称" name="accountTypeItem">
          <t-select
              v-model="model.accountTypeItem"
              :options="accountTypeItemOptions"
              :keys="{ value: 'key', label: 'title' }"
              placeholder="请选择账户类别名称"
          />
        </t-form-item>
        <t-form-item label="名称" name="name">
          <t-input v-model="model.name" placeholder="请输入名称" :maxlength="32"/>
        </t-form-item>
        <t-form-item label="币别" name="currency">
          <t-input v-model="model.currency" placeholder="请输入币别"/>
        </t-form-item>
        <t-form-item label="账户余额" name="balance">
          <t-input-number
              v-model="model.balance"
              theme="normal"
              :decimal-places="2"
              disabled
              style="width: 100%"
          />
        </t-form-item>
      </t-form>
    </div>
    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: 账户FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Account from '@js/api/basic/Account';
import {MessagePlugin} from 'tdesign-vue-next';
import {CopyObj} from '@common/utils';
import {accountTypes, accountTypeItems} from '@common/dict';

export default {
  name: 'AccountForm',
  emits: {close: null, success: null},
  props: {
    entity: Object
  },
  data() {
    return {
      loading: false,
      accountTypeOptions: accountTypes,
      accountTypeItemOptions: accountTypeItems,
      model: {
        id: null,
        name: null,
        currency: 'RMB',
        accountType: '资产',
        accountTypeItem: '银行账户',
        balance: 0
      },
      rules: {
        accountType: [{required: true, message: '请选择账户类别', type: 'error'}],
        accountTypeItem: [{required: true, message: '请选择账户类别名称', type: 'error'}],
        name: [{required: true, message: '请输入名称', type: 'error'}],
        currency: [{required: true, message: '请输入币别', type: 'error'}]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        Account.save(this.model)
          .then(() => {
            MessagePlugin.success('保存成功~');
            this.$emit('success');
          })
          .finally(() => (this.loading = false));
      }).catch(() => {});
    }
  },
  created() {
    CopyObj(this.model, this.entity);
  }
};
</script>
