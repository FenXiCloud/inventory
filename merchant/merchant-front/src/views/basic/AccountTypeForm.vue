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
        <t-form-item v-if="parent" label="上级类别">
          <t-input :value="parent.name" disabled/>
        </t-form-item>
        <t-form-item label="收支类别" name="costType">
          <t-radio-group v-model="model.costType" :disabled="!!parent || !!entity">
            <t-radio value="收入">收入</t-radio>
            <t-radio value="支出">支出</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="名称" name="name">
          <t-input
              v-model="model.name"
              :placeholder="parent ? '请输入下级名称' : '请输入名称'"
              :maxlength="32"
              clearable
          />
        </t-form-item>
      </t-form>
    </div>
    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button v-auth="'accountType:edit'" theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
import AccountType from '@js/api/basic/AccountType';
import {MessagePlugin} from 'tdesign-vue-next';
import {CopyObj} from '@common/utils';

export default {
  name: 'AccountTypeForm',
  emits: ['close', 'success'],
  props: {
    entity: Object,
    parent: Object,
    defaultCostType: {type: String, default: '支出'}
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        pid: null,
        name: null,
        costType: '支出',
        enabled: true
      },
      rules: {
        costType: [{required: true, message: '请选择收支类别', type: 'error'}],
        name: [{required: true, message: '请输入名称', type: 'error', trigger: 'blur'}]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        const payload = {
          id: this.model.id,
          name: (this.model.name || '').trim(),
          costType: this.model.costType,
          enabled: this.model.enabled !== false,
          pid: this.model.pid || null
        };
        if (!payload.name) {
          MessagePlugin.warning('请输入名称');
          return;
        }
        this.loading = true;
        AccountType.save(payload)
          .then(() => {
            MessagePlugin.success('保存成功~');
            this.$emit('success');
          })
          .finally(() => (this.loading = false));
      }).catch(() => {});
    }
  },
  created() {
    this.model.costType = this.defaultCostType || '支出';
    if (this.entity) {
      CopyObj(this.model, this.entity);
    }
    if (this.parent) {
      this.model.id = null;
      this.model.pid = this.parent.id;
      this.model.costType = this.parent.costType || this.defaultCostType;
      this.model.name = null;
      this.model.enabled = true;
    }
  }
};
</script>
