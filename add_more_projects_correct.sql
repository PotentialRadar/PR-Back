-- 추가 프로젝트 데이터 삽입 (정확한 테이블 구조에 맞춤)

-- 1. 먼저 프로젝트 기본 정보 삽입
INSERT INTO project_recruitment (title, description, status, team_leader_id, recruit_count, view_count, recruit_deadline, start_date, end_date, created_at, updated_at) VALUES

-- Backend 프로젝트들
('마이크로서비스 아키텍처 구축', 'Spring Boot와 Docker를 활용한 마이크로서비스 시스템 개발', 'RECRUITING', 4, 5, 89, '2024-04-01', '2024-04-15', '2024-08-15', NOW(), NOW()),
('GraphQL API 서버 개발', 'Node.js와 GraphQL을 이용한 유연한 API 서버 구축', 'RECRUITING', 9, 3, 67, '2024-03-25', '2024-04-10', '2024-07-10', NOW(), NOW()),
('실시간 채팅 서버', 'WebSocket을 활용한 실시간 채팅 시스템 개발', 'IN_PROGRESS', 1, 4, 124, '2024-03-20', '2024-03-30', '2024-06-30', NOW(), NOW()),
('Django REST API', 'Python Django를 이용한 RESTful API 개발', 'RECRUITING', 6, 3, 45, '2024-04-05', '2024-04-20', '2024-07-20', NOW(), NOW()),
('.NET Core 웹 API', 'C# .NET Core를 활용한 고성능 웹 API 개발', 'RECRUITING', 12, 4, 38, '2024-03-30', '2024-04-12', '2024-07-12', NOW(), NOW()),

-- Frontend 프로젝트들
('React 관리자 대시보드', 'React와 TypeScript를 활용한 관리자 패널 개발', 'RECRUITING', 13, 3, 156, '2024-04-02', '2024-04-17', '2024-07-17', NOW(), NOW()),
('Vue.js 전자상거래 사이트', 'Vue.js와 Nuxt.js를 이용한 쇼핑몰 프론트엔드', 'RECRUITING', 14, 4, 98, '2024-03-28', '2024-04-15', '2024-08-15', NOW(), NOW()),
('Next.js 블로그 플랫폼', 'Next.js 13을 활용한 모던 블로그 시스템', 'IN_PROGRESS', 17, 2, 203, '2024-03-15', '2024-03-25', '2024-06-25', NOW(), NOW()),
('Angular 엔터프라이즈 앱', 'Angular를 이용한 대규모 기업용 웹 애플리케이션', 'RECRUITING', 16, 5, 72, '2024-04-08', '2024-04-25', '2024-09-25', NOW(), NOW()),
('Svelte 포트폴리오 사이트', 'Svelte를 활용한 인터랙티브 포트폴리오 웹사이트', 'RECRUITING', 19, 2, 41, '2024-03-22', '2024-04-05', '2024-06-05', NOW(), NOW()),

-- Mobile 프로젝트들
('iOS 피트니스 앱', 'Swift와 SwiftUI를 활용한 운동 관리 앱', 'RECRUITING', 25, 3, 87, '2024-04-03', '2024-04-18', '2024-08-18', NOW(), NOW()),
('Android 음식 배달 앱', 'Kotlin으로 개발하는 배달 서비스 모바일 앱', 'RECRUITING', 26, 4, 145, '2024-03-26', '2024-04-12', '2024-09-12', NOW(), NOW()),
('Flutter 소셜 미디어 앱', 'Flutter를 이용한 크로스플랫폼 SNS 앱 개발', 'IN_PROGRESS', 28, 3, 267, '2024-03-10', '2024-03-20', '2024-07-20', NOW(), NOW()),
('React Native 금융 앱', 'React Native로 개발하는 모바일 뱅킹 앱', 'RECRUITING', 29, 5, 112, '2024-04-01', '2024-04-20', '2024-10-20', NOW(), NOW()),
('하이브리드 교육 앱', 'Ionic을 활용한 온라인 학습 플랫폼', 'RECRUITING', 32, 3, 58, '2024-03-29', '2024-04-15', '2024-08-15', NOW(), NOW()),

