-- Create user_tech_stack table
CREATE TABLE IF NOT EXISTS user_tech_stack (
    user_tech_stack_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tech_stack_id BIGINT NOT NULL,
    skill_level INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (tech_stack_id) REFERENCES tech_stack(tech_stack_id) ON DELETE CASCADE,
    UNIQUE(user_id, tech_stack_id)
);

-- Insert sample UserTechStack relationship data
-- Assuming we have users with IDs 1-30 and tech stacks with IDs 1-20

-- User 1 (Backend focused)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
(1, 1, 4), (1, 2, 5), (1, 3, 3) -- Java, Spring Boot, MySQL
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- User 2 (Frontend focused) 
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
(2, 4, 5), (2, 5, 4), (2, 6, 3) -- JavaScript, React, TypeScript
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- User 3 (Full Stack)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
(3, 1, 4), (3, 4, 4), (3, 5, 3), (3, 7, 3) -- Java, JavaScript, React, Node.js
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- User 4 (Mobile focused)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
(4, 8, 5), (4, 9, 4), (4, 10, 3) -- Flutter, Kotlin, Swift
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- User 5 (DevOps focused)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
(5, 11, 4), (5, 12, 5), (5, 13, 3) -- Docker, AWS, Kubernetes
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- User 6 (Python Backend)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
(6, 14, 5), (6, 15, 4), (6, 16, 3) -- Python, Django, PostgreSQL
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- User 7 (이지은 - Frontend)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
(7, 4, 5), (7, 5, 4), (7, 17, 4) -- JavaScript, React, Vue.js
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- User 8 (AI/ML focused)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
(8, 14, 4), (8, 18, 5), (8, 19, 4) -- Python, TensorFlow, PyTorch
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- User 9 (Go Backend)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
(9, 20, 5), (9, 11, 3), (9, 16, 4) -- Go, Docker, PostgreSQL
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- User 10 (Angular Frontend)
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
(10, 6, 4), (10, 21, 5), (10, 22, 3) -- TypeScript, Angular, HTML/CSS
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;

-- Continue for remaining users (11-30) with diverse tech stack combinations
INSERT INTO user_tech_stack (user_id, tech_stack_id, skill_level) VALUES 
-- User 11 (Backend)
(11, 1, 3), (11, 2, 4), (11, 3, 3),
-- User 12 (Frontend)
(12, 4, 4), (12, 5, 3), (12, 22, 4),
-- User 13 (Mobile)
(13, 8, 4), (13, 9, 5), (13, 4, 3),
-- User 14 (DevOps)
(14, 11, 5), (14, 12, 4), (14, 20, 3),
-- User 15 (Python)
(15, 14, 4), (15, 15, 3), (15, 18, 4),
-- User 16 (Full Stack)
(16, 1, 4), (16, 4, 4), (16, 5, 4),
-- User 17 (AI/ML)
(17, 14, 5), (17, 18, 4), (17, 19, 3),
-- User 18 (Go)
(18, 20, 4), (18, 11, 4), (18, 12, 3),
-- User 19 (Vue.js)
(19, 4, 4), (19, 17, 5), (19, 6, 3),
-- User 20 (Spring)
(20, 1, 5), (20, 2, 4), (20, 16, 4),
-- User 21 (React Native)
(21, 4, 4), (21, 23, 5), (21, 8, 3),
-- User 22 (Backend)
(22, 1, 3), (22, 2, 3), (22, 3, 4),
-- User 23 (Frontend)
(23, 4, 5), (23, 5, 4), (23, 21, 3),
-- User 24 (Mobile)
(24, 9, 4), (24, 10, 5), (24, 4, 3),
-- User 25 (DevOps)
(25, 11, 4), (25, 12, 5), (25, 13, 4),
-- User 26 (Python)
(26, 14, 5), (26, 15, 4), (26, 16, 3),
-- User 27 (Full Stack)
(27, 7, 4), (27, 4, 4), (27, 3, 3),
-- User 28 (AI/ML)
(28, 14, 4), (28, 18, 5), (28, 19, 4),
-- User 29 (Go)
(29, 20, 5), (29, 16, 4), (29, 11, 3),
-- User 30 (Angular)
(30, 6, 5), (30, 21, 4), (30, 22, 4)
ON CONFLICT (user_id, tech_stack_id) DO NOTHING;