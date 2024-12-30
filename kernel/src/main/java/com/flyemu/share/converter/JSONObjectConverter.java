package com.flyemu.share.converter;


import cn.hutool.json.JSONObject;

import jakarta.persistence.AttributeConverter;
import java.io.Serializable;

/**
 * @功能描述: CONVERTER
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
public class JSONObjectConverter implements AttributeConverter<JSONObject, String>, Serializable {

    @Override
    public String convertToDatabaseColumn(JSONObject jsonObject) {
        return jsonObject != null ? jsonObject.toString() : null;
    }

    @Override
    public JSONObject convertToEntityAttribute(String string) {
        return new JSONObject(string);
    }
}
