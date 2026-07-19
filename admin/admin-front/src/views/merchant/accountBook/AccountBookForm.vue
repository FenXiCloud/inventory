<template>
  <div class="m-16px">
    <t-form ref="form" :data="model" :rules="rules" label-width="120" label-align="right">
      <t-form-item label="名称" name="name">
        <t-input placeholder="请输入名称" v-model="model.name"/>
      </t-form-item>
    </t-form>
  </div>
  <div class="dialog-footer">
    <t-button @click="$emit('close')" :loading="loading">取消</t-button>
    <t-button theme="primary" @click="confirm" :loading="loading">保存</t-button>
  </div>
</template>

<script>

import AccountBook from "@js/api/AccountBook";
import {MessagePlugin} from "tdesign-vue-next";
import {CopyObj} from "@common/utils";

export default {
  name: "AccountBookForm",
  emits: {
    close: null,
    success: null
  },
  props: {
    accountBook: Object,
    merchantId: [String, Number],
  },
  data() {
    return {
      loading: false,
      merchantList: [],
      model: {
        id: null,
        code: null,
        name: null,
        phone: null,
        merchantId: null,
      },
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
      }
    }
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result === true) {
          this.loading = true;
          AccountBook.save(this.model).then(() => {
            MessagePlugin.success("保存成功~");
            this.$emit('success');
          }).finally(() => this.loading = false);
        }
      });
    },
  },
  created() {
    CopyObj(this.model, this.accountBook);
    this.model.merchantId = this.merchantId;
  }
}
</script>

<style>
.dialog-footer {
  text-align: right;
  padding: 12px 16px;
  border-top: 1px solid #e7e7e7;
}
.dialog-footer .t-button {
  margin-left: 8px;
}
</style>
