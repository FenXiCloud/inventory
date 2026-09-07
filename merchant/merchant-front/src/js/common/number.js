/**
 * 全局数值精度工具：读取当前账套参数（/init 下发的 quantityDecimal / priceDecimal），
 * 供 decimal-places 绑定与展示格式化统一使用；未加载/缺字段时兜底 2 位（与后端 findOrCreate 默认一致）。
 *
 * 口径约定：
 * - 数量类（quantity/库存/调拨量等）随 qtyDp；
 * - 单价类（unitPrice/secondaryPrice/均价等）随 priceDp；
 * - 金额类恒 2 位（后端 setScale(2) 全按金额口径，前端不随参数漂移）。
 */
import store from '@js/store';

const bookParam = (key, def) => {
  const book = store.state.accountBook;
  const v = book ? book[key] : null;
  return v == null ? def : v;
};

/** 数量小数位（账套参数） */
export const qtyDp = () => bookParam('quantityDecimal', 2);

/** 单价小数位（账套参数） */
export const priceDp = () => bookParam('priceDecimal', 2);

const toNumber = (v) => {
  if (v === null || v === undefined || v === '') return null;
  const n = Number(v);
  return isNaN(n) ? null : n;
};

/** 数量格式化：默认跟随账套数量小数位 */
export const fmtQty = (v, dp) => {
  const n = toNumber(v);
  return n == null ? '' : n.toFixed(dp == null ? qtyDp() : dp);
};

/** 单价/均价格式化：默认跟随账套单价小数位 */
export const fmtPrice = (v, dp) => {
  const n = toNumber(v);
  return n == null ? '' : n.toFixed(dp == null ? priceDp() : dp);
};

/** 金额格式化：恒 2 位（财务口径） */
export const fmtMoney = (v, dp) => {
  const n = toNumber(v);
  return n == null ? '' : n.toFixed(dp == null ? 2 : dp);
};

/** 金额千分位（表格合计、看板等展示） */
export const fmtMoneyComma = (v) => {
  const n = toNumber(v);
  return n == null ? '' : n.toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
};
