import Ajax from '@common/Request';
import jsonToFormData from '@ajoelp/json-to-formdata';

export default {
  save(param) {
    return Ajax[param.id ? 'put' : 'post']('/admin', param);
  },
  list(param) {
    return Ajax.get('/admin', param);
  },
  addUserByDingDing(param) {
    return Ajax.get('/admin/addUserByDingDing', param);
  },
  syncProgress() {
    return Ajax.get('/admin/dingDingSyncProgress', null, {repeatable: true});
  },
  remove(adminId) {
    return Ajax.delete('/admin/' + adminId);
  },
  updatePassword(param) {
    return Ajax.put('/admin/update/password', jsonToFormData(param));
  },
  resetPassword(adminId) {
    return Ajax.put('/admin/reset/password/' + adminId);
  }
};
