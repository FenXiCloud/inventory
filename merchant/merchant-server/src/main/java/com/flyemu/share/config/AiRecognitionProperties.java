package com.flyemu.share.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.recognition")
public class AiRecognitionProperties {
    /** true = 启用大模型识别；false 或未配置 apiKey 时走规则降级 */
    private boolean enabled = false;
    /** API地址 */
    private String endpoint;
    /** API密钥 */
    private String apiKey;
    /** 模型名称 */
    private String model = "mimo-v2.5-pro";
}
