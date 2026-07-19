<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form :label-width="110" ref="form" :model="model" :rules="validationRules" mode="single">
        <FormItem label="职员编号" required prop="code">
          <Input placeholder="请输入编号" v-model="model.code"/>
        </FormItem>
        <FormItem label="职员名称" required prop="name">
          <Input placeholder="请输入名称" maxlength="10" v-model="model.name"/>
        </FormItem>
        <FormItem label="手机号码" required prop="phone">
          <Input placeholder="请输入号码"  v-model="model.phone"/>
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
/**
 * @功能描述: 账户FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Account from "@js/api/fund/Account";
import {MessagePlugin} from "tdesign-vue-next";
import OrderReceipt from '@js/api/fund/OrderReceipt';
import { add } from 'xe-utils';
// import {CopyObj} from "@common/utils";

export default {
  name: "OrderStaffForm",
  props: {
  },
  data() {
    return {
      loading: false,
      model: {
       
      },
      validationRules: {}
    }
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((res) => {
        if (res === true || res.result === true) {
          this.loading = true;
          OrderReceipt.orderStaffAdd(this.model).then(() => {
            MessagePlugin.success("保存成功~");
            this.$emit('success');
          }).finally(() => this.loading = false);
        }
      }).catch(() => {});
    }
  },
  created() {
  }
}
</script>
