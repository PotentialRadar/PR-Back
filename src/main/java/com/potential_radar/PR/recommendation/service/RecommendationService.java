package com.potential_radar.PR.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential_radar.PR.recommendation.dto.RecommendRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedProjectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

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
    public List<RecommendedProjectResponse> getRecommendedProjectsForUser(RecommendRequest request) {
        System.out.println("Python 추천 API 호출 중: " + pythonApiHost + "/api/recommend/projects");

        try {
            String jsonBody = objectMapper.writeValueAsString(request);
            System.out.println("전송 JSON 바디: " + jsonBody);

            Flux<RecommendedProjectResponse> projectsFlux = webClient.post()
                    .uri("/api/recommend/projects")
                    .contentType(MediaType.APPLICATION_JSON) // 요청 Body의 Content-Type 설정
                    .bodyValue(request) // 요청 Body에 RecommendRequest 객체 전달
                    .retrieve()
                    .bodyToFlux(RecommendedProjectResponse.class)
                    .onErrorResume(e -> {
                        System.err.println("Python 추천 API 호출 실패: " + e.getMessage());
                        return Flux.empty();
                    });

            return projectsFlux.collectList().block();
        } catch (Exception e) {
            System.err.println("추천 서비스에서 예외 발생: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
