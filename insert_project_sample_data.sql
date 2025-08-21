-- 프로젝트 샘플 데이터 삽입

-- 프로젝트 모집 데이터 (20개)
INSERT INTO project_recruitment (team_leader_id, title, description, recruit_deadline, start_date, end_date, status, view_count, recruit_count, created_at, updated_at) VALUES 
-- Backend 중심 프로젝트들
(1, 'E-commerce 백엔드 API 개발', '쇼핑몰 백엔드 REST API를 Spring Boot로 개발할 프로젝트입니다. MSA 아키텍처를 적용하고 싶습니다.', '2024-02-15', '2024-02-20', '2024-05-20', 'RECRUITING', 45, 3, NOW(), NOW()),
(5, '실시간 채팅 서버 구축', 'WebSocket을 활용한 실시간 채팅 서버를 Node.js로 구현합니다. Redis와 Socket.IO를 사용합니다.', '2024-02-10', '2024-02-15', '2024-04-15', 'RECRUITING', 32, 2, NOW(), NOW()),
(13, '금융 데이터 분석 API', 'Python과 FastAPI를 사용해서 주식 데이터 분석 API를 만들어보려고 합니다.', '2024-02-20', '2024-02-25', '2024-06-25', 'RECRUITING', 28, 4, NOW(), NOW()),
(19, '마이크로서비스 플랫폼', 'Docker와 Kubernetes를 활용한 마이크로서비스 플랫폼을 구축합니다.', '2024-02-12', '2024-02-18', '2024-07-18', 'RECRUITING', 67, 5, NOW(), NOW()),
(25, 'IoT 데이터 수집 서버', 'Go언어로 고성능 IoT 데이터 수집 및 처리 서버를 개발합니다.', '2024-02-08', '2024-02-12', '2024-05-12', 'RECRUITING', 23, 3, NOW(), NOW()),

-- Frontend 중심 프로젝트들  
(2, '반응형 포트폴리오 웹사이트', 'React와 TypeScript로 개발자 포트폴리오 웹사이트를 만들어봅시다. 애니메이션과 UX에 집중합니다.', '2024-02-18', '2024-02-22', '2024-04-22', 'RECRUITING', 89, 2, NOW(), NOW()),
(8, 'Vue.js 관리자 대시보드', 'Vue 3 Composition API를 활용한 관리자 대시보드를 개발합니다. Chart.js 연동 포함.', '2024-02-14', '2024-02-20', '2024-05-20', 'RECRUITING', 56, 3, NOW(), NOW()),
(14, 'Next.js 블로그 플랫폼', 'Next.js와 Tailwind CSS로 개인 블로그 플랫폼을 구축합니다. SEO 최적화 포함.', '2024-02-16', '2024-02-21', '2024-06-21', 'RECRUITING', 73, 2, NOW(), NOW()),
(20, 'React Native 웹뷰 하이브리드', 'React로 웹앱을 만들고 React Native WebView로 모바일 앱화 하는 프로젝트입니다.', '2024-02-11', '2024-02-16', '2024-05-16', 'RECRUITING', 41, 4, NOW(), NOW()),
(26, '실시간 협업 툴 프론트엔드', 'WebRTC와 Socket.IO를 활용한 실시간 협업 툴의 프론트엔드를 개발합니다.', '2024-02-13', '2024-02-18', '2024-07-18', 'RECRUITING', 92, 3, NOW(), NOW()),

-- Mobile 중심 프로젝트들
(3, 'Flutter 피트니스 앱', 'Flutter로 운동 기록 및 분석 앱을 개발합니다. Firebase 연동 포함.', '2024-02-17', '2024-02-23', '2024-05-23', 'RECRUITING', 84, 3, NOW(), NOW()),
(9, 'React Native 푸드 딜리버리', 'React Native로 음식 배달 앱을 만들어봅시다. 지도 API와 결제 시스템 연동.', '2024-02-09', '2024-02-14', '2024-06-14', 'RECRUITING', 127, 4, NOW(), NOW()),
(15, 'Kotlin 안드로이드 게임', 'Kotlin으로 2D 퍼즐 게임을 개발합니다. 게임엔진 없이 네이티브로 구현.', '2024-02-19', '2024-02-25', '2024-08-25', 'RECRUITING', 63, 2, NOW(), NOW()),
(21, 'Swift iOS 날씨 앱', 'SwiftUI로 날씨 정보 앱을 만듭니다. Core Data와 Combine 패턴 사용.', '2024-02-07', '2024-02-12', '2024-04-12', 'RECRUITING', 38, 3, NOW(), NOW()),
(27, '크로스플랫폼 여행 일기', 'Flutter로 여행 일기 앱을 개발합니다. 사진, 위치, 메모 기능 포함.', '2024-02-21', '2024-02-26', '2024-07-26', 'RECRUITING', 95, 3, NOW(), NOW()),

