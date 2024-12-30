<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules" mode="block">
        <FormItem label="编码" required prop="code">
          <Input placeholder="请输入分类编码" :disabled="model.id" v-model="model.code"/>
        </FormItem>
        <FormItem label="名称" required prop="name">
          <Input placeholder="请输入名称" v-model="model.name"/>
        </FormItem>
      </Form>
    </div>
    <div class="modal-column-between">
      <Button @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>

import SupplierCategory from "@js/api/SupplierCategory";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";

export default {
  name: "SupplierCategoryForm",
  props: {
    entity: Object,
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        name: null,
        code: null,
      },
      validationRules: {}
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        SupplierCategory.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    }
  },
  created() {
    CopyObj(this.model, this.entity);
  }
}
</script>
