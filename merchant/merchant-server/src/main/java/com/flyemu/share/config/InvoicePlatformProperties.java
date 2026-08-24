package com.flyemu.share.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "invoice.platform")
public class InvoicePlatformProperties {
    /** true = 模拟模式，不调用真实纷析云接口 */
    private boolean mockEnabled = true;
    /** 平台账号配置 */
    private Account account = new Account();

    @Data
    public static class Account {
        private String username;
        private String password;
        private String phone;
        /** 登录人真实姓名 */
        private String realName;
        /** 登录身份：办税员/开票员/法定代表人/财务负责人 */
        private String roleName;
    }

    /** 销方（开票方）信息 */
    private Seller seller = new Seller();

    @Data
    public static class Seller {
        /** 销方名称 */
        private String name;
        /** 销方电话 */
        private String phone;
        /** 发票类型: 030=数电普票, 032=数电专票 */
        private String invoiceType = "030";
        /** 销方开户银行 */
        private String bank;
        /** 销方银行账号 */
        private String bankAccount;
    }
}
