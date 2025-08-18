# 팀원 추천 API 스펙 정의

## 📋 API 개요

팀장이 프로젝트에 적합한 팀원을 추천받는 기능

### 🔗 엔드포인트
```
POST /api/recommend/members
```

## 📥 Request 스펙

### TypeScript Interface
```typescript
interface RecommendMemberRequest {
  projectId: number;                    // 프로젝트 ID
  requiredSkills: string[];             // 필요한 기술스택 목록
  teamSize?: number;                    // 추천받을 팀원 수 (기본값: 5)
  experienceLevel?: 'beginner' | 'intermediate' | 'advanced' | 'any';  // 경험 수준
}
```

### JSON 예시
```json
{
  "projectId": 1,
  "requiredSkills": ["React", "TypeScript", "Node.js"],
  "teamSize": 5,
  "experienceLevel": "intermediate"
}
```

## 📤 Response 스펙

### TypeScript Interface
```typescript
interface MemberExplanation {
  main_reason: string;                  // 주요 추천 이유
  detailed_reasons: string[];           // 상세 이유 목록
  matched_skills: string[];             // 매칭된 기술스택
  growth_opportunities: string[];       // 성장 가능한 기술
  simple_explanation: string;           // 간단한 한 줄 설명
  experience_match: string;             // 경험 수준 매칭 설명
}

interface RecommendedMember {
  userId: number;                       // 사용자 ID
  name: string;                         // 사용자 이름
  email?: string;                       // 이메일 (권한에 따라)
  profileImage?: string;                // 프로필 이미지
  matchScore: number;                   // 매칭 점수 (0-1)
  
  // 기술 정보
  userTechStacks: Array<{
    name: string;
    level: number;
  }>;
  
  // 추천 설명
  explanation: MemberExplanation;
  
  // 추가 정보
  experience: string;                   // 경력 (예: "2년")
  portfolioCount: number;               // 포트폴리오 프로젝트 수
  completedProjects: number;           // 완료한 프로젝트 수
  averageRating?: number;              // 평균 평점 (있는 경우)
  lastActiveDate: string;              // 마지막 활동일
  
  // 참여 가능성
  isAvailable: boolean;                // 현재 참여 가능 여부
  currentProjectCount: number;         // 현재 참여 중인 프로젝트 수
}

// API 응답
type RecommendMembersResponse = RecommendedMember[];
```

### JSON 응답 예시
```json
[
  {
    "userId": 15,
    "name": "김개발",
    "email": "kim@example.com",
    "profileImage": "/images/profiles/kim.jpg",
    "matchScore": 0.87,
    "userTechStacks": [
      {"name": "React", "level": 4},
      {"name": "TypeScript", "level": 3},
      {"name": "Java", "level": 5}
    ],
    "explanation": {
      "main_reason": "React와 TypeScript 경험이 프로젝트와 87% 일치합니다",
      "detailed_reasons": [
        "React 4년 경험으로 프론트엔드 개발에 적합",
        "TypeScript 활용 경험으로 안정적인 코드 작성 가능"
      ],
      "matched_skills": ["React", "TypeScript"],
      "growth_opportunities": ["Node.js"],
      "simple_explanation": "React + TypeScript 전문가로 프론트엔드 리드 가능",
      "experience_match": "중급 이상의 숙련된 개발자입니다"
    },
    "experience": "4년",
    "portfolioCount": 8,
    "completedProjects": 12,
    "averageRating": 4.6,
    "lastActiveDate": "2025-08-15",
    "isAvailable": true,
    "currentProjectCount": 1
  }
]
```

## 🔐 권한 관리

### Spring Boot 권한 확인
```java
// 프로젝트 소유자 또는 관리자만 접근 가능
@PreAuthorize("@projectService.isProjectOwner(#request.projectId, authentication.name)")
```

## 📊 매칭 알고리즘 고려사항

1. **기술스택 매칭**: 필요 기술과 사용자 기술의 유사도
2. **경험 수준**: 프로젝트 복잡도와 사용자 경험의 적합성
3. **가용성**: 현재 참여 중인 프로젝트 수, 활동 상태
4. **성과**: 과거 프로젝트 완료율, 평점
5. **학습 잠재력**: 성장 가능한 기술스택 보유 여부

## 🎯 프론트엔드에서 사용할 주요 데이터

```vue
<!-- 핵심 표시 정보 -->
<div class="member-card">
  <img :src="member.profileImage" />
  <h3>{{ member.name }}</h3>
  <div class="match-score">{{ Math.round(member.matchScore * 100) }}% 매칭</div>
  <p class="explanation">{{ member.explanation.simple_explanation }}</p>
  
  <div class="skills">
    <span v-for="skill in member.explanation.matched_skills" class="skill-tag matched">
      ✅ {{ skill }}
    </span>
    <span v-for="skill in member.explanation.growth_opportunities" class="skill-tag growth">
      🌱 {{ skill }}
    </span>
  </div>
  
  <div class="stats">
    <span>경력: {{ member.experience }}</span>
    <span>완료: {{ member.completedProjects }}개</span>
    <span v-if="member.averageRating">평점: ⭐{{ member.averageRating }}</span>
  </div>
  
  <button :disabled="!member.isAvailable" class="invite-btn">
    {{ member.isAvailable ? '팀 초대' : '참여 불가' }}
  </button>
</div>
```