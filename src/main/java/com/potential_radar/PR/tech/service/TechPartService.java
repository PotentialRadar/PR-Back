package com.potential_radar.PR.tech.service;

import com.potential_radar.PR.tech.domain.TechPart;
import com.potential_radar.PR.tech.repository.TechPartRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TechPartService {
    
    private final TechPartRepository techPartRepository;

    @PostConstruct
    @Transactional
    public void initializeTechParts() {
        List<String> techPartNames = Arrays.asList(
                "프론트엔드", "백엔드", "풀스택", "모바일", "데브옵스",
                "데이터사이언스", "AI/ML", "게임개발", "보안", "QA/테스터",
                "UI/UX디자인", "PM/기획", "기타"
        );

        for (String name : techPartNames) {
            if (techPartRepository.findByNameIgnoreCase(name).isEmpty()) {
                TechPart techPart = TechPart.builder()
                        .name(name)
                        .build();
                techPartRepository.save(techPart);
            }
        }
    }


    /**
     * 모든 기술 파트 조회 (캐시 적용)
     * 애플리케이션 시작 후 첫 조회 시에만 DB에 접근하고, 이후는 캐시에서 조회
     */
    @Cacheable(value = "techParts", key = "'all'")
    public List<TechPart> getAllTechParts() {
        log.info("데이터베이스에서 모든 기술 파트 조회 중...");
        return techPartRepository.findAll();
    }
    
    /**
     * 기술 파트명 리스트만 조회 (캐시 적용)
     * 프론트엔드에서 필터 옵션으로 사용하기 위한 간단한 문자열 리스트
     */
    @Cacheable(value = "techPartNames", key = "'all'")
    public List<String> getAllTechPartNames() {
        log.info("데이터베이스에서 기술 파트명 리스트 조회 중...");
        return techPartRepository.findAll().stream()
                .map(TechPart::getName)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 기술 파트 조회 (캐시 적용)
     */
    @Cacheable(value = "techParts", key = "#name.toLowerCase()")
    public TechPart findByName(String name) {
        log.info("기술 파트 조회: {}", name);
        return techPartRepository.findByNameIgnoreCase(name)
                .orElse(null);
    }
}
