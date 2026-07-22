<template>
  <div class="app-logo">
    <a aria-current="page" href="/" class="router-link-active router-link-exact-active link">
      <span class="app-logo-icon"></span>
      <span class="app-logo-text">纷析云</span>
    </a>
  </div>
</template>

<script>
export default {
  name: "AppLogo"
}
</script>

<style scoped>
.app-logo {
  font-family: Futura, Helvetica Neue For Number, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, PingFang SC, Hiragino Sans GB, Microsoft YaHei, Helvetica Neue, Helvetica, Arial, sans-serif;
  padding: 0 24px;
  line-height: 60px;
  height: 60px;
  overflow: hidden;
  white-space: nowrap;
  transition: padding .3s;
}

.app-logo .app-logo-text {
  font-size: 20px;
  vertical-align: middle;
}

.app-logo .app-logo-icon {
  background: url("../../assets/logo.png");
  background-size: contain;
  height: 26px;
  width: 26px;
  display: inline-block;
  margin-right: 10px;
  transition: .3s;
  vertical-align: middle;
}

.link {
  color: #3788ee;
  cursor: pointer;
  text-decoration: none;
  outline: none;
  transition: color .2s ease;
}
</style>
