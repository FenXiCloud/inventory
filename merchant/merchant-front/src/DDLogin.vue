<template>
  <div class="login-container">
    <el-card class="box-card">
      <div class="login-content">
        <div class="loading">
          <div slot="default" class="loading-spinner">
            <i class="el-icon-loading"></i>
            正在检测登录状态...
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
// import { getToken } from '@/utils/auth';
import { DDLogin } from '@js/api/App';
import * as dd from 'dingtalk-jsapi';
export default {
  name: 'DDLogin',
  data() {
    return {
      isLoading: true,
      result: '1',
      corpId: 1222221,
      form: {
        username: null,
        password: null
      }
      // token: getToken()
    };
  },
  created() {
    console.log(1);
    // 获取图形验证码
    this.tologin();
  },
  methods: {
    //获取Code
    tologin() {
      // 加上判断条件
      // if (dd.env.platform == 'notInDingTalk') {
      //   console.log(this.$router, '不在钉钉中');
      //   this.$router.push({ name: 'Login' }).catch(() => {});
      // }
      //获取入参
      // let corpId = this.$route.query.corpId;
      // 获取 URL 查询参数
      const search = window.location.search;
      const params = new URLSearchParams(search);
      let corpId = params.get('corpId');
      // console.log(corpId);

      if (!corpId) {
        corpId = window.localStorage.getItem('cropId');
      } else {
        window.localStorage.setItem('cropId', corpId); // 缓存以便下次使用
      }

      this.corpId = corpId;
      let that = this;

      dd.runtime.permission.requestAuthCode({
        corpId: corpId,
        onSuccess: function (result) {
          DDLogin({
            authCode: result.code,
            corpId: corpId
          })
            .then(({ success, data: { account } }) => {
              if (success) {
                // message('登录成功~');
                // localStorage.setItem('m_cache_username', this.form.username);
                window.location.replace('/');
              }
            })
            .finally(() => {
              // this.loading = false;
            });
        },
        onFail: function (err) {
          that.$message.error(err);
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

.box-card {
  width: 400px;
}

.login-content {
  text-align: center;
}

.loading-spinner {
  font-size: 24px;
  color: #409eff;
  /* Element UI 的主题蓝色 */
}
</style>
