package com.yms.yms.domain.vehicle.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.vehicle.entity.PreNotification;
import com.yms.yms.domain.vehicle.service.QrCodeService;
import com.yms.yms.domain.vehicle.service.VehicleRegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class VehicleOperationsController {

    private final VehicleRegistrationService registrationService;
    private final QrCodeService qrCodeService;

    public VehicleOperationsController(VehicleRegistrationService registrationService,
                                      QrCodeService qrCodeService) {
        this.registrationService = registrationService;
        this.qrCodeService = qrCodeService;
    }

    @PostMapping("/pre-notifications")
    public ApiResponse<PreNotification> register(@Valid @RequestBody RegistrationRequest request) {
        return ApiResponse.ok(registrationService.register(new VehicleRegistrationService.RegistrationRequest(
                request.partnerAId(), request.plateNumber(), request.plannedArrival(), request.driverName(),
                request.driverPhone(), request.carrierCompany(), request.vehicleType(), request.serviceIds())));
    }

    @GetMapping("/pre-notifications/waiting")
    public ApiResponse<List<PreNotification>> waiting() {
        return ApiResponse.ok(registrationService.waiting());
    }

    @GetMapping("/pre-notifications/in-yard")
    public ApiResponse<List<PreNotification>> inYard() {
        return ApiResponse.ok(registrationService.inYard());
    }

    @PostMapping("/pre-notifications/{id}/qr")
    public ApiResponse<QrCodeService.QrResult> issueQr(@PathVariable UUID id) {
        return ApiResponse.ok(qrCodeService.issue(id));
    }

    public record RegistrationRequest(
            @NotNull UUID partnerAId,
            @NotBlank String plateNumber,
            @NotNull Instant plannedArrival,
            @NotBlank String driverName,
            String driverPhone,
            String carrierCompany,
            String vehicleType,
            List<@NotNull UUID> serviceIds
    ) {
    }
}
