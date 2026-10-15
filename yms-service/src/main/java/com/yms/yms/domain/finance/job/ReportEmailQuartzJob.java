package com.yms.yms.domain.finance.job;

import com.yms.yms.domain.finance.service.ReportDeliveryService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

public class ReportEmailQuartzJob implements Job {
    @Autowired
    private ReportDeliveryService reportDeliveryService;

    @Override
    public void execute(JobExecutionContext context) {
        reportDeliveryService.sendDueReports();
    }
}
