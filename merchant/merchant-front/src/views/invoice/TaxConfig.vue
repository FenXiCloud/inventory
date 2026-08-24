<template>
  <div class="simple-page">
    <div class="simple-page__hint">
      配置本企业作为开票方（销方）的税号与银行信息，开具数电发票时使用。
    </div>

    <div class="tax-config__body">
      <t-form
          ref="formRef"
          :data="form"
          :rules="rules"
          label-align="right"
          label-width="120px"
          style="max-width: 640px;"
      >
        <t-form-item label="纳税人识别号" name="nsrsbh">
          <t-input v-model="form.nsrsbh" placeholder="请输入统一社会信用代码 / 税号" clearable/>
        </t-form-item>
        <t-form-item label="企业名称" name="name">
          <t-input v-model="form.name" placeholder="请输入销方名称" clearable/>
        </t-form-item>
        <t-form-item label="电话" name="phone">
          <t-input v-model="form.phone" placeholder="请输入联系电话" clearable/>
        </t-form-item>
        <t-form-item label="地址" name="address">
          <t-input v-model="form.address" placeholder="请输入注册/营业地址" clearable/>
        </t-form-item>
        <t-form-item label="开户银行" name="bank">
          <t-input v-model="form.bank" placeholder="请输入开户银行" clearable/>
        </t-form-item>
        <t-form-item label="银行账号" name="bankAccount">
          <t-input v-model="form.bankAccount" placeholder="请输入银行账号" clearable/>
        </t-form-item>
        <t-form-item label="发票类型" name="invoiceType">
          <t-select v-model="form.invoiceType" :options="invoiceTypeOptions" style="width: 220px;"/>
        </t-form-item>
        <t-form-item label="备注" name="remarks">
          <t-textarea v-model="form.remarks" placeholder="备注（可选）" :maxlength="200"/>
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" :loading="loading" @click="save">保 存</t-button>
            <t-button variant="outline" @click="reset">重 置</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </div>
  </div>
</template>

<script>
import Invoice from "@js/api/invoice/Invoice";
import {MessagePlugin} from "tdesign-vue-next";

export default {
  name: "TaxConfig",
  data() {
    return {
      loading: false,
      form: {
        id: null,
        nsrsbh: null,
        name: null,
        phone: null,
        address: null,
        bank: null,
        bankAccount: null,
        invoiceType: '030',
        remarks: null
      },
      invoiceTypeOptions: [
        {label: '数电普票（030）', value: '030'},
        {label: '数电专票（032）', value: '032'},
      ],
      rules: {
        nsrsbh: [{required: true, message: '请输入纳税人识别号', type: 'error'}],
        name: [{required: true, message: '请输入企业名称', type: 'error'}],
      }
    };
  },
  methods: {
    load() {
      this.loading = true;
      Invoice.getSellerConfig().then(({data}) => {
        if (data) {
          Object.assign(this.form, data);
        }
      }).finally(() => this.loading = false);
    },
    save() {
      this.$refs.formRef.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        Invoice.saveSellerConfig(this.form).then(() => {
          MessagePlugin.success('保存成功~');
          this.load();
        }).finally(() => this.loading = false);
      });
    },
    reset() {
      this.form = {
        id: this.form.id,
        nsrsbh: null, name: null, phone: null, address: null,
        bank: null, bankAccount: null, invoiceType: '030', remarks: null
      };
    }
  },
  created() {
    this.load();
  }
};
</script>

<style scoped>
.tax-config__body {
  background: #fff;
  border-radius: 4px;
  padding: 24px;
  overflow: auto;
}

.simple-page__hint {
  flex-shrink: 0;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f3f3f3;
  color: #555;
  font-size: 13px;
  line-height: 1.6;
}
</style>
