<template>
  <div class="ml-100px" style="width: 600px;">
    <Form ref="form" :model="model" :rules="validationRules" :labelWidth="150">
      <FormItem label="公众号名称：" required prop="name">
        <Input placeholder="请输入公众号名称" v-model="model.name"/>
      </FormItem>
        <FormItem label="原始ID：" required prop="originalId" >
          <Input placeholder="请输入原始ID" v-model="model.originalId"/>
        </FormItem>
        <FormItem label="二维码图片："  prop="qrcode" >

        </FormItem>
        <FormItem label="开发者AppId：" required prop="AppID" >
          <Input placeholder="请输入开发者AppId" v-model="model.AppID"/>
        </FormItem>
        <FormItem label="开发者AppSecret：" required  prop="AppSecret" >
          <Input placeholder="请输入开发者AppSecret" v-model="model.AppSecret"/>
        </FormItem>
    </Form>
  </div>
  <div align="center">
    <Button icon="fa fa-close" @click="$emit('close')" :loading="loading">
      取消
    </Button>
    <Button icon="fa fa-save" color="primary" @click="confirm" :loading="loading">
      保存
    </Button>
  </div>
</template>

<script>

import WechatSetting from "@js/api/WechatSetting";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";

export default {
  name: "WechatSetting",
  emits: {
    close: null,
    success: null
  },
  props: {
    merchant: Object
  },
  data() {
    return {
      opened: true,
      loading: false,
      model: {
        id: null,
        originalId: null,
        name: null,
        qrcode: null,
        AppID: null,
        AppSecret: null,
      },
      validationRules: {
        originalId: ['originalId'],
        name: ['name']
      }
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        WechatSetting.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    }
  },
  created() {
    CopyObj(this.model, this.merchant);
  }
}
</script>
