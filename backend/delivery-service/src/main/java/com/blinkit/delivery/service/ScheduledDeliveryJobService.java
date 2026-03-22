package com.blinkit.delivery.service;

import com.blinkit.delivery.entity.DeliveryPartner;
import com.blinkit.delivery.entity.DeliveryTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduled jobs that keep the delivery lifecycle reliable:
 *
 *  1. retryUnassignedTasks   — every 30 s: try to assign a partner to UNASSIGNED tasks
 *  2. releaseCooldownPartners — every 60 s: mark partners available after cooldown expires
 *  3. autoCompleteStale       — every 5 m:  auto-mark DELIVERED for tasks past their ETA
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledDeliveryJobService {

    private final DeliveryTaskService    taskService;
    private final DeliveryPartnerService partnerService;

    // ── 1. Retry assignment for UNASSIGNED tasks ──────────────────

    @Scheduled(fixedDelay = 30_000)   // every 30 seconds
    public void retryUnassignedTasks() {
        List<DeliveryTask> unassigned = taskService.getUnassignedTasks();
        if (unassigned.isEmpty()) return;

        log.debug("Scheduler: {} UNASSIGNED task(s) found — attempting assignment", unassigned.size());
        for (DeliveryTask task : unassigned) {
            taskService.tryAutoAssign(task);
        }
    }

    // ── 2. Release partners whose cooldown has expired ────────────

    @Scheduled(fixedDelay = 60_000)   // every 60 seconds
    public void releaseCooldownPartners() {
        List<DeliveryPartner> expired = partnerService.findPartnersWithExpiredCooldown();
        if (expired.isEmpty()) return;

        log.info("Scheduler: releasing {} partner(s) from cooldown", expired.size());
        for (DeliveryPartner partner : expired) {
            partner.setIsAvailable(true);
            partner.setCooldownUntil(null);
            partnerService.savePartner(partner);
            log.info("Partner {} is now available again after cooldown", partner.getPartnerId());
        }
    }

    // ── 3. Auto-complete tasks that are past their delivery ETA ───

    @Scheduled(fixedDelay = 300_000)  // every 5 minutes
    public void autoCompleteStale() {
        List<DeliveryTask> stale = taskService.getStaleInProgressTasks();
        if (stale.isEmpty()) return;

        log.warn("Scheduler: {} stale in-progress task(s) past ETA — auto-completing", stale.size());
        for (DeliveryTask task : stale) {
            taskService.autoCompleteTask(task);
        }
    }
}
