-- 验证：以销定购来源的采购入库单状态分布
SELECT source_type, order_status, COUNT(*) AS cnt
FROM jxc_purchase_inbound
WHERE source_type = '以销定购'
GROUP BY source_type, order_status;
