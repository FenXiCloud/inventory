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
    return Ajax.get('/otherExpense', param);
  },
  total(param) {
    return Ajax.get('/otherExpense/total', param);
  },
  load(id) {
    return Ajax.get('/otherExpense/load/' + id);
  },
  remove(id) {
    return Ajax.delete('/otherExpense/' + id);
  },
  approved(state, ids) {
    return Ajax.post('/otherExpense/approved/' + state, toIds(ids));
  },
  save(param) {
    return Ajax[param.order?.id ? 'put' : 'post']('/otherExpense', param);
  }
};
