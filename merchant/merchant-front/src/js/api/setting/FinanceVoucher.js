import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/financeVoucher', param);
  },
  save(param) {
    return Ajax.post('/financeVoucher', param);
  },
  delete(id) {
    return Ajax.delete('/financeVoucher/' + id);
  },
  load(id) {
    return Ajax.get('/financeVoucher/load/' + id);
  },
  candidates(param) {
    return Ajax.get('/financeVoucher/candidates', param);
  },
  batch(forms) {
    return Ajax.post('/financeVoucher/batch', forms);
  },
  batchDelete(ids) {
    return Ajax.delete('/financeVoucher/batch', ids);
  },
  balance(param) {
    return Ajax.get('/financeVoucher/balance', param);
  },
  auxiliary(param) {
    return Ajax.get('/financeVoucher/auxiliary', param);
  },
  sync(param) {
    return Ajax.postJson('/financeVoucher/sync', param);
  },
  remote(param) {
    return Ajax.get('/financeVoucher/remote', param);
  }
};
