package com.flyemu.share.scheduling;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>****************************************************************************</p>
 * <p><b>Copyright © 2010-2023 杭州财汇餐谋科技有限公司 All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : 价格协议定时任务</li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2023年09月18日</li>
 * <li>@author     : ____′↘ts</li>
 * </ul>
 * <p>****************************************************************************</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgreementScheduling {


    @Scheduled(cron = "0 1 0 * * ?")
    public void scheduledMethod() {
    }
}
