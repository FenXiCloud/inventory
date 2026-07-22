package com.flyemu.share.scheduling;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 价格协议定时任务 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgreementScheduling {

    @Scheduled(cron = "0 1 0 * * ?")
    public void scheduledMethod() {
    }
}
