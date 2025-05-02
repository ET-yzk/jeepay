package com.jeequan.jeepay.core.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import com.jeequan.jeepay.core.annotation.ZeroOrOne;
import com.jeequan.jeepay.core.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 预填订单配置表
 * </p>
 *
 * @author [mybatis plus generator]
 * @since 2025-04-23
 */
@Schema(description = "预填订单表")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_prefilled_order")
public class PrefilledOrder extends BaseModel {

    public static LambdaQueryWrapper<PrefilledOrder> gw(){
        return new LambdaQueryWrapper<>();
    }

    @Serial
    private static final long serialVersionUID=1L;

    /**
     * 预填订单ID 
     */
    @Schema(title = "prefilledOrderId", description = "预填订单号")
    @TableId
    private String prefilledOrderId;

    /**
     * 商户号
     */
    @Schema(title = "mchNo", description = "商户号")
    // 默认当前登录用户的商户号
    // @NotBlank(message = "商户号不能为空")
    private String mchNo;

    /**
     * 应用ID
     */
    @Schema(title = "appId", description = "应用ID")
    @NotBlank(message = "应用ID不能为空")
    private String appId;

    /**
     * 订单标题
     */
    @Schema(title = "subject", description = "商品标题")
    @NotBlank(message = "订单标题不能为空")
    private String subject;

    /**
     * 支付金额,单位分
     */
    @Schema(title = "amount", description = "支付金额,单位分")
    @Positive(message = "支付金额必须大于0")
    private Long amount;

    /**
     * 三位货币代码 (默认是人民币 cny)
     */
    @Schema(title = "currency", description = "三位货币代码,人民币:cny")
    // 可空，已在数据库定义默认值
    // @NotBlank(message = "币种不能为空")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "币种必须是3位字母")
    private String currency;

    /**
     * 订单描述信息
     */
    @Schema(title = "body", description = "订单描述信息")
    @Size(max = 256, message = "描述信息长度不能超过256字符")
    private String body;

    /**
     * 备注配置，自动序列化/反序列化为 JSON
     */
    @Schema(title = "remarkConfig", description = "备注配置")
    @Valid
    @TableField(value = "remark_config", typeHandler = FastjsonTypeHandler.class)
    private RemarkConfig remarkConfig;

    /**
     * 状态: 0-禁用, 1-启用
     */
    @Schema(title = "status", description = "状态: 0-禁用, 1-启用")
    // 可空，已在数据库定义默认值
    // @NotNull(message = "状态不能为空")
    @ZeroOrOne
    private Byte status;

    /**
     * 生效开始时间
     */
    @Schema(title = "startTime", description = "生效开始时间")
    private Date startTime;

    /**
     * 生效结束时间
     */
    @Schema(title = "endTime", description = "生效结束时间")
    private Date endTime;

    /**
     * 最大成功支付次数
     */
    @Schema(title = "maxUsageCount", description = "最大成功支付次数")
    @Min(value = 1, message = "最大使用次数必须大于0")
    private Integer maxUsageCount;

    /**
     * 当前成功支付次数
     */
    @Schema(title = "currentUsageCount", description = "当前成功支付次数")
    private Integer currentUsageCount;

    /**
     * 创建时间
     */
    @Schema(title = "createdAt", description = "创建时间")
    private Date createdAt;

    /**
     * 更新时间
     */
    @Schema(title = "updatedAt", description = "更新时间")
    private Date updatedAt;

    @AssertTrue(message = "生效时间必须早于失效时间")
    public boolean isStartTimeBeforeEndTime() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return startTime.before(endTime);
    }
}
