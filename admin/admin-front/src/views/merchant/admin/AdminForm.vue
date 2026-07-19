<template>
  <div class="m-16px">
    <t-form ref="form" :data="model" :rules="rules" label-align="right">
      <t-form-item label="姓名" name="name">
        <t-input placeholder="请输入真实姓名" v-model="model.name"/>
      </t-form-item>
      <t-form-item label="手机号" name="phone">
        <t-input placeholder="请输入常用手机号" v-model="model.phone"/>
      </t-form-item>
      <t-form-item label="角色" name="roleId">
        <t-select :options="selectRoles" placeholder="请选择角色" v-model="model.roleId"/>
      </t-form-item>
      <t-form-item label="账号" name="username">
        <t-input :readonly="!!model.id" placeholder="为空系统自动生成账号" v-model="model.username"/>
      </t-form-item>
      <t-form-item v-if="!model.id" label="密码" name="password">
        <t-input type="password" placeholder="默认手机号后6位" v-model="model.password"/>
      </t-form-item>
    </t-form>
    <div class="dialog-footer">
      <t-button @click="$emit('close')" :loading="loading">取消</t-button>
      <t-button theme="primary" @click="confirm" :loading="loading">保存</t-button>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: 管理员FROM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Admin from "@js/api/Admin";
import {MessagePlugin} from "tdesign-vue-next";
import {CopyObj} from "@common/utils";
import Role from "@js/api/Role";

export default {
  name: "AdminForm",
  props: {
    entity: Object,
    merchant: Object,
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
        phone: null
      },
      rules: {
        name: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
        phone: [{ required: true, message: '请输入常用手机号', trigger: 'blur' }],
      }
    }
  },
  computed: {
    selectRoles() {
      return this.roleList.map(r => ({ label: r.name, value: r.id }));
    }
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result === true) {
          this.loading = true;
          this.model.merchantId = this.merchant.id;
          Admin.save(this.model).then(() => {
            MessagePlugin.success("保存成功~");
            this.$emit('success');
          }).finally(() => this.loading = false);
        }
      });
    },
  },
  created() {
    CopyObj(this.model, this.entity);
    if (this.entity) {
      this.adminRoles = this.entity.adminRoles || [];
    }
    Role.simpleList({merchantId: this.merchant.id}).then(({data}) => {
      this.roleList = data;
    })
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
