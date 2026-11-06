package com.potential_radar.PR.user.token;

import com.potential_radar.PR.user.service.token.DbRefreshTokenStore;
import com.potential_radar.PR.user.service.token.MigratingRefreshTokenStore;
import com.potential_radar.PR.user.service.token.RedisRefreshTokenStore;
import com.potential_radar.PR.user.service.token.TokenStoreProperties;
import com.potential_radar.PR.user.service.token.TokenStoreProperties.ReadSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MigratingRefreshTokenStoreTest {

    private static final Duration TTL = Duration.ofDays(14);

    private final DbRefreshTokenStore db = mock(DbRefreshTokenStore.class);
    private final RedisRefreshTokenStore redis = mock(RedisRefreshTokenStore.class);

    private MigratingRefreshTokenStore store(boolean dualWrite, ReadSource readSource) {
        TokenStoreProperties props = new TokenStoreProperties();
        props.setDualWrite(dualWrite);
        props.setReadSource(readSource);
        return new MigratingRefreshTokenStore(db, redis, props);
    }

    @Test
    void stage1_DB읽기는_Redis를_조회하지_않는다() {
        when(db.findUserIdByToken("t")).thenReturn(Optional.of(1L));

        assertThat(store(true, ReadSource.DB).findUserIdByToken("t")).contains(1L);
        verifyNoInteractions(redis);
    }

    @Test
    void 이중쓰기는_DB와_Redis에_모두_저장한다() {
        store(true, ReadSource.REDIS).save(1L, "t", TTL);

        verify(db).save(1L, "t", TTL);
        verify(redis).save(1L, "t", TTL);
    }

    @Test
    void stage2_Redis에_없으면_DB로_폴백하고_카운트한다() {
        when(redis.findOwnerOfUsedToken("t")).thenReturn(Optional.empty());
        when(redis.findUserIdByToken("t")).thenReturn(Optional.empty());
        when(db.findUserIdByToken("t")).thenReturn(Optional.of(1L));
        MigratingRefreshTokenStore store = store(true, ReadSource.REDIS);

        assertThat(store.findUserIdByToken("t")).contains(1L);
        assertThat(store.getFallbackCount()).isEqualTo(1);
    }

    @Test
    void stage2_Redis에_있으면_DB를_조회하지_않는다() {
        when(redis.findOwnerOfUsedToken("t")).thenReturn(Optional.empty());
        when(redis.findUserIdByToken("t")).thenReturn(Optional.of(1L));
        MigratingRefreshTokenStore store = store(true, ReadSource.REDIS);

        assertThat(store.findUserIdByToken("t")).contains(1L);
        verify(db, never()).findUserIdByToken(any());
        assertThat(store.getFallbackCount()).isZero();
    }

    @Test
    void stage3_Redis에_없으면_폴백하지_않는다() {
        when(redis.findOwnerOfUsedToken("t")).thenReturn(Optional.empty());
        when(redis.findUserIdByToken("t")).thenReturn(Optional.empty());

        assertThat(store(false, ReadSource.REDIS).findUserIdByToken("t")).isEmpty();
        verifyNoInteractions(db);
    }

    @Test
    void 재사용된_토큰은_양쪽_저장소에서_사용자_토큰을_모두_무효화한다() {
        when(redis.findOwnerOfUsedToken("old")).thenReturn(Optional.of(7L));

        assertThat(store(true, ReadSource.REDIS).findUserIdByToken("old")).isEmpty();
        verify(db).deleteAllForUser(7L);
        verify(redis).deleteAllForUser(7L);
    }

    @Test
    void 이중쓰기중_Redis_쓰기실패는_요청을_실패시키지_않는다() {
        doThrow(new QueryTimeoutException("down")).when(redis).save(any(), any(), any());

        store(true, ReadSource.REDIS).save(1L, "t", TTL);
        verify(db).save(1L, "t", TTL);
    }

    @Test
    void stage3_Redis_쓰기실패는_그대로_올린다() {
        doThrow(new QueryTimeoutException("down")).when(redis).save(any(), any(), any());

        assertThatThrownBy(() -> store(false, ReadSource.REDIS).save(1L, "t", TTL))
                .isInstanceOf(QueryTimeoutException.class);
    }

    @Test
    void 이중쓰기중_Redis_조회실패는_DB로_폴백한다() {
        when(redis.findOwnerOfUsedToken("t")).thenThrow(new QueryTimeoutException("down"));
        when(db.findUserIdByToken("t")).thenReturn(Optional.of(1L));

        assertThat(store(true, ReadSource.REDIS).findUserIdByToken("t")).contains(1L);
    }

    @Test
    void Redis_회전이_거부되면_DB도_갱신하지_않는다() {
        when(redis.rotate(1L, "old", "new", TTL)).thenReturn(false);

        assertThat(store(true, ReadSource.REDIS).rotate(1L, "old", "new", TTL)).isFalse();
        verify(db, never()).rotate(any(), any(), any(), any());
    }

    @Test
    void DB읽기와_단독쓰기_조합은_기동시_거부된다() {
        TokenStoreProperties props = new TokenStoreProperties();
        props.setDualWrite(false);
        props.setReadSource(ReadSource.DB);

        assertThatThrownBy(() -> {
            var m = TokenStoreProperties.class.getDeclaredMethod("validate");
            m.setAccessible(true);
            m.invoke(props);
        }).hasRootCauseInstanceOf(IllegalStateException.class);
    }
}
