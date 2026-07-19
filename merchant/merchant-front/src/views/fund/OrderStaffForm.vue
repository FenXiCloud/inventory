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
        <t-form-item label="职员编号" name="code">
          <t-input v-model="model.code" placeholder="请输入编号" />
        </t-form-item>
        <t-form-item label="职员名称" name="name">
          <t-input v-model="model.name" placeholder="请输入名称" :maxlength="10" />
        </t-form-item>
        <t-form-item label="手机号码" name="phone">
          <t-input v-model="model.phone" placeholder="请输入号码" />
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
 * @功能描述: 业务员FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import { MessagePlugin } from 'tdesign-vue-next';
import OrderReceipt from '@js/api/fund/OrderReceipt';

export default {
  name: 'OrderStaffForm',
  emits: { close: null, success: null },
  data() {
    return {
      loading: false,
      model: {
        code: null,
        name: null,
        phone: null
      },
      rules: {
        code: [{ required: true, message: '请输入职员编号', type: 'error' }],
        name: [{ required: true, message: '请输入职员名称', type: 'error' }],
        phone: [{ required: true, message: '请输入手机号码', type: 'error' }]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        OrderReceipt.orderStaffAdd(this.model)
          .then(() => {
            MessagePlugin.success('保存成功~');
            this.$emit('success');
          })
          .finally(() => (this.loading = false));
      }).catch(() => {});
    }
  }
};
</script>
