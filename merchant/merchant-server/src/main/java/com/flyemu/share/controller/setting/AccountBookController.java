package com.flyemu.share.controller.setting;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.flyemu.share.common.Constants;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountBookDto;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.AccountBook;
import com.flyemu.share.service.setting.AccountBookService;
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
    public JsonResult list(Page page, AccountBookService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bindMerchant(accountDto, query::setMerchantId);
        return JsonResult.successful(accountBookService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid AccountBookDto accountBookDto, @SaAccountVal AccountDto accountDto) {
        accountBookDto.setCurrent(false);
        TenantScope.bindMerchant(accountDto, accountBookDto::setMerchantId);
        accountBookDto.setEnabled(true);
        accountBookService.save(accountBookDto);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountBookDto accountBookDto, @SaAccountVal AccountDto accountDto) {
        TenantScope.bindMerchant(accountDto, accountBookDto::setMerchantId);
        accountBookService.save(accountBookDto);
        return JsonResult.successful();
    }

    /**
     * 修改默认账套
     */
    @PutMapping("/change/current/{accountBookId}")
    public JsonResult changeCurrentAccountBook(@PathVariable Long accountBookId, @SaAccountVal AccountDto accountDto) {
        AccountBook accountBook = accountBookService.changeCurrentAccountBook(accountDto.getMerchantId(), accountBookId);
        accountDto.setAccountBook(accountBook);
        SaSession session = StpUtil.getTokenSession();
        session.set(Constants.SESSION_ACCOUNT, accountDto);
        return JsonResult.successful();
    }

    @DeleteMapping("/{accountBookId}")
    public JsonResult delete(@PathVariable Long accountBookId, @SaAccountVal AccountDto accountDto) {
        accountBookService.delete(accountDto.getMerchantId(), accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(accountBookService.select(accountDto.getMerchantId()));
    }
}
