<template>
  <div class="frame-page">
    <div class="h-panel">
      <div class="h-panel-body">
        <Tabs :datas="tabs" v-model="tab"/>
        <Form v-if="tab==='base'" class="mt-16px w-400px" mode="block" ref="form" :model="admin" :rules="validationRules">
          <FormItem label="账号">
            <Input v-model="admin.username" readonly/>
          </FormItem>
          <FormItem label="姓名" prop="name">
            <Input v-model="admin.name"/>
          </FormItem>
          <FormItem label="电话" prop="phone">
            <Input v-model="admin.phone"/>
          </FormItem>
          <FormItem>
            <Button @click="doSave" :loading="loading" icon="fa fa-save" color="primary">保 存</Button>
          </FormItem>
        </Form>
        <Form v-if="tab==='safe'" class="mt-16px w-400px" mode="block" ref="pform" :model="passwordForm" :rules="validationRules">
          <FormItem label="原密码" prop="oldPassword">
            <Input v-model="passwordForm.oldPassword"/>
          </FormItem>
          <FormItem label="新密码" prop="newPassword">
            <Input v-model="passwordForm.newPassword"/>
          </FormItem>
          <FormItem label="确认新密码" prop="confirmPassword">
            <Input v-model="passwordForm.confirmPassword"/>
          </FormItem>
          <FormItem>
            <Button @click="doChange" :loading="loading" icon="fa fa-save" color="primary">修 改 密 码</Button>
          </FormItem>
        </Form>
      </div>
    </div>
  </div>
</template>

<script>

import {mapState} from "vuex"
import {clone} from "xe-utils"
import {message} from "heyui.ext";
import Admin from "@js/api/Admin";

export default {
  name: "AccountBasic",
  computed: {
    ...mapState(['user'])
  },
  data() {
    return {
      loading: false,
      tab: 'base',
      tabs: {
        base: '基本信息',
        safe: '安全设置',
      },
      admin: {},
      passwordForm: {
        oldPassword: null,
        newPassword: null,
        confirmPassword: null
      },
      validationRules: {
        required: ['name', 'linkman', 'phone', 'oldPassword', 'newPassword', 'confirmPassword'],
        mobile: ['phone']
      }
    }
  },
  methods: {
    doSave() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        Admin.save(this.admin).then(() => {
          message("保存成功,重新登录后生效~");
        }).finally(() => this.loading = false);
      }
    },
    doChange() {
      let validResult = this.$refs.pform.valid();
      if (validResult.result) {
        this.loading = true;
        Admin.updatePassword(this.passwordForm).then(() => {
          message("保存成功,重新登录时生效~");
        }).finally(() => this.loading = false);
      }
    }
  },
  created() {
    this.admin = clone(this.user.admin, true);
  }
}
</script>
