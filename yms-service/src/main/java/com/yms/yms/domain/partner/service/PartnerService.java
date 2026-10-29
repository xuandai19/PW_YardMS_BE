package com.yms.yms.domain.partner.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.partner.entity.Partner;
import com.yms.yms.domain.partner.repository.PartnerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;

    public PartnerService(PartnerRepository partnerRepository) {
        this.partnerRepository = partnerRepository;
    }

    public List<Partner> findAll() {
        return partnerRepository.findAll();
    }

    public Partner findById(UUID id) {
        return partnerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Partner not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Partner create(Partner partner) {
        if (partnerRepository.existsByCode(partner.getCode())) {
            throw new BusinessException("Mã đối tác đã tồn tại. Hãy chọn mã khác.", HttpStatus.CONFLICT);
        }
        if (partnerRepository.existsByNameIgnoreCase(partner.getName().trim())) {
            throw new BusinessException("Tên đối tác đã tồn tại. Hãy kiểm tra hoặc chọn tên khác.", HttpStatus.CONFLICT);
        }
        if (!"A".equals(partner.getPartnerType()) && !"C".equals(partner.getPartnerType())) {
            throw new BusinessException("partnerType must be A or C", HttpStatus.BAD_REQUEST);
        }
        partner.setCode(partner.getCode().trim());
        partner.setName(partner.getName().trim());
        return partnerRepository.save(partner);
    }

    @Transactional
    public Partner update(UUID id, Partner incoming) {
        Partner existing = findById(id);
        if (incoming.getName() != null && partnerRepository.existsByNameIgnoreCaseAndIdNot(incoming.getName().trim(), id)) {
            throw new BusinessException("Tên đối tác đã được sử dụng bởi đối tác khác.", HttpStatus.CONFLICT);
        }
        existing.setName(incoming.getName());
        existing.setTaxCode(incoming.getTaxCode());
        existing.setContactPerson(incoming.getContactPerson());
        existing.setContactPhone(incoming.getContactPhone());
        existing.setContactEmail(incoming.getContactEmail());
        existing.setAddress(incoming.getAddress());
        existing.setCountryCode(incoming.getCountryCode());
        existing.setActive(incoming.isActive());
        return partnerRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!partnerRepository.existsById(id)) {
            throw new BusinessException("Partner not found", HttpStatus.NOT_FOUND);
        }
        partnerRepository.deleteById(id);
    }

    @Transactional
    public Partner linkRelationship(UUID partnerAId, UUID partnerCId) {
        Partner a = findById(partnerAId);
        Partner c = findById(partnerCId);
        if (!"A".equals(a.getPartnerType()) || !"C".equals(c.getPartnerType())) {
            throw new BusinessException("Relationship requires Partner A and Partner C", HttpStatus.BAD_REQUEST);
        }
        if (a.getId().equals(c.getId())) {
            throw new BusinessException("Cannot link partner to itself", HttpStatus.BAD_REQUEST);
        }
        Set<Partner> related = new HashSet<>(a.getRelatedCPartners());
        related.add(c);
        a.setRelatedCPartners(related);
        return partnerRepository.save(a);
    }
}
