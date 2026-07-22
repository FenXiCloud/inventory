<template>
  <div class="simple-page">
    <div class="settings-card">
      <div class="settings-card__title">商户信息</div>
      <t-form
          ref="form"
          :data="merchant"
          :rules="formRules"
          label-width="80px"
          class="settings-card__form"
          @submit="doSave"
      >
        <t-form-item label="编号" name="code">
          <t-input v-model="merchant.code" readonly style="width: 320px; border-radius: 4px"/>
        </t-form-item>
        <t-form-item label="名称" name="name">
          <t-input v-model="merchant.name" style="width: 320px; border-radius: 4px"/>
        </t-form-item>
        <t-form-item label="联系人" name="linkman">
          <t-input v-model="merchant.linkman" style="width: 320px; border-radius: 4px"/>
        </t-form-item>
        <t-form-item label="电话" name="mobile">
          <t-input v-model="merchant.mobile" style="width: 320px; border-radius: 4px"/>
        </t-form-item>
        <t-form-item label="地址" name="address">
          <t-input v-model="merchant.address" style="width: 320px; border-radius: 4px"/>
        </t-form-item>
        <t-form-item label="邮箱" name="email">
          <t-input v-model="merchant.email" style="width: 320px; border-radius: 4px"/>
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" type="submit" :loading="loading" style="border-radius: 4px">保 存</t-button>
        </t-form-item>
      </t-form>
    </div>
  </div>
</template>

<script>
import {mapState} from "vuex"
import {clone} from '@common/utils'
import {MessagePlugin} from "tdesign-vue-next";
import Merchant from "@js/api/setting/Merchant";

export default {
  name: "MerchantInfo",
  computed: {
    ...mapState(['user'])
  },
  data() {
    return {
      loading: false,
      merchant: {},
      formRules: {
        name: [{required: true, message: '请输入名称'}],
        linkman: [{required: true, message: '请输入联系人'}],
        mobile: [{required: true, message: '请输入电话'}]
      }
    }
  },
  methods: {
    doSave({validateResult}) {
      if (validateResult !== true) {
        return;
      }
      this.loading = true;
      Merchant.save(this.merchant).then(() => {
        MessagePlugin.success("保存成功,重新登录后生效~");
      }).finally(() => this.loading = false);
    }
  },
  created() {
    this.merchant = clone(this.user.merchant, true);
  }
}
</script>

<style scoped>

.settings-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #333639;
  margin-bottom: 20px;
}

.settings-card__form {
  max-width: 480px;
}
</style>
