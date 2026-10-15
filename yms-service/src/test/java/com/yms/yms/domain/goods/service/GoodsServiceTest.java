package com.yms.yms.domain.goods.service;

import com.yms.yms.domain.goods.entity.Goods;
import com.yms.yms.domain.goods.repository.GoodsDiscrepancyRepository;
import com.yms.yms.domain.goods.repository.GoodsRepository;
import com.yms.yms.domain.vehicle.repository.TripRepository;
import com.yms.yms.domain.goods.entity.GoodsDiscrepancy;
import com.yms.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoodsServiceTest {
    private final GoodsRepository goods = mock(GoodsRepository.class);
    private final GoodsDiscrepancyRepository discrepancies = mock(GoodsDiscrepancyRepository.class);
    private final TripRepository trips = mock(TripRepository.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private GoodsService service;

    @BeforeEach
    void setUp() {
        service = new GoodsService(goods, discrepancies, trips, messaging,
                Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC),
                "test-qr-secret-key-at-least-32-characters");
    }

    @Test
    void generatedQrValuesAreUniqueForEachGoodsItem() {
        when(trips.existsById(any())).thenReturn(true);
        when(goods.save(any(Goods.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<GoodsService.GoodsQr> result = service.createBatch(UUID.randomUUID(), List.of(
                new GoodsService.GoodsInput("ITEM-1", null, BigDecimal.ONE, "box"),
                new GoodsService.GoodsInput("ITEM-2", null, BigDecimal.ONE, "box")));

        assertEquals(2, result.size());
        assertNotEquals(result.get(0).goods().getQrValue(), result.get(1).goods().getQrValue());
        assertTrue(result.get(0).imageBase64().length() > 100);
    }

    @Test
    void mismatchedQuantityPersistsDiscrepancyAndClampsReceivedCount() {
        Goods item = new Goods();
        item.setId(UUID.randomUUID());
        item.setQuantity(BigDecimal.TEN);
        item.setUnit("box");
        item.setQrValue("qr-signed-value");
        when(goods.findById(item.getId())).thenReturn(java.util.Optional.of(item));
        when(goods.save(any(Goods.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(discrepancies.findByGoodsIdAndResolvedFalse(item.getId())).thenReturn(List.of());
        when(discrepancies.save(any(GoodsDiscrepancy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GoodsService.ScanResult result = service.reconcile(item.getId(), "qr-signed-value", BigDecimal.valueOf(12));

        assertTrue(result.mismatch());
        assertEquals(BigDecimal.TEN, result.goods().getReceivedQuantity());
        verify(discrepancies).save(any(GoodsDiscrepancy.class));
        verify(messaging).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/goods/discrepancies"),
                org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void rejectsInvalidQr() {
        Goods item = new Goods();
        item.setId(UUID.randomUUID());
        item.setQrValue("expected");
        when(goods.findById(item.getId())).thenReturn(java.util.Optional.of(item));

        assertThrows(BusinessException.class,
                () -> service.reconcile(item.getId(), "invalid", BigDecimal.ZERO));
    }
}
