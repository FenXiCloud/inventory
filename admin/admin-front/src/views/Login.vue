<template>
  <div class="login">
    <div class="login__panel">
      <div class="login__brand">
        <div class="login__brand-name">纷析云</div>
        <div class="login__brand-product">进销存管理系统</div>
        <div class="login__brand-sub">管理中心</div>
        <p class="login__brand-desc">统一管理商户、菜单与系统配置</p>
      </div>
      <div class="login__form-wrap">
        <t-form
            ref="loginForm"
            :data="form"
            :rules="rules"
            class="login__form"
            label-align="top"
        >
          <div class="login__form-title">登录</div>
          <t-form-item label="账号" name="username">
            <t-input
                v-model="form.username"
                clearable
                autocomplete="off"
                placeholder="请输入登录账号"
                style="border-radius: 4px"
            />
          </t-form-item>
          <t-form-item label="密码" name="password">
            <t-input
                v-model="form.password"
                type="password"
                autocomplete="off"
                placeholder="请输入密码"
                style="border-radius: 4px"
                @enter="submitForm"
            />
          </t-form-item>
          <t-form-item>
            <t-button
                theme="primary"
                block
                :loading="loading"
                style="height: 40px; border-radius: 4px"
                @click="submitForm"
            >登 录</t-button>
          </t-form-item>
        </t-form>
      </div>
    </div>
    <div class="login__footer">Copyright © 2014-2024 纷析云（杭州）科技有限公司</div>
  </div>
</template>

<script>
import {Login} from "@js/api/App";
import {MessagePlugin} from "tdesign-vue-next";

export default {
  name: "Login",
  data() {
    return {
      loading: false,
      form: {
        username: null,
        password: null
      },
      rules: {
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
      }
    }
  },
  methods: {
    submitForm() {
      this.$refs.loginForm.validate().then((result) => {
        if (result === true) {
          this.loading = true;
          Login(this.form).then(({success}) => {
            if (success) {
              localStorage.setItem("admin_cache_username", this.form.username);
              MessagePlugin.success("登录成功~");
              window.location.replace("/");
            }
          }).finally(() => {
            this.loading = false;
          });
        }
      });
    }
  },
  created() {
    let username = localStorage.getItem("admin_cache_username");
    if (username) {
      this.form.username = username;
    }
  }
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 24px;
  box-sizing: border-box;
  background:
      radial-gradient(ellipse at 20% 20%, rgba(61, 116, 255, 0.18), transparent 50%),
      radial-gradient(ellipse at 80% 80%, rgba(30, 90, 200, 0.12), transparent 45%),
      linear-gradient(160deg, #f4f7fb 0%, #e8eef8 55%, #dfe8f6 100%);
}

.login__panel {
  display: flex;
  width: min(880px, 100%);
  min-height: 440px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 12px 40px rgba(31, 45, 61, 0.12);
}

.login__brand {
  flex: 1.05;
  padding: 48px 40px;
  background: linear-gradient(155deg, #2f5fd0 0%, #3d74ff 55%, #5a8dff 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-sizing: border-box;
}

.login__brand-name {
  font-size: 42px;
  font-weight: 700;
  letter-spacing: 2px;
  line-height: 1.15;
}

.login__brand-product {
  margin-top: 10px;
  font-size: 22px;
  font-weight: 500;
  letter-spacing: 1px;
  opacity: 0.95;
}

.login__brand-sub {
  margin-top: 18px;
  display: inline-block;
  width: fit-content;
  padding: 4px 10px;
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 4px;
  font-size: 13px;
  letter-spacing: 2px;
}

.login__brand-desc {
  margin: 28px 0 0;
  font-size: 14px;
  line-height: 1.6;
  opacity: 0.85;
  max-width: 280px;
}

.login__form-wrap {
  flex: 0 0 360px;
  display: flex;
  align-items: center;
  padding: 40px 36px;
  box-sizing: border-box;
}

.login__form {
  width: 100%;
}

.login__form-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 24px;
  letter-spacing: 2px;
}

.login__footer {
  margin-top: 24px;
  font-size: 12px;
  color: #8f959e;
}

@media (max-width: 760px) {
  .login__panel {
    flex-direction: column;
    min-height: auto;
  }

  .login__brand {
    padding: 32px 28px;
  }

  .login__brand-name {
    font-size: 32px;
  }

  .login__form-wrap {
    flex: none;
    width: 100%;
  }
}
</style>
