<template>
  <div class="modal-column supplier-form">
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
            <t-form-item label="编码" name="code">
              <t-input
                  v-model="model.code"
                  placeholder="请输入编码，不填自动生成"
                  :disabled="!!model.id"
              />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="名称" name="name">
              <t-input v-model="model.name" placeholder="请输入货商名称"/>
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="货商分类" name="supplierCategoryId">
              <t-select
                  v-model="model.supplierCategoryId"
                  :options="supplierCategoryList"
                  :keys="{ value: 'id', label: 'name' }"
                  filterable
                  clearable
                  placeholder="请选择货商分类"
              />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="应付余额" name="balance" tips="正数=应付欠款，负数=预付；由单据/期初维护">
              <t-input-number
                  v-model="model.balance"
                  theme="normal"
                  :decimal-places="2"
                  placeholder="0.00"
                  disabled
                  style="width: 100%"
              />
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="联系人" name="contact">
              <t-input v-model="model.contact" placeholder="联系人"/>
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="电话" name="phone">
              <t-input v-model="model.phone" placeholder="电话"/>
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="税号" name="taxNo">
              <t-input v-model.trim="model.taxNo" placeholder="税号" :maxlength="32"/>
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="是否启用" name="enabled">
              <t-radio-group v-model="model.enabled">
                <t-radio :value="true">启用</t-radio>
                <t-radio :value="false">禁用</t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>

          <t-col :span="12">
            <t-form-item label="地址" name="address">
              <t-input v-model="model.address" placeholder="地址"/>
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
    </div>

    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button v-auth="'supplier:edit'" theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
import Supplier from '@js/api/basic/Supplier';
import {MessagePlugin} from 'tdesign-vue-next';
import {CopyObj} from '@common/utils';
import SupplierCategory from '@js/api/basic/SupplierCategory';

export default {
  name: 'SupplierForm',
  emits: {close: null, success: null},
  props: {
    entity: Object
  },
  data() {
    return {
      loading: false,
      supplierCategoryList: [],
      model: {
        balance: 0,
        id: null,
        code: null,
        name: null,
        contact: null,
        phone: null,
        taxNo: null,
        supplierCategoryId: null,
        address: null,
        enabled: true
      },
      rules: {
        name: [{required: true, message: '请输入货商名称', type: 'error'}],
        supplierCategoryId: [{required: true, message: '请选择货商分类', type: 'error'}]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        const payload = {...this.model};
        if (!payload.id) {
          payload.balance = 0;
        }
        Supplier.save(payload)
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
    SupplierCategory.select().then(({data}) => {
      this.supplierCategoryList = data || [];
    });
  }
};
</script>

<style scoped>
.supplier-form :deep(.t-form__item) {
  margin-bottom: 8px;
}

.supplier-form :deep(.t-form__label) {
  padding-bottom: 4px !important;
}
</style>
