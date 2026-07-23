<template>
  <div class="m-16px">
    <t-form ref="form" :data="model" :rules="rules" label-align="right">
      <t-form-item label="商户名称" name="name">
        <t-input placeholder="请输入商户名称" v-model="model.name"/>
      </t-form-item>
      <div class="flex">
        <t-form-item label="联系人姓名" name="contact" class="flex-1 mr-16px">
          <t-input placeholder="请输入联系人姓名" v-model="model.contact"/>
        </t-form-item>
        <t-form-item label="联系人电话" name="mobile" class="flex-1">
          <t-input placeholder="请输入联系人常用手机号" v-model="model.mobile"/>
        </t-form-item>
      </div>
      <div class="flex">
        <t-form-item label="邮箱" name="email" class="flex-1 mr-16px">
          <t-input placeholder="请输入联系人常用邮箱" v-model="model.email"/>
        </t-form-item>
        <t-form-item label="地址" name="address" class="flex-1">
          <t-input placeholder="请输入地址" v-model="model.address"/>
        </t-form-item>
      </div>
    </t-form>
  </div>
  <div class="dialog-footer">
    <t-button @click="$emit('close')" :loading="loading">取消</t-button>
    <t-button theme="primary" @click="confirm" :loading="loading">保存</t-button>
  </div>
</template>

<script>
/**
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
import {MessagePlugin} from "tdesign-vue-next";
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
        contact: null,
        name: null,
        mobile: null,
        startCheckDate: null,
      },
      rules: {
        name: [{ required: true, message: '请输入商户名称', trigger: 'blur' }],
        contact: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
        mobile: [
          { required: true, message: '请输入联系人电话', trigger: 'blur' },
          { pattern: /^1\d{10}$/, message: '请输入11位手机号', trigger: 'blur' }
        ],
      }
    }
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result === true) {
          this.loading = true;
          Merchant.save(this.model).then(() => {
            MessagePlugin.success("保存成功~");
            this.$emit('success');
          }).finally(() => this.loading = false);
        }
      });
    }
  },
  created() {
    CopyObj(this.model, this.merchant);
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
