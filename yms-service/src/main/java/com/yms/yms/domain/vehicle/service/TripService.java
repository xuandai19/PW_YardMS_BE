package com.yms.yms.domain.vehicle.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.vehicle.entity.Trip;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TripService {
    private final TripRepository tripRepository;

    public TripService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Transactional
    public Trip updateCoordinates(UUID tripId, BigDecimal originLatitude, BigDecimal originLongitude,
            BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found", HttpStatus.NOT_FOUND));

        if (originLatitude != null) {
            trip.setOriginLatitude(originLatitude);
            trip.setOriginLongitude(originLongitude);
        }
        if (destinationLatitude != null) {
            trip.setDestinationLatitude(destinationLatitude);
            trip.setDestinationLongitude(destinationLongitude);
        }
        return tripRepository.save(trip);
    }
}
