-- 기존 user_tech_stack 데이터 삭제 후 다시 생성 (간단한 버전)
DELETE FROM user_tech_stack;

-- 먼저 필요한 기술 스택들이 tech_stack 테이블에 존재하는지 확인하고 없으면 추가
INSERT INTO tech_stack (name) VALUES 
('Java'), ('Spring Boot'), ('React'), ('TypeScript'), ('Python'), ('Django'),
('Node.js'), ('Vue.js'), ('PostgreSQL'), ('Flutter'), ('JavaScript'), 
('Kotlin'), ('MySQL'), ('Angular'), ('Docker'), ('AWS'), ('Kubernetes'),
('TensorFlow'), ('PyTorch'), ('Swift'), ('React Native'), ('MongoDB'),
('Go'), ('HTML/CSS')
ON CONFLICT (name) DO NOTHING;

-- 1-10번 사용자 기술 스택 할당
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level)
SELECT 1, tech_stack_id, 4 FROM tech_stack WHERE name = 'Java'
UNION ALL SELECT 1, tech_stack_id, 5 FROM tech_stack WHERE name = 'Spring Boot'
UNION ALL SELECT 1, tech_stack_id, 3 FROM tech_stack WHERE name = 'React'

UNION ALL SELECT 2, tech_stack_id, 5 FROM tech_stack WHERE name = 'React'
UNION ALL SELECT 2, tech_stack_id, 4 FROM tech_stack WHERE name = 'TypeScript'
UNION ALL SELECT 2, tech_stack_id, 3 FROM tech_stack WHERE name = 'Python'

UNION ALL SELECT 3, tech_stack_id, 5 FROM tech_stack WHERE name = 'Flutter'
UNION ALL SELECT 3, tech_stack_id, 4 FROM tech_stack WHERE name = 'JavaScript'
UNION ALL SELECT 3, tech_stack_id, 3 FROM tech_stack WHERE name = 'Node.js'

UNION ALL SELECT 4, tech_stack_id, 5 FROM tech_stack WHERE name = 'Docker'
UNION ALL SELECT 4, tech_stack_id, 4 FROM tech_stack WHERE name = 'Kubernetes'
UNION ALL SELECT 4, tech_stack_id, 3 FROM tech_stack WHERE name = 'Java'

UNION ALL SELECT 5, tech_stack_id, 5 FROM tech_stack WHERE name = 'Python'
UNION ALL SELECT 5, tech_stack_id, 4 FROM tech_stack WHERE name = 'TensorFlow'
UNION ALL SELECT 5, tech_stack_id, 3 FROM tech_stack WHERE name = 'Django'

UNION ALL SELECT 6, tech_stack_id, 5 FROM tech_stack WHERE name = 'Node.js'
UNION ALL SELECT 6, tech_stack_id, 4 FROM tech_stack WHERE name = 'Vue.js'
UNION ALL SELECT 6, tech_stack_id, 4 FROM tech_stack WHERE name = 'PostgreSQL'

UNION ALL SELECT 7, tech_stack_id, 5 FROM tech_stack WHERE name = 'Spring Boot'
UNION ALL SELECT 7, tech_stack_id, 4 FROM tech_stack WHERE name = 'Kotlin'
UNION ALL SELECT 7, tech_stack_id, 4 FROM tech_stack WHERE name = 'MySQL'

UNION ALL SELECT 8, tech_stack_id, 5 FROM tech_stack WHERE name = 'Angular'
UNION ALL SELECT 8, tech_stack_id, 4 FROM tech_stack WHERE name = 'Docker'
UNION ALL SELECT 8, tech_stack_id, 3 FROM tech_stack WHERE name = 'AWS'

UNION ALL SELECT 9, tech_stack_id, 5 FROM tech_stack WHERE name = 'React Native'
UNION ALL SELECT 9, tech_stack_id, 4 FROM tech_stack WHERE name = 'Python'
UNION ALL SELECT 9, tech_stack_id, 3 FROM tech_stack WHERE name = 'TensorFlow'

UNION ALL SELECT 10, tech_stack_id, 5 FROM tech_stack WHERE name = 'Kubernetes'
UNION ALL SELECT 10, tech_stack_id, 4 FROM tech_stack WHERE name = 'React'
UNION ALL SELECT 10, tech_stack_id, 4 FROM tech_stack WHERE name = 'TypeScript'

ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 11-20번 사용자 기술 스택 할당
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level)
SELECT 11, tech_stack_id, 5 FROM tech_stack WHERE name = 'PyTorch'
UNION ALL SELECT 11, tech_stack_id, 3 FROM tech_stack WHERE name = 'JavaScript'
UNION ALL SELECT 11, tech_stack_id, 3 FROM tech_stack WHERE name = 'Vue.js'

UNION ALL SELECT 12, tech_stack_id, 4 FROM tech_stack WHERE name = 'Java'
UNION ALL SELECT 12, tech_stack_id, 4 FROM tech_stack WHERE name = 'React'
UNION ALL SELECT 12, tech_stack_id, 3 FROM tech_stack WHERE name = 'Flutter'

UNION ALL SELECT 13, tech_stack_id, 5 FROM tech_stack WHERE name = 'Python'
UNION ALL SELECT 13, tech_stack_id, 4 FROM tech_stack WHERE name = 'Django'
UNION ALL SELECT 13, tech_stack_id, 3 FROM tech_stack WHERE name = 'PyTorch'

UNION ALL SELECT 14, tech_stack_id, 5 FROM tech_stack WHERE name = 'React'
UNION ALL SELECT 14, tech_stack_id, 4 FROM tech_stack WHERE name = 'Docker'
UNION ALL SELECT 14, tech_stack_id, 3 FROM tech_stack WHERE name = 'AWS'

