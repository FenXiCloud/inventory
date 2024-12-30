/**
 * @功能描述: 初始化接口
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */

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
