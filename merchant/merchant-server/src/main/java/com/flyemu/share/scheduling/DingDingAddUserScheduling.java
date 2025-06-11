package com.flyemu.share.scheduling;

import com.flyemu.share.service.setting.DDLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时拉去钉钉用户信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingDingAddUserScheduling {

    @Autowired
    private DDLoginService loginService;

//    @Scheduled(cron = "0 0 23 * * ?")
    public void scheduledMethod() {
        loginService.addUserByDingDing();
    }

}
