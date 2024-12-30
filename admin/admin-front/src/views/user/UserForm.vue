<template>
  <div class="m-16px">
    <Form ref="form" :model="model" :rules="validationRules">
      <FormItem label="姓名" required prop="name">
        <Input placeholder="请输入真实姓名" v-model="model.name"/>
      </FormItem>
      <FormItem label="账号" prop="username">
        <Input :readonly="!!model.id" placeholder="为空系统自动生成账号" v-model="model.username"/>
      </FormItem>
      <FormItem v-if="!model.id" label="密码" prop="password">
        <Input type="password" placeholder="默认手机号后6位" v-model="model.password"/>
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

<script>/**
 * /**
 *  * @功能描述: 管理员FROM
 *  * @创建时间: 2023年08月08日
 *  * @公司官网: www.fenxi365.com
 *  * @公司信息: 纷析云（杭州）科技有限公司
 *  * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 *  */
import User from "@js/api/User";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";

export default {
  name: "UserForm",
  props: {
    entity: Object,
    merchant: Object,
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        name: null,
        username: null,
        password: null,
      },
      validationRules: {}
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        User.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    },
  },
  created() {
    CopyObj(this.model, this.entity);
  }
}
</script>
