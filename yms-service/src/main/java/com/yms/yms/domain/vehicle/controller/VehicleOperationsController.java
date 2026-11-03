package com.yms.yms.domain.vehicle.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.vehicle.entity.PreNotification;
import com.yms.yms.domain.vehicle.service.QrCodeService;
import com.yms.yms.domain.vehicle.service.VehicleRegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
                request.driverPhone(), request.carrierCompany(), request.vehicleType(), request.startAddress(),
                request.originLatitude(), request.originLongitude(), request.targetYardId(), request.serviceIds())));
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
    public ApiResponse<QrCodeService.QrResult> issueQr(@PathVariable UUID id,
                                                       @RequestParam(required = false) String appUrl) {
        return ApiResponse.ok(qrCodeService.issue(id, appUrl));
    }

    public record RegistrationRequest(
            @NotNull UUID partnerAId,
            @NotBlank String plateNumber,
            @NotNull Instant plannedArrival,
            @NotBlank String driverName,
            String driverPhone,
            String carrierCompany,
            String vehicleType,
            @NotBlank @Size(max = 255) String startAddress,
            @DecimalMin("-90.0") @DecimalMax("90.0") Double originLatitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") Double originLongitude,
            @NotNull UUID targetYardId,
            List<@NotNull UUID> serviceIds
    ) {
    }
}