-- DevOps 프로젝트들
('CI/CD 파이프라인 구축', 'Jenkins와 Docker를 활용한 자동화 배포 시스템', 'RECRUITING', 36, 4, 134, '2024-04-04', '2024-04-22', '2024-08-22', NOW(), NOW()),
('AWS 클라우드 마이그레이션', '온프레미스에서 AWS로의 시스템 이전 프로젝트', 'IN_PROGRESS', 35, 6, 189, '2024-03-18', '2024-04-01', '2024-10-01', NOW(), NOW()),
('모니터링 시스템 구축', 'Prometheus와 Grafana를 이용한 인프라 모니터링', 'RECRUITING', 39, 3, 76, '2024-04-06', '2024-04-25', '2024-08-25', NOW(), NOW()),
('멀티 클라우드 관리', 'AWS, GCP, Azure 통합 관리 시스템', 'RECRUITING', 40, 5, 95, '2024-03-31', '2024-04-18', '2024-09-18', NOW(), NOW()),

-- AI/ML 프로젝트들
('이미지 분류 AI 모델', 'TensorFlow를 활용한 딥러닝 이미지 분석 시스템', 'RECRUITING', 41, 4, 158, '2024-04-07', '2024-04-28', '2024-10-28', NOW(), NOW()),
('자연어 처리 챗봇', 'PyTorch와 Transformers를 이용한 대화형 AI', 'IN_PROGRESS', 45, 3, 201, '2024-03-12', '2024-03-28', '2024-08-28', NOW(), NOW()),
('추천 시스템 구축', '머신러닝을 활용한 개인화 추천 엔진 개발', 'RECRUITING', 42, 4, 89, '2024-04-09', '2024-04-30', '2024-11-30', NOW(), NOW()),
('MLOps 플랫폼', 'Kubeflow를 활용한 ML 모델 배포 및 관리 시스템', 'RECRUITING', 46, 5, 127, '2024-04-11', '2024-05-02', '2024-12-02', NOW(), NOW()),

-- Full Stack 프로젝트들
('풀스택 전자상거래', 'React + Spring Boot를 활용한 온라인 쇼핑몰', 'RECRUITING', 47, 6, 234, '2024-04-12', '2024-05-05', '2024-12-05', NOW(), NOW()),
('MEAN 스택 블로그', 'MongoDB, Express, Angular, Node.js로 개발하는 블로그', 'RECRUITING', 48, 4, 167, '2024-04-14', '2024-05-08', '2024-11-08', NOW(), NOW()),
('Django + Vue 커뮤니티', 'Python Django + Vue.js 온라인 커뮤니티 플랫폼', 'IN_PROGRESS', 49, 5, 145, '2024-03-16', '2024-04-02', '2024-10-02', NOW(), NOW()),
('Next.js + NestJS 스타트업', 'Next.js + NestJS로 개발하는 SaaS 플랫폼', 'RECRUITING', 50, 7, 289, '2024-04-16', '2024-05-10', '2025-01-10', NOW(), NOW()),

-- 혼합 기술 스택 프로젝트들
('게임 개발 플랫폼', 'Unity와 C#을 활용한 멀티플레이어 게임', 'RECRUITING', 12, 8, 178, '2024-04-18', '2024-05-15', '2025-02-15', NOW(), NOW()),
('IoT 모니터링 시스템', 'Go와 React를 활용한 IoT 데이터 수집 시스템', 'RECRUITING', 7, 5, 92, '2024-04-20', '2024-05-18', '2024-12-18', NOW(), NOW()),
('블록체인 DApp', 'Solidity와 React를 이용한 탈중앙화 애플리케이션', 'RECRUITING', 47, 4, 156, '2024-04-22', '2024-05-20', '2025-01-20', NOW(), NOW()),
('실시간 스트리밍 플랫폼', 'WebRTC와 Node.js를 활용한 라이브 스트리밍', 'RECRUITING', 15, 6, 267, '2024-04-24', '2024-05-25', '2025-03-25', NOW(), NOW());

-- 2. 프로젝트 기술 파트 매핑 (project_tech_part 테이블에 삽입)
-- 이 부분은 프로젝트 ID가 자동생성되므로 별도로 실행해야 함

-- 예시: 프로젝트 ID 56부터 시작한다고 가정 (실제로는 조회 후 확인)
-- Backend 프로젝트들의 기술 파트
INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '마이크로서비스 아키텍처 구축' AND tp.name = 'Backend';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'GraphQL API 서버 개발' AND tp.name = 'Backend';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '실시간 채팅 서버' AND tp.name = 'Backend';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'Django REST API' AND tp.name = 'Backend';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '.NET Core 웹 API' AND tp.name = 'Backend';

