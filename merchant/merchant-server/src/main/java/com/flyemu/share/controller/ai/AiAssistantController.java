package com.flyemu.share.controller.ai;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.service.ai.AiAssistantService;
import com.flyemu.share.service.ai.AiAssistantService.CustomerInfo;
import com.flyemu.share.service.ai.AiAssistantService.ProductInfo;
import com.flyemu.share.service.ai.AiAssistantService.WarehouseInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/ai/assistant")
@RequiredArgsConstructor
@Slf4j
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 发送消息给AI助手（普通版本）
     */
    @PostMapping("/chat")
    public JsonResult chat(@RequestBody ChatRequest request, @SaAccountVal AccountDto accountDto) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        AiAssistantService.AiResponse response = aiAssistantService.processMessage(
                sessionId,
                request.getMessage(),
                accountDto.getMerchantId(),
                accountDto.getAccountBookId()
        );

        return JsonResult.successful(new ChatResponse(sessionId, response));
    }

    /**
     * 发送消息给AI助手（流式版本）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request, @SaAccountVal AccountDto accountDto) {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        String finalSessionId = sessionId;

        executorService.execute(() -> {
            try {
                // 发送sessionId
                emitter.send(SseEmitter.event()
                        .data("{\"type\":\"sessionId\",\"sessionId\":\"" + finalSessionId + "\"}")
                        .id("session"));

                // 处理消息
                aiAssistantService.processMessageStream(
                        finalSessionId,
                        request.getMessage(),
                        accountDto.getMerchantId(),
                        accountDto.getAccountBookId(),
                        new AiAssistantService.StreamCallback() {
                            @Override
                            public void onChunk(String chunk) {
                                try {
                                    String escaped = chunk.replace("\\", "\\\\")
                                            .replace("\"", "\\\"")
                                            .replace("\n", "\\n")
                                            .replace("\r", "\\r");
                                    emitter.send(SseEmitter.event()
                                            .data("{\"type\":\"chunk\",\"content\":\"" + escaped + "\"}")
                                            .id("chunk"));
                                } catch (IOException e) {
                                    log.error("发送chunk失败", e);
                                }
                            }

                            @Override
                            public void onComplete(String fullResponse) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .data("{\"type\":\"done\"}")
                                            .id("done"));
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.error("发送done失败", e);
                                }
                            }

                            @Override
                            public void onError(String error) {
                                try {
                                    String escaped = error.replace("\\", "\\\\")
                                            .replace("\"", "\\\"")
                                            .replace("\n", "\\n")
                                            .replace("\r", "\\r");
                                    emitter.send(SseEmitter.event()
                                            .data("{\"type\":\"error\",\"message\":\"" + escaped + "\"}")
                                            .id("error"));
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.error("发送error失败", e);
                                }
                            }
                        }
                );
            } catch (Exception e) {
                log.error("流式处理失败", e);
                try {
                    emitter.send(SseEmitter.event()
                            .data("{\"type\":\"error\",\"message\":\"处理失败：" + e.getMessage() + "\"}")
                            .id("error"));
                    emitter.complete();
                } catch (IOException ex) {
                    log.error("发送error失败", ex);
                }
            }
        });

        return emitter;
    }

    /**
     * 查询客户
     */
    @GetMapping("/customers")
    public JsonResult searchCustomers(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer limit,
            @SaAccountVal AccountDto accountDto) {
        List<CustomerInfo> customers = aiAssistantService.searchCustomers(
                keyword,
                accountDto.getMerchantId(),
                accountDto.getAccountBookId(),
                limit
        );
        return JsonResult.successful(customers);
    }

    /**
     * 查询商品
     */
    @GetMapping("/products")
    public JsonResult searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer limit,
            @SaAccountVal AccountDto accountDto) {
        List<ProductInfo> products = aiAssistantService.searchProducts(
                keyword,
                accountDto.getMerchantId(),
                accountDto.getAccountBookId(),
                limit
        );
        return JsonResult.successful(products);
    }

    /**
     * 获取仓库列表
     */
    @GetMapping("/warehouses")
    public JsonResult getWarehouses(@SaAccountVal AccountDto accountDto) {
        List<WarehouseInfo> warehouses = aiAssistantService.getWarehouses(
                accountDto.getMerchantId(),
                accountDto.getAccountBookId()
        );
        return JsonResult.successful(warehouses);
    }

    /**
     * 清除会话上下文
     */
    @PostMapping("/clear-context")
    public JsonResult clearContext(@RequestBody ClearContextRequest request) {
        aiAssistantService.clearContext(request.getSessionId());
        return JsonResult.successful();
    }

    // ==================== 请求/响应类 ====================

    @Data
    public static class ChatRequest {
        private String sessionId;
        private String message;
    }

    @Data
    public static class ChatResponse {
        private String sessionId;
        private AiAssistantService.AiResponse aiResponse;

        public ChatResponse(String sessionId, AiAssistantService.AiResponse aiResponse) {
            this.sessionId = sessionId;
            this.aiResponse = aiResponse;
        }
    }

    @Data
    public static class ClearContextRequest {
        private String sessionId;
    }
}
