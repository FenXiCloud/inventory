<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules">
        <FormItem label="编码" prop="code" required>
          <Input
              placeholder="请输入分类编码"
              :disabled="model.id"
              v-model="model.code"
          />
        </FormItem>
        <FormItem label="名称" required prop="name">
          <Input placeholder="请输入分类名称" v-model="model.name" />
        </FormItem>
        <FormItem label="排序" required prop="sort">
          <Input placeholder="请输入排序" v-model="model.sort" />
        </FormItem>
      </Form>
    </div>
    <div class="modal-column-between">
      <Button @click="$emit('close')" :loading="loading"> 取消 </Button>
      <Button color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
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
import {message} from 'heyui.ext';
import {CopyObj} from '@common/utils';

export default {
  name: 'ProductCategoryForm',
  emits: {
    close: null,
    success: null
  },
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
      validationRules: {}
    };
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        ProductCategory.save(this.model)
            .then(() => {
              message('保存成功~');
              this.$emit('success');
            })
            .finally(() => (this.loading = false));
      }
    }
  },
  created() {
    CopyObj(this.model, this.productCategory);
    if (this.parent) {
      this.model.pid = this.parent.id;
    }
  }
};
</script>
