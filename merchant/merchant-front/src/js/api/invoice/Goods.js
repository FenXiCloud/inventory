import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/invoice/goods', param);
  },
  get(id) {
    return Ajax.get(`/invoice/goods/${id}`);
  },
  create(data) {
    return Ajax.post('/invoice/goods', data);
  },
  update(id, data) {
    return Ajax.put(`/invoice/goods/${id}`, data);
  },
  remove(id) {
    return Ajax.delete(`/invoice/goods/${id}`);
  }
};
