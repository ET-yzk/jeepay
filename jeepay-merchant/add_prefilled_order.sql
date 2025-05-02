-- CREATE TABLE `t_sys_entitlement` (
--   `ent_id` VARCHAR(64) NOT NULL COMMENT '权限ID[ENT_功能模块_子模块_操作], eg: ENT_ROLE_LIST_ADD',
--   `ent_name` VARCHAR(32) NOT NULL COMMENT '权限名称',
--   `menu_icon` VARCHAR(32) COMMENT '菜单图标',
--   `menu_uri` VARCHAR(128) COMMENT '菜单uri/路由地址',
--   `component_name` VARCHAR(32) COMMENT '组件Name（前后端分离使用）',
--   `ent_type` CHAR(2) NOT NULL COMMENT '权限类型 ML-左侧显示菜单, MO-其他菜单, PB-页面/按钮',
--   `quick_jump` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '快速开始菜单 0-否, 1-是',
--   `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '状态 0-停用, 1-启用',
--   `pid` VARCHAR(32) NOT NULL COMMENT '父ID',
--   `ent_sort` INT(11) NOT NULL DEFAULT 0 COMMENT '排序字段, 规则：正序',
--   `sys_type` VARCHAR(8) NOT NULL COMMENT '所属系统： MGR-运营平台, MCH-商户中心',
--   `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
--   `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
--   PRIMARY KEY (`ent_id`, `sys_type`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 预填订单
insert into t_sys_entitlement values('ENT_PREFILLED_ORDER', '预填订单', 'carry-out', '/prefilledOrder', 'PrefilledOrderListPage', 'ML', 0, 1, 'ENT_ORDER', '5', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_PREFILLED_ORDER_LIST', '页面：预填订单列表', 'no-icon', '', '', 'PB', 0, 1, 'ENT_PREFILLED_ORDER', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_PREFILLED_ORDER_ADD', '按钮：新增', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PREFILLED_ORDER', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_PREFILLED_ORDER_EDIT', '按钮：编辑', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PREFILLED_ORDER', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_PREFILLED_ORDER_VIEW', '按钮：详情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PREFILLED_ORDER', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_PREFILLED_ORDER_DEL', '按钮：删除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PREFILLED_ORDER', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_PREFILLED_ORDER_PAYWAY_LIST', '页面：获取预填订单支持的支付方式列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PREFILLED_ORDER', '0', 'MCH', now(), now());

-- 为支付订单和预填订单添加关联字段
ALTER TABLE t_pay_order ADD COLUMN source_prefilled_order_id VARCHAR(30) NULL COMMENT '来源预填订单ID';
ALTER TABLE t_pay_order ADD INDEX idx_source_prefilled (source_prefilled_order_id);


-- 预填订单表
DROP TABLE IF EXISTS t_prefilled_order;
CREATE TABLE `t_prefilled_order` (
    `prefilled_order_id` VARCHAR(30) NOT NULL COMMENT '预填订单ID ',
    `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号',
    `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID',
    `subject` VARCHAR(64) NOT NULL COMMENT '订单标题',
    `amount` BIGINT(20) NOT NULL COMMENT '支付金额,单位分',
    `currency` VARCHAR(3) NOT NULL DEFAULT 'cny' COMMENT '三位货币代码 (默认是人民币 cny)',
    `body` VARCHAR(256) DEFAULT NULL COMMENT '订单描述信息',

    `remark_config` JSON DEFAULT NULL COMMENT '备注配置',
    -- 备注配置的例子 (几种可能的方式):
    -- 方式1 (默认例子): 允许普通备注但不强制，允许发票备注且必须填，发票类型支持个人和企业。
    -- {"general_enabled": true, "general_required": false, "invoice_enabled": true, "invoice_required": true, "allowed_invoice_types": ["personal", "corporate"]}
    -- 方式2: 只允许普通备注，且必须填，不允许发票。
    -- {"general_enabled": true, "general_required": true, "invoice_enabled": false, "invoice_required": false, "allowed_invoice_types": []}
    -- 方式3: 只允许发票备注，不强制填，只支持企业发票。
    -- {"general_enabled": false, "general_required": false, "invoice_enabled": true, "invoice_required": false, "allowed_invoice_types": ["corporate"]}
    -- 方式4: 不允许任何备注和发票配置。
    -- {"general_enabled": false, "general_required": false, "invoice_enabled": false, "invoice_required": false, "allowed_invoice_types": []}

    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `start_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '生效开始时间',
    `end_time` TIMESTAMP DEFAULT NULL COMMENT '生效结束时间',
    `max_usage_count` INT DEFAULT NULL COMMENT '最大成功支付次数',
    `current_usage_count` INT NOT NULL DEFAULT 0 COMMENT '当前成功支付次数',

    `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`prefilled_order_id`),
    INDEX `idx_mch_app` (`mch_no`, `app_id`), -- 方便按商家查询
    INDEX `idx_created_at` (`created_at`), -- 按创建时间查询
    INDEX `idx_end_time` (`end_time`) -- 方便查询快过期的
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预填订单配置表';