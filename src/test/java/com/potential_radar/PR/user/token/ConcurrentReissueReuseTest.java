package com.potential_radar.PR.user.token;

import com.potential_radar.PR.common.exception.InvalidTokenException;
import com.potential_radar.PR.search.repository.ProjectSearchRepository;
import com.potential_radar.PR.search.repository.UserSearchRepository;
import com.potential_radar.PR.user.domain.Provider;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.service.TokenService;
import com.potential_radar.PR.user.service.token.TokenStoreProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 재사용 탐지의 오탐(정상인데 막힘)과 미탐(공격인데 통과)을 같이 측정한다.
 *
 * 실행 (로컬 PostgreSQL + Redis 기동 상태에서):
 *   RUN_TOKEN_BENCHMARK=true ./gradlew test --tests '*ConcurrentReissueReuseTest' -i
 * 결과는 콘솔([REUSE_FP], [REUSE_DETECT])과 build/reports/token-reuse-grace.csv 에 남는다.
 * 토큰 로직은 검색과 무관하므로 Elasticsearch 저장소는 목으로 대체한다(ES 없이 실행 가능).
 *
 * 오탐 판정: 두 요청 중 하나라도 거절되거나, 끝난 뒤 사용자의 활성 토큰이 사라졌으면 오탐.
 * 탐지 판정: 옛 토큰 재사용이 거절되고, 사용자의 활성 토큰이 모두 무효화됐으면 탐지.
 */
@EnabledIfEnvironmentVariable(named = "RUN_TOKEN_BENCHMARK", matches = "true")
@SpringBootTest(properties = {
        "auth.token.dual-write=false",
        "auth.token.read-source=REDIS",
        "auth.token.reuse-grace-seconds=" + ConcurrentReissueReuseTest.GRACE_SECONDS
})
class ConcurrentReissueReuseTest {

    static final int TRIALS = 100;
    static final int GRACE_SECONDS = 2;
    static final String CSV = "build/reports/token-reuse-grace.csv";

    @Autowired TokenService tokenService;
    @Autowired UserRepository userRepository;
    @Autowired StringRedisTemplate redis;
    @Autowired TokenStoreProperties props;
    @MockitoBean UserSearchRepository userSearchRepository;
    @MockitoBean ProjectSearchRepository projectSearchRepository;

    private final List<User> users = new ArrayList<>();

    @AfterEach
    void tearDown() {
        users.forEach(u -> tokenService.deleteRefreshToken(u.getUserId()));
        userRepository.deleteAll(users);
    }

    // ---------------------------------------------------------------- 오탐

