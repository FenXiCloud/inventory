/**
 * 单据日期可选范围：
 * - 已结账：只能选结账日之后（与后端 CheckoutService.assertEditable 一致）
 * - 未结账：不限制日期
 */
export function buildOrderDateDisable(accountBook) {
  const ab = accountBook || {};
  const checkout = ab.checkoutDate;
  if (checkout) {
    let first = ab.checkoutSDate;
    if (!first || String(first).includes('Invalid')) {
      const d = new Date(checkout);
      if (!Number.isNaN(d.getTime())) {
        d.setDate(d.getDate() + 1);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        first = `${y}-${m}-${day}`;
      }
    }
    if (first) {
      return {before: first};
    }
  }
  return undefined;
}
