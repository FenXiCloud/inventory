package com.flyemu.share.converter;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;

import java.util.Date;

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
