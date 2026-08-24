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
	quickCreate(param) {
		return Ajax.post('/product/quickCreate', param)
	},
	update(param) {
		return Ajax.put('/product', {product: param})
	},
    customerLevelPrice(productId) {
        return Ajax.get('/product/customerLevel/price/' + productId)
    },
    importData(formData) {
        return Ajax.post('/product/importData', formData)
    },
    brands() {
        return Ajax.get('/product/brands')
    },
    renameBrand(param) {
        return Ajax.put('/product/brand', param)
    },
    deleteBrand(brand) {
        return Ajax.delete('/product/brand?brand=' + encodeURIComponent(brand))
    },
}
