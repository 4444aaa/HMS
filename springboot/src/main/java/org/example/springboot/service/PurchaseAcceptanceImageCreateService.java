package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Medicine;
import org.example.springboot.entity.PurchaseAcceptance;
import org.example.springboot.entity.PurchaseAcceptanceItem;
import org.example.springboot.entity.PurchaseOrder;
import org.example.springboot.entity.PurchaseOrderItem;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.MedicineMapper;
import org.example.springboot.mapper.PurchaseOrderItemMapper;
import org.example.springboot.mapper.PurchaseOrderMapper;
import org.example.springboot.mapper.SupplierMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class PurchaseAcceptanceImageCreateService {

    @Value("${qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String qwenBaseUrl;

    @Value("${qwen.api-key:}")
    private String qwenApiKey;

    @Value("${qwen.vision-model:qwen-vl-plus}")
    private String qwenVisionModel;

    @Value("${qwen.timeout-seconds:60}")
    private long timeoutSeconds;

    @Resource
    private PurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private PurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private SupplierMapper supplierMapper;
    @Resource
    private MedicineMapper medicineMapper;
    @Resource
    private PurchaseAcceptanceService purchaseAcceptanceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PurchaseAcceptance createByImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请先上传图片");
        }
        ParseResult parseResult = callQwenVisionParser(file);
        return createAcceptanceByStructured(parseResult.acceptanceNode, parseResult.rawJson);
    }

    private ParseResult callQwenVisionParser(MultipartFile file) {
        if (qwenApiKey == null || qwenApiKey.isBlank()) {
            throw new ServiceException("未配置 Qwen API Key，请在 application.properties 设置 qwen.api-key");
        }
        try {
            String contentType = file.getContentType() == null ? "image/jpeg" : file.getContentType();
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String imageUrl = "data:" + contentType + ";base64," + base64Image;
            String prompt = """
                    你是医院采购核验单结构化助手。
                    请从图片中提取验收信息，输出严格 JSON（不要 markdown，不要解释）：
                    {
                      "acceptance": {
                        "purchase_order_no": "string",
                        "supplier_name": "string",
                        "remark": "string",
                        "items": [
                          {
                            "medicine_code": "string",
                            "medicine_name": "string",
                            "received_qty": 1,
                            "qualified_qty": 1,
                            "production_date": "yyyy-MM-dd",
                            "expiry_date": "yyyy-MM-dd",
                            "remark": "string"
                          }
                        ]
                      }
                    }
                    要求：
                    0) “订货单号”“采购单号”“订单号”都视为同一业务字段，请统一填写到 purchase_order_no；
                    1) 数量为非负整数，qualified_qty 不大于 received_qty；
                    2) 日期无法识别时填空字符串；
                    3) 仅返回 JSON。
                    """;

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", qwenVisionModel);
            payload.put("temperature", 0.1);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", "你是严谨的图像结构化信息提取助手。"),
                    Map.of("role", "user", "content", List.of(
                            Map.of("type", "text", "text", prompt),
                            Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
                    ))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(qwenBaseUrl))
                    .header("Authorization", "Bearer " + qwenApiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .timeout(Duration.ofSeconds(Math.max(10, timeoutSeconds)))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ServiceException("识图服务调用失败: HTTP " + response.statusCode() + "，响应：" + response.body());
            }
            JsonNode qwenRoot = objectMapper.readTree(response.body());
            String content = qwenRoot.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new ServiceException("识图服务未返回内容");
            }
            JsonNode root = tryParseJson(content);
            JsonNode acceptance = root.path("acceptance");
            if (acceptance.isObject()) {
                return new ParseResult(acceptance, jsonForEcho(root));
            }
            if (root.isObject()) {
                return new ParseResult(root, jsonForEcho(root));
            }
            throw new ServiceException("识图服务未返回有效结构化结果");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("识图服务调用失败: " + e.getMessage());
        } catch (IOException e) {
            throw new ServiceException("识图服务调用失败: " + e.getMessage());
        }
    }

    private JsonNode tryParseJson(String content) throws IOException {
        String t = content.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        return objectMapper.readTree(t);
    }

    private PurchaseAcceptance createAcceptanceByStructured(JsonNode acceptanceNode, String aiRawJson) {
        String orderNoHint = firstNonBlank(
                textOrEmpty(acceptanceNode.path("purchase_order_no")),
                textOrEmpty(acceptanceNode.path("order_no")),
                textOrEmpty(acceptanceNode.path("purchaseOrderNo"))
        );
        String supplierNameHint = firstNonBlank(
                textOrEmpty(acceptanceNode.path("supplier_name")),
                textOrEmpty(acceptanceNode.path("supplierName"))
        );
        String remark = firstNonBlank(
                textOrEmpty(acceptanceNode.path("remark")),
                "识图创建验收单"
        );

        JsonNode itemsNode = acceptanceNode.path("items");
        List<StructuredItem> structuredItems = parseStructuredItems(itemsNode);
        PurchaseOrder order = findBestPurchaseOrder(orderNoHint, supplierNameHint, structuredItems);
        if (order == null) {
            throw new ServiceException("未匹配到可用采购单（仅支持已发送采购单）。"
                    + buildDebugEcho(orderNoHint, supplierNameHint, structuredItems)
                    + "；【AI原始JSON】" + emptyToDash(aiRawJson));
        }

        List<PurchaseOrderItem> orderItems = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, order.getId())
        );
        if (orderItems.isEmpty()) {
            throw new ServiceException("匹配到的采购单没有明细，无法创建验收单");
        }
        Map<Long, List<PurchaseOrderItem>> orderItemsByMedicine = new HashMap<>();
        for (PurchaseOrderItem item : orderItems) {
            if (item.getMedicineId() == null) {
                continue;
            }
            orderItemsByMedicine.computeIfAbsent(item.getMedicineId(), k -> new ArrayList<>()).add(item);
        }

        List<PurchaseAcceptanceItem> acceptanceItems = new ArrayList<>();
        Set<Long> usedOrderItemIds = new HashSet<>();
        for (StructuredItem si : structuredItems) {
            if (si.medicineId == null) {
                continue;
            }
            List<PurchaseOrderItem> candidates = orderItemsByMedicine.get(si.medicineId);
            if (candidates == null || candidates.isEmpty()) {
                continue;
            }
            PurchaseOrderItem matched = null;
            for (PurchaseOrderItem candidate : candidates) {
                if (!usedOrderItemIds.contains(candidate.getId())) {
                    matched = candidate;
                    break;
                }
            }
            if (matched == null) {
                continue;
            }
            usedOrderItemIds.add(matched.getId());
            acceptanceItems.add(buildAcceptanceItem(matched, si));
        }

        if (acceptanceItems.isEmpty()) {
            for (PurchaseOrderItem orderItem : orderItems) {
                acceptanceItems.add(buildAcceptanceItem(orderItem, null));
            }
        }

        PurchaseAcceptance acceptance = new PurchaseAcceptance();
        acceptance.setPurchaseOrderId(order.getId());
        acceptance.setRemark(remark);
        acceptance.setItems(acceptanceItems);
        return purchaseAcceptanceService.createAcceptance(acceptance);
    }

    private PurchaseAcceptanceItem buildAcceptanceItem(PurchaseOrderItem orderItem, StructuredItem structuredItem) {
        PurchaseAcceptanceItem item = new PurchaseAcceptanceItem();
        item.setPurchaseOrderItemId(orderItem.getId());
        int ordered = safeQty(orderItem.getOrderQty());
        int received = structuredItem == null ? ordered : normalizeQty(structuredItem.receivedQty, ordered);
        int qualifiedDefault = structuredItem == null ? received : structuredItem.qualifiedQty;
        int qualified = normalizeQty(qualifiedDefault, received);
        item.setReceivedQty(received);
        item.setQualifiedQty(qualified);
        if (structuredItem != null) {
            item.setProductionDate(structuredItem.productionDate);
            item.setExpiryDate(structuredItem.expiryDate);
            item.setRemark(structuredItem.remark);
        }
        return item;
    }

    private List<StructuredItem> parseStructuredItems(JsonNode itemsNode) {
        List<StructuredItem> result = new ArrayList<>();
        if (!itemsNode.isArray()) {
            return result;
        }
        for (JsonNode itemNode : itemsNode) {
            StructuredItem item = new StructuredItem();
            item.medicineCode = firstNonBlank(
                    textOrEmpty(itemNode.path("medicine_code")),
                    textOrEmpty(itemNode.path("medicineCode"))
            );
            item.medicineName = firstNonBlank(
                    textOrEmpty(itemNode.path("medicine_name")),
                    textOrEmpty(itemNode.path("medicineName"))
            );
            item.receivedQty = parseQty(firstNonBlank(
                    textOrEmpty(itemNode.path("received_qty")),
                    textOrEmpty(itemNode.path("receivedQty")),
                    textOrEmpty(itemNode.path("qty")),
                    textOrEmpty(itemNode.path("quantity"))
            ));
            Integer qualified = parseQty(firstNonBlank(
                    textOrEmpty(itemNode.path("qualified_qty")),
                    textOrEmpty(itemNode.path("qualifiedQty"))
            ));
            item.qualifiedQty = qualified == null ? item.receivedQty : qualified;
            item.productionDate = parseDate(firstNonBlank(
                    textOrEmpty(itemNode.path("production_date")),
                    textOrEmpty(itemNode.path("productionDate"))
            ));
            item.expiryDate = parseDate(firstNonBlank(
                    textOrEmpty(itemNode.path("expiry_date")),
                    textOrEmpty(itemNode.path("expiryDate"))
            ));
            item.remark = textOrEmpty(itemNode.path("remark"));
            Medicine medicine = findMedicine(item.medicineCode, item.medicineName);
            if (medicine != null) {
                item.medicineId = medicine.getId();
            }
            result.add(item);
        }
        return result;
    }

    private PurchaseOrder findBestPurchaseOrder(String orderNoHint, String supplierNameHint, List<StructuredItem> items) {
        if (orderNoHint != null && !orderNoHint.isBlank()) {
            PurchaseOrder byNo = findOrderByOrderNo(orderNoHint);
            if (byNo != null) {
                return byNo;
            }
        }

        List<PurchaseOrder> candidates;
        if (supplierNameHint != null && !supplierNameHint.isBlank()) {
            List<Long> supplierIds = supplierMapper.selectList(new LambdaQueryWrapper<org.example.springboot.entity.Supplier>()
                            .like(org.example.springboot.entity.Supplier::getName, supplierNameHint))
                    .stream()
                    .map(org.example.springboot.entity.Supplier::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (supplierIds.isEmpty()) {
                // 供应商名可能被识别成“收货单位”或存在简称差异，此时降级到全量已发送采购单再做药品打分匹配
                candidates = purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getStatus, 1)
                        .orderByDesc(PurchaseOrder::getUpdateTime));
            } else {
                candidates = purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getStatus, 1)
                        .in(PurchaseOrder::getSupplierId, supplierIds)
                        .orderByDesc(PurchaseOrder::getUpdateTime));
            }
        } else {
            candidates = purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                    .eq(PurchaseOrder::getStatus, 1)
                    .orderByDesc(PurchaseOrder::getUpdateTime));
        }
        if (candidates.isEmpty()) {
            return null;
        }
        if (items == null || items.isEmpty()) {
            return candidates.get(0);
        }

        Set<Long> parsedMedicineIds = items.stream()
                .map(i -> i.medicineId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (parsedMedicineIds.isEmpty()) {
            return candidates.get(0);
        }

        PurchaseOrder bestOrder = null;
        int bestScore = -1;
        int bestTotal = Integer.MAX_VALUE;
        for (PurchaseOrder candidate : candidates) {
            List<PurchaseOrderItem> orderItems = purchaseOrderItemMapper.selectList(
                    new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, candidate.getId())
            );
            Set<Long> orderMedicineIds = orderItems.stream()
                    .map(PurchaseOrderItem::getMedicineId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            int score = 0;
            for (Long mid : parsedMedicineIds) {
                if (orderMedicineIds.contains(mid)) {
                    score++;
                }
            }
            int total = orderItems.size();
            if (score > bestScore || (score == bestScore && total < bestTotal)) {
                bestOrder = candidate;
                bestScore = score;
                bestTotal = total;
            }
        }
        if (bestOrder == null || bestScore <= 0) {
            return null;
        }
        return bestOrder;
    }

    private PurchaseOrder findOrderByOrderNo(String orderNoHint) {
        String raw = orderNoHint.trim();
        String normalizedHint = normalizeOrderNo(raw);

        List<PurchaseOrder> candidates = purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getStatus, 1)
                .orderByDesc(PurchaseOrder::getUpdateTime));
        if (candidates.isEmpty()) {
            return null;
        }

        // 1) 精确匹配优先
        for (PurchaseOrder order : candidates) {
            if (raw.equalsIgnoreCase(String.valueOf(order.getOrderNo()))) {
                return order;
            }
        }

        // 2) 归一化后匹配（去空格、横杠、斜杠等）
        for (PurchaseOrder order : candidates) {
            String n = normalizeOrderNo(order.getOrderNo());
            if (!normalizedHint.isBlank() && normalizedHint.equalsIgnoreCase(n)) {
                return order;
            }
        }

        // 3) 包含匹配（处理 OCR 混入前后缀字符）
        for (PurchaseOrder order : candidates) {
            String n = normalizeOrderNo(order.getOrderNo());
            if (!normalizedHint.isBlank() && (n.contains(normalizedHint) || normalizedHint.contains(n))) {
                return order;
            }
        }
        return null;
    }

    private Medicine findMedicine(String medicineCode, String medicineName) {
        if (medicineCode != null && !medicineCode.isBlank()) {
            Medicine byCode = medicineMapper.selectOne(new LambdaQueryWrapper<Medicine>()
                    .eq(Medicine::getMedicineCode, medicineCode)
                    .last("limit 1"));
            if (byCode != null) {
                return byCode;
            }
        }
        if (medicineName != null && !medicineName.isBlank()) {
            Medicine exact = medicineMapper.selectOne(new LambdaQueryWrapper<Medicine>()
                    .eq(Medicine::getMedicineName, medicineName)
                    .last("limit 1"));
            if (exact != null) {
                return exact;
            }
            return medicineMapper.selectOne(new LambdaQueryWrapper<Medicine>()
                    .like(Medicine::getMedicineName, medicineName)
                    .last("limit 1"));
        }
        return null;
    }

    private Integer parseQty(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().replaceAll("[^0-9]", "");
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int normalizeQty(Integer value, int maxAllowed) {
        int safeMax = Math.max(0, maxAllowed);
        int v = value == null ? safeMax : Math.max(0, value);
        return Math.min(v, safeMax);
    }

    private int safeQty(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim().replace('.', '-').replace('/', '-');
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-M-d", Locale.CHINA),
                DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA),
                DateTimeFormatter.ofPattern("yyyyMMdd", Locale.CHINA)
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String textOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalizeOrderNo(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .replaceAll("[\\s\\-_/：:]", "")
                .toUpperCase(Locale.ROOT);
    }

    private String buildDebugEcho(String orderNoHint, String supplierNameHint, List<StructuredItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("【调试回显】");
        sb.append("识别采购单号=").append(emptyToDash(orderNoHint));
        sb.append("（归一化=").append(emptyToDash(normalizeOrderNo(orderNoHint))).append("）");
        sb.append("；识别供应商=").append(emptyToDash(supplierNameHint));

        if (items == null || items.isEmpty()) {
            sb.append("；识别药品明细=0条");
            return sb.toString();
        }

        sb.append("；识别药品明细=").append(items.size()).append("条");
        int limit = Math.min(5, items.size());
        List<String> sample = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            StructuredItem it = items.get(i);
            String name = !textOrEmptyValue(it.medicineName).isBlank() ? it.medicineName : it.medicineCode;
            String qty = it.receivedQty == null ? "-" : String.valueOf(it.receivedQty);
            String matched = it.medicineId == null ? "未匹配药品" : "已匹配药品ID:" + it.medicineId;
            sample.add((name == null || name.isBlank() ? "-" : name) + " x" + qty + "(" + matched + ")");
        }
        sb.append("；样例=").append(String.join("、", sample));
        if (items.size() > limit) {
            sb.append("...");
        }
        return sb.toString();
    }

    private String emptyToDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value.trim();
    }

    private String textOrEmptyValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String jsonForEcho(JsonNode node) {
        try {
            String json = objectMapper.writeValueAsString(node);
            if (json.length() > 8000) {
                return json.substring(0, 8000) + "...(truncated)";
            }
            return json;
        } catch (Exception e) {
            return "{}";
        }
    }

    private static class ParseResult {
        final JsonNode acceptanceNode;
        final String rawJson;

        ParseResult(JsonNode acceptanceNode, String rawJson) {
            this.acceptanceNode = acceptanceNode;
            this.rawJson = rawJson;
        }
    }

    private static class StructuredItem {
        Long medicineId;
        String medicineCode;
        String medicineName;
        Integer receivedQty;
        Integer qualifiedQty;
        LocalDate productionDate;
        LocalDate expiryDate;
        String remark;
    }
}
