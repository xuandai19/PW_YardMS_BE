package com.yms.yms.domain.service.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.service.entity.ServiceOrder;
import com.yms.yms.domain.service.service.ServiceDispatchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/service-orders")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class ServiceDispatchController {

    private final ServiceDispatchService dispatchService;

    public ServiceDispatchController(ServiceDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping
    public ApiResponse<List<ServiceOrder>> assign(@Valid @RequestBody AssignmentRequest request) {
        return ApiResponse.ok(dispatchService.assign(request.preNotificationId(), request.serviceIds()));
    }

    @GetMapping
    public ApiResponse<List<ServiceOrder>> list(@RequestParam(required = false) UUID vehicleId) {
        return ApiResponse.ok(dispatchService.list(vehicleId));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<ServiceOrder> start(@PathVariable UUID id) {
        return ApiResponse.ok(dispatchService.start(id));
    }

    @PostMapping("/{id}/end")
    public ApiResponse<ServiceOrder> end(@PathVariable UUID id) {
        return ApiResponse.ok(dispatchService.end(id));
    }

    public record AssignmentRequest(@NotNull UUID preNotificationId, @NotEmpty List<@NotNull UUID> serviceIds) {
    }
}
