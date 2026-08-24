import Ajax from "@common/Request";

export default {
  list(param) {
    return Ajax.get('/checkout', param)
  },
  save(param) {
    return Ajax.post('/checkout', param)
  },
  toCheck(param) {
    return Ajax.post('/checkout', param)
  },
  antiCheckout(param) {
    return Ajax.put('/checkout', param)
  },
  preCheck(param) {
    return Ajax.get('/checkout/preCheck', param)
  },
  monthlySummary(param) {
    return Ajax.get('/checkout/monthlySummary', param)
  },
  history(param) {
    return Ajax.get('/checkout/history', param)
  },
}
