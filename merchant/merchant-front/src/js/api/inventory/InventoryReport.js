import Ajax from '@common/Request';

export default {
  warning() {
    return Ajax.get('/inventory/report/warning');
  },
  overstock() {
    return Ajax.get('/inventory/report/overstock');
  },
  warningSummary() {
    return Ajax.get('/inventory/report/warning-summary');
  },
  overview() {
    return Ajax.get('/inventory/report/overview');
  },
  distribution() {
    return Ajax.get('/inventory/report/distribution');
  },
  virtualStock() {
    return Ajax.get('/inventory/report/virtual-stock');
  },
  transfer() {
    return Ajax.get('/inventory/report/transfer');
  },
  batch() {
    return Ajax.get('/inventory/report/batch');
  },
  lossGain() {
    return Ajax.get('/inventory/report/loss-gain');
  },
  expiry() {
    return Ajax.get('/inventory/report/expiry');
  },
  shelfLife() {
    return Ajax.get('/inventory/report/shelf-life');
  },
  updateShelfLife(itemId, form) {
    return Ajax.put('/inventory/report/shelf-life/' + itemId, form);
  },
  batchAvailable(param) {
    return Ajax.get('/inventory/report/batch-available', param);
  }
};
