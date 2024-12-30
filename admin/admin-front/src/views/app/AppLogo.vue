<template>
  <div class="app-logo">
    <a aria-current="page" href="/" class="router-link-active router-link-exact-active link">
      <span class="app-logo-icon"></span>
      <span class="app-logo-text">纷析云</span>
    </a>
  </div>
</template>

<script>
/**
 * @功能描述: LOGO
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AppLogo"
}
</script>

<style scoped lang="less">
.app-logo {
  font-family: Futura, Helvetica Neue For Number, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, PingFang SC, Hiragino Sans GB, Microsoft YaHei, Helvetica Neue, Helvetica, Arial, sans-serif;
  padding: 0 24px;
  margin-bottom: 30px;
  line-height: 60px;
  height: 60px;
  overflow: hidden;
  white-space: nowrap;
  -webkit-transition: padding .3s;
  transition: padding .3s;

  .app-logo .app-logo-text {
    font-size: 20px;
    vertical-align: middle;
  }

  .app-logo-icon {
    background: url("../../assets/logo.png");
    background-size: contain;
    height: 26px;
    width: 26px;
    display: inline-block;
    margin-right: 10px;
    -webkit-transition: .3s;
    transition: .3s;
    vertical-align: middle;
  }
}

.link {
  color: #3788ee;
  cursor: pointer;
  text-decoration: none;
  outline: none;
  -webkit-transition: color .2s ease;
  transition: color .2s ease;
}
</style>
