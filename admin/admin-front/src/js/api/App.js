import Ajax from "@common/Request";
import jsonToFormData from '@ajoelp/json-to-formdata';

/**
 * 初始化获取用户信息
 * @constructor
 */
export const Init = () => {
	return Ajax.get("/init");
}

/**
 * 登录
 * @constructor
 */
export const Login = (formData) => {
	return Ajax.post("/login", jsonToFormData(formData));
}

/**
 * 登出
 * @constructor
 */
export const Logout = () => {
	return Ajax.get("/logout");
}

/**
 * 注册
 * @constructor
 */
export const Registration = (entity) => {
	return Ajax.post("/registration", jsonToFormData(entity));
}

export const FileUpload = (type, formData) => {
	return Ajax.post("/upload/" + type, formData);
}
