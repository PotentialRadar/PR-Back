package com.potential_radar.PR.user.token;

import org.springframework.boot.test.context.SpringBootTest;

/** 1단계 - 이중 쓰기, DB 읽기 (전환 전 기준선) */
@SpringBootTest(properties = {
        TokenStoreBenchmarkSupport.COMMON_PROPS,
        "auth.token.dual-write=true",
        "auth.token.read-source=DB"
})
class DbReadTokenBenchmarkTest extends TokenStoreBenchmarkSupport {
}
