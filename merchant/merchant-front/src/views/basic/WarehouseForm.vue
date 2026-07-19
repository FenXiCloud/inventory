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
        <t-form-item label="仓库编码" name="code">
          <t-input v-model="model.code" placeholder="请输入仓库编码"/>
        </t-form-item>
        <t-form-item label="仓库名称" name="name">
          <t-input v-model="model.name" placeholder="请输入仓库名称"/>
        </t-form-item>
        <t-form-item label="仓库地址" name="address">
          <t-input v-model="model.address" placeholder="请输入仓库地址"/>
        </t-form-item>
        <t-form-item label="是否启用" name="enabled">
          <t-radio-group v-model="model.enabled">
            <t-radio :value="true">启用</t-radio>
            <t-radio :value="false">禁用</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="是否默认" name="systemDefault">
          <t-radio-group v-model="model.systemDefault">
            <t-radio :value="true">是</t-radio>
            <t-radio :value="false">否</t-radio>
          </t-radio-group>
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
import Warehouse from '@js/api/basic/Warehouse';
import {MessagePlugin} from 'tdesign-vue-next';
import {CopyObj} from '@common/utils';

export default {
  name: 'WarehouseForm',
  emits: {close: null, success: null},
  props: {
    warehouse: Object
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        name: null,
        code: null,
        address: null,
        enabled: true,
        systemDefault: false
      },
      rules: {
        name: [{required: true, message: '请输入仓库名称', type: 'error'}]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        Warehouse.save(this.model)
          .then(() => {
            MessagePlugin.success('保存成功~');
            this.$emit('success');
          })
          .finally(() => (this.loading = false));
      }).catch(() => {});
    }
  },
  created() {
    CopyObj(this.model, this.warehouse);
  }
};
</script>
