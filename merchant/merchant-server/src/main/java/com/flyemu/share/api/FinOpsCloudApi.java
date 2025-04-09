package com.flyemu.share.api;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyemu.share.exception.ServiceException;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinOpsCloudApi {

    @Resource
    private ObjectMapper objectMapper;

    public String getCookie(FinOpsRequest finOpsRequest) {
        JSONObject jsonObject = this.toLogin(finOpsRequest);
        return jsonObject.getString("cookie");
    }

    public JSONObject getLoginResult(FinOpsRequest finOpsRequest) {
        return this.toLogin(finOpsRequest);
    }

    private JSONObject toLogin(FinOpsRequest finOpsRequest) {
        HttpRequest post = HttpUtil.createPost(finOpsRequest.getBaseUrl() + "/login");
        Map<String, Object> map = new HashMap<>();
        map.put("password", finOpsRequest.getPassword());
        map.put("mobile", finOpsRequest.getAccount());
        post.form(map);
        HttpResponse response = post.execute();
        if (response.isOk()) {
            JSONObject jsonObject = JSON.parseObject(response.body());
            if (0 == jsonObject.getIntValue("error_code")) {
                String cookie = response.header("Set-Cookie");
                jsonObject.put("cookie", cookie);
                return jsonObject;
            }
            throw new HttpException(jsonObject.getString("errmsg"));
        }
        throw new ServiceException("财务系统提示：" + JSON.parseObject(response.body()).getString("msg"));
    }

    /**
     * 加载帐套信息
     *
     * @param finOpsRequest
     * @return
     */
    public JSONArray loadAccountSets(FinOpsRequest finOpsRequest) {
        JSONObject res = this.executeJson(0, finOpsRequest.getBaseUrl() + "/account-sets", null, finOpsRequest);
        log.info("帐套信息{}", res);
        return res.getJSONArray("data");
    }

    private JSONObject executeJson(int retry, String url, Long accountSetsId, FinOpsRequest finOpsRequest) {
        HttpRequest request = HttpUtil.createGet(url);
        request.header("cookie", finOpsRequest.getCookie());
        if (accountSetsId != null) {
            request.header("accountSetId", accountSetsId.toString());
        }
        HttpResponse response = request.execute();
        if (response.isOk()) {
            log.debug("workflowFormsSchemasProcessCodes：{}", response.bodyBytes());
            return JSON.parseObject(response.bodyBytes());
        } else if (401 == response.getStatus() && retry < 3) {
            String cookie = getCookie(finOpsRequest);
            if (StringUtils.isNotBlank(cookie)) {
                FinOpsCallback callback = finOpsRequest.getCallback();
                if (callback != null) {
                    callback.failCallback(cookie);
                }
                finOpsRequest.setCookie(cookie);
            }
            return executeJson(retry + 1, url, accountSetsId, finOpsRequest);
        } else {
            throw new ServiceException("workflowFormsSchemasProcessCodes HttpCode：" + response.getStatus());
        }
    }


    private JSONObject execute(HttpRequest post, int retry, Long accountSetsId, FinOpsRequest finOpsRequest) {
        post.header("cookie", finOpsRequest.getCookie());
        if (accountSetsId != null) {
            post.header("accountSetId", accountSetsId.toString());
        }
        HttpResponse response = post.execute();
        if (response.isOk()) {
            JSONObject jsonObject = JSON.parseObject(response.body());
            if (0 == jsonObject.getIntValue("error_code")) {
                return jsonObject;
            }
            throw new HttpException(jsonObject.getString("errmsg"));
        } else {
            if (401 == response.getStatus() && retry < 3) {
                String cookie = getCookie(finOpsRequest);
                if (StringUtils.isNotBlank(cookie)) {
                    FinOpsCallback callback = finOpsRequest.getCallback();
                    if (callback != null) {
                        callback.failCallback(cookie);
                    }
                    finOpsRequest.setCookie(cookie);
                }
                return execute(post, retry + 1, accountSetsId, finOpsRequest);
            }
        }
        throw new ServiceException("财务系统提示：" + JSON.parseObject(response.body()).getString("msg"));
    }

    /**
     * 加载凭证字
     *
     * @param finOpsRequest
     * @param accountSetsId
     * @return
     */
    public JSONArray loadVoucherWord(FinOpsRequest finOpsRequest, Long accountSetsId) {
        JSONObject res = this.executeJson(0, finOpsRequest.getBaseUrl() + "/voucher-word", accountSetsId, finOpsRequest);
        log.info("加载凭证字{}", res);
        return res.getJSONArray("data");
    }

    /**
     * 加载科目
     *
     * @param finOpsRequest
     * @param accountSetsId
     * @return
     */
    public JSONArray loadSubject(FinOpsRequest finOpsRequest, Long accountSetsId) {
        JSONObject res = this.executeJson(0, finOpsRequest.getBaseUrl() + "/subject/voucher/select", accountSetsId, finOpsRequest);
        log.info("科目{}", res);
        return res.getJSONArray("data");
    }

    /**
     * 加载账号分类
     *
     * @param finOpsRequest
     * @param accountSetsId
     * @param categoryIdSet
     * @return
     */
    public JSONObject loadAccountingCategory(FinOpsRequest finOpsRequest, Long accountSetsId, List<Long> categoryIdSet) {
        HttpRequest post = HttpUtil.createPost(finOpsRequest.getBaseUrl() + "/accounting-category/byid");
        post.body(JSON.toJSONString(categoryIdSet), "application/json");
        return execute(post, 0, accountSetsId, finOpsRequest);
    }
}
