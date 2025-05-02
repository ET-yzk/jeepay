package com.jeequan.jeepay.core.validation;

import com.jeequan.jeepay.core.annotation.ZeroOrOne;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * description...
 *
 * @author zkye
 * @version 1.0
 * @see <a href=""></a>
 * @since 2025/4/24
 */
public class ZeroOrOneValidator  implements ConstraintValidator<ZeroOrOne, Byte> {
    @Override
    public boolean isValid(Byte value, ConstraintValidatorContext context) {
        if (value == null) return true;  // 允许 null（若需禁止，需配合 @NotNull）
        return value == 0 || value == 1;
    }
}
