-- 간단한 프로젝트 데이터 삽입 (사용자 1-5번만 사용)

-- 프로젝트 모집 데이터 (10개, 간단 버전)
INSERT INTO project_recruitment (team_leader_id, title, description, recruit_deadline, start_date, end_date, status, view_count, recruit_count, created_at, updated_at) VALUES 
(1, 'Spring Boot 백엔드 API', 'REST API 개발 프로젝트입니다. Spring Boot 사용합니다.', '2024-03-15', '2024-03-20', '2024-06-20', 'RECRUITING', 45, 3, NOW(), NOW()),
(2, 'React 프론트엔드', 'React로 웹 프론트엔드를 개발합니다.', '2024-03-10', '2024-03-15', '2024-06-15', 'RECRUITING', 32, 2, NOW(), NOW()),
(3, 'Flutter 모바일 앱', 'Flutter로 크로스플랫폼 앱을 만듭니다.', '2024-03-20', '2024-03-25', '2024-07-25', 'RECRUITING', 28, 4, NOW(), NOW()),
(4, 'Python 데이터 분석', 'Python으로 데이터 분석 시스템을 구축합니다.', '2024-03-12', '2024-03-18', '2024-06-18', 'RECRUITING', 67, 3, NOW(), NOW()),
(5, 'Node.js 웹 서버', 'Node.js로 웹 서버를 개발합니다.', '2024-03-08', '2024-03-12', '2024-05-12', 'RECRUITING', 23, 2, NOW(), NOW()),

(1, 'Vue.js 관리자 페이지', 'Vue.js로 관리자 대시보드를 만듭니다.', '2024-03-18', '2024-03-22', '2024-06-22', 'RECRUITING', 89, 2, NOW(), NOW()),
(2, 'React Native 앱', 'React Native로 모바일 앱을 개발합니다.', '2024-03-14', '2024-03-20', '2024-05-20', 'RECRUITING', 56, 3, NOW(), NOW()),
(3, 'Django 웹 애플리케이션', 'Django로 웹 애플리케이션을 구축합니다.', '2024-03-16', '2024-03-21', '2024-06-21', 'RECRUITING', 73, 2, NOW(), NOW()),
(4, 'TypeScript 프로젝트', 'TypeScript로 타입 안전한 애플리케이션을 만듭니다.', '2024-03-11', '2024-03-16', '2024-05-16', 'RECRUITING', 41, 4, NOW(), NOW()),
(5, 'Go 마이크로서비스', 'Go 언어로 마이크로서비스를 개발합니다.', '2024-03-13', '2024-03-18', '2024-07-18', 'RECRUITING', 92, 3, NOW(), NOW());