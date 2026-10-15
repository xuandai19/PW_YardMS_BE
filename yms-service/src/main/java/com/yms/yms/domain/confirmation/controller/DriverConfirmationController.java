package com.yms.yms.domain.confirmation.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.confirmation.entity.DriverConfirmation;
import com.yms.yms.domain.confirmation.entity.DriverConfirmationAlert;
import com.yms.yms.domain.confirmation.service.DriverConfirmationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/confirmations")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class DriverConfirmationController {
    private final DriverConfirmationService service;

    public DriverConfirmationController(DriverConfirmationService service) {
        this.service = service;
    }

    @PostMapping("/trips/{tripId}")
    public ApiResponse<DriverConfirmation> request(@PathVariable UUID tripId,
            @Valid @RequestBody ConfirmationRequest request) {
        return ApiResponse.ok(service.request(tripId, request.confirmationType()));
    }

    @GetMapping
    public ApiResponse<List<DriverConfirmation>> forTrip(@RequestParam UUID tripId) {
        return ApiResponse.ok(service.forTrip(tripId));
    }

    @PostMapping("/{confirmationId}/confirm")
    public ApiResponse<DriverConfirmation> confirm(@PathVariable UUID confirmationId) {
        return ApiResponse.ok(service.confirm(confirmationId));
    }

    @PostMapping(value = "/{confirmationId}/signature", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DriverConfirmation> signature(@PathVariable UUID confirmationId,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(service.storeSignature(confirmationId, file));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<DriverConfirmationAlert>> alerts() {
        return ApiResponse.ok(service.alerts());
    }

    public record ConfirmationRequest(@NotBlank String confirmationType) {
    }
}
