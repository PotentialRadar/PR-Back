package com.potential_radar.PR.tech.service;

import com.potential_radar.PR.tech.domain.TechPart;
import com.potential_radar.PR.tech.repository.TechPartRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TechPartService {

    private final TechPartRepository techPartRepository;

    @PostConstruct
    private void initializeTechParts() {
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
    
    public List<TechPart> getAllTechParts() {
        return techPartRepository.findAll();
    }
}
