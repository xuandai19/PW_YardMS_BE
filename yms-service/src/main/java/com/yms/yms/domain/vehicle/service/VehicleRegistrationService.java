package com.yms.yms.domain.vehicle.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.partner.entity.Partner;
import com.yms.yms.domain.partner.repository.PartnerRepository;
import com.yms.yms.domain.vehicle.entity.PreNotification;
import com.yms.yms.domain.vehicle.entity.Trip;
import com.yms.yms.domain.vehicle.entity.Vehicle;
import com.yms.yms.domain.vehicle.repository.PreNotificationRepository;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import com.yms.yms.domain.vehicle.repository.VehicleRepository;
import com.yms.yms.domain.service.service.ServiceDispatchService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class VehicleRegistrationService {

    private static final Duration MINIMUM_NOTICE = Duration.ofHours(4);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final VehicleRepository vehicleRepository;
    private final PreNotificationRepository notificationRepository;
    private final TripRepository tripRepository;
    private final PartnerRepository partnerRepository;
    private final ServiceDispatchService dispatchService;
    private final Clock clock;

    public VehicleRegistrationService(VehicleRepository vehicleRepository,
                                      PreNotificationRepository notificationRepository,
                                      TripRepository tripRepository,
                                      PartnerRepository partnerRepository,
                                      ServiceDispatchService dispatchService,
                                      Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.notificationRepository = notificationRepository;
        this.tripRepository = tripRepository;
        this.partnerRepository = partnerRepository;
        this.dispatchService = dispatchService;
        this.clock = clock;
    }

    @Transactional
    public PreNotification register(RegistrationRequest request) {
        Instant now = clock.instant();
        if (request.plannedArrival().isBefore(now.plus(MINIMUM_NOTICE))) {
            throw new BusinessException("Vehicle arrival must be registered at least 4 hours in advance",
                    HttpStatus.BAD_REQUEST);
        }
        String plate = normalizePlate(request.plateNumber());
        LocalDate arrivalDate = request.plannedArrival().atZone(BUSINESS_ZONE).toLocalDate();
        if (notificationRepository.existsByPlateNumberAndArrivalDate(plate, arrivalDate)) {
            throw new BusinessException("This plate already has a registration for the arrival date",
                    HttpStatus.CONFLICT);
        }
        Partner partner = partnerRepository.findById(request.partnerAId())
                .filter(p -> p.isActive() && "A".equalsIgnoreCase(p.getPartnerType()))
                .orElseThrow(() -> new BusinessException("Active Partner A not found", HttpStatus.NOT_FOUND));

        Vehicle vehicle = vehicleRepository.findFirstByPlateNumberIgnoreCase(plate).orElseGet(Vehicle::new);
        if (vehicle.getId() != null && !vehicle.getPartnerAId().equals(partner.getId())) {
            throw new BusinessException("Vehicle plate is already registered to another Partner A",
                    HttpStatus.CONFLICT);
        }
        vehicle.setPlateNumber(plate);
        vehicle.setPartnerAId(partner.getId());
        vehicle.setDriverName(request.driverName().trim());
        vehicle.setCarrierCompany(request.carrierCompany());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setStatus("REGISTERED");
        vehicle = vehicleRepository.save(vehicle);

        Trip trip = new Trip();
        trip.setVehicleId(vehicle.getId());
        trip.setOrigin("Pre-notified arrival");
        trip.setDestination("Yard");
        trip.setPlannedAt(request.plannedArrival());
        trip.setStatus("PLANNED");
        trip = tripRepository.save(trip);

        PreNotification notification = new PreNotification();
        notification.setVehicleId(vehicle.getId());
        notification.setPartnerAId(partner.getId());
        notification.setTripId(trip.getId());
        notification.setPlateNumber(plate);
        notification.setArrivalDate(arrivalDate);
        notification.setPlannedArrival(request.plannedArrival());
        notification.setDriverName(request.driverName().trim());
        notification.setDriverPhone(request.driverPhone());
        notification.setCarrierCompany(request.carrierCompany());
        notification.setVehicleType(request.vehicleType());
        notification.setStatus("REGISTERED");
        PreNotification saved;
        try {
            saved = notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("This plate already has a registration for the arrival date",
                    HttpStatus.CONFLICT);
        }
        if (request.serviceIds() != null && !request.serviceIds().isEmpty()) {
            dispatchService.preassign(saved.getTripId(), saved.getVehicleId(), request.serviceIds());
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PreNotification> waiting() {
        return notificationRepository.findByStatusOrderByPlannedArrivalAsc("REGISTERED");
    }

    @Transactional(readOnly = true)
    public List<PreNotification> inYard() {
        return notificationRepository.findByStatusOrderByPlannedArrivalAsc("CHECKED_IN");
    }

    private String normalizePlate(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (normalized.isBlank() || normalized.length() > 30) {
            throw new BusinessException("Plate number is invalid", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    public record RegistrationRequest(UUID partnerAId, String plateNumber, Instant plannedArrival,
                                     String driverName, String driverPhone, String carrierCompany,
                                     String vehicleType, List<UUID> serviceIds) {
    }
}