-- Full Stack 프로젝트들
(6, '온라인 학습 플랫폼', 'MERN 스택으로 온라인 강의 플랫폼을 구축합니다. 동영상 스트리밍과 결제 시스템 포함.', '2024-02-22', '2024-02-28', '2024-08-28', 'RECRUITING', 156, 6, NOW(), NOW()),
(12, 'SNS 프로젝트', 'Next.js + Node.js + MongoDB로 소셜 네트워크 서비스를 개발합니다.', '2024-02-06', '2024-02-11', '2024-07-11', 'RECRUITING', 203, 5, NOW(), NOW()),
(18, '부동산 중개 플랫폼', 'Vue.js + Django로 부동산 매물 검색 및 중개 플랫폼을 만듭니다.', '2024-02-25', '2024-03-01', '2024-09-01', 'RECRUITING', 78, 4, NOW(), NOW()),
(24, '펫샵 온라인 쇼핑몰', 'React + Spring Boot + MySQL로 반려동물 용품 쇼핑몰을 개발합니다.', '2024-02-05', '2024-02-10', '2024-06-10', 'RECRUITING', 112, 5, NOW(), NOW()),
(30, 'AI 챗봇 서비스', 'Python + React + OpenAI API로 AI 챗봇 서비스를 구축합니다.', '2024-02-28', '2024-03-05', '2024-08-05', 'RECRUITING', 187, 4, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 프로젝트별 기술 스택 할당
INSERT INTO project_tech_stack (project_id, tech_stack_id) VALUES 
-- 프로젝트 1: E-commerce 백엔드 (Java, Spring Boot, MySQL)
(1, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Java' LIMIT 1)),
(1, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Spring Boot' LIMIT 1)),
(1, (SELECT tech_stack_id FROM tech_stack WHERE name = 'MySQL' LIMIT 1)),

-- 프로젝트 2: 포트폴리오 웹사이트 (React, TypeScript, JavaScript)
(2, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1)),
(2, (SELECT tech_stack_id FROM tech_stack WHERE name = 'TypeScript' LIMIT 1)),
(2, (SELECT tech_stack_id FROM tech_stack WHERE name = 'JavaScript' LIMIT 1)),

-- 프로젝트 3: Flutter 피트니스 앱 (Flutter, Dart)
(3, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Flutter' LIMIT 1)),

-- 프로젝트 4: 실시간 채팅 서버 (Node.js, JavaScript)
(4, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Node.js' LIMIT 1)),
(4, (SELECT tech_stack_id FROM tech_stack WHERE name = 'JavaScript' LIMIT 1)),

-- 프로젝트 5: 마이크로서비스 플랫폼 (Docker, Kubernetes, Java)
(5, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Docker' LIMIT 1)),
(5, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Kubernetes' LIMIT 1)),
(5, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Java' LIMIT 1)),

-- 프로젝트 6: 온라인 학습 플랫폼 (React, Node.js, MongoDB)
(6, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1)),
(6, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Node.js' LIMIT 1)),
(6, (SELECT tech_stack_id FROM tech_stack WHERE name = 'MongoDB' LIMIT 1)),

-- 프로젝트 7: Vue.js 관리자 대시보드 (Vue.js, JavaScript)
(7, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Vue.js' LIMIT 1)),
(7, (SELECT tech_stack_id FROM tech_stack WHERE name = 'JavaScript' LIMIT 1)),

-- 프로젝트 8: Next.js 블로그 (React, TypeScript)
(8, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1)),
(8, (SELECT tech_stack_id FROM tech_stack WHERE name = 'TypeScript' LIMIT 1)),

-- 프로젝트 9: React Native 푸드 딜리버리 (React Native, JavaScript)
(9, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React Native' LIMIT 1)),
(9, (SELECT tech_stack_id FROM tech_stack WHERE name = 'JavaScript' LIMIT 1)),

-- 프로젝트 10: React Native 웹뷰 하이브리드 (React, React Native)
(10, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1)),
(10, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React Native' LIMIT 1)),

-- 프로젝트 11: 금융 데이터 분석 API (Python, FastAPI)
(11, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Python' LIMIT 1)),

-- 프로젝트 12: SNS 프로젝트 (React, Node.js, MongoDB)
(12, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1)),
(12, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Node.js' LIMIT 1)),
(12, (SELECT tech_stack_id FROM tech_stack WHERE name = 'MongoDB' LIMIT 1)),

-- 프로젝트 13: Kotlin 안드로이드 게임 (Kotlin)
(13, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Kotlin' LIMIT 1)),

-- 프로젝트 14: 실시간 협업 툴 프론트엔드 (React, TypeScript)
(14, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1)),
(14, (SELECT tech_stack_id FROM tech_stack WHERE name = 'TypeScript' LIMIT 1)),

-- 프로젝트 15: IoT 데이터 수집 서버 (Go, Docker)
(15, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Go' LIMIT 1)),
(15, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Docker' LIMIT 1)),

-- 프로젝트 16: 부동산 중개 플랫폼 (Vue.js, Python, Django)
(16, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Vue.js' LIMIT 1)),
(16, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Python' LIMIT 1)),
(16, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Django' LIMIT 1)),

-- 프로젝트 17: Swift iOS 날씨 앱 (Swift)
(17, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Swift' LIMIT 1)),

-- 프로젝트 18: 펫샵 온라인 쇼핑몰 (React, Spring Boot, MySQL)
(18, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1)),
(18, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Spring Boot' LIMIT 1)),
(18, (SELECT tech_stack_id FROM tech_stack WHERE name = 'MySQL' LIMIT 1)),

-- 프로젝트 19: 크로스플랫폼 여행 일기 (Flutter)
(19, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Flutter' LIMIT 1)),

-- 프로젝트 20: AI 챗봇 서비스 (Python, React)
(20, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Python' LIMIT 1)),
(20, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1))

ON CONFLICT DO NOTHING;