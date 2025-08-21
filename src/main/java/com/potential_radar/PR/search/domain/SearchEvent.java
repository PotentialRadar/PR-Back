package com.potential_radar.PR.search.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SearchEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String keyword;
    private String singleTechStack;
    private String techPart;
    private LocalDateTime searchTime;
    private String sessionId;
    private Integer resultCount;
    
    @PrePersist
    protected void onCreate() {
        searchTime = LocalDateTime.now();
    }
}