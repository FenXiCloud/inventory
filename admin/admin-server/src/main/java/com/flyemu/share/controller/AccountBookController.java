package com.flyemu.share.controller;

import cn.hutool.core.lang.Assert;
import com.flyemu.share.entity.AccountBook;
import com.flyemu.share.service.AccountBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/accountBook")
@RequiredArgsConstructor
public class AccountBookController {

    private final AccountBookService accountBookService;

    @GetMapping
    public JsonResult list(Page page, AccountBookService.Query query) {
        return JsonResult.successful(accountBookService.query(page, query));
    }

    @GetMapping("all")
    public JsonResult listAll(Long merchantId) {
        return JsonResult.successful(accountBookService.listAll(merchantId));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid AccountBook accountBook) {
        accountBookService.save(accountBook);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountBook accountBook) {
        accountBookService.save(accountBook);
        return JsonResult.successful();
    }

    @DeleteMapping("/{accountBookId}")
    public JsonResult delete(@PathVariable Long accountBookId) {
        accountBookService.delete(accountBookId);
        return JsonResult.successful();
    }
}
