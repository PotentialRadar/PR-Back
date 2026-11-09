-- Refresh Token 회전을 원자적으로 처리한다. 판정(재사용/유예)과 회전을 한 스크립트에서 끝낸다.
-- KEYS[1] used_token:{old}      KEYS[2] token_mapping:{old}
-- KEYS[3] token_mapping:{new}   KEYS[4] refresh_token:{userId}
-- KEYS[5] reuse_grace:{old}
-- ARGV[1] userId  ARGV[2] newToken  ARGV[3] refreshTtlSec  ARGV[4] usedTtlSec  ARGV[5] graceSec
-- 반환: {판정, 돌려줄 Refresh Token}
--   ROTATED        처음 쓰는 토큰. newToken 으로 회전
--   GRACE_HIT      유예 안에 다시 온 직전 토큰이고, 사용자의 현재 토큰이 그때 발급한 토큰 그대로.
--                  새로 발급하지 않고 그 토큰을 돌려준다(토큰 체인이 갈라지지 않음)
--   REUSE_DETECTED 유예가 지났거나, 그 사이 다음 토큰으로 이미 회전됐거나, 로그아웃/무효화된 상태

if redis.call('EXISTS', KEYS[1]) == 1 then
    local graced = redis.call('GET', KEYS[5])
    local current = redis.call('GET', KEYS[4])
    if graced and current and graced == current then
        return {'GRACE_HIT', graced}
    end
    return {'REUSE_DETECTED', ''}
end

redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[4])
redis.call('DEL', KEYS[2])
redis.call('SET', KEYS[3], ARGV[1], 'EX', ARGV[3])
redis.call('SET', KEYS[4], ARGV[2], 'EX', ARGV[3])
if tonumber(ARGV[5]) > 0 then
    redis.call('SET', KEYS[5], ARGV[2], 'EX', ARGV[5])
end
return {'ROTATED', ARGV[2]}
