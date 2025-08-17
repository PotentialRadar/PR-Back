package com.potential_radar.PR.search.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoCompleteRes {
    private List<String> suggestions;
}