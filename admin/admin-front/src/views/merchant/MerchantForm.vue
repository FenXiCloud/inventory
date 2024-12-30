<template>
  <div class="m-16px">
    <Form ref="form" :model="model" :rules="validationRules" mode="block">
      <FormItem label="商户名称" required prop="name">
        <Input placeholder="请输入商户名称" v-model="model.name"/>
      </FormItem>
      <div class="flex">
        <FormItem label="联系人姓名" required prop="linkman" class="flex-1 ">
          <Input placeholder="请输入联系人姓名" v-model="model.linkman"/>
        </FormItem>
        <FormItem label="联系人电话" required prop="mobile" class="flex-1">
          <Input placeholder="请输入联系人常用手机号" v-model="model.mobile"/>
        </FormItem>
      </div>
      <div class="flex">
        <FormItem label="邮箱" prop="email" class="flex-1 mr-16px">
          <Input placeholder="请输入联系人常用邮箱" v-model="model.email"/>
        </FormItem>
        <FormItem label="地址" prop="address" class="flex-1">
          <Input placeholder="请输入地址" v-model="model.address"/>
        </FormItem>
      </div>
      <div class="flex">
        <FormItem label="服务开始日期" required prop="serviceStartDate" class="flex-1">
          <DatePicker v-model="model.serviceStartDate" format="YYYY-MM" type="month" :clearable="false"/>
        </FormItem>
        <FormItem label="服务结束日期" required prop="serviceEndDate" class="flex-1">
          <DatePicker v-model="model.serviceEndDate" format="YYYY-MM" type="month" :clearable="false"/>
        </FormItem>
      </div>
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

<script>/**
 * <p>****************************************************************************</p>
 * <p><b>Copyright © 2010-2022 soho team All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : </li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2022年03月08日</li>
 * <li>@author     : ____′↘夏悸</li>
 * </ul>
 * <p>****************************************************************************</p>
 */
import Merchant from "@js/api/Merchant";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";

export default {
  name: "MerchantForm",
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
        address: null,
        email: null,
        linkman: null,
        name: null,
        phone: null,
        startCheckDate: null,
        serviceStartDate: null,
        serviceEndDate: null,
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
        Merchant.save(this.model).then(() => {
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
