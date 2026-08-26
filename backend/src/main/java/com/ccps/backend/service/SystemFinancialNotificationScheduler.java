package com.ccps.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.service.SystemFinancialNotificationService.BuildingTask;

@Component
public class SystemFinancialNotificationScheduler {
    private static final Logger log = LoggerFactory.getLogger(SystemFinancialNotificationScheduler.class);
    private final SystemFinancialNotificationService service;

    public SystemFinancialNotificationScheduler(SystemFinancialNotificationService service) {
        this.service = service;
    }

    @Scheduled(cron = "${ccps.financial-notifications.cron:0 15 * * * *}")
    public void sendDueFinancialNotifications() {
        int sent = 0;
        for (BuildingTask task : service.dueBuildingTasks()) {
            try {
                service.sendBuildingNotice(task.relatedId(), task.stage());
                sent++;
            } catch (ResponseStatusException exception) {
                log.warn("System building payment notice skipped {} {}: {}",
                        task.relatedId(), task.stage(), exception.getReason());
            } catch (RuntimeException exception) {
                log.error("System building payment notice failed {} {}", task.relatedId(), task.stage(), exception);
            }
        }
        for (Long accountId : service.dueReserveAccounts()) {
            try {
                service.sendReserveNotice(accountId);
                sent++;
            } catch (ResponseStatusException exception) {
                log.warn("System reserve notice skipped {}: {}", accountId, exception.getReason());
            } catch (RuntimeException exception) {
                log.error("System reserve notice failed {}", accountId, exception);
            }
        }
        if (sent > 0) log.info("System financial notifications sent {} notices", sent);
    }
}
