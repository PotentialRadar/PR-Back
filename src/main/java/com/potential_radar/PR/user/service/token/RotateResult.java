package com.potential_radar.PR.user.service.token;

/**
 * Refresh Token 회전 결과.
 *
 * @param outcome      판정
 * @param refreshToken 클라이언트에게 돌려줄 Refresh Token (REUSE_DETECTED면 null)
 */
public record RotateResult(Outcome outcome, String refreshToken) {

    public enum Outcome {
        /** 처음 쓰는 토큰. 새 토큰으로 회전했다. */
        ROTATED,
        /** 유예 구간 안에 다시 온 직전 토큰(동시 요청). 방금 발급한 토큰을 그대로 돌려준다. */
        GRACE_HIT,
        /** 이미 회전된 토큰의 재사용. 사용자 토큰을 모두 무효화해야 한다. */
        REUSE_DETECTED
    }

    public static RotateResult rotated(String newToken) {
        return new RotateResult(Outcome.ROTATED, newToken);
    }

    public static RotateResult graceHit(String issuedToken) {
        return new RotateResult(Outcome.GRACE_HIT, issuedToken);
    }

    public static RotateResult reuseDetected() {
        return new RotateResult(Outcome.REUSE_DETECTED, null);
    }

    public boolean isReuse() {
        return outcome == Outcome.REUSE_DETECTED;
    }
}
