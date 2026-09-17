package com.ccps.backend.service;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminFundOperationsRequest.GenerateRemittanceBatch;

@Component
public class OwnerRemittanceScheduler {
    private static final Logger log = LoggerFactory.getLogger(OwnerRemittanceScheduler.class);
    private final AdminFundOperationsService service;

    public OwnerRemittanceScheduler(AdminFundOperationsService service) {
        this.service = service;
    }

    @Scheduled(cron = "${ccps.owner-remittance.cron:0 20 1 * * *}")
    public void generateDueBatch() {
        try {
            service.generateRemittanceBatch(null,
                    new GenerateRemittanceBatch(LocalDate.now(), "系统按到期汇款计划自动生成"));
            log.info("Automatic owner remittance batch generated");
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                log.debug("Automatic owner remittance batch skipped: {}", exception.getReason());
            } else {
                throw exception;
            }
        }
    }
}
