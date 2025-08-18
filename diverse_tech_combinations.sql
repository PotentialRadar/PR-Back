-- 기존 user_tech_stack 데이터 삭제 후 다시 생성 (기술 파트와 무관한 다양한 조합)
DELETE FROM user_tech_stack;

-- 흥미로운 기술 스택 조합들 (기술 파트와 상관없이)

-- 1번 사용자: Backend 파트지만 Frontend 기술도 알고 있는 풀스택
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT 1, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Java', 'Spring Boot', 'React')
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 2번 사용자: Frontend 파트지만 Python도 다룰 수 있는 개발자
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT 2, tech_stack_id, 5 FROM tech_stack WHERE name IN ('React', 'TypeScript', 'Python')
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 3번 사용자: Mobile 파트지만 웹 기술도 아는 크로스플랫폼 개발자
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT 3, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Flutter', 'JavaScript', 'Node.js')
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 4번 사용자: DevOps 파트지만 백엔드 개발도 가능
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT 4, tech_stack_id, 3 FROM tech_stack WHERE name IN ('Docker', 'Kubernetes', 'Java')
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 5번 사용자: AI/ML 파트지만 웹 개발도 하는 연구원
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT 5, tech_stack_id, 5 FROM tech_stack WHERE name IN ('Python', 'TensorFlow', 'Django')
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 6번 사용자: Full Stack이지만 주로 백엔드 중심
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT 6, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Node.js', 'Vue.js', 'PostgreSQL')
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 7번 사용자 (이지은): Backend 파트이지만 모바일 앱도 개발 
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT 7, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Spring Boot', 'Kotlin', 'MySQL')
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 8번 사용자: Frontend 파트이지만 DevOps 도구들도 사용
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT 8, tech_stack_id, 3 FROM tech_stack WHERE name IN ('Angular', 'Docker', 'AWS')
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 9번 사용자: Mobile 파트이지만 AI 기술에도 관심
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT 9, tech_stack_id, 4 FROM tech_stack WHERE name IN ('React Native', 'Python', 'TensorFlow')
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 10번 사용자: DevOps 파트이지만 프론트엔드도 가능
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT 10, tech_stack_id, 5 FROM tech_stack WHERE name IN ('Kubernetes', 'React', 'TypeScript')
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 나머지 사용자들도 다양한 조합으로 할당 (11-30번)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
-- 11번: AI/ML + 웹개발
(11, (SELECT tech_stack_id FROM tech_stack WHERE name = 'PyTorch' LIMIT 1), 5),
(11, (SELECT tech_stack_id FROM tech_stack WHERE name = 'JavaScript' LIMIT 1), 3),
(11, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Vue.js' LIMIT 1), 3),

-- 12번: Full Stack + 모바일
(12, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Java' LIMIT 1), 4),
(12, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1), 4),
(12, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Flutter' LIMIT 1), 3),

-- 13번: Backend + AI
(13, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Python' LIMIT 1), 5),
(13, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Django' LIMIT 1), 4),
(13, (SELECT tech_stack_id FROM tech_stack WHERE name = 'PyTorch' LIMIT 1), 3),

-- 14번: Frontend + DevOps  
(14, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1), 5),
(14, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Docker' LIMIT 1), 4),
(14, (SELECT tech_stack_id FROM tech_stack WHERE name = 'AWS' LIMIT 1), 3),

-- 15번: Mobile + Backend
(15, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Swift' LIMIT 1), 5),
(15, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Node.js' LIMIT 1), 4),
(15, (SELECT tech_stack_id FROM tech_stack WHERE name = 'MongoDB' LIMIT 1), 3)

ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 더 많은 다양한 조합들 추가 (16-30번 사용자)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT * FROM (VALUES
    -- 다양한 조합들
    (16, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Go' LIMIT 1), 4),
    (16, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Angular' LIMIT 1), 3),
    (17, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Kotlin' LIMIT 1), 5),
    (17, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Spring Boot' LIMIT 1), 3),
    (18, (SELECT tech_stack_id FROM tech_stack WHERE name = 'TypeScript' LIMIT 1), 4),
    (18, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Docker' LIMIT 1), 4),
    (19, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Vue.js' LIMIT 1), 5),
    (19, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Python' LIMIT 1), 3),
    (20, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React Native' LIMIT 1), 4),
    (20, (SELECT tech_stack_id FROM tech_stack WHERE name = 'PostgreSQL' LIMIT 1), 4),
    (21, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Flutter' LIMIT 1), 5),
    (21, (SELECT tech_stack_id FROM tech_stack WHERE name = 'AWS' LIMIT 1), 3),
    (22, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Java' LIMIT 1), 4),
    (22, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1), 4),
    (23, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Python' LIMIT 1), 5),
    (23, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Kubernetes' LIMIT 1), 4),
    (24, (SELECT tech_stack_id FROM tech_stack WHERE name = 'JavaScript' LIMIT 1), 4),
    (24, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Swift' LIMIT 1), 3),
    (25, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Node.js' LIMIT 1), 5),
    (25, (SELECT tech_stack_id FROM tech_stack WHERE name = 'TensorFlow' LIMIT 1), 3),
    (26, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Angular' LIMIT 1), 4),
    (26, (SELECT tech_stack_id FROM tech_stack WHERE name = 'MySQL' LIMIT 1), 4),
    (27, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Django' LIMIT 1), 5),
    (27, (SELECT tech_stack_id FROM tech_stack WHERE name = 'React' LIMIT 1), 3),
    (28, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Spring Boot' LIMIT 1), 4),
    (28, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Flutter' LIMIT 1), 4),
    (29, (SELECT tech_stack_id FROM tech_stack WHERE name = 'TypeScript' LIMIT 1), 5),
    (29, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Docker' LIMIT 1), 4),
    (30, (SELECT tech_stack_id FROM tech_stack WHERE name = 'PyTorch' LIMIT 1), 4),
    (30, (SELECT tech_stack_id FROM tech_stack WHERE name = 'Vue.js' LIMIT 1), 4)
) AS t(user_id, tech_stack_id, skill_level)
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;