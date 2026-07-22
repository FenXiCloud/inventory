package com.flyemu.share.resolver;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.Constants;
import com.flyemu.share.dto.AccountDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
public class SaTokenHandlerMethodArgumentResolver implements SaHandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ((parameter.getParameterType().isAssignableFrom(AccountDto.class)) && parameter.hasParameterAnnotation(SaAccountVal.class))
                || ((parameter.getParameterType().isAssignableFrom(Long.class)) && parameter.hasParameterAnnotation(SaAdminId.class));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        StpUtil.checkLogin();
        SaSession session = StpUtil.getTokenSession();

        AccountDto accountDto = session.getModel(Constants.SESSION_ACCOUNT, AccountDto.class);

        if (parameter.hasParameterAnnotation(SaAccountVal.class)) {
            return accountDto;
        }

        if (parameter.hasParameterAnnotation(SaAdminId.class)) {
            return accountDto.getAdminId();
        }

        return null;
    }
}
