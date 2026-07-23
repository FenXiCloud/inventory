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
      accountBook.checkoutSDate = manba(accountBook.checkoutDate).add(1,manba.DAY).format("YYYY-MM-dd")
      state.accountBook = accountBook
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
      const tabData = state.currentTabData;
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
      if (!state.tabs.some(val => String(val.key) === String(key))) {
        state.tabs.push({...tab, key, component});
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
