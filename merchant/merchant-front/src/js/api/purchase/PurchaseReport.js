import Ajax from "@common/Request";

export default {
  item(param) {
    return Ajax.get('/purchaseReport', param)
  },
  summary(param) {
    return Ajax.get('/purchaseReport/summary', param)
  },
  statistics(param) {
    return Ajax.get('/purchaseReport/statistics', param)
  },
}
