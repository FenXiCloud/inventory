/**
 * 多单位换算（前端）——与后端 UnitConvert 约定一致。
 * 约定：基本数量 = 业务数量 × 换算率；基本单价 = 业务单价 ÷ 换算率。
 *
 * 切换"采购/业务单位"时价格按【基本单价等比换算】，保持成本与金额口径一致（不取商品档案中该辅助单位的预设价），
 * 用于采购订单/采购入库等单据行的单位下拉切换。
 */
import { priceDp } from '@common/number';

// 行的基本单价锚点：优先取已存基本单价 unitPrice；否则由当前业务单价/换算率反推
export function baseCost(row) {
  const up = Number(row.unitPrice);
  if (up > 0) return up;
  const sp = Number(row.secondaryPrice) || 0;
  const r = Number(row.conversionRate) || 1;
  return r ? sp / r : sp;
}

/**
 * 把一行切换到目标单位（unit 来自该行单位下拉选项，含 unitId/unitName/conversionRate）。
 * 会就地更新单位、换算率、单价/基本单价、基本数量与小计；不把 0 数量强制为 1。
 */
export function switchUnit(row, unit) {
  const rate = Number(unit.conversionRate) || 1;
  const base = baseCost(row);
  row.secondaryUnitId = unit.unitId;
  row.secondaryUnitName = unit.unitName;
  row.conversionRate = rate;
  row.unitPrice = Number(base.toFixed(priceDp()));
  row.secondaryPrice = Number((base * rate).toFixed(priceDp()));
  const qty = Number(row.secondaryQuantity) || 0;
  row.quantity = Number((qty * rate).toFixed(2));
  const dr = Number(row.discountRate) || 0;
  const gross = qty * row.secondaryPrice;
  row.discountAmount = Number((gross * dr / 100).toFixed(2));
  row.subtotal = Number((gross * (100 - dr) / 100).toFixed(2));
  return row;
}
