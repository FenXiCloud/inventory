import Ajax from "@common/Request";

export default {
  save(param) {
    return Ajax[param.id ? 'put' : 'post']('/purchaseInbound', param)
  },
  list(param) {
    return Ajax.get('/purchaseInbound', param)
  },
  total(param) {
    return Ajax.get('/purchaseInbound/total', param)
  },
  remove(id) {
    return Ajax.delete('/purchaseInbound/' + id);
  },
  select(param) {
    return Ajax.get('/purchaseInbound/select', param)
  },
  load(id) {
    return Ajax.get('/purchaseInbound/load/' + id);
  },
  listToReturn(param) {
    return Ajax.get('/purchaseInbound/toReturn', param)
  },
  toReturn(supplierId, ids) {
    return Ajax.post('/purchaseInbound/toReturn/' + supplierId, ids);
  },
  approved(state, ids) {
    return Ajax.post('/purchaseInbound/approved/' + state, ids);
  },
  importData(formData) {
    return Ajax.post('/purchaseInbound/importData', formData, {'Content-Type': 'multipart/form-data', repeatable: true});
  },
}
