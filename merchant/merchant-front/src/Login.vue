<template>
  <div class="login">
    <div class="body-wrapper">
      <div class="login-header">
        <div class="login-logo"></div>&nbsp;
        <div class="login-title">纷析云进销存管理系统
          <div class="login-title-bot">FinXi MART Management System</div>
        </div>
      </div>
      <div class="section login-bg">
        <div class="login-form">
          <Form ref="loginForm" :model="form"  :labelWidth="70" :rules="rules">
            <div class="wel"> &nbsp;<span></span>
            </div>
            <FormItem>
              <template v-slot:label>
                <span class="white-color">账 号:</span>
              </template>
              <Input type="text" style="border-radius: 3px;" v-model="form.username" autocomplete="off" placeholder="请输入登录账号"/>
            </FormItem>
            <FormItem >
              <template v-slot:label>
                <span class="white-color">密 码:</span>
              </template>
              <Input type="password" style="border-radius: 3px;"  v-model="form.password" autocomplete="off" @keyup.enter="submitForm" placeholder="请输入密码"/>
            </FormItem>
            <FormItem >
              <Button :loading="loading" class="login-form-btn" color="primary" @click="submitForm">登 录</Button>
            </FormItem>
          </Form>
        </div>
      </div>
      <div class="login-footer">
        <p>开启智慧之旅，获取虚拟学习的无尽宝库.</p>
        <p>Start a journey of wisdom and gain an endless treasure trove of virtual learning.</p>
      </div>
    </div>
    <div class="footerWrap">
      <div class="copyright">
        <span>Copyright © 2014-2024, 纷析云（杭州）科技有限公司</span>
      </div>
    </div>
  </div>
</template>

<script>

import {Login} from "@js/api/App";
import {message} from "heyui.ext";

export default {
  name: "Login",
  data() {
    return {
      loading: false,
      account: null,
      selected: null,
      form: {
        username: null,
        password: null
      },
      rules: {
        required: ['username', 'password']
      }
    }
  },

  methods: {
    submitForm() {
      let validResult = this.$refs.loginForm.valid();
      if (validResult.result) {
        this.loading = true;
        Login(this.form).then(({success, data: {account}}) => {
          if (success) {
            message("登录成功~");
            localStorage.setItem("m_cache_username", this.form.username)
            window.location.replace("/");
          }
        }).finally(() => {
          this.loading = false;
        });
      }
    },
  },
  created() {
    let username = localStorage.getItem("m_cache_username");
    if (username) {
      this.form.username = username;
    }
  }
}
</script>
<style scoped lang="less">
.login {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: white;
}

.body-wrapper {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 900px;
  height: 560px;
  margin-left: -450px;
  margin-top: -300px;

  .wel {
    font-size: 24px;
    color: #fff;
    font-weight: normal;
    margin: 60px 0 50px;
    line-height: 1;
  }

  .lineD {
    float: right;
    width: 180px;
    height: 1px;
    background: #96aecc;
    margin-top: 20px;
  }

  .login-header {
    display: flex;
    word-wrap: break-word;
    overflow: hidden;
    width: 100%;
    margin-bottom: 30px;
    text-align: left;

    .login-logo {
      width: 110px;
      height: 38px;
      background: url(@/assets/logo_login.png) no-repeat;
      background-size: 99%;
      padding-right: 12px;

    }

    .login-title {
      font-size: 20px;
      color: #5a5a5a;
      padding-left: 8px;
      line-height: 1.2;
      letter-spacing: 0.5px;
      font-weight: bold;
      border-left: 1px solid #bebebe;
      color: #5a5a5a;
    }

    .login-title-bot {
      display: block;
      font-size: 12px;
      color: #5a5a5a;
      line-height: 1.6;
      letter-spacing: 0;
      font-weight: normal;
    }
  }


  .login-bg {
    width: 900px;
    height: 398px;
    background: url(@/assets/login-bg.jpg) no-repeat;
    border-radius: 3px;
  }

  .login-form {
    float: right;
    width: 320px;
    height: 398px;
    background: #a7bcde;
    padding: 0 30px;
    .login-form-btn{
      width: 100%;
      background-color: #3d74ff;
      border-radius: 3px;
    }
  }

  .login-footer {
    text-align: right;
    padding-top: 40px;

    p {
      font-size: 13px;
      color: #3d74ff;
      line-height: 1.6;
      margin: 0;
    }
  }
}

.footerWrap {
  position: fixed;
  bottom: 0;
  width: 100%;
  height: 70px;
  background: #3d74ff;
  padding-top: 20px;

  .copyright {
    width: 900px;
    margin: 0 auto;

    span {
      font-size: 12px;
      color: #fff;
      padding-left: 10px;
      opacity: .8;
    }
  }
}
</style>
