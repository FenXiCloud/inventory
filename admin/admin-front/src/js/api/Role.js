import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/role', param)
	},
	list(param) {
		return Ajax.get('/role', param)
	},
	simpleList(params) {
		return Ajax.get('/role/simple', params)
	},
	remove(roleId) {
		return Ajax.delete('/role/' + roleId);
	},
	getMenuRole(roleId) {
		return Ajax.get('/role/grant/' + roleId);
	},
	roleGrant(roleId, grantMenuIds) {
		return Ajax.post('/role/grant/' + roleId, grantMenuIds);
	},
}
