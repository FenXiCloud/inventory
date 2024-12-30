package com.flyemu.share.converter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import jakarta.persistence.AttributeConverter;
import java.util.List;

/**
 * @功能描述: CONVERTER
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
public class StringListConverter implements AttributeConverter<List<?>, String> {

    @Override
    public String convertToDatabaseColumn(List<?> attribute) {
        return CollUtil.isNotEmpty(attribute) ? JSON.toJSONString(attribute) : null;
    }

    @Override
    public List<?> convertToEntityAttribute(String dbData) {
        return StrUtil.isNotEmpty(dbData) && !StrUtil.isNullOrUndefined(dbData) ? JSONArray.parseArray(dbData) : null;
    }
}
