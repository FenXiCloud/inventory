import Ajax from '@common/Request';

export default {
  register(form) {
    return Ajax.post('/productSerial', form);
  },
  list(params) {
    return Ajax.get('/productSerial', params);
  },
  outbound(form) {
    return Ajax.put('/productSerial/outbound', form);
  },
  scrap(ids) {
    return Ajax.put('/productSerial/scrap', ids);
  },
  remove(id) {
    return Ajax.delete('/productSerial/' + id);
  }
};
