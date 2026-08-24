package com.flyemu.share.service.ai;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.flyemu.share.config.AiRecognitionProperties;
import com.flyemu.share.entity.basic.QProduct;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiRecognitionService extends BaseService {

    private static final QProduct qProduct = QProduct.product;

    private final AiRecognitionProperties properties;

    /**
     * 识别订单文本，返回匹配到的商品列表。
     * 优先调用大模型（当 enabled 且 apiKey 配置），失败或未配置时降级为规则匹配。
     */
    public List<RecognizedProduct> recognize(String text, Long merchantId, Long accountBookId) {
        List<ProductRow> products = loadProducts(merchantId, accountBookId);
        if (products.isEmpty()) {
            return new ArrayList<>();
        }

        List<ParsedLine> parsed = new ArrayList<>();
        if (properties.isEnabled() && StrUtil.isNotBlank(properties.getApiKey())
                && StrUtil.isNotBlank(properties.getEndpoint())) {
            try {
                parsed = callLlm(text);
            } catch (Exception e) {
                log.warn("AI 识别调用失败，降级规则匹配：{}", e.getMessage());
            }
        }

        // 无论 LLM 是否成功，最终走规则匹配兜底（LLM 返回的 name/code 也会在这里匹配）
        if (parsed.isEmpty()) {
            parsed = ruleParse(text);
        }

        List<RecognizedProduct> result = new ArrayList<>();
        for (ParsedLine line : parsed) {
            ProductRow matched = match(line, products);
            if (matched == null) {
                continue;
            }
            RecognizedProduct rp = new RecognizedProduct();
            rp.setProductId(matched.id);
            rp.setCode(matched.code);
            rp.setName(matched.name);
            rp.setQuantity(line.quantity);
            rp.setUnitPrice(line.unitPrice);
            rp.setTaxRate(matched.taxRate);
            rp.setMatched(true);
            result.add(rp);
        }
        return result;
    }

    private List<ProductRow> loadProducts(Long merchantId, Long accountBookId) {
        List<Tuple> fetch = bqf.selectFrom(qProduct)
                .select(qProduct.id, qProduct.code, qProduct.name, qProduct.pinyin, qProduct.taxRate)
                .where(qProduct.merchantId.eq(merchantId)
                        .and(qProduct.accountBookId.eq(accountBookId))
                        .and(qProduct.enabled.isTrue()))
                .fetch();
        List<ProductRow> list = new ArrayList<>();
        for (Tuple tuple : fetch) {
            ProductRow row = new ProductRow();
            row.id = tuple.get(qProduct.id);
            row.code = tuple.get(qProduct.code);
            row.name = tuple.get(qProduct.name);
            row.pinyin = tuple.get(qProduct.pinyin);
            row.taxRate = tuple.get(qProduct.taxRate);
            list.add(row);
        }
        return list;
    }

    /**
     * 规则解析：按换行/分号/逗号切行，正则抽取数量与单价。
     */
    private List<ParsedLine> ruleParse(String text) {
        List<ParsedLine> lines = new ArrayList<>();
        if (StrUtil.isBlank(text)) {
            return lines;
        }
        String[] rawLines = text.split("[\\n;；,，]");
        for (String raw : rawLines) {
            String s = raw.trim();
            if (s.isEmpty()) {
                continue;
            }
            ParsedLine line = new ParsedLine();
            // 去掉可能出现的“x2”/“X3”/“×3”形式的数量标记，并抽取数字
            String cleaned = s.replaceAll("[×xX*]", "*");
            line.text = cleaned.replaceAll("[0-9.]+", "").replaceAll("[*]", "").trim();
            // 抽取所有数字
            List<BigDecimal> nums = extractNumbers(cleaned);
            if (nums.size() >= 1) {
                line.quantity = nums.get(0);
            }
            if (nums.size() >= 2) {
                line.unitPrice = nums.get(1);
            }
            lines.add(line);
        }
        return lines;
    }

    private List<BigDecimal> extractNumbers(String s) {
        List<BigDecimal> nums = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+(\\.\\d+)?").matcher(s);
        while (m.find()) {
            try {
                nums.add(new BigDecimal(m.group()));
            } catch (NumberFormatException ignored) {
            }
        }
        return nums;
    }

    /**
     * 调用 OpenAI 兼容接口，让大模型返回 JSON 数组 [{name,code,quantity,unitPrice}]。
     */
    private List<ParsedLine> callLlm(String text) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("model", properties.getModel());
        payload.put("temperature", 0);
        JSONArray messages = new JSONArray();
        JSONObject sys = new JSONObject();
        sys.put("role", "system");
        sys.put("content", "你是进销存系统的订单识别助手。从用户文本中提取商品明细，只输出 JSON 数组，"
                + "每个元素包含字段：name(商品名或编码)、code(编码，可为空)、quantity(数量，数字)、unitPrice(单价，数字，可为空)。"
                + "不要输出任何解释或多余文字，只输出 JSON。");
        messages.add(sys);
        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", text);
        messages.add(user);
        payload.put("messages", messages);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getEndpoint()))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(payload.toJSONString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM HTTP " + response.statusCode());
        }
        JSONObject respJson = JSON.parseObject(response.body());
        JSONArray choices = respJson.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("LLM 无 choices");
        }
        String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
        return parseLlmContent(content);
    }

    private List<ParsedLine> parseLlmContent(String content) {
        List<ParsedLine> lines = new ArrayList<>();
        if (StrUtil.isBlank(content)) {
            return lines;
        }
        String jsonStr = content.trim();
        // 去掉可能的 ```json ... ``` 包裹
        int start = jsonStr.indexOf('[');
        int end = jsonStr.lastIndexOf(']');
        if (start >= 0 && end > start) {
            jsonStr = jsonStr.substring(start, end + 1);
        }
        JSONArray arr = JSON.parseArray(jsonStr);
        if (arr == null) {
            return lines;
        }
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            ParsedLine line = new ParsedLine();
            line.text = StrUtil.isNotBlank(obj.getString("code")) ? obj.getString("code") : obj.getString("name");
            line.code = obj.getString("code");
            BigDecimal qty = obj.getBigDecimal("quantity");
            BigDecimal price = obj.getBigDecimal("unitPrice");
            line.quantity = qty;
            line.unitPrice = price;
            lines.add(line);
        }
        return lines;
    }

    private ProductRow match(ParsedLine line, List<ProductRow> products) {
        if (line == null || StrUtil.isBlank(line.text)) {
            return null;
        }
        String key = line.text.trim().toLowerCase(Locale.ROOT);
        // 1. 编码精确匹配
        for (ProductRow p : products) {
            if (p.code != null && p.code.toLowerCase(Locale.ROOT).equals(key)) {
                return p;
            }
        }
        // 2. 名称包含匹配
        for (ProductRow p : products) {
            if (p.name != null && p.name.toLowerCase(Locale.ROOT).contains(key)) {
                return p;
            }
        }
        // 3. 拼音包含匹配
        for (ProductRow p : products) {
            if (p.pinyin != null && p.pinyin.toLowerCase(Locale.ROOT).contains(key)) {
                return p;
            }
        }
        // 4. 名称被包含（用户文本可能比名称长）
        for (ProductRow p : products) {
            if (p.name != null && key.contains(p.name.toLowerCase(Locale.ROOT))) {
                return p;
            }
        }
        return null;
    }

    @Data
    public static class ProductRow {
        private Long id;
        private String code;
        private String name;
        private String pinyin;
        private BigDecimal taxRate;
    }

    @Data
    public static class ParsedLine {
        private String text;
        private String code;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
    }

    @Data
    public static class RecognizedProduct {
        private Long productId;
        private String code;
        private String name;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private boolean matched;
    }
}
