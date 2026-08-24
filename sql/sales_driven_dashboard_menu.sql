-- 以销定购看板菜单 + 禁用旧预订菜单
-- 执行方式：mysql -u root -p jxc < sales_driven_dashboard_menu.sql
USE jxc;

-- 1. 禁用旧的销售预订、进货预订、以销定购菜单
UPDATE jxc_menu SET enabled = 0 WHERE id IN (257, 258, 259, 207);

-- 2. 新增「以销定购看板」菜单（挂在采购单据下，parent_id=51，pos=3）
INSERT INTO jxc_menu (id, component, name, icon_cls, require_auth, parent_id, enabled, pos, menu_module, menu_type, menu_group)
SELECT 260, 'SalesDrivenDashboard', '以销定购看板', NULL, NULL, 51, b'1', 3, 'MERCHANT', 'MENU', 'MERCHANT'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM jxc_menu WHERE id = 260);

-- 3. 为所有商户授权新菜单
INSERT INTO jxc_merchant_menu (menu_id, merchant_id)
SELECT 260, t.merchant_id
FROM (SELECT DISTINCT merchant_id FROM jxc_merchant_menu) t
WHERE NOT EXISTS (
    SELECT 1 FROM jxc_merchant_menu x
    WHERE x.menu_id = 260 AND x.merchant_id = t.merchant_id
);

-- 4. 采购单据内重新排序（进货预订和智能补货位置调整）
UPDATE jxc_menu SET pos = 3 WHERE id = 260; -- 以销定购看板
UPDATE jxc_menu SET pos = 4 WHERE id = 206; -- 进货单导入
UPDATE jxc_menu SET pos = 5 WHERE id = 209; -- 智能补货
