import Ajax from '@common/Request';

function qs(obj) {
  return Object.entries(obj || {})
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
}

export default {
  // ===== 蓝字发票 =====
  issue(data) {
    return Ajax.post('/invoice/issue', data);
  },
  batchIssue(ids) {
    return Ajax.post('/invoice/batch-issue', ids);
  },
  get(id) {
    return Ajax.get(`/invoice/${id}`);
  },
  getPdfUrl(id) {
    return `/api/invoice/${id}/pdf`;
  },
  searchGoodsTax(name) {
    return Ajax.get('/invoice/goods-tax', {name});
  },

  // ===== 列表 =====
  listPaged(param) {
    return Ajax.get('/invoice/paged', param);
  },
  listBluePaged(param) {
    return Ajax.get('/invoice/blue-paged', param);
  },
  listRedPaged(param) {
    return Ajax.get('/invoice/red-paged', param);
  },
  search(param) {
    return Ajax.get('/invoice/search', param);
  },

  // ===== 红字 =====
  applyRed(param) {
    return Ajax.post(`/invoice/red/apply?${qs(param)}`);
  },
  issueRed(param) {
    return Ajax.post(`/invoice/red/issue?${qs(param)}`);
  },
  syncRed() {
    return Ajax.post('/invoice/red/sync');
  },
  manualRed(param) {
    return Ajax.post(`/invoice/red/manual?${qs(param)}`);
  },

  // ===== 统计 =====
  stats() {
    return Ajax.get('/invoice/stats');
  },

  // ===== 发票归集 =====
  outputAggregation() {
    return Ajax.get('/invoice/output-aggregation');
  },
  inputAggregation() {
    return Ajax.get('/invoice/input/aggregation');
  },
  inputList(param) {
    return Ajax.get('/invoice/input', param);
  },
  inputSave(data) {
    return Ajax.post('/invoice/input', data);
  },
  inputDelete(id) {
    return Ajax.delete(`/invoice/input/${id}`);
  },

  // ===== 税号配置（销方信息） =====
  getSellerConfig() {
    return Ajax.get('/invoice/seller-config');
  },
  saveSellerConfig(data) {
    return Ajax.post('/invoice/seller-config', data);
  }
};
