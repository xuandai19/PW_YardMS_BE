package com.yms.yms.domain.finance.service;

import com.yms.yms.domain.finance.entity.ReportSchedule;
import com.yms.yms.domain.finance.repository.ReportScheduleRepository;
import com.yms.common.exception.BusinessException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ReportDeliveryService {
    private final ReportScheduleRepository schedules;
    private final ReportService reportService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Clock clock;

    public ReportDeliveryService(ReportScheduleRepository schedules, ReportService reportService,
            ObjectProvider<JavaMailSender> mailSenderProvider, Clock clock) {
        this.schedules = schedules;
        this.reportService = reportService;
        this.mailSenderProvider = mailSenderProvider;
        this.clock = clock;
    }

    public void sendDueReports() {
        LocalDate today = LocalDate.now(clock);
        if (today.getDayOfMonth() != 1) {
            return;
        }
        LocalDate previousMonth = today.minusMonths(1);
        if (previousMonth.getMonthValue() % 3 == 0) {
            ReportService.QuarterlyReport report = reportService.quarterly(previousMonth.getYear(),
                    (previousMonth.getMonthValue() - 1) / 3 + 1);
            for (ReportSchedule schedule : schedules.findByActiveTrueAndReportType("QUARTERLY")) {
                send(schedule, "Quarterly report " + previousMonth.getYear() + " Q"
                        + ((previousMonth.getMonthValue() - 1) / 3 + 1),
                        "Revenue: " + report.revenue() + "\nInvoices: " + report.invoiceCount()
                                + "\nCompleted service orders: " + report.completedServiceOrders());
            }
        }
        ReportService.MonthlyReport report = reportService.monthly(previousMonth.getYear(),
                previousMonth.getMonthValue());
        for (ReportSchedule schedule : schedules.findByActiveTrueAndReportType("MONTHLY")) {
            send(schedule, "Monthly report " + previousMonth,
                    "Revenue: " + report.revenue() + "\nInvoices: " + report.invoiceCount()
                            + "\nOutstanding debt: " + report.outstandingDebt());
        }
    }

    private void send(ReportSchedule schedule, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new BusinessException("Email delivery is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(schedule.getRecipientEmail());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public List<ReportSchedule> listSchedules() {
        return schedules.findAll();
    }

    public ReportSchedule createSchedule(String reportType, String recipientEmail) {
        if (!"MONTHLY".equals(reportType) && !"QUARTERLY".equals(reportType)) {
            throw new BusinessException("Report type must be MONTHLY or QUARTERLY", HttpStatus.BAD_REQUEST);
        }
        ReportSchedule schedule = new ReportSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setReportType(reportType);
        schedule.setRecipientEmail(recipientEmail);
        schedule.setActive(true);
        schedule.setCreatedAt(clock.instant());
        return schedules.save(schedule);
    }

    public void deleteSchedule(UUID id) {
        if (!schedules.existsById(id)) {
            throw new BusinessException("Report schedule not found", HttpStatus.NOT_FOUND);
        }
        schedules.deleteById(id);
    }
}
