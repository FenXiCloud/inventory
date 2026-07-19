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
          <t-input v-model="model.name" placeholder="请输入分类名称" :maxlength="32" clearable/>
        </t-form-item>
        <t-form-item label="排序" name="sort">
          <t-input-number v-model="model.sort" theme="normal" :min="0" style="width: 100%"/>
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
 * @功能描述: 产品分类Form
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import ProductCategory from '@js/api/basic/ProductCategory';
import {MessagePlugin} from 'tdesign-vue-next';
import {CopyObj} from '@common/utils';

export default {
  name: 'ProductCategoryForm',
  emits: ['close', 'success'],
  props: {
    productCategory: Object,
    parent: Object
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        code: null,
        pid: null,
        name: null,
        path: null,
        sort: 1
      },
      rules: {
        name: [{required: true, message: '请输入分类名称', type: 'error', trigger: 'blur'}],
        sort: [{required: true, message: '请输入排序', type: 'error'}]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        ProductCategory.save(this.model)
          .then(() => {
            MessagePlugin.success('保存成功~');
            this.$emit('success');
          })
          .finally(() => (this.loading = false));
      }).catch(() => {});
    }
  },
  created() {
    if (this.productCategory) {
      CopyObj(this.model, this.productCategory);
    }
    if (this.parent) {
      this.model.pid = this.parent.id;
    }
  }
};
</script>
