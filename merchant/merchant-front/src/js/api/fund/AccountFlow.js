import Ajax from '@common/Request';

export default {
  /*  save(param) {
        return Ajax[param.id ? 'put' : 'post']('/accountFlow', param)
    },
    list(param) {
        return Ajax.get('/accountFlow', param)
    },
    remove(id) {
        return Ajax.delete('/accountFlow/' + id);
    },
    select(param) {
        return Ajax.get('/accountFlow/select', param)
    } */
  // 现金流水
  list(param) {
    return Ajax.get('/accountFlow/list', param);
  },
  //   应付账款明细列表
  getPayableDetailReport(param) {
    return Ajax.get('/accountFlow/getPayableDetailReport', param);
  },
  // 应收账款明细列表
  getReceivableDetailReport(param) {
    return Ajax.get('/accountFlow/getReceivableDetailReport', param);
  },
  //应付汇总列表
  summaryPayableDetails(param) {
    return Ajax.get('/accountFlow/summaryPayableDetails', param);
  },
  // 应收汇总列表
  summaryReceivableDetails(param) {
    return Ajax.get('/accountFlow/summaryReceivableDetails', param);
  },
  //   其他收支明细列表
  otherFundDetails(param) {
    return Ajax.get('/accountFlow/otherFundDetails', param);
  },
  //   供应商对账单
  listBySupplier(param) {
    return Ajax.get('/supplierFlow/listBySupplier', param);
  },
  //   客户对账单
  getCustomerBillFlows(param) {
    return Ajax.get('/customerFlow/getCustomerBillFlows', param);
  }
};
