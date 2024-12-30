import {createStore} from 'vuex'
import { getMenus } from '@js/config/menu-config';

export default createStore({
	state: {
		siderCollapsed: false,
		user: {},
		menus: [],
		currentTab: 'DashboardMain',
	},
	mutations: {
		updateMenus(state, data) {
			state.menus = getMenus(data);
			state.menuKeys = data;
		},
		updateAccount(state, data) {
			state.user = data;
		},
		updateSiderCollapse(state, isShow) {
			setTimeout(() => {
				state.pageResizeCount += 1;
			}, 600);
			state.siderCollapsed = isShow;
		},
		updateMsgCount(state, data) {
			state.msgCount = data;
		}
	},
	actions: {
		updateMenuKeys({commit}, data) {
			commit('updateMenus', data);
		},
		updateAccount({commit}, data) {
			commit('updateAccount', data);
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
