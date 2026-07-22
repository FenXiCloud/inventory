import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.product?.id ? 'put' : 'post']('/product', param)
	},
	list(param) {
		return Ajax.get('/product', param)
	},
	remove(id) {
		return Ajax.delete('/product/' + id);
	},
	load(id) {
		return Ajax.get('/product/load/' + id);
	},
	select(param) {
		return Ajax.get('/product/select', param)
	},
	update(param) {
		return Ajax.put('/product', {product: param})
	},
    customerLevelPrice(productId) {
        return Ajax.get('/product/customerLevel/price/' + productId)
    },
}
