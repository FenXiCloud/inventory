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