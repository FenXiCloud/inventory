<template>
  <div class="simple-page">
    <div class="settings-card">
      <t-tabs v-model="tab">
        <t-tab-panel value="base" label="基本信息">
          <t-form
              ref="form"
              class="settings-card__form"
              :data="admin"
              :rules="baseRules"
              label-width="80px"
              @submit="doSave"
          >
            <t-form-item label="账号" name="username">
              <t-input v-model="admin.username" readonly style="width: 320px; border-radius: 4px"/>
            </t-form-item>
            <t-form-item label="姓名" name="name">
              <t-input v-model="admin.name" style="width: 320px; border-radius: 4px"/>
            </t-form-item>
            <t-form-item label="电话" name="phone">
              <t-input v-model="admin.phone" style="width: 320px; border-radius: 4px"/>
            </t-form-item>
            <t-form-item>
              <t-button theme="primary" type="submit" :loading="loading" style="border-radius: 4px">保 存</t-button>
            </t-form-item>
          </t-form>
        </t-tab-panel>
        <t-tab-panel value="safe" label="安全设置">
          <t-form
              ref="pform"
              class="settings-card__form"
              :data="passwordForm"
              :rules="safeRules"
              label-width="100px"
              @submit="doChange"
          >
            <t-form-item label="原密码" name="oldPassword">
              <t-input v-model="passwordForm.oldPassword" type="password" style="width: 320px; border-radius: 4px"/>
            </t-form-item>
            <t-form-item label="新密码" name="newPassword">
              <t-input v-model="passwordForm.newPassword" type="password" style="width: 320px; border-radius: 4px"/>
            </t-form-item>
            <t-form-item label="确认新密码" name="confirmPassword">
              <t-input v-model="passwordForm.confirmPassword" type="password" style="width: 320px; border-radius: 4px"/>
            </t-form-item>
            <t-form-item>
              <t-button theme="primary" type="submit" :loading="loading" style="border-radius: 4px">修改密码</t-button>
            </t-form-item>
          </t-form>
        </t-tab-panel>
      </t-tabs>
    </div>
  </div>
</template>

<script>
import {mapState} from "vuex"
import {clone} from '@common/utils'
import {MessagePlugin} from "tdesign-vue-next";
import Admin from "@js/api/setting/Admin";

export default {
  name: "AccountBasic",
  computed: {
    ...mapState(['user'])
  },
  data() {
    return {
      loading: false,
      tab: 'base',
      admin: {},
      passwordForm: {
        oldPassword: null,
        newPassword: null,
        confirmPassword: null
      },
      baseRules: {
        name: [{required: true, message: '请输入姓名'}],
        phone: [{required: true, message: '请输入电话'}]
      },
      safeRules: {
        oldPassword: [{required: true, message: '请输入原密码'}],
        newPassword: [{required: true, message: '请输入新密码'}],
        confirmPassword: [
          {required: true, message: '请确认新密码'},
          {
            validator: (val) => val === this.passwordForm.newPassword,
            message: '两次密码不一致'
          }
        ]
      }
    }
  },
  methods: {
    doSave({validateResult}) {
      if (validateResult !== true) {
        return;
      }
      this.loading = true;
      Admin.save(this.admin).then(() => {
        MessagePlugin.success("保存成功,重新登录后生效~");
      }).finally(() => this.loading = false);
    },
    doChange({validateResult}) {
      if (validateResult !== true) {
        return;
      }
      this.loading = true;
      Admin.updatePassword(this.passwordForm).then(() => {
        MessagePlugin.success("保存成功,重新登录时生效~");
      }).finally(() => this.loading = false);
    }
  },
  created() {
    this.admin = clone(this.user.admin, true);
  }
}
</script>

<style scoped>

.settings-card__form {
  max-width: 480px;
  margin-top: 20px;
}
</style>
