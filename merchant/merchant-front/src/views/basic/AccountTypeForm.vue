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
        <t-form-item label="收支类别" name="costType">
          <t-select
              v-model="model.costType"
              :options="costTypeOptions"
              :keys="{ value: 'key', label: 'title' }"
              :clearable="false"
              :disabled="!!parent"
              placeholder="请选择收支类别"
          />
        </t-form-item>
        <t-form-item label="名称" name="name">
          <t-input v-model="model.name" placeholder="请输入名称" :maxlength="10" clearable/>
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
 * @功能描述: 收支类别FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import AccountType from '@js/api/basic/AccountType';
import {MessagePlugin} from 'tdesign-vue-next';
import {CopyObj} from '@common/utils';
import {costTypes} from '@common/dict';

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
      costTypeOptions: costTypes,
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
        this.loading = true;
        AccountType.save(this.model)
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
      this.model.pid = this.parent.id;
      this.model.costType = this.parent.costType;
    }
  }
};
</script>
