package com.yms.yms.domain.vehicle.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.vehicle.entity.PreNotification;
import com.yms.yms.domain.vehicle.repository.PreNotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class QrCodeService {

    private static final long QR_TTL_SECONDS = 24 * 60 * 60;
    private final PreNotificationRepository notificationRepository;
    private final Clock clock;
    private final byte[] secret;

    public QrCodeService(PreNotificationRepository notificationRepository,
                         Clock clock,
                         @Value("${yms.qr.hmac-secret:${yms.security.jwt-secret}}") String secret) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalArgumentException("QR HMAC secret must contain at least 32 bytes");
        }
    }

    @Transactional
    public QrResult issue(UUID notificationId) {
        PreNotification notification = findNotification(notificationId);
        if (!"REGISTERED".equals(notification.getStatus())) {
            throw new BusinessException("QR can only be issued for a registered vehicle", HttpStatus.CONFLICT);
        }
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(QR_TTL_SECONDS);
        String payload = notificationId + "." + expiresAt.getEpochSecond();
        String token = payload + "." + sign(payload);
        notification.setQrToken(token);
        notification.setQrExpiresAt(expiresAt);
        notificationRepository.save(notification);
        return new QrResult(notificationId, token, expiresAt, imageData(token));
    }

    @Transactional(readOnly = true)
    public PreNotification verify(String token) {
        if (token == null || token.length() > 500) {
            throw new BusinessException("QR code is invalid", HttpStatus.BAD_REQUEST);
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new BusinessException("QR code is invalid", HttpStatus.BAD_REQUEST);
        }

        UUID notificationId;
        long expiry;
        try {
            notificationId = UUID.fromString(parts[0]);
            expiry = Long.parseLong(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("QR code is invalid", HttpStatus.BAD_REQUEST);
        }
        String payload = parts[0] + "." + parts[1];
        byte[] provided = parts[2].getBytes(StandardCharsets.US_ASCII);
        byte[] expected = sign(payload).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(provided, expected)) {
            throw new BusinessException("QR signature is invalid", HttpStatus.UNAUTHORIZED);
        }
        if (clock.instant().getEpochSecond() >= expiry) {
            throw new BusinessException("QR code has expired", HttpStatus.GONE);
        }

        PreNotification notification = findNotification(notificationId);
        if (!token.equals(notification.getQrToken())
                || notification.getQrExpiresAt() == null
                || !notification.getQrExpiresAt().isAfter(clock.instant())) {
            throw new BusinessException("QR code is no longer active", HttpStatus.GONE);
        }
        return notification;
    }

    private PreNotification findNotification(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Pre-notification not found", HttpStatus.NOT_FOUND));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to sign QR payload", exception);
        }
    }

    private String imageData(String token) {
        try {
            var hints = Map.of(EncodeHintType.MARGIN, 1);
            var matrix = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 320, 320, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("Unable to generate QR image", exception);
        }
    }

    public record QrResult(UUID preNotificationId, String token, Instant expiresAt, String imageBase64) {
    }
}
