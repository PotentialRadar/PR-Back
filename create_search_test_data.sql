-- 검색 테스트용 유저 데이터 생성 (50명)
-- 기술 파트와 보유 기술 스택을 다양하게 구성

-- 기존 데이터 정리
DELETE FROM user_tech_stack WHERE user_id > 1;
DELETE FROM users WHERE user_id > 1;

-- 기술 스택 데이터 삽입 (다양한 기술들)
INSERT INTO tech_stack (name) VALUES 
('Java'),
('JavaScript'),
('TypeScript'),
('Python'),
('React'),
('Vue.js'),
('Angular'),
('Node.js'),
('Express.js'),
('Spring Boot'),
('Spring'),
('Django'),
('FastAPI'),
('MySQL'),
('PostgreSQL'),
('MongoDB'),
('Redis'),
('Docker'),
('Kubernetes'),
('AWS'),
('GCP'),
('Azure'),
('Jenkins'),
('GitHub Actions'),
('Terraform'),
('Swift'),
('Kotlin'),
('React Native'),
('Flutter'),
('TensorFlow'),
('PyTorch'),
('Pandas'),
('NumPy'),
('Go'),
('Rust'),
('C++'),
('C#'),
('.NET'),
('GraphQL'),
('Next.js')
ON CONFLICT (name) DO NOTHING;

-- 유저 데이터 생성 (ID 2부터 시작, 총 50명)
INSERT INTO users (nickname, email, password, experience_range, is_portfolio_open, review_count, tech_part, created_at, updated_at) VALUES 
-- Backend 개발자들 (12명)
('백엔드마스터', 'minjun.kim@email.com', 'password', 'Y1_3', true, 0, 'Backend', NOW(), NOW()),
('스프링부트러버', 'seoyun.lee@email.com', 'password', 'Y5_10', true, 0, 'Backend', NOW(), NOW()),
('자바개발자', 'jiho.park@email.com', 'password', 'Y1_3', true, 0, 'Backend', NOW(), NOW()),
('마이크로서비스전문가', 'sua.jung@email.com', 'password', 'GE_10', true, 0, 'Backend', NOW(), NOW()),
('코틀린러버', 'daeun.choi@email.com', 'password', 'Y1_3', true, 0, 'Backend', NOW(), NOW()),
('파이썬백엔드', 'taehyun.kim@email.com', 'password', 'Y5_10', true, 0, 'Backend', NOW(), NOW()),
('고랑개발자', 'haneul.lee@email.com', 'password', 'Y1_3', true, 0, 'Backend', NOW(), NOW()),
('DB전문가', 'sungmin.park@email.com', 'password', 'Y5_10', true, 0, 'Backend', NOW(), NOW()),
('노드개발자', 'yerin.jung@email.com', 'password', 'Y1_3', true, 0, 'Backend', NOW(), NOW()),
('API마스터', 'seojun.yoon@email.com', 'password', 'Y5_10', true, 0, 'Backend', NOW(), NOW()),
('러스트러버', 'doyun.kim@email.com', 'password', 'Y1_3', true, 0, 'Backend', NOW(), NOW()),
('.NET개발자', 'chaewon.lee@email.com', 'password', 'Y5_10', true, 0, 'Backend', NOW(), NOW()),

-- Frontend 개발자들 (12명)
('리액트마스터', 'seohyun.park@email.com', 'password', 'Y1_3', true, 0, 'Frontend', NOW(), NOW()),
('Vue전문가', 'woojin.jung@email.com', 'password', 'Y5_10', true, 0, 'Frontend', NOW(), NOW()),
('타입스크립트러버', 'yena.kim@email.com', 'password', 'Y1_3', true, 0, 'Frontend', NOW(), NOW()),
('앵귤러개발자', 'gunwoo.lee@email.com', 'password', 'Y5_10', true, 0, 'Frontend', NOW(), NOW()),
('Next.js전문가', 'jimin.park@email.com', 'password', 'Y1_3', true, 0, 'Frontend', NOW(), NOW()),
('UI/UX개발자', 'soyoung.jung@email.com', 'password', 'Y1_3', true, 0, 'Frontend', NOW(), NOW()),
('웹퍼포먼스전문가', 'hyunsu.kim@email.com', 'password', 'Y5_10', true, 0, 'Frontend', NOW(), NOW()),
('CSS마스터', 'yujin.lee@email.com', 'password', 'Y1_3', true, 0, 'Frontend', NOW(), NOW()),
('자바스크립트닌자', 'minseo.park@email.com', 'password', 'Y5_10', true, 0, 'Frontend', NOW(), NOW()),
('프론트엔드아키텍트', 'hajun.jung@email.com', 'password', 'GE_10', true, 0, 'Frontend', NOW(), NOW()),
('모바일웹전문가', 'jia.yoon@email.com', 'password', 'Y1_3', true, 0, 'Frontend', NOW(), NOW()),
('PWA개발자', 'taewoo.kim@email.com', 'password', 'Y5_10', true, 0, 'Frontend', NOW(), NOW()),

