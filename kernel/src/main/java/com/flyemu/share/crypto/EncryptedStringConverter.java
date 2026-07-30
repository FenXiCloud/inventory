package com.flyemu.share.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA 属性转换器：自动对标记了 @Convert 的敏感字符串字段进行 AES 加解密。
 * <p>
 * 用法：在实体字段上添加 {@code @Convert(converter = EncryptedStringConverter.class)}
 * <p>
 * 加解密逻辑委托给 {@link AesUtil}，密钥通过环境变量 APP_CRYPTO_AES_KEY 或
 * 系统属性 app.crypto.aes-key 注入。
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return AesUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return AesUtil.decrypt(dbData);
    }
}
