import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/orderStaff', param);
  },
  select(param) {
    return Ajax.get('/orderStaff/select', param);
  },
  load(id) {
    return Ajax.get('/orderStaff/load/' + id);
  },
  save(param) {
    return Ajax[param.id ? 'put' : 'post']('/orderStaff', param);
  },
  remove(id) {
    return Ajax.delete('/orderStaff/' + id);
  }
};
