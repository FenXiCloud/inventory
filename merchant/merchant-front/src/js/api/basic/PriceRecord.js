import Ajax from '@common/Request';

export default {
  save(param) {
    return Ajax[param.id ? 'put' : 'post']('/priceRecord', param);
  },
  list(param) {
    return Ajax.get('/priceRecord', param);
  },
  remove(id) {
    return Ajax.delete('/priceRecord/' + id);
  },
  select(param) {
    return Ajax.get('/priceRecord/select', param);
  },
  productList(param) {
    return Ajax.get('/priceRecord/product', param);
  },
  productSave(param) {
    return Ajax.post('/priceRecord/product', param);
  },
  productCellSave(param) {
    return Ajax.post('/priceRecord/product/cell', param);
  },
  price(param) {
    return Ajax.get('/priceRecord/price', param);
  },
  purchasePrice(param) {
    return Ajax.get('/priceRecord/purchasePrice', param);
  }
};
