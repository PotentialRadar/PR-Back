package com.potential_radar.PR.recommendation.service;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.ProjectTechStack;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.project.repository.ProjectTechStackRepository;
import com.potential_radar.PR.recommendation.dto.RecommendedMemberResponse;
import com.potential_radar.PR.recommendation.dto.RecommendedProjectResponse;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    // 사용자 관련 Repository는 직접 주입받지 않고, Mock 데이터를 사용합니다.
    // private final UserRepository userRepository;
    // private final UserTechStackRepository userTechStackRepository;
    // private final UserProfileRepository userProfileRepository;
    // private final TechCategoryRepository techCategoryRepository;
    // private final TechStackRepository techStackRepository;

    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectTechStackRepository projectTechStackRepository;


    public RecommendationService(ProjectRecruitmentRepository projectRecruitmentRepository,
                                 ProjectTechStackRepository projectTechStackRepository) {
        this.projectRecruitmentRepository = projectRecruitmentRepository;
        this.projectTechStackRepository = projectTechStackRepository;
    }

    /**
     * 특정 사용자(팀원)의 프로필과 기술 스택을 기반으로 참여할 만한 프로젝트를 추천
     * @param userId 프로젝트를 추천받을 사용자의 ID
     * @return 추천된 프로젝트 목록
     */
    public List<RecommendedProjectResponse> getRecommendedProjectsForUser(Long userId) {
        // 1. 사용자(팀원) 정보 및 관련 프로필/기술 스택 조회 (하드코딩된 Mock Data 사용)
        User user = createMockUser(userId); // 하드코딩된 사용자 데이터 생성

        // ERD 기반: UserProfile에서 경력 정보 조회 (Mock UserProfile 사용)
        UserProfile userProfile = user.getUserProfile();

        // ERD 기반: UserTechStack에서 사용자의 기술 스택 및 숙련도 조회 (Mock UserTechStack 사용)
        Map<String, Integer> userTechStackMap = user.getUserTechStacks().stream()
                .collect(Collectors.toMap(
                        uts -> uts.getTechStack().getName(),
                        UserTechStack::getSkillLevel
                ));

        // ERD 기반: 사용자의 주 포지션 (UserTechStack의 TechCategory를 통해 유추)
        PositionType userMainPosition = determineUserMainPosition(user.getUserTechStacks());


        // 2. 모든 활성 프로젝트 조회 (RECRUITING 상태의 프로젝트만)
        List<ProjectRecruitment> allRecruitingProjects = projectRecruitmentRepository.findByStatus(com.potential_radar.PR.project.domain.ProjectStatus.RECRUITING);

        List<RecommendedProjectResponse> recommendedProjects = new ArrayList<>();

        // 3. 각 프로젝트와 사용자의 유사도(매칭도) 계산 및 필터링
        for (ProjectRecruitment project : allRecruitingProjects) {
            // 프로젝트가 요구하는 기술 스택 및 포지션 조회
            List<ProjectTechStack> projectRequiredTechStacks = project.getTechStacks();
            Set<PositionType> projectRequiredPositions = extractProjectRequiredPositions(projectRequiredTechStacks);


            double matchScore = calculateMatchScoreForProject(
                    userTechStackMap, userMainPosition,
                    projectRequiredTechStacks, projectRequiredPositions,
                    userProfile != null ? userProfile.getExperienceRange() : null // 사용자 경력 정보
            );

            if (matchScore > 0.1) { // 매칭 점수가 0.1보다 큰 경우만 추천 (임계치는 조정 가능)
                recommendedProjects.add(convertToRecommendedProjectResponse(project, matchScore));
            }
        }

        // 4. 추천된 프로젝트를 유사도 점수 또는 다른 기준(예: 최신순, 인기순)으로 정렬
        recommendedProjects.sort(Comparator.comparingDouble(RecommendedProjectResponse::getMatchScore).reversed());

        return recommendedProjects;
    }

    /**
     * 특정 프로젝트(팀장)의 구인 요건을 기반으로 적합한 멤버를 추천
     * @param projectId 멤버를 추천받을 프로젝트의 ID
     * @return 추천된 멤버 목록
     */
    public List<RecommendedMemberResponse> getRecommendedMembersForProject(Long projectId) {
        // 1. 프로젝트 정보 및 요구사항 조회
        ProjectRecruitment project = projectRecruitmentRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with ID: " + projectId));

        List<ProjectTechStack> projectRequiredTechStacks = project.getTechStacks();
        Set<PositionType> projectRequiredPositions = extractProjectRequiredPositions(projectRequiredTechStacks);


        // 2. 모든 활성 사용자(팀원) 조회 (하드코딩된 Mock Data 사용)
        List<User> allUsers = createMockUsers(); // 하드코딩된 사용자 목록 생성

        List<RecommendedMemberResponse> recommendedMembers = new ArrayList<>();

        // 3. 각 사용자와 프로젝트의 유사도(매칭도) 계산 및 필터링
        for (User user : allUsers) {
            // 사용자의 프로필 및 기술 스택 정보 조회 (Mock Data 사용)
            UserProfile userProfile = user.getUserProfile();

            Map<String, Integer> userTechStackMap = user.getUserTechStacks().stream()
                    .collect(Collectors.toMap(
                            uts -> uts.getTechStack().getName(),
                            UserTechStack::getSkillLevel
                    ));

            PositionType userMainPosition = determineUserMainPosition(user.getUserTechStacks());


            double matchScore = calculateMatchScoreForMember(
                    projectRequiredTechStacks, projectRequiredPositions,
                    userTechStackMap, userMainPosition,
                    userProfile != null ? userProfile.getExperienceRange() : null, // 사용자 경력 정보
                    user.getReputationScore().doubleValue() // 사용자 평판 점수
            );

            if (matchScore > 0.1) { // 매칭 점수가 0.1보다 큰 경우만 추천
                recommendedMembers.add(convertToRecommendedMemberResponse(user, matchScore));
            }
        }

        // 4. 추천된 멤버를 유사도 점수 또는 다른 기준(예: 평판 점수)으로 정렬
        recommendedMembers.sort(Comparator.comparingDouble(RecommendedMemberResponse::getMatchScore).reversed());

        return recommendedMembers;
    }

    // --- 추천 로직 구현을 위한 헬퍼 메서드 ---

    /**
     * 사용자의 보유 기술 스택 및 숙련도를 기반으로 주 포지션을 결정합니다.
     * (ERD 상 UserProfile에 직접적인 position 필드가 없으므로 UserTechStack의 TechCategory를 활용)
     */
    private PositionType determineUserMainPosition(List<UserTechStack> userTechStacksWithLevel) {
        if (userTechStacksWithLevel == null || userTechStacksWithLevel.isEmpty()) {
            return null; // 기술 스택이 없으면 포지션 결정 불가
        }

        // 각 카테고리별 기술 스택 개수를 세어 가장 많은 카테고리를 주 포지션으로 가정
        Map<PositionType, Long> categoryCounts = userTechStacksWithLevel.stream()
                .map(uts -> uts.getTechStack().getTechCategory().getName()) // TechStack의 TechCategory 이름
                .map(categoryName -> { // String to PositionType Enum 변환
                    try {
                        return PositionType.valueOf(categoryName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null; // 매핑되지 않는 카테고리 이름 처리
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(positionType -> positionType, Collectors.counting()));

        return categoryCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null); // 가장 많은 카테고리가 없으면 null
    }

    /**
     * ProjectTechStack 목록에서 프로젝트가 요구하는 포지션을 추출합니다.
     * ProjectTechStack 엔티티가 TechStack을 참조하고, TechStack이 TechCategory를 참조하는 구조를 활용합니다.
     */
    private Set<PositionType> extractProjectRequiredPositions(List<ProjectTechStack> projectTechStacks) {
        // ProjectTechStack -> TechStack -> TechCategory.name 을 통해 PositionType을 유추
        return projectTechStacks.stream()
                .map(pt -> pt.getTechStack().getTechCategory().getName())
                .map(categoryName -> {
                    try {
                        return PositionType.valueOf(categoryName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 사용자(팀원)와 프로젝트 간의 매칭 점수를 계산하는 로직입니다.
     * 기술 스택 일치도, 포지션 일치도, 경력 적합도 등을 고려합니다.
     */
    private double calculateMatchScoreForProject(Map<String, Integer> userTechStackMap, PositionType userPosition,
                                                 List<ProjectTechStack> projectRequiredTechStacks, Set<PositionType> projectRequiredPositions,
                                                 String userExperienceRange) {
        double score = 0.0;

        // 1. 기술 스택 일치도 (숙련도 반영)
        for (ProjectTechStack projectTech : projectRequiredTechStacks) {
            String techName = projectTech.getTechStack().getName(); // ProjectTechStack이 TechStack을 참조
            if (userTechStackMap.containsKey(techName)) {
                int skillLevel = userTechStackMap.get(techName); // 1-5 레벨
                score += skillLevel * 1.0; // 숙련도가 높을수록 더 높은 점수 (가중치 조정 가능)
            }
        }

        // 2. 포지션 일치도
        if (userPosition != null && projectRequiredPositions.contains(userPosition)) {
            score += 5.0; // 포지션 일치 시 높은 가중치 부여
        }

        // 3. 경력 적합도 (예시: 프로젝트가 특정 경력을 요구하고 사용자가 해당 경력 범위에 있을 경우)
        // ERD 상 ProjectRecruitment에 요구 경력 필드가 없으므로, 이 부분은 추후 확장 가능
        // 현재는 userExperienceRange만 활용
        if (userExperienceRange != null && userExperienceRange.length() > 0) {
            // TODO: 프로젝트의 요구 경력과 사용자의 경력을 비교하는 로직 추가
            // 예: "신입", "주니어", "미들", "시니어" 등으로 경력 구간이 나뉘어 있다면 매칭
            score += 1.0; // 경력 정보가 있다면 기본 점수
        }

        return score;
    }

    /**
     * 프로젝트와 사용자(팀원) 간의 매칭 점수를 계산하는 로직
     * 기술 스택 일치도, 포지션 일치도, 경력 적합도, 평판 등을 고려
     */
    private double calculateMatchScoreForMember(List<ProjectTechStack> projectRequiredTechStacks, Set<PositionType> projectRequiredPositions,
                                                Map<String, Integer> userTechStackMap, PositionType userPosition,
                                                String userExperienceRange, double userReputationScore) {
        double score = 0.0;

        // 1. 기술 스택 일치도 (숙련도 반영)
        for (ProjectTechStack projectTech : projectRequiredTechStacks) {
            String techName = projectTech.getTechStack().getName();
            if (userTechStackMap.containsKey(techName)) {
                int skillLevel = userTechStackMap.get(techName);
                score += skillLevel * 1.0; // 숙련도에 따라 점수 가산
            }
        }

        // 2. 포지션 일치도
        if (userPosition != null && projectRequiredPositions.contains(userPosition)) {
            score += 5.0;
        }

        // 3. 경력 적합도
        if (userExperienceRange != null && userExperienceRange.length() > 0) {
            // TODO: 프로젝트의 요구 경력과 사용자의 경력을 비교하는 로직 추가
            score += 1.0;
        }

        // 4. 평판 점수 반영
        score += userReputationScore * 0.05; // 평판 점수가 높을수록 가중치 (가중치 조정)

        return score;
    }

    /**
     * ProjectRecruitment 엔티티를 RecommendedProjectResponse DTO로 변환합니다.
     */
    private RecommendedProjectResponse convertToRecommendedProjectResponse(ProjectRecruitment project, double matchScore) {
        return new RecommendedProjectResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                matchScore
                // 필요에 따라 모집 마감일, 개발 기간, 팀 리더 정보 등 추가
        );
    }

    /**
     * User 엔티티를 RecommendedMemberResponse DTO로 변환합니다.
     */
    private RecommendedMemberResponse convertToRecommendedMemberResponse(User user, double matchScore) {
        return new RecommendedMemberResponse(
                user.getUserId(),
                user.getUseername(),
                user.getEmail(),
                matchScore
                // 필요에 따라 기술 스택 목록, 포지션, 경력 요약, 프로필 이미지 등 추가
        );
    }

    // --- 하드코딩된 Mock Data 생성 메서드 ---
    // User, UserProfile, UserTechStack, TechCategory, TechStack 엔티티를 이 RecommendationService 내부에서 임시로 정의하여 사용합니다.
    // 실제 DB 연동 시 이 내부 클래스들은 제거하고, 별도로 생성된 엔티티 클래스들을 import하여 사용해야 합니다.

    // Mock User 객체 생성
    private User createMockUser(Long userId) {
        User user = User.builder()
                .userId(userId)
                .email("mockuser" + userId + "@example.com")
                .password("mockpassword")
                .name("가상 사용자 " + userId)
                .nickname("모의유저" + userId)
                .profileImage("https://placehold.co/50x50/aabbcc/ffffff?text=U" + userId)
                .isPortfolioOpen(true)
                .provider(User.Provider.LOCAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .reputationScore(BigDecimal.valueOf(80 + (userId * 2)))
                .reviewCount((int) (userId * 5))
                .build();

        // ERD 기반으로 UserProfile Mocking
        UserProfile userProfile = UserProfile.builder()
                .userProfileId(userId * 100)
                .user(user) // User 객체 연결
                .experienceRange(userId % 3 == 0 ? "신입" : (userId % 3 == 1 ? "주니어" : "시니어"))
                .bio("가상 사용자 " + userId + "의 자기소개입니다.")
                .build();
        user.setUserProfile(userProfile); // User 엔티티에 UserProfile 필드가 있다고 가정

        // ERD 기반으로 TechCategory Mocking
        TechCategory backendCategory = TechCategory.builder().categoryId(1L).name("BACKEND").build();
        TechCategory frontendCategory = TechCategory.builder().categoryId(2L).name("FRONTEND").build();
        TechCategory devopsCategory = TechCategory.builder().categoryId(3L).name("DEVOPS").build();


        // ERD 기반으로 TechStack Mocking
        TechStack javaStack = TechStack.builder().stackId(10L).techCategory(backendCategory).name("Java").build();
        TechStack springStack = TechStack.builder().stackId(11L).techCategory(backendCategory).name("Spring Boot").build();
        TechStack reactStack = TechStack.builder().stackId(20L).techCategory(frontendCategory).name("React").build();
        TechStack vueStack = TechStack.builder().stackId(21L).techCategory(frontendCategory).name("Vue.js").build();
        TechStack dockerStack = TechStack.builder().stackId(30L).techCategory(devopsCategory).name("Docker").build();
        TechStack kubernetesStack = TechStack.builder().stackId(31L).techCategory(devopsCategory).name("Kubernetes").build();


        // ERD 기반으로 UserTechStack Mocking
        List<UserTechStack> userTechStacks = new ArrayList<>();
        if (userId % 2 == 0) { // 짝수 ID 사용자: 백엔드 위주
            userTechStacks.add(UserTechStack.builder().userTechStackId(userId * 1000 + 1).userProfile(userProfile).techStack(javaStack).skillLevel(5).build());
            userTechStacks.add(UserTechStack.builder().userTechStackId(userId * 1000 + 2).userProfile(userProfile).techStack(springStack).skillLevel(4).build());
            userTechStacks.add(UserTechStack.builder().userTechStackId(userId * 1000 + 5).userProfile(userProfile).techStack(dockerStack).skillLevel(3).build());
        } else { // 홀수 ID 사용자: 프론트엔드 위주
            userTechStacks.add(UserTechStack.builder().userTechStackId(userId * 1000 + 3).userProfile(userProfile).techStack(reactStack).skillLevel(5).build());
            userTechStacks.add(UserTechStack.builder().userTechStackId(userId * 1000 + 4).userProfile(userProfile).techStack(vueStack).skillLevel(4).build());
            userTechStacks.add(UserTechStack.builder().userTechStackId(userId * 1000 + 6).userProfile(userProfile).techStack(kubernetesStack).skillLevel(2).build());
        }
        user.setUserTechStacks(userTechStacks); // User 엔티티에 UserTechStack List 필드가 있다고 가정

        return user; // Mock User 반환
    }

    private List<User> createMockUsers() {
        List<User> mockUsers = new ArrayList<>();
        for (long i = 1; i <= 10; i++) { // 10명의 가상 사용자 생성
            mockUsers.add(createMockUser(i));
        }
        return mockUsers;
    }

    // --- ERD 기반으로 추가될 엔티티 클래스 (RecommendationService 내부에서 임시 정의) ---
    // 이 클래스들은 실제 프로젝트의 user.model 또는 user.domain 패키지에 생성되어야 합니다.
    // 여기서는 RecommendationService 내부에서 컴파일 오류를 막기 위한 임시 정의입니다.
    // 실제 파일이 생성되면 이 내부 클래스들은 제거하고 해당 클래스를 import하여 사용해야 합니다.

    // User 엔티티 (ERD 기반, Mocking을 위해 필요한 필드만 정의)
    // 실제 User.java 파일의 내용을 모두 복사해오는 것이 아니라, Mocking에 필요한 최소한의 필드만 정의
    // UserProfile과 UserTechStack 리스트를 가질 수 있도록 필드 추가
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class User {
        private Long userId;
        private String email;
        private String password;
        private String name;
        private String nickname;
        private String profileImage;
        private boolean isPortfolioOpen;
        private Provider provider;
        private String providerUserId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private BigDecimal reputationScore;
        private int reviewCount;

        // Mocking을 위해 추가: UserProfile과 UserTechStack 리스트
        private UserProfile userProfile;
        private List<UserTechStack> userTechStacks = new ArrayList<>();

        public enum Provider {
            LOCAL, KAKAO, GOOGLE
        }
    }

    // TechCategory 엔티티 (ERD 기반)
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TechCategory {
        private Long categoryId;
        private String name; // BACKEND, FRONTEND, DEVOPS
    }

    // TechStack 엔티티 (ERD 기반)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechStack {
        private Long stackId;
        private TechCategory techCategory; // TechCategory와 연관관계
        private String name; // Java, Vue, Docker
    }

    // UserTechStack 엔티티 (ERD 기반)
    @Getter
    @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserTechStack {
        private Long userTechStackId;
        private UserProfile userProfile; // UserProfile과 연관관계
        private TechStack techStack; // TechStack과 연관관계
        private Integer skillLevel; // 1-5
    }

    // UserProfile 엔티티 (ERD 기반)
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserProfile {
        private Long userProfileId;
        private User user; // User와 1:1 관계
        private String experienceRange; // 경력 구간
        private String bio; // 자기소개
    }
}
