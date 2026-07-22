import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/customerLevel', param)
	},
	list(param) {
		return Ajax.get('/customerLevel', param)
	},
    delete(customerLevelId) {
		return Ajax.delete('/customerLevel/' + customerLevelId);
	},
	select(param) {
		return Ajax.get('/customerLevel/select', param)
	},
}