-- Frontend 프로젝트들의 기술 파트
INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'React 관리자 대시보드' AND tp.name = 'Frontend';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'Vue.js 전자상거래 사이트' AND tp.name = 'Frontend';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'Next.js 블로그 플랫폼' AND tp.name = 'Frontend';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'Angular 엔터프라이즈 앱' AND tp.name = 'Frontend';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'Svelte 포트폴리오 사이트' AND tp.name = 'Frontend';

-- Mobile 프로젝트들의 기술 파트  
INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'iOS 피트니스 앱' AND tp.name = 'Mobile';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'Android 음식 배달 앱' AND tp.name = 'Mobile';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'Flutter 소셜 미디어 앱' AND tp.name = 'Mobile';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'React Native 금융 앱' AND tp.name = 'Mobile';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '하이브리드 교육 앱' AND tp.name = 'Mobile';

-- DevOps 프로젝트들의 기술 파트
INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'CI/CD 파이프라인 구축' AND tp.name = 'DevOps';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'AWS 클라우드 마이그레이션' AND tp.name = 'DevOps';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '모니터링 시스템 구축' AND tp.name = 'DevOps';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '멀티 클라우드 관리' AND tp.name = 'DevOps';

-- AI/ML 프로젝트들의 기술 파트
INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '이미지 분류 AI 모델' AND tp.name = 'AI/ML';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '자연어 처리 챗봇' AND tp.name = 'AI/ML';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '추천 시스템 구축' AND tp.name = 'AI/ML';

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'MLOps 플랫폼' AND tp.name = 'AI/ML';

-- Full Stack 프로젝트들의 기술 파트 (여러 개)
INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '풀스택 전자상거래' AND tp.name IN ('Frontend', 'Backend');

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'MEAN 스택 블로그' AND tp.name IN ('Frontend', 'Backend');

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'Django + Vue 커뮤니티' AND tp.name IN ('Frontend', 'Backend');

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'Next.js + NestJS 스타트업' AND tp.name IN ('Frontend', 'Backend');

-- 혼합 프로젝트들의 기술 파트
INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '게임 개발 플랫폼' AND tp.name IN ('Mobile', 'Backend');

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = 'IoT 모니터링 시스템' AND tp.name IN ('Backend', 'Frontend', 'DevOps');

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '블록체인 DApp' AND tp.name IN ('Frontend', 'Backend');

INSERT INTO project_tech_part (project_id, tech_part_id)
SELECT pr.project_id, tp.tech_part_id 
FROM project_recruitment pr, tech_part tp
WHERE pr.title = '실시간 스트리밍 플랫폼' AND tp.name IN ('Frontend', 'Backend');

-- 프로젝트 기술 스택 매핑은 너무 많아서 주요한 것들만 추가
-- 마이크로서비스 프로젝트 기술 스택
INSERT INTO project_tech_stack (project_id, tech_stack_id)
SELECT pr.project_id, ts.tech_stack_id 
FROM project_recruitment pr, tech_stack ts
WHERE pr.title = '마이크로서비스 아키텍처 구축' AND ts.name IN ('Java', 'Spring Boot', 'Docker', 'Kubernetes');

-- React 대시보드 프로젝트 기술 스택
INSERT INTO project_tech_stack (project_id, tech_stack_id)
SELECT pr.project_id, ts.tech_stack_id 
FROM project_recruitment pr, tech_stack ts
WHERE pr.title = 'React 관리자 대시보드' AND ts.name IN ('React', 'TypeScript');

-- Flutter 프로젝트 기술 스택
INSERT INTO project_tech_stack (project_id, tech_stack_id)
SELECT pr.project_id, ts.tech_stack_id 
FROM project_recruitment pr, tech_stack ts
WHERE pr.title = 'Flutter 소셜 미디어 앱' AND ts.name = 'Flutter';

-- Vue.js 프로젝트 기술 스택
INSERT INTO project_tech_stack (project_id, tech_stack_id)
SELECT pr.project_id, ts.tech_stack_id 
FROM project_recruitment pr, tech_stack ts
WHERE pr.title = 'Vue.js 전자상거래 사이트' AND ts.name = 'Vue.js';

-- Python AI 프로젝트 기술 스택
INSERT INTO project_tech_stack (project_id, tech_stack_id)
SELECT pr.project_id, ts.tech_stack_id 
FROM project_recruitment pr, tech_stack ts
WHERE pr.title = '이미지 분류 AI 모델' AND ts.name IN ('Python', 'TensorFlow');

-- 확인 쿼리
SELECT COUNT(*) as '추가된 프로젝트 수' FROM project_recruitment WHERE project_id > 55;