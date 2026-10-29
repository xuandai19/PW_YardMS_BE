package com.yms.yms.domain.service.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.service.entity.ServiceEntity;
import com.yms.yms.domain.service.entity.ServicePriceHistory;
import com.yms.yms.domain.service.repository.ServicePriceHistoryRepository;
import com.yms.yms.domain.service.repository.ServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;
    private final ServicePriceHistoryRepository priceHistoryRepository;

    public ServiceCatalogService(ServiceRepository serviceRepository,
                                 ServicePriceHistoryRepository priceHistoryRepository) {
        this.serviceRepository = serviceRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public List<ServiceEntity> findAll() {
        return serviceRepository.findAll();
    }

    public List<ServiceEntity> findActive() {
        return serviceRepository.findByActiveTrue();
    }

    public ServiceEntity findById(UUID id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Service not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public ServiceEntity create(ServiceEntity entity) {
        if (serviceRepository.existsByCode(entity.getCode())) {
            throw new BusinessException("Mã dịch vụ đã tồn tại. Hãy chọn mã khác.", HttpStatus.CONFLICT);
        }
        if (serviceRepository.existsByNameIgnoreCase(entity.getName().trim())) {
            throw new BusinessException("Tên dịch vụ đã tồn tại. Hãy chọn tên khác.", HttpStatus.CONFLICT);
        }
        entity.setName(entity.getName().trim());
        entity.setCode(entity.getCode().trim());
        ServiceEntity saved = serviceRepository.save(entity);
        ServicePriceHistory history = new ServicePriceHistory();
        history.setServiceId(saved.getId());
        history.setPrice(saved.getHourlyRate());
        history.setValidFrom(Instant.now());
        priceHistoryRepository.save(history);
        return saved;
    }

    @Transactional
    public ServiceEntity update(UUID id, ServiceEntity incoming, UUID changedBy) {
        ServiceEntity existing = findById(id);
        if (serviceRepository.existsByNameIgnoreCaseAndIdNot(incoming.getName().trim(), id)) {
            throw new BusinessException("Tên dịch vụ đã được sử dụng bởi dịch vụ khác.", HttpStatus.CONFLICT);
        }
        BigDecimal oldRate = existing.getHourlyRate();
        existing.setName(incoming.getName().trim());
        existing.setServiceType(incoming.getServiceType());
        existing.setUnit(incoming.getUnit());
        existing.setActive(incoming.isActive());
        existing.setExpectedDurationMinutes(incoming.getExpectedDurationMinutes());
        if (incoming.getHourlyRate() != null && oldRate.compareTo(incoming.getHourlyRate()) != 0) {
            // close previous history row
            priceHistoryRepository.findByServiceIdOrderByValidFromDesc(id).stream()
                    .filter(h -> h.getValidTo() == null)
                    .findFirst()
                    .ifPresent(open -> {
                        open.setValidTo(Instant.now());
                        priceHistoryRepository.save(open);
                    });
            existing.setHourlyRate(incoming.getHourlyRate());
            ServicePriceHistory history = new ServicePriceHistory();
            history.setServiceId(id);
            history.setPrice(incoming.getHourlyRate());
            history.setValidFrom(Instant.now());
            history.setChangedBy(changedBy);
            priceHistoryRepository.save(history);
        }
        return serviceRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!serviceRepository.existsById(id)) {
            throw new BusinessException("Service not found", HttpStatus.NOT_FOUND);
        }
        serviceRepository.deleteById(id);
    }

    public List<ServicePriceHistory> priceHistory(UUID serviceId) {
        findById(serviceId);
        return priceHistoryRepository.findByServiceIdOrderByValidFromDesc(serviceId);
    }
}
