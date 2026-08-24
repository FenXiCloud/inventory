import Ajax from '@common/Request';

export default {
  profit(param) {
    return Ajax.get('/fund/report/profit', param);
  },
  profitMonthly(param) {
    return Ajax.get('/fund/report/profit-monthly', param);
  }
};
