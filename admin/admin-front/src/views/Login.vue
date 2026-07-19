<template>
  <div class="login">
    <div class="body-wrapper">
      <div class="bg1"></div>
      <div class="gyl">
        <div class="gy2">纷析云进销存管理系统</div>
      </div>
      <div class="bg">
        <div class="form">
          <t-form
              ref="loginForm"
              :data="form"
              :rules="rules"
              class="login-form"
              label-align="top">
            <div class="wel">管理中心</div>
            <t-form-item label="账号" name="username">
              <t-input type="text" v-model="form.username" autocomplete="off" placeholder="请输入登录账号"/>
            </t-form-item>
            <t-form-item label="密码" name="password">
              <t-input type="password" v-model="form.password" autocomplete="off" @keyup.enter="submitForm" placeholder="请输入密码"/>
            </t-form-item>
            <t-form-item>
              <t-button :loading="loading" class="login-form-btn" theme="primary" block @click="submitForm">登 录</t-button>
            </t-form-item>
          </t-form>
        </div>
      </div>
    </div>
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
  background: url("@/assets/login-bg.jpg") no-repeat;
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background-size: cover;
  background-position: center;
}

.login-form {
  margin: 20px;
}

.login-form-btn {
  width: 100%;
  margin-top: 10px;
  height: 40px;
}

.login .wel {
  color: #3d74ff;
  font-weight: bold;
  letter-spacing: 5px;
  font-size: 1.5rem;
  margin-bottom: 10px;
  text-align: center;
}

.login .bg1 {
  width: 100%;
  height: 237px;
  background: #3d74ff;
  opacity: 0.6;
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  margin: auto;
}

.login .bg {
  position: absolute;
  left: 56%;
  top: 0;
  right: 0;
  bottom: 0;
  margin: auto;
  z-index: 1;
  border-radius: 5px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.login .bg .form {
  width: 400px;
  background: #fff;
  box-shadow: 0 0 50px rgba(0, 0, 0, 0.4);
}

.login .gyl {
  width: 530px;
  height: 237px;
  color: #FFFFFF;
  font-size: 65px;
  position: absolute;
  left: 15%;
  top: 11%;
  bottom: 0;
  margin: auto;
}

.login .gy2 {
  color: #fff;
  margin-left: 6px;
  font-size: 18px;
  text-align: center;
  margin-top: 10px;
}
</style>
