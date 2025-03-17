import Ajax from "@common/Request";

export default {
  list(param) {
    return Ajax.get('/purchaseReport', param)
  },
  listStat(param) {
    return Ajax.get('/purchaseReport/stat', param)
  },
}
