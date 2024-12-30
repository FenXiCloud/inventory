package com.flyemu.share.resolver;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.flyemu.share.annotation.SaUserVal;
import com.flyemu.share.common.Constants;
import com.flyemu.share.entity.MerchantUser;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class SaTokenHandlerMethodArgumentResolver implements SaHandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ((parameter.getParameterType().isAssignableFrom(MerchantUser.class)) && parameter.hasParameterAnnotation(SaUserVal.class));
    }


    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        StpUtil.checkLogin();
        SaSession session = StpUtil.getSession();

        MerchantUser merchantUser = session.getModel(Constants.SESSION_ACCOUNT, MerchantUser.class);

        if (parameter.hasParameterAnnotation(SaUserVal.class)) {
            return merchantUser;
        }

        return null;
    }
}
