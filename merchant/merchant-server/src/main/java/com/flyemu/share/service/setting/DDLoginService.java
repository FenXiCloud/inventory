package com.flyemu.share.service.setting;

public interface DDLoginService {

    String getLoginAuth(String authCode, String corpId);

    String addUserByDingDing();

    String getToken();
}
