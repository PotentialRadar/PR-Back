package com.potential_radar.PR.tech.controller;

import com.potential_radar.PR.tech.domain.TechPart;
import com.potential_radar.PR.tech.service.TechPartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tech-parts")
@RequiredArgsConstructor
public class  TechPartController {

    private final TechPartService techPartService;

    // TechPart 목록 조회 엔드포인트
    @GetMapping()
    public ResponseEntity<List<Map<String, Object>>> getTechParts() {
        List<TechPart> techParts = techPartService.getAllTechParts();
        List<Map<String, Object>> response = techParts.stream()
                .map(tp -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("techPartId", tp.getTechPartId());
                    map.put("name", tp.getName());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(response);
    }
}
