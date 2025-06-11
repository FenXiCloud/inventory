import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/orderReceipt/list', param);
  },
  details(param) {
    return Ajax.get('/orderReceipt/selectById', param);
  },
  remove(id) {
    return Ajax.post('/orderReceipt/delete', id);
  },
  batchAudit(param) {
    return Ajax.post('/orderReceipt/updateStatus', param);
  },
  addEdit(param) {
    return Ajax.post('/orderReceipt/save', param);
  },
  writeOffTheOrder(param) {
    return Ajax.get(`/orderReceipt/writeOffTheOrder`, param);
  },

  orderStaffList(param) {
    return Ajax.get('/orderStaff/list', param);
  },
  orderStaffAdd(param) {
    return Ajax.post('/orderStaff/add', param);
  }
};
