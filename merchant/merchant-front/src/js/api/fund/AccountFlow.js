import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/accountFlow', param);
  },
  payableDetail(param) {
    return Ajax.get('/accountFlow/payableDetail', param);
  },
  receivableDetail(param) {
    return Ajax.get('/accountFlow/receivableDetail', param);
  },
  summaryPayable(param) {
    return Ajax.get('/accountFlow/summaryPayable', param);
  },
  summaryReceivable(param) {
    return Ajax.get('/accountFlow/summaryReceivable', param);
  },
  otherFund(param) {
    return Ajax.get('/accountFlow/otherFund', param);
  },
  supplierStatement(param) {
    return Ajax.get('/supplierFlow/statement', param);
  },
  customerStatement(param) {
    return Ajax.get('/customerFlow/statement', param);
  },
  customerStatementSummary(param) {
    return Ajax.get('/customerFlow/statementSummary', param);
  },
  supplierStatementSummary(param) {
    return Ajax.get('/supplierFlow/statementSummary', param);
  }
};
