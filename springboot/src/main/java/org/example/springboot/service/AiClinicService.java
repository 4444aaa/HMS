package org.example.springboot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Department;
import org.example.springboot.entity.Doctor;
import org.example.springboot.entity.Medicine;
import org.example.springboot.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiClinicService {
    private static final int MAX_MEMORY_MESSAGES = 20; // 10轮问答（user+assistant）
    private static final int MAX_SESSION_CACHE_SIZE = 200;

    @Value("${qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String qwenBaseUrl;

    @Value("${qwen.api-key:}")
    private String qwenApiKey;

    @Value("${qwen.text-model:qwen-plus}")
    private String qwenTextModel;

    @Value("${qwen.vision-model:qwen-vl-plus}")
    private String qwenVisionModel;

    @Value("${qwen.timeout-seconds:60}")
    private long timeoutSeconds;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private DepartmentService departmentService;

    @Resource
    private DoctorService doctorService;

    @Resource
    private MedicineService medicineService;

    private final ConcurrentHashMap<String, Deque<Map<String, Object>>> chatMemoryStore = new ConcurrentHashMap<>();

    public Map<String, Object> consult(String patientInfo, String question) {
        return consult(patientInfo, question, null);
    }

    public Map<String, Object> consult(String patientInfo, String question, String loginToken) {
        if (question == null || question.isBlank()) {
            throw new ServiceException("问诊问题不能为空");
        }

        Map<String, Object> system = Map.of(
                "role", "system",
                "content", """
                        你是一个医疗问诊助手，请根据用户提供信息做初步建议，并可按需调用工具查询医院系统信息。
                        输出要求（必须遵守）：
                        1) 用简体中文，尽量短句，避免大段连续文字。
                        2) 输出时按以下结构分段，并用标题标注：
                           【初步判断】
                           【建议就诊科室】
                           【居家观察与注意事项】
                           【危险信号】
                           【温馨提示】
                        3) 每个分段下优先使用 2-5 条要点，使用“• ”开头。
                        4) 当用户问到科室、医生、药品时，优先通过工具获取数据后回答，不要编造。
                        5) 如问题涉及药品，需强调“本院”该药品的情况，避免谈及“市面”“药店”。
                        6) 在“危险信号”里明确哪些情况要立即线下就医或急诊。
                        7) 最后一行必须是：以上仅供参考，不能替代医生面诊。
                        """
        );

        String userPrompt = "患者信息：" + safeText(patientInfo) + "\n问题：" + question.trim();
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(system);
        List<Map<String, Object>> history = getHistoryMessages(loginToken);
        if (!history.isEmpty()) {
            messages.addAll(history);
        }
        messages.add(Map.of("role", "user", "content", userPrompt));

        String answer = chatWithTools(qwenTextModel, messages, buildClinicTools());
        answer = beautifyAnswer(answer);
        saveHistory(loginToken, userPrompt, answer);
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("model", qwenTextModel);
        return result;
    }

    private List<Map<String, Object>> getHistoryMessages(String loginToken) {
        String key = buildMemoryKey(loginToken);
        if (key.isBlank()) {
            return List.of();
        }
        Deque<Map<String, Object>> queue = chatMemoryStore.get(key);
        if (queue == null || queue.isEmpty()) {
            return List.of();
        }
        synchronized (queue) {
            return new ArrayList<>(queue);
        }
    }

    private void saveHistory(String loginToken, String userPrompt, String answer) {
        String key = buildMemoryKey(loginToken);
        if (key.isBlank()) {
            return;
        }
        if (chatMemoryStore.size() > MAX_SESSION_CACHE_SIZE) {
            chatMemoryStore.clear();
        }
        Deque<Map<String, Object>> queue = chatMemoryStore.computeIfAbsent(key, k -> new LinkedList<>());
        synchronized (queue) {
            queue.addLast(Map.of("role", "user", "content", userPrompt));
            queue.addLast(Map.of("role", "assistant", "content", answer));
            while (queue.size() > MAX_MEMORY_MESSAGES) {
                queue.pollFirst();
            }
        }
    }

    private String buildMemoryKey(String loginToken) {
        String token = safeText(loginToken);
        if (token.isBlank()) {
            return "";
        }
        String shortToken = token.length() > 24 ? token.substring(0, 24) : token;
        return "clinic:" + shortToken;
    }

    private String chatWithTools(String model, List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        // 最多做 4 轮工具调用，防止无限循环
        for (int i = 0; i < 4; i++) {
            JsonNode root = callQwenRaw(model, messages, tools);
            JsonNode messageNode = root.path("choices").path(0).path("message");
            if (messageNode.isMissingNode() || messageNode.isNull()) {
                throw new ServiceException("Qwen API 返回异常：缺少 message");
            }

            String content = messageNode.path("content").asText("");
            JsonNode toolCallsNode = messageNode.path("tool_calls");
            if (!toolCallsNode.isArray() || toolCallsNode.isEmpty()) {
                String finalText = content == null ? "" : content.trim();
                if (finalText.isBlank()) {
                    throw new ServiceException("Qwen API 返回为空");
                }
                return finalText;
            }

            messages.add(toAssistantMessage(messageNode));
            for (JsonNode call : toolCallsNode) {
                String toolCallId = call.path("id").asText("");
                String toolName = call.path("function").path("name").asText("");
                String arguments = call.path("function").path("arguments").asText("{}");
                String toolResult = executeTool(toolName, arguments);
                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", toolCallId,
                        "content", toolResult
                ));
            }
        }
        throw new ServiceException("工具调用轮次过多，请简化问题后重试");
    }

    private List<Map<String, Object>> buildClinicTools() {
        Map<String, Object> deptTool = Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "queryDepartments",
                        "description", "查询科室信息，可按关键词过滤（科室名称/编码/描述）",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "keyword", Map.of("type", "string", "description", "可选关键词，如 内科、儿科"),
                                        "limit", Map.of("type", "integer", "description", "返回条数，默认5，最大10")
                                )
                        )
                )
        );
        Map<String, Object> doctorTool = Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "queryDoctors",
                        "description", "查询医生信息，可按医生姓名、职称、专长、科室关键词过滤",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "keyword", Map.of("type", "string", "description", "医生或科室相关关键词"),
                                        "limit", Map.of("type", "integer", "description", "返回条数，默认5，最大10")
                                )
                        )
                )
        );
        Map<String, Object> medTool = Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "queryMedicines",
                        "description", "查询药品信息，可按药品名、编码、分类、说明过滤",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "keyword", Map.of("type", "string", "description", "药品相关关键词"),
                                        "limit", Map.of("type", "integer", "description", "返回条数，默认5，最大10")
                                )
                        )
                )
        );
        return List.of(deptTool, doctorTool, medTool);
    }

    private String executeTool(String toolName, String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            String keyword = Objects.toString(args.getOrDefault("keyword", ""), "").trim();
            int limit = normalizeLimit(args.get("limit"));

            return switch (toolName) {
                case "queryDepartments" -> objectMapper.writeValueAsString(queryDepartments(keyword, limit));
                case "queryDoctors" -> objectMapper.writeValueAsString(queryDoctors(keyword, limit));
                case "queryMedicines" -> objectMapper.writeValueAsString(queryMedicines(keyword, limit));
                default -> "{\"error\":\"unknown tool: " + toolName + "\"}";
            };
        } catch (Exception e) {
            return "{\"error\":\"tool execution failed: " + safeText(e.getMessage()) + "\"}";
        }
    }

    private List<Map<String, Object>> queryDepartments(String keyword, int limit) {
        List<Department> departments = departmentService.getAllDepartments();
        List<Map<String, Object>> result = new ArrayList<>();
        String kw = safeText(keyword).toLowerCase();
        for (Department d : departments) {
            if (!kw.isBlank()) {
                String merged = (safeText(d.getDeptName()) + " " + safeText(d.getDeptCode()) + " " + safeText(d.getDescription())).toLowerCase();
                if (!merged.contains(kw)) {
                    continue;
                }
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("deptName", d.getDeptName());
            item.put("deptCode", d.getDeptCode());
            item.put("description", d.getDescription());
            item.put("status", d.getStatus());
            result.add(item);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private List<Map<String, Object>> queryDoctors(String keyword, int limit) {
        List<Doctor> doctors = doctorService.getAllDoctors();
        List<Map<String, Object>> result = new ArrayList<>();
        String kw = safeText(keyword).toLowerCase();
        for (Doctor d : doctors) {
            String deptName = d.getDepartment() == null ? "" : safeText(d.getDepartment().getDeptName());
            if (!kw.isBlank()) {
                String merged = (safeText(d.getName()) + " " + safeText(d.getTitle()) + " " + safeText(d.getExpertise()) + " " + deptName).toLowerCase();
                if (!merged.contains(kw)) {
                    continue;
                }
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("doctorNo", d.getDoctorNo());
            item.put("name", d.getName());
            item.put("title", d.getTitle());
            item.put("expertise", d.getExpertise());
            item.put("departmentName", deptName);
            item.put("status", d.getStatus());
            result.add(item);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private List<Map<String, Object>> queryMedicines(String keyword, int limit) {
        List<Medicine> medicines = medicineService.getAllMedicines();
        List<Map<String, Object>> result = new ArrayList<>();
        String kw = safeText(keyword).toLowerCase();
        for (Medicine m : medicines) {
            if (!kw.isBlank()) {
                String merged = (safeText(m.getMedicineName()) + " " + safeText(m.getMedicineCode()) + " " + safeText(m.getCategory()) + " " + safeText(m.getInstructions())).toLowerCase();
                if (!merged.contains(kw)) {
                    continue;
                }
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("medicineCode", m.getMedicineCode());
            item.put("medicineName", m.getMedicineName());
            item.put("category", m.getCategory());
            item.put("specification", m.getSpecification());
            item.put("salePrice", m.getSalePrice());
            item.put("stock", m.getStock());
            item.put("instructions", m.getInstructions());
            result.add(item);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private int normalizeLimit(Object rawLimit) {
        if (rawLimit == null) {
            return 5;
        }
        try {
            int v = Integer.parseInt(String.valueOf(rawLimit));
            if (v < 1) {
                return 5;
            }
            return Math.min(v, 10);
        } catch (Exception e) {
            return 5;
        }
    }

    private Map<String, Object> toAssistantMessage(JsonNode messageNode) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "assistant");
        if (messageNode.path("content").isNull()) {
            msg.put("content", null);
        } else {
            msg.put("content", messageNode.path("content").asText(""));
        }
        JsonNode toolCallsNode = messageNode.path("tool_calls");
        if (toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
            List<Map<String, Object>> toolCalls = new ArrayList<>();
            for (JsonNode call : toolCallsNode) {
                Map<String, Object> callMap = new HashMap<>();
                callMap.put("id", call.path("id").asText(""));
                callMap.put("type", "function");
                callMap.put("function", Map.of(
                        "name", call.path("function").path("name").asText(""),
                        "arguments", call.path("function").path("arguments").asText("{}")
                ));
                toolCalls.add(callMap);
            }
            msg.put("tool_calls", toolCalls);
        }
        return msg;
    }

    private JsonNode callQwenRaw(String model, List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        if (qwenApiKey == null || qwenApiKey.isBlank()) {
            throw new ServiceException("未配置 Qwen API Key，请在 application.properties 设置 qwen.api-key");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", 0.3);
        if (tools != null && !tools.isEmpty()) {
            payload.put("tools", tools);
            payload.put("tool_choice", "auto");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(qwenBaseUrl))
                    .header("Authorization", "Bearer " + qwenApiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .timeout(Duration.ofSeconds(Math.max(10, timeoutSeconds)))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ServiceException("Qwen API 调用失败: HTTP " + response.statusCode() + "，响应：" + response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("Qwen API 调用被中断");
        } catch (IOException e) {
            throw new ServiceException("Qwen API 调用失败: " + e.getMessage());
        }
    }

    // 保留该方法，兼容已有控制器接口；前端可不使用
    public Map<String, Object> visionConsult(org.springframework.web.multipart.MultipartFile file, String question) {
        throw new ServiceException("当前版本未启用智能问诊识图功能");
    }

    @SuppressWarnings("unused")
    private String callQwen(String model, List<Map<String, Object>> messages) {
        JsonNode root = callQwenRaw(model, messages, null);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String content = contentNode.asText("").trim();
        if (content.isBlank()) {
            throw new ServiceException("Qwen API 返回为空");
        }
        return content;
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String beautifyAnswer(String raw) {
        String text = safeText(raw)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        if (text.isBlank()) {
            return text;
        }

        // 合并连续空行，避免稀疏排版
        while (text.contains("\n\n\n")) {
            text = text.replace("\n\n\n", "\n\n");
        }

        // 兜底：把挤在同一行的分段标题/要点拆开
        text = text.replaceAll("\\s*【", "\n【");
        text = text.replaceAll("\\s*•\\s*", "\n• ");
        text = text.replaceAll("\\n{3,}", "\n\n").trim();

        // 没有明显结构时，按句切分成要点，避免大段文本
        boolean hasSectionTitle = text.contains("【初步判断】")
                || text.contains("【建议就诊科室】")
                || text.contains("【居家观察与注意事项】")
                || text.contains("【危险信号】")
                || text.contains("【温馨提示】");
        if (!hasSectionTitle) {
            String[] sentences = text.split("(?<=[。！？；])");
            StringBuilder sb = new StringBuilder();
            sb.append("【问诊建议】\n");
            for (String sentence : sentences) {
                String s = sentence.trim();
                if (s.isEmpty()) {
                    continue;
                }
                sb.append("• ").append(s).append("\n");
            }
            text = sb.toString().trim();
        }

        // 把破折号/数字列表统一成项目符号
        String[] lines = text.split("\n");
        StringBuilder formatted = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) {
                if (formatted.length() > 0 && formatted.charAt(formatted.length() - 1) != '\n') {
                    formatted.append('\n');
                }
                formatted.append('\n');
                continue;
            }
            boolean isTitle = t.startsWith("【") && t.endsWith("】");
            if (!isTitle && (t.matches("^[0-9]+[\\.|、].*") || t.startsWith("- ") || t.startsWith("—") || t.startsWith("* "))) {
                t = "• " + t.replaceFirst("^[0-9]+[\\.|、]\\s*", "")
                        .replaceFirst("^[-—*]\\s*", "");
            }
            formatted.append(t).append('\n');
        }

        String out = formatted.toString().trim();
        if (!out.contains("以上仅供参考，不能替代医生面诊。")) {
            out = out + "\n\n以上仅供参考，不能替代医生面诊。";
        }
        return out;
    }
}
