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
import com.flyemu.share.entity.setting.FinanceAccountLink;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.service.setting.FinanceAccountLinkService;
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
public class FinOps {

    @Resource
    private ObjectMapper objectMapper;

//    @Resource
//    private FinanceAccountLinkService financeAccountLinkService;


    /**
     * 加载帐套信息
     *
     * @param financeAccountLink
     * @param accountSetsId
     * @return
     */
    public JSONArray loadAccountSets(FinanceAccountLink financeAccountLink, Long accountSetsId) {
        JSONObject res = this.executeJson(0, financeAccountLink.getUrl() + "/account-sets", accountSetsId, financeAccountLink);
        log.info("帐套信息{}", res);
        return res.getJSONArray("data");
    }

    /**
     * 加载科目
     *
     * @param financeAccountLink
     * @param accountSetsId
     * @return
     */
    public JSONArray loadSubject(FinanceAccountLink financeAccountLink, Long accountSetsId) {
        JSONObject res = this.executeJson(0, financeAccountLink.getUrl() + "/subject/voucher/select", accountSetsId, financeAccountLink);
        log.info("科目{}", res);
        return res.getJSONArray("data");
    }

    /**
     * 加载账号分类
     *
     * @param financeAccountLink
     * @param accountSetsId
     * @param categoryIdSet
     * @return
     */
    public JSONObject loadAccountingCategory(FinanceAccountLink financeAccountLink, Long accountSetsId, Set<Long> categoryIdSet) {
        HttpRequest post = HttpUtil.createPost(financeAccountLink.getUrl() + "/accounting-category/byid");
        post.body(JSON.toJSONString(categoryIdSet), "application/json");
        return execute(post, 0, accountSetsId, financeAccountLink);
    }

    /**
     * 加载凭证
     *
     * @param financeAccountLink
     * @param accountSetsId
     * @param voucherId
     * @return
     */
    public JSONObject loadVoucher(FinanceAccountLink financeAccountLink, Long accountSetsId, Long voucherId) {
        JSONObject res = this.executeJson(0, financeAccountLink.getUrl() + "/voucher" + "/" + voucherId, accountSetsId, financeAccountLink);
        log.info("凭证详情{}", res);
        return res.getJSONObject("data");
    }

    /**
     * 加载凭证字
     *
     * @param financeAccountLink
     * @param accountSetsId
     * @return
     */
    public JSONArray loadVoucherWord(FinanceAccountLink financeAccountLink, Long accountSetsId) {
        JSONObject res = this.executeJson(0, financeAccountLink.getUrl() + "/voucher-word", accountSetsId, financeAccountLink);
        log.info("加载凭证字{}", res);
        return res.getJSONArray("data");
    }

    /**
     * 加载凭证号
     *
     * @param financeAccountLink
     * @param accountSetsId
     * @param word
     * @param currentAccountDate
     * @return
     * @throws UnsupportedEncodingException
     */
    public Integer loadWordCode(FinanceAccountLink financeAccountLink, Long accountSetsId, String word, LocalDate currentAccountDate) throws UnsupportedEncodingException {
        JSONObject res = this.executeJson(0, financeAccountLink.getUrl() + "/voucher/code" + "?word=" + URLEncoder.encode(word, "UTF-8") + "&currentAccountDate=" + currentAccountDate, accountSetsId, financeAccountLink);
        log.info("凭证号{}", res);
        return res.getInteger("data");
    }


    /**
     * 新建凭证
     *
     * @param financeAccountLink
     * @param accountSetsId
     * @param dto
     * @return
     * @throws JsonProcessingException
     */
    public JSONObject createVoucher(FinanceAccountLink financeAccountLink, Long accountSetsId, VoucherDto dto) throws JsonProcessingException {
        HttpRequest post = HttpUtil.createPost(financeAccountLink.getUrl() + "/voucher");
        post.body(objectMapper.writeValueAsString(dto), "application/json");
        return execute(post, 0, accountSetsId, financeAccountLink).getJSONObject("data");
    }


    /**
     * 更新凭证
     *
     * @param financeAccountLink
     * @param accountSetsId
     * @param dto
     * @return
     * @throws JsonProcessingException
     */
    public JSONObject upVoucher(FinanceAccountLink financeAccountLink, Long accountSetsId, VoucherDto dto) throws JsonProcessingException {
        HttpRequest post = HttpRequest.put(financeAccountLink.getUrl() + "/voucher");
        post.body(objectMapper.writeValueAsString(dto), "application/json");
        return execute(post, 0, accountSetsId, financeAccountLink).getJSONObject("data");
    }


    private JSONObject executeJson(int retry, String url, Long accountSetsId, FinanceAccountLink financeAccountLink) {
        HttpRequest request = HttpUtil.createGet(url);
        request.header("cookie", financeAccountLink.getFinanceCookie());
        if (accountSetsId != null) {
            request.header("accountSetId", accountSetsId.toString());
        }
        HttpResponse response = request.execute();
        if (response.isOk()) {
            log.debug("workflowFormsSchemasProcessCodes：{}", response.bodyBytes());
            return JSON.parseObject(response.bodyBytes());
        } else if (401 == response.getStatus() && retry < 3) {
//            financeAccountLink =
//                    financeAccountLinkService.upCookie(financeAccountLink.getMerchantId(), financeAccountLink.getAccountBookId());
            return executeJson(retry + 1, url, accountSetsId, financeAccountLink);
        } else {
            throw new ServiceException("workflowFormsSchemasProcessCodes HttpCode：" + response.getStatus());
        }
    }

    private JSONObject execute(HttpRequest post, int retry, Long accountSetsId, FinanceAccountLink financeAccountLink) {
        post.header("cookie", financeAccountLink.getFinanceCookie());
        post.header("accountSetId", accountSetsId.toString());
        HttpResponse response = post.execute();
        if (response.isOk()) {
            JSONObject jsonObject = JSON.parseObject(response.body());
            if (0 == jsonObject.getIntValue("error_code")) {
                return jsonObject;
            } else if (401 == response.getStatus() && retry < 3) {
//                financeAccountLink = financeAccountLinkService.upCookie(financeAccountLink.getMerchantId(), financeAccountLink.getAccountBookId());
                return execute(post, retry + 1, accountSetsId, financeAccountLink);
            }
            throw new HttpException(jsonObject.getString("errmsg"));
        }
        throw new ServiceException("财务系统提示：" + JSON.parseObject(response.body()).getString("msg"));
    }


}
