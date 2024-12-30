package com.flyemu.share.aliyuncs;

import lombok.Data;
import lombok.NonNull;

import java.util.Map;

/**
 * <p>****************************************************************************</p >
 * <p><b>Copyright © 2010-2024 纷析云（杭州）技有限公司 All Rights Reserved<b></p >
 * <ul style="margin:15px;">
 * <li>Description : com.flyemu.share.annotation</li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2024年02月18日</li>
 * <li>@author     : ____xzy</li>
 * </ul>
 * <p>****************************************************************************</p >
 */
public interface SmsService {

    String PRODUCT = "Dysmsapi";

    String DOMAIN = "dysmsapi.aliyuncs.com";

    /**
     * 发送短信
     *
     * @param smsBody
     */
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
