package com.yms.yms.domain.finance.service;

import com.yms.yms.domain.finance.repository.DebtRepository;
import com.yms.yms.domain.finance.repository.InvoiceRepository;
import com.yms.yms.domain.service.repository.ServiceOrderRepository;
import com.yms.yms.domain.yard.repository.YardRepository;
import com.yms.yms.domain.yard.repository.YardVehicleAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceTest {
    @Test
    void monthlyReportBindsOffsetDateTimeForPostgresTimestampParameters() {
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        DebtRepository debts = mock(DebtRepository.class);
        ServiceOrderRepository serviceOrders = mock(ServiceOrderRepository.class);
        YardRepository yards = mock(YardRepository.class);
        YardVehicleAssignmentRepository assignments = mock(YardVehicleAssignmentRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-10T12:00:00Z"), ZoneOffset.UTC);
        ReportService reports = new ReportService(invoices, debts, serviceOrders, yards, assignments, jdbc, clock);

        when(invoices.findByIssueDateBetweenOrderByIssueDate(LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31))).thenReturn(List.of());
        when(debts.findAll()).thenReturn(List.of());
        when(yards.findAll()).thenReturn(List.of());
        when(assignments.findOverlappingPeriod(any(), any())).thenReturn(List.of());
        when(jdbc.queryForObject(any(), eq(Long.class), any(), any())).thenReturn(0L);

        reports.monthly(2026, 5);

        var start = org.mockito.ArgumentCaptor.forClass(Object.class);
        var end = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(jdbc).queryForObject(eq("SELECT COUNT(*) FROM trips WHERE created_at >= ? AND created_at < ?"),
                eq(Long.class), start.capture(), end.capture());
        assertInstanceOf(OffsetDateTime.class, start.getValue());
        assertInstanceOf(OffsetDateTime.class, end.getValue());
        assertEquals(OffsetDateTime.parse("2026-05-01T00:00Z"), start.getValue());
        assertEquals(OffsetDateTime.parse("2026-06-01T00:00Z"), end.getValue());

        var entriesStart = org.mockito.ArgumentCaptor.forClass(Object.class);
        var entriesEnd = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(jdbc).queryForObject(eq("SELECT COUNT(*) FROM yard_logs WHERE check_in_at >= ? AND check_in_at < ?"),
                eq(Long.class), entriesStart.capture(), entriesEnd.capture());
        assertInstanceOf(OffsetDateTime.class, entriesStart.getValue());
        assertInstanceOf(OffsetDateTime.class, entriesEnd.getValue());
        assertEquals(OffsetDateTime.parse("2026-05-01T00:00Z"), entriesStart.getValue());
        assertEquals(OffsetDateTime.parse("2026-06-01T00:00Z"), entriesEnd.getValue());
    }
}
