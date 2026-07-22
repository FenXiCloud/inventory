import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/role', param)
	},
	list(param) {
		return Ajax.get('/role', param)
	},
	simpleList(param) {
		return Ajax.get('/role/simple', param)
	},
	remove(roleId) {
		return Ajax.delete('/role/' + roleId);
	},
	getMenuRole(roleId) {
		return Ajax.get('/role/grant/menu/' + roleId)
	},
	saveMenuRole(roleId, menus = {}) {
		return Ajax.post('/role/grant/' + roleId, menus)
	},

}
