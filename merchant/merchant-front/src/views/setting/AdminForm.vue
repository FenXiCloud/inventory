<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules">
        <FormItem label="姓名" required prop="name">
          <Input placeholder="请输入真实姓名" v-model="model.name"/>
        </FormItem>
        <FormItem label="手机号" required prop="mobile">
          <Input placeholder="请输入常用手机号" v-model="model.mobile"/>
        </FormItem>
        <FormItem label="角色" prop="roleId" required>
          <Select :datas="roleList" keyName="id" titleName="name" placeholder="请选择角色" v-model="model.roleId"/>
        </FormItem>
        <FormItem label="账号" prop="username">
          <Input :readonly="!!model.id" placeholder="为空系统自动生成账号" v-model="model.username"/>
        </FormItem>
        <FormItem v-if="!model.id" label="密码" prop="password">
          <Input type="password" placeholder="默认手机号后6位" v-model="model.password"/>
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
 * @功能描述: 用户FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Admin from "@js/api/Admin";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";
import Role from "@js/api/Role";

export default {
  name: "AdminForm",
  computed: {
  },
  props: {
    entity: Object,
  },
  data() {
    return {
      loading: false,
      roleList: [],
      adminRoles: [],
      model: {
        id: null,
        name: null,
        username: null,
        password: null,
        roleId: null,
        mobile: null
      },
      validationRules: {
        mobile: ['mobile']
      }
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        Admin.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    },
  },
  created() {
    CopyObj(this.model, this.entity);
    if (this.entity) {
      this.adminRoles = this.entity.adminRoles || [];
    }
    Role.simpleList().then(({data}) => {
      this.roleList = data;
    })
  }
}
</script>