UNION ALL SELECT 15, tech_stack_id, 5 FROM tech_stack WHERE name = 'Swift'
UNION ALL SELECT 15, tech_stack_id, 4 FROM tech_stack WHERE name = 'Node.js'
UNION ALL SELECT 15, tech_stack_id, 3 FROM tech_stack WHERE name = 'MongoDB'

UNION ALL SELECT 16, tech_stack_id, 4 FROM tech_stack WHERE name = 'Go'
UNION ALL SELECT 16, tech_stack_id, 3 FROM tech_stack WHERE name = 'Angular'
UNION ALL SELECT 16, tech_stack_id, 5 FROM tech_stack WHERE name = 'Docker'

UNION ALL SELECT 17, tech_stack_id, 5 FROM tech_stack WHERE name = 'Kotlin'
UNION ALL SELECT 17, tech_stack_id, 3 FROM tech_stack WHERE name = 'Spring Boot'
UNION ALL SELECT 17, tech_stack_id, 4 FROM tech_stack WHERE name = 'TensorFlow'

UNION ALL SELECT 18, tech_stack_id, 4 FROM tech_stack WHERE name = 'TypeScript'
UNION ALL SELECT 18, tech_stack_id, 4 FROM tech_stack WHERE name = 'Docker'
UNION ALL SELECT 18, tech_stack_id, 5 FROM tech_stack WHERE name = 'Node.js'

UNION ALL SELECT 19, tech_stack_id, 5 FROM tech_stack WHERE name = 'Vue.js'
UNION ALL SELECT 19, tech_stack_id, 3 FROM tech_stack WHERE name = 'Python'
UNION ALL SELECT 19, tech_stack_id, 4 FROM tech_stack WHERE name = 'React Native'

UNION ALL SELECT 20, tech_stack_id, 4 FROM tech_stack WHERE name = 'React Native'
UNION ALL SELECT 20, tech_stack_id, 4 FROM tech_stack WHERE name = 'PostgreSQL'
UNION ALL SELECT 20, tech_stack_id, 3 FROM tech_stack WHERE name = 'AWS'

ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- 21-30번 사용자 기술 스택 할당
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level)
SELECT 21, tech_stack_id, 5 FROM tech_stack WHERE name = 'Flutter'
UNION ALL SELECT 21, tech_stack_id, 3 FROM tech_stack WHERE name = 'AWS'
UNION ALL SELECT 21, tech_stack_id, 4 FROM tech_stack WHERE name = 'Kubernetes'

UNION ALL SELECT 22, tech_stack_id, 4 FROM tech_stack WHERE name = 'Java'
UNION ALL SELECT 22, tech_stack_id, 4 FROM tech_stack WHERE name = 'React'
UNION ALL SELECT 22, tech_stack_id, 5 FROM tech_stack WHERE name = 'Python'

UNION ALL SELECT 23, tech_stack_id, 5 FROM tech_stack WHERE name = 'Python'
UNION ALL SELECT 23, tech_stack_id, 4 FROM tech_stack WHERE name = 'Kubernetes'
UNION ALL SELECT 23, tech_stack_id, 4 FROM tech_stack WHERE name = 'Django'

UNION ALL SELECT 24, tech_stack_id, 4 FROM tech_stack WHERE name = 'JavaScript'
UNION ALL SELECT 24, tech_stack_id, 3 FROM tech_stack WHERE name = 'Swift'
UNION ALL SELECT 24, tech_stack_id, 5 FROM tech_stack WHERE name = 'React'

UNION ALL SELECT 25, tech_stack_id, 5 FROM tech_stack WHERE name = 'Node.js'
UNION ALL SELECT 25, tech_stack_id, 3 FROM tech_stack WHERE name = 'TensorFlow'
UNION ALL SELECT 25, tech_stack_id, 4 FROM tech_stack WHERE name = 'Flutter'

UNION ALL SELECT 26, tech_stack_id, 4 FROM tech_stack WHERE name = 'Angular'
UNION ALL SELECT 26, tech_stack_id, 4 FROM tech_stack WHERE name = 'MySQL'
UNION ALL SELECT 26, tech_stack_id, 5 FROM tech_stack WHERE name = 'TypeScript'

UNION ALL SELECT 27, tech_stack_id, 5 FROM tech_stack WHERE name = 'Django'
UNION ALL SELECT 27, tech_stack_id, 3 FROM tech_stack WHERE name = 'React'
UNION ALL SELECT 27, tech_stack_id, 4 FROM tech_stack WHERE name = 'Docker'

UNION ALL SELECT 28, tech_stack_id, 4 FROM tech_stack WHERE name = 'Spring Boot'
UNION ALL SELECT 28, tech_stack_id, 4 FROM tech_stack WHERE name = 'Flutter'
UNION ALL SELECT 28, tech_stack_id, 5 FROM tech_stack WHERE name = 'Python'

UNION ALL SELECT 29, tech_stack_id, 5 FROM tech_stack WHERE name = 'TypeScript'
UNION ALL SELECT 29, tech_stack_id, 4 FROM tech_stack WHERE name = 'Docker'
UNION ALL SELECT 29, tech_stack_id, 4 FROM tech_stack WHERE name = 'PostgreSQL'

UNION ALL SELECT 30, tech_stack_id, 4 FROM tech_stack WHERE name = 'PyTorch'
UNION ALL SELECT 30, tech_stack_id, 4 FROM tech_stack WHERE name = 'Vue.js'
UNION ALL SELECT 30, tech_stack_id, 5 FROM tech_stack WHERE name = 'Kotlin'

ON CONFLICT (user_id, tech_stack_id) DO NOTHING;