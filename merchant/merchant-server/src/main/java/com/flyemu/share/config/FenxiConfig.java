package com.flyemu.share.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 纷析云开放平台 SDK 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "fenxi")
public class FenxiConfig {
    private String appId;
    private String key;
    private String endpoint;
}
