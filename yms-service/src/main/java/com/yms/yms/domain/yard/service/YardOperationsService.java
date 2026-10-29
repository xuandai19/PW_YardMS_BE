package com.yms.yms.domain.yard.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.vehicle.entity.PreNotification;
import com.yms.yms.domain.vehicle.entity.Trip;
import com.yms.yms.domain.vehicle.entity.Vehicle;
import com.yms.yms.domain.vehicle.repository.PreNotificationRepository;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import com.yms.yms.domain.vehicle.repository.VehicleRepository;
import com.yms.yms.domain.vehicle.service.QrCodeService;
import com.yms.yms.domain.yard.entity.YardLog;
import com.yms.yms.domain.yard.repository.YardLogRepository;
import com.yms.yms.domain.yard.repository.YardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class YardOperationsService {

    private final QrCodeService qrCodeService;
    private final PreNotificationRepository notificationRepository;
    private final YardLogRepository yardLogRepository;
    private final YardRepository yardRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final YardManagementService yardManagementService;
    private final Clock clock;

    public YardOperationsService(QrCodeService qrCodeService,
                                 PreNotificationRepository notificationRepository,
                                 YardLogRepository yardLogRepository,
                                 YardRepository yardRepository,
                                 VehicleRepository vehicleRepository,
                                 TripRepository tripRepository,
                                 YardManagementService yardManagementService,
                                 Clock clock) {
        this.qrCodeService = qrCodeService;
        this.notificationRepository = notificationRepository;
        this.yardLogRepository = yardLogRepository;
        this.yardRepository = yardRepository;
        this.vehicleRepository = vehicleRepository;
        this.tripRepository = tripRepository;
        this.yardManagementService = yardManagementService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public VehicleCheckInInfo scan(String token) {
        PreNotification notification = qrCodeService.verify(token);
        if (!"REGISTERED".equals(notification.getStatus())) {
            throw new BusinessException("Vehicle is not awaiting check-in", HttpStatus.CONFLICT);
        }
        Vehicle vehicle = vehicleRepository.findById(notification.getVehicleId())
                .orElseThrow(() -> new BusinessException("Vehicle not found", HttpStatus.NOT_FOUND));
        return new VehicleCheckInInfo(notification.getId(), vehicle.getId(), vehicle.getPlateNumber(),
                notification.getDriverName(), notification.getPlannedArrival(), notification.getPartnerAId());
    }

    @Transactional(readOnly = true)
    public PublicVehicleQrInfo publicQrInfo(String token) {
        PreNotification notification = qrCodeService.verify(token);
        if (!"REGISTERED".equals(notification.getStatus())) {
            throw new BusinessException("QR chỉ hiển thị thông tin khi xe đang chờ vào bãi", HttpStatus.GONE);
        }
        Vehicle vehicle = vehicleRepository.findById(notification.getVehicleId())
                .orElseThrow(() -> new BusinessException("Vehicle not found", HttpStatus.NOT_FOUND));
        Trip trip = tripRepository.findById(notification.getTripId())
                .orElseThrow(() -> new BusinessException("Trip not found", HttpStatus.NOT_FOUND));
        return new PublicVehicleQrInfo(vehicle.getPlateNumber(), notification.getDriverName(),
                notification.getDriverPhone(), notification.getCarrierCompany(), notification.getVehicleType(),
                notification.getPlannedArrival(), trip.getDestination());
    }

    @Transactional
    public YardLog checkIn(String token, UUID yardId) {
        PreNotification notification = qrCodeService.verify(token);
        return checkIn(notification, yardId);
    }

    @Transactional
    public YardLog manualCheckIn(UUID preNotificationId, UUID yardId) {
        PreNotification notification = notificationRepository.findById(preNotificationId)
                .orElseThrow(() -> new BusinessException("Pre-notification not found", HttpStatus.NOT_FOUND));
        return checkIn(notification, yardId);
    }

    private YardLog checkIn(PreNotification notification, UUID yardId) {
        if (!"REGISTERED".equals(notification.getStatus())) {
            throw new BusinessException("Vehicle is not awaiting check-in", HttpStatus.CONFLICT);
        }
        if (yardLogRepository.existsByPreNotificationIdAndCheckOutAtIsNull(notification.getId())) {
            throw new BusinessException("Vehicle has already checked in", HttpStatus.CONFLICT);
        }
        Instant now = clock.instant();
        Vehicle vehicle = vehicleRepository.findById(notification.getVehicleId())
                .orElseThrow(() -> new BusinessException("Vehicle not found", HttpStatus.NOT_FOUND));
        yardManagementService.assignVehicle(yardId, vehicle.getId(), now);

        YardLog log = new YardLog();
        log.setPreNotificationId(notification.getId());
        log.setVehicleId(vehicle.getId());
        log.setYardId(yardId);
        log.setCheckInAt(now);
        YardLog saved = yardLogRepository.save(log);

        notification.setStatus("CHECKED_IN");
        notification.setQrToken(null);
        notificationRepository.save(notification);
        Trip trip = tripRepository.findById(notification.getTripId())
                .orElseThrow(() -> new BusinessException("Trip not found", HttpStatus.INTERNAL_SERVER_ERROR));
        trip.setArrivedAt(now);
        trip.setStatus("ARRIVED");
        tripRepository.save(trip);
        return saved;
    }

    @Transactional
    public CheckOutResult checkOut(UUID preNotificationId) {
        PreNotification notification = notificationRepository.findById(preNotificationId)
                .orElseThrow(() -> new BusinessException("Pre-notification not found", HttpStatus.NOT_FOUND));
        if (!"CHECKED_IN".equals(notification.getStatus())) {
            throw new BusinessException("Vehicle is not currently checked in", HttpStatus.CONFLICT);
        }
        YardLog log = yardLogRepository.findByPreNotificationIdAndCheckOutAtIsNull(preNotificationId)
                .orElseThrow(() -> new BusinessException("Open yard log not found", HttpStatus.CONFLICT));
        Instant now = clock.instant();
        if (now.isBefore(log.getCheckInAt())) {
            throw new BusinessException("Check-out time cannot be before check-in", HttpStatus.CONFLICT);
        }
        long durationSeconds = Duration.between(log.getCheckInAt(), now).getSeconds();
        log.setCheckOutAt(now);
        log.setDurationSeconds(durationSeconds);
        yardLogRepository.save(log);
        yardManagementService.closeAssignment(log.getVehicleId(), now);

        notification.setStatus("CHECKED_OUT");
        notificationRepository.save(notification);
        return new CheckOutResult(log, durationSeconds);
    }

    @Transactional(readOnly = true)
    public List<YardHistoryEntry> history() {
        return yardLogRepository.findAllByOrderByCheckInAtDesc().stream()
                .map(log -> {
                    PreNotification notification = notificationRepository.findById(log.getPreNotificationId())
                            .orElseThrow(() -> new BusinessException("Pre-notification not found", HttpStatus.NOT_FOUND));
                    var yard = yardRepository.findById(log.getYardId())
                            .orElseThrow(() -> new BusinessException("Yard not found", HttpStatus.NOT_FOUND));
                    return new YardHistoryEntry(log.getId(), log.getPreNotificationId(), notification.getPlateNumber(),
                            notification.getDriverName(), yard.getId(), yard.getCode(), yard.getName(), log.getCheckInAt(),
                            log.getCheckOutAt(), log.getDurationSeconds());
                })
                .toList();
    }

    public record VehicleCheckInInfo(UUID preNotificationId, UUID vehicleId, String plateNumber,
                                     String driverName, Instant plannedArrival, UUID partnerAId) {
    }

    public record PublicVehicleQrInfo(String plateNumber, String driverName, String driverPhone,
                                      String carrierCompany, String vehicleType, Instant plannedArrival,
                                      String destinationYard) {
    }

    public record CheckOutResult(YardLog yardLog, long durationSeconds) {
    }

    public record YardHistoryEntry(UUID id, UUID preNotificationId, String plateNumber, String driverName,
                                   UUID yardId, String yardCode, String yardName, Instant checkInAt,
                                   Instant checkOutAt, Long durationSeconds) {
    }
}
