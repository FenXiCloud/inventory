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
      const key = tab.key;
      if (!state.tabs.some(val => String(val.key) === String(key))) {
        state.tabs.push({...tab, key});
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
