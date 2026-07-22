package com.flyemu.share.converter;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
public class LocalDateConverter implements Converter<String, LocalDate> {

    @Override
    public LocalDate convert(String source) {
        if (StrUtil.isEmpty(source)) {
            return null;
        }
        source = DateUtil.parse(source, "yyyy-MM-dd", "yyyy-MM").toString("yyyy-MM-dd");
        return LocalDate.parse(source, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
