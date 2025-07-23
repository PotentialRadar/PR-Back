package com.potential_radar.PR.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// record는 불변(immutable) 데이터 객체를 간단히 선언할 수 있는 문법입니다. 특히 DTO에 적합합니다.

/**
 * 항목	설명
 * 코드 간결성	getter, 생성자, toString, equals, hashCode 자동 생성
 * 불변성 보장	필드는 final이고, setter 없음
 * DTO 목적에 적합	DTO는 보통 상태 변화 없이 전달만 하므로 record가 잘 맞음
 * 직렬화 지원	기본적으로 Serializable
 */
public record UserSignupRequest (

    @NotBlank(message = "이메일은 필수입니다.")
    @Email
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    String password,

    @NotBlank
    String name,

    @NotBlank
    String nickname
){}
