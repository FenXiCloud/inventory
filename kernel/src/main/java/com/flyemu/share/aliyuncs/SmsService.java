package com.flyemu.share.aliyuncs;

import lombok.Data;
import lombok.NonNull;

import java.util.Map;

/** 阿里云短信发送 */
public interface SmsService {

    String PRODUCT = "Dysmsapi";

    String DOMAIN = "dysmsapi.aliyuncs.com";

    /** 发送短信 */
    void send(SmsBody smsBody);

    @Data
    class SmsBody {
        @NonNull
        private String phoneNumbers;
        @NonNull
        private String signName;
        @NonNull
        private String templateCode;

        private Map<String, String> templateParam;
        private String outId;
        private String smsUpExtendCode;
    }
}
