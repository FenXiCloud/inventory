<template>
  <div class="login">
    <div class="body-wrapper">
      <div class="bg1"></div>
      <div class="gyl">
        纷析云
        <div class="gy2">纷析云进销存管理系统</div>
      </div>
      <div class="bg">
        <div class="form">
          <Form
              ref="loginForm"
              :model="form"
              :rules="rules"
              class="login-form"
              mode="block">
            <div class="wel">管理中心</div>
            <FormItem label="账号" prop="username">
              <template v-slot:label> 账号</template>
              <Input type="text" v-model="form.username" autocomplete="off" placeholder="请输入登录账号"/>
            </FormItem>
            <FormItem label="密码" prop="password">
              <Input type="password" v-model="form.password" autocomplete="off" @keyup.enter="submitForm" placeholder="请输入密码"/>
            </FormItem>
            <FormItem :showLabel="false">
              <Button :loading="loading" class="login-form-btn" color="primary" @click="submitForm">登 录</Button>
            </FormItem>
          </Form>
        </div>
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
      form: {
        username: null,
        password: null
      },
      rules: {
        required: ['username', 'password'],
      }
    }
  },
  methods: {
    submitForm() {
      let validResult = this.$refs.loginForm.valid();
      if (validResult.result) {
        this.loading = true;
        Login(this.form).then(({success}) => {
          if (success) {
            localStorage.setItem("admin_cache_username", this.form.username);
            message("登录成功~");
            window.location.replace("/");
          }
        }).finally(() => {
          this.loading = false;
        });
      }
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
<style scoped lang="less">

.login {
  background: url("@/assets/login-bg.jpg") no-repeat;
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background-size: cover;
  background-position: center;

  &-form {
    margin: 20px 20px 20px;

    &-btn {
      width: 100%;
      margin-top: 10px;
    }

    &-action {
      width: 100%;
      font-size: 12px;
      color: @primary-color;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
    }
  }

  .login-form {
    .login-form-btn {
      height: 40px;
    }
  }


  .img {
    width: 100%;
    position: absolute;
    left: 0;
    right: 0;
    margin: 0 auto;
    z-index: -1;
  }

  .wel {
    color: @primary-color;
    font-weight: bold;
    letter-spacing: 5px;
    font-size: 1.5rem;
    margin-bottom: 10px;
    text-align: center;
  }

  .bg1 {
    width: 100%;
    height: 237px;
    background: @primary-color;
    opacity: 0.6;
    position: absolute;
    left: 0;
    top: 0;
    right: 0;
    bottom: 0;
    margin: auto;
  }

  .bg {
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

    .form {
      width: 400px;
      background: #fff;
      box-shadow: 0 0 50px rgba(0, 0, 0, 0.4);
    }
  }

  .gyl {
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

  .gy2 {
    color: #fff;
    margin-left: 6px;
    font-size: 18px;
    text-align: center;
    margin-top: 10px;
  }

  .btn {
    position: absolute;
    top: 16rem;
    right: 2.5rem;
    border: none;
    color: #fff;
    width: 18.825rem;
    text-align: center;
    text-indent: 0;
  }
}
</style>
