import Ajax from '@common/Request';

export default {
  list(param) {
    return Ajax.get('/systemLog', param);
  },
  remove(id) {
    return Ajax.delete('/systemLog/' + id);
  }
};
