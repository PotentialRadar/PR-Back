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

-- ========= 사용자별 기술 스택 매핑 =========
-- user_tech_stack 테이블에 데이터를 삽입합니다.
-- CTE(Common Table Expression)를 사용하여 각 유저와 기술 스택의 ID를 명시적으로 매핑합니다.

INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level)
SELECT u.user_id, ts.tech_stack_id, 3 -- skill_level은 3으로 통일
FROM users u, tech_stack ts
WHERE
  -- Backend 개발자들
  (u.nickname = '백엔드마스터' AND ts.name IN ('Java', 'Spring Boot', 'MySQL', 'Redis')) OR
  (u.nickname = '스프링부트러버' AND ts.name IN ('Spring Boot', 'JPA', 'Kotlin', 'PostgreSQL')) OR
  (u.nickname = '자바개발자' AND ts.name IN ('Java', 'Spring', 'MySQL', 'GitHub Actions')) OR
  (u.nickname = '마이크로서비스전문가' AND ts.name IN ('Java', 'Spring Boot', 'Docker', 'Kubernetes', 'AWS')) OR
  (u.nickname = '코틀린러버' AND ts.name IN ('Kotlin', 'Spring Boot', 'JPA', 'PostgreSQL')) OR
  (u.nickname = '파이썬백엔드' AND ts.name IN ('Python', 'Django', 'FastAPI', 'MongoDB')) OR
  (u.nickname = '고랑개발자' AND ts.name IN ('Go', 'Kubernetes', 'gRPC', 'Redis')) OR
  (u.nickname = 'DB전문가' AND ts.name IN ('MySQL', 'PostgreSQL', 'MongoDB', 'Redis')) OR
  (u.nickname = '노드개발자' AND ts.name IN ('Node.js', 'Express.js', 'TypeScript', 'MongoDB')) OR
  (u.nickname = 'API마스터' AND ts.name IN ('Java', 'Spring Boot', 'GraphQL', 'AWS')) OR
  (u.nickname = '러스트러버' AND ts.name IN ('Rust', 'Actix-web', 'PostgreSQL', 'Docker')) OR
  (u.nickname = '.NET개발자' AND ts.name IN ('C#', '.NET', 'Azure', 'MS-SQL')) OR

  -- Frontend 개발자들
  (u.nickname = '리액트마스터' AND ts.name IN ('React', 'TypeScript', 'Next.js', 'GraphQL')) OR
  (u.nickname = 'Vue전문가' AND ts.name IN ('Vue.js', 'JavaScript', 'Nuxt.js', 'Pinia')) OR
  (u.nickname = '타입스크립트러버' AND ts.name IN ('TypeScript', 'React', 'Node.js', 'Webpack')) OR
  (u.nickname = '앵귤러개발자' AND ts.name IN ('Angular', 'TypeScript', 'RxJS', 'Ngrx')) OR
  (u.nickname = 'Next.js전문가' AND ts.name IN ('Next.js', 'React', 'Vercel', 'TypeScript')) OR
  (u.nickname = 'UI/UX개발자' AND ts.name IN ('React', 'Figma', 'Storybook', 'CSS-in-JS')) OR
  (u.nickname = '웹퍼포먼스전문가' AND ts.name IN ('JavaScript', 'Lighthouse', 'Webpack', 'Performance API')) OR
  (u.nickname = 'CSS마스터' AND ts.name IN ('CSS3', 'Sass', 'Tailwind CSS', 'Styled-components')) OR
  (u.nickname = '자바스크립트닌자' AND ts.name IN ('JavaScript', 'ES6+', 'Webpack', 'Babel')) OR
  (u.nickname = '프론트엔드아키텍트' AND ts.name IN ('React', 'TypeScript', 'Micro-Frontends', 'Webpack')) OR
  (u.nickname = '모바일웹전문가' AND ts.name IN ('React', 'PWA', 'Responsive Web Design', 'JavaScript')) OR
  (u.nickname = 'PWA개발자' AND ts.name IN ('PWA', 'Service Worker', 'JavaScript', 'Webpack')) OR

  -- Mobile 개발자들
  (u.nickname = '아이폰개발자' AND ts.name IN ('Swift', 'iOS', 'Xcode', 'Combine')) OR
  (u.nickname = '안드로이드마스터' AND ts.name IN ('Kotlin', 'Android', 'Jetpack Compose', 'Coroutines')) OR
  (u.nickname = '크로스플랫폼전문가' AND ts.name IN ('React Native', 'Flutter', 'JavaScript', 'Dart')) OR
  (u.nickname = '플러터개발자' AND ts.name IN ('Flutter', 'Dart', 'BLoC', 'Firebase')) OR
  (u.nickname = 'RN개발자' AND ts.name IN ('React Native', 'TypeScript', 'Redux', 'React Navigation')) OR
  (u.nickname = '스위프트러버' AND ts.name IN ('Swift', 'SwiftUI', 'RxSwift', 'Alamofire')) OR
  (u.nickname = '코틀린모바일' AND ts.name IN ('Kotlin', 'Android', 'Ktor', 'Coroutines')) OR
  (u.nickname = '하이브리드앱전문가' AND ts.name IN ('Ionic', 'Capacitor', 'Angular', 'TypeScript')) OR

  -- DevOps 개발자들
  (u.nickname = '쿠버네티스마스터' AND ts.name IN ('Kubernetes', 'Docker', 'Go', 'Prometheus')) OR
  (u.nickname = '도커전문가' AND ts.name IN ('Docker', 'Docker Compose', 'CI/CD', 'Jenkins')) OR
  (u.nickname = 'AWS아키텍트' AND ts.name IN ('AWS', 'Terraform', 'Kubernetes', 'Lambda')) OR
  (u.nickname = 'CI/CD마스터' AND ts.name IN ('Jenkins', 'GitHub Actions', 'Docker', 'Kubernetes')) OR
  (u.nickname = '인프라전문가' AND ts.name IN ('Terraform', 'Ansible', 'AWS', 'Linux')) OR
  (u.nickname = '테라폼러버' AND ts.name IN ('Terraform', 'AWS', 'GCP', 'HCL')) OR
  (u.nickname = '모니터링전문가' AND ts.name IN ('Prometheus', 'Grafana', 'ELK Stack', 'Datadog')) OR
  (u.nickname = '클라우드엔지니어' AND ts.name IN ('AWS', 'GCP', 'Azure', 'Kubernetes')) OR

  -- AI/ML 개발자들
  (u.nickname = 'AI연구원' AND ts.name IN ('Python', 'TensorFlow', 'PyTorch', 'NumPy')) OR
  (u.nickname = '머신러닝엔지니어' AND ts.name IN ('Python', 'Scikit-learn', 'Pandas', 'Docker')) OR
  (u.nickname = '딥러닝전문가' AND ts.name IN ('PyTorch', 'TensorFlow', 'CUDA', 'Python')) OR
  (u.nickname = '데이터사이언티스트' AND ts.name IN ('Python', 'Pandas', 'Jupyter', 'SQL')) OR
  (u.nickname = 'NLP전문가' AND ts.name IN ('Python', 'Hugging Face', 'NLTK', 'PyTorch')) OR
  (u.nickname = 'MLOps엔지니어' AND ts.name IN ('Kubeflow', 'MLflow', 'Docker', 'Kubernetes', 'AWS')) OR

  -- Full Stack 개발자들
  (u.nickname = '풀스택개발자' AND ts.name IN ('React', 'Node.js', 'Spring Boot', 'TypeScript', 'AWS')) OR
  (u.nickname = '웹개발마스터' AND ts.name IN ('Java', 'Spring Boot', 'React', 'PostgreSQL')) OR
  (u.nickname = '만능개발자' AND ts.name IN ('Python', 'Django', 'Vue.js', 'Docker', 'GCP')) OR
  (u.nickname = '스타트업개발자' AND ts.name IN ('Next.js', 'Node.js', 'TypeScript', 'MongoDB', 'AWS'));

-- 삽입된 데이터 확인
SELECT 'User tech stacks inserted:', COUNT(*) FROM user_tech_stack WHERE user_id > 1;

-- 데이터 확인 쿼리
SELECT COUNT(*) as total_users FROM users WHERE user_id > 1;
SELECT nickname, tech_part FROM users WHERE user_id > 1 ORDER BY user_id LIMIT 10;