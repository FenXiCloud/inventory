
import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/account', param)
	},
	list(param) {
		return Ajax.get('/account', param)
	},
	remove(id) {
		return Ajax.delete('/account/' + id);
	},
	select(param) {
		return Ajax.get('/account/select', param)
	}
}
