package com.jeequan.jeepay.core.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.model.ApiRes;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 定义全局校验异常捕获
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiRes handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        return ApiRes.fail(ApiCodeEnum.PARAMS_ERROR, "参数校验失败", errors);
    }

    // 处理解析异常（如非法输入），JSON解析失败等
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiRes handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String errorMessage = ex.getMessage();
        return ApiRes.fail(ApiCodeEnum.PARAMS_ERROR, errorMessage);
    }

    // 在这里添加其他 @ExceptionHandler 方法来处理其他类型的异常，
    // 并同样返回 ApiRes 对象，以保持风格一致。
    // 例如，可以处理前面提到的 JeepayAuthenticationException, ResponseException 等，
    // 但 BizExceptionResolver 已经处理了 BizException，所以需要避免重复处理。
}