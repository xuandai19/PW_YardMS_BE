package com.yms.yms.domain.contract.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.contract.entity.Contract;
import com.yms.yms.domain.contract.service.ContractService;
import com.yms.yms.infrastructure.storage.ContractStorageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
public class ContractUploadController {

    private final ContractStorageService storageService;
    private final ContractService contractService;

    public ContractUploadController(ContractStorageService storageService, ContractService contractService) {
        this.storageService = storageService;
        this.contractService = contractService;
    }

    @PostMapping("/{id}/upload")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Contract> upload(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        String url = storageService.store(id, file);
        return ApiResponse.ok(contractService.attachFile(id, url));
    }
}
