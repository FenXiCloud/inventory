package com.flyemu.share.api;

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
import com.flyemu.share.entity.setting.FinanceRel;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.service.setting.FinanceRelService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.Set;


@Service
@Slf4j
@RequiredArgsConstructor
public final class Fenxi {
    @Resource
    private ObjectMapper objectMapper;

    private final FinanceRelService financeRelService;


    /**
     * 加载帐套信息
     *
     * @param financeRel
     * @param accountSetsId
     * @return
     */
    public JSONArray loadAccountSets(FinanceRel financeRel, Long accountSetsId) {
        JSONObject res = this.executeJson(0, financeRel.getUrl() + "/account-sets", accountSetsId, financeRel);
        log.info("帐套信息", res);
        return res.getJSONArray("data");
    }

    /**
     * 加载科目
     *
     * @param financeRel
     * @param accountSetsId
     * @return
     */
    public JSONArray loadSubject(FinanceRel financeRel, Long accountSetsId) {
        JSONObject res = this.executeJson(0, financeRel.getUrl() + "/subject/voucher/select", accountSetsId, financeRel);
        log.info("科目", res);
        return res.getJSONArray("data");
    }

    /**
     * 加载账号分类
     *
     * @param financeRel
     * @param accountSetsId
     * @param categoryIdSet
     * @return
     */
    public JSONObject loadAccountingCategory(FinanceRel financeRel, Long accountSetsId, Set<Long> categoryIdSet) {
        HttpRequest post = HttpUtil.createPost(financeRel.getUrl() + "/accounting-category/byid");
        post.body(JSON.toJSONString(categoryIdSet), "application/json");
        return execute(post, 0, accountSetsId, financeRel);
    }

    /**
     * 加载凭证
     *
     * @param financeRel
     * @param accountSetsId
     * @param voucherId
     * @return
     */
    public JSONObject loadVoucher(FinanceRel financeRel, Long accountSetsId, Long voucherId) {
        JSONObject res = this.executeJson(0, financeRel.getUrl() + "/voucher" + "/" + voucherId, accountSetsId, financeRel);
        log.info("凭证详情", res);
        return res.getJSONObject("data");
    }

    /**
     * 加载凭证字
     *
     * @param financeRel
     * @param accountSetsId
     * @return
     */
    public JSONArray loadVoucherWord(FinanceRel financeRel, Long accountSetsId) {
        JSONObject res = this.executeJson(0, financeRel.getUrl() + "/voucher-word", accountSetsId, financeRel);
        log.info("加载凭证字", res);
        return res.getJSONArray("data");
    }

    /**
     * 加载凭证号
     *
     * @param financeRel
     * @param accountSetsId
     * @param word
     * @param currentAccountDate
     * @return
     * @throws UnsupportedEncodingException
     */
    public Integer loadWordCode(FinanceRel financeRel, Long accountSetsId, String word, LocalDate currentAccountDate) throws UnsupportedEncodingException {
        JSONObject res = this.executeJson(0, financeRel.getUrl() + "/voucher/code" + "?word=" + URLEncoder.encode(word, "UTF-8") + "&currentAccountDate=" + currentAccountDate, accountSetsId, financeRel);
        log.info("凭证号", res);
        return res.getInteger("data");
    }


    /**
     * 新建凭证
     *
     * @param financeRel
     * @param accountSetsId
     * @param dto
     * @return
     * @throws JsonProcessingException
     */
    public JSONObject createVoucher(FinanceRel financeRel, Long accountSetsId, VoucherDto dto) throws JsonProcessingException {
        HttpRequest post = HttpUtil.createPost(financeRel.getUrl() + "/voucher");
        post.body(objectMapper.writeValueAsString(dto), "application/json");
        return execute(post, 0, accountSetsId, financeRel).getJSONObject("data");
    }


    /**
     * 更新凭证
     *
     * @param financeRel
     * @param accountSetsId
     * @param dto
     * @return
     * @throws JsonProcessingException
     */
    public JSONObject upVoucher(FinanceRel financeRel, Long accountSetsId, VoucherDto dto) throws JsonProcessingException {
        HttpRequest post = HttpRequest.put(financeRel.getUrl() + "/voucher");
        post.body(objectMapper.writeValueAsString(dto), "application/json");
        return execute(post, 0, accountSetsId, financeRel).getJSONObject("data");
    }


    private JSONObject executeJson(int retry, String url, Long accountSetsId, FinanceRel financeRel) {
        HttpRequest request = HttpUtil.createGet(url);
        request.header("cookie", financeRel.getCookie());
        if (accountSetsId != null) {
            request.header("accountSetId", accountSetsId.toString());
        }
        HttpResponse response = request.execute();
        if (response.isOk()) {
            log.debug("workflowFormsSchemasProcessCodes：{}", response.bodyBytes());
            return JSON.parseObject(response.bodyBytes());
        } else if (401 == response.getStatus() && retry < 3) {
            financeRel = financeRelService.upCookie(financeRel.getMerchantId(), financeRel.getAccountBookId());
            return executeJson(retry + 1, url, accountSetsId, financeRel);
        } else {
            throw new ServiceException("workflowFormsSchemasProcessCodes HttpCode：" + response.getStatus());
        }
    }

    private JSONObject execute(HttpRequest post, int retry, Long accountSetsId, FinanceRel financeRel) {
        post.header("cookie", financeRel.getCookie());
        post.header("accountSetId", accountSetsId.toString());
        HttpResponse response = post.execute();
        if (response.isOk()) {
            JSONObject jsonObject = JSON.parseObject(response.body());
            if (0 == jsonObject.getIntValue("error_code")) {
                return jsonObject;
            } else if (401 == response.getStatus() && retry < 3) {
                financeRel = financeRelService.upCookie(financeRel.getMerchantId(), financeRel.getAccountBookId());
                return execute(post, retry + 1, accountSetsId, financeRel);
            }
            throw new HttpException(jsonObject.getString("errmsg"));
        }
        throw new ServiceException("财务系统提示：" + JSON.parseObject(response.body()).getString("msg"));
    }


}
