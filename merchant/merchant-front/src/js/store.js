import {createStore} from 'vuex'
import {toArrayTree} from '@common/utils'
import {Init} from "@js/api/App";
import manba from "manba";

export default createStore({
  state: {
    siderCollapsed: false,
    user: {},
    menus: [],
    accountBooks: [],
    accountBook: {},
    granted: [],
    tabs: [],
    currentTab:  'DashboardMain',
    currentTabData: null
  },
  mutations: {
    updateMenus(state, data) {
      state.menus = data;
    },
    updateAccountBooks(state, {accountBooks}) {
      state.accountBooks = accountBooks;
      let accountBook = accountBooks.find(val=>val.current===true)
      if (accountBook) {
        accountBook.checkoutSDate = manba(accountBook.checkoutDate).add(1,manba.DAY).format("YYYY-MM-dd")
        state.accountBook = accountBook
      } else {
        // 兜底：后端 /init 已随账套下发数量/单价小数位（quantityDecimal/priceDecimal），无 current 时保持空对象
        console.warn('未找到当前账套');
      }
    },
    updateAccountBook(state, checkoutDate) {
      if(checkoutDate){
        state.accountBook.checkoutDate = checkoutDate
        state.accountBook.checkoutSDate = manba(checkoutDate).add(1,manba.DAY).format("YYYY-MM-dd")
      }
    },
    updateAccount(state, { account}) {
      state.user = account;
      state.granted = account.granted || [];
    },
    updateSiderCollapse(state, isShow) {
      state.siderCollapsed = isShow;
    },
    newTab(state, key) {
      state.currentTab = key;
    },
    updateTab(state, tab) {
      state.currentTab = tab;
    },
    pushTab(state, tab) {
      // 保存原始组件名用于渲染，key 用于 tab 去重和切换
      const component = tab.component || tab.key;
      const tabData = tab.params || state.currentTabData;
      // 新增类型：每次创建独立 tab，不与已有 tab 冲突
      if (tabData && tabData.type === 'add') {
        const uniqueKey = tab.key + '_add_' + Date.now();
        state.tabs.push({...tab, key: uniqueKey, component});
        state.currentTab = uniqueKey;
        return;
      }
      // 编辑类型：按 orderId 去重，同一订单复用同一 tab
      if (tabData && tabData.type === 'edit' && tabData.orderId) {
        const editKey = tab.key + '_edit_' + tabData.orderId;
        if (!state.tabs.some(val => String(val.key) === String(editKey))) {
          state.tabs.push({...tab, key: editKey, component});
        }
        state.currentTab = editKey;
        return;
      }
      // 其他类型：按 key 去重
      const key = tab.key;
      const existing = state.tabs.find(val => String(val.key) === String(key));
      if (!existing) {
        state.tabs.push({...tab, key, component});
      } else {
        // 同一组件可被多个菜单复用（占位页、红字发票/发票查询等）：
        // 复用已有 tab，并刷新标题/图标/menuId，保证菜单高亮跟随最后点击的菜单项
        if (tab.title != null) existing.title = tab.title;
        if (tab.icon != null) existing.icon = tab.icon;
        if (tab.menuId != null) existing.menuId = tab.menuId;
      }
      state.currentTab = key;
    },
    clearTabs(state) {
      state.tabs = [];
      state.currentTab = 'DashboardMain';
    },
    closeOtherTab(state, index) {
      state.tabs = index < 0 ? [] : [state.tabs[index]];
      state.currentTab = index < 0 ? 'DashboardMain' : state.tabs[0].key;
    },
    closeSelfTab(state, index) {
      state.currentTab = index - 1 > -1 ? state.tabs[index - 1].key : 'DashboardMain';
      state.tabs.splice(index, 1);
    },
    closeTabKey(state, key) {
      let index = state.tabs.findIndex(val => val.key === key);
      state.tabs.splice(index, 1);
      state.currentTab = index - 1 > -1 ? state.tabs[index - 1].key : 'DashboardMain';
    },
    SET_TAB_DATA(state, data) {
      state.currentTabData = data;
    }
  },
  actions: {
    init({commit}) {
      return new Promise((resolve, reject) => {
        Init().then(({success, data}) => {
          if (success) {
            commit('updateAccount', data);
            commit('updateAccountBooks', data);
            commit('updateMenus', toArrayTree(data.menus, {strict: true}));
            resolve(data.account);
          } else {
            reject();
          }
        }).catch(() => {
          reject();
        })
      })
    },
    updateSiderCollapse({commit}, data) {
      commit('updateSiderCollapse', data);
    }
  },
  getters: {
    account: state => {
      return state.user;
    },
    siderCollapsed: state => {
      return state.siderCollapsed;
    }
  }
})
