<template>
  <AppFrame v-if="user.merchantId"/>
  <Login v-else/>
</template>
<script>
import AppFrame from "@views/app/AppFrame";
import {mapState} from "vuex";
import Login from "./Login";

export default {
  components: {Login, AppFrame},
  computed: {
    ...mapState(['user'])
  }
}
</script>
