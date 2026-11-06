package com.potential_radar.PR.user.token;

import com.potential_radar.PR.user.domain.Provider;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.RefreshTokenRepository;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.service.TokenService;
import com.potential_radar.PR.user.service.token.MigratingRefreshTokenStore;
import com.potential_radar.PR.user.service.token.TokenStoreProperties;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 토큰 저장소 측정 공통 로직. 하위 클래스가 @SpringBootTest(properties=...)로 모드만 바꾼다.
 *
 * 실행 (로컬 PostgreSQL + Redis 기동 상태에서):
 *   RUN_TOKEN_BENCHMARK=true ./gradlew test --tests '*TokenBenchmarkTest' -i
 * 결과는 콘솔과 build/reports/token-benchmark.csv 에 남는다.
 *
 * 지표 A: store.findUserIdByToken() 1회 호출 시간 (워밍업 10회 후 100회, avg/p50/p95)
 * 지표 B: tokenService.refreshTokenPair() 100회 동안 실행된 SQL 수 (Hibernate Statistics)
 */
@EnabledIfEnvironmentVariable(named = "RUN_TOKEN_BENCHMARK", matches = "true")
abstract class TokenStoreBenchmarkSupport {

    static final String COMMON_PROPS = "spring.jpa.properties.hibernate.generate_statistics=true";
    private static final int WARMUP = 10;
    private static final int ITERATIONS = 100;

    @Autowired MigratingRefreshTokenStore store;
    @Autowired TokenService tokenService;
    @Autowired TokenStoreProperties props;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired EntityManagerFactory emf;

    private User user;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        user = userRepository.save(User.builder()
                .email("bench-" + unique + "@example.com")
                .nickname("bench-" + unique)
                .provider(Provider.EMAIL)
                .build());
    }

    @AfterEach
    void tearDown() {
        tokenService.deleteRefreshToken(user.getUserId());
        refreshTokenRepository.findByUserId(user.getUserId()).ifPresent(refreshTokenRepository::delete);
        userRepository.delete(user);
    }

    @Test
    void measure() throws IOException {
        String mode = "dualWrite=" + props.isDualWrite() + ",readSource=" + props.getReadSource();
        long fallbackBefore = store.getFallbackCount();

        // 지표 A: 조회 시간
        String token = tokenService.issueRefreshToken(user.getUserId());
        for (int i = 0; i < WARMUP; i++) store.findUserIdByToken(token);
        long[] samples = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            assertThat(store.findUserIdByToken(token)).contains(user.getUserId());
            samples[i] = System.nanoTime() - start;
        }
        Arrays.sort(samples);
        double avgMs = Arrays.stream(samples).average().orElse(0) / 1e6;
        double p50Ms = samples[ITERATIONS / 2 - 1] / 1e6;
        double p95Ms = samples[(int) (ITERATIONS * 0.95) - 1] / 1e6;

        // 지표 B: 재발급 100회 동안의 SQL 수
        for (int i = 0; i < WARMUP; i++) token = tokenService.refreshTokenPair(token).getRefreshToken();
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        for (int i = 0; i < ITERATIONS; i++) {
            token = tokenService.refreshTokenPair(token).getRefreshToken();
        }
        long sqlCount = stats.getPrepareStatementCount();
        long fallbacks = store.getFallbackCount() - fallbackBefore;

        String line = String.format("%s,%s,%.3f,%.3f,%.3f,%d,%.2f,%d",
                LocalDateTime.now(), mode, avgMs, p50Ms, p95Ms, sqlCount, sqlCount / (double) ITERATIONS, fallbacks);
        System.out.printf("%n[TOKEN_BENCHMARK] %s%n  lookup avg=%.3fms p50=%.3fms p95=%.3fms%n"
                        + "  reissue x%d SQL=%d (%.2f/req), fallback=%d%n%n",
                mode, avgMs, p50Ms, p95Ms, ITERATIONS, sqlCount, sqlCount / (double) ITERATIONS, fallbacks);

        Path out = Path.of("build/reports/token-benchmark.csv");
        Files.createDirectories(out.getParent());
        if (Files.notExists(out)) {
            Files.writeString(out, "timestamp,mode,lookup_avg_ms,lookup_p50_ms,lookup_p95_ms,reissue_sql_total,reissue_sql_per_req,fallback_count\n");
        }
        Files.writeString(out, line + "\n", StandardOpenOption.APPEND);
    }
}
