-- 修复：菜单 component 字段
-- 原因：菜单定义在 admin-server（buildDefaultMenus），但生产只部署 merchant-server，
--       其 @PostConstruct 不会执行，故需直接更新 jxc_menu 表把占位 ComingSoon 换成真实组件。
-- 执行环境：docker exec -it contract_mysql8 mysql -uroot -p jxc

UPDATE jxc_menu SET component = 'InventoryWarning'     WHERE id = 216; -- 库存预警
UPDATE jxc_menu SET component = 'AdvanceReceiptList'   WHERE id = 217; -- 预收款单
UPDATE jxc_menu SET component = 'AdvancePaymentList'   WHERE id = 218; -- 预付款单
UPDATE jxc_menu SET component = 'ProfitReport'         WHERE id = 219; -- 利润表
UPDATE jxc_menu SET component = 'AdvanceBalanceReport' WHERE id = 255; -- 预收预付余额
UPDATE jxc_menu SET component = 'InventoryOverstock'   WHERE id = 256; -- 库存上限预警
UPDATE jxc_menu SET component = 'QuickOrder'          WHERE id = 200; -- 快速开单
UPDATE jxc_menu SET component = 'ProductComboList'    WHERE id = 237; -- 商品套餐
UPDATE jxc_menu SET component = 'InvoiceAggregation'   WHERE id = 234; -- 销项归集
UPDATE jxc_menu SET component = 'InvoiceAggregation'   WHERE id = 235; -- 进项归集
UPDATE jxc_menu SET component = 'AccountFlowList'      WHERE id = 220; -- 资金流水
UPDATE jxc_menu SET component = 'LossOrderList'        WHERE id = 246; -- 报损单
UPDATE jxc_menu SET component = 'GainOrderList'        WHERE id = 247; -- 报溢单
UPDATE jxc_menu SET component = 'InventoryOverview'    WHERE id = 248; -- 库存状况总览
UPDATE jxc_menu SET component = 'InventoryDistribution' WHERE id = 249; -- 库存分布
UPDATE jxc_menu SET component = 'VirtualStock'         WHERE id = 250; -- 虚拟库存状况表
UPDATE jxc_menu SET component = 'BatchTracking'        WHERE id = 251; -- 批次跟踪查询
UPDATE jxc_menu SET component = 'ShelfLifeList'        WHERE id = 214; -- 保质期管理
UPDATE jxc_menu SET component = 'SerialManagement'     WHERE id = 215; -- 序列号管理
UPDATE jxc_menu SET component = 'StockTakeList'        WHERE id = 252; -- 盘点记录查询
UPDATE jxc_menu SET component = 'LossGainReport'       WHERE id = 253; -- 报损报溢汇总表
UPDATE jxc_menu SET component = 'TransferReport'       WHERE id = 254; -- 调拨统计报表

-- 验证（应全部变为上面的新组件名）
SELECT id, name, component FROM jxc_menu
WHERE id IN (216,217,218,219,220,246,247,248,249,250,251,252,253,254)
ORDER BY id;
