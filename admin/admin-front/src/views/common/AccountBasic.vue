<template>
  <div class="frame-page">
    <div class="t-panel">
      <div class="t-panel-body p-16px">
        <t-tabs v-model="tab">
          <t-tab-panel value="base" label="基本信息" />
          <t-tab-panel value="safe" label="安全设置" />
        </t-tabs>
        <t-form v-if="tab==='base'" class="mt-16px w-400px" ref="form" :data="userForm"
              :rules="baseRules" label-align="right">
          <t-form-item label="用户名">
            <t-input v-model="userForm.username" disabled/>
          </t-form-item>
          <t-form-item label="姓名" name="name">
            <t-input v-model="userForm.name"/>
          </t-form-item>
          <t-form-item>
            <t-button @click="doSave" :loading="loading" theme="primary">保 存</t-button>
          </t-form-item>
        </t-form>
        <t-form v-if="tab==='safe'" class="mt-16px w-400px" ref="pform" :data="passwordForm"
              :rules="pwdRules" label-align="right">
          <t-form-item label="原密码" name="oldPassword">
            <t-input type="password" v-model="passwordForm.oldPassword"/>
          </t-form-item>
          <t-form-item label="新密码" name="newPassword">
            <t-input type="password" v-model="passwordForm.newPassword"/>
          </t-form-item>
          <t-form-item label="确认新密码" name="confirmPassword">
            <t-input type="password" v-model="passwordForm.confirmPassword"/>
          </t-form-item>
          <t-form-item>
            <t-button @click="doChange" :loading="loading" theme="primary">修 改 密 码</t-button>
          </t-form-item>
        </t-form>
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
import {MessagePlugin} from "tdesign-vue-next";
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
      userForm: {},
      passwordForm: {
        oldPassword: null,
        newPassword: null,
        confirmPassword: null
      },
      baseRules: {
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
      },
      pwdRules: {
        oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
        confirmPassword: [{ required: true, message: '请输入确认新密码', trigger: 'blur' }],
      },
    }
  },
  methods: {
    doSave() {
      this.$refs.form.validate().then((result) => {
        if (result === true) {
          this.loading = true;
          User.save(this.userForm).then(() => {
            MessagePlugin.success("保存成功,重新登录后生效~");
          }).finally(() => this.loading = false);
        }
      });
    },
    doChange() {
      this.$refs.pform.validate().then((result) => {
        if (result === true) {
          this.loading = true;
          User.updatePassword(this.passwordForm).then(() => {
            MessagePlugin.success("保存成功,重新登录时生效~");
          }).finally(() => this.loading = false);
        }
      });
    }
  },
  created() {
    this.userForm = clone(this.user, true);
  }
}
</script>
