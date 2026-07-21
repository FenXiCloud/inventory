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
    return Ajax.get('/otherReceipt', param);
  },
  total(param) {
    return Ajax.get('/otherReceipt/total', param);
  },
  load(id) {
    return Ajax.get('/otherReceipt/load/' + id);
  },
  remove(id) {
    return Ajax.delete('/otherReceipt/' + id);
  },
  approved(state, ids) {
    return Ajax.post('/otherReceipt/approved/' + state, toIds(ids));
  },
  save(param) {
    return Ajax[param.order?.id ? 'put' : 'post']('/otherReceipt', param);
  }
};
