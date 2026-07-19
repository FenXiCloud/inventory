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
 * @功能描述: 单位FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Unit from '@js/api/basic/Unit';
import {MessagePlugin} from 'tdesign-vue-next';
import {CopyObj} from '@common/utils';

export default {
  name: 'UnitForm',
  emits: ['close', 'success'],
  props: {
    entity: Object
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        name: null
      },
      rules: {
        name: [{required: true, message: '请输入名称', type: 'error', trigger: 'blur'}]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        Unit.save(this.model)
          .then(() => {
            MessagePlugin.success('保存成功~');
            this.$emit('success');
          })
          .finally(() => (this.loading = false));
      }).catch(() => {});
    }
  },
  created() {
    if (this.entity) {
      CopyObj(this.model, this.entity);
    }
  }
};
</script>
