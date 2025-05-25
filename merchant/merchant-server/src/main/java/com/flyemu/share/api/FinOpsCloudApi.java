package com.flyemu.share.api;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyemu.share.dto.VoucherDto;
import com.flyemu.share.exception.ServiceException;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        HttpRequest post = HttpUtil.createPost(finOpsRequest.getBaseUrl() + "/api/login");
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
        JSONObject res = this.executeJson(0, finOpsRequest.getBaseUrl() + "/api/account-sets", null, finOpsRequest);
        log.info("帐套信息{}", res);
        return res.getJSONObject("data").getJSONArray("results");
    }

    /**
     * 加载凭证号
     *
     * @param finOpsRequest
     * @param accountSetsId
     * @param word
     * @param currentAccountDate
     * @return
     * @throws UnsupportedEncodingException
     */
    public Integer loadWordCode(FinOpsRequest finOpsRequest, String accountSetsId, String word, LocalDate currentAccountDate) throws UnsupportedEncodingException {
        JSONObject res = this.executeJson(0, finOpsRequest.getBaseUrl() + "/api/voucher/code" + "?word=" + URLEncoder.encode(word, "UTF-8") + "&currentAccountDate=" + currentAccountDate, accountSetsId, finOpsRequest);
        log.info("凭证号{}", res);
        return res.getInteger("data");
    }


    public JSONObject loadVoucherSelect(FinOpsRequest finOpsRequest, String accountSetsId) {
        JSONObject res = this.executeJson(0, finOpsRequest.getBaseUrl() + "/api/subject/voucher/select", accountSetsId, finOpsRequest);
        log.info("凭证号{}", res);
        return res;
    }

    private JSONObject executeJson(int retry, String url, String accountSetsId, FinOpsRequest finOpsRequest) {
        HttpRequest request = HttpUtil.createGet(url);
        request.header("cookie", finOpsRequest.getCookie());
        if (accountSetsId != null) {
            request.header("accountSetId", accountSetsId.toString());
        }
        HttpResponse response = request.execute();
        if (response.isOk()) {
            log.debug("workflowFormsSchemasProcessCodes：{}", response.bodyBytes());
            JSONObject jsonObject = JSON.parseObject(response.bodyBytes());
            if (jsonObject.getBooleanValue("success")) {
                return jsonObject;
            }
            throw new HttpException(jsonObject.getString("msg"));
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


    private JSONObject execute(HttpRequest post, int retry, String accountSetsId, FinOpsRequest finOpsRequest) {
        post.header("cookie", finOpsRequest.getCookie());
        if (accountSetsId != null) {
            post.header("accountSetId", accountSetsId.toString());
        }
        HttpResponse response = post.execute();
        if (response.isOk()) {
            JSONObject jsonObject = JSON.parseObject(response.body());
            if (jsonObject.getBooleanValue("success")) {
                return jsonObject;
            }
            throw new ServiceException(jsonObject.getString("msg"));
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
    public JSONArray loadVoucherWord(FinOpsRequest finOpsRequest, String accountSetsId) {
        JSONObject res = this.executeJson(0, finOpsRequest.getBaseUrl() + "/api/voucher-word", accountSetsId, finOpsRequest);
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
    public JSONArray loadSubject(FinOpsRequest finOpsRequest, String accountSetsId) {
        JSONObject res = this.executeJson(0, finOpsRequest.getBaseUrl() + "/api/subject/voucher/select", accountSetsId, finOpsRequest);
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
    public JSONObject loadAccountingCategory(FinOpsRequest finOpsRequest, String accountSetsId, List<Long> categoryIdSet) {
        HttpRequest post = HttpUtil.createPost(finOpsRequest.getBaseUrl() + "/api/accounting-category/byid");
        post.body(JSON.toJSONString(categoryIdSet), "application/json");
        return execute(post, 0, accountSetsId, finOpsRequest);
    }

    /**
     * 新建凭证
     *
     * @param finOpsRequest
     * @param accountSetsId
     * @param dto
     * @return
     * @throws JsonProcessingException
     */
    public JSONObject createVoucher(FinOpsRequest finOpsRequest, String accountSetsId, VoucherDto dto) throws JsonProcessingException {
        HttpRequest post = HttpUtil.createPost(finOpsRequest.getBaseUrl() + "/api/voucher");
        post.body(objectMapper.writeValueAsString(dto), "application/json");
        JSONObject execute = execute(post, 0, accountSetsId, finOpsRequest);
        if (execute.getBooleanValue("success")) {
            return execute.getJSONObject("data");
        }
        throw new ServiceException(execute.getString("msg"));
    }


    /**
     * 更新凭证
     *
     * @param finOpsRequest
     * @param accountSetsId
     * @param dto
     * @return
     * @throws JsonProcessingException
     */
    public JSONObject upVoucher(FinOpsRequest finOpsRequest, String accountSetsId, VoucherDto dto) throws JsonProcessingException {
        HttpRequest post = HttpRequest.put(finOpsRequest.getBaseUrl() + "/api/voucher");
        post.body(objectMapper.writeValueAsString(dto), "application/json");
        return execute(post, 0, accountSetsId, finOpsRequest).getJSONObject("data");
    }

    public Object loadVoucherSummary(FinOpsRequest finOpsRequest, String accountSetsId) {
        JSONObject res = this.executeJson(0, finOpsRequest.getBaseUrl() + "/api/voucher/summary", accountSetsId, finOpsRequest);
        log.info("loadVoucherSummary{}", res);
        return res;
    }

    public Double balance(FinOpsRequest finOpsRequest, String accountSetsId, String subjectId, String categoryId, String categoryDetailsId) {
        HttpRequest post = HttpUtil.createPost(finOpsRequest.getBaseUrl() + "/api/subject/balance");
        post.body(JSON.toJSONString(new HashMap<>() {{
            if (StrUtil.isNotBlank(subjectId)) {
                put("subjectId", subjectId);
            }
            if (StrUtil.isNotBlank(categoryId)) {
                put("categoryId", categoryId);
            }
            if (StrUtil.isNotBlank(categoryDetailsId)) {
                put("categoryDetailsId", categoryDetailsId);
            }
        }}), "application/json");
        JSONObject execute = execute(post, 0, accountSetsId, finOpsRequest);
        log.info("balance{}", execute);
        return execute.getDouble("data");
    }

    public Object loadAuxiliaryAccountingData(FinOpsRequest finOpsRequest, String accountSetsId, List<Map<String, Object>> categories) {
        HttpRequest post = HttpUtil.createPost(finOpsRequest.getBaseUrl() + "/api/accounting-category-details/loadAuxiliaryAccountingData");
        post.body(JSON.toJSONString(categories), "application/json");
        JSONObject execute = execute(post, 0, accountSetsId, finOpsRequest);
        log.info("loadAuxiliaryAccountingData{}", execute);
        return execute.getDouble("data");
    }

    public Object loadVoucher(FinOpsRequest finOpsRequest, String accountSetsId, String voucherId) {
        JSONObject res = this.executeJson(0, finOpsRequest.getBaseUrl() + "/api/voucher/" + voucherId, accountSetsId, finOpsRequest);
        log.info("凭证{}", res);
        return res;
    }
}
