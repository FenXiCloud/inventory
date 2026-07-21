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
    return Ajax.get('/verification', param);
  },
  total(param) {
    return Ajax.get('/verification/total', param);
  },
  load(id) {
    return Ajax.get('/verification/load/' + id);
  },
  remove(id) {
    return Ajax.delete('/verification/' + id);
  },
  approved(state, ids) {
    return Ajax.post('/verification/approved/' + state, toIds(ids));
  },
  save(param) {
    return Ajax[param.order?.id ? 'put' : 'post']('/verification', param);
  }
};
