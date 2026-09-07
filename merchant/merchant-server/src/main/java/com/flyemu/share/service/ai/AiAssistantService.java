package com.flyemu.share.service.ai;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.flyemu.share.config.AiRecognitionProperties;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.Tuple;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiAssistantService extends BaseService {

    private static final QCustomer qCustomer = QCustomer.customer;
    private static final QProduct qProduct = QProduct.product;
    private static final QWarehouse qWarehouse = QWarehouse.warehouse;

    private final AiRecognitionProperties properties;

    // 会话上下文存储（生产环境应使用Redis）
    private final Map<String, ConversationContext> contextMap = new ConcurrentHashMap<>();

    /**
     * 处理用户消息（非流式）
     */
    public AiResponse processMessage(String sessionId, String message, Long merchantId, Long accountBookId) {
        ConversationContext context = contextMap.computeIfAbsent(sessionId, k -> new ConversationContext());
        try {
            String systemPrompt = buildSystemPrompt(merchantId, accountBookId);
            AiResponse response = callAiModel(systemPrompt, context.getMessages(), message);
            context.addMessage("user", message);
            context.addMessage("assistant", JSON.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.error("AI处理失败", e);
            return AiResponse.error("抱歉，处理您的请求时出现错误：" + e.getMessage());
        }
    }

    /**
     * 处理用户消息（流式版本）
     */
    public void processMessageStream(String sessionId, String message, Long merchantId, Long accountBookId,
                                     StreamCallback callback) {
        // 获取或创建会话上下文
        ConversationContext context = contextMap.computeIfAbsent(sessionId, k -> new ConversationContext());

        try {
            // 构建完整的系统提示词
            String systemPrompt = buildSystemPrompt(merchantId, accountBookId);

            // 调用AI模型（流式）
            callAiModelStream(systemPrompt, context.getMessages(), message, new StreamCallback() {
                @Override
                public void onChunk(String chunk) {
                    callback.onChunk(chunk);
                }

                @Override
                public void onComplete(String fullResponse) {
                    // 更新上下文
                    context.addMessage("user", message);
                    context.addMessage("assistant", fullResponse);
                    callback.onComplete(fullResponse);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
        } catch (Exception e) {
            log.error("AI处理失败", e);
            callback.onError("抱歉，处理您的请求时出现错误：" + e.getMessage());
        }
    }

    /**
     * 流式回调接口
     */
    public interface StreamCallback {
        void onChunk(String chunk);
        void onComplete(String fullResponse);
        void onError(String error);
    }

    /**
     * 查询客户
     */
    public List<CustomerInfo> searchCustomers(String keyword, Long merchantId, Long accountBookId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        List<Tuple> fetch = bqf.selectFrom(qCustomer)
                .select(qCustomer.id, qCustomer.code, qCustomer.name, qCustomer.phone, qCustomer.customerLevelId)
                .where(qCustomer.merchantId.eq(merchantId)
                        .and(qCustomer.accountBookId.eq(accountBookId))
                        .and(qCustomer.enabled.isTrue())
                        .and(qCustomer.name.contains(keyword)
                                .or(qCustomer.code.contains(keyword))
                                .or(qCustomer.phone.contains(keyword))))
                .limit(limit)
                .fetch();

        List<CustomerInfo> list = new ArrayList<>();
        for (Tuple tuple : fetch) {
            CustomerInfo info = new CustomerInfo();
            info.setId(tuple.get(qCustomer.id));
            info.setCode(tuple.get(qCustomer.code));
            info.setName(tuple.get(qCustomer.name));
            info.setPhone(tuple.get(qCustomer.phone));
            info.setLevelId(tuple.get(qCustomer.customerLevelId));
            list.add(info);
        }
        return list;
    }


    /**
     * 查询商品
     */
    public List<ProductInfo> searchProducts(String keyword, Long merchantId, Long accountBookId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        List<Tuple> fetch = bqf.selectFrom(qProduct)
                .select(qProduct.id, qProduct.code, qProduct.name, qProduct.specification,
                        qProduct.unitId, qProduct.retailCustomerPrice, qProduct.pinyin)
                .where(qProduct.merchantId.eq(merchantId)
                        .and(qProduct.accountBookId.eq(accountBookId))
                        .and(qProduct.enabled.isTrue())
                        .and(qProduct.name.contains(keyword)
                                .or(qProduct.code.contains(keyword))
                                .or(qProduct.pinyin.contains(keyword))))
                .limit(limit)
                .fetch();

        List<ProductInfo> list = new ArrayList<>();
        for (Tuple tuple : fetch) {
            ProductInfo info = new ProductInfo();
            info.setId(tuple.get(qProduct.id));
            info.setCode(tuple.get(qProduct.code));
            info.setName(tuple.get(qProduct.name));
            info.setSpecification(tuple.get(qProduct.specification));
            info.setUnitId(tuple.get(qProduct.unitId));
            info.setRetailPrice(tuple.get(qProduct.retailCustomerPrice));
            list.add(info);
        }
        return list;
    }

    /**
     * 获取仓库列表
     */
    public List<WarehouseInfo> getWarehouses(Long merchantId, Long accountBookId) {
        List<Tuple> fetch = bqf.selectFrom(qWarehouse)
                .select(qWarehouse.id, qWarehouse.code, qWarehouse.name)
                .where(qWarehouse.merchantId.eq(merchantId)
                        .and(qWarehouse.accountBookId.eq(accountBookId))
                        .and(qWarehouse.enabled.isTrue()))
                .fetch();

        List<WarehouseInfo> list = new ArrayList<>();
        for (Tuple tuple : fetch) {
            WarehouseInfo info = new WarehouseInfo();
            info.setId(tuple.get(qWarehouse.id));
            info.setCode(tuple.get(qWarehouse.code));
            info.setName(tuple.get(qWarehouse.name));
            list.add(info);
        }
        return list;
    }

    /**
     * 构建系统提示词（极致优化版）
     */
    private String buildSystemPrompt(Long merchantId, Long accountBookId) {
        // 只加载最近的客户和商品，减少提示词长度
        List<CustomerInfo> customers = searchCustomers("", merchantId, accountBookId, 10);
        List<ProductInfo> products = searchProducts("", merchantId, accountBookId, 20);
        List<WarehouseInfo> warehouses = getWarehouses(merchantId, accountBookId);

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是进销存系统的AI助手，帮助用户创建销售订单和销售出库单。\n");
        prompt.append("规则：只处理销售模块，只创建草稿状态，遇到模糊信息主动提问。\n");
        prompt.append("用中文回复。\n\n");

        prompt.append("客户列表：\n");
        for (CustomerInfo c : customers) {
            prompt.append("- ").append(c.getName());
            if (StrUtil.isNotBlank(c.getCode())) prompt.append("(").append(c.getCode()).append(")");
            prompt.append("\n");
        }

        prompt.append("\n商品列表：\n");
        for (ProductInfo p : products) {
            prompt.append("- ").append(p.getName());
            if (p.getRetailPrice() != null) prompt.append(" ¥").append(p.getRetailPrice());
            prompt.append("\n");
        }

        prompt.append("\n仓库列表：\n");
        for (WarehouseInfo w : warehouses) {
            prompt.append("- ").append(w.getName()).append("\n");
        }

        prompt.append("\n你的回复必须是以下JSON格式：\n");
        prompt.append("{\"action\":\"ask|draft|error\",\"message\":\"回复用户的内容\",\"data\":{},\"context\":{}}\n");
        prompt.append("action说明：ask=向用户提问，draft=生成订单草稿，error=错误\n");
        prompt.append("只输出JSON，不要输出其他内容。\n");

        return prompt.toString();
    }

    /**
     * 调用AI模型（流式版本）
     */
    private void callAiModelStream(String systemPrompt, List<ChatMessage> history, String userMessage,
                                   StreamCallback callback) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("model", properties.getModel());
        payload.put("max_tokens", 2048);
        payload.put("temperature", 0.1);
        payload.put("stream", true); // 启用流式响应

        // Anthropic格式：system单独传递
        payload.put("system", systemPrompt);

        // 消息列表
        JSONArray messages = new JSONArray();

        // 历史消息（只保留最近5条）
        int start = Math.max(0, history.size() - 5);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            JSONObject hist = new JSONObject();
            hist.put("role", msg.getRole());
            hist.put("content", msg.getContent());
            messages.add(hist);
        }

        // 用户消息
        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(user);

        payload.put("messages", messages);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getEndpoint()))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-api-key", properties.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toJSONString(), StandardCharsets.UTF_8))
                .build();

        // 使用流式响应
        HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new RuntimeException("AI HTTP " + response.statusCode());
        }

        // 读取流式响应
        StringBuilder fullContent = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if (data.equals("[DONE]")) {
                        break;
                    }
                    try {
                        JSONObject chunk = JSON.parseObject(data);
                        // Anthropic流式格式
                        if (chunk.containsKey("type") && "content_block_delta".equals(chunk.getString("type"))) {
                            JSONObject delta = chunk.getJSONObject("delta");
                            if (delta != null && delta.containsKey("text")) {
                                String text = delta.getString("text");
                                fullContent.append(text);
                                callback.onChunk(text);
                            }
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                        log.debug("解析流式数据失败: {}", data);
                    }
                }
            }
        }

        callback.onComplete(fullContent.toString());
    }

    /**
     * 非流式调用（保留兼容）
     */
    private AiResponse callAiModel(String systemPrompt, List<ChatMessage> history, String userMessage) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("model", properties.getModel());
        payload.put("max_tokens", 2048);
        payload.put("temperature", 0.1);

        payload.put("system", systemPrompt);

        JSONArray messages = new JSONArray();
        int start = Math.max(0, history.size() - 5);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            JSONObject hist = new JSONObject();
            hist.put("role", msg.getRole());
            hist.put("content", msg.getContent());
            messages.add(hist);
        }

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(user);

        payload.put("messages", messages);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getEndpoint()))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("x-api-key", properties.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toJSONString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("AI HTTP " + response.statusCode() + ": " + response.body());
        }

        JSONObject respJson = JSON.parseObject(response.body());
        JSONArray contentArray = respJson.getJSONArray("content");
        if (contentArray == null || contentArray.isEmpty()) {
            throw new RuntimeException("AI 无响应");
        }

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < contentArray.size(); i++) {
            JSONObject block = contentArray.getJSONObject(i);
            if ("text".equals(block.getString("type"))) {
                content.append(block.getString("text"));
            }
        }

        return parseAiResponse(content.toString());
    }

    /**
     * 解析AI响应
     */
    private AiResponse parseAiResponse(String content) {
        try {
            // 尝试解析JSON
            String jsonStr = content.trim();
            // 去掉可能的 ```json ... ``` 包裹
            int start = jsonStr.indexOf('{');
            int end = jsonStr.lastIndexOf('}');
            if (start >= 0 && end > start) {
                jsonStr = jsonStr.substring(start, end + 1);
            }

            JSONObject json = JSON.parseObject(jsonStr);
            AiResponse response = new AiResponse();
            response.setAction(json.getString("action"));
            response.setMessage(json.getString("message"));

            if (json.containsKey("data")) {
                response.setData(json.getJSONObject("data"));
            }
            if (json.containsKey("context")) {
                response.setContext(json.getJSONObject("context"));
            }

            return response;
        } catch (Exception e) {
            // 如果解析失败，返回普通消息
            AiResponse response = new AiResponse();
            response.setAction("message");
            response.setMessage(content);
            return response;
        }
    }

    /**
     * 清除会话上下文
     */
    public void clearContext(String sessionId) {
        contextMap.remove(sessionId);
    }

    // ==================== 数据类 ====================

    @Data
    public static class CustomerInfo {
        private Long id;
        private String code;
        private String name;
        private String phone;
        private Long levelId;
    }

    @Data
    public static class ProductInfo {
        private Long id;
        private String code;
        private String name;
        private String specification;
        private Long unitId;
        private BigDecimal retailPrice;
    }

    @Data
    public static class WarehouseInfo {
        private Long id;
        private String code;
        private String name;
    }

    @Data
    public static class AiResponse {
        private String action;
        private String message;
        private JSONObject data;
        private JSONObject context;

        public static AiResponse error(String message) {
            AiResponse response = new AiResponse();
            response.setAction("error");
            response.setMessage(message);
            return response;
        }

        public static AiResponse ask(String message, List<String> options) {
            AiResponse response = new AiResponse();
            response.setAction("ask");
            response.setMessage(message);
            JSONObject data = new JSONObject();
            data.put("options", options);
            response.setData(data);
            return response;
        }

        public static AiResponse draft(String message, JSONObject order) {
            AiResponse response = new AiResponse();
            response.setAction("draft");
            response.setMessage(message);
            JSONObject data = new JSONObject();
            data.put("order", order);
            response.setData(data);
            return response;
        }
    }

    @Data
    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    public static class ConversationContext {
        private List<ChatMessage> messages = new ArrayList<>();
        private JSONObject selectedCustomer;
        private List<JSONObject> selectedProducts = new ArrayList<>();
        private String currentStep;

        public void addMessage(String role, String content) {
            messages.add(new ChatMessage(role, content));
            // 只保留最近20条消息
            if (messages.size() > 20) {
                messages = messages.subList(messages.size() - 20, messages.size());
            }
        }
    }
}
