import Ajax from '@common/Request';

function toIds(ids) {
  if (Array.isArray(ids)) {
    return ids.map(Number).filter(Boolean);
  }
  return String(ids ?? '')
    .split(',')
    .map((s) => Number(s.trim()))
    .filter(Boolean);
}

export default {
  list(param) {
    return Ajax.get('/orderPayment', param);
  },
  total(param) {
    return Ajax.get('/orderPayment/total', param);
  },
  load(id) {
    return Ajax.get('/orderPayment/load/' + id);
  },
  remove(id) {
    return Ajax.delete('/orderPayment/' + id);
  },
  approved(state, ids) {
    return Ajax.post('/orderPayment/approved/' + state, toIds(ids));
  },
  save(param) {
    return Ajax[param.orderPayment?.id ? 'put' : 'post']('/orderPayment', param);
  },
  writeOff(param) {
    return Ajax.get('/orderPayment/writeOff', param);
  }
};
