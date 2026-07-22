import {clone} from '@common/utils';

const fullMenus = [
	{
		title: '商户管理',
		key: 'MerchantList',
		icon: 'usergroup'
	},
	{
		title: '菜单管理',
		key: 'MenuList',
		icon: 'star'
	},
	{
		title: '账号管理',
		key: 'UserList',
		icon: 'user'
	}
];

const getMenus = function (menuIdList = []) {
	return getAuthMenu(fullMenus, menuIdList);
};

let getAuthMenu = (menus, menuIdList) => {
	let configMenu = [];
	for (let menu of menus) {
		let m = clone(menu, true);
		if (menuIdList.indexOf(m.key) > -1) {
			configMenu.push(m);
		}
		if (menu.children && menu.children.length) {
			m.children = getAuthMenu(menu.children, menuIdList);
		}
	}
	return configMenu;
};

const getKeys = function (menus) {
	let keys = [];
	for (let menu of menus) {
		keys.push(menu.key);
		if (menu.children && menu.children.length) {
			keys.push(...getKeys(menu.children));
		}
	}
	return keys;
};

let fullMenuKeys = getKeys(fullMenus);

const isAuthPage = function (menus, name) {
	if (fullMenuKeys.indexOf(name) > -1 && menus.indexOf(name) == -1) {
		return false;
	}
	return true;
};

export {getMenus, fullMenus, fullMenuKeys, isAuthPage};
