package com.potential_radar.PR.tech.controller;

import com.potential_radar.PR.tech.domain.TechStack;
import com.potential_radar.PR.tech.repository.TechStackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tech-stacks")
@RequiredArgsConstructor
public class TechStackController {

    private final TechStackRepository techStackRepository;

    // 전체 기술 스택 목록 조회 엔드포인트 (ID와 이름 포함)
    @GetMapping()
    public ResponseEntity<List<Map<String, Object>>> getAllTechStacks() {
        List<TechStack> techStacks = techStackRepository.findAll();
        List<Map<String, Object>> response = techStacks.stream()
                .map(ts -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("techStackId", ts.getTechStackId());
                    map.put("name", ts.getName());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(response);
    }
}