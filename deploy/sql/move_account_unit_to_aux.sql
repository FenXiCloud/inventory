-- 账户管理、计量单位：从「基础资料」挪到「辅助资料」

UPDATE jxc_menu SET parent_id = 10, pos = 2 WHERE id = 79; -- 账户管理
UPDATE jxc_menu SET parent_id = 10, pos = 3 WHERE id = 80; -- 计量单位
UPDATE jxc_menu SET pos = 4 WHERE id = 23; -- 收支类型
UPDATE jxc_menu SET pos = 5 WHERE id = 24; -- 结算方式
