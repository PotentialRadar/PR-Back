# 🎯 Potential Radar (PR)

> **팀 매칭의 새로운 기준을 제시하는 AI 기반 개발자 매칭 플랫폼**

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen?style=for-the-badge&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14-blue?style=for-the-badge&logo=postgresql)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.0-yellow?style=for-the-badge&logo=elasticsearch)
![Redis](https://img.shields.io/badge/Redis-7.0-red?style=for-the-badge&logo=redis)
![AWS](https://img.shields.io/badge/AWS-S3-orange?style=for-the-badge&logo=amazon-aws)

</div>

## 📋 개요

**Potential Radar(PR)**는 팀 매칭 플랫폼입니다. 프로젝트 리더에게는 필요한 기술 스택에 맞는 최적의 팀원을 추천하고, 개발자에게는 자신의 역량을 발휘할 수 있는 맞춤형 프로젝트를 연결합니다.

단순한 팀 프로젝트 매칭을 넘어, **성공적인 프로젝트 완수와 신뢰 기반의 개발자 생태계 조성**을 목표로 합니다.

## ✨ 핵심 특징

### 🤖 AI 기반 양방향 매칭
- **개인화된 추천**: 사용자의 기술 스택, 프로젝트 경험, 동료 평가 데이터를 기반으로 최적의 팀원과 프로젝트를 양방향으로 추천
- **머신러닝 알고리즘**: 과거 프로젝트 성공률과 팀워크 데이터를 학습하여 매칭 정확도 지속 개선

### 🔍 강력한 검색 & 필터링
- **Elasticsearch 기반**: 실시간 검색과 복합 조건 필터링으로 원하는 조건의 프로젝트와 팀원을 빠르고 정확하게 발견
- **다중 검색 조건**: 기술 스택, 경험 수준, 프로젝트 유형, 지역 등 다양한 조건으로 세밀한 검색 가능

### 📊 통합 포트폴리오 관리
- **프로젝트 이력 연동**: 완료된 프로젝트가 자동으로 포트폴리오에 추가
- **평판 시스템**: 팀원 간 상호 평가를 통한 신뢰도 점수 관리
- **성장 추적**: 개발자의 기술 향상과 경험 누적을 시각적으로 표현

### 🔐 소셜 로그인 & 보안
- **OAuth2 통합**: Google, Kakao 소셜 로그인 지원
- **JWT 토큰**: 안전한 인증/인가 시스템
- **Redis 기반**: 토큰 관리 및 세션 보안 강화

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.3
- **Language**: Java 17
- **Security**: Spring Security + OAuth2 + JWT
- **Database**: PostgreSQL (HikariCP 커넥션 풀)
- **Search Engine**: Elasticsearch 8.0
- **Cache**: Redis 7.0
- **Cloud Storage**: AWS S3

### Infrastructure & DevOps
- **Connection Pool**: HikariCP (최적화된 데이터베이스 연결 관리)
- **Email**: Spring Mail (Gmail SMTP)
- **WebSocket**: 실시간 알림 시스템
- **Validation**: Spring Validation

### 외부 연동
- **AI 추천 서비스**: Python API 연동 (FastAPI)
- **OAuth Providers**: Google, Kakao
- **File Storage**: AWS S3 (프로필 이미지, 포트폴리오 파일)

## 🗄 데이터베이스 설계

### 핵심 엔티티
```
User (사용자)
├── UserProfile (프로필)
├── UserTechStack (기술 스택)
├── UserEducation (학력)
├── UserExperience (경력)
└── UserProject (프로젝트 이력)

Project (프로젝트)
├── ProjectTechStack (필요 기술)
├── ProjectTechPart (모집 포지션)
├── ProjectMember (팀원)
├── ProjectApplication (지원)
└── ProjectComment (댓글)

Recommendation (추천)
├── RecommendationHistory (추천 이력)
└── MemberExplanation (추천 근거)
```

## 🚀 시작하기

### 필수 사전 설치
```bash
# Java 17
java --version

# PostgreSQL 14+
psql --version

# Redis 7.0+
redis-server --version

# Elasticsearch 8.0+
elasticsearch --version
```

### 설치 및 실행

1. **저장소 클론**
```bash
git clone https://github.com/PotentialRadar/PR-Back.git
cd PR-Back
```

2. **데이터베이스 설정**
```sql
-- PostgreSQL 데이터베이스 생성
CREATE DATABASE pr;
CREATE USER postgres WITH PASSWORD '1234';
GRANT ALL PRIVILEGES ON DATABASE pr TO postgres;
```

3. **Redis 서버 시작**
```bash
redis-server
```

4. **Elasticsearch 서버 시작**
```bash
# Elasticsearch 실행
elasticsearch
```

5. **애플리케이션 설정**
```bash
# application.yml에서 환경별 설정 확인
# - 데이터베이스 연결 정보
# - Redis 연결 정보
# - Elasticsearch 연결 정보
# - OAuth2 클라이언트 정보
# - AWS S3 설정
```

6. **애플리케이션 실행**
```bash
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 실행됩니다.

## 📡 API 문서

### 인증 API
- `POST /api/users/signup` - 회원가입
- `POST /api/users/login` - 로그인
- `POST /api/auth/token` - 토큰 갱신

### 사용자 API
- `GET /api/users/me` - 내 정보 조회
- `PUT /api/users/profile` - 프로필 수정
- `POST /api/users/tech-stacks` - 기술 스택 추가

### 프로젝트 API
- `GET /api/projects` - 프로젝트 목록 조회
- `POST /api/projects` - 프로젝트 생성
- `POST /api/projects/{id}/apply` - 프로젝트 지원

### 검색 API
- `GET /api/search/projects` - 프로젝트 검색
- `GET /api/search/users` - 사용자 검색

### 추천 API
- `GET /api/recommendations/members` - 팀원 추천
- `GET /api/recommendations/projects` - 프로젝트 추천

## 🏗 아키텍처

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Spring Boot    │    │   PostgreSQL    │
│   (React)       │◄──►│   Backend        │◄──►│   Database      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                    ┌──────────────────┐    ┌─────────────────┐
                    │   Elasticsearch  │    │     Redis       │
                    │   (검색/분석)     │    │   (캐시/세션)    │
                    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                    ┌──────────────────┐    ┌─────────────────┐
                    │   Python API     │    │     AWS S3      │
                    │   (AI 추천)       │    │  (파일 저장)     │
                    └──────────────────┘    └─────────────────┘
```

## 🔧 주요 설정

### 데이터베이스 커넥션 풀 (HikariCP)
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 6
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Elasticsearch 인덱스 설정
```yaml
app:
  search:
    indices:
      default:
        number-of-replicas: 0
        number-of-shards: 1
```

### Redis 캐시 설정
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
```

## 🤝 기여하기

1. 이 저장소를 포크합니다
2. 새 기능 브랜치를 생성합니다 (`git checkout -b feature/amazing-feature`)
3. 변경사항을 커밋합니다 (`git commit -m 'Add amazing feature'`)
4. 브랜치에 푸시합니다 (`git push origin feature/amazing-feature`)
5. Pull Request를 생성합니다

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 👥 팀

**Potential Radar Team** - 개발자 생태계의 새로운 가능성을 탐지합니다.

<div align="center">

| 팀원 | 역할 | 담당 기능 | GitHub |
|------|------|-----------|---------|
| **이휘** | **Backend Developer** | • 프로젝트 전체 라이프사이클 기능 구현<br/>• S3 파일 업로드 기능 구현<br/>• 댓글 및 좋아요 기능 구현 | [![GitHub](https://img.shields.io/badge/GitHub-0630hwi-black?style=flat-square&logo=github)](https://github.com/0630hwi) |
| **김지은** | **Search & Infrastructure** | • Elasticsearch 검색 및 필터링 기능<br/>• Redis 인기 검색어 캐싱<br/>• SSE를 이용한 실시간 알림 | [![GitHub](https://img.shields.io/badge/GitHub-keemzleun-black?style=flat-square&logo=github)](https://github.com/keemzleun) |
| **박준** | **AI & Recommendation** | • AI 기반 매칭 시스템 구현<br/>• 하이브리드 추천 알고리즘<br/>• Spring Boot - FastAPI 연동<br/>• 실시간 피드백 학습 시스템 | [![GitHub](https://img.shields.io/badge/GitHub-myjuniverse-black?style=flat-square&logo=github)](https://github.com/myjuniverse) |
| **임희진** | **Authentication & User** | • 회원가입 및 로그인 기능 구현<br/>• JWT를 이용한 인증/인가 구현<br/>• OAuth2 소셜 로그인 (Google, Kakao)<br/>• 유저 및 포트폴리오 관련 기능 구현 | [![GitHub](https://img.shields.io/badge/GitHub-xeexin-black?style=flat-square&logo=github)](https://github.com/xeexin) |

</div>

### 🎯 팀 역할 분담

#### 🏗 **Backend Core Development**
- **프로젝트 라이프사이클**: 생성 → 지원 → 승인 → 마감 → 리뷰 전 과정
- **파일 관리**: AWS S3 연동 및 파일 업로드/다운로드
- **소셜 기능**: 댓글, 좋아요, 상호작용 시스템

#### 🔍 **Search & Real-time Infrastructure**
- **고성능 검색**: Elasticsearch 기반 실시간 검색 엔진
- **캐싱 시스템**: Redis를 활용한 인기 검색어 및 성능 최적화
- **실시간 알림**: Server-Sent Events(SSE) 기반 알림 시스템

#### 🤖 **AI & Machine Learning**
- **지능형 매칭**: 사용자 선호도 및 프로젝트 특성 기반 추천
- **하이브리드 알고리즘**: 협업 필터링 + 콘텐츠 기반 필터링
- **마이크로서비스**: Spring Boot와 FastAPI 간 효율적 연동
- **학습 시스템**: 사용자 피드백 기반 실시간 모델 개선

#### 🔐 **Security & User Management**
- **인증/인가**: JWT 토큰 기반 보안 시스템 + Redis 세션 관리
- **소셜 로그인**: OAuth2 프로토콜 기반 Google, Kakao 연동
- **사용자 관리**: 회원가입, 프로필 관리, 포트폴리오 시스템
- **이메일 인증**: SMTP 기반 이메일 검증 시스템

---

<div align="center">

**🌟 Star this project if you found it helpful!**

</div>
