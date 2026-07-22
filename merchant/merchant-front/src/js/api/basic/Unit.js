import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/unit', param)
	},
	list(param) {
		return Ajax.get('/unit', param)
	},
    delete(unitId) {
		return Ajax.delete('/unit/' + unitId);
	},
	select(param) {
		return Ajax.get('/unit/select', param)
	},
}
