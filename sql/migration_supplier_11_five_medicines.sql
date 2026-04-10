-- 将供应商精简为 11 家：药品 id 1–54 按每 5 个对应一家（末家 4 个药品）。
-- 并补全联系人、电话、地址、邮箱（与 hospital_db.sql 初始化一致）。
-- 执行前请备份数据库。若存在引用旧供应商 id 的采购/结算数据，脚本会先按药品重算 supplier_id。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1) 药品主供应商
UPDATE `medicine`
SET `supplier_id` = FLOOR((`id` - 1) / 5) + 1
WHERE `id` BETWEEN 1 AND 54;

UPDATE `medicine` SET `supplier_id` = 1 WHERE `supplier_id` IS NULL OR `supplier_id` < 1 OR `supplier_id` > 11;

-- 2) 采购单供应商：按该单首条明细药品推导（无明细则归为 1）
UPDATE `purchase_order` po
LEFT JOIN (
    SELECT poi.`order_id`, FLOOR((MIN(m.`id`) - 1) / 5) + 1 AS `nsid`
    FROM `purchase_order_item` poi
    INNER JOIN `medicine` m ON m.`id` = poi.`medicine_id`
    GROUP BY poi.`order_id`
) x ON x.`order_id` = po.`id`
SET po.`supplier_id` = COALESCE(x.`nsid`, 1);

-- 3) 采购结算明细 / 主单
UPDATE `purchase_settlement_detail` psd
INNER JOIN `medicine` m ON m.`id` = psd.`medicine_id`
SET psd.`supplier_id` = m.`supplier_id`;

UPDATE `purchase_settlement_order` pso
LEFT JOIN (
    SELECT `settlement_order_id`, MIN(`supplier_id`) AS `sid`
    FROM `purchase_settlement_detail`
    WHERE `settlement_order_id` IS NOT NULL
    GROUP BY `settlement_order_id`
) t ON t.`settlement_order_id` = pso.`id`
SET pso.`supplier_id` = COALESCE(t.`sid`, 1);

-- 4) 供应商-药品关系表
DELETE FROM `supplier_medicine`;
INSERT INTO `supplier_medicine` (`supplier_id`, `medicine_id`, `create_time`)
SELECT FLOOR((m.`id` - 1) / 5) + 1, m.`id`, NOW()
FROM `medicine` m
WHERE m.`id` BETWEEN 1 AND 54;

-- 5) 更新保留的 11 家供应商资料
UPDATE `supplier` SET `supplier_code` = 'SUP-001', `name` = '华东康达医药有限公司', `contact_name` = '周文轩', `contact_phone` = '13810001001', `address` = '浙江省杭州市滨江区江南大道3688号康达大厦7层', `email` = 'zhou@kangda-east.demo.com', `status` = 1 WHERE `id` = 1;
UPDATE `supplier` SET `supplier_code` = 'SUP-002', `name` = '北方九州药品经销', `contact_name` = '韩沐阳', `contact_phone` = '13810001002', `address` = '天津市滨海新区黄海路156号九州物流园B2', `email` = 'han@jz-north.demo.com', `status` = 1 WHERE `id` = 2;
UPDATE `supplier` SET `supplier_code` = 'SUP-003', `name` = '岭南本草供应链', `contact_name` = '林若溪', `contact_phone` = '13810001003', `address` = '广东省佛山市顺德区乐从镇医药大道22号', `email` = 'lin@lingnan-bc.demo.com', `status` = 1 WHERE `id` = 3;
UPDATE `supplier` SET `supplier_code` = 'SUP-004', `name` = '川渝惠民药业配送', `contact_name` = '唐思远', `contact_phone` = '13810001004', `address` = '重庆市渝北区金开大道990号惠民仓储中心', `email` = 'tang@cy-huimin.demo.com', `status` = 1 WHERE `id` = 4;
UPDATE `supplier` SET `supplier_code` = 'SUP-005', `name` = '江汉明泽医药贸易', `contact_name` = '彭雨晴', `contact_phone` = '13810001005', `address` = '湖北省武汉市江夏区光谷大道1888号明泽园', `email` = 'peng@mingze-jh.demo.com', `status` = 1 WHERE `id` = 5;
UPDATE `supplier` SET `supplier_code` = 'SUP-006', `name` = '齐鲁明德制药渠道', `contact_name` = '孙宇航', `contact_phone` = '13810001006', `address` = '山东省济南市高新区舜华路2006号明德广场', `email` = 'sun@mingde-ql.demo.com', `status` = 1 WHERE `id` = 6;
UPDATE `supplier` SET `supplier_code` = 'SUP-007', `name` = '闽台海峡健康商贸', `contact_name` = '蔡佳宁', `contact_phone` = '13810001007', `address` = '福建省厦门市湖里区港中路518号海峡医药港', `email` = 'cai@strait-mn.demo.com', `status` = 1 WHERE `id` = 7;
UPDATE `supplier` SET `supplier_code` = 'SUP-008', `name` = '云滇绿野药材集散', `contact_name` = '段清扬', `contact_phone` = '13810001008', `address` = '云南省昆明市官渡区珥季路333号绿野园区3栋', `email` = 'duan@luye-yn.demo.com', `status` = 1 WHERE `id` = 8;
UPDATE `supplier` SET `supplier_code` = 'SUP-009', `name` = '关中秦川医疗供应', `contact_name` = '冯子墨', `contact_phone` = '13810001009', `address` = '陕西省西安市未央区尚稷路899号秦川产业园', `email` = 'feng@qin-chuan.demo.com', `status` = 1 WHERE `id` = 9;
UPDATE `supplier` SET `supplier_code` = 'SUP-010', `name` = '辽东瑞丰冷链物流药业', `contact_name` = '高景行', `contact_phone` = '13810001010', `address` = '辽宁省沈阳市铁西区建设大路77号瑞丰冷库4区', `email` = 'gao@ruifeng-ld.demo.com', `status` = 1 WHERE `id` = 10;
UPDATE `supplier` SET `supplier_code` = 'SUP-011', `name` = '琼海蓝天医药服务', `contact_name` = '符安然', `contact_phone` = '13810001011', `address` = '海南省海口市秀英区长滨路66号蓝天医药园', `email` = 'fu@lantian-qh.demo.com', `status` = 1 WHERE `id` = 11;

-- 6) 删除多余供应商（请先确保上列 UPDATE 已无指向 id>11 的外键数据）
DELETE FROM `supplier` WHERE `id` > 11;

ALTER TABLE `supplier` AUTO_INCREMENT = 12;

SET FOREIGN_KEY_CHECKS = 1;
