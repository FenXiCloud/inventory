import Ajax from '@common/Request';

export default {
  advanceBalance(param) {
    return Ajax.get('/fund/report/advance-balance', param);
  }
};
