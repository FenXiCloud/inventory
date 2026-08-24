package com.flyemu.share.config;

import com.fenxi365.api.Fenxi365;
import com.fenxi365.api.Fenxi365Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FenxiBeanConfig {

    @Bean
    public Fenxi365 fenxi365(FenxiConfig config) {
        return Fenxi365Client.builder()
                .appId(config.getAppId())
                .secretKey(config.getKey())
                .endpoint(config.getEndpoint())
                .build();
    }
}
