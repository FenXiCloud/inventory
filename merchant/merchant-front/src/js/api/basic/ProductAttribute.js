import Ajax from "@common/Request";

export default {
	list(param) {
		return Ajax.get('/productAttribute', param)
	},
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/productAttribute', param)
	},
	remove(id) {
		return Ajax.delete('/productAttribute/' + id);
	},
}
