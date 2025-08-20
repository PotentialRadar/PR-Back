-- AI 추천 테스트를 위한 다양한 기술스택 조합 프로젝트 더미 데이터

-- 기존 데이터 삭제 (필요시)
-- DELETE FROM project_tech_stack;
-- DELETE FROM project_recruitment;

-- 프로젝트 데이터 추가 (17개 프로젝트)
INSERT INTO project_recruitment 
  (project_id, team_leader_id, title, description, recruit_deadline, start_date, end_date, status, file_url, created_at, updated_at, view_count, recruit_count)
VALUES
  (1, 1, 'React 기반 쇼핑몰 개발', 'TypeScript와 React를 활용한 현대적인 이커머스 플랫폼 개발', '2025-08-15', '2025-08-25', '2025-12-15', 'RECRUITING', null, NOW(), NOW(), 45, 3),
  (2, 2, 'Vue3 + Nuxt 포트폴리오 사이트', 'Nuxt3와 Vue3 Composition API를 활용한 개인 포트폴리오 웹사이트', '2025-08-20', '2025-09-01', '2025-11-30', 'RECRUITING', null, NOW(), NOW(), 67, 2),
  (3, 3, 'Django REST API 서버 구축', 'Python Django를 활용한 RESTful API 서버와 관리자 페이지 개발', '2025-08-18', '2025-08-28', '2025-12-01', 'RECRUITING', null, NOW(), NOW(), 89, 4),
  (4, 1, 'Spring Boot 마이크로서비스', 'MSA 아키텍처 기반 Spring Boot 서비스 개발 및 Docker 컨테이너화', '2025-08-22', '2025-09-05', '2026-01-15', 'RECRUITING', null, NOW(), NOW(), 123, 5),
  (5, 2, 'Node.js 실시간 채팅 앱', 'Socket.io와 Express를 활용한 실시간 채팅 애플리케이션', '2025-08-25', '2025-09-10', '2025-12-20', 'RECRUITING', null, NOW(), NOW(), 78, 3),
  (6, 3, 'React Native 투두 앱', 'TypeScript와 React Native를 활용한 크로스플랫폼 모바일 앱', '2025-08-30', '2025-09-15', '2025-11-25', 'RECRUITING', null, NOW(), NOW(), 92, 2),
  (7, 1, 'Flutter 전자상거래 앱', 'Dart와 Flutter를 활용한 모바일 쇼핑 애플리케이션', '2025-09-01', '2025-09-20', '2025-12-30', 'RECRUITING', null, NOW(), NOW(), 105, 4),
  (8, 2, 'FastAPI 비동기 웹서버', 'Python FastAPI를 활용한 고성능 비동기 웹 서버 및 API 개발', '2025-09-05', '2025-09-25', '2026-01-10', 'RECRUITING', null, NOW(), NOW(), 134, 3),
  (9, 3, 'Angular 관리자 대시보드', 'Angular와 TypeScript로 구축하는 기업용 관리자 패널', '2025-09-10', '2025-10-01', '2026-02-28', 'RECRUITING', null, NOW(), NOW(), 67, 5),
  (10, 1, 'SvelteKit 블로그 플랫폼', 'Svelte와 SvelteKit을 활용한 개인 블로그 및 CMS 시스템', '2025-09-12', '2025-10-05', '2025-12-25', 'RECRUITING', null, NOW(), NOW(), 43, 2),
  (11, 2, 'Go 마이크로서비스 API', 'Golang과 Gin 프레임워크를 활용한 고성능 마이크로서비스', '2025-09-15', '2025-10-10', '2026-01-20', 'RECRUITING', null, NOW(), NOW(), 156, 4),
  (12, 3, '.NET Core 웹 애플리케이션', 'C#과 .NET Core를 활용한 엔터프라이즈 웹 애플리케이션', '2025-09-18', '2025-10-15', '2026-02-15', 'RECRUITING', null, NOW(), NOW(), 98, 3),
  (13, 1, 'Laravel CMS 시스템', 'PHP Laravel을 활용한 콘텐츠 관리 시스템 및 API 서버', '2025-09-20', '2025-10-20', '2026-01-30', 'RECRUITING', null, NOW(), NOW(), 87, 4),
  (14, 2, 'Rust 고성능 웹서버', 'Rust와 Actix-web을 활용한 초고속 웹 서버 및 API 개발', '2025-09-25', '2025-11-01', '2026-03-01', 'RECRUITING', null, NOW(), NOW(), 76, 2),
  (15, 3, 'Unity 2D 인디게임', 'Unity와 C#을 활용한 2D 플랫폼 액션 게임 개발', '2025-09-28', '2025-11-05', '2026-02-28', 'RECRUITING', null, NOW(), NOW(), 145, 5),
  (16, 1, 'Kubernetes 클러스터 구축', 'Docker, Kubernetes, Terraform을 활용한 클라우드 인프라 구축', '2025-10-01', '2025-11-10', '2026-03-15', 'RECRUITING', null, NOW(), NOW(), 189, 3),
  (17, 2, 'Python ML 추천시스템', 'TensorFlow와 scikit-learn을 활용한 AI 기반 상품 추천 시스템', '2025-10-05', '2025-11-15', '2026-04-01', 'RECRUITING', null, NOW(), NOW(), 167, 4);

