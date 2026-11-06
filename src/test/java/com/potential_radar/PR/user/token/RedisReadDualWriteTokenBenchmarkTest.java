package com.potential_radar.PR.user.token;

import org.springframework.boot.test.context.SpringBootTest;

/** 2단계 - 이중 쓰기, Redis 읽기 + DB 폴백 */
@SpringBootTest(properties = {
        TokenStoreBenchmarkSupport.COMMON_PROPS,
        "auth.token.dual-write=true",
        "auth.token.read-source=REDIS"
})
class RedisReadDualWriteTokenBenchmarkTest extends TokenStoreBenchmarkSupport {
}
