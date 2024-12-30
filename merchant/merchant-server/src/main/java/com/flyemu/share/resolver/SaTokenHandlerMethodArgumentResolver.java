package com.flyemu.share.resolver;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.flyemu.share.annotation.*;
import com.flyemu.share.common.Constants;
import com.flyemu.share.dto.AccountDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @功能描述: SaToken
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Slf4j
@Component
public class SaTokenHandlerMethodArgumentResolver implements SaHandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ((parameter.getParameterType().isAssignableFrom(AccountDto.class)) && parameter.hasParameterAnnotation(SaAccountVal.class))
                || ((parameter.getParameterType().isAssignableFrom(Long.class)) && parameter.hasParameterAnnotation(SaMerchantId.class))
                || ((parameter.getParameterType().isAssignableFrom(Long.class)) && parameter.hasParameterAnnotation(SaAdminId.class))
                || ((parameter.getParameterType().isAssignableFrom(Long.class)) && parameter.hasParameterAnnotation(SaAccountBookId.class));
    }


    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        StpUtil.checkLogin();
        SaSession session = StpUtil.getTokenSession();

        AccountDto accountDto = session.getModel(Constants.SESSION_ACCOUNT, AccountDto.class);

        if (parameter.hasParameterAnnotation(SaAccountVal.class)) {
            return accountDto;
        }

        if (parameter.hasParameterAnnotation(SaMerchantId.class)) {
            return accountDto.getMerchantId();
        }

        if (parameter.hasParameterAnnotation(SaAdminId.class)) {
            return accountDto.getAdminId();
        }

        if (parameter.hasParameterAnnotation(SaAccountBookId.class)) {
            return accountDto.getAccountBookId();
        }

        return null;
    }
}
