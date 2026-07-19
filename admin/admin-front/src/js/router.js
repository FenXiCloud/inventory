import {createRouter, createWebHistory} from 'vue-router'
import {LoadingPlugin} from "tdesign-vue-next";

const routes = [
	{
		path: '/login',
		name: 'Login',
		component: () => import('@/views/Login'),
		meta: {title: '登录'}
	},
	{
		path: '/',
		name: 'AppFrame',
		component: () => import('@/views/app/AppFrame'),
		redirect: '/merchant',
		children: [
			{
				path: 'merchant',
				name: 'MerchantList',
				component: () => import('@/views/merchant/MerchantList'),
				meta: {title: '商户管理'}
			},
			{
				path: 'menu',
				name: 'MenuList',
				component: () => import('@/views/menu/MenuList'),
				meta: {title: '菜单管理'}
			},
			{
				path: 'user',
				name: 'UserList',
				component: () => import('@/views/user/UserList'),
				meta: {title: '账号管理'}
			},
			{
				path: 'account',
				name: 'AccountBasic',
				component: () => import('@/views/common/AccountBasic'),
				meta: {title: '个人信息'}
			}
		]
	}, {
		path: '/permission/error',
		name: 'PermissionError',
		component: () => import('@/views/common/PermissionError')
	}
]

const router = createRouter({
	history: createWebHistory(process.env.BASE_URL),
	routes
})

router.beforeEach((to, from, next) => {
	LoadingPlugin(true);
	if (to.meta && to.meta.title) {
		document.title = to.meta.title + ' - 进销存';
	} else {
		document.title = '进销存';
	}
	next();
});

router.afterEach(() => {
	LoadingPlugin(false);
	document.documentElement.scrollTop = 0;
	document.body.scrollTop = 0;
	let layoutContent = document.querySelector('.t-layout__content');
	if (layoutContent) {
		layoutContent.scrollTop = 0;
	}
});

export default router
