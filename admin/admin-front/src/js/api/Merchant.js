import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/merchant', param)
	},
	list(param) {
		return Ajax.get('/merchant', param)
	},
	remove(id) {
		return Ajax.delete('/merchant/' + id);
	},
	select(param) {
		return Ajax.get('/merchant/select',param);
	}
}
