import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/otherIncome/list', param);
  },
  details(param) {
    return Ajax.get('/otherIncome/selectById', param);
  },
  remove(id) {
    return Ajax.post('/otherIncome/delete', id);
  },
  batchAudit(param) {
    return Ajax.post('/otherIncome/updateStatus', param);
  },
  addEdit(param) {
    return Ajax.post('/otherIncome/save', param);
  },
  select(param) {
    return Ajax.get('/otherIncome/select', param);
  }
};
