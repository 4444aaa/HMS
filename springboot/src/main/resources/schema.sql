-- 医生表
CREATE TABLE IF NOT EXISTS doctor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '医生ID',
    doctor_no VARCHAR(20) COMMENT '医生编号',
    name VARCHAR(50) NOT NULL COMMENT '医生姓名',
    department_id BIGINT COMMENT '所属科室ID',
    title VARCHAR(50) COMMENT '职称',
    expertise TEXT COMMENT '专长',
    introduction TEXT COMMENT '简介',
    user_id BIGINT COMMENT '关联用户ID',
    status INT DEFAULT 1 COMMENT '状态: 1-在职, 0-离职',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (department_id) REFERENCES department(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL
) COMMENT '医生信息表'; 

-- 药品表
CREATE TABLE IF NOT EXISTS medicine (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '药品ID',
    name VARCHAR(100) NOT NULL COMMENT '药品名称',
    medicine_code VARCHAR(50) UNIQUE COMMENT '药品编码',
    specification VARCHAR(100) COMMENT '规格',
    price DECIMAL(10, 2) COMMENT '单价',
    stock INT DEFAULT 0 COMMENT '库存',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    production_date DATE COMMENT '生产日期',
    expiry_date DATE COMMENT '有效期',
    category VARCHAR(50) COMMENT '药品分类',
    is_prescription TINYINT(1) DEFAULT 0 COMMENT '是否处方药: 1-是, 0-否',
    status TINYINT(1) DEFAULT 1 COMMENT '状态: 1-正常, 0-下架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '药品信息表';

-- 排班表
CREATE TABLE IF NOT EXISTS schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '排班ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    schedule_date DATE NOT NULL COMMENT '排班日期',
    time_slot VARCHAR(20) NOT NULL COMMENT '时间段(上午/下午/晚上)',
    max_patients INT DEFAULT 20 COMMENT '最大接诊人数',
    current_patients INT DEFAULT 0 COMMENT '当前预约人数',
    status TINYINT(1) DEFAULT 1 COMMENT '状态: 1-正常, 0-停诊',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (doctor_id) REFERENCES doctor(id) ON DELETE CASCADE,
    UNIQUE KEY `uk_doctor_date_time` (`doctor_id`, `schedule_date`, `time_slot`) COMMENT '同一医生同一天同一时间段只能有一条排班'
) COMMENT '医生排班表';

-- 预约表
CREATE TABLE IF NOT EXISTS appointment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预约ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    schedule_id BIGINT NOT NULL COMMENT '排班ID',
    appointment_no VARCHAR(50) UNIQUE NOT NULL COMMENT '预约编号',
    appointment_date DATE NOT NULL COMMENT '预约日期',
    time_slot VARCHAR(20) NOT NULL COMMENT '时间段(上午/下午/晚上)',
    symptoms TEXT COMMENT '症状描述',
    status TINYINT(1) DEFAULT 1 COMMENT '状态: 1-待就诊, 2-已就诊, 0-已取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctor(id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id) REFERENCES schedule(id) ON DELETE CASCADE
) COMMENT '预约挂号表'; 

-- ===========================
-- 药房采购（采购计划->采购单->验收单->入库单）
-- ===========================

-- 供应商表
CREATE TABLE IF NOT EXISTS supplier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '供应商ID',
    supplier_code VARCHAR(50) UNIQUE COMMENT '供应商编码',
    name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact_name VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(50) COMMENT '联系电话',
    address VARCHAR(255) COMMENT '地址',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT(1) DEFAULT 1 COMMENT '状态: 1-启用, 0-停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '供应商表';

-- 药品表补充供应商外键（在supplier表创建后执行）
ALTER TABLE medicine
    ADD CONSTRAINT fk_medicine_supplier
    FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE RESTRICT;

-- 供应商与药品对应关系表（用于采购单供应商可供药品校验）
CREATE TABLE IF NOT EXISTS supplier_medicine (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    medicine_id BIGINT NOT NULL COMMENT '药品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_supplier_medicine (supplier_id, medicine_id),
    FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id) REFERENCES medicine(id) ON DELETE CASCADE
) COMMENT '供应商与药品对应关系表';

-- 采购计划主表
CREATE TABLE IF NOT EXISTS purchase_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采购计划ID',
    plan_no VARCHAR(50) UNIQUE NOT NULL COMMENT '计划编号',
    title VARCHAR(200) COMMENT '计划主题',
    creator_user_id BIGINT COMMENT '创建人用户ID',
    status TINYINT(1) DEFAULT 0 COMMENT '状态: 0-草稿, 1-已提交, 2-已完结',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '采购计划主表';

-- 采购计划明细
CREATE TABLE IF NOT EXISTS purchase_plan_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计划明细ID',
    plan_id BIGINT NOT NULL COMMENT '采购计划ID',
    medicine_id BIGINT NOT NULL COMMENT '药品ID',
    plan_qty INT NOT NULL COMMENT '计划数量',
    purchased_qty INT DEFAULT 0 COMMENT '已下单数量(累计)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (plan_id) REFERENCES purchase_plan(id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id) REFERENCES medicine(id) ON DELETE RESTRICT
) COMMENT '采购计划明细表';

