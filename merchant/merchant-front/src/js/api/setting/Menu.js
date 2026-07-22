import Ajax from "@common/Request";

export default {
	list(param) {
		return Ajax.get('/menu', param)
	},
	merchantMenu(menuGroup) {
		return Ajax.get('/menu/merchant', {menuGroup})
	},
}
