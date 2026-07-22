import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/supplier', param)
	},
	list(param) {
		return Ajax.get('/supplier', param)
	},
	remove(id) {
		return Ajax.delete('/supplier/' + id);
	},
	select(param) {
		return Ajax.get('/supplier/select', param)
	},
	selectProduct(id) {
        return Ajax.get('/supplier/product/select/' + id)
	}
}
