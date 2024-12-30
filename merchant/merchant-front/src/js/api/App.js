/**
 * @功能描述: 初始化js
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

export const HomeView = () => {
  return Ajax.get("/home/view");
}

export const Login = (formData) => {
  return Ajax.post("/login", jsonToFormData(formData));
}

export const Logout = () => {
  return Ajax.get("/logout");
}

export const Upload = (type, formData) => {
  return Ajax.post(`/upload/${type}`, formData, {'Content-Type': 'multipart/form-data', repeatable: true});
}


export const OssUpload = (type, formData) => {
  return Ajax.post(`/oss/upload/${type}`, formData, {'Content-Type': 'multipart/form-data', repeatable: true});
}

