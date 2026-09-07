/**
 * 打印模板预览用的示例单据数据（非真实单据，仅用于直观展示模板排版效果）。
 * 独立成模块，避免 print.js 与预览组件互相 import 形成循环依赖。
 */

const ORDER_NO_PREFIX = [
  ['采购', 'CG'], ['销售', 'XS'], ['收款', 'SK'], ['付款', 'FK'], ['核销', 'HX'],
  ['调拨', 'DB'], ['盘点', 'PD'], ['成本', 'CB'], ['转帐', 'ZZ'], ['转账', 'ZZ'], ['其他', 'QT']
];

function pad(n) {
  return String(n).padStart(2, '0');
}

function todayStr() {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function sampleOrderNo(documentType) {
  const t = documentType || '';
  const hit = ORDER_NO_PREFIX.find(([kw]) => t.includes(kw));
  const d = new Date();
  return `${hit ? hit[1] : 'DJ'}${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}0001`;
}

function samplePartner(documentType) {
  const t = documentType || '';
  if (t.includes('采购') || t.includes('付款')) return '杭州华越贸易有限公司';
  if (t.includes('销售') || t.includes('收款') || t.includes('核销')) return '上海云帆科技有限公司';
  if (t.includes('转帐') || t.includes('转账')) return '基本户 / 招商银行 6225****3344';
  if (t.includes('调拨') || t.includes('盘点') || t.includes('其他') || t.includes('成本')) return '中心仓库';
  return '示例往来单位';
}

export function buildSamplePrintData(documentType) {
  return {
    header: {
      orderNo: sampleOrderNo(documentType),
      orderDate: todayStr(),
      partner: samplePartner(documentType),
      amount: '4030.00',
      remarks: '示例数据，仅供模板效果预览'
    },
    items: [
      {productName: '无线蓝牙耳机 Pro 白色', quantity: '20', price: '129.50', amount: '2590.00', remarks: '含合格证'},
      {productName: 'USB-C 快充数据线 1.5m', quantity: '100', price: '9.90', amount: '990.00', remarks: ''},
      {productName: '便携收纳包 灰色', quantity: '10', price: '45.00', amount: '450.00', remarks: '赠品'}
    ]
  };
}
