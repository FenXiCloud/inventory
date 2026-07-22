<template>
  <div class="modal-column customer-form">
    <div class="modal-column-full-body">
      <t-form
          ref="form"
          :data="model"
          :rules="rules"
          layout="vertical"
          label-align="top"
          scroll-to-first-error="smooth"
      >
        <t-row :gutter="[16, 8]">
          <t-col :span="6">
            <t-form-item label="客户编码" name="code">
              <t-input
                  v-model="model.code"
                  placeholder="编码（不填写系统自动生成）"
                  :disabled="!!model.id"
              />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="客户名称" name="name">
              <t-input v-model="model.name" placeholder="请输入客户名称"/>
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="客户分类" name="customerCategoryId">
              <t-select
                  v-model="model.customerCategoryId"
                  :options="customerCategoryList"
                  :keys="{ value: 'id', label: 'name' }"
                  filterable
                  clearable
                  placeholder="请选择客户分类"
              />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="客户等级" name="customerLevelId">
              <t-select
                  v-model="model.customerLevelId"
                  :options="customerLevelList"
                  :keys="{ value: 'id', label: 'name' }"
                  filterable
                  clearable
                  placeholder="请选择客户等级"
              />
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="联系人" name="contact">
              <t-input v-model.trim="model.contact" placeholder="联系人"/>
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="电话" name="phone">
              <t-input v-model.trim="model.phone" placeholder="电话"/>
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="余额" name="balance">
              <t-input-number
                  v-model="model.balance"
                  theme="normal"
                  :decimal-places="2"
                  placeholder="请输入余额"
                  style="width: 100%"
              />
            </t-form-item>
          </t-col>

          <t-col :span="12">
            <t-form-item label="客户描述" name="remarks">
              <t-textarea
                  v-model="model.remarks"
                  placeholder="客户描述"
                  :maxlength="150"
                  :autosize="{ minRows: 3, maxRows: 5 }"
              />
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
    </div>

    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
import Customer from '@js/api/basic/Customer';
import {MessagePlugin} from 'tdesign-vue-next';
import {CopyObj} from '@common/utils';
import CustomerCategory from '@js/api/basic/CustomerCategory';
import CustomerLevel from '@js/api/basic/CustomerLevel';

export default {
  name: 'CustomerForm',
  emits: {close: null, success: null},
  props: {
    entity: Object
  },
  data() {
    return {
      loading: false,
      customerCategoryList: [],
      customerLevelList: [],
      model: {
        balance: 0,
        id: null,
        code: null,
        name: null,
        contact: null,
        phone: null,
        customerCategoryId: null,
        customerLevelId: null,
        remarks: null
      },
      rules: {
        name: [{required: true, message: '请输入客户名称', type: 'error'}],
        customerCategoryId: [{required: true, message: '请选择客户分类', type: 'error'}],
        customerLevelId: [{required: true, message: '请选择客户等级', type: 'error'}],
        balance: [{required: true, message: '请输入余额', type: 'error'}]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        if (Number(this.model.balance) < 0) {
          MessagePlugin.warning('金额不可为负');
          return;
        }
        this.loading = true;
        Customer.save(this.model)
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
    Promise.all([CustomerCategory.select(), CustomerLevel.select()]).then((results) => {
      this.customerCategoryList = results[0].data || [];
      this.customerLevelList = results[1].data || [];
    });
  }
};
</script>

<style scoped>
.customer-form :deep(.t-form__item) {
  margin-bottom: 8px;
}

.customer-form :deep(.t-form__label) {
  padding-bottom: 4px !important;
}
</style>
