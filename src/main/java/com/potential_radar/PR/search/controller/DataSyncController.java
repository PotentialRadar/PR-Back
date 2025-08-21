package com.potential_radar.PR.search.controller;

import com.potential_radar.PR.search.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search/admin/sync")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class DataSyncController {
    
    private final DataSyncService dataSyncService;
    
    
    @PostMapping("/all")
    public ResponseEntity<Map<String, String>> syncAll() {
        try {
            dataSyncService.syncAllData();
            return ResponseEntity.ok(Map.of("message", "All data synchronized successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to sync data: " + e.getMessage()));
        }
    }
}