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
import com.yms.yms.domain.yard.entity.Yard;
import com.yms.yms.domain.yard.repository.YardRepository;
import com.yms.yms.domain.service.service.ServiceDispatchService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final YardRepository yardRepository;

    @Autowired
    public VehicleRegistrationService(VehicleRepository vehicleRepository,
                                      PreNotificationRepository notificationRepository,
                                      TripRepository tripRepository,
                                      PartnerRepository partnerRepository,
                                      ServiceDispatchService dispatchService,
                                      Clock clock, YardRepository yardRepository) {
        this.vehicleRepository = vehicleRepository;
        this.notificationRepository = notificationRepository;
        this.tripRepository = tripRepository;
        this.partnerRepository = partnerRepository;
        this.dispatchService = dispatchService;
        this.clock = clock;
        this.yardRepository = yardRepository;
    }

    public VehicleRegistrationService(VehicleRepository vehicleRepository,
                                      PreNotificationRepository notificationRepository,
                                      TripRepository tripRepository,
                                      PartnerRepository partnerRepository,
                                      ServiceDispatchService dispatchService,
                                      Clock clock) {
        this(vehicleRepository, notificationRepository, tripRepository, partnerRepository, dispatchService, clock, null);
    }

    @Transactional
    public PreNotification register(RegistrationRequest request) {
        Instant now = clock.instant();
        if (request.plannedArrival().isBefore(now.plus(MINIMUM_NOTICE))) {
            throw new BusinessException("Thời gian đến phải cách thời điểm đăng ký ít nhất 4 giờ",
                    HttpStatus.BAD_REQUEST);
        }
        String plate = normalizePlate(request.plateNumber());
        if (request.driverName() == null || request.driverName().isBlank() || request.driverName().trim().length() > 150) {
            throw new BusinessException("Tên tài xế không được để trống và tối đa 150 ký tự", HttpStatus.BAD_REQUEST);
        }
        if (request.driverPhone() != null && !request.driverPhone().isBlank()
                && !request.driverPhone().matches("[+0-9 ().-]{7,30}")) {
            throw new BusinessException("Số điện thoại phải có 7-30 ký tự và chỉ gồm số, dấu +, khoảng trắng, ngoặc, chấm hoặc gạch ngang",
                    HttpStatus.BAD_REQUEST);
        }
        if ((request.originLatitude() == null) != (request.originLongitude() == null)) {
            throw new BusinessException("Cần cung cấp đồng thời vĩ độ và kinh độ của điểm xuất phát", HttpStatus.BAD_REQUEST);
        }
        LocalDate arrivalDate = request.plannedArrival().atZone(BUSINESS_ZONE).toLocalDate();
        if (notificationRepository.existsByPlateNumberAndArrivalDate(plate, arrivalDate)) {
            throw new BusinessException("Biển số này đã được đăng ký trong ngày đến đã chọn",
                    HttpStatus.CONFLICT);
        }
        Partner partner = partnerRepository.findById(request.partnerAId())
                .filter(p -> p.isActive() && "A".equalsIgnoreCase(p.getPartnerType()))
                .orElseThrow(() -> new BusinessException("Không tìm thấy đối tác A đang hoạt động", HttpStatus.NOT_FOUND));

        Vehicle vehicle = vehicleRepository.findFirstByPlateNumberIgnoreCase(plate).orElseGet(Vehicle::new);
        if (vehicle.getId() != null && !vehicle.getPartnerAId().equals(partner.getId())) {
            throw new BusinessException("Biển số xe đã được đăng ký cho một đối tác A khác",
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
        trip.setTripCode("TRIP-" + arrivalDate.toString().replace("-", "") + "-" + plate.replaceAll("[^A-Z0-9]", ""));
        trip.setTripName("Chuyến " + plate + " - " + arrivalDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        trip.setOrigin(request.startAddress().trim());
        trip.setOriginLatitude(request.originLatitude() == null ? null : BigDecimal.valueOf(request.originLatitude()));
        trip.setOriginLongitude(request.originLongitude() == null ? null : BigDecimal.valueOf(request.originLongitude()));
        Yard targetYard = request.targetYardId() == null || yardRepository == null ? null : yardRepository.findById(request.targetYardId())
                .filter(yard -> "ACTIVE".equals(yard.getStatus()))
                .orElseThrow(() -> new BusinessException("Bãi xe đích không tồn tại hoặc đang ngừng hoạt động", HttpStatus.BAD_REQUEST));
        trip.setDestination(targetYard == null ? "Yard" : targetYard.getName());
        if (targetYard != null) {
            trip.setDestinationYardId(targetYard.getId());
            trip.setDestinationLatitude(targetYard.getLatitude());
            trip.setDestinationLongitude(targetYard.getLongitude());
        }
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
            throw new BusinessException("Biển số này đã được đăng ký trong ngày đến đã chọn",
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
            throw new BusinessException("Biển số xe không hợp lệ (tối đa 30 ký tự)", HttpStatus.BAD_REQUEST);
        }
        if (!normalized.matches("[A-Z0-9.-]+")) {
            throw new BusinessException("Biển số chỉ được gồm chữ cái, chữ số, dấu chấm hoặc dấu gạch ngang",
                    HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    public record RegistrationRequest(UUID partnerAId, String plateNumber, Instant plannedArrival,
                                     String driverName, String driverPhone, String carrierCompany,
                                     String vehicleType, String startAddress, Double originLatitude,
                                     Double originLongitude, UUID targetYardId, List<UUID> serviceIds) {
        public RegistrationRequest(UUID partnerAId, String plateNumber, Instant plannedArrival,
                                   String driverName, String driverPhone, String carrierCompany,
                                   String vehicleType, String startAddress, Double originLatitude,
                                   Double originLongitude, List<UUID> serviceIds) {
            this(partnerAId, plateNumber, plannedArrival, driverName, driverPhone, carrierCompany,
                    vehicleType, startAddress, originLatitude, originLongitude, null, serviceIds);
        }
    }
}
