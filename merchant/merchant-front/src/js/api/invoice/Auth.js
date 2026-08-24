import Ajax from '@common/Request';

function qs(obj) {
  return Object.entries(obj || {})
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
}

export default {
  // 状态
  state() {
    return Ajax.get('/invoice/auth/state');
  },
  // 订购产品
  purchase(param) {
    return Ajax.post(`/invoice/auth/purchase?${qs(param)}`);
  },
  // 查询/创建账号
  account() {
    return Ajax.post('/invoice/auth/account');
  },
  // 登录
  login() {
    return Ajax.post('/invoice/auth/login');
  },
  sms(taskId, code) {
    return Ajax.post(`/invoice/auth/sms?${qs({taskId, code})}`);
  },
  // 人脸识别
  qr() {
    return Ajax.get('/invoice/auth/qr');
  },
  qrImageUrl() {
    return `/api/invoice/auth/qr-image?_t=${Date.now()}`;
  },
  qrResult() {
    return Ajax.get('/invoice/auth/qr/result');
  },
  // 扫脸时长
  scanDuration() {
    return Ajax.get('/invoice/auth/scan-duration');
  },
  setScanDuration(seconds) {
    return Ajax.post(`/invoice/auth/scan-duration?${qs({seconds})}`);
  }
};
