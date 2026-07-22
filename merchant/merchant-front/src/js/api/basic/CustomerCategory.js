import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/customerCategory', param)
	},
	list(param) {
		return Ajax.get('/customerCategory', param)
	},
	remove(customerCategoryId) {
		return Ajax.delete('/customerCategory/' + customerCategoryId);
	},
	select(param) {
		return Ajax.get('/customerCategory/select', param)
	},
}
