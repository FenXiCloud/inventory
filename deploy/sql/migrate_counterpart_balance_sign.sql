-- 往来余额符号迁移：欠款为正、预收/预付为负
-- 将历史档案余额与流水结余字段取反，与销售/采购审核新口径对齐
-- 执行前请备份；仅执行一次

UPDATE jxc_customer
SET balance = -balance
WHERE balance IS NOT NULL
  AND balance <> 0;

UPDATE jxc_supplier
SET balance = -balance
WHERE balance IS NOT NULL
  AND balance <> 0;

UPDATE jxc_customer_flow
SET balance_receivables = -balance_receivables
WHERE balance_receivables IS NOT NULL
  AND balance_receivables <> 0;

UPDATE jxc_supplier_flow
SET balance_payable = -balance_payable
WHERE balance_payable IS NOT NULL
  AND balance_payable <> 0;
