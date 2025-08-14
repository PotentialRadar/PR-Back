package com.potential_radar.PR.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential_radar.PR.recommendation.dto.RecommendRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedProjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class RecommendationService {

    private final WebClient webClient;
    private final String pythonApiHost;
    // 클래스 멤버에 ObjectMapper 추가
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecommendationService(WebClient.Builder webClientBuilder,
                                 @Value("${python.api.host:http://localhost:8000}") String pythonApiHost) {
        this.pythonApiHost = pythonApiHost;
        this.webClient = webClientBuilder.baseUrl(pythonApiHost).build();
    }

    /**
     * Python API를 호출하여 사용자에게 적합한 프로젝트를 추천
     * WebClient를 사용하여 비동기적으로 POST 요청을 보냄
     *
     * @param request 사용자 정보가 담긴 RecommendRequest 객체
     * @return 추천된 프로젝트 목록 (API 호출 실패 시 빈 리스트 반환)
     */
    public List<RecommendedProjectResponse> getRecommendedProjectsForUser(
            RecommendRequest request,
            int topN,
            double minScore,
            double minOverlap,
            boolean strict
    ) {
        log.info("[SPRING->AI] strict={} topN={} minScore={} minOverlap={}",
                strict, topN, minScore, minOverlap);

        try {
            return webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/recommend/projects")
                            .queryParam("topN", topN)
                            .queryParam("minScore", minScore)
                            .queryParam("minOverlap", minOverlap)
                            .queryParam("strict", strict)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(RecommendedProjectResponse.class)
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            log.error("FastAPI 응답 오류: {}", e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("추천 서비스 예외: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<RecommendedProjectResponse> callFastApi(RecommendRequest request) {
        try {
            return webClient.post()
                    .uri("/api/recommend/projects")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(RecommendedProjectResponse.class)
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            log.error("FastAPI 응답 오류: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

}
