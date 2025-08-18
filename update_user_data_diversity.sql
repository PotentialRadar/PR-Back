-- 기존 user_tech_stack 데이터 삭제 후 다시 생성 (더 다양하게)
DELETE FROM user_tech_stack;

-- 먼저 tech_stack 테이블에 기술 스택들이 있는지 확인하고, 없으면 추가
INSERT INTO tech_stack (name) VALUES 
('Java'), ('Spring Boot'), ('MySQL'), ('JavaScript'), ('React'), ('TypeScript'),
('Node.js'), ('Flutter'), ('Kotlin'), ('Swift'), ('Docker'), ('AWS'), 
('Kubernetes'), ('Python'), ('Django'), ('PostgreSQL'), ('Vue.js'), 
('TensorFlow'), ('PyTorch'), ('Go'), ('Angular'), ('HTML/CSS'), ('React Native')
ON CONFLICT (name) DO NOTHING;

-- 사용자별로 다양한 기술 스택 할당
-- Backend 개발자들 (1-8번 사용자)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) 
SELECT 1, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Java', 'Spring Boot', 'MySQL')
UNION ALL
SELECT 2, tech_stack_id, 5 FROM tech_stack WHERE name IN ('Python', 'Django', 'PostgreSQL')
UNION ALL  
SELECT 3, tech_stack_id, 3 FROM tech_stack WHERE name IN ('Node.js', 'JavaScript', 'MySQL')
UNION ALL
SELECT 4, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Go', 'PostgreSQL', 'Docker')
UNION ALL
SELECT 5, tech_stack_id, 5 FROM tech_stack WHERE name IN ('Java', 'Spring Boot', 'AWS')
UNION ALL
SELECT 6, tech_stack_id, 3 FROM tech_stack WHERE name IN ('Python', 'Flask', 'MySQL')
UNION ALL
SELECT 7, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Node.js', 'Express', 'MongoDB')
UNION ALL
SELECT 8, tech_stack_id, 5 FROM tech_stack WHERE name IN ('Java', 'Microservices', 'Kubernetes');

-- Frontend 개발자들 (9-16번 사용자)  
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level)
SELECT 9, tech_stack_id, 5 FROM tech_stack WHERE name IN ('JavaScript', 'React', 'TypeScript')
UNION ALL
SELECT 10, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Vue.js', 'JavaScript', 'HTML/CSS')
UNION ALL
SELECT 11, tech_stack_id, 3 FROM tech_stack WHERE name IN ('Angular', 'TypeScript', 'SCSS')
UNION ALL
SELECT 12, tech_stack_id, 5 FROM tech_stack WHERE name IN ('React', 'Next.js', 'Tailwind')
UNION ALL
SELECT 13, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Vue.js', 'Nuxt.js', 'JavaScript')
UNION ALL
SELECT 14, tech_stack_id, 3 FROM tech_stack WHERE name IN ('React', 'Redux', 'Webpack')
UNION ALL
SELECT 15, tech_stack_id, 5 FROM tech_stack WHERE name IN ('Angular', 'RxJS', 'TypeScript')
UNION ALL
SELECT 16, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Svelte', 'JavaScript', 'Vite');

-- Mobile 개발자들 (17-22번 사용자)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level)
SELECT 17, tech_stack_id, 5 FROM tech_stack WHERE name IN ('Flutter', 'Dart', 'Firebase')
UNION ALL
SELECT 18, tech_stack_id, 4 FROM tech_stack WHERE name IN ('React Native', 'JavaScript', 'Expo')
UNION ALL
SELECT 19, tech_stack_id, 5 FROM tech_stack WHERE name IN ('Kotlin', 'Android', 'Room')
UNION ALL
SELECT 20, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Swift', 'iOS', 'CoreData')
UNION ALL
SELECT 21, tech_stack_id, 3 FROM tech_stack WHERE name IN ('Flutter', 'GetX', 'Hive')
UNION ALL
SELECT 22, tech_stack_id, 5 FROM tech_stack WHERE name IN ('React Native', 'Redux', 'AsyncStorage');

-- DevOps/인프라 개발자들 (23-26번 사용자)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level)
SELECT 23, tech_stack_id, 5 FROM tech_stack WHERE name IN ('Docker', 'Kubernetes', 'AWS')
UNION ALL
SELECT 24, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Jenkins', 'GitLab CI', 'Terraform')
UNION ALL
SELECT 25, tech_stack_id, 5 FROM tech_stack WHERE name IN ('AWS', 'CloudFormation', 'ECS')
UNION ALL
SELECT 26, tech_stack_id, 4 FROM tech_stack WHERE name IN ('Docker', 'Ansible', 'Prometheus');

-- AI/ML 개발자들 (27-30번 사용자)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level)
SELECT 27, tech_stack_id, 5 FROM tech_stack WHERE name IN ('Python', 'TensorFlow', 'Keras')
UNION ALL
SELECT 28, tech_stack_id, 4 FROM tech_stack WHERE name IN ('PyTorch', 'Pandas', 'Scikit-learn')
UNION ALL
SELECT 29, tech_stack_id, 5 FROM tech_stack WHERE name IN ('Python', 'OpenCV', 'NumPy')
UNION ALL
SELECT 30, tech_stack_id, 4 FROM tech_stack WHERE name IN ('TensorFlow', 'Jupyter', 'MLflow');