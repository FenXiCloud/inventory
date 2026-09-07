import Ajax from "@common/Request";

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/accountType', param)
	},
	list(param) {
		return Ajax.get('/accountType', param)
	},
	remove(id) {
		return Ajax.delete('/accountType/' + id);
	},
	select(param) {
		return Ajax.get('/accountType/select', param)
	},
	/**
	 * 按收支类别查询类别列表
	 * @param {string} costType - 收支类别：'收入' 或 '支出'
	 * @param {object} param - 其他查询参数
	 */
	listByType(costType, param = {}) {
		return Ajax.get('/accountType', { ...param, costType });
	}
}
