import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/accountTransfer/list', param);
  },
  details(param) {
    return Ajax.get('/accountTransfer/selectById', param);
  },
  remove(id) {
    return Ajax.post('/accountTransfer/delete', id);
  },
  batchAudit(param) {
    return Ajax.post('/accountTransfer/updateStatus', param);
  },
  addEdit(param) {
    return Ajax.post('/accountTransfer/save', param);
  }
};
