package com.jeequan.jeepay.core.annotation;

import com.jeequan.jeepay.core.validation.ZeroOrOneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验0/1注解的元数据
 *
 * @author zkye
 * @version 1.0
 * @see <a href=""></a>
 * @since 2025/4/24
 */

@Documented
@Constraint(validatedBy = ZeroOrOneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ZeroOrOne {
    String message() default "值必须是0或1";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
