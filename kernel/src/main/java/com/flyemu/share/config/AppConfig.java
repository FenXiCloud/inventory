package com.flyemu.share.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @功能描述: AppConfig
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@Component
@ConfigurationProperties("app")
public class AppConfig {

    private String uploadRoot = SystemUtil.get(SystemUtil.USER_DIR);

    private ALiYun aliyun;

    public File getUploadRoot() {
        File root;
        if (StrUtil.isEmpty(this.uploadRoot)) {
            root = new File(SystemUtil.getUserInfo().getHomeDir(), "budland");
        } else {
            root = new File(uploadRoot);
        }
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    public File getUploadRoot(String child) {
        File root = new File(this.getUploadRoot(), child);
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }


    @Data
    public static class ALiYun {
        /**
         * 账号D
         */
        private String accessKeyId;
        /**
         * 账号秘钥
         */
        private String accessKeySecret;

        private Sms sms;

    }

    @Data
    public static class Sms {
        /**
         * 短信签名
         */
        private String signature;

        private TemplateCode templateCode;
    }

    @Data
    public static class TemplateCode {
        /**
         * 验证码模板Code
         */
        private String verification;
        /**
         * 注册成功发送密码模板Code
         * 模板内容：您的手机已经成功注册纷析云财务软件，密码是${password},您可以登录使用啦！
         */
        private String register;
    }
}
