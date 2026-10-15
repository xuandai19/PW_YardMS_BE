package com.yms.yms.domain.yard.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.yard.entity.Yard;
import com.yms.yms.domain.yard.entity.YardAlert;
import com.yms.yms.domain.yard.entity.YardLog;
import com.yms.yms.domain.yard.service.YardManagementService;
import com.yms.yms.domain.yard.service.YardOperationsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/yards")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class YardController {

    private final YardManagementService yardService;
    private final YardOperationsService operationsService;
    public YardController(YardManagementService yardService, YardOperationsService operationsService) {
        this.yardService = yardService;
        this.operationsService = operationsService;
    }

    @GetMapping
    public ApiResponse<List<Yard>> list() {
        return ApiResponse.ok(yardService.list());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Yard> create(@Valid @RequestBody YardRequest request) {
        return ApiResponse.ok(yardService.create(toEntity(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Yard> update(@PathVariable UUID id, @Valid @RequestBody YardRequest request) {
        return ApiResponse.ok(yardService.update(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        yardService.deactivate(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{yardId}/assignments")
    public ApiResponse<YardLog> assignVehicle(@PathVariable UUID yardId,
                                              @Valid @RequestBody YardAssignmentRequest request) {
        return ApiResponse.ok(operationsService.checkIn(request.token(), yardId));
    }

    @GetMapping("/inventory")
    public ApiResponse<List<YardManagementService.YardInventory>> inventory() {
        return ApiResponse.ok(yardService.inventory());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<YardAlert>> alerts() {
        return ApiResponse.ok(yardService.activeAlerts());
    }

    private Yard toEntity(YardRequest request) {
        Yard yard = new Yard();
        yard.setCode(request.code());
        yard.setName(request.name());
        yard.setAddress(request.address());
        yard.setLatitude(request.latitude());
        yard.setLongitude(request.longitude());
        yard.setCapacity(request.capacity());
        yard.setStatus(request.status());
        return yard;
    }

    public record YardRequest(@NotBlank String code, @NotBlank String name, @NotBlank String address,
                              BigDecimal latitude, BigDecimal longitude, @Positive int capacity, String status) {
    }

    public record YardAssignmentRequest(@NotBlank String token) {
    }
}
