import Ajax from "@common/Request";

export default {
  list(param) {
    return Ajax.get('/inventoryCostBatch', param);
  },
}
