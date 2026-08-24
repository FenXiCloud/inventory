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
    return Ajax.get('/settlement', param);
  },
  total(param) {
    return Ajax.get('/settlement/total', param);
  },
  load(id) {
    return Ajax.get('/settlement/load/' + id);
  },
  remove(id) {
    return Ajax.delete('/settlement/' + id);
  },
  approved(state, ids) {
    return Ajax.post('/settlement/approved/' + state, toIds(ids));
  },
  save(param) {
    return Ajax[param.order?.id ? 'put' : 'post']('/settlement', param);
  },
  writeOff(param) {
    return Ajax.get('/settlement/writeOff', param);
  }
};
