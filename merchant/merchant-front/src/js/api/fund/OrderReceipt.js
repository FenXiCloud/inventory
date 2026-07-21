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
    return Ajax.get('/orderReceipt', param);
  },
  total(param) {
    return Ajax.get('/orderReceipt/total', param);
  },
  load(id) {
    return Ajax.get('/orderReceipt/load/' + id);
  },
  remove(id) {
    return Ajax.delete('/orderReceipt/' + id);
  },
  approved(state, ids) {
    return Ajax.post('/orderReceipt/approved/' + state, toIds(ids));
  },
  save(param) {
    return Ajax[param.orderReceipt?.id ? 'put' : 'post']('/orderReceipt', param);
  },
  writeOff(param) {
    return Ajax.get('/orderReceipt/writeOff', param);
  }
};
