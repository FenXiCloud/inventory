<template>
  <div class="m-16px">
    <Form ref="form" :model="model" :rules="validationRules" :labelWidth="120" mode="twocolumn">
      <FormItem label="名称" required prop="name">
        <Input placeholder="请输入门店名称" v-model="model.name"/>
      </FormItem>
      <FormItem label="编码" prop="code">
        <Input :readonly="model.id" placeholder="门店编码" v-model="model.code"/>
      </FormItem>
    </Form>
  </div>
  <div class="layui-layer-btn layui-layer-btn-r">
    <Button icon="fa fa-close" @click="$emit('close')" :loading="loading">
      取消
    </Button>
    <Button icon="fa fa-save" color="primary" @click="confirm" :loading="loading">
      保存
    </Button>
  </div>
</template>

<script>

import AccountBook from "@js/api/AccountBook";
import {message} from "heyui.ext";
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
      validationRules: {
        mobile: ['phone']
      }
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        AccountBook.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    },
  },
  created() {
    CopyObj(this.model, this.accountBook);
    this.model.merchantId = this.merchantId;
  }
}
</script>
