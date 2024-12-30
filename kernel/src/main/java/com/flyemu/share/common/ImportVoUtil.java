package com.flyemu.share.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import com.flyemu.share.annotation.ColName;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>****************************************************************************</p>
 * <p><b>Copyright © 2010-2021纷析云（杭州）科技有限公司All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : cn.gson.hr.kernel.common</li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2021年08月15日</li>
 * <li>@author     : ____′↘夏悸</li>
 * </ul>
 * <p>****************************************************************************</p>
 */
@Slf4j
public final class ImportVoUtil {

    /**
     * 获取列别名
     *
     * @param type
     * @return
     */
    public static void setHeaderAlias(Class<?> type, ExcelWriter writer) {
        setHeaderAlias(type, writer, false);
    }

    /**
     * @param type
     * @param revert
     * @return
     */
    public static void setHeaderAlias(Class<?> type, ExcelWriter writer, boolean revert) {
        Map<String, String> headerAlias = new HashMap<>();
        Field[] colFields = ReflectUtil.getFields(type, field -> field.isAnnotationPresent(ColName.class));
        List<String> as = new ArrayList<>();
        for (Field colField : colFields) {
            if (revert) {
                writer.addHeaderAlias(colField.getAnnotation(ColName.class).value(), colField.getName());
            } else {
                as.add("writer.addHeaderAlias(\"" + colField.getName() + "\", \"" + colField.getAnnotation(ColName.class).value() + "\");");
                writer.addHeaderAlias(colField.getName(), colField.getAnnotation(ColName.class).value());
            }
        }
        log.info("\n{}", CollUtil.join(as, "\n"));
    }

    public static void setHeaderAlias(Class<?> type, ExcelReader reader) {
        Field[] colFields = ReflectUtil.getFields(type, field -> field.isAnnotationPresent(ColName.class));
        for (Field colField : colFields) {
            reader.addHeaderAlias(colField.getAnnotation(ColName.class).value(), colField.getName());
        }
    }
}
