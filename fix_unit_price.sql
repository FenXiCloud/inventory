-- ============================================================
-- 修复 unitPrice 历史数据
-- 原因：旧公式 unitPrice = secondaryPrice / quantity（错）
--       新公式 unitPrice = secondaryPrice / conversionRate（正确）
-- 执行前建议先备份！
-- ============================================================

-- 1. 先看一下受影响的记录数（预览，不修改）
SELECT 'purchase_inbound_item' AS table_name, COUNT(*) AS affected_rows
FROM purchase_inbound_item
WHERE unit_price IS NOT NULL
  AND secondary_price IS NOT NULL
  AND COALESCE(NULLIF(conversion_rate, 0), 1) > 0
UNION ALL
SELECT 'purchase_order_item', COUNT(*)
FROM purchase_order_item
WHERE unit_price IS NOT NULL
  AND secondary_price IS NOT NULL
  AND COALESCE(NULLIF(conversion_rate, 0), 1) > 0
UNION ALL
SELECT 'jxc_inventory_item (采购入库)', COUNT(*)
FROM jxc_inventory_item
WHERE operation_type = '采购入库' AND unit_price IS NOT NULL;

-- 2. 修复 purchase_inbound_item.unit_price
UPDATE purchase_inbound_item
SET unit_price = ROUND(secondary_price / COALESCE(NULLIF(conversion_rate, 0), 1), 2)
WHERE secondary_price IS NOT NULL
  AND COALESCE(NULLIF(conversion_rate, 0), 1) > 0;

-- 3. 修复 purchase_order_item.unit_price
UPDATE purchase_order_item
SET unit_price = ROUND(secondary_price / COALESCE(NULLIF(conversion_rate, 0), 1), 2)
WHERE secondary_price IS NOT NULL
  AND COALESCE(NULLIF(conversion_rate, 0), 1) > 0;

-- 4. 修复 jxc_inventory_item.unit_price（采购入库）
--    从 purchase_inbound_item 取正确的 unit_price 回写
UPDATE jxc_inventory_item ii
JOIN jxc_purchase_inbound pi ON pi.id = ii.order_id
JOIN purchase_inbound_item pii ON pii.purchase_inbound_id = pi.id
  AND pii.product_id = ii.product_id
SET ii.unit_price = pii.unit_price
WHERE ii.operation_type = '采购入库';

-- 5. 验证：抽查几条数据看看对不对
SELECT
    ii.id,
    ii.product_id,
    ii.batch_number,
    ii.quantity,
    ii.unit_price AS inventory_unit_price,
    ii.subtotal,
    pii.secondary_price,
    pii.secondary_quantity,
    pii.conversion_rate,
    ROUND(pii.secondary_price / COALESCE(NULLIF(pii.conversion_rate, 0), 1), 2) AS expected_unit_price
FROM jxc_inventory_item ii
JOIN jxc_purchase_inbound pi ON pi.id = ii.order_id
JOIN purchase_inbound_item pii ON pii.purchase_inbound_id = pi.id
  AND pii.product_id = ii.product_id
WHERE ii.operation_type = '采购入库'
LIMIT 10;
