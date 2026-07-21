import Ajax from '@common/Request';

function postWithPage(url, param) {
  const page = param.page || 1;
  const pageSize = param.pageSize || 20;
  const body = Object.assign({}, param);
  delete body.page;
  delete body.pageSize;
  return Ajax.post(`${url}?page=${page}&pageSize=${pageSize}`, body);
}

export default {
  item(param) {
    return postWithPage('/salesReport/item', param || {});
  },
  summary(param) {
    return postWithPage('/salesReport/summary', param || {});
  },
  profit(param) {
    return postWithPage('/salesReport/profit', param || {});
  },
  ranking(param) {
    return postWithPage('/salesReport/ranking', param || {});
  }
};