-- 프로젝트별 기술스택 연결 데이터 (올바른 매핑)
INSERT INTO project_tech_stack (project_id, tech_stack_id, recruit_count)
VALUES
  -- 프로젝트 1: React 기반 쇼핑몰 개발
  (1, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'React'), 1),
  (1, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'TypeScript'), 1),
  (1, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Node.js'), 1),
  (1, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Express'), 1),
  (1, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'MongoDB'), 1),
  
  -- 프로젝트 2: Vue3 + Nuxt 포트폴리오 사이트
  (2, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Vue.js'), 1),
  (2, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Nuxt.js'), 1),
  (2, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'TypeScript'), 1),
  (2, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Tailwind CSS'), 1),
  
  -- 프로젝트 3: Django REST API 서버 구축
  (3, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Python'), 2),
  (3, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Django'), 1),
  (3, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'PostgreSQL'), 1),
  (3, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Redis'), 1),
  (3, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Docker'), 1),
  
  -- 프로젝트 4: Spring Boot 마이크로서비스
  (4, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Java'), 2),
  (4, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Spring Boot'), 2),
  (4, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Docker'), 1),
  (4, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Kubernetes'), 1),
  (4, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'PostgreSQL'), 1),
  
  -- 프로젝트 5: Node.js 실시간 채팅 앱
  (5, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Node.js'), 1),
  (5, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Express'), 1),
  (5, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Socket.io'), 1),
  (5, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'MongoDB'), 1),
  (5, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Redis'), 1),
  
  -- 프로젝트 6: React Native 투두 앱
  (6, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'React Native'), 1),
  (6, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'TypeScript'), 1),
  (6, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Expo'), 1),
  (6, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Firebase'), 1),
  
  -- 프로젝트 7: Flutter 전자상거래 앱
  (7, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Flutter'), 2),
  (7, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Dart'), 2),
  (7, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Firebase'), 1),
  (7, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'SQLite'), 1),
  
  -- 프로젝트 8: FastAPI 비동기 웹서버
  (8, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Python'), 1),
  (8, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'FastAPI'), 1),
  (8, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'SQLAlchemy'), 1),
  (8, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'PostgreSQL'), 1),
  (8, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Redis'), 1),
  
  -- 프로젝트 9: Angular 관리자 대시보드
  (9, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Angular'), 2),
  (9, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'TypeScript'), 1),
  (9, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'RxJS'), 1),
  (9, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Material UI'), 1),
  (9, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Chart.js'), 1),
  
  -- 프로젝트 10: SvelteKit 블로그 플랫폼
  (10, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Svelte'), 1),
  (10, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'SvelteKit'), 1),
  (10, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'TypeScript'), 1),
  (10, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Prisma'), 1),
  (10, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'SQLite'), 1),
  
  -- 프로젝트 11: Go 마이크로서비스 API
  (11, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Go'), 2),
  (11, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Gin'), 1),
  (11, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'gRPC'), 1),
  (11, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'PostgreSQL'), 1),
  (11, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Docker'), 1),
  
  -- 프로젝트 12: .NET Core 웹 애플리케이션
  (12, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'C#'), 1),
  (12, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = '.NET Core'), 1),
  (12, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Entity Framework'), 1),
  (12, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'SQL Server'), 1),
  (12, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Azure'), 1),
  
  -- 프로젝트 13: Laravel CMS 시스템
  (13, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'PHP'), 2),
  (13, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Laravel'), 1),
  (13, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'MySQL'), 1),
  (13, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Eloquent ORM'), 1),
  (13, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Blade'), 1),
  
  -- 프로젝트 14: Rust 고성능 웹서버
  (14, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Rust'), 1),
  (14, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Actix-web'), 1),
  (14, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Diesel'), 1),
  (14, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'PostgreSQL'), 1),
  (14, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Redis'), 1),
  
  -- 프로젝트 15: Unity 2D 인디게임
  (15, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Unity'), 2),
  (15, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'C#'), 1),
  (15, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Visual Scripting'), 1),
  (15, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Blender'), 1),
  
  -- 프로젝트 16: Kubernetes 클러스터 구축
  (16, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Docker'), 1),
  (16, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Kubernetes'), 1),
  (16, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Terraform'), 1),
  (16, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'AWS'), 1),
  (16, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Jenkins'), 1),
  
  -- 프로젝트 17: Python ML 추천시스템
  (17, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Python'), 2),
  (17, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'TensorFlow'), 1),
  (17, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'scikit-learn'), 1),
  (17, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'Pandas'), 1),
  (17, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'NumPy'), 1),
  (17, (SELECT tech_stack_id FROM tech_stack WHERE tech_stack_name = 'FastAPI'), 1);

-- 데이터 확인 쿼리
-- SELECT p.project_id, p.title, string_agg(pts.tech_stack_name, ', ') as tech_stacks
-- FROM project_recruitment p
-- LEFT JOIN project_tech_stack pts ON p.project_id = pts.project_id
-- GROUP BY p.project_id, p.title
-- ORDER BY p.project_id;