-- 采购单主表（由计划拆分，可拆多个供应商）
CREATE TABLE IF NOT EXISTS purchase_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采购单ID',
    order_no VARCHAR(50) UNIQUE NOT NULL COMMENT '采购单号',
    plan_id BIGINT NOT NULL COMMENT '来源采购计划ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    creator_user_id BIGINT COMMENT '创建人用户ID',
    status TINYINT(1) DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发送, 2-已验收完成',
    total_amount DECIMAL(12, 2) DEFAULT 0 COMMENT '总金额(可选)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (plan_id) REFERENCES purchase_plan(id) ON DELETE RESTRICT,
    FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE RESTRICT
) COMMENT '采购单主表';

-- 采购单明细
CREATE TABLE IF NOT EXISTS purchase_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采购单明细ID',
    order_id BIGINT NOT NULL COMMENT '采购单ID',
    plan_item_id BIGINT NOT NULL COMMENT '来源计划明细ID',
    medicine_id BIGINT NOT NULL COMMENT '药品ID',
    order_qty INT NOT NULL COMMENT '下单数量',
    unit_price DECIMAL(12, 2) DEFAULT 0 COMMENT '含税单价(可选)',
    amount DECIMAL(12, 2) DEFAULT 0 COMMENT '金额(可选)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (order_id) REFERENCES purchase_order(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_item_id) REFERENCES purchase_plan_item(id) ON DELETE RESTRICT,
    FOREIGN KEY (medicine_id) REFERENCES medicine(id) ON DELETE RESTRICT
) COMMENT '采购单明细表';

-- 验收单主表（必须来源采购单）
CREATE TABLE IF NOT EXISTS purchase_acceptance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '验收单ID',
    acceptance_no VARCHAR(50) UNIQUE NOT NULL COMMENT '验收单号',
    purchase_order_id BIGINT NOT NULL COMMENT '来源采购单ID',
    inspector_user_id BIGINT COMMENT '验收人用户ID',
    acceptance_time DATETIME COMMENT '验收时间',
    status TINYINT(1) DEFAULT 0 COMMENT '状态: 0-草稿, 1-已完成',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_order(id) ON DELETE RESTRICT
) COMMENT '采购验收单主表';

-- 验收单明细
CREATE TABLE IF NOT EXISTS purchase_acceptance_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '验收明细ID',
    acceptance_id BIGINT NOT NULL COMMENT '验收单ID',
    purchase_order_item_id BIGINT NOT NULL COMMENT '来源采购单明细ID',
    medicine_id BIGINT NOT NULL COMMENT '药品ID',
    ordered_qty INT NOT NULL COMMENT '下单数量(冗余)',
    received_qty INT NOT NULL COMMENT '到货数量',
    qualified_qty INT NOT NULL COMMENT '合格数量',
    batch_no VARCHAR(100) COMMENT '批号',
    production_date DATE COMMENT '生产日期',
    expiry_date DATE COMMENT '有效期',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (acceptance_id) REFERENCES purchase_acceptance(id) ON DELETE CASCADE,
    FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_item(id) ON DELETE RESTRICT,
    FOREIGN KEY (medicine_id) REFERENCES medicine(id) ON DELETE RESTRICT
) COMMENT '采购验收单明细表';

-- 入库单主表（必须来源验收单）
CREATE TABLE IF NOT EXISTS stock_in_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '入库单ID',
    stock_in_no VARCHAR(50) UNIQUE NOT NULL COMMENT '入库单号',
    acceptance_id BIGINT NOT NULL COMMENT '来源验收单ID',
    operator_user_id BIGINT COMMENT '入库操作人用户ID',
    stock_in_time DATETIME COMMENT '入库时间',
    status TINYINT(1) DEFAULT 0 COMMENT '状态: 0-草稿, 1-已过账',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (acceptance_id) REFERENCES purchase_acceptance(id) ON DELETE RESTRICT
) COMMENT '入库单主表';

-- 入库单明细（仅允许来自验收合格数量）
CREATE TABLE IF NOT EXISTS stock_in_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '入库明细ID',
    stock_in_id BIGINT NOT NULL COMMENT '入库单ID',
    acceptance_item_id BIGINT NOT NULL COMMENT '来源验收明细ID',
    medicine_id BIGINT NOT NULL COMMENT '药品ID',
    stock_in_qty INT NOT NULL COMMENT '入库数量',
    unit_cost DECIMAL(12, 2) DEFAULT 0 COMMENT '单位成本(可选)',
    amount DECIMAL(12, 2) DEFAULT 0 COMMENT '金额(可选)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (stock_in_id) REFERENCES stock_in_order(id) ON DELETE CASCADE,
    FOREIGN KEY (acceptance_item_id) REFERENCES purchase_acceptance_item(id) ON DELETE RESTRICT,
    FOREIGN KEY (medicine_id) REFERENCES medicine(id) ON DELETE RESTRICT
) COMMENT '入库单明细表';