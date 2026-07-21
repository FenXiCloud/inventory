import Ajax from '@common/Request';

export default {
  list() {
    return Ajax.get('/financeAccountLink');
  },
  save(param) {
    return Ajax[param.id ? 'put' : 'post']('/financeAccountLink', param);
  },
  accountSets(param) {
    return Ajax.post('/financeAccountLink/accountSets', param);
  },
  byAccountBook(param) {
    return Ajax.get(`/financeAccountLink/byAccountBook/${param}`);
  },
  load(param) {
    return Ajax.get(`/financeAccountLink/load/${param}`);
  },
  voucherWord() {
    return Ajax.get('/financeAccountLink/voucherWord');
  },
  code(param) {
    return Ajax.get('/financeAccountLink/code', param);
  },
  subject() {
    return Ajax.get('/financeAccountLink/subject');
  },
  voucherSelect() {
    return Ajax.get('/financeAccountLink/voucherSelect');
  },
  voucherSummary() {
    return Ajax.get('/financeAccountLink/voucherSummary');
  },
  accountingCategory(param) {
    return Ajax.get('/financeAccountLink/accountingCategory', param);
  }
};
