package com.yms.yms.domain.goods.repository;

import com.yms.yms.domain.goods.entity.GoodsDiscrepancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoodsDiscrepancyRepository extends JpaRepository<GoodsDiscrepancy, UUID> {
    List<GoodsDiscrepancy> findTop100ByOrderByCreatedAtDesc();
    List<GoodsDiscrepancy> findByGoodsIdAndResolvedFalse(UUID goodsId);
}
