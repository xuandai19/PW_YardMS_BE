package com.yms.yms.domain.yard.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.yard.entity.YardLog;
import com.yms.yms.domain.yard.service.YardOperationsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/yard-logs")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class YardOperationsController {

    private final YardOperationsService operationsService;

    public YardOperationsController(YardOperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @PostMapping("/scan")
    public ApiResponse<YardOperationsService.VehicleCheckInInfo> scan(@Valid @RequestBody ScanRequest request) {
        return ApiResponse.ok(operationsService.scan(request.token()));
    }

    @PostMapping("/check-in")
    public ApiResponse<YardLog> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ApiResponse.ok(operationsService.checkIn(request.token(), request.yardId()));
    }

    @PostMapping("/check-out")
    public ApiResponse<YardOperationsService.CheckOutResult> checkOut(@Valid @RequestBody CheckOutRequest request) {
        return ApiResponse.ok(operationsService.checkOut(request.preNotificationId()));
    }

    @GetMapping
    public ApiResponse<List<YardOperationsService.YardHistoryEntry>> history() {
        return ApiResponse.ok(operationsService.history());
    }

    public record ScanRequest(@NotBlank String token) {
    }

    public record CheckInRequest(@NotBlank String token, @NotNull UUID yardId) {
    }

    public record CheckOutRequest(@NotNull UUID preNotificationId) {
    }
}
