package com.yms.yms.domain.yard.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.vehicle.entity.Vehicle;
import com.yms.yms.domain.vehicle.repository.VehicleRepository;
import com.yms.yms.domain.yard.entity.Yard;
import com.yms.yms.domain.yard.entity.YardAlert;
import com.yms.yms.domain.yard.entity.YardVehicleAssignment;
import com.yms.yms.domain.yard.repository.YardAlertRepository;
import com.yms.yms.domain.yard.repository.YardRepository;
import com.yms.yms.domain.yard.repository.YardVehicleAssignmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class YardManagementService {

    private static final String ACTIVE = "ACTIVE";
    private final YardRepository yardRepository;
    private final YardVehicleAssignmentRepository assignmentRepository;
    private final YardAlertRepository alertRepository;
    private final VehicleRepository vehicleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public YardManagementService(YardRepository yardRepository,
                                 YardVehicleAssignmentRepository assignmentRepository,
                                 YardAlertRepository alertRepository,
                                 VehicleRepository vehicleRepository,
                                 SimpMessagingTemplate messagingTemplate,
                                 Clock clock) {
        this.yardRepository = yardRepository;
        this.assignmentRepository = assignmentRepository;
        this.alertRepository = alertRepository;
        this.vehicleRepository = vehicleRepository;
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Yard> list() {
        return yardRepository.findAll();
    }

    @Transactional
    public Yard create(Yard yard) {
        if (yardRepository.existsByCode(yard.getCode())) {
            throw new BusinessException("Yard code already exists", HttpStatus.CONFLICT);
        }
        validateYard(yard);
        Yard saved = yardRepository.save(yard);
        publishInventory();
        return saved;
    }

    @Transactional
    public Yard update(UUID id, Yard incoming) {
        Yard existing = getYard(id);
        validateYard(incoming);
        long occupancy = assignmentRepository.countCurrentByYardId(id);
        if (incoming.getCapacity() < occupancy) {
            throw new BusinessException("Yard capacity cannot be lower than current occupancy",
                    HttpStatus.CONFLICT);
        }
        if ("INACTIVE".equals(incoming.getStatus()) && occupancy > 0) {
            throw new BusinessException("A yard with vehicles cannot be deactivated", HttpStatus.CONFLICT);
        }
        existing.setName(incoming.getName());
        existing.setAddress(incoming.getAddress());
        existing.setLatitude(incoming.getLatitude());
        existing.setLongitude(incoming.getLongitude());
        existing.setCapacity(incoming.getCapacity());
        existing.setStatus(incoming.getStatus());
        Yard saved = yardRepository.save(existing);
        refreshAlerts();
        publishInventory();
        return saved;
    }

    @Transactional
    public void deactivate(UUID id) {
        Yard yard = getYard(id);
        if (assignmentRepository.countCurrentByYardId(id) > 0) {
            throw new BusinessException("A yard with vehicles cannot be deactivated", HttpStatus.CONFLICT);
        }
        yard.setStatus("INACTIVE");
        yardRepository.save(yard);
        publishInventory();
    }

    @Transactional
    public YardVehicleAssignment assignVehicle(UUID yardId, UUID vehicleId, Instant assignedAt) {
        Yard yard = yardRepository.findByIdForUpdate(yardId)
                .orElseThrow(() -> new BusinessException("Yard not found", HttpStatus.NOT_FOUND));
        if (!ACTIVE.equals(yard.getStatus())) {
            throw new BusinessException("Vehicles cannot be assigned to an inactive yard", HttpStatus.CONFLICT);
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException("Vehicle not found", HttpStatus.NOT_FOUND));
        if (assignmentRepository.findByVehicleIdAndCheckOutAtIsNull(vehicleId).isPresent()) {
            throw new BusinessException("Vehicle is already assigned to a yard", HttpStatus.CONFLICT);
        }
        if (assignmentRepository.countCurrentByYardId(yardId) >= yard.getCapacity()) {
            throw new BusinessException("Yard is at full capacity", HttpStatus.CONFLICT);
        }

        YardVehicleAssignment assignment = new YardVehicleAssignment();
        assignment.setYardId(yardId);
        assignment.setVehicleId(vehicleId);
        assignment.setCheckInAt(assignedAt);
        assignment.setStatus("IN_YARD");
        vehicle.setStatus("IN_YARD");
        vehicleRepository.save(vehicle);
        YardVehicleAssignment saved = assignmentRepository.save(assignment);
        refreshAlerts();
        publishInventory();
        return saved;
    }

    @Transactional
    public void closeAssignment(UUID vehicleId, Instant checkedOutAt) {
        YardVehicleAssignment assignment = assignmentRepository.findByVehicleIdAndCheckOutAtIsNull(vehicleId)
                .orElseThrow(() -> new BusinessException("Vehicle is not currently assigned to a yard",
                        HttpStatus.CONFLICT));
        assignment.setCheckOutAt(checkedOutAt);
        assignment.setStatus("COMPLETED");
        assignmentRepository.save(assignment);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException("Vehicle not found", HttpStatus.NOT_FOUND));
        vehicle.setStatus("CHECKED_OUT");
        vehicleRepository.save(vehicle);
        refreshAlerts();
        publishInventory();
    }

    @Transactional(readOnly = true)
    public List<YardAlert> activeAlerts() {
        return alertRepository.findByResolvedAtIsNullOrderByCreatedAtDesc();
    }

    @Transactional
    public List<YardInventory> inventory() {
        refreshAlerts();
        List<YardInventory> result = yardRepository.findAll().stream()
                .map(this::inventoryFor)
                .toList();
        messagingTemplate.convertAndSend("/topic/yards/inventory", result);
        messagingTemplate.convertAndSend("/topic/yards/alerts", activeAlerts());
        return result;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${yms.yard.alert-check-ms:30000}")
    public void refreshAndPublishInventory() {
        refreshAlerts();
        publishInventory();
    }

    private void refreshAlerts() {
        Instant now = clock.instant();
        for (Yard yard : yardRepository.findAll()) {
            long occupancy = assignmentRepository.countCurrentByYardId(yard.getId());
            boolean overThreshold = isAtAlertThreshold(occupancy, yard.getCapacity());
            var active = alertRepository.findFirstByYardIdAndResolvedAtIsNullOrderByCreatedAtDesc(yard.getId());
            if (overThreshold && active.isEmpty()) {
                YardAlert alert = new YardAlert();
                alert.setYardId(yard.getId());
                alert.setOccupancy(Math.toIntExact(occupancy));
                alert.setCapacity(yard.getCapacity());
                alert.setMessage("Yard " + yard.getName() + " has reached "
                        + (occupancy * 100 / yard.getCapacity()) + "% capacity");
                alertRepository.save(alert);
            } else if (!overThreshold) {
                active.ifPresent(alert -> {
                    alert.setResolvedAt(now);
                    alertRepository.save(alert);
                });
            } else {
                active.ifPresent(alert -> {
                    alert.setOccupancy(Math.toIntExact(occupancy));
                    alert.setCapacity(yard.getCapacity());
                    alertRepository.save(alert);
                });
            }
        }
    }

    static boolean isAtAlertThreshold(long occupancy, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Yard capacity must be positive");
        }
        return occupancy * 100L >= capacity * 90L;
    }

    private void publishInventory() {
        List<YardInventory> result = yardRepository.findAll().stream().map(this::inventoryFor).toList();
        messagingTemplate.convertAndSend("/topic/yards/inventory", result);
        messagingTemplate.convertAndSend("/topic/yards/alerts", activeAlerts());
    }

    private YardInventory inventoryFor(Yard yard) {
        List<VehicleSummary> vehicles = assignmentRepository.findByYardIdAndCheckOutAtIsNull(yard.getId()).stream()
                .map(assignment -> vehicleRepository.findById(assignment.getVehicleId())
                        .orElseThrow(() -> new BusinessException("Assigned vehicle not found", HttpStatus.INTERNAL_SERVER_ERROR)))
                .map(vehicle -> new VehicleSummary(vehicle.getId(), vehicle.getPlateNumber(),
                        vehicle.getDriverName(), vehicle.getStatus()))
                .toList();
        int occupancy = vehicles.size();
        boolean alert = occupancy * 100L >= yard.getCapacity() * 90L;
        return new YardInventory(yard, occupancy, occupancy * 100 / yard.getCapacity(), alert, vehicles);
    }

    private Yard getYard(UUID id) {
        return yardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Yard not found", HttpStatus.NOT_FOUND));
    }

    private void validateYard(Yard yard) {
        if (yard.getCode() == null || yard.getCode().isBlank()
                || yard.getName() == null || yard.getName().isBlank()
                || yard.getAddress() == null || yard.getAddress().isBlank()
                || yard.getCapacity() <= 0) {
            throw new BusinessException("Yard code, name, address and positive capacity are required",
                    HttpStatus.BAD_REQUEST);
        }
        if (yard.getStatus() == null || yard.getStatus().isBlank()) {
            yard.setStatus(ACTIVE);
        } else if (!ACTIVE.equals(yard.getStatus()) && !"INACTIVE".equals(yard.getStatus())) {
            throw new BusinessException("Yard status must be ACTIVE or INACTIVE", HttpStatus.BAD_REQUEST);
        }
    }

    public record VehicleSummary(UUID id, String plateNumber, String driverName, String status) {
    }

    public record YardInventory(Yard yard, int occupancy, int occupancyPercent, boolean alert,
                                List<VehicleSummary> vehicles) {
    }
}
