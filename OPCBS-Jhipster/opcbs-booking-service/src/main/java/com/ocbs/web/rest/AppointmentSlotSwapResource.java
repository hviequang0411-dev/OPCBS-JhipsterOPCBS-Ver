package com.ocbs.web.rest;

import com.ocbs.service.AppointmentSlotSwapService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for testing Slot Swapping & JVM Deadlock chaos scenarios.
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentSlotSwapResource {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentSlotSwapResource.class);

    private final AppointmentSlotSwapService swapService;

    public AppointmentSlotSwapResource(AppointmentSlotSwapService swapService) {
        this.swapService = swapService;
    }

    /**
     * {@code POST /appointments/swap-slots-deadlock} : Hoán đổi ca khám với logic gây Deadlock.
     */
    @PostMapping("/swap-slots-deadlock")
    public ResponseEntity<Map<String, Object>> swapSlotsWithDeadlock(
        @RequestParam(name = "sourceSlotId") Long sourceSlotId,
        @RequestParam(name = "targetSlotId") Long targetSlotId
    ) {
        LOG.info("Request swap slots WITH DEADLOCK risk: source={}, target={}", sourceSlotId, targetSlotId);
        boolean success = swapService.swapSlotsWithDeadlock(sourceSlotId, targetSlotId);
        return ResponseEntity.ok(Map.of(
            "success", success,
            "sourceSlotId", sourceSlotId,
            "targetSlotId", targetSlotId,
            "message", "Hoán đổi ca khám hoàn tất"
        ));
    }

    /**
     * {@code POST /appointments/swap-slots-safe} : Hoán đổi ca khám an toàn (Global Lock Ordering).
     */
    @PostMapping("/swap-slots-safe")
    public ResponseEntity<Map<String, Object>> swapSlotsSafe(
        @RequestParam(name = "sourceSlotId") Long sourceSlotId,
        @RequestParam(name = "targetSlotId") Long targetSlotId
    ) {
        LOG.info("Request swap slots SAFE: source={}, target={}", sourceSlotId, targetSlotId);
        boolean success = swapService.swapSlotsSafe(sourceSlotId, targetSlotId);
        return ResponseEntity.ok(Map.of(
            "success", success,
            "sourceSlotId", sourceSlotId,
            "targetSlotId", targetSlotId,
            "message", "Hoán đổi an toàn thành công"
        ));
    }
}
