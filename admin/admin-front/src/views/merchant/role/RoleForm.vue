<template>
  <div class="m-16px">
    <Form ref="form" :model="model" :rules="validationRules" mode="block">
      <FormItem label="角色名称" required prop="name">
        <Input placeholder="请输入角色名称" v-model="model.name"/>
      </FormItem>
    </Form>
    <div class="layui-layer-btn layui-layer-btn-r">
      <Button icon="fa fa-close" @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button icon="fa fa-save" color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: 角色FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Role from "@js/api/Role";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";

export default {
  name: "RoleForm",
  props: {
    entity: Object,
    merchant: Object
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        name: null,
        merchantId: null,
        enabled: true,
        systemDefault: false,
        type: 0
      },
      validationRules: {}
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        Role.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    }
  },
  created() {
    CopyObj(this.model, this.entity);
      this.model.merchantId = this.merchant.id;
    if (this.type) {
      this.model.type = this.type;
    }
  }
}
</script>
