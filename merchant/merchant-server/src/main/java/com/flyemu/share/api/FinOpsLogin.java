package com.flyemu.share.api;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.flyemu.share.entity.setting.FinanceAccountLink;
import com.flyemu.share.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public final class FinOpsLogin {

    private final String API_SITE = "https://v4.fenxi365.com/api";

    public String getCookie(FinanceAccountLink financeAccountLink) {
        HttpRequest post = HttpUtil.createPost(financeAccountLink.getUrl() + "/login");
        Map<String, Object> map = new HashMap<>();
        map.put("password", financeAccountLink.getFinancePassword());
        map.put("mobile", financeAccountLink.getFinanceAccount());
        return toLogin(post, map);
    }


    private String toLogin(HttpRequest post, Map<String, Object> map) {
        post.form(map);
        HttpResponse response = post.execute();
        if (response.isOk()) {
            JSONObject jsonObject = JSON.parseObject(response.body());
            if (0 == jsonObject.getIntValue("error_code")) {
                return response.header("Set-Cookie");
            }
            throw new HttpException(jsonObject.getString("errmsg"));
        }
        throw new ServiceException("财务系统提示：" + JSON.parseObject(response.body()).getString("msg"));
    }
}
