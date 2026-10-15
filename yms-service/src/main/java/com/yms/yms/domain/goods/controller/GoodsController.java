package com.yms.yms.domain.goods.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.goods.entity.Goods;
import com.yms.yms.domain.goods.entity.GoodsDiscrepancy;
import com.yms.yms.domain.goods.service.GoodsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goods")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class GoodsController {
    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @PostMapping("/batch")
    public ApiResponse<List<GoodsService.GoodsQr>> createBatch(@Valid @RequestBody BatchRequest request) {
        return ApiResponse.ok(goodsService.createBatch(request.tripId(), request.items().stream()
                .map(item -> new GoodsService.GoodsInput(item.itemCode(), item.description(),
                        item.quantity(), item.unit())).toList()));
    }

    @GetMapping
    public ApiResponse<List<Goods>> byTrip(@RequestParam UUID tripId) {
        return ApiResponse.ok(goodsService.byTrip(tripId));
    }

    @GetMapping("/{goodsId}/qr")
    public ApiResponse<GoodsService.GoodsQr> qr(@PathVariable UUID goodsId) {
        return ApiResponse.ok(goodsService.qr(goodsId));
    }

    @PostMapping("/{goodsId}/reconcile")
    public ApiResponse<GoodsService.ScanResult> reconcile(@PathVariable UUID goodsId,
            @Valid @RequestBody ReconcileRequest request) {
        return ApiResponse.ok(goodsService.reconcile(goodsId, request.qrValue(), request.scannedQuantity()));
    }

    @GetMapping("/discrepancies")
    public ApiResponse<List<GoodsDiscrepancy>> discrepancies() {
        return ApiResponse.ok(goodsService.discrepancies());
    }

    public record BatchRequest(@NotNull UUID tripId, @NotEmpty List<@Valid GoodsItem> items) {
    }

    public record GoodsItem(@NotBlank String itemCode, String description,
            @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity, @NotBlank String unit) {
    }

    public record ReconcileRequest(@NotBlank String qrValue,
            @NotNull @DecimalMin("0") BigDecimal scannedQuantity) {
    }
}
