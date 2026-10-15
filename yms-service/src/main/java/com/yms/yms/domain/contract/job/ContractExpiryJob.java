package com.yms.yms.domain.contract.job;

import com.yms.yms.domain.contract.service.ContractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContractExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ContractExpiryJob.class);
    private final ContractService contractService;

    public ContractExpiryJob(ContractService contractService) {
        this.contractService = contractService;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void expireContracts() {
        int count = contractService.expireContracts();
        if (count > 0) {
            log.info("Marked {} contracts as EXPIRED", count);
        }
    }
}
