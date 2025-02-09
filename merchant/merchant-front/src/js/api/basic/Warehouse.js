import Ajax from "@common/Request";

export default {
  save(param) {
    return Ajax[param.id ? 'put' : 'post']('/warehouse', param)
  },
  list(param) {
    return Ajax.get('/warehouse', param)
  },
    delete(id) {
    return Ajax.delete('/warehouse/' + id);
  },
  select(param) {
    return Ajax.get('/warehouse/select', param)
  },
}
