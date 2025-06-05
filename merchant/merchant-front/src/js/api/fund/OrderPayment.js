import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/orderPayment/list', param);
  },
  details(param) {
    return Ajax.get('/orderPayment/selectById', param);
  },
  remove(id) {
    return Ajax.post('/orderPayment/delete', id);
  },
  batchAudit(param) {
    return Ajax.post('/orderPayment/updateStatus', param);
  },
  addEdit(param) {
    return Ajax.post('/orderPayment/save', param);
  },
  writeOffTheOrder(param) {
    return Ajax.get(`/orderPayment/writeOffTheOrder`, param);
  },

  orderStaffList(param) {
    return Ajax.get('/orderStaff/list', param);
  },
  orderStaffAdd(param) {
    return Ajax.post('/orderStaff/add', param);
  }
};
