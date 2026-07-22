package com.flyemu.share;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MerchantApplication {
    public static void main(String[] args) {
        SpringApplication.run(MerchantApplication.class, args);
        System.err.println("=========================================");
        System.err.println("=           启动成功！欢迎使用           =");
        System.err.println("=========================================");
    }
}
