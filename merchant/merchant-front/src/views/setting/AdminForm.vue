<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <t-form ref="form" :data="model" :rules="validationRules">
        <t-form-item label="姓名" name="name">
          <t-input placeholder="请输入真实姓名" v-model="model.name"/>
        </t-form-item>
        <t-form-item label="手机号" name="mobile">
          <t-input placeholder="请输入常用手机号" v-model="model.mobile"/>
        </t-form-item>
        <t-form-item label="角色" name="roleId">
          <t-select
              :options="roleList"
              :keys="{ value: 'id', label: 'name' }"
              placeholder="请选择角色"
              v-model="model.roleId"
              clearable
          />
        </t-form-item>
        <t-form-item label="账号" name="username">
          <t-input :readonly="!!model.id" placeholder="为空系统自动生成账号" v-model="model.username"/>
        </t-form-item>
        <t-form-item v-if="!model.id" label="密码" name="password">
          <t-input type="password" placeholder="默认手机号后6位" v-model="model.password"/>
        </t-form-item>
      </t-form>
    </div>
    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button v-auth="'admin:edit'" theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
import Admin from "@js/api/setting/Admin";
import {MessagePlugin} from "tdesign-vue-next";
import {CopyObj} from "@common/utils";
import Role from "@js/api/setting/Role";

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
        name: [{ required: true, message: '请输入姓名', type: 'error', trigger: 'blur' }],
        mobile: [{ required: true, message: '请输入手机号', type: 'error', trigger: 'blur' }],
        roleId: [{ required: true, message: '请选择角色', type: 'error' }]
      }
    }
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        Admin.save(this.model).then(() => {
          MessagePlugin.success("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }).catch(() => {});
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
