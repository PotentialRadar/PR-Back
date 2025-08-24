package com.potential_radar.PR.notification.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class EmitterRepositoryImpl implements EmitterRepository {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();


    // 새로운 sseEmitter 객체를 emitters에 저장
    @Override
    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    // 지정된 emitterId에 해당하는 이벤트를 eventCache에 저장
    @Override
    public void saveEventCache(String emitterId, Object event) {
        eventCache.put(emitterId, event);
    }

    // 특정 memberId로 시작하는 모든 emitterId를 가진 SseEmitter 객체를 찾아 Map 형태로 반환
    @Override
    public Map<String, SseEmitter> findAllEmitterStartWithByMemberId(String memberId) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(memberId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // 특정 memberId로 시작하는 모든 emitterId를 가진 이벤트 캐시 객체를 찾아 Map 형태로 반환
    @Override
    public Map<String, Object> findAllEventCacheStartWithByMemberId(String memberId) {
        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(memberId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // 주어진 emitterId에 해당하는 SseEmitter를 emitters에서 제거
    @Override
    public void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    // 특정 memberId로 시작하는 모든 emitterId를 사진 SseEmitter 객체들을 emitters 맵에서 제거
    @Override
    public void deleteAllEmitterStartWithId(String memberId) {
        emitters.keySet().stream()
                .filter(key -> key.startsWith(memberId))
                .collect(Collectors.toList()) // ConcurrentModificationException 방지를 위해 리스트로 수집 후 제거
                .forEach(emitters::remove);
    }

    // 특정 memberId로 시작하는 모든 emitterId를 가진 이벤트 캐시 객체들을 eventCache 맵에서 제거
    @Override
    public void deleteAllEventCacheStartWithId(String memberId) {
        // ConcurrentModificationException 방지를 위해 키를 먼저 수집 후 제거
        List<String> keysToRemove = eventCache.keySet().stream()
                .filter(key -> key.startsWith(memberId))
                .collect(Collectors.toList());
        
        keysToRemove.forEach(eventCache::remove);
    }

    @Override
    public Map<String, SseEmitter> findAll() {
        return new ConcurrentHashMap<>(emitters);
    }


}
