# 更新

## 预填表单需求

### 需求描述

在商户平台(merchant)需要能够填入预填的订单信息，并给出预填订单的链接/二维码供用户实现下单支付。

### 需求分析

1.  商户在平台(merchant)预填订单信息，平台给出供用户下单支付的预填订单的链接/二维码。
2.  预填订单需要满足以下条件：
    *   商家可以预先定义好订单的内容，包括金额、标题、描述、备注等。
    *   由于需要满足用户开具发票的需求，备注功能可以分为一般备注和发票备注，是否必填。
    *   针对发票备注，需要能够对发票备注进行分类，个人发票：姓名、邮箱号；企业发票：姓名、单位、税号、邮箱号。
    *   预填订单存在有效时间，未到开始时间/超过截止时间，预填订单无效，且可主动失效订单。
    *   预填订单需要支持多种支付方式，如微信支付、支付宝支付等。
    *   预填订单配置完成后，需要能够被用户进行访问，能够允许用户选择支付方式。
3.  预填订单数据库设计：
    ```sql
      -- 预填订单表
      DROP TABLE IF EXISTS t_prefilled_order;
      CREATE TABLE `t_prefilled_order` (
         `prefilled_order_id` VARCHAR(30) NOT NULL COMMENT '预填订单ID (这个是给用户访问的唯一凭证，会放在链接或二维码里)',
         `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号 (哪个商家创建的)',
         `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID (商家通过哪个应用创建的)',
         `subject` VARCHAR(64) NOT NULL COMMENT '订单标题 (将显示在支付软件的支付信息页面)',
         `amount` BIGINT(20) NOT NULL COMMENT '支付金额,单位分',
         `currency` VARCHAR(3) NOT NULL DEFAULT 'cny' COMMENT '三位货币代码 (默认是人民币 cny)',
         `body` VARCHAR(256) DEFAULT NULL COMMENT '订单描述信息 (更详细的说明)',

         `remark_config` JSON DEFAULT NULL COMMENT '备注配置 (这里用JSON格式存，告诉系统要不要让用户填备注、发票怎么填等。',
         -- 备注配置的例子 (几种可能的方式):
         -- 方式1 (默认例子): 允许普通备注但不强制，允许发票备注且必须填，发票类型支持个人和企业。
         -- {"general_enabled": true, "general_required": false, "invoice_enabled": true, "invoice_required": true, "allowed_invoice_types": ["personal", "corporate"]}
         -- 方式2: 只允许普通备注，且必须填，不允许发票。
         -- {"general_enabled": true, "general_required": true, "invoice_enabled": false, "invoice_required": false, "allowed_invoice_types": []}
         -- 方式3: 只允许发票备注，不强制填，只支持企业发票。
         -- {"general_enabled": false, "general_required": false, "invoice_enabled": true, "invoice_required": false, "allowed_invoice_types": ["corporate"]}
         -- 方式4: 不允许任何备注和发票配置。
         -- {"general_enabled": false, "general_required": false, "invoice_enabled": false, "invoice_required": false, "allowed_invoice_types": []}

         `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用 (商家可以手动让它失效)',
         `start_time` DATETIME DEFAULT NULL COMMENT '生效开始时间 (如果没填，立刻生效)',
         `end_time` DATETIME DEFAULT NULL COMMENT '生效结束时间 (如果没填，就永远有效)',
         `max_usage_count` INT DEFAULT NULL COMMENT '最大成功支付次数 (比如搞活动，前100名有效，null表示不限制)',
         `current_usage_count` INT NOT NULL DEFAULT 0 COMMENT '当前成功支付次数 (记录有多少人通过这个模板成功付款了)',

         `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
         `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
         PRIMARY KEY (`prefilled_order_id`),
         INDEX `idx_mch_app` (`mch_no`, `app_id`), -- 方便按商家查询
         INDEX `idx_created_at` (`created_at`), -- 按创建时间查询
         INDEX `idx_end_time` (`end_time`) -- 方便查询快过期的
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预填订单配置表';
    ```
4.  支付订单表关联（建议）：
    为了追踪哪个支付订单是从哪个预填订单来的，建议在 `t_pay_order` 表中增加一个字段：
    ```sql
    -- 建议: 在 t_pay_order 表中增加一个字段用于关联
    ALTER TABLE t_pay_order ADD COLUMN source_prefilled_order_id VARCHAR(30) NULL COMMENT '来源预填订单ID';
    ALTER TABLE t_pay_order ADD INDEX idx_source_prefilled (source_prefilled_order_id); -- 加个索引方便查询
    ```
5. 用户无需登陆即可完成订单确认-信息填写-支付订单的流程

### 交互流程

1.  **针对商家 (Merchant)：**
    *   登录商户平台，找到“预填订单”功能入口。
    *   点击“新建”。
    *   填写订单基本信息：标题 (`subject`)、金额 (`amount`)、描述 (`body`)。
    *   配置备注和发票信息 (`remark_config`)：
        *   是否启用普通备注，是否必填。
        *   是否启用发票备注，是否必填，支持哪种发票类型（个人、企业）。
    *   （可选）选择允许的支付方式 (`allowed_pay_ways`)，不选则默认支持该商户开通的所有方式。
    *   （可选）设置生效时间段 (`start_time`, `end_time`)。
    *   （可选）设置最大可用次数 (`max_usage_count`)。
    *   保存创建。
    *   在“预填订单列表”中，可以看到创建好的订单模板。
    *   可以获取每个模板的专属链接或二维码，发给用户。
    *   可以查看每个模板的使用次数 (`current_usage_count`)。
    *   可以手动“启用”或“禁用” (`status`) 某个模板。
    *   可以编辑或删除模板。

2.  **针对用户 (User)：**
    *   前端界面使用步骤条：确认订单信息->填写备注->下单支付->完成支付
    *   用户通过手机扫描商家给的二维码，或者点击链接。
    *   系统后台检查这个预填订单 (`t_prefilled_order`) 是否有效：
        *   状态 (`status`) 是不是启用 (1)？
        *   当前时间是否在 `start_time` 和 `end_time` 之间（如果设置了的话）？
        *   当前使用次数 (`current_usage_count`) 是否小于最大次数 (`max_usage_count`)（如果设置了的话）？
        *   如果无效，提示用户“该订单已失效”或类似信息。
    *   如果有效，展示一个订单确认页面给用户：
        *   显示订单标题 (`subject`)、金额 (`amount`)、描述 (`body`)。
    *   根据 `remark_config` 的设置，判断是否需要用户填写信息：
        *   如果需要普通备注，显示备注输入框（如果必填，提示用户必须填写）。
        *   如果需要发票备注，显示发票信息输入框（根据支持的类型，可能需要填姓名、邮箱、单位、税号等；如果必填，提示用户必须填写）。
    *   用户确认信息（如果需要填写，则填写完毕后）并点击“确认支付”或类似按钮。
    *   系统展示可用的支付方式（根据 `allowed_pay_ways` 或商户默认设置）。
    *   用户选择支付方式（比如微信支付）。
    *   系统调用支付接口，可能会跳转到微信支付页面，或者显示一个支付二维码让用户扫码。
    *   用户完成支付。
    *   支付成功后：
        *   系统记录一条真实的支付订单到 `t_pay_order` 表，并把 `prefilled_order_id` 记在 `source_prefilled_order_id` 字段里。
        *   系统更新 `t_prefilled_order` 表里对应模板的 `current_usage_count`（加 1）。
        *   给用户显示支付成功页面。
    *   支付失败或用户取消：
        *   系统记录一条失败或关闭的支付订单到 `t_pay_order` 表。
        *   给用户显示支付失败或订单已关闭的页面。