    /** 두 탭이 같은 Refresh Token으로 같은 순간에 재발급을 요청한다. */
    @Test
    void 오탐_동시_재발급() throws Exception {
        int falsePositive = 0, sameToken = 0;
        long[] gapsNs = new long[TRIALS];
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < TRIALS; i++) {
                User user = newUser();
                String t1 = tokenService.issueRefreshToken(user.getUserId());

                CountDownLatch start = new CountDownLatch(1);
                long[] arrivedAt = new long[2];
                Future<String> a = pool.submit(reissueAt(t1, start, arrivedAt, 0));
                Future<String> b = pool.submit(reissueAt(t1, start, arrivedAt, 1));
                start.countDown();   // 두 요청을 같은 순간에 출발

                String ra = a.get(), rb = b.get();
                if (ra == null || rb == null || loggedOut(user)) falsePositive++;
                else if (ra.equals(rb)) sameToken++;
                gapsNs[i] = Math.abs(arrivedAt[0] - arrivedAt[1]);
            }
        } finally {
            pool.shutdown();
        }

        Arrays.sort(gapsNs);
        report("concurrent", falsePositive, "-", sameToken,
                String.format("arrivalGap p50=%.3fms max=%.3fms", gapsNs[TRIALS / 2 - 1] / 1e6, gapsNs[TRIALS - 1] / 1e6));
    }

    /**
     * 탭 A의 재발급 응답(새 쿠키)이 오기 전에 탭 B가 옛 쿠키로 보낸 요청이 A가 끝난 직후 도착한다.
     * 실제 브라우저에서 더 흔한 순서이며, 이 경우 B는 회전 스크립트가 아니라 조회 단계에서 막힌다.
     */
    @Test
    void 오탐_엇갈린_재발급() throws Exception {
        int falsePositive = 0, sameToken = 0;
        for (int i = 0; i < TRIALS; i++) {
            User user = newUser();
            String t1 = tokenService.issueRefreshToken(user.getUserId());

            String ra = reissue(t1);   // 탭 A 완료
            String rb = reissue(t1);   // 탭 B, 옛 쿠키 그대로
            if (ra == null || rb == null || loggedOut(user)) falsePositive++;
            else if (ra.equals(rb)) sameToken++;
        }
        report("staggered", falsePositive, "-", sameToken, "");
    }

    // ---------------------------------------------------------------- 탐지

    /** A. 유예가 지난 뒤 옛 토큰 재사용 → 탐지돼야 함. 100명을 회전시킨 뒤 한 번만 기다린다. */
    @Test
    void 탐지_유예가_지난_재사용() throws Exception {
        List<User> batch = new ArrayList<>();
        List<String> oldTokens = new ArrayList<>();
        for (int i = 0; i < TRIALS; i++) {
            User user = newUser();
            String t1 = tokenService.issueRefreshToken(user.getUserId());
            reissue(t1);   // T2 발급
            batch.add(user);
            oldTokens.add(t1);
        }
        Thread.sleep((GRACE_SECONDS + 1) * 1000L);

        int detected = 0;
        for (int i = 0; i < TRIALS; i++) {
            if (reissue(oldTokens.get(i)) == null && loggedOut(batch.get(i))) detected++;
        }
        report("reuse_after_grace", "-", detected, 0, "");
    }

    /** B. 유예 안이지만 정상 사용자가 이미 T2로 다시 회전함 → 탐지돼야 함. */
    @Test
    void 탐지_다시_회전된_뒤_재사용() throws Exception {
        int detected = 0;
        for (int i = 0; i < TRIALS; i++) {
            User user = newUser();
            String t1 = tokenService.issueRefreshToken(user.getUserId());
            String t2 = reissue(t1);
            String t3 = reissue(t2);   // 현재 토큰 = T3

            // C. 탐지 후 정상 사용자의 최신 토큰(T3)도 무효화됐는지까지 확인
            if (reissue(t1) == null && loggedOut(user) && reissue(t3) == null) detected++;
        }
        report("reuse_after_rerotate", "-", detected, 0, "");
    }

    // ---------------------------------------------------------------- helpers

    private Callable<String> reissueAt(String token, CountDownLatch start, long[] arrivedAt, int idx) {
        return () -> {
            start.await();
            arrivedAt[idx] = System.nanoTime();
            return reissue(token);
        };
    }

    /** 재발급 성공 시 새 Refresh Token, 거절되면 null. */
    private String reissue(String token) {
        try {
            return tokenService.refreshTokenPair(token).getRefreshToken();
        } catch (InvalidTokenException e) {
            return null;
        }
    }

    private boolean loggedOut(User user) {
        return redis.opsForValue().get("refresh_token:" + user.getUserId()) == null;
    }

    private User newUser() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .email("reuse-" + unique + "@example.com")
                .nickname("reuse-" + unique)
                .provider(Provider.EMAIL)
                .build());
        users.add(user);
        return user;
    }

    private void report(String caseName, Object falsePositive, Object detected, int sameToken, String extra)
            throws IOException {
        String phase = System.getenv().getOrDefault("REUSE_PHASE", "-");   // before / after
        String grace = String.valueOf(props.getReuseGraceSeconds());
        System.out.printf("%n[REUSE_RESULT] phase=%s case=%s grace=%ss trials=%d falsePositive=%s detected=%s sameToken=%d %s%n%n",
                phase, caseName, grace, TRIALS, falsePositive, detected, sameToken, extra);

        Path out = Path.of(CSV);
        Files.createDirectories(out.getParent());
        if (Files.notExists(out)) {
            Files.writeString(out, "timestamp,phase,case,grace_seconds,trials,false_positive,detected,same_token\n");
        }
        Files.writeString(out, String.format("%s,%s,%s,%s,%d,%s,%s,%d%n",
                LocalDateTime.now(), phase, caseName, grace, TRIALS, falsePositive, detected, sameToken),
                StandardOpenOption.APPEND);
    }
}
