/**
 * @功能描述: 商品管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax.post('/product', param)
	},
	list(param) {
		return Ajax.get('/product', param)
	},
	remove(id) {
		return Ajax.delete('/product/' + id);
	},
	load(id) {
		return Ajax.get('/product/get/' + id);
	},
	select(param) {
		return Ajax.get('/product/select', param)
	},
	// loadToOrder(param) {
	// 	return Ajax.get('/product/loadTo/order', param)
	// },
    customerLevelPrice(productId) {
        return Ajax.get('/product/customerLevel/price/' + productId)
    },
	// goodsPrice(customersId) {
	// 	return Ajax.get('/product/goods/price/' + customersId)
	// },
}

