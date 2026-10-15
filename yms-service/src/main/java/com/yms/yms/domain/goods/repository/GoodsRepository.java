package com.yms.yms.domain.goods.repository;

import com.yms.yms.domain.goods.entity.Goods;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoodsRepository extends JpaRepository<Goods, UUID> {
    List<Goods> findByTripIdOrderByItemCode(UUID tripId);
}
