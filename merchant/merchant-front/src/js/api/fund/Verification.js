import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/verification/list', param);
  },
  details(param) {
    return Ajax.get('/verification/selectById', param);
  },
  remove(id) {
    return Ajax.post('/verification/delete', id);
  },
  batchAudit(param) {
    return Ajax.post('/verification/updateStatus', param);
  },
  addEdit(param) {
    return Ajax.post('/verification/save', param);
  },
  writeOffTheOrder(param) {
    return Ajax.get(`/verification/writeOffTheOrder`, param);
  },

  orderStaffList(param) {
    return Ajax.get('/orderStaff/list', param);
  },
  orderStaffAdd(param) {
    return Ajax.post('/orderStaff/add', param);
  }
};
