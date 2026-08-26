package com.ccps.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReserveTargetPolicyScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReserveTargetPolicyScheduler.class);
    private final ReserveTargetPolicyService service;

    public ReserveTargetPolicyScheduler(ReserveTargetPolicyService service) {
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void calculateOnStartup() {
        recalculateAll();
    }

    @Scheduled(cron = "${ccps.reserve-target.cron:0 10 * * * *}")
    public void recalculateAll() {
        int updated = 0;
        for (Long accountId : service.activeAccountIds()) {
            try {
                service.recalculate(accountId);
                updated++;
            } catch (RuntimeException exception) {
                log.error("Automatic reserve target calculation failed for account {}", accountId, exception);
            }
        }
        if (updated > 0) log.info("Automatic reserve targets recalculated for {} accounts", updated);
    }
}
