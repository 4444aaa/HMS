package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Medicine;
import org.example.springboot.entity.PurchasePlan;
import org.example.springboot.entity.PurchasePlanItem;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.MedicineMapper;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchasePlanImageCreateService {

    @Value("${qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String qwenBaseUrl;

    @Value("${qwen.api-key:}")
    private String qwenApiKey;

    @Value("${qwen.vision-model:qwen-vl-plus}")
    private String qwenVisionModel;

    @Value("${qwen.timeout-seconds:60}")
    private long timeoutSeconds;

    @Resource
    private MedicineMapper medicineMapper;

    @Resource
    private PurchasePlanService purchasePlanService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PurchasePlan createByImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请先上传图片");
        }
        JsonNode planNode = callQwenVisionParser(file);
        return createPlanByStructured(planNode);
    }

    private JsonNode callQwenVisionParser(MultipartFile file) {
        if (qwenApiKey == null || qwenApiKey.isBlank()) {
            throw new ServiceException("未配置 Qwen API Key，请在 application.properties 设置 qwen.api-key");
        }
        try {
            String contentType = file.getContentType() == null ? "image/jpeg" : file.getContentType();
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String imageUrl = "data:" + contentType + ";base64," + base64Image;
            String prompt = """
                    你是医院采购单据结构化助手。
                    请从图片中提取采购计划信息，输出严格 JSON（不要 markdown，不要解释）：
                    {
                      "plan": {
                        "title": "string",
                        "remark": "string",
                        "items": [
                          {
                            "medicine_code": "string",
                            "medicine_name": "string",
                            "plan_qty": 1,
                            "remark": "string"
                          }
                        ]
                      }
                    }
                    要求：
                    1) plan_qty 必须为正整数；
                    2) 若字段缺失填空字符串；items 至少返回可识别项；
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
            JsonNode plan = root.path("plan");
            if (plan.isMissingNode() || !plan.isObject()) {
                throw new ServiceException("识图服务未返回有效结构化结果");
            }
            return plan;
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

    private PurchasePlan createPlanByStructured(JsonNode planNode) {
        String title = textOrEmpty(planNode.path("title"));
        String remark = textOrEmpty(planNode.path("remark"));
        if (title.isBlank()) {
            title = "识图创建采购计划";
        }

        JsonNode itemsNode = planNode.path("items");
        if (!itemsNode.isArray() || itemsNode.isEmpty()) {
            throw new ServiceException("识图结果中没有可用的药品明细");
        }

        List<PurchasePlanItem> items = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (JsonNode itemNode : itemsNode) {
            String medicineCode = textOrEmpty(itemNode.path("medicine_code"));
            String medicineName = textOrEmpty(itemNode.path("medicine_name"));
            int planQty = parseQty(itemNode.path("plan_qty"));

            Medicine medicine = findMedicine(medicineCode, medicineName);
            if (medicine == null) {
                unresolved.add(!medicineCode.isBlank() ? medicineCode : medicineName);
                continue;
            }
            PurchasePlanItem item = new PurchasePlanItem();
            item.setMedicineId(medicine.getId());
            item.setPlanQty(planQty);
            item.setRemark(textOrEmpty(itemNode.path("remark")));
            items.add(item);
        }

        if (!unresolved.isEmpty()) {
            throw new ServiceException("以下药品未匹配到系统数据: " + String.join("、", unresolved));
        }
        if (items.isEmpty()) {
            throw new ServiceException("识图结果未匹配到有效药品明细");
        }

        PurchasePlan plan = new PurchasePlan();
        plan.setTitle(title);
        plan.setRemark(remark);
        plan.setItems(items);
        return purchasePlanService.createPlan(plan);
    }

    private Medicine findMedicine(String medicineCode, String medicineName) {
        if (!medicineCode.isBlank()) {
            Medicine byCode = medicineMapper.selectOne(new LambdaQueryWrapper<Medicine>()
                    .eq(Medicine::getMedicineCode, medicineCode)
                    .last("limit 1"));
            if (byCode != null) {
                return byCode;
            }
        }
        if (!medicineName.isBlank()) {
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

    private int parseQty(JsonNode qtyNode) {
        if (qtyNode == null || qtyNode.isNull()) {
            return 1;
        }
        if (qtyNode.isInt()) {
            return Math.max(1, qtyNode.asInt());
        }
        try {
            return Math.max(1, Integer.parseInt(qtyNode.asText("1")));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String textOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }
}
