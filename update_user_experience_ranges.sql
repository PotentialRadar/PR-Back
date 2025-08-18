-- 사용자들에게 다양한 경력 범위 할당
UPDATE users SET experience_range = 'FRESHER' WHERE user_id IN (1, 6, 11, 16, 21, 26);
UPDATE users SET experience_range = 'LT_1' WHERE user_id IN (2, 7, 12, 17, 22, 27);
UPDATE users SET experience_range = 'Y1_3' WHERE user_id IN (3, 8, 13, 18, 23, 28);
UPDATE users SET experience_range = 'Y5_10' WHERE user_id IN (4, 9, 14, 19, 24, 29);
UPDATE users SET experience_range = 'GE_10' WHERE user_id IN (5, 10, 15, 20, 25, 30);