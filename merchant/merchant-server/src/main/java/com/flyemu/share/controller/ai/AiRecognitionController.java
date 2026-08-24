package com.flyemu.share.controller.ai;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.service.ai.AiRecognitionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiRecognitionController {

    private final AiRecognitionService aiRecognitionService;

    @PostMapping("/recognize-order")
    public JsonResult recognize(@RequestBody RecognizeRequest request, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(
                aiRecognitionService.recognize(request.getText(), accountDto.getMerchantId(), accountDto.getAccountBookId())
        );
    }

    @Data
    public static class RecognizeRequest {
        private String text;
    }
}
