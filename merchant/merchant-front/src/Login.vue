<template>
  <div class="login">
    <div class="login__main">
      <div class="login__header">
        <div class="login__logo"></div>
        <div class="login__title-wrap">
          <div class="login__title">纷析云进销存管理系统</div>
          <div class="login__title-en">FinXi MART Management System</div>
        </div>
      </div>

      <div class="login__body">
        <div class="login__visual"></div>
        <div class="login__form-panel">
          <t-form ref="loginForm" :data="form" :rules="rules" label-align="top" @submit.prevent>
            <div class="login__form-title">商户登录</div>
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
                  style="height: 40px; border-radius: 4px; background: #3d74ff"
                  @click="submitForm"
              >登 录</t-button>
            </t-form-item>
          </t-form>
        </div>
      </div>

      <div class="login__tagline">
        <p>开启智慧之旅，获取虚拟学习的无尽宝库.</p>
        <p>Start a journey of wisdom and gain an endless treasure trove of virtual learning.</p>
      </div>
    </div>

    <div class="login__footer">
      <span>Copyright © 2014-2024, 纷析云（杭州）科技有限公司</span>
    </div>
  </div>
</template>

<script>
import { Login, loginByBumer } from '@js/api/App';
import { MessagePlugin } from 'tdesign-vue-next';

export default {
  name: 'Login',
  data() {
    return {
      loading: false,
      form: {
        username: null,
        password: null
      },
      rules: {
        username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    };
  },
  methods: {
    loginByMobile() {
      const search = window.location.search;
      const params = new URLSearchParams(search);
      const mobile = params.get('mobile');

      if (!mobile) return;
      loginByBumer({ mobile }).then(({ success }) => {
        if (success) {
          MessagePlugin.success('登录成功');
          window.location.replace('/');
        }
      });
    },
    submitForm() {
      this.$refs.loginForm.validate().then((res) => {
        if (res === true || (res && res.result === true)) {
          this.loading = true;
          Login(this.form)
            .then(({ success }) => {
              if (success) {
                MessagePlugin.success('登录成功');
                localStorage.setItem('m_cache_username', this.form.username);
                window.location.replace('/');
              }
            })
            .catch((err) => {
              if (err && err.msg) {
                MessagePlugin.error(err.msg);
              }
            })
            .finally(() => {
              this.loading = false;
            });
        }
      }).catch(() => {});
    }
  },
  created() {
    let username = localStorage.getItem('m_cache_username');
    if (username) {
      this.form.username = username;
    }
    this.loginByMobile();
  }
};
</script>

<style scoped lang="less">
.login {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  background:
      radial-gradient(ellipse at 12% 18%, rgba(61, 116, 255, 0.1), transparent 42%),
      linear-gradient(180deg, #f7f9fc 0%, #eef3fa 100%);
}

.login__main {
  flex: 1;
  width: min(920px, calc(100% - 32px));
  margin: 40px auto 24px;
  display: flex;
  flex-direction: column;
}

.login__header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.login__logo {
  width: 110px;
  height: 38px;
  background: url(@/assets/logo_login.png) no-repeat;
  background-size: contain;
  flex-shrink: 0;
}

.login__title-wrap {
  margin-left: 12px;
  padding-left: 12px;
  border-left: 1px solid #c8ced6;
}

.login__title {
  font-size: 20px;
  font-weight: 700;
  color: #333639;
  letter-spacing: 0.5px;
  line-height: 1.2;
}

.login__title-en {
  font-size: 12px;
  color: #8f959e;
  margin-top: 2px;
}

.login__body {
  display: flex;
  height: 400px;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 10px 32px rgba(31, 45, 61, 0.1);
}

.login__visual {
  flex: 1;
  background: url(@/assets/login-bg.jpg) no-repeat center/cover;
}

.login__form-panel {
  flex: 0 0 340px;
  padding: 36px 32px;
  background: #fff;
  box-sizing: border-box;
  display: flex;
  align-items: center;
}

.login__form-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
  letter-spacing: 2px;
  margin-bottom: 24px;
}

.login__tagline {
  margin-top: 20px;
  text-align: right;

  p {
    margin: 0;
    font-size: 13px;
    color: #3d74ff;
    line-height: 1.6;
  }
}

.login__footer {
  height: 56px;
  background: #3d74ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
}

@media (max-width: 760px) {
  .login__body {
    flex-direction: column;
    height: auto;
  }

  .login__visual {
    height: 160px;
  }

  .login__form-panel {
    flex: none;
    width: 100%;
  }

  .login__tagline {
    text-align: left;
  }
}
</style>
