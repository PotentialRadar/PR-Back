package com.potential_radar.PR.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExperienceRange {
    FRESHER("신입"),
    LT_1("1년미만"),
    Y1_3("1~3년"),
    Y5_10("5~10년"),
    GE_10("10년이상");

    private final String description;
}