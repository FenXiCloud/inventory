import Ajax from "@common/Request";

export default {
  list(param) {
    return Ajax.get('/checkout', param)
  },
  save(param) {
    return Ajax.post('/checkout', param)
  },
  cancelCheckout(param) {
    return Ajax.put('/checkout', param)
  },
}
