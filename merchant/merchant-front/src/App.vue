<template>
  <AppFrame v-if="user.merchantId" />
  <DDLogin v-else-if="!showLogin" />
  <Login v-else />
</template>
<script>
import AppFrame from '@views/app/AppFrame';
import { mapState } from 'vuex';
import Login from './Login';
import * as dd from 'dingtalk-jsapi';

export default {
  components: { Login, AppFrame },
  data() {
    return {
      showLogin: true
    };
  },
  computed: {
    ...mapState(['user'])
  },
  created() {
    if (dd.env.platform == 'notInDingTalk') {
      this.showLogin = true;
    } else {
      this.showLogin = false;
    }
  }
};
</script>
