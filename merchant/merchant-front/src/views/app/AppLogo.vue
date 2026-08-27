<template>
  <div class="app-logo">
    <a aria-current="page" href="/public" class="router-link-active router-link-exact-active link">
      <span class="app-logo-text">菜鸟进销存</span>
    </a>
  </div>
</template>

<script>
export default {
  name: "AppLogo"
}
</script>

<style scoped lang="less">
.app-logo {
  font-family: Futura, Helvetica Neue For Number, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, PingFang SC, Hiragino Sans GB, Microsoft YaHei, Helvetica Neue, Helvetica, Arial, sans-serif;
  padding: 0 5px;
  margin-bottom: 0;
  line-height: 60px;
  height: 60px;
  overflow: hidden;
  white-space: nowrap;
  -webkit-transition: padding .3s;
  transition: padding .3s;
  background: var(--td-menu-dark-bg-color, #242424);
  .app-logo-text {
    color: #fff;
    font-size: 18px;
    font-weight: 700;
    vertical-align: middle;
    letter-spacing: 2px;
  }
}

.link {
  color: #FFF;
  cursor: pointer;
  text-decoration: none;
  outline: none;
  -webkit-transition: color .2s ease;
  transition: color .2s ease;
}
</style>
