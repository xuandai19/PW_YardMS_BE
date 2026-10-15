package com.yms.yms.domain.contract.repository;

import com.yms.yms.domain.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {
    Optional<Contract> findByContractNo(String contractNo);
    boolean existsByContractNo(String contractNo);
    List<Contract> findByEndDateBetween(LocalDate from, LocalDate to);
    List<Contract> findByStatus(String status);
    List<Contract> findByPartnerAId(UUID partnerAId);
    List<Contract> findByPartnerAIdAndPartnerCIdAndStatus(UUID partnerAId, UUID partnerCId, String status);
}
