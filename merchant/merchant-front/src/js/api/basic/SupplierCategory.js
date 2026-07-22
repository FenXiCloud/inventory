import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/supplierCategory', param)
	},
	list(param) {
		return Ajax.get('/supplierCategory', param)
	},
	delete(vendorsCategoryId) {
		return Ajax.delete('/supplierCategory/' + vendorsCategoryId);
	},
	select(param) {
		return Ajax.get('/supplierCategory/select', param)
	},
}
