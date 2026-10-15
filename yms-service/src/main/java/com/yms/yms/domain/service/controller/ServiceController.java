package com.yms.yms.domain.service.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.service.entity.ServiceEntity;
import com.yms.yms.domain.service.entity.ServicePriceHistory;
import com.yms.yms.domain.service.service.ServiceCatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    public ServiceController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping
    public ApiResponse<List<ServiceEntity>> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.ok(activeOnly ? serviceCatalogService.findActive() : serviceCatalogService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceEntity> get(@PathVariable UUID id) {
        return ApiResponse.ok(serviceCatalogService.findById(id));
    }

    @GetMapping("/{id}/price-history")
    public ApiResponse<List<ServicePriceHistory>> priceHistory(@PathVariable UUID id) {
        return ApiResponse.ok(serviceCatalogService.priceHistory(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ServiceEntity> create(@Valid @RequestBody ServiceRequest request) {
        return ApiResponse.ok(serviceCatalogService.create(toEntity(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ServiceEntity> update(@PathVariable UUID id, @Valid @RequestBody ServiceRequest request) {
        return ApiResponse.ok(serviceCatalogService.update(id, toEntity(request), null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        serviceCatalogService.delete(id);
        return ApiResponse.ok(null);
    }

    private ServiceEntity toEntity(ServiceRequest request) {
        ServiceEntity entity = new ServiceEntity();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setServiceType(request.serviceType());
        entity.setUnit(request.unit() != null ? request.unit() : "HOUR");
        entity.setHourlyRate(request.hourlyRate());
        entity.setActive(request.active() == null || request.active());
        return entity;
    }

    public record ServiceRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String serviceType,
            String unit,
            @NotNull BigDecimal hourlyRate,
            Boolean active
    ) {
    }
}
