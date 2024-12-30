<template>
  <div class="frame-page">
    <div class="h-panel">
      <div class="h-panel-body">
        <Form class="mt-16px w-400px" ref="form" :model="merchant" :rules="validationRules">
          <FormItem label="编号">
            <Input v-model="merchant.code" readonly/>
          </FormItem>
          <FormItem label="名称" prop="name">
            <Input v-model="merchant.name"/>
          </FormItem>
          <FormItem label="联系人" prop="linkman">
            <Input v-model="merchant.linkman"/>
          </FormItem>
          <FormItem label="电话" prop="mobile">
            <Input v-model="merchant.mobile"/>
          </FormItem>
          <FormItem label="地址">
            <Input v-model="merchant.address"/>
          </FormItem>
          <FormItem label="邮箱">
            <Input v-model="merchant.email"/>
          </FormItem>
          <FormItem>
            <Button @click="doSave" :loading="loading" color="primary">保 存</Button>
          </FormItem>
        </Form>
      </div>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: 商户信息
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import {mapState} from "vuex"
import {clone} from "xe-utils"
import {message} from "heyui.ext";
import Merchant from "@js/api/Merchant";

export default {
  name: "MerchantInfo",
  computed: {
    ...mapState(['user'])
  },
  data() {
    return {
      loading: false,
      merchant: {},
      validationRules: {
        required: ['name', 'linkman', 'phone'],
        mobile: ['phone']
      }
    }
  },
  methods: {
    doSave() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        Merchant.save(this.merchant).then(() => {
          message("保存成功,重新登录后生效~");
        }).finally(() => this.loading = false);
      }
    }
  },
  created() {
    this.merchant = clone(this.user.merchant, true);
  }
}
</script>
