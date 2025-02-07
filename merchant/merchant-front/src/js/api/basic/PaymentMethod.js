import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/paymentMethod', param)
	},
	list(param) {
		return Ajax.get('/paymentMethod', param)
	},
    delete(id) {
		return Ajax.delete('/paymentMethod/' + id);
	},
	select(param) {
		return Ajax.get('/paymentMethod/select', param)
	}
}
