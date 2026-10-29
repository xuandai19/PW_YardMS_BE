package com.yms.yms.domain.yard.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.yard.service.YardOperationsService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yard-logs")
public class PublicVehicleQrController {

    private final YardOperationsService operationsService;

    public PublicVehicleQrController(YardOperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @GetMapping("/qr-info")
    public ApiResponse<YardOperationsService.PublicVehicleQrInfo> qrInfo(@RequestParam @NotBlank String token) {
        return ApiResponse.ok(operationsService.publicQrInfo(token));
    }
}
