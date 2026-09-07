package com.flyemu.share.service.setting;

import cn.hutool.core.lang.Dict;

public interface DDLoginService {

    String getLoginAuth(String authCode, String corpId);

    /**
     * 提交钉钉用户同步任务（后台异步执行，立即返回；进度与结果用 getSyncProgress 轮询）
     */
    String submitUserSync();

    /**
     * 当前同步任务状态：{running 是否进行中, progress 进度描述, result 最近一次结果}
     */
    Dict getSyncProgress();

    String getToken();
}
