package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.example.springboot.common.Result;
import org.example.springboot.service.AiClinicService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AI问诊接口")
@RestController
@RequestMapping("/ai/clinic")
public class AiClinicController {

    @Resource
    private AiClinicService aiClinicService;

    @Operation(summary = "智能问诊（文本）")
    @PostMapping("/consult")
    public Result<?> consult(@RequestBody ConsultRequest request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("token");
        return Result.success(aiClinicService.consult(request.getPatientInfo(), request.getQuestion(), token));
    }

    @Operation(summary = "智能识图问诊（图片）")
    @PostMapping("/vision-consult")
    public Result<?> visionConsult(@RequestParam("file") MultipartFile file,
                                   @RequestParam(required = false) String question) {
        return Result.success(aiClinicService.visionConsult(file, question));
    }

    @Data
    public static class ConsultRequest {
        private String patientInfo;
        private String question;
    }
}
