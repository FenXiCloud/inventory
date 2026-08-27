-- 以销定购模块菜单SQL（「以销定购」专题组 + 销售预订 + 进货预订）
-- 说明：新商户由 admin-server buildDefaultMenus 自动 seed；此脚本用于生产环境
--       （仅部署 merchant-server 时不 seed 菜单）手动补录，或修复历史错位菜单。
-- 执行方式：mysql -u root -p jxc < sales_reservation_menu.sql
USE jxc;

-- 「以销定购」专题组（挂在 采购管理 下，pos=1）
INSERT INTO jxc_menu (id, component, name, icon_cls, require_auth, parent_id, enabled, pos, menu_module, menu_type, menu_group)
SELECT 259, NULL, '以销定购', 'cart', NULL, 25, b'1', 1, 'MERCHANT', 'MENU', 'MERCHANT'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM jxc_menu WHERE id = 259);

-- 销售预订（挂在 以销定购 下）
INSERT INTO jxc_menu (id, component, name, icon_cls, require_auth, parent_id, enabled, pos, menu_module, menu_type, menu_group)
SELECT 257, 'SalesReservationList', '销售预订', NULL, NULL, 259, b'1', 0, 'MERCHANT', 'MENU', 'MERCHANT'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM jxc_menu WHERE id = 257);

-- 进货预订（挂在 以销定购 下）
INSERT INTO jxc_menu (id, component, name, icon_cls, require_auth, parent_id, enabled, pos, menu_module, menu_type, menu_group)
SELECT 258, 'PurchaseReservationList', '进货预订', NULL, NULL, 259, b'1', 1, 'MERCHANT', 'MENU', 'MERCHANT'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM jxc_menu WHERE id = 258);

-- 为所有商户授权这三条菜单
INSERT INTO jxc_merchant_menu (menu_id, merchant_id)
SELECT m.id, t.merchant_id
FROM jxc_menu m
JOIN (SELECT DISTINCT merchant_id FROM jxc_merchant_menu) t
WHERE m.id IN (259, 257, 258)
  AND NOT EXISTS (
      SELECT 1 FROM jxc_merchant_menu x
      WHERE x.menu_id = m.id AND x.merchant_id = t.merchant_id
  );
