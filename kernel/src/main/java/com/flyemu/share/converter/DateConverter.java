package com.flyemu.share.converter;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;

import java.util.Date;


/**
 * @功能描述: CONVERTER
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Slf4j
public class DateConverter implements Converter<String, Date> {

    private final String[] formatter = {"yyyy-MM-dd", "yyyy-MM", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH"};

    @Override
    public Date convert(String source) {
        if (StrUtil.isEmpty(source)) {
            return null;
        }
        return DateUtil.parse(source, formatter);
    }
}
