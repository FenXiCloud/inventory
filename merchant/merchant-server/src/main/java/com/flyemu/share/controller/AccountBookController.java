package com.flyemu.share.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.common.Constants;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.AccountBookDto;
import com.flyemu.share.entity.setting.AccountBook;
import com.flyemu.share.service.AccountBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@Slf4j
@RequestMapping("/accountBook")
@RequiredArgsConstructor
public class AccountBookController {

    private final AccountBookService accountBookService;


    @GetMapping
    public JsonResult list(Page page, @SaMerchantId Long merchantId, AccountBookService.Query query) {
        query.setMerchantId(merchantId);
        return JsonResult.successful(accountBookService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid AccountBookDto accountBookDto, @SaMerchantId Long merchantId) {
        accountBookDto.setCurrent(false);
        accountBookDto.setMerchantId(merchantId);
        accountBookDto.setEnabled(true);
        accountBookDto.setStartDate(accountBookDto.getStartDate());
        accountBookService.save(accountBookDto);
        return JsonResult.successful();
    }


    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountBookDto accountBookDto) {
        accountBookService.save(accountBookDto);
        return JsonResult.successful();
    }

    /**
     * 修改默认账套
     * @param merchantId
     * @param accountBookId
     * @return
     */
    @PutMapping("change/current/{accountBookId}")
    public  JsonResult changeCurrentAccountBook(@SaMerchantId Long merchantId,@PathVariable Long accountBookId){
        AccountBook accountBook = accountBookService.changeCurrentAccountBook(merchantId,accountBookId);
        AccountDto accountDto = (AccountDto) StpUtil.getTokenSession().get(Constants.SESSION_ACCOUNT);
        accountDto.setAccountBook(accountBook);
        SaSession session = StpUtil.getTokenSession();
        session.set(Constants.SESSION_ACCOUNT, accountDto);
        return JsonResult.successful();
    }

    @DeleteMapping("/{accountBookId}")
    public JsonResult delete(@PathVariable Long accountBookId, Long merchantId) {
        accountBookService.delete(merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId) {
        return JsonResult.successful(accountBookService.select(merchantId));
    }
}
