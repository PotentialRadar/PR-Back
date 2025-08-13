package com.potential_radar.PR.search.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private boolean hasNext;
    private boolean hasPrevious;
    private long searchTimeMs;
}
