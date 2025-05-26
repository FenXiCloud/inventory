package com.flyemu.share.service.setting;

/**
 * @author: wangwenjia
 * @since: 2025/5/20 15:11
 * @description:
 */
public interface DDLoginService {

    String getLoginAuth(String authCode, String corpId);

    String addUserByDingDing();

    String getToken();
}
