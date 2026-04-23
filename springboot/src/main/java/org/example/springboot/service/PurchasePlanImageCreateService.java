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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PurchasePlanImageCreateService {

    @Value("${procurement.ai.url:http://127.0.0.1:8000/procurement/parse-plan}")
    private String aiUrl;

    @Resource
    private MedicineMapper medicineMapper;

    @Resource
    private PurchasePlanService purchasePlanService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PurchasePlan createByImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请先上传图片");
        }
        JsonNode planNode = callPythonParser(file);
        return createPlanByStructured(planNode);
    }

    private JsonNode callPythonParser(MultipartFile file) {
        String boundary = "----SpringBootBoundary" + UUID.randomUUID();
        try {
            byte[] body = buildMultipartBody(file, boundary);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ServiceException("识图服务调用失败: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
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

    private byte[] buildMultipartBody(MultipartFile file, String boundary) throws IOException {
        String lineEnd = "\r\n";
        String filename = file.getOriginalFilename() == null ? "upload.jpg" : file.getOriginalFilename();
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();

        byte[] fileBytes = file.getBytes();
        byte[] prefix = (
                "--" + boundary + lineEnd +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + lineEnd +
                        "Content-Type: " + contentType + lineEnd + lineEnd
        ).getBytes(StandardCharsets.UTF_8);
        byte[] suffix = (lineEnd + "--" + boundary + "--" + lineEnd).getBytes(StandardCharsets.UTF_8);

        byte[] merged = new byte[prefix.length + fileBytes.length + suffix.length];
        System.arraycopy(prefix, 0, merged, 0, prefix.length);
        System.arraycopy(fileBytes, 0, merged, prefix.length, fileBytes.length);
        System.arraycopy(suffix, 0, merged, prefix.length + fileBytes.length, suffix.length);
        return merged;
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
