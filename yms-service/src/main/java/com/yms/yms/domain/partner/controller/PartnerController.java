package com.yms.yms.domain.partner.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.partner.entity.Partner;
import com.yms.yms.domain.partner.service.PartnerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partners")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping
    public ApiResponse<List<Partner>> list() {
        return ApiResponse.ok(partnerService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Partner> get(@PathVariable UUID id) {
        return ApiResponse.ok(partnerService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Partner> create(@Valid @RequestBody CreatePartnerRequest request) {
        Partner partner = new Partner();
        partner.setCode(request.code());
        partner.setName(request.name());
        partner.setTaxCode(request.taxCode());
        partner.setContactPerson(request.contactPerson());
        partner.setContactPhone(request.contactPhone());
        partner.setContactEmail(request.contactEmail());
        partner.setAddress(request.address());
        partner.setCountryCode(request.countryCode());
        partner.setPartnerType(request.partnerType());
        partner.setActive(request.active() == null || request.active());
        return ApiResponse.ok(partnerService.create(partner));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Partner> update(@PathVariable UUID id, @RequestBody UpdatePartnerRequest request) {
        Partner incoming = new Partner();
        incoming.setName(request.name());
        incoming.setTaxCode(request.taxCode());
        incoming.setContactPerson(request.contactPerson());
        incoming.setContactPhone(request.contactPhone());
        incoming.setContactEmail(request.contactEmail());
        incoming.setAddress(request.address());
        incoming.setCountryCode(request.countryCode());
        incoming.setActive(request.active() == null || request.active());
        return ApiResponse.ok(partnerService.update(id, incoming));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        partnerService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{partnerAId}/relationships/{partnerCId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Partner> link(@PathVariable UUID partnerAId, @PathVariable UUID partnerCId) {
        return ApiResponse.ok(partnerService.linkRelationship(partnerAId, partnerCId));
    }

    public record CreatePartnerRequest(
            @NotBlank String code,
            @NotBlank String name,
            String taxCode,
            String contactPerson,
            String contactPhone,
            String contactEmail,
            String address,
            @NotBlank String countryCode,
            @NotBlank String partnerType,
            Boolean active
    ) {
    }

    public record UpdatePartnerRequest(
            String name,
            String taxCode,
            String contactPerson,
            String contactPhone,
            String contactEmail,
            String address,
            String countryCode,
            Boolean active
    ) {
    }
}
