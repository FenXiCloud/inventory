import Ajax from "@common/Request";

export default {
  list(param) {
    return Ajax.get('/productCombo', param);
  },
  load(id) {
    return Ajax.get('/productCombo/load/' + id);
  },
  save(param) {
    return Ajax.post('/productCombo', param);
  },
  remove(id) {
    return Ajax.delete('/productCombo/' + id);
  },
};