-- Mobile 개발자들 (8명)
('아이폰개발자', 'seojin.lee@email.com', 'password', 'Y1_3', true, 0, 'Mobile', NOW(), NOW()),
('안드로이드마스터', 'jiwoo.park@email.com', 'password', 'Y5_10', true, 0, 'Mobile', NOW(), NOW()),
('크로스플랫폼전문가', 'haeun.jung@email.com', 'password', 'Y1_3', true, 0, 'Mobile', NOW(), NOW()),
('플러터개발자', 'dohyun.kim@email.com', 'password', 'Y1_3', true, 0, 'Mobile', NOW(), NOW()),
('RN개발자', 'subin.lee@email.com', 'password', 'Y5_10', true, 0, 'Mobile', NOW(), NOW()),
('스위프트러버', 'junseo.park@email.com', 'password', 'Y1_3', true, 0, 'Mobile', NOW(), NOW()),
('코틀린모바일', 'minji.jung@email.com', 'password', 'Y5_10', true, 0, 'Mobile', NOW(), NOW()),
('하이브리드앱전문가', 'sungho.yoon@email.com', 'password', 'Y1_3', true, 0, 'Mobile', NOW(), NOW()),

-- DevOps 개발자들 (8명)
('쿠버네티스마스터', 'junhyuk.kim@email.com', 'password', 'Y5_10', true, 0, 'DevOps', NOW(), NOW()),
('도커전문가', 'chaerin.lee@email.com', 'password', 'Y1_3', true, 0, 'DevOps', NOW(), NOW()),
('AWS아키텍트', 'siwoo.park@email.com', 'password', 'GE_10', true, 0, 'DevOps', NOW(), NOW()),
('CI/CD마스터', 'yejun.jung@email.com', 'password', 'Y5_10', true, 0, 'DevOps', NOW(), NOW()),
('인프라전문가', 'harin.kim@email.com', 'password', 'Y1_3', true, 0, 'DevOps', NOW(), NOW()),
('테라폼러버', 'doyun2.lee@email.com', 'password', 'Y5_10', true, 0, 'DevOps', NOW(), NOW()),
('모니터링전문가', 'seoyeon.park@email.com', 'password', 'Y1_3', true, 0, 'DevOps', NOW(), NOW()),
('클라우드엔지니어', 'hyunwoo.jung@email.com', 'password', 'Y5_10', true, 0, 'DevOps', NOW(), NOW()),

-- AI/ML 개발자들 (6명)
('AI연구원', 'jiwon.kim@email.com', 'password', 'Y5_10', true, 0, 'AI/ML', NOW(), NOW()),
('머신러닝엔지니어', 'sungmin.lee@email.com', 'password', 'Y1_3', true, 0, 'AI/ML', NOW(), NOW()),
('딥러닝전문가', 'yeeun.park@email.com', 'password', 'Y5_10', true, 0, 'AI/ML', NOW(), NOW()),
('데이터사이언티스트', 'seungho.jung@email.com', 'password', 'GE_10', true, 0, 'AI/ML', NOW(), NOW()),
('NLP전문가', 'sohee.yoon@email.com', 'password', 'Y1_3', true, 0, 'AI/ML', NOW(), NOW()),
('MLOps엔지니어', 'taeyoung.kim@email.com', 'password', 'Y5_10', true, 0, 'AI/ML', NOW(), NOW()),

-- Full Stack 개발자들 (4명)
('풀스택개발자', 'junyoung.lee@email.com', 'password', 'Y5_10', true, 0, 'Full Stack', NOW(), NOW()),
('웹개발마스터', 'dahyun.park@email.com', 'password', 'Y1_3', true, 0, 'Full Stack', NOW(), NOW()),
('만능개발자', 'sihyun.jung@email.com', 'password', 'GE_10', true, 0, 'Full Stack', NOW(), NOW()),
('스타트업개발자', 'seojun.kim@email.com', 'password', 'Y1_3', true, 0, 'Full Stack', NOW(), NOW());

-- 사용자 데이터 확인
SELECT 'Users created:', COUNT(*) FROM users WHERE user_id > 1;

-- 기술 스택 매핑은 별도로 실행하거나 애플리케이션에서 처리

-- 데이터 확인 쿼리
SELECT COUNT(*) as total_users FROM users WHERE user_id > 1;
SELECT nickname, tech_part FROM users WHERE user_id > 1 ORDER BY user_id LIMIT 10;