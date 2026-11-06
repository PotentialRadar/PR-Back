package com.potential_radar.PR.user.token;

import org.springframework.boot.test.context.SpringBootTest;

/** 3단계 - Redis 단독 */
@SpringBootTest(properties = {
        TokenStoreBenchmarkSupport.COMMON_PROPS,
        "auth.token.dual-write=false",
        "auth.token.read-source=REDIS"
})
class RedisOnlyTokenBenchmarkTest extends TokenStoreBenchmarkSupport {
}
