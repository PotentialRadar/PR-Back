-- Refresh Token 회전을 원자적으로 처리한다. (유예 처리 포함)
-- KEYS[1] used_token:{old}      KEYS[2] token_mapping:{old}
-- KEYS[3] token_mapping:{new}   KEYS[4] refresh_token:{userId}
-- KEYS[5] rotation_grace:{old}
-- ARGV[1] userId (직렬화된 값, token_mapping 값과 동일한 바이트)
-- ARGV[2] RefreshTokenInfo (직렬화된 JSON)
-- ARGV[3] refreshTtlSec  ARGV[4] usedTtlSec
-- ARGV[5] newToken (원문)  ARGV[6] gracePeriodMs (0이면 유예 없음)
-- 반환:  'OK'            = 회전 성공 (새 토큰 = ARGV[5])
--        'GRACE:<token>' = 유예 시간 안의 재요청 → 직전에 발급한 토큰을 그대로 돌려줌
--        'REUSED'        = 이미 사용된 토큰 + 유예 불가 → 재사용 공격으로 간주
--        'INVALID'       = 매핑이 없거나 소유자가 다름 (만료/무효화된 토큰)

if redis.call('EXISTS', KEYS[1]) == 1 then
    -- 유예 조건: 유예 키가 살아 있고, 그 토큰이 아직 사용자의 현재 토큰일 때만
    -- (그 사이 한 번 더 회전됐거나 무효화됐다면 유예하지 않는다)
    local graceToken = redis.call('GET', KEYS[5])
    if graceToken then
        local current = redis.call('GET', KEYS[4])
        if current then
            local ok, info = pcall(cjson.decode, current)
            if ok and type(info) == 'table' and info.token == graceToken then
                return 'GRACE:' .. graceToken
            end
        end
    end
    return 'REUSED'
end

local owner = redis.call('GET', KEYS[2])
if (not owner) or owner ~= ARGV[1] then
    return 'INVALID'
end

redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[4])
redis.call('DEL', KEYS[2])
redis.call('SET', KEYS[3], ARGV[1], 'EX', ARGV[3])
redis.call('SET', KEYS[4], ARGV[2], 'EX', ARGV[3])
if tonumber(ARGV[6]) > 0 then
    redis.call('SET', KEYS[5], ARGV[5], 'PX', ARGV[6])
end
return 'OK'
