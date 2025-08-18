-- 기존 user_tech_stack 데이터 삭제 후 다시 생성 (안전한 버전)
DELETE FROM user_tech_stack;

-- 먼저 필요한 기술 스택들이 tech_stack 테이블에 존재하는지 확인하고 없으면 추가
INSERT INTO tech_stack (name) VALUES 
('Java'), ('Spring Boot'), ('React'), ('TypeScript'), ('Python'), ('Django'),
('Node.js'), ('Vue.js'), ('PostgreSQL'), ('Flutter'), ('JavaScript'), 
('Kotlin'), ('MySQL'), ('Angular'), ('Docker'), ('AWS'), ('Kubernetes'),
('TensorFlow'), ('PyTorch'), ('Swift'), ('React Native'), ('MongoDB'),
('Go'), ('HTML/CSS')
ON CONFLICT (name) DO NOTHING;

-- 안전한 방식으로 user_tech_stack 데이터 삽입
-- WITH 절을 사용해서 tech_stack_id를 먼저 조회

WITH tech_ids AS (
  SELECT name, tech_stack_id FROM tech_stack 
  WHERE name IN ('Java', 'Spring Boot', 'React', 'TypeScript', 'Python', 'Django',
                 'Node.js', 'Vue.js', 'PostgreSQL', 'Flutter', 'JavaScript', 
                 'Kotlin', 'MySQL', 'Angular', 'Docker', 'AWS', 'Kubernetes',
                 'TensorFlow', 'PyTorch', 'Swift', 'React Native', 'MongoDB',
                 'Go', 'HTML/CSS')
)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level)
SELECT user_id, tech_stack_id, skill_level FROM (
  VALUES 
    -- 1번 사용자: Backend 파트지만 Frontend 기술도 알고 있는 풀스택
    (1, 'Java', 4),
    (1, 'Spring Boot', 5),
    (1, 'React', 3),
    
    -- 2번 사용자: Frontend 파트지만 Python도 다룰 수 있는 개발자
    (2, 'React', 5),
    (2, 'TypeScript', 4),
    (2, 'Python', 3),
    
    -- 3번 사용자: Mobile 파트지만 웹 기술도 아는 크로스플랫폼 개발자
    (3, 'Flutter', 5),
    (3, 'JavaScript', 4),
    (3, 'Node.js', 3),
    
    -- 4번 사용자: DevOps 파트지만 백엔드 개발도 가능
    (4, 'Docker', 5),
    (4, 'Kubernetes', 4),
    (4, 'Java', 3),
    
    -- 5번 사용자: AI/ML 파트지만 웹 개발도 하는 연구원
    (5, 'Python', 5),
    (5, 'TensorFlow', 4),
    (5, 'Django', 3),
    
    -- 6번 사용자: Full Stack이지만 주로 백엔드 중심
    (6, 'Node.js', 5),
    (6, 'Vue.js', 4),
    (6, 'PostgreSQL', 4),
    
    -- 7번 사용자 (이지은): Backend 파트이지만 모바일 앱도 개발 
    (7, 'Spring Boot', 5),
    (7, 'Kotlin', 4),
    (7, 'MySQL', 4),
    
    -- 8번 사용자: Frontend 파트이지만 DevOps 도구들도 사용
    (8, 'Angular', 5),
    (8, 'Docker', 4),
    (8, 'AWS', 3),
    
    -- 9번 사용자: Mobile 파트이지만 AI 기술에도 관심
    (9, 'React Native', 5),
    (9, 'Python', 4),
    (9, 'TensorFlow', 3),
    
    -- 10번 사용자: DevOps 파트이지만 프론트엔드도 가능
    (10, 'Kubernetes', 5),
    (10, 'React', 4),
    (10, 'TypeScript', 4),
    
    -- 11번: AI/ML + 웹개발
    (11, 'PyTorch', 5),
    (11, 'JavaScript', 3),
    (11, 'Vue.js', 3),
    
    -- 12번: Full Stack + 모바일
    (12, 'Java', 4),
    (12, 'React', 4),
    (12, 'Flutter', 3),
    
    -- 13번: Backend + AI
    (13, 'Python', 5),
    (13, 'Django', 4),
    (13, 'PyTorch', 3),
    
    -- 14번: Frontend + DevOps  
    (14, 'React', 5),
    (14, 'Docker', 4),
    (14, 'AWS', 3),
    
    -- 15번: Mobile + Backend
    (15, 'Swift', 5),
    (15, 'Node.js', 4),
    (15, 'MongoDB', 3),
    
    -- 16번: DevOps + Frontend
    (16, 'Go', 4),
    (16, 'Angular', 3),
    (16, 'Docker', 5),
    
    -- 17번: AI/ML + Mobile
    (17, 'Kotlin', 5),
    (17, 'Spring Boot', 3),
    (17, 'TensorFlow', 4),
    
    -- 18번: Backend + Frontend
    (18, 'TypeScript', 4),
    (18, 'Docker', 4),
    (18, 'Node.js', 5),
    
    -- 19번: Mobile + Frontend
    (19, 'Vue.js', 5),
    (19, 'Python', 3),
    (19, 'React Native', 4),
    
    -- 20번: Full Stack + DevOps
    (20, 'React Native', 4),
    (20, 'PostgreSQL', 4),
    (20, 'AWS', 3),
    
    -- 21번: DevOps + Mobile
    (21, 'Flutter', 5),
    (21, 'AWS', 3),
    (21, 'Kubernetes', 4),
    
    -- 22번: AI/ML + Backend
    (22, 'Java', 4),
    (22, 'React', 4),
    (22, 'Python', 5),
    
    -- 23번: Backend + DevOps
    (23, 'Python', 5),
    (23, 'Kubernetes', 4),
    (23, 'Django', 4),
    
    -- 24번: Frontend + Mobile
    (24, 'JavaScript', 4),
    (24, 'Swift', 3),
    (24, 'React', 5),
    
    -- 25번: Mobile + AI/ML
    (25, 'Node.js', 5),
    (25, 'TensorFlow', 3),
    (25, 'Flutter', 4),
    
    -- 26번: Full Stack + Frontend
    (26, 'Angular', 4),
    (26, 'MySQL', 4),
    (26, 'TypeScript', 5),
    
    -- 27번: DevOps + Backend
    (27, 'Django', 5),
    (27, 'React', 3),
    (27, 'Docker', 4),
    
    -- 28번: AI/ML + Full Stack
    (28, 'Spring Boot', 4),
    (28, 'Flutter', 4),
    (28, 'Python', 5),
    
    -- 29번: Backend + Frontend
    (29, 'TypeScript', 5),
    (29, 'Docker', 4),
    (29, 'PostgreSQL', 4),
    
    -- 30번: Mobile + AI/ML
    (30, 'PyTorch', 4),
    (30, 'Vue.js', 4),
    (30, 'Kotlin', 5)
) AS user_tech_data(user_id, tech_name, skill_level)
JOIN tech_ids ON tech_ids.name = user_tech_data.tech_name
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;