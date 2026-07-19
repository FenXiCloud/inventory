<template>
  <div class="login-container">
    <div class="login-card">
      <t-loading :loading="true" text="正在检测登录状态..." />
    </div>
  </div>
</template>

<script>
import { DDLogin } from '@js/api/App';
import { MessagePlugin } from 'tdesign-vue-next';
import * as dd from 'dingtalk-jsapi';

export default {
  name: 'DDLogin',
  data() {
    return {
      corpId: null
    };
  },
  created() {
    this.tologin();
  },
  methods: {
    tologin() {
      const params = new URLSearchParams(window.location.search);
      let corpId = params.get('corpId');

      if (!corpId) {
        corpId = window.localStorage.getItem('cropId');
      } else {
        window.localStorage.setItem('cropId', corpId);
      }

      this.corpId = corpId;

      dd.runtime.permission.requestAuthCode({
        corpId: corpId,
        onSuccess: (result) => {
          DDLogin({
            authCode: result.code,
            corpId: corpId
          }).then(({ success }) => {
            if (success) {
              window.location.replace('/');
            }
          });
        },
        onFail: (err) => {
          MessagePlugin.error(typeof err === 'string' ? err : '钉钉登录失败');
        }
      });
    }
  }
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f0f0;
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  text-align: center;
}
</style>
