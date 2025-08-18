-- 사용자들에게 다양한 경력 범위 할당 (수정된 버전)

-- FRESHER (신입) - 1, 6, 11, 16, 21, 26번 사용자
UPDATE users SET experience_range = 'FRESHER' WHERE user_id = 1;
UPDATE users SET experience_range = 'FRESHER' WHERE user_id = 6;
UPDATE users SET experience_range = 'FRESHER' WHERE user_id = 11;
UPDATE users SET experience_range = 'FRESHER' WHERE user_id = 16;
UPDATE users SET experience_range = 'FRESHER' WHERE user_id = 21;
UPDATE users SET experience_range = 'FRESHER' WHERE user_id = 26;

-- LT_1 (1년 미만) - 2, 7, 12, 17, 22, 27번 사용자
UPDATE users SET experience_range = 'LT_1' WHERE user_id = 2;
UPDATE users SET experience_range = 'LT_1' WHERE user_id = 7;
UPDATE users SET experience_range = 'LT_1' WHERE user_id = 12;
UPDATE users SET experience_range = 'LT_1' WHERE user_id = 17;
UPDATE users SET experience_range = 'LT_1' WHERE user_id = 22;
UPDATE users SET experience_range = 'LT_1' WHERE user_id = 27;

-- Y1_3 (1-3년) - 3, 8, 13, 18, 23, 28번 사용자
UPDATE users SET experience_range = 'Y1_3' WHERE user_id = 3;
UPDATE users SET experience_range = 'Y1_3' WHERE user_id = 8;
UPDATE users SET experience_range = 'Y1_3' WHERE user_id = 13;
UPDATE users SET experience_range = 'Y1_3' WHERE user_id = 18;
UPDATE users SET experience_range = 'Y1_3' WHERE user_id = 23;
UPDATE users SET experience_range = 'Y1_3' WHERE user_id = 28;

-- Y5_10 (5-10년) - 4, 9, 14, 19, 24, 29번 사용자
UPDATE users SET experience_range = 'Y5_10' WHERE user_id = 4;
UPDATE users SET experience_range = 'Y5_10' WHERE user_id = 9;
UPDATE users SET experience_range = 'Y5_10' WHERE user_id = 14;
UPDATE users SET experience_range = 'Y5_10' WHERE user_id = 19;
UPDATE users SET experience_range = 'Y5_10' WHERE user_id = 24;
UPDATE users SET experience_range = 'Y5_10' WHERE user_id = 29;

-- GE_10 (10년 이상) - 5, 10, 15, 20, 25, 30번 사용자
UPDATE users SET experience_range = 'GE_10' WHERE user_id = 5;
UPDATE users SET experience_range = 'GE_10' WHERE user_id = 10;
UPDATE users SET experience_range = 'GE_10' WHERE user_id = 15;
UPDATE users SET experience_range = 'GE_10' WHERE user_id = 20;
UPDATE users SET experience_range = 'GE_10' WHERE user_id = 25;
UPDATE users SET experience_range = 'GE_10' WHERE user_id = 30;