package com.flyemu.share.controller.invoice;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.service.invoice.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/invoice/auth")
@RequiredArgsConstructor
public class InvoiceAuthController {

    private final AuthService authService;

    // ========== 状态查询 ==========

    @GetMapping("/state")
    public JsonResult state(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(authService.currentState(accountDto.getMerchantId()));
    }

    // ========== 1. 订购产品 ==========

    @PostMapping("/purchase")
    public JsonResult purchase(@RequestParam String aggOrgName,
                               @RequestParam(defaultValue = "33") String dq,
                               @RequestParam String nsrsbh,
                               @RequestParam(defaultValue = "0004") String productCode,
                               @SaAccountVal AccountDto accountDto) {
        String aggOrgId = authService.purchaseProduct(accountDto.getMerchantId(), aggOrgName, dq, nsrsbh, productCode);
        return JsonResult.successful(Map.of("aggOrgId", aggOrgId, "message", "订购成功"));
    }

    // ========== 2. 账号管理 ==========

    @PostMapping("/account")
    public JsonResult createAccount(@SaAccountVal AccountDto accountDto) {
        String accountId = authService.queryOrCreateAccount(accountDto.getMerchantId());
        return JsonResult.successful(Map.of("accountId", accountId, "message", "账号就绪"));
    }

    // ========== 3. 登录 ==========

    @PostMapping("/login")
    public JsonResult login(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(authService.doLogin(accountDto.getMerchantId()));
    }

    @PostMapping("/sms")
    public JsonResult submitSms(@RequestParam String taskId, @RequestParam String code, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(authService.submitSms(accountDto.getMerchantId(), taskId, code));
    }

    // ========== 4. 人脸识别 ==========

    @GetMapping("/qr")
    public JsonResult getQr(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(authService.getQrCode(accountDto.getMerchantId()));
    }

    @GetMapping(value = "/qr-image", produces = "image/png")
    public ResponseEntity<byte[]> qrImage(@SaAccountVal AccountDto accountDto) {
        byte[] png = authService.getQrImage(accountDto.getMerchantId());
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .body(png);
    }

    @GetMapping("/qr/result")
    public JsonResult checkQr(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(Map.of("success", authService.checkQrResult(accountDto.getMerchantId())));
    }

    // ========== 5. 扫脸时长 ==========

    @GetMapping("/scan-duration")
    public JsonResult queryScanDuration(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(authService.queryScanDuration(accountDto.getMerchantId()));
    }

    @PostMapping("/scan-duration")
    public JsonResult setScanDuration(@RequestParam int seconds, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(authService.setScanDuration(accountDto.getMerchantId(), seconds));
    }
}
