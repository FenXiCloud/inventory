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
    /** OpenAI 兼容 chat/completions 地址，如 https://api.openai.com/v1/chat/completions */
    private String endpoint;
    private String apiKey;
    private String model = "gpt-4o-mini";
}
