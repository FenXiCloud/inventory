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
    return Ajax.get('/accountTransfer', param);
  },
  total(param) {
    return Ajax.get('/accountTransfer/total', param);
  },
  load(id) {
    return Ajax.get('/accountTransfer/load/' + id);
  },
  remove(id) {
    return Ajax.delete('/accountTransfer/' + id);
  },
  approved(state, ids) {
    return Ajax.post('/accountTransfer/approved/' + state, toIds(ids));
  },
  save(param) {
    return Ajax[param.order?.id ? 'put' : 'post']('/accountTransfer', param);
  }
};
