-- 采购单支持多采购计划、多计划明细分摊

CREATE TABLE IF NOT EXISTS purchase_order_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    order_id BIGINT NOT NULL COMMENT '采购单ID',
    plan_id BIGINT NOT NULL COMMENT '采购计划ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_plan (order_id, plan_id),
    KEY idx_pop_plan_id (plan_id),
    CONSTRAINT fk_pop_order FOREIGN KEY (order_id) REFERENCES purchase_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_pop_plan FOREIGN KEY (plan_id) REFERENCES purchase_plan(id) ON DELETE RESTRICT
) COMMENT '采购单-采购计划关联表';

CREATE TABLE IF NOT EXISTS purchase_order_item_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分摊ID',
    order_item_id BIGINT NOT NULL COMMENT '采购单明细ID',
    plan_item_id BIGINT NOT NULL COMMENT '采购计划明细ID',
    allocated_qty INT NOT NULL COMMENT '分摊数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_order_item_plan (order_item_id, plan_item_id),
    KEY idx_poip_plan_item_id (plan_item_id),
    CONSTRAINT fk_poip_order_item FOREIGN KEY (order_item_id) REFERENCES purchase_order_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_poip_plan_item FOREIGN KEY (plan_item_id) REFERENCES purchase_plan_item(id) ON DELETE RESTRICT
) COMMENT '采购单明细-计划明细分摊表';

-- 兼容历史单计划数据：为历史采购单写入主计划关联
INSERT INTO purchase_order_plan(order_id, plan_id, create_time)
SELECT po.id, po.plan_id, NOW()
FROM purchase_order po
LEFT JOIN purchase_order_plan pop ON pop.order_id = po.id AND pop.plan_id = po.plan_id
WHERE pop.id IS NULL;

-- 兼容历史采购单明细：按旧字段补全单条分摊关系
INSERT INTO purchase_order_item_plan(order_item_id, plan_item_id, allocated_qty, create_time)
SELECT poi.id, poi.plan_item_id, poi.order_qty, NOW()
FROM purchase_order_item poi
LEFT JOIN purchase_order_item_plan poip ON poip.order_item_id = poi.id AND poip.plan_item_id = poi.plan_item_id
WHERE poi.plan_item_id IS NOT NULL AND poip.id IS NULL;
