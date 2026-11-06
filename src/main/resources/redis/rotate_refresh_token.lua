-- Refresh Token 회전을 원자적으로 처리한다.
-- KEYS[1] used_token:{old}      KEYS[2] token_mapping:{old}
-- KEYS[3] token_mapping:{new}   KEYS[4] refresh_token:{userId}
-- ARGV[1] userId  ARGV[2] newToken  ARGV[3] refreshTtlSec  ARGV[4] usedTtlSec
-- 반환: 1 = 회전 성공, 0 = 이미 사용된 토큰(동시 요청으로 먼저 회전됨)

if redis.call('EXISTS', KEYS[1]) == 1 then
    return 0
end

redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[4])
redis.call('DEL', KEYS[2])
redis.call('SET', KEYS[3], ARGV[1], 'EX', ARGV[3])
redis.call('SET', KEYS[4], ARGV[2], 'EX', ARGV[3])
return 1
