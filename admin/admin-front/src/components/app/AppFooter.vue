<template>
  <div class="app-footer">
<!--    Copyright © {{ nowYear }} -->
  </div>
</template>

<script>
/**
 * @功能描述: 底部
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AppFooter",
  data() {
    return {
      nowYear: new Date().getFullYear()
    }
  },
}
</script>

<style scoped lang="less">
.app-footer {
  text-align: center;
  padding: 10px 0 30px;
  color: rgba(0, 0, 0, 0.45);

  a {
    color: rgba(0, 0, 0, 0.45);
  }
}
</style>
