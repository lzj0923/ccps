package com.ccps.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminRentCollectionWorkflowResponse;

@Component
public class AdminRentCollectionScheduler {
    private static final Logger log = LoggerFactory.getLogger(AdminRentCollectionScheduler.class);

    private final AdminRentCollectionWorkflowService service;

    public AdminRentCollectionScheduler(AdminRentCollectionWorkflowService service) {
        this.service = service;
    }

    @Scheduled(cron = "${ccps.rent-collection.cron:0 5 * * * *}")
    public void sendDueNotices() {
        int sent = 0;
        for (AdminRentCollectionWorkflowResponse.Item item : service.overview().items()) {
            if (!isDue(item)) continue;
            try {
                service.sendStage(null, item.invoiceId(), item.currentStage());
                sent++;
            } catch (ResponseStatusException exception) {
                log.warn("Automatic rent collection skipped invoice {} stage {}: {}",
                        item.invoiceId(), item.currentStage(), exception.getReason());
            } catch (RuntimeException exception) {
                log.error("Automatic rent collection failed for invoice {} stage {}",
                        item.invoiceId(), item.currentStage(), exception);
            }
        }
        if (sent > 0) log.info("Automatic rent collection sent {} notices", sent);
    }

    private boolean isDue(AdminRentCollectionWorkflowResponse.Item item) {
        return "active".equals(item.workflowStatus())
                && !"waiting".equals(item.currentStage())
                && !"on_hold".equals(item.currentStage());
    }
}
