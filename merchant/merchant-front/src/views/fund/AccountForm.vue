<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form :label-width="110" ref="form" :model="model" :rules="validationRules" mode="single">
        <FormItem label="账户类别" required prop="accountType">
          <Select placeholder="请选择账户类别" v-model="model.accountType" dict="accountTypes"/>
        </FormItem>
        <FormItem label="账户类别名称" required prop="accountTypeItem">
          <Select placeholder="请选择账户类别名称" v-model="model.accountTypeItem" dict="accountTypeItems"/>
        </FormItem>
        <FormItem label="名称" required prop="name">
          <Input placeholder="请输入名称" maxlength="10" v-model="model.name"/>
        </FormItem>
        <FormItem label="币别" required prop="currency">
          <Input placeholder="请输入币别" v-model="model.currency"/>
        </FormItem>
        <FormItem label="账户余额" prop="balance">
          <Input placeholder="账户余额" disabled v-model="model.balance"/>
        </FormItem>

      </Form>
    </div>
    <div class="modal-column-between">
      <Button @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
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
import Account from "@js/api/fund/Account";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";

export default {
  name: "AccountForm",
  props: {
    entity: Object,
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        name: null,
        currency: 'RMB',
        accountType: '资产',
        accountTypeItem: '银行账户',
        balance: 0.00,
      },
      validationRules: {}
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        Account.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    }
  },
  created() {
    CopyObj(this.model, this.entity);
  }
}
</script>
