package com.yms.yms.domain.yard.repository;

import com.yms.yms.domain.yard.entity.Yard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface YardRepository extends JpaRepository<Yard, UUID> {
    boolean existsByCode(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
    Optional<Yard> findByCode(String code);
    List<Yard> findByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select y from Yard y where y.id = :yardId")
    Optional<Yard> findByIdForUpdate(@Param("yardId") UUID yardId);
}
