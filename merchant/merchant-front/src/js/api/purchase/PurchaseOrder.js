import Ajax from "@common/Request";

export default {
  save(param) {
    return Ajax[param.id ? 'put' : 'post']('/purchaseOrder', param)
  },
  list(param) {
    return Ajax.get('/purchaseOrder', param)
  },
  listToReturn(param) {
    return Ajax.get('/purchaseOrder/toReturn', param)
  },
  remove(id) {
    return Ajax.delete('/purchaseOrder/' + id);
  },
  select(param) {
    return Ajax.get('/purchaseOrder/select', param)
  },
  load(id) {
    return Ajax.get('/purchaseOrder/load/' + id);
  },
  approved(state, ids) {
    return Ajax.post('/purchaseOrder/approved/' + state, ids);
  },
  toInbound(supplierId, ids) {
    return Ajax.post('/purchaseOrder/toInbound/' + supplierId, ids);
  },
  toReturn(supplierId, ids) {
    return Ajax.post('/purchaseOrder/toReturn/' + supplierId, ids);
  },
}
