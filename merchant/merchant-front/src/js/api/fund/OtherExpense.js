import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/otherExpense/list', param);
  },
  details(param) {
    return Ajax.get('/otherExpense/selectById', param);
  },
  remove(id) {
    return Ajax.post('/otherExpense/delete', id);
  },
  batchAudit(param) {
    return Ajax.post('/otherExpense/updateStatus', param);
  },
  addEdit(param) {
    return Ajax.post('/otherExpense/save', param);
  }
};
