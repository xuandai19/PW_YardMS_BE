package com.yms.yms.domain.contract.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.contract.entity.Contract;
import com.yms.yms.domain.contract.service.ContractService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public ApiResponse<List<Contract>> list() {
        return ApiResponse.ok(contractService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Contract> get(@PathVariable UUID id) {
        return ApiResponse.ok(contractService.findById(id));
    }

    @GetMapping("/expiring")
    public ApiResponse<List<Contract>> expiring(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(contractService.findExpiring(days));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Contract> create(@Valid @RequestBody CreateContractRequest request) {
        Contract contract = new Contract();
        contract.setContractNo(request.contractNo());
        contract.setContractType(request.contractType());
        contract.setPartnerAId(request.partnerAId());
        contract.setPartnerCId(request.partnerCId());
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setContractValue(request.contractValue());
        contract.setFileUrl(request.fileUrl());
        contract.setStatus(request.status() != null ? request.status() : "ACTIVE");
        return ApiResponse.ok(contractService.create(contract));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Contract> update(@PathVariable UUID id, @Valid @RequestBody CreateContractRequest request) {
        Contract contract = new Contract();
        contract.setContractType(request.contractType());
        contract.setPartnerAId(request.partnerAId());
        contract.setPartnerCId(request.partnerCId());
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setContractValue(request.contractValue());
        contract.setFileUrl(request.fileUrl());
        contract.setStatus(request.status() != null ? request.status() : "ACTIVE");
        return ApiResponse.ok(contractService.update(id, contract));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        contractService.delete(id);
        return ApiResponse.ok(null);
    }

    public record CreateContractRequest(
            @NotBlank String contractNo,
            @NotBlank @Pattern(regexp = "A-B|A-C", message = "contractType phải là A-B hoặc A-C") String contractType,
            @NotNull UUID partnerAId,
            UUID partnerCId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull BigDecimal contractValue,
            String fileUrl,
            String status
    ) {
    }
}
