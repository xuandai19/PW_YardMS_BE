package com.yms.yms.domain.goods.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.goods.entity.Goods;
import com.yms.yms.domain.goods.entity.GoodsDiscrepancy;
import com.yms.yms.domain.goods.repository.GoodsDiscrepancyRepository;
import com.yms.yms.domain.goods.repository.GoodsRepository;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class GoodsService {
    private final GoodsRepository goodsRepository;
    private final GoodsDiscrepancyRepository discrepancyRepository;
    private final TripRepository tripRepository;
    private final SimpMessagingTemplate messaging;
    private final Clock clock;
    private final byte[] signingKey;

    public GoodsService(GoodsRepository goodsRepository, GoodsDiscrepancyRepository discrepancyRepository,
            TripRepository tripRepository, SimpMessagingTemplate messaging, Clock clock,
            @Value("${yms.qr.hmac-secret}") String secret) {
        this.goodsRepository = goodsRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.tripRepository = tripRepository;
        this.messaging = messaging;
        this.clock = clock;
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public List<GoodsQr> createBatch(UUID tripId, List<GoodsInput> items) {
        if (!tripRepository.existsById(tripId)) {
            throw new BusinessException("Trip not found", HttpStatus.NOT_FOUND);
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException("At least one goods item is required", HttpStatus.BAD_REQUEST);
        }
        return items.stream().map(input -> {
            Goods goods = new Goods();
            goods.setId(UUID.randomUUID());
            goods.setTripId(tripId);
            goods.setItemCode(input.itemCode().trim());
            goods.setDescription(input.description());
            goods.setQuantity(input.quantity());
            goods.setUnit(input.unit().trim());
            goods.setCreatedAt(clock.instant());
            goods.setQrValue(sign(goods.getId()));
            Goods saved = goodsRepository.save(goods);
            return new GoodsQr(saved, createQrImage(saved.getQrValue()));
        }).toList();
    }

    public List<Goods> byTrip(UUID tripId) {
        return goodsRepository.findByTripIdOrderByItemCode(tripId);
    }

    public GoodsQr qr(UUID goodsId) {
        Goods goods = goodsRepository.findById(goodsId)
                .orElseThrow(() -> new BusinessException("Goods item not found", HttpStatus.NOT_FOUND));
        return new GoodsQr(goods, createQrImage(goods.getQrValue()));
    }

    @Transactional
    public ScanResult reconcile(UUID goodsId, String qrValue, BigDecimal scannedQuantity) {
        Goods goods = goodsRepository.findById(goodsId)
                .orElseThrow(() -> new BusinessException("Goods item not found", HttpStatus.NOT_FOUND));
        if (qrValue == null || !MessageDigest.isEqual(goods.getQrValue().getBytes(StandardCharsets.UTF_8),
                qrValue.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException("Goods QR is invalid", HttpStatus.BAD_REQUEST);
        }
        if (scannedQuantity == null || scannedQuantity.signum() < 0) {
            throw new BusinessException("Scanned quantity cannot be negative", HttpStatus.BAD_REQUEST);
        }
        goods.setReceivedQuantity(scannedQuantity.min(goods.getQuantity()));
        boolean mismatch = scannedQuantity.compareTo(goods.getQuantity()) != 0;
        goods.setStatus(mismatch ? "DISCREPANCY" : "RECEIVED");
        goodsRepository.save(goods);

        List<GoodsDiscrepancy> open = discrepancyRepository.findByGoodsIdAndResolvedFalse(goodsId);
        if (mismatch) {
            GoodsDiscrepancy discrepancy = new GoodsDiscrepancy();
            discrepancy.setId(UUID.randomUUID());
            discrepancy.setGoodsId(goodsId);
            discrepancy.setExpectedQuantity(goods.getQuantity());
            discrepancy.setScannedQuantity(scannedQuantity);
            discrepancy.setMessage("Expected " + goods.getQuantity() + " " + goods.getUnit()
                    + " but scanned " + scannedQuantity);
            discrepancy.setCreatedAt(clock.instant());
            discrepancy.setResolved(false);
            discrepancyRepository.save(discrepancy);
            messaging.convertAndSend("/topic/goods/discrepancies", discrepancy);
        } else {
            open.forEach(discrepancy -> discrepancy.setResolved(true));
            discrepancyRepository.saveAll(open);
        }
        return new ScanResult(goods, mismatch, scannedQuantity);
    }

    public List<GoodsDiscrepancy> discrepancies() {
        return discrepancyRepository.findTop100ByOrderByCreatedAtDesc();
    }

    private String sign(UUID goodsId) {
        String payload = "YMS-GOODS:" + goodsId;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return payload + ":" + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to sign goods QR", exception);
        }
    }

    private String createQrImage(String value) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 280, 280);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("Unable to create goods QR image", exception);
        }
    }

    public record GoodsInput(String itemCode, String description, BigDecimal quantity, String unit) {
    }

    public record GoodsQr(Goods goods, String imageBase64) {
    }

    public record ScanResult(Goods goods, boolean mismatch, BigDecimal scannedQuantity) {
    }
}
