package com.yms.yms.domain.service.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.service.entity.ServiceEntity;
import com.yms.yms.domain.service.entity.ServiceOrder;
import com.yms.yms.domain.service.repository.ServiceOrderRepository;
import com.yms.yms.domain.service.repository.ServiceRepository;
import com.yms.yms.domain.service.repository.YardServiceWorkerRepository;
import com.yms.yms.domain.finance.service.TripInvoiceService;
import com.yms.yms.domain.vehicle.entity.PreNotification;
import com.yms.yms.domain.vehicle.repository.PreNotificationRepository;
import com.yms.yms.domain.yard.entity.YardVehicleAssignment;
import com.yms.yms.domain.yard.repository.YardVehicleAssignmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ServiceDispatchService {

    private static final int MONEY_SCALE = 4;
    private static final BigDecimal MILLIS_PER_HOUR = BigDecimal.valueOf(3_600_000L);
    private final ServiceOrderRepository orderRepository;
    private final ServiceRepository serviceRepository;
    private final PreNotificationRepository notificationRepository;
    private final YardVehicleAssignmentRepository assignmentRepository;
    private final YardServiceWorkerRepository workerRepository;
    private final TripInvoiceService tripInvoiceService;
    private final Clock clock;

    public ServiceDispatchService(ServiceOrderRepository orderRepository,
                                  ServiceRepository serviceRepository,
                                  PreNotificationRepository notificationRepository,
                                  YardVehicleAssignmentRepository assignmentRepository,
                                  YardServiceWorkerRepository workerRepository,
                                  TripInvoiceService tripInvoiceService,
                                  Clock clock) {
        this.orderRepository = orderRepository;
        this.serviceRepository = serviceRepository;
        this.notificationRepository = notificationRepository;
        this.assignmentRepository = assignmentRepository;
        this.workerRepository = workerRepository;
        this.tripInvoiceService = tripInvoiceService;
        this.clock = clock;
    }

    @Transactional
    public List<ServiceOrder> assign(UUID preNotificationId, List<UUID> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new BusinessException("At least one service must be selected", HttpStatus.BAD_REQUEST);
        }
        PreNotification notification = notificationRepository.findById(preNotificationId)
                .orElseThrow(() -> new BusinessException("Pre-notification not found", HttpStatus.NOT_FOUND));
        if (!"CHECKED_IN".equals(notification.getStatus())) {
            throw new BusinessException("Services can only be assigned to a vehicle checked into a yard",
                    HttpStatus.CONFLICT);
        }
        YardVehicleAssignment assignment = assignmentRepository
                .findByVehicleIdAndCheckOutAtIsNull(notification.getVehicleId())
                .orElseThrow(() -> new BusinessException("Vehicle has no active yard assignment",
                        HttpStatus.CONFLICT));
        List<ServiceOrder> orders = new ArrayList<>();
        for (UUID serviceId : serviceIds.stream().distinct().toList()) {
            ServiceEntity service = serviceRepository.findById(serviceId)
                    .filter(ServiceEntity::isActive)
                    .orElseThrow(() -> new BusinessException("Active service not found: " + serviceId,
                            HttpStatus.NOT_FOUND));
            ServiceOrder existing = orderRepository.findByTripIdAndServiceId(notification.getTripId(), serviceId)
                    .orElse(null);
            if (existing != null) {
                existing.setYardId(assignment.getYardId());
                ServiceEntity existingService = serviceRepository.findById(serviceId).orElseThrow();
                if (existing.getExpectedEndAt() == null) {
                    existing.setExpectedEndAt(clock.instant().plus(Duration.ofMinutes(existingService.getExpectedDurationMinutes())));
                }
                orderRepository.save(existing);
                continue;
            }
            ServiceOrder order = new ServiceOrder();
            order.setTripId(notification.getTripId());
            order.setVehicleId(notification.getVehicleId());
            order.setYardId(assignment.getYardId());
            order.setServiceId(service.getId());
            order.setUnitPrice(service.getHourlyRate());
            order.setQuantity(BigDecimal.ZERO.setScale(MONEY_SCALE));
            order.setTotalAmount(BigDecimal.ZERO.setScale(MONEY_SCALE));
            order.setStatus("ASSIGNED");
            order.setExpectedEndAt(clock.instant().plus(Duration.ofMinutes(service.getExpectedDurationMinutes())));
            orders.add(orderRepository.save(order));
        }
        return orders;
    }

    @Transactional
    public void preassign(UUID tripId, UUID vehicleId, List<UUID> serviceIds) {
        for (UUID serviceId : serviceIds.stream().distinct().toList()) {
            ServiceEntity service = serviceRepository.findById(serviceId)
                    .filter(ServiceEntity::isActive)
                    .orElseThrow(() -> new BusinessException("Active service not found: " + serviceId,
                            HttpStatus.NOT_FOUND));
            ServiceOrder order = new ServiceOrder();
            order.setTripId(tripId);
            order.setVehicleId(vehicleId);
            order.setServiceId(service.getId());
            order.setUnitPrice(service.getHourlyRate());
            order.setQuantity(BigDecimal.ZERO.setScale(MONEY_SCALE));
            order.setTotalAmount(BigDecimal.ZERO.setScale(MONEY_SCALE));
            order.setStatus("SCHEDULED");
            order.setExpectedEndAt(clock.instant().plus(Duration.ofMinutes(service.getExpectedDurationMinutes())));
            orderRepository.save(order);
        }
    }

    @Transactional
    public ServiceOrder start(UUID orderId) {
        ServiceOrder order = findOrder(orderId);
        if (!"ASSIGNED".equals(order.getStatus())) {
            if (!"SCHEDULED".equals(order.getStatus())) {
                throw new BusinessException("Only assigned services can be started", HttpStatus.CONFLICT);
            }
        }
        YardVehicleAssignment assignment = assignmentRepository.findByVehicleIdAndCheckOutAtIsNull(order.getVehicleId())
                .orElseThrow(() -> new BusinessException("Vehicle must check into a yard before service starts",
                        HttpStatus.CONFLICT));
        order.setYardId(assignment.getYardId());
        order.setStartAt(clock.instant());
        ServiceEntity service = serviceRepository.findById(order.getServiceId())
                .orElseThrow(() -> new BusinessException("Service not found", HttpStatus.NOT_FOUND));
        order.setExpectedEndAt(order.getStartAt().plus(Duration.ofMinutes(service.getExpectedDurationMinutes())));
        order.setStatus("IN_PROGRESS");
        return orderRepository.save(order);
    }

    @Transactional
    public ServiceOrder end(UUID orderId) {
        ServiceOrder order = findOrder(orderId);
        if (!"IN_PROGRESS".equals(order.getStatus()) || order.getStartAt() == null) {
            throw new BusinessException("Only in-progress services can be ended", HttpStatus.CONFLICT);
        }
        Instant endAt = clock.instant();
        long durationMillis = Duration.between(order.getStartAt(), endAt).toMillis();
        if (durationMillis <= 0) {
            throw new BusinessException("Service end time must be after start time", HttpStatus.CONFLICT);
        }
        order.setEndAt(endAt);
        order.setQuantity(BigDecimal.valueOf(durationMillis)
                .divide(MILLIS_PER_HOUR, MONEY_SCALE, RoundingMode.HALF_UP));
        order.setTotalAmount(calculateCost(order.getUnitPrice(), durationMillis));
        order.setStatus("COMPLETED");
        ServiceOrder completed = orderRepository.save(order);
        tripInvoiceService.issueWhenTripServicesComplete(order.getTripId());
        return completed;
    }

    @Transactional(readOnly = true)
    public List<ServiceOrder> list(UUID vehicleId) {
        if (vehicleId != null) {
            return orderRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
        }
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public WorkerAvailability availability(UUID vehicleId, UUID serviceId) {
        YardVehicleAssignment assignment = assignmentRepository.findByVehicleIdAndCheckOutAtIsNull(vehicleId)
                .orElseThrow(() -> new BusinessException("Vehicle has no active yard assignment", HttpStatus.CONFLICT));
        UUID yardId = assignment.getYardId();
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("Service not found", HttpStatus.NOT_FOUND));
        List<com.yms.yms.domain.service.entity.YardServiceWorker> workers = workerRepository
                .findByYardIdAndServiceTypeAndActiveTrueOrderByWorkerName(yardId, service.getServiceType());
        long busy = orderRepository.countByYardIdAndServiceIdAndStatus(yardId, serviceId, "IN_PROGRESS");
        List<WorkerAvailabilityItem> items = workers.stream().map(worker ->
                new WorkerAvailabilityItem(worker.getId(), worker.getWorkerName(), itemsAvailableIndex(workers, worker, busy)))
                .toList();
        return new WorkerAvailability(yardId, serviceId, service.getName(), items.size(),
                Math.max(0, items.size() - (int) busy), items);
    }

    private boolean itemsAvailableIndex(List<com.yms.yms.domain.service.entity.YardServiceWorker> workers,
                                        com.yms.yms.domain.service.entity.YardServiceWorker worker, long busy) {
        return workers.indexOf(worker) >= busy;
    }

    private ServiceOrder findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Service order not found", HttpStatus.NOT_FOUND));
    }

    public record WorkerAvailability(UUID yardId, UUID serviceId, String serviceName, int workerCount,
                                     int availableCount, List<WorkerAvailabilityItem> workers) { }

    public record WorkerAvailabilityItem(UUID id, String name, boolean available) { }

    static BigDecimal calculateCost(BigDecimal hourlyRate, long durationMillis) {
        if (hourlyRate == null || hourlyRate.signum() < 0 || durationMillis < 0) {
            throw new IllegalArgumentException("Hourly rate and service duration must be non-negative");
        }
        return hourlyRate.multiply(BigDecimal.valueOf(durationMillis))
                .divide(MILLIS_PER_HOUR, MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
