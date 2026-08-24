import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/customer', param)
	},
	updateCreditLimit(param) {
		return Ajax.put('/customer/credit-limit', param)
	},
	list(param) {
		return Ajax.get('/customer', param)
	},
	delete(adminId) {
		return Ajax.delete('/customer/' + adminId);
	},
	select(param) {
		return Ajax.get('/customer/select', param)
	},
	selectProduct(id) {
		return Ajax.get('/customer/product/select/' + id)
	},
	importData(formData) {
		return Ajax.post('/customer/importData', formData, {'Content-Type': 'multipart/form-data', repeatable: true});
	},
	exportToExcel(params) {
		return Ajax.get("/customer/exportToExcel", params, {responseType: 'blob'})
	},
}
