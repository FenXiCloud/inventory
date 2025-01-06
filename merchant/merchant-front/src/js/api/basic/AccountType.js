import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/accountType', param)
	},
	list(param) {
		return Ajax.get('/accountType', param)
	},
	remove(id) {
		return Ajax.delete('/accountType/' + id);
	},
	select(param) {
		return Ajax.get('/accountType/select', param)
	}
}
