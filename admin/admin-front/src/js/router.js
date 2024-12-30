import {createRouter, createWebHistory} from 'vue-router'
import {loadingBar} from "heyui.ext";

const routes = [
	{
		path: '/login',
		name: 'Login',
		component: () => import('@components/Login'),
		meta: {title: '登录'}
	},
	{
		path: '/',
		name: 'AppFrame',
		component: () => import('@components/app/AppFrame'),
		children: [
			{
				path: '',
				name: 'DashboardMain',
				component: () => import('@components/dashboard/DashboardMain'),
				meta: {title: '桌面', icon: 'icon-monitor'}
			},
			{
				path: 'merchant',
				name: 'MerchantList',
				component: () => import('@components/merchant/MerchantList'),
				meta: {title: '商户管理', icon: 'icon-monitor'}
			},
			{
				path: 'menu',
				name: 'MenuList',
				component: () => import('@components/menu/MenuList'),
				meta: {title: '菜单管理', icon: 'icon-monitor'}
			},
			{
				path: 'user',
				name: 'UserList',
				component: () => import('@components/user/UserList'),
				meta: {title: '管理员管理', icon: 'icon-monitor'}
			},
			{
				path: 'wechat/setting',
				name: 'WechatSetting',
				component: () => import('@components/wechat/WechatSetting'),
				meta: {title: '微信设置', icon: 'icon-monitor'}
			},
			{
				path: 'account',
				name: 'AccountBasic',
				component: () => import('@components/common/AccountBasic'),
				meta: {title: '账号信息', icon: 'icon-monitor'}
			},
		]
	}, {
		path: '/permission/error',
		name: 'PermissionError',
		component: () => import('@components/common/PermissionError')
	}
]

const router = createRouter({
	history: createWebHistory(process.env.BASE_URL),
	routes
})

let isFirstRouter = true;

router.beforeEach((to, from, next) => {
	loadingBar.start();
	if (to.meta && to.meta.title) {
		document.title = to.meta.title + ' - 进销存';
	} else {
		document.title = '进销存';
	}
	isFirstRouter = false;
	next();
});

router.afterEach(() => {
	loadingBar.success();
	document.documentElement.scrollTop = 0;
	document.body.scrollTop = 0;
	let layoutContent = document.querySelector('.h-layout-content');
	if (layoutContent) {
		layoutContent.scrollTop = 0;
	}
});

export default router
