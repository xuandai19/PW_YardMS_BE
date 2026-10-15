package com.yms.yms.domain.contract.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.contract.entity.Contract;
import com.yms.yms.domain.contract.repository.ContractRepository;
import com.yms.yms.domain.partner.repository.PartnerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final PartnerRepository partnerRepository;

    public ContractService(ContractRepository contractRepository, PartnerRepository partnerRepository) {
        this.contractRepository = contractRepository;
        this.partnerRepository = partnerRepository;
    }

    public List<Contract> findAll() {
        return contractRepository.findAll();
    }

    public Contract findById(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Contract not found", HttpStatus.NOT_FOUND));
    }

    public List<Contract> findExpiring(int days) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(days);
        return contractRepository.findByEndDateBetween(from, to);
    }

    @Transactional
    public Contract create(Contract contract) {
        validate(contract);
        if (contractRepository.existsByContractNo(contract.getContractNo())) {
            throw new BusinessException("Contract number already exists", HttpStatus.CONFLICT);
        }
        return contractRepository.save(contract);
    }

    @Transactional
    public Contract update(UUID id, Contract incoming) {
        Contract existing = findById(id);
        incoming.setId(id);
        incoming.setContractNo(existing.getContractNo());
        validate(incoming);
        existing.setContractType(incoming.getContractType());
        existing.setPartnerAId(incoming.getPartnerAId());
        existing.setPartnerCId(incoming.getPartnerCId());
        existing.setStartDate(incoming.getStartDate());
        existing.setEndDate(incoming.getEndDate());
        existing.setContractValue(incoming.getContractValue());
        existing.setFileUrl(incoming.getFileUrl());
        existing.setStatus(incoming.getStatus());
        return contractRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!contractRepository.existsById(id)) {
            throw new BusinessException("Contract not found", HttpStatus.NOT_FOUND);
        }
        contractRepository.deleteById(id);
    }

    @Transactional
    public Contract attachFile(UUID id, String fileUrl) {
        Contract contract = findById(id);
        contract.setFileUrl(fileUrl);
        return contractRepository.save(contract);
    }

    @Transactional
    public int expireContracts() {
        List<Contract> active = contractRepository.findByStatus("ACTIVE");
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Contract c : active) {
            if (c.getEndDate().isBefore(today)) {
                c.setStatus("EXPIRED");
                contractRepository.save(c);
                count++;
            }
        }
        return count;
    }

    private void validate(Contract contract) {
        if (contract.getEndDate().isBefore(contract.getStartDate())) {
            throw new BusinessException("endDate must be >= startDate", HttpStatus.BAD_REQUEST);
        }
        if (!partnerRepository.existsById(contract.getPartnerAId())) {
            throw new BusinessException("Partner A not found", HttpStatus.BAD_REQUEST);
        }
        if ("A-C".equals(contract.getContractType())) {
            if (contract.getPartnerCId() == null) {
                throw new BusinessException("A-C contract requires Partner C", HttpStatus.BAD_REQUEST);
            }
            if (!partnerRepository.existsById(contract.getPartnerCId())) {
                throw new BusinessException("Partner C not found", HttpStatus.BAD_REQUEST);
            }
        } else if ("A-B".equals(contract.getContractType())) {
            if (contract.getPartnerCId() != null) {
                throw new BusinessException("A-B contract must not have Partner C", HttpStatus.BAD_REQUEST);
            }
        } else {
            throw new BusinessException("contractType must be A-B or A-C", HttpStatus.BAD_REQUEST);
        }
    }
}
