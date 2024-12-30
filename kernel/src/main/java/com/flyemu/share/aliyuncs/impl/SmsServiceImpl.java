package com.flyemu.share.aliyuncs.impl;

import cn.hutool.core.util.StrUtil;
import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyemu.share.aliyuncs.SmsService;
import com.flyemu.share.config.AppConfig;
import com.flyemu.share.exception.ServiceException;
import darabonba.core.client.ClientOverrideConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * <p>****************************************************************************</p>
 * <p><b>Copyright © 2010-2019纷析云（杭州）科技有限公司All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : cn.gson.accountantplatform.service.aliyuncs</li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2019年01月14日</li>
 * <li>@author     : ____′↘夏悸</li>
 * </ul>
 * <p>****************************************************************************</p>
 */
@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    private final ObjectMapper mapper;
    private AsyncClient acsClient;

    public SmsServiceImpl(AppConfig appConfig, ObjectMapper objectMapper) {
        this.mapper = objectMapper;
        StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                // Please ensure that the environment variables ALIBABA_CLOUD_ACCESS_KEY_ID and ALIBABA_CLOUD_ACCESS_KEY_SECRET are set.
                .accessKeyId(appConfig.getAliyun().getAccessKeyId())
                .accessKeySecret(appConfig.getAliyun().getAccessKeySecret())
                .build());
        this.acsClient = AsyncClient.builder()
                .credentialsProvider(provider)
                .overrideConfiguration(
                        ClientOverrideConfiguration.create()
                                .setEndpointOverride("dysmsapi.aliyuncs.com"))
                .build();
    }

    /**
     * 发送短信
     *
     * @param smsBody
     */
    @Override
    public void send(SmsBody smsBody) {
        try {
            // Parameter settings for API request
            SendSmsRequest.Builder builder = SendSmsRequest.builder()
                    .phoneNumbers(smsBody.getPhoneNumbers())
                    .signName(smsBody.getSignName())
                    .templateCode(smsBody.getTemplateCode());

            if (smsBody.getTemplateParam() != null && !smsBody.getTemplateParam().isEmpty()) {
                builder.templateParam(mapper.writeValueAsString(smsBody.getTemplateParam()));
            }

            //可选-上行短信扩展码(扩展码字段控制在7位或以下，无特殊需求用户请忽略此字段)
            if (StrUtil.isNotEmpty(smsBody.getSmsUpExtendCode())) {
                builder.smsUpExtendCode(smsBody.getSmsUpExtendCode());
            }

            //可选:outId为提供给业务方扩展字段,最终在短信回执消息中将此值带回给调用者
            if (StrUtil.isNotEmpty(smsBody.getOutId())) {
                builder.outId(smsBody.getOutId());
            }

            //请求失败这里会抛ClientException异常
            CompletableFuture<SendSmsResponse> response = acsClient.sendSms(builder.build());
            SendSmsResponse sendSmsResponse = response.get();
            if (sendSmsResponse.getBody().getCode() == null || !sendSmsResponse.getBody().getCode().equals("OK")) {
                throw new RuntimeException(sendSmsResponse.getBody().getMessage());
            }
        } catch (Exception e) {
            log.error("短信发送失败", e);
            throw new ServiceException(e.getMessage(), e);
        }
    }
}
