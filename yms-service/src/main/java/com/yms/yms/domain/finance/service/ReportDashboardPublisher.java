package com.yms.yms.domain.finance.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReportDashboardPublisher {
    private final ReportService reportService;
    private final SimpMessagingTemplate messaging;

    public ReportDashboardPublisher(ReportService reportService, SimpMessagingTemplate messaging) {
        this.reportService = reportService;
        this.messaging = messaging;
    }

    @Scheduled(fixedDelayString = "${yms.finance.dashboard-publish-ms:30000}")
    public void publishDashboard() {
        messaging.convertAndSend("/topic/dashboard", reportService.dashboard());
    }
}
