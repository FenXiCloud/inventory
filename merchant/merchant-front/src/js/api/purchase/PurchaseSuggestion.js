import Ajax from "@common/Request";

export default {
	salesDriven(param) {
		return Ajax.get('/purchaseSuggestion/sales-driven', param)
	},
	replenishment(param) {
		return Ajax.get('/purchaseSuggestion/replenishment', param)
	},
	generate(param) {
		return Ajax.post('/purchaseSuggestion/generate', param)
	},
}
