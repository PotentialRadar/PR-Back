package com.potential_radar.PR.tech.controller;

import com.potential_radar.PR.tech.domain.TechPart;
import com.potential_radar.PR.tech.repository.TechPartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tech")
@RequiredArgsConstructor
@Slf4j
public class TechController {
    
    private final TechPartRepository techPartRepository;
    
    @PostMapping("/parts/init")
    public ResponseEntity<Map<String, Object>> initTechParts() {
        try {
            log.info("수동으로 기술 파트 초기화 시작");
            
            List<String> techPartNames = Arrays.asList(
                "프론트엔드", "백엔드", "풀스택", "모바일", "데브옵스", 
                "데이터사이언스", "AI/ML", "게임개발", "보안", "QA/테스터", 
                "UI/UX디자인", "PM/기획"
            );

            int createdCount = 0;
            for (String name : techPartNames) {
                if (techPartRepository.findByNameIgnoreCase(name).isEmpty()) {
                    TechPart techPart = TechPart.builder()
                        .name(name)
                        .build();
                    techPartRepository.save(techPart);
                    createdCount++;
                    log.info("기술 파트 생성: {}", name);
                }
            }
            
            long totalCount = techPartRepository.count();
            log.info("기술 파트 초기화 완료. 생성된 개수: {}, 전체 개수: {}", createdCount, totalCount);
            
            return ResponseEntity.ok(Map.of(
                "message", "기술 파트 초기화 완료",
                "createdCount", createdCount,
                "totalCount", totalCount
            ));
            
        } catch (Exception e) {
            log.error("기술 파트 초기화 실패", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "기술 파트 초기화 실패: " + e.getMessage()));
        }
    }
    
    @GetMapping("/parts")
    public ResponseEntity<List<TechPart>> getAllTechParts() {
        List<TechPart> techParts = techPartRepository.findAll();
        return ResponseEntity.ok(techParts);
    }
}