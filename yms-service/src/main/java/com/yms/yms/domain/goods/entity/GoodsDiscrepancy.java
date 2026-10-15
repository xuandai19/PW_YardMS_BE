package com.yms.yms.domain.goods.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "goods_discrepancies")
@Getter
@Setter
@NoArgsConstructor
public class GoodsDiscrepancy {
    @Id
    private UUID id;

    @Column(name = "goods_id", nullable = false)
    private UUID goodsId;

    @Column(name = "expected_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedQuantity;

    @Column(name = "scanned_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal scannedQuantity;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean resolved;
}
