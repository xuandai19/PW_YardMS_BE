package com.yms.yms.infrastructure.config;

import com.yms.yms.domain.finance.job.ReportEmailQuartzJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.quartz.spi.TriggerFiredBundle;
import java.util.TimeZone;

@Configuration
public class QuartzConfig {
    @Bean
    JobDetail reportEmailJobDetail() {
        return JobBuilder.newJob(ReportEmailQuartzJob.class)
                .withIdentity("ymsReportEmailJob")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger reportEmailTrigger(JobDetail reportEmailJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(reportEmailJobDetail)
                .withIdentity("ymsReportEmailTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 10 0 * * ?")
                        .inTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")))
                .build();
    }

    @Bean
    SchedulerFactoryBeanCustomizer quartzJobFactoryCustomizer(AutowireCapableBeanFactory beanFactory) {
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory() {
            @Override
            protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
                Object job = super.createJobInstance(bundle);
                beanFactory.autowireBean(job);
                return job;
            }
        };
        return scheduler -> scheduler.setJobFactory(jobFactory);
    }
}
