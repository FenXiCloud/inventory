
import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/settlementMethod', param)
	},
	list(param) {
		return Ajax.get('/settlementMethod', param)
	},
	remove(id) {
		return Ajax.delete('/settlementMethod/' + id);
	},
	select(param) {
		return Ajax.get('/settlementMethod/select', param)
	}
}
