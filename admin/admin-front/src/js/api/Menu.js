import Ajax from "@common/Request";

export default {
  save(param) {
    return Ajax[param.id ? 'put' : 'post']('/menu', param)
  },
  list(param) {
    return Ajax.get('/menu', param)
  },
  remove(id) {
    return Ajax.delete('/menu/' + id);
  },
  grantMerchant(param){
    return Ajax.post('/menu/grant', param)
  },
  queryGrantMenu(merchantId){
    return Ajax.get('/menu/query/grant/'+merchantId)
  },
  merchantMenu(merchantId){
    return Ajax.get('/menu/query/merchant/'+merchantId)
  }
}
