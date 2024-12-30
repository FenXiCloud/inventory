<template>
  <div class="frame-page">
    <div class="h-panel">
      <div class="h-panel-body">
        <Tabs :datas="tabs" v-model="tab"/>
        <Form v-if="tab==='base'" class="mt-16px w-400px" mode="block" ref="form" :model="userForm"
              :rules="validationRules">
          <FormItem label="用户名">
            <Input v-model="userForm.username" disabled/>
          </FormItem>
          <FormItem label="姓名" prop="name">
            <Input v-model="userForm.name"/>
          </FormItem>
          <FormItem>
            <Button @click="doSave" :loading="loading" icon="fa fa-save" color="primary">保 存</Button>
          </FormItem>
        </Form>
        <Form v-if="tab==='safe'" class="mt-16px w-400px" mode="block" ref="pform" :model="passwordForm"
              :rules="validationRules">
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
/**
 * @功能描述: 账户设置\修改密码
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import {mapState} from "vuex"
import {clone} from "xe-utils"
import {message} from "heyui.ext";
import User from "@js/api/User";

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
      userForm: {},
      passwordForm: {
        oldPassword: null,
        newPassword: null,
        confirmPassword: null
      },
      validationRules: {
        required: ['name', 'oldPassword', 'newPassword', 'confirmPassword'],
      }
    }
  },
  methods: {
    doSave() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        User.save(this.userForm).then(() => {
          message("保存成功,重新登录后生效~");
        }).finally(() => this.loading = false);
      }
    },
    doChange() {
      let validResult = this.$refs.pform.valid();
      if (validResult.result) {
        this.loading = true;
        User.updatePassword(this.passwordForm).then(() => {
          message("保存成功,重新登录时生效~");
        }).finally(() => this.loading = false);
      }
    }
  },
  created() {
    this.userForm = clone(this.user, true);
  }
}
</script>
