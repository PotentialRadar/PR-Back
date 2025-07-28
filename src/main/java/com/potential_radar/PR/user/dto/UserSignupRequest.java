package com.potential_radar.PR.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// record는 불변(immutable) 데이터 객체를 간단히 선언할 수 있는 문법입니다. 특히 DTO에 적합합니다.

/**
 * 항목	설명
 * 코드 간결성	getter, 생성자, toString, equals, hashCode 자동 생성
 * 불변성 보장	필드는 final이고, setter 없음
 * DTO 목적에 적합	DTO는 보통 상태 변화 없이 전달만 하므로 record가 잘 맞음
 * 직렬화 지원	기본적으로 Serializable
 */
public record UserSignupRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
                message = "비밀번호는 8~20자이며 대소문자, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다."
        )
        String password,


        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2-20자 사이여야 합니다.")
        @Pattern(regexp = "^[a-zA-Z0-9가-힣]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다.")
        String nickname
) {
}
