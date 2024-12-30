/**
 * @功能描述: 门店账号需要控制权限
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import store from "@/js/store";

export default {
  mounted(el, binding) {
    //门店账号需要控制权限
    if (store.state.user.role.accountBookId) {
      const {value} = binding
      if (value && value instanceof Array && value.length > 0) {
        const hasPermission = store.state.granted.some(role => {
          return value.includes(role)
        })
        if (!hasPermission) {
          el.parentNode && el.parentNode.removeChild(el)
        }
      } else if (value && typeof value === 'string') {
        if (!store.state.granted.includes(value)) {
          el.parentNode && el.parentNode.removeChild(el);
        }
      } else {
        throw new Error(`need permission! Like v-auth="['create','editor']" or v-auth="'create'"`)
      }
    }
  }
}