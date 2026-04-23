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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PurchaseAcceptanceImageCreateService {

    @Value("${procurement.ai.acceptance-url:http://127.0.0.1:8000/procurement/parse-acceptance}")
    private String aiUrl;

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
        JsonNode acceptanceNode = callPythonParser(file);
        return createAcceptanceByStructured(acceptanceNode);
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
            JsonNode acceptance = root.path("acceptance");
            if (acceptance.isObject()) {
                return acceptance;
            }
            if (root.isObject()) {
                return root;
            }
            throw new ServiceException("识图服务未返回有效结构化结果");
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

    private PurchaseAcceptance createAcceptanceByStructured(JsonNode acceptanceNode) {
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
            throw new ServiceException("未匹配到可用采购单（仅支持已发送采购单）");
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
            item.setBatchNo(structuredItem.batchNo);
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
            item.batchNo = textOrEmpty(itemNode.path("batch_no"));
            if (item.batchNo.isBlank()) {
                item.batchNo = textOrEmpty(itemNode.path("batchNo"));
            }
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
            PurchaseOrder byNo = purchaseOrderMapper.selectOne(new LambdaQueryWrapper<PurchaseOrder>()
                    .eq(PurchaseOrder::getOrderNo, orderNoHint)
                    .eq(PurchaseOrder::getStatus, 1)
                    .orderByDesc(PurchaseOrder::getUpdateTime)
                    .last("limit 1"));
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
                candidates = new ArrayList<>();
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

    private static class StructuredItem {
        Long medicineId;
        String medicineCode;
        String medicineName;
        Integer receivedQty;
        Integer qualifiedQty;
        String batchNo;
        LocalDate productionDate;
        LocalDate expiryDate;
        String remark;
    }
}
