import Ajax from "@common/Request";
import jsonToFormData from '@ajoelp/json-to-formdata';

export default {
	save(param) {
		return Ajax[param.id ? 'put' : 'post']('/merchant/user', param)
	},
	list(param) {
		return Ajax.get('/merchant/user', param)
	},
	remove(adminId) {
		return Ajax.delete('/merchant/user/' + adminId);
	},
	updatePassword(param) {
		return Ajax.put("/merchant/user/update/password", jsonToFormData(param))
	},
	resetPassword(adminId) {
		return Ajax.put("/merchant/user/reset/password/" + adminId)
	}
}
