package com.jeequan.jeepay.core.entity;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * description...
 *
 * @author zkye
 * @version 1.0
 * @see <a href=""></a>
 * @since 2025/4/24
 */
@Data
@NoArgsConstructor
public class RemarkConfig {

    /**
     * 是否启用普通备注
     */
    @NotNull
    private Boolean generalEnabled;

    /**
     * 普通备注是否必填
     */
    @NotNull
    private Boolean generalRequired;

    /**
     * 是否启用发票备注
     */
    @NotNull
    private Boolean invoiceEnabled;

    /**
     * 发票备注是否必填
     */
    @NotNull
    private Boolean invoiceRequired;

    /**
     * 支持的发票类型（"personal","corporate"）
     */
    private List<@Pattern(regexp = "personal|corporate", message = "可选的类型为：personal、corporate") String> allowedInvoiceTypes;

    /**
     * 校验复杂业务规则
     */
    public void validate() {
        if (invoiceRequired && !invoiceEnabled) {
            throw new IllegalArgumentException("invoiceRequired 为 true 时 invoiceEnabled 必须为 true");
        }
        if (invoiceEnabled && allowedInvoiceTypes.isEmpty()) {
            throw new IllegalArgumentException("invoiceEnabled 为 true 时 allowedInvoiceTypes 不能为空");
        }
    }

    /**
     * 默认配置工厂方法
     */
    public static RemarkConfig defaultConfig() {
        RemarkConfig c = new RemarkConfig();
        c.generalEnabled = true;
        c.generalRequired = false;
        c.invoiceEnabled = true;
        c.invoiceRequired = true;
        c.allowedInvoiceTypes = Arrays.asList("personal", "corporate");
        return c;
    }
}
