import Ajax from '@common/Request';
import jsonToFormData from '@ajoelp/json-to-formdata';

/**
 * 初始化获取用户信息
 * @constructor
 */
export const Init = () => {
  return Ajax.get('/init');
};

export const HomeView = () => {
  return Ajax.get('/home/view');
};

export const Login = (formData) => {
  return Ajax.post('/login', jsonToFormData(formData));
};
export const loginByBumer = (number) => {
  return Ajax.post('/financial/login', jsonToFormData(number));
};

export const DDLogin = (param) => {
  return Ajax.post('/dd/auth', param);
};

export const Logout = () => {
  return Ajax.get('/logout');
};

export const Upload = (type, formData) => {
  return Ajax.post(`/upload/${type}`, formData, {
    'Content-Type': 'multipart/form-data',
    repeatable: true
  });
};

export const OssUpload = (type, formData) => {
  return Ajax.post(`/oss/upload/${type}`, formData, {
    'Content-Type': 'multipart/form-data',
    repeatable: true
  });
};
