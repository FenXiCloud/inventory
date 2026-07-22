package com.flyemu.share.controller.advice;

import cn.dev33.satoken.exception.InvalidContextException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.core.collection.CollUtil;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.exception.ServiceException;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class ErrorControllerAdvice {

    /**
     * 处理@Validated参数校验失败异常
     *
     * @param exception 异常类
     * @return 响应
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public JsonResult exceptionHandler(MethodArgumentNotValidException exception) {
        BindingResult result = exception.getBindingResult();
        List<String> errors = new ArrayList<>();
        if (result.hasErrors()) {
            result.getAllErrors().forEach(p -> {
                FieldError fieldError = (FieldError) p;
                log.warn("Bad Request Parameters: dto entity [{}],field [{}],message [{}]", fieldError.getObjectName(), fieldError.getField(), fieldError.getDefaultMessage());
                errors.add(fieldError.getDefaultMessage());
            });
        }
        return JsonResult.failure(CollUtil.join(errors, "，"));
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public JsonResult exceptionHandler(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> result = exception.getConstraintViolations();
        List<String> errors = new ArrayList<>();
        result.forEach(p -> errors.add(p.getMessage()));
        return JsonResult.failure(CollUtil.join(errors, "，"));
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public JsonResult exceptionHandler(BindException exception) {
        BindingResult result = exception.getBindingResult();
        List<String> errors = new ArrayList<>();
        result.getAllErrors().forEach(p -> errors.add(p.getDefaultMessage()));
        return JsonResult.failure(CollUtil.join(errors, "，"));
    }

    /**
     * 服务异常
     *
     * @param throwable
     * @return
     */
    @ExceptionHandler({ServiceException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    public JsonResult exception(final RuntimeException throwable) {
        log.error("ErrorControllerAdvice", throwable);
        Sentry.captureException(throwable);
        return JsonResult.failure(throwable.getMessage());
    }

    // 全局异常拦截（拦截项目中的NotLoginException异常）
    @ExceptionHandler(NotLoginException.class)
    public JsonResult handlerNotLoginException(NotLoginException nle) {
        // 判断场景值，定制化异常信息
        String message = "";
        if (nle.getType().equals(NotLoginException.NOT_TOKEN)) {
            message = "";
        } else if (nle.getType().equals(NotLoginException.INVALID_TOKEN)) {
            message = "";
        } else if (nle.getType().equals(NotLoginException.TOKEN_TIMEOUT)) {
            message = "登录已过期";
        } else if (nle.getType().equals(NotLoginException.BE_REPLACED)) {
            message = "登录已被顶下线";
        } else if (nle.getType().equals(NotLoginException.KICK_OUT)) {
            message = "登录已被踢下线";
        } else {
            message = "当前会话未登录";
        }
        log.warn("token状态：{}", nle.getType());
        // 返回给前端
        return JsonResult.failure(message, 401);
    }

    /**
     * 统一其他异常
     *
     * @param throwable
     * @return
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public JsonResult exception(final Throwable throwable) {
        log.error("Throwable Error", throwable);
        Sentry.captureException(throwable);
        return JsonResult.failure("操作被禁止或发生错误！");
    }

    /**
     * 无效内容
     * @return
     */
    @ExceptionHandler(InvalidContextException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public JsonResult invalidContextException(final InvalidContextException invalidContextException) {
        log.error("invalidContextException", invalidContextException);
        Sentry.captureException(invalidContextException);
        return JsonResult.failure(invalidContextException.getMessage());
    }
}
